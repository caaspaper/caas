package de.uni_stuttgart.caas.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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

	public static void generateDistributedQueries(final int numOfQueriesPerNodeAndSecond, Map<InetSocketAddress, NodeInfo> nodes, LogSender logger,
			final boolean uniform) {

		final CountDownLatch count = new CountDownLatch(nodes.size());
		final int perNode = totalBenchmarkTime * numOfQueriesPerNodeAndSecond;
		final int qcount = perNode * nodes.size();

		final QueryReceiver receiver = new QueryReceiver(logger, qcount);
		(new Thread(receiver)).start();
		
		String tempHost = "";
		try {
			tempHost = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
			assert false;
			return;
		};
		
		final String localHost = tempHost;
		final int port = receiver.getPort();

		// always place the hotspot in the center of the grid as to avoid
		// another random variable in the game.
		final LocationOfNode hotspot = Grid.CenterPoint();

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

						LocationOfNode point;
						if (uniform) {
							point = Grid.RandomPoint();
						} else {
							// in accordance with carlos' paper, do a gauss
							// distribution with a standard deviation of 0.3 the
							// grid size
							// TODO: is a poisson distribution a better model?
							point = Grid.SampleGaussian(hotspot, 0.2);
						}

						// generate an uniformly random grid point
						final QueryMessage m = new QueryMessage(point, localHost, port, adr, localId + i);
						sendQuery(m, receiver);

						// attempt to throttle request rate (far from accurate
						// though)
						final long timeEl = sleepError + (System.nanoTime() - time) / 1000000;
						final long wait = 1000 / numOfQueriesPerNodeAndSecond - timeEl;

						if (wait < 0) {
							System.out.println("unable to produce queries this fast");
						}

						final long sleepTime = System.nanoTime();
						try {
							Thread.sleep(Math.max(0, wait));
						} catch (InterruptedException e) {
							e.printStackTrace();
							assert false;
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
		System.out.println("benchmark completed, " + qcount + " queries over " + totalBenchmarkTime + "s and " + nodes.size()
				+ " nodes. " + perNode + " queries per node, " + (uniform ? "uniform" : "gaussian") + " distribution");
		
		// compute mean and median of all timings
		long[] times = receiver.GetTimes();
		Arrays.sort(times);
		
		// take the upper 95% (P95)
		times = Arrays.copyOfRange(times, 0, (int)(times.length * 0.95) );
		
		assert times.length > 0;
		final long median = times[times.length/2];
		
		double mean = 0.0;
		for(long l: times) {
			mean += (double)l;
		}
		mean /= times.length;
		
		System.out.println("mean: " + mean + "ms");
		System.out.println("median: " + median + "ms");
	}


	public static void sendQuery(QueryMessage m, QueryReceiver r) {
		try {
			Socket s = new Socket(m.ENTRY_LOCATION.getHostString(), m.ENTRY_LOCATION.getPort());
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			r.waitForQuery(m, System.nanoTime());
			out.writeObject(m);

			// bad idea, makes our benchmarking excessively slow - rather GC
			// collect the socket.s
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
	
	private final long times[];

	public QueryReceiver(LogSender logger, int numOfQueriesSent) {
		times = new long[numOfQueriesSent];
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

	public void waitForQuery(QueryMessage m, long time) {
		queries.putIfAbsent(m.ID, new de.uni_stuttgart.caas.base.QueryLog(time, m.ID));
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
	
	public long[] GetTimes() {
		return times;
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
						ObjectInputStream in;
						try {
							in = new ObjectInputStream(s.getInputStream());
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}
						Object o;
						try {
							o = in.readObject();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
							return;
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}
						long time = System.nanoTime();
						if (o instanceof QueryResult) {

							logger.write("Client received answer to a query");
							QueryResult r = (QueryResult) o;
							QueryLog l = queries.get(r.ID);
							l.finishQuery(time, r.getDebuggingInfo().split("-"));
							
							synchronized(syncPoint) { // TODO: too long.
								l.writeToFile(writer);
								
								
								long l1 = syncPoint.getCount();
								times[(int) (l1 - 1)] = l.getTransitTime() / 1000000;
								
								syncPoint.countDown();
								
								if (l1 % 100 == 0 || l1 < 50) {
									System.out.println(l1);
								}
							}
						} else {
							logger.write("Client didn't receive QueryResult but some other Object");
						}
						try {
							in.close();
							s.close();
						} catch (IOException e) {
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
