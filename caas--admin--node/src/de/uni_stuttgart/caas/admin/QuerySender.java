package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.QueryMessage;

public class QuerySender {

	// seconds
	public static final int totalBenchmarkTime = 10;

	public static void generateDistributedQueries(final int numOfQueriesPerNodeAndSecond, final Map<InetSocketAddress, NodeInfo> nodes, final LogSender logger,
			final boolean uniform) {

		// TODO: migrate from sysout to logger - right now sysout is used
		// because logging is purposedly turned off during benchmarks, but we
		// would still like to see benchmark results.

		final CountDownLatch count = new CountDownLatch(nodes.size());
		final int perNode = totalBenchmarkTime * numOfQueriesPerNodeAndSecond;
		final int qcount = perNode * nodes.size();

		final QueryReceiver receiver;
		try {
			receiver = new QueryReceiver(logger, qcount, false);
		} catch (IOException e2) {
			System.out.println("benchmark: failed to construct QueryReceiver");
			e2.printStackTrace();
			return;
		}

		// TODO: hardcoded for now because getHostName() gives incorrect value
		// on the elb, thus causing queries to be not received.
		String tempHost;
		try {
			tempHost = Inet4Address.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
			assert false;
			return;
		}
		
		System.out.println("Query response host: " + tempHost);

		final String localHost = tempHost;
		final int port = receiver.getPort();

		// always place the hotspot at a fixed position in the grid as to avoid
		// another random variable in the game. Do not place it in the center,
		// as this would minimize the number of hops and is thus unfair with
		// respect to the uniform case.
		final LocationOfNode hotspot = new LocationOfNode(Grid.MAX_GRID_INDEX / 5, Grid.MAX_GRID_INDEX / 5);

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
							point = Grid.SampleGaussian(hotspot, 0.18);
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

		final long missing = receiver.join();
		System.out.println("benchmark completed, " + qcount + " queries over " + totalBenchmarkTime + "s and " + nodes.size() + " nodes. " + perNode
				+ " queries per node, " + (uniform ? "uniform" : "gaussian") + " distribution");

		if (missing > 0) {
			System.out.println(missing + " queries never returned");
		}

		// compute mean and median of all timings
		long[] times = receiver.GetTimes();
		Arrays.sort(times);

		// take the upper 95% (P95)
		times = Arrays.copyOfRange(times, 0, (int) (times.length * 0.95));

		assert times.length > 0;
		final long median = times[times.length / 2];

		double mean = 0.0;
		for (long l : times) {
			mean += (double) l;
		}
		mean /= times.length;

		System.out.println("mean: " + mean + "ms");
		System.out.println("median: " + median + "ms");
	}

	private static void sendQuery(QueryMessage m, QueryReceiver r) {
		try {
			Socket s = new Socket(m.ENTRY_LOCATION.getHostString(), m.ENTRY_LOCATION.getPort());
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			r.expectQueryResponse(m, System.nanoTime());
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
}
