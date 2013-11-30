package de.uni_stuttgart.caas.test;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;

import de.uni_stuttgart.caas.base.LogReceiver;
import de.uni_stuttgart.caas.base.LogSender;


public class LogTester {
	
	public static final int NUM_OF_MESSAGES = 600;

	private int receivedMessages = 0;
	@Test
	public void test() {
		LogSender s = new LogSender(new InetSocketAddress("localhost", 6050));
		LogReceiveTester r = new LogReceiveTester(6050);
		(new Thread(r)).start();
		
		for (int i = 0; i < NUM_OF_MESSAGES; ++i) {
			s.write("derp");
		}
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(receivedMessages);
		assertEquals(NUM_OF_MESSAGES, receivedMessages);
	}
	
	
	private class LogReceiveTester extends LogReceiver {

		public LogReceiveTester(int port) {
			super(port, true, false);
		}
		
		@Override
		public void processMessage(String message) {
			++receivedMessages;
		}
	}

}
