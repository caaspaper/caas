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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
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

	// seconds
	public static final int totalBenchmarkTime = 10;

	public static void generateRandomQuery(String ip, int port, LogSender logger) {
		QueryReceiver receiver = new QueryReceiver(logger, 1);
		(new Thread(receiver)).start();
		sendQuery(new QueryMessage(new LocationOfNode(500, 500), ip, receiver.getPort(), new InetSocketAddress(ip, port), UUID.randomUUID()
				.getLeastSignificantBits()), receiver);
	}

	public static void generateUniformlyDistributedQueries(final int numOfQueriesPerNodeAndSecond, Map<InetSocketAddress, NodeInfo> nodes, LogSender logger) {

		// ExecutorCompletionService is nasty, so use a countdown to join on
		// them
		final CountDownLatch count = new CountDownLatch(nodes.size());
		final int perNode = totalBenchmarkTime * numOfQueriesPerNodeAndSecond;
		final int qcount = perNode * nodes.size();

		final QueryReceiver receiver = new QueryReceiver(logger, qcount);
		(new Thread(receiver)).start();
		final String ip = receiver.getHost();
		final int port = receiver.getPort();

		long id = 0;
		for (final Entry<InetSocketAddress, NodeInfo> e : nodes.entrySet()) {
			final InetSocketAddress adr = e.getValue().ADDRESS_FOR_CACHENODE_QUERYLISTENER;

			final long localId = id;
			id += perNode;

			// serialize queries on one cache node to avoid exhausting them
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {

					long sleepError = 0;
					for (int i = 0; i < perNode; ++i) {
						final long time = System.nanoTime();
						
						// generate an uniformly random grid point
						final QueryMessage m = new QueryMessage(Grid.RandomPoint(), ip, port, adr, localId + i);
						sendQuery(m, receiver);

						// attempt to throttle request rate (far from accurate though)
						final long timeEl = sleepError + (System.nanoTime() - time) / 1000000;
						final long wait = 1000 / numOfQueriesPerNodeAndSecond - timeEl;

						if (wait < 0) {
							System.out.println("unable to produce queries this fast");
						}

						final long sleepTime = System.nanoTime();
						try {
							Thread.sleep(Math.max(0, wait));
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						final long timeSlept = (System.nanoTime() - sleepTime) / 1000000;
						sleepError = timeSlept - wait;
					}
					count.countDown();
				}
			});
			t.start();
		}

		try {
			count.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			assert false;
		}

		receiver.join();
		System.out.println("generateUniformlyDistributedQueries completed, " + qcount + " queries over " + totalBenchmarkTime + "s and " + nodes.size()
				+ " nodes. " + perNode + " queries per node");
	}

	public static void generateQueriesWithRandomHotspotOneEntryPoint(int numOfQueries, Grid g, LogSender logger) {

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

	public static void generateUniformlyDistributedQueriesEnteringAtOneLocation(int numOfQueriesPerNode, Map<InetSocketAddress, NodeInfo> nodes,
			LogSender logger) {

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

			// bad idea, makes our benchmarking excessively slow - rather GC
			// collect the socket.
			// Thread.sleep(500);
			// out.close();
			// s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
	}

	public static void main(String[] args) {
		generateRandomQuery("localhost", 40048, new LogSender(null, false, false, true));
	}
}

class QueryReceiver implements Runnable {

	private ServerSocket serverSocket;
	private Thread t;
	private LogSender logger;
	private CountDownLatch syncPoint;
	private ConcurrentHashMap<Long, de.uni_stuttgart.caas.base.QueryLog> queries;
	private BufferedWriter writer;

	public QueryReceiver(LogSender logger, int numOfQueriesSent) {
		this.logger = logger;
		syncPoint = new CountDownLatch(numOfQueriesSent);
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

	public void join() {
		try {
			syncPoint.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		return (int) syncPoint.getCount();
	}

	@Override
	public void run() {
		t = Thread.currentThread();
		while (!t.isInterrupted()) {
			try {
				Socket s = serverSocket.accept();

				// note: avoid the overhead of creating a thread here as the
				// actual work
				// we do here is less than 1/1000 of the cost of creating a
				// thread.

				ObjectInputStream in = new ObjectInputStream(s.getInputStream());
				Object o = in.readObject();
				Date d = new Date();
				if (o instanceof QueryResult) {

					logger.write("Client received answer to a query");
					QueryResult r = (QueryResult) o;
					QueryLog l = queries.get(r.ID);
					l.finishQuery(d, r.getDebuggingInfo().split("-"));
					l.writeToFile(writer);

					syncPoint.countDown();
					long l1 = syncPoint.getCount();
					if(l1 % 100 == 0 || l1 < 50) {
						System.out.println(syncPoint.getCount());
					}
				} else {
					logger.write("Client didn't receive QueryResult but some other Object");
				}
				in.close();
				s.close();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
