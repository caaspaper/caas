package de.uni_stuttgart.caas.base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
	private final boolean consoleLogs, fileLogs;
	private BufferedWriter writer;

	public LogReceiver(int port, boolean consoleLogs, boolean fileLogs) {
		this.consoleLogs = consoleLogs;
		this.fileLogs = fileLogs;
		logProcessor = new LogProcessor();
		messageQueue = new LinkedBlockingQueue<>();
		try {
			serverSocket = new DatagramSocket(port);
			serverSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (fileLogs) {
			try {
				writer = new BufferedWriter(new FileWriter(new File("/tmp/combined_logs.txt")));
			} catch (IOException e1) {
				try {
					writer = new BufferedWriter(new FileWriter(new File("combined_logs.txt")));
				} catch (IOException e2) {
					e1.printStackTrace();
					e2.printStackTrace();
				}
			}
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
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				t.join();
			} catch (InterruptedException e) {
				// should never occur
			}
		}

	}

	public void processMessage(String message) {
		if (consoleLogs) {
			System.out.println(message);
		}
		if (fileLogs) {
			try {
				writer.write(message + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
