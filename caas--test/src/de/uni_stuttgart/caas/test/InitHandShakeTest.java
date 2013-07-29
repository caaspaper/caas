package de.uni_stuttgart.caas.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import de.uni_stuttgart.caas.admin.AdminNode;
import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.cache.CacheNode;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;

public class InitHandShakeTest {

	private CountDownLatch rejectedCacheNodesLatch = null;
	private CountDownLatch activateLatch = null;

	private volatile boolean canAddToGrid = false;
	private AtomicInteger canActivateCountdown = null;

	private class CacheNodeTester extends CacheNode {

		public CacheNodeTester(String host, int port) throws IOException {
			super(host, port);
		}

		@Override
		public IMessage process(IMessage message) {
			final MessageType type = message.getMessageType();

			switch (type) {

			case CONFIRM:
				//
				final ConfirmationMessage c = (ConfirmationMessage) message;
				if (c.STATUS_CODE != 0) {
					// the cache node's request to be part of the grid failed
					rejectedCacheNodesLatch.countDown();
				}
				break;

			case ADD_TO_GRID:
				assertTrue(canAddToGrid);
				if (canActivateCountdown.decrementAndGet() == 0) {
					canAddToGrid = false;
				}
				break;

			case ACTIVATE:
				assertTrue(canActivateCountdown.get() == 0);
				activateLatch.countDown();
				break;

			default:
				fail("unexpected message");
			}

			return super.process(message);
		}
	}

	/**
	 * Run an AdminNode with capacity of `capacity` and connect `numNodes`
	 * CacheNode instances to it. Verify that the behavior of all parties is
	 * correct, including for rejected cache nodes.
	 * 
	 * @param capacity
	 *            Admin node grid capacity, must be greater than 0
	 * @param numNodes
	 *            Number of cache nodes to check, must be greater equal than the
	 *            value given for `capacity`
	 */
	protected void testInitialHandshake(final int capacity, final int numNodes) {
		assertTrue(capacity > 0);
		assertTrue(numNodes >= capacity);

		canActivateCountdown = new AtomicInteger(capacity);

		// we expect `capacity` nodes to finally receive ACTIVATE messages
		activateLatch = new CountDownLatch(capacity);

		// and `numNodes - capacity` nodes to receive error confirms upon
		// sending their JOIN request
		rejectedCacheNodesLatch = new CountDownLatch(numNodes - capacity);

		final int adminPort = 6535;

		AdminNode admin = null;
		try {
			admin = new AdminNode(adminPort, capacity);
		} catch (IOException e1) {
			e1.printStackTrace();
			fail("unexpected interruption (1)");
		}

		CacheNode[] nodes = new CacheNode[numNodes];

		// connect the requested number of cache nodes to the admin node
		for (int i = 0; i < numNodes; ++i) {
			if (i == capacity - 1) {
				// after this node has been added, the server is allowed to send
				// ADD_TO_GRID messages
				canAddToGrid = true;
			}
			while (true) {
				try {
					nodes[i] = new CacheNodeTester("localhost", 6535);
					break;
				} catch (IOException e) {
					// this might happen if the thread did not start yet,
					// therefore, keep trying. Only do this for the first
					// cache node, though.
					assertEquals(0, i);
				}
			}
		}

		// very moderate timeout to make sure this eventually fails if the
		// configuration hangs
		try {
			assertTrue(activateLatch.await(20, TimeUnit.SECONDS));
			assertTrue(rejectedCacheNodesLatch.await(20, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("unexpected interruption (1)");
		}

		// cleanup
		for (CacheNode node : nodes) {
			node.close();
		}
		admin.close();
	}

	@Test
	public void testCapacity3NodeCount3() {
		testInitialHandshake(3, 3);
	}

	@Test
	public void testCapacity3NodeCount5() {
		testInitialHandshake(3, 5);
	}

	@Test
	public void testCapacity50NodeCount50() {
		testInitialHandshake(50, 50);
	}

	@Test
	public void testCapacity50NodeCount51() {
		testInitialHandshake(50, 51);
	}
}
