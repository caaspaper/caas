package de.uni_stuttgart.caas.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.base.FullDuplexMPI.IResponseHandler;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;

public class FullDuplexMPITest {

	private int party0Counter = 0;
	private int party1Counter = 100;

	private final CountDownLatch latch = new CountDownLatch(2);

	// Utility for testBidirectionalCommunicationWithResponses()
	private void sendRecursiveAsyncMessages(final FullDuplexMPI party, final boolean first) {

		final int counter = first ? party0Counter : party1Counter;

		System.out.println(party + ": send " + counter);
		party.sendMessageAsync(new ConfirmationMessage(counter, ""), new IResponseHandler() {
			@Override
			public void onResponseReceived(IMessage response) {
				assertTrue(response instanceof ConfirmationMessage);
				assertTrue(((ConfirmationMessage) response).STATUS_CODE == counter + (first ? 1 : -1));

				final int newCounter = counter + (first ? 2 : -2);
				if (first && newCounter == 100 || !first && newCounter == 0) {
					latch.countDown();
					return;
				}

				if (first) {
					party0Counter = newCounter;
				} else {
					party1Counter = newCounter;
				}
				sendRecursiveAsyncMessages(party, first);
			}

			@Override
			public void onConnectionAborted() {
				fail("onConnectionAborted() unexpected");
			}
		});
	}

	/**
	 * Create two FullDuplexMPI instances and connect them to each other. Have
	 * each of them send 50 messages to the other party with status codes so
	 * that the sequence of messages and responses is: 0 (message), 1
	 * (response), 2 (message), 3 (...), 4 .. respectively 100, 99, 98 .. for
	 * the other direction
	 */
	@Test
	public void testBidirectionalCommunicationWithResponses() {

		final int port0 = 6535;
		final int port1 = 6536;

		final Socket sock1 = new Socket();

		Thread adminThread = null;

		// sock0 is "server", waiting for sock1 to connect
		try {
			final ServerSocket tempServer = new ServerSocket();
			// allow re-using previous server sockets. This is needed because
			// the OS would otherwise not allow to bind to the port if the last
			// test run was within the TCP TIME_WAIT time.
			tempServer.setReuseAddress(true);
			tempServer.bind(new InetSocketAddress("localhost", port0));

			adminThread = new Thread(new Runnable() {

				@Override
				public void run() {
					Socket sock0 = null;

					try {
						sock0 = tempServer.accept();
						tempServer.close();
					} catch (IOException e) {
						fail("connection failed (3)");
						try {
							tempServer.close();
						} catch (IOException e1) {
							// ignore
						}
						return;
					}

					FullDuplexMPI party0 = null;
					try {
						party0 = new FullDuplexMPI(sock0, System.out) {

							@Override
							public IMessage processIncomingMessage(IMessage message) {
								assertTrue(message instanceof ConfirmationMessage);
								assertTrue(((ConfirmationMessage) message).STATUS_CODE == party1Counter);
								return new ConfirmationMessage(--party1Counter, null);
							}
						};
						sendRecursiveAsyncMessages(party0, true);
					} catch (IOException e) {
						fail("connection failed (0)");
					}

					try {
						// moderate timeout to make sure we eventually get a
						// failure
						latch.await(20, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						fail("unexpected interruption");
					} finally {
						if (party0 != null) {
							party0.close();
						}
					}
				}
			});
		} catch (IOException e1) {
			fail("connection failed (2)");
		}

		adminThread.start();

		FullDuplexMPI party1 = null;

		// sock1 is "client"
		try {
			sock1.bind(new InetSocketAddress("localhost", port1));
			sock1.connect(new InetSocketAddress("localhost", port0));
			party1 = new FullDuplexMPI(sock1, System.out) {

				@Override
				public IMessage processIncomingMessage(IMessage message) {
					assertTrue(message instanceof ConfirmationMessage);
					assertTrue(((ConfirmationMessage) message).STATUS_CODE == party0Counter);
					return new ConfirmationMessage(++party0Counter, null);
				}
			};
		} catch (IOException e) {
			fail("connection failed (1)");
		}

		assert party1 != null;
		sendRecursiveAsyncMessages(party1, false);

		try {
			// moderate timeout to make sure we eventually get a failure
			latch.await(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("unexpected interruption (1)");
			e.printStackTrace();
		}

		// check if all messages were acknowledged
		assertEquals(99, party0Counter);
		assertEquals(1, party1Counter);

		try {
			sock1.close();
		} catch (IOException e) {
			// ignore
		}
		try {
			adminThread.join();
		} catch (InterruptedException e) {
			fail("unexpected interruption (2)");
			e.printStackTrace();
		}
	}
}
