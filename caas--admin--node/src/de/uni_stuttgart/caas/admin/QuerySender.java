package de.uni_stuttgart.caas.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class QuerySender {

	
	public QuerySender() {
		
	}
	
	
	
	public void generateRandomQuery(String host, int port, int numOfQueries) {
		for (int i = 0; i < numOfQueries; i++) {
			new QueryRunner(host, port);
		}
	}
	
	private class QueryRunner implements Runnable{
		
		public final String host;
		public final int port;
		
		public QueryRunner(String host, int port) {
			
			this.host = host;
			this.port = port;
			(new Thread(this)).start();
		}

		@Override
		public void run() {
			BufferedReader reader = null;
			PrintWriter writer = null;
			Socket server;
			
			
			
			try {
				server = new Socket(host, port);
				reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
				writer = new PrintWriter(server.getOutputStream());
				writer.write("\n");
				writer.flush();
				
				String response;
				while ((response = reader.readLine()) != null) {
					if (response.equals("END")) {
						
					} else {
						
					}
				}
				
				reader.close();
				writer.close();
				server.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
