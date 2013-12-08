package de.uni_stuttgart.caas.admin;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.base.QueryLog;
import de.uni_stuttgart.caas.messages.QueryMessage;
import de.uni_stuttgart.caas.messages.QueryResult;

public final class QueryReceiver {

	private final ServerSocket serverSocket;
	private final LogSender logger;
	private final CountDownLatch syncPoint;
	private final ConcurrentHashMap<Long, de.uni_stuttgart.caas.base.QueryLog> queries;
	private final BufferedWriter writer;
	private volatile boolean stop = false;

	private long times[];
	private final Thread thread;

	public QueryReceiver(LogSender _logger, int numOfQueriesSent, boolean enableLogging) throws IOException {
		queries = new ConcurrentHashMap<>(numOfQueriesSent);
		times = new long[numOfQueriesSent];
		logger = _logger;
		syncPoint = new CountDownLatch(numOfQueriesSent);

		// extra-long backlog to make sure we can process incoming queries
		serverSocket = new ServerSocket(0, 1024);

		BufferedWriter buff = null;

		if (enableLogging) {
			try {
				buff = new BufferedWriter(new FileWriter(new File("/tmp/queryStats.csv")));
			} catch (IOException e) {
				try {
					buff = new BufferedWriter(new FileWriter(new File("queryStats.csv")));
				} catch (IOException e1) {
					e.printStackTrace();
					throw e1;
				}

			}

			try {
				buff.write("ID,queryTime(ms),hopCount,path\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		writer = buff;

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				acceptor();
			}
		});
		thread.start();
	}

	/**
	 * Await reception of all query responses previously registered using
	 * expectQueryResponse().
	 * 
	 * Additionally, a 10s timeout is used to cut out any failed queries. After
	 * join() returns, getTimes() can be used to get an array of millisecond
	 * latencies for every query for which a response was received.
	 * 
	 * @return Number of queries for which a response was not received within
	 *         the time.
	 */
	public long join() {
		// if some queries fail, we cut them off with a timeout
		try {
			syncPoint.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long missing = 0;
		synchronized (syncPoint) {
			missing = syncPoint.getCount();
			times = Arrays.copyOf(times, (int) (times.length - missing));

			thread.interrupt();
			stop = true;
		}

		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return missing;
	}

	/** Register a query with the QueryReceiver to enable tracking for it */
	public void expectQueryResponse(QueryMessage m, long time) {
		queries.putIfAbsent(m.ID, new QueryLog(time, m.ID));
	}

	/** Get the local port that we listen on */
	public int getPort() {
		return serverSocket.getLocalPort();
	}

	public String getHost() {
		return serverSocket.getInetAddress().getHostAddress();
	}

	/** @note Only valid after join() returns */
	public long[] GetTimes() {
		assert stop;
		return times;
	}

	private void acceptor() {

		while (!thread.isInterrupted()) {
			try {
				final Socket s = serverSocket.accept();
				new Thread(new Runnable() {

					@Override
					public void run() {
						ObjectInputStream in;
						try {
							in = new ObjectInputStream(s.getInputStream());
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}

						while (true) {
							Object o;

							try {
								o = in.readObject();
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
								break;
							} catch (EOFException e) {
								// expected
								break;
							} catch (IOException e) {
								e.printStackTrace();
								break;
							}
							long time = System.nanoTime();
							if (!(o instanceof QueryResult)) {
								logger.write("didn't receive QueryResult object, do not now what to do");
								continue;
							}

							logger.write("received answer to a query");
							QueryResult r = (QueryResult) o;
							QueryLog l = queries.get(r.ID);
							l.finishQuery(time, r.getDebuggingInfo().split("-"));

							// TODO: lock time is potentially too long.
							synchronized (syncPoint) {
								// if the stop event happened, no further
								// times
								// may be recorded
								if (stop) {
									return;
								}

								if (writer != null) {
									l.writeToFile(writer);
								}
								long l1 = syncPoint.getCount();
								times[(int) (l1 - 1)] = l.getTransitTime() / 1000000;

								if (l1 % 100 == 0 || l1 < 50) {
									System.out.println(l1);
								}
								syncPoint.countDown();
							}
						}

						try {
							in.close();
							s.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}