package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.QueryMessage;
import de.uni_stuttgart.caas.messages.QueryResult;

public class QuerySender {

	public static void generateRandomQuery(String ip, int port, LogSender logger) {
		QueryReceiver receiver = new QueryReceiver(logger, 1);
		(new Thread(receiver)).start();
		sendQuery(new QueryMessage(new LocationOfNode(500, 500), ip, receiver.getPort(), new InetSocketAddress(ip, port)));
	}

	public static void generateUniformlyDistributedQueries(int numOfQueriesPerNode, Map<InetSocketAddress, NodeInfo> nodes, LogSender logger) {
		List<InetSocketAddress> addresses = new ArrayList<>(nodes.size());
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			addresses.add(e.getValue().ADDRESS_FOR_CACHENODE_QUERYLISTENER);
		}
		int counter = 0;
		ExecutorService executor = Executors.newFixedThreadPool(10);
		QueryReceiver receiver = new QueryReceiver(logger, numOfQueriesPerNode * addresses.size());
		(new Thread(receiver)).start();
		String ip = receiver.getHost();
		int port = receiver.getPort();
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			for (int i = 0; i < numOfQueriesPerNode; ++i) {
				final QueryMessage m = new QueryMessage(e.getValue().getLocationOfNode(), ip, port, addresses.get(counter));
				executor.execute(new Runnable() {

					@Override
					public void run() {
						sendQuery(m);
					}
				});
				++counter;
			}
			counter = 0;
		}
	}
	
	public static void generateUniformlyDistributedQueriesEnteringAtOneLocation(int numOfQueriesPerNode,
			Map<InetSocketAddress, NodeInfo> nodes, LogSender logger) {
	
		ExecutorService executor = Executors.newFixedThreadPool(10);
		QueryReceiver receiver = new QueryReceiver(logger, numOfQueriesPerNode * nodes.size());
		(new Thread(receiver)).start();
		
		String ip = receiver.getHost();
		int port = receiver.getPort();
		
		int randomNum = (int) (Math.random() * nodes.size());
		InetSocketAddress randomEntryNode = null;
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			if (randomNum == 0) {
				randomEntryNode = e.getValue().ADDRESS_FOR_CACHENODE_QUERYLISTENER;
				break;
			}
			--randomNum;
		}
		assert randomEntryNode != null;
		
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			for (int i = 0; i < numOfQueriesPerNode; ++i) {
				final QueryMessage m = new QueryMessage(e.getValue().getLocationOfNode(), ip, port, randomEntryNode);
				executor.execute(new Runnable() {

					@Override
					public void run() {
						sendQuery(m);
					}
				});
			}
		}
	}

	public static void sendQuery(QueryMessage m) {
		try {
			Socket s = new Socket(m.ENTRY_LOCATION.getHostString(), m.ENTRY_LOCATION.getPort());
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(m);
			Thread.sleep(500);
			out.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		generateRandomQuery("localhost", 40048, new LogSender(null, false, false, true));
	}
}

class QueryReceiver implements Runnable {

	private ServerSocket serverSocket;
	private Thread t;
	private LogSender logger;
	private AtomicInteger numOfQueriesSent;

	public QueryReceiver(LogSender logger, int numOfQueriesSent) {
		this.logger = logger;
		this.numOfQueriesSent = new AtomicInteger(numOfQueriesSent);
		try {
			serverSocket = new ServerSocket(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getPort() {
		return serverSocket.getLocalPort();
	}
	
	public String getHost() {
		return serverSocket.getInetAddress().getHostAddress();
	}

	public int stopAndGetNumOfOpenQueries() {
		t.interrupt();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return numOfQueriesSent.get();
	}

	@Override
	public void run() {
		t = Thread.currentThread();
		while (!t.isInterrupted()) {
			try {
				final Socket s = serverSocket.accept();
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(s.getInputStream());

							Object o = in.readObject();
							if (o instanceof QueryResult) {
								numOfQueriesSent.decrementAndGet();
								logger.write("Client received: " + (((QueryResult) o).getDebuggingInfo()));
							} else {
								logger.write("Client didn't receive QueryResult but some other Object");
							}
							in.close();
							out.close();
							s.close();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}

					}
				});
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
