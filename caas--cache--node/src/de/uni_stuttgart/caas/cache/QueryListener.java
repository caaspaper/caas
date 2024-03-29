package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.messages.QueryMessage;

public class QueryListener implements Runnable {

	private CacheNode cacheNode;
	private ServerSocket serverSocket;
	private Thread t;
	private LogSender logger;
	
	public QueryListener(CacheNode cacheNode, LogSender logger) {
		
		this.cacheNode = cacheNode;
		this.logger = logger;
		try {
			serverSocket = new ServerSocket(0);
			serverSocket.setSoTimeout(1000);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		t = Thread.currentThread();
		Socket clientSocket = null;
		while (!t.isInterrupted() || clientSocket != null && !clientSocket.isClosed()) {
			
			try {
				clientSocket = serverSocket.accept();
				logger.write("client connected requesting data");
				
				// no thread here - this is too expensive and we do not typically block for long			
				Runnable r = new ListenerThread(clientSocket);
				r.run();
				
			} catch (IOException e) {
				
			}
			
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stops the server
	 */
	public void stop() {
		t.interrupt();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Accepts new queries
	 * 
	 */
	private class ListenerThread implements Runnable{
		
		private Socket clientSocket;
		
		public ListenerThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
	
				Object o = in.readObject();
				if (o instanceof QueryMessage) {
					logger.write("passing QueryMessage to CacheNode");
					cacheNode.processQuery((QueryMessage) o);
				} 
				else {
					assert false;
				}
				in.close();
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} 
		}
	}


	public int getPort() {
		return serverSocket.getLocalPort();
	}
	
	public InetAddress getInetAddress() {
		return serverSocket.getInetAddress();
	}
}
