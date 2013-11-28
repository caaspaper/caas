package de.uni_stuttgart.caas.base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

public class LogReceiver implements Runnable {

	private DatagramSocket serverSocket;
	private Thread t;
	private LinkedBlockingQueue<String> messageQueue;
	private LogProcessor logProcessor;

	public LogReceiver(int port) {
		logProcessor = new LogProcessor();
		messageQueue = new LinkedBlockingQueue<>();
		try {
			serverSocket = new DatagramSocket(port);
			serverSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	@Override
	public void run() {
		(new Thread(logProcessor)).start();
		t = Thread.currentThread();
		while (!t.isInterrupted()) {
			DatagramPacket p;
			byte buffer[] = new byte[256];
			p = new DatagramPacket(buffer, buffer.length);
			try {
				serverSocket.receive(p);
				messageQueue.offer(new String(buffer,0,p.getLength()));
			} catch (SocketTimeoutException e) {
				// normal continue
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		logProcessor.stop();
		t.interrupt();
		try {
			t.join();
		} catch (InterruptedException e) {
			// should never occur
		}
	}

	private class LogProcessor implements Runnable {

		private Thread t;

		@Override
		public void run() {
			t = Thread.currentThread();

			while (true) {
				try {
					processMessage(messageQueue.take());
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		public void stop() {
			t.interrupt();
			try {
				t.join();
			} catch (InterruptedException e) {
				// should never occur
			}
		}

	}

	public void processMessage(String message) {
		System.out.println(message);
	}

}
