package de.uni_stuttgart.caas.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class QueryListener implements Runnable {

	private CacheNode cacheNode;
	private ServerSocket serverSocket;
	private boolean isRunning;
	
	public QueryListener(CacheNode cacheNode, int port) {
		
		this.cacheNode = cacheNode;
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(1000);
			isRunning = true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		
		Socket clientSocket;
		while (isRunning) {
			
			try {
				clientSocket = serverSocket.accept();
				new Thread(new ListenerThread(cacheNode, clientSocket)).start();
			} catch (IOException e) {
				e.printStackTrace();
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
		isRunning = false;
	}
	
	
	/**
	 * Accepts new queries
	 * 
	 */
	private class ListenerThread implements Runnable{
		
		private CacheNode cacheNode;
		private Socket clientSocket;
		
		public ListenerThread(CacheNode cacheNode, Socket clientSocket) {
			this.cacheNode = cacheNode;
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.equals("STOP")) {
						cacheNode.processIncomingQueryToAdaptItToNetwork(null, clientSocket);
					} else {
						break;
					}
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
