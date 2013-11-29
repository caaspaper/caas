package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.messages.QueryResult;

public class QuerySender {

	public static void generateRandomQuery(String host, int port, int numOfQueries, LogSender logger) {
		for (int i = 0; i < numOfQueries; i++) {
			new QueryRunner(host, port, logger);
		}
	}
	
	
	public static void main(String[] args) {
		generateRandomQuery("localhost", 45530, 1, new LogSender(null, false, false, true));
	}
}

class QueryRunner implements Runnable{
	
	public final String host;
	public final int port;
	private LogSender logger;
	
	public QueryRunner(String host, int port, LogSender logger) {
		
		this.host = host;
		this.port = port;
		this.logger = logger;
		(new Thread(this)).start();
	}
	
	@Override
	public void run() {
		
		
		try {
			Socket server = new Socket(host, port);
			// 0 for a random port
			ServerSocket receiverSocket = new ServerSocket(0);
			ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
			out.writeObject(receiverSocket.getInetAddress().getHostAddress());
			out.writeObject(receiverSocket.getLocalPort());
			out.close();
			
			Socket client = receiverSocket.accept();

			// out only needed so we can create the inputStreamReader
			out = new ObjectOutputStream(client.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			Object o = in.readObject();
			if (o instanceof QueryResult) {
				logger.write("Client received: " + (((QueryResult) o).getDebuggingInfo()));
			} else {
				logger.write("Client didn't receive QueryResult but some other Object");
			}
			server.close();
			client.close();
			in.close();
			out.close();
			receiverSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}