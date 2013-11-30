package de.uni_stuttgart.caas.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.base.QueryLog;
import de.uni_stuttgart.caas.messages.QueryMessage;
import de.uni_stuttgart.caas.messages.QueryResult;

public class QuerySender {

	public static void generateRandomQuery(String ip, int port, LogSender logger) {
		QueryReceiver receiver = new QueryReceiver(logger, 1);
		(new Thread(receiver)).start();
		sendQuery(new QueryMessage(new LocationOfNode(500, 500), ip, receiver.getPort(), new InetSocketAddress(ip, port), UUID.randomUUID().getLeastSignificantBits()), receiver);
	}

	public static void generateUniformlyDistributedQueries(int numOfQueriesPerNode, Map<InetSocketAddress, NodeInfo> nodes, LogSender logger) {
		List<InetSocketAddress> addresses = new ArrayList<>(nodes.size());
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			addresses.add(e.getValue().ADDRESS_FOR_CACHENODE_QUERYLISTENER);
		}
		int counter = 0;
		ExecutorService executor = Executors.newFixedThreadPool(10);
		final QueryReceiver receiver = new QueryReceiver(logger, numOfQueriesPerNode * addresses.size());
		(new Thread(receiver)).start();
		String ip = receiver.getHost();
		int port = receiver.getPort();
		long id = 0;
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			for (int i = 0; i < numOfQueriesPerNode; ++i) {
				final QueryMessage m = new QueryMessage(e.getValue().getLocationOfNode(), ip, port, addresses.get(counter), ++id);
				executor.execute(new Runnable() {

					@Override
					public void run() {
						sendQuery(m, receiver);
					}
				});
				++counter;
			}
			counter = 0;
		}
	}
	
	public static void generateQueriesWithRandomHotspotOneEntryPoint (int numOfQueries, Grid g, LogSender logger) {
		
		int randomNum = (int) (Math.random() * g.getConnectedNodes().size());
		int counter = 0;
		NodeInfo randomNode = null;
		for (Entry<InetSocketAddress, NodeInfo> e : g.getConnectedNodes().entrySet()) {
			if (counter == randomNum) {
				randomNode = e.getValue();
				break;
			}
			++counter;
		}
		ExecutorService executor = Executors.newFixedThreadPool(10);
		final QueryReceiver receiver = new QueryReceiver(logger, numOfQueries);
		(new Thread(receiver)).start();
		
		String ip = receiver.getHost();
		int port = receiver.getPort();
		
		ArrayList<NodeInfo> neighbors = new ArrayList<>(g.getNeighborInfo(randomNode.NODE_ADDRESS));
		neighbors.add(randomNode);
		for (int i = 0; i < numOfQueries; ++i) {
			randomNum = (int) (Math.random() * neighbors.size());
			final QueryMessage m = new QueryMessage(neighbors.get(randomNum).getLocationOfNode(), ip, port, randomNode.ADDRESS_FOR_CACHENODE_QUERYLISTENER, i);
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					sendQuery(m, receiver);
				}
			});
		}
	}
	
	public static void generateUniformlyDistributedQueriesEnteringAtOneLocation(int numOfQueriesPerNode,
			Map<InetSocketAddress, NodeInfo> nodes, LogSender logger) {
	
		ExecutorService executor = Executors.newFixedThreadPool(10);
		final QueryReceiver receiver = new QueryReceiver(logger, numOfQueriesPerNode * nodes.size());
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
		long id = 0;
		for (Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			for (int i = 0; i < numOfQueriesPerNode; ++i) {
				final QueryMessage m = new QueryMessage(e.getValue().getLocationOfNode(), ip, port, randomEntryNode, ++id);
				executor.execute(new Runnable() {

					@Override
					public void run() {
						sendQuery(m, receiver);
					}
				});
			}
		}
	}

	public static void sendQuery(QueryMessage m, QueryReceiver r) {
		try {
			Socket s = new Socket(m.ENTRY_LOCATION.getHostString(), m.ENTRY_LOCATION.getPort());
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			r.waitForQuery(m, new Date());
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
	private ConcurrentHashMap<Long, de.uni_stuttgart.caas.base.QueryLog> queries;
	private BufferedWriter writer;

	public QueryReceiver(LogSender logger, int numOfQueriesSent) {
		this.logger = logger;
		this.numOfQueriesSent = new AtomicInteger(numOfQueriesSent);
		try {
			serverSocket = new ServerSocket(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer = new BufferedWriter(new FileWriter(new File("/tmp/queryStats.csv")));
		} catch (IOException e) {
			try {
				writer = new BufferedWriter(new FileWriter(new File("queryStats.csv")));
			} catch (IOException e1) {
				e.printStackTrace();
				e1.printStackTrace();
			}

		}
		assert writer != null;
		try {
			writer.write("ID,queryTime(ms),hopCount,path\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		queries = new ConcurrentHashMap<>(numOfQueriesSent);
	}
	
	public void waitForQuery(QueryMessage m, Date d) {
		queries.putIfAbsent(m.ID, new de.uni_stuttgart.caas.base.QueryLog(d, m.ID));
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
							Date d = new Date();
							if (o instanceof QueryResult) {
								numOfQueriesSent.decrementAndGet();
								logger.write("Client received answer to a query");
								QueryResult r = (QueryResult) o;
								QueryLog l = queries.get(r.ID);
								l.finishQuery(d, r.getDebuggingInfo().split("-"));
								synchronized (writer) {
									l.writeToFile(writer);	
								}
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
