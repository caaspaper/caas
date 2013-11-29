package de.uni_stuttgart.caas.base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

public class LogSender {

	private DatagramSocket socket;
	private final SocketAddress socketAddress;
	private BufferedWriter writer;
	private static int ID = 0;
	public final boolean remoteLogs, fileLogs, consoleLogs;
	
	
	public LogSender(SocketAddress s) {
		this(s, true, true, false);
	}
	public LogSender(SocketAddress s, boolean remoteLogs, boolean fileLogs, boolean consoleLogs) {
		this.remoteLogs = remoteLogs;
		this.fileLogs = fileLogs;
		this.consoleLogs = consoleLogs;
		
		if (!(remoteLogs || fileLogs || consoleLogs)) {
			throw new IllegalArgumentException("Where should I log??");
		}
		
		if (remoteLogs) {
			try {
				socket = new DatagramSocket();
			} catch (SocketException e) {
				System.out.println("Failed to initialize Logger, bailing");
				e.printStackTrace();
				System.exit(-1);
			}
			socketAddress = s;
		} else {
			socketAddress = null;
		}
		if (fileLogs) {
			int id = ++ID;
			try {
				writer = new BufferedWriter(new FileWriter(new File("/tmp/log_" + id + ".txt")));
			} catch (IOException e) {
				// non-unix where  /tmp is probably not available
				try {
					writer = new BufferedWriter(new FileWriter(new File("log_" + id + ".txt")));
				} catch (IOException e2) {
					e.printStackTrace();
					e2.printStackTrace();
				}
			}
		}
	}
	
	
	public void write(String message) {
		if (fileLogs) {
			try {
				writer.write(message + "\n");
				writer.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if (remoteLogs) {
			byte[] buffer = message.getBytes();
			try {
				DatagramPacket p = new DatagramPacket(buffer, buffer.length, socketAddress);
				socket.send(p);
			} catch (IOException e) {
				System.out.println("Failed to send log message");
				e.printStackTrace();
			}
		}
		if (consoleLogs) {
			System.out.println(message);
		}
	}
}
