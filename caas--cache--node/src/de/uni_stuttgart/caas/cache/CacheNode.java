package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.base.FullDuplexMPI.IResponseHandler;
import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.*;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;

/**
 * CacheNode - stand-alone runnable single node of the distributed cache
 * overlay.
 */
public class CacheNode {

	/**
	 * Number of connections per second allowed before the node starts
	 * forwarding queries to neighbors.
	 */
	public static final int MAX_QUERIES_PER_SECOND = 20;

	/**
	 * Fake time for processing a query if the query produces a cache hit
	 * respectively a cache miss, in milliseconds.
	 */
	public static final int QUERY_PROCESSING_TIME_HIT = 1000 / MAX_QUERIES_PER_SECOND;
	public static final int QUERY_PROCESSING_TIME_MISS = QUERY_PROCESSING_TIME_HIT * 10;

	/**
	 * Fake latency introduced into any messages received from neighboring nodes
	 * to simulate a real, physical network instead of loopback. Set to 0 if the
	 * grid is tested on an actual physical network. The units is milliseconds.
	 */
	public static final int FAKE_NEIGHBOR_LATENCY = 2;

	/** Default port to send log messages to on the logging node */
	public static final int DEFAULT_LOG_RECEIVER_PORT = 43215;

	/**
	 * Cache id, which is unique within the grid and assigned during initial
	 * handshake
	 */
	public long id = -1;

	/**
	 * List containing time stamps of the most recent queries to calculate the
	 * load on the cache node. Too old entries are removed during getLoad().
	 */
	private volatile LinkedBlockingQueue<Long> queryProcessTimes;

	/**
	 * Position of this node in the grid
	 */
	private LocationOfNode position;

	/**
	 * List of neighboring nodes, keyed by their address info
	 */
	private HashMap<NodeInfo, NeighborConnector> neighborConnectors;

	/**
	 * Current state - volatile because it is read and written to concurrently
	 */
	private volatile CacheNodeState currentState = CacheNodeState.INITIAL_STATE;

	/**
	 * Holds the connection to the admin node
	 */
	private AdminConnector connectionToAdmin;

	/**
	 * Logger
	 */
	private LogSender logger;

	/**
	 * ServerSocket for the handshake with neighbor nodes to establish
	 * connections.
	 */
	private ServerSocket serverSocket;

	/**
	 * Listener for incoming queries from outside the grid.
	 */
	private QueryListener queryListener;
	private final Object activationMonitor = new Object();

	/**
	 * Construct a new cache node given the address of the admin node
	 * 
	 * @param addr
	 *            address info of the admin node
	 * @throws IOException
	 */
	public CacheNode(InetSocketAddress addr) throws IOException {
		queryProcessTimes = new LinkedBlockingQueue<>();
		logger = new LogSender(new InetSocketAddress("localhost", DEFAULT_LOG_RECEIVER_PORT));

		if (addr.isUnresolved()) {
			throw new IllegalArgumentException("unresolved host: " + addr);
		}

		// bind it to a random port since more than one CacheNode might reside
		// on the same Computer
		serverSocket = new ServerSocket(0);
		new Thread(queryListener = new QueryListener(this, logger)).start();

		final int port = queryListener.getPort();
		logger.write("listening for queries on port" + port);

		// constructing the AdminConnector fires up the CacheNode's lifecycle
		try {
			connectionToAdmin = new AdminConnector(addr);
		} catch (IOException e) {
			throw new IOException("Could not connect to server", e);
		}
	}

	/**
	 * Constructs a new CacheNode given a host and a port of the admin node
	 * 
	 * @param host
	 *            the hostname or ip of the admin node
	 * @param port
	 *            the port, the admin is running on
	 */
	public CacheNode(String host, int port) throws IOException {
		this(new InetSocketAddress(host, port));
	}

	/**
	 * Process an AddToGridMessage adding neighbor info and own location
	 * 
	 * @param message
	 *            the AddToGridMessage
	 */
	private void proccessAddToGridMessage(AddToGridMessage message) {
		this.id = message.id;
		this.position = message.locationOfNode;
		addNeighboringNodes(message.getNeighboringNodes());
	}

	/**
	 * Kills the node immediately, there is no notifications to neighbors so
	 * they have to deal with getting connection failures.
	 */
	public void close() {
		if (currentState == CacheNodeState.DEAD) {
			return;
		}
		logger.write("cache node: shutting down");
		currentState = CacheNodeState.DEAD;
		connectionToAdmin.close();
		// free up the reference
		connectionToAdmin = null;
	}

	/**
	 * Add new neighboring nodes
	 * 
	 * @param collection
	 *            A collection of Information about neighboring nodes
	 */
	private void addNeighboringNodes(Collection<NodeInfo> neighboringNodesSource) {
		// store nodes for now. Later, in onActivate(), we establish connections
		// to all of them
		neighborConnectors = new HashMap<>();
		for (NodeInfo info : neighboringNodesSource) {
			neighborConnectors.put(info, null);
		}
	}

	/**
	 * Determine the number of neighbor connections for which this node has the
	 * server role during the initial neighbor handshake.
	 * 
	 * @return [0, |neighbors|]
	 */
	private int CountServerRoles() {
		int mcount = 0;
		for (NodeInfo info : neighborConnectors.keySet()) {
			assert id != info.ID;
			if (id < info.ID) {
				++mcount;
			}
		}
		return mcount;
	}

	/**
	 * Called upon activation of the cache node. At this point, all neighboring
	 * nodes know about each other and are ready to connect.
	 */
	private ConfirmationMessage onActivate() {
		// concurrent access on this, but do not use ConcurrentHashMap as
		// the output we are supposed to produce is a normal HashMap.
		final HashMap<NodeInfo, NeighborConnector> newMap = new HashMap<>();

		//
		// When connecting neighbors, both nodes try at approximately the same
		// time since one of them needs to be server and the other one client,
		// we arbitrarily make the one with the LOWER ID the server, and the
		// other one the client.
		//
		// Let a node have n neighbors and thus establish n connections. For 0
		// <= m <= n of those it is the server, and for n-m
		// it is the client.
		//
		// All n-m incoming client connections to a node share the same
		// ServerSocket, so we have to map them to their corresponding nodes.
		// The m outgoing connections can be created directly. If they fail to
		// connect, the ServerSocket on the other side is not ready to accept
		// yet, we solve this by retrying (the alternative would be a
		// multiple stage protocol).
		//
		// Important: to avoid deadlocking in a setup where there is a circle
		// within the directed graph induced by the direction in which neighbor
		// connections are established, we have to accept incoming connections
		// asynchronously.
		//

		assert id >= 0;
		final int m = CountServerRoles();

		// epic hack because Java lacks real closures, and I want real closures.
		final ConfirmationMessage[] confirm = new ConfirmationMessage[1];
		confirm[0] = null;

		final CountDownLatch counter = new CountDownLatch(m + 1);
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// accept remaining connections on our ServerSocket, and map
				// them to their source. This is done asynchronously by
				// intercepting the PUBLISH_ID message received by
				// NeighborConnector
				for (int remaining = m; remaining > 0; --remaining) {
					try {
						Socket sock = serverSocket.accept();
						new NeighborConnector(sock) {
							@Override
							protected void onReceiveId(PublishIdMessage message) {
								super.onReceiveId(message);
								final long nid = GetNeighborId();

								// TODO: to improve this, the HM could be keyed
								// on the id and not the NodeInfo
								for (NodeInfo info : neighborConnectors.keySet()) {
									if (info.ID == nid) {
										synchronized (newMap) {
											newMap.put(info, this);
										}
										counter.countDown();
										return;
									}
								}

								assert false;
							}
						};
					} catch (IOException e) {
						e.printStackTrace();
						final String msg = "failed to accept incoming neighbor connection";
						logger.write("cache node: " + msg);
						confirm[0] = new ConfirmationMessage(-6, msg);
					}
				}
				counter.countDown();
			}
		});
		t.start();

		// handle connections where we are the client
		for (NodeInfo info : neighborConnectors.keySet()) {
			assert id != info.ID;
			if (id > info.ID) {
				try {
					Socket sock = new Socket();
					sock.connect(info.ADDRESS_FOR_CACHENODE_NODECONNECTOR);
					// this implicitly sends PUBLISH_ID messages with _this_ id
					newMap.put(info, new NeighborConnector(sock, info.ID));
				} catch (IOException e) {
					e.printStackTrace();
					final String msg = "failed to connect to neighbor @ " + info.ADDRESS_FOR_CACHENODE_NODECONNECTOR;
					logger.write("cache node: " + msg);
					return new ConfirmationMessage(-5, msg);
				}
			}
		}

		try {
			counter.await();
		} catch (InterruptedException e) {
			// should not happen - current thread is never interrupted
			assert false;
			e.printStackTrace();
		}

		// forward async failure
		if (confirm[0] != null) {
			return confirm[0];
		}

		// throw away the temporary connector list and set the new one
		neighborConnectors = newMap;
		logger.write("cache node: established " + neighborConnectors.size() + " neighbor links");
		return new ConfirmationMessage(0, "cache node is now active and connected to neighbors");
	}

	/**
	 * Represents a communication channel with a neighboring node
	 */
	private class NeighborConnector extends FullDuplexMPI {

		/**
		 * ID of neighbor we are talking to. If the connection was established
		 * with us as a client, the id is known from the beginning, otherwise it
		 * is exchanged in the PUBLISH_ID message.
		 */
		private long nid = -1;

		public long GetNeighborId() {
			return nid;
		}

		/**
		 * Construct a new neighbor connector pipe to a neighbor of which have
		 * not yet resolved its id.
		 * 
		 * @param sock
		 *            Fully connected socket
		 * @throws IOException
		 *             Any network errors are forwarded
		 */
		public NeighborConnector(Socket sock) throws IOException {
			super(sock, System.out, true);
		}

		/**
		 * Construct a new neighbor connector pipe for a neighbor whose id is
		 * already resolved. In this case, the NeighborConnection immediately
		 * sends a PUBLISH_ID message to inform the other side of who they are
		 * talking to. This asymmetry is due to the forced client-server nature
		 * of neighbor connections.
		 * 
		 * @param sock
		 *            Fully connected socket
		 * @param nid
		 *            Id of neighbor
		 * @throws IOException
		 *             Any network errors are forwarded
		 */
		public NeighborConnector(Socket sock, long nid) throws IOException {
			super(sock, System.out, true);
			this.nid = nid;

			sendMessageAsync(new PublishIdMessage(id));
		}

		@Override
		public IMessage processIncomingMessage(IMessage message) {
			final MessageType kind = message.getMessageType();
			logger.write("cache node: received: " + kind + " from neighbor connection " + toString());

			if (kind == MessageType.QUERY_MESSAGE) {
				// (hack) extra penalty to simulate latency in a real, physical
				// network
				try {
					Thread.sleep(FAKE_NEIGHBOR_LATENCY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				processQuery((QueryMessage) message);
				return new ConfirmationMessage(1, "message processed");
			} else if (kind == MessageType.PUBLISH_ID) {
				onReceiveId((PublishIdMessage) message);
				return new ConfirmationMessage(0, "id received");
			} else if (kind == MessageType.LOAD_MESSAGE) {
				for (NodeInfo n : neighborConnectors.keySet()) {
					if (n.ID == nid) {
						n.setLoad(((LoadMessage) message).LOAD);
						break;
					}
				}
			} else {
				logger.write("cache node: unexpected neighbor message, reveived message was: " + message.getMessageType());
			}
			return null;
		}

		protected void onReceiveId(PublishIdMessage message) {
			assert nid == -1;
			nid = message.ID;
			assert nid > 0 && id != nid;
		}
	}

	/**
	 * Communication channel to the administrator node, responsible for handling
	 * the cache node's lifetime state transitions.
	 */
	private class AdminConnector extends FullDuplexMPI {

		/**
		 * Construct a new connector class
		 * 
		 * @param address
		 *            the address of the admin node to connect to
		 * @throws IOException
		 *             if the Socket can't be created, pass the error up
		 */
		public AdminConnector(InetSocketAddress address) throws IOException {
			super(new Socket(address.getAddress(), address.getPort()), System.out, true);

			final String localHost = Inet4Address.getLocalHost().getHostAddress();
			final InetSocketAddress neighborAdr = new InetSocketAddress(localHost, serverSocket.getLocalPort());
			final InetSocketAddress queryAdr = new InetSocketAddress(localHost, queryListener.getPort());

			sendMessageAsync(new JoinMessage(neighborAdr, queryAdr), new IResponseHandler() {

				@Override
				public void onResponseReceived(IMessage response) {
					processIncomingMessage(response);
				}

				@Override
				public void onConnectionAborted() {
					logger.write("cache node: connection to admin was closed");
				}
			});
		}

		@Override
		public IMessage processIncomingMessage(IMessage message) {
			assert message != null;
			if (currentState == CacheNodeState.DEAD) {
				return null;
			}

			MessageType type = message.getMessageType();

			// logger.write("cache node: received: " + type);
			switch (currentState) {

			case INITIAL_STATE:
				if (type != MessageType.CONFIRM) {
					logger.write("Error in Protocol");
				}
				ConfirmationMessage confirm = (ConfirmationMessage) message;
				if (confirm.STATUS_CODE != 0) {
					logger.write("cache node: failure, reveived message was: " + confirm.MESSAGE);
					// not so graceful shutdown
					close();
					return null;
				}
				currentState = CacheNodeState.AWAITING_DATA;
				return null;

			case AWAITING_DATA:

				if (type != MessageType.ADD_TO_GRID) {
					logger.write("Error in Protocol");
				}
				proccessAddToGridMessage((AddToGridMessage) message);
				currentState = CacheNodeState.AWAITING_ACTIVATION;
				return new ConfirmationMessage(0, "Added neighbors");

			case AWAITING_ACTIVATION:

				if (type != MessageType.ACTIVATE) {
					logger.write("Error in Protocol");
				}

				ConfirmationMessage response = onActivate();
				if (response.STATUS_CODE == 0) {
					synchronized (activationMonitor) {
						currentState = CacheNodeState.ACTIVE;
						activationMonitor.notifyAll();
					}
				} else {
					// TODO - introduce failure state?
				}
				return response;

			case ACTIVE:
				// TODO what comes here?
				break;

			default:
				logger.write("Error in Protocol");
			}
			return new ConfirmationMessage(-1, "message type unexpected: " + type.toString());
		}
	}

	/**
	 * Processes a query either by fetching the result, or by forwarding/routing
	 * it to a different node.
	 */
	public void processQuery(QueryMessage message) {

		// it is possible that we receive queries before all neighbor
		// connections have been fully established. In this case, we
		// simply synchronize on the completion of this stage.
		//
		// we are guaranteed that this does not cause deadlock as we can never
		// be waiting on establishing a network connection with the node that
		// we got this message from (and on whose message queue we are blocking)
		synchronized (activationMonitor) {
			while (currentState != CacheNodeState.ACTIVE) {
				try {
					activationMonitor.wait();
				} catch (InterruptedException e) {
					assert false; // this thread is not interrupted
					e.printStackTrace();
				}
			}
		}

		assert currentState == CacheNodeState.ACTIVE;

		message.appendToDebuggingInfo(id + "-");
		if (!message.isPropagtionThroughNetworkAllowed()) {
			logger.write("got forwarded message, no further propagation possible");
			processQueryLocally(message);
			return;
		}

		LocationOfNode queryLocation = message.QUERY_LOCATION;
		Entry<NodeInfo, NeighborConnector> closestNodeToQuery = null, tempNode;

		Iterator<Entry<NodeInfo, NeighborConnector>> iterator = neighborConnectors.entrySet().iterator();
		closestNodeToQuery = iterator.next();

		// initialize minimum distance with distance between location of
		// this node and the query.
		double minDistance = calculateDistance(position, queryLocation), tempDistance;
		while (iterator.hasNext()) {
			tempNode = iterator.next();
			tempDistance = calculateDistance(tempNode.getKey(), queryLocation);
			if (tempDistance < minDistance) {
				minDistance = tempDistance;
				closestNodeToQuery = tempNode;
			}
		}

		if (minDistance < calculateDistance(position, queryLocation)) {
			// greedy routing
			closestNodeToQuery.getValue().sendMessageAsync(message);
		} else {
			if (getLoad() > 1) {
				logger.write("forwarding message as local load is too high");
				forwardMessageToNeighbor(message);
			} else {
				processQueryLocally(message);
			}
		}
	}

	/** Forwards a query to a random neighbor and prevents further propagation */
	private void forwardMessageToNeighbor(QueryMessage message) {
		assert message != null;

		int randomNum = (int) (Math.random() * neighborConnectors.size());

		for (NodeInfo n : neighborConnectors.keySet()) {
			if (randomNum == 0) {
				message.stopPropagationOfTheMessage();
				neighborConnectors.get(n).sendMessageAsync(message);
				break;
			}
			--randomNum;
		}
	}

	/**
	 * This method processes the query on this node and ensures a result is
	 * returned to the client. Since we are not really serving the cache, we can
	 * simulate the cache by sleeping for some time
	 * 
	 * @param message
	 */
	private void processQueryLocally(QueryMessage message) {
		assert message != null;

		// remember the message's time of processing so we can calculate the
		// load over a sliding window of recent queries.
		queryProcessTimes.add(System.currentTimeMillis());

		// TODO: simulate real caching behaviour ...
		try {
			Thread.sleep(QUERY_PROCESSING_TIME_HIT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sendQueryResultToClient(message);
	}

	/**
	 * Send a processed query result back to the client where the query
	 * originated. Right now, the response is a dummy.
	 * */
	private void sendQueryResultToClient(final QueryMessage message) {
		assert message != null;

		new Thread(new Runnable() {
			// kick off an extra thread to make sure the cache node is not
			// blocked out. In a benchmark scenario, this is important as we
			// would otherwise be measuring the speed of the measurement device.

			@Override
			public void run() {
				try {
					Socket client = new Socket(message.CLIENT_IP, message.CLIENT_PORT);

					ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
					out.writeObject(new QueryResult(message.getDebuggingInfo(), message.ID));

					// note: Let the GC collect the socket ... closing
					// immediately can cause writeObject to fail
					/*
					 * try { Thread.sleep(500); } catch (InterruptedException e)
					 * { e.printStackTrace(); } out.close(); client.close();
					 */
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Helper method to calculate the square distance between a queryLocation
	 * and a CacheNode center
	 * 
	 * @param node
	 *            the cacheNode
	 * @param queryLocation
	 *            the queryLocation
	 * @return the distance between the node and the location of the query
	 */
	private static final double calculateDistance(NodeInfo node, LocationOfNode queryLocation) {
		return calculateDistance(node.getLocationOfNode(), queryLocation);
	}

	/**
	 * Helper method to calculate the square distance between to points
	 * 
	 * @param a
	 *            first point
	 * @param b
	 *            second point
	 * @return the distance between the two points
	 */
	private static final double calculateDistance(LocationOfNode a, LocationOfNode b) {
		return Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2);
	}

	/**
	 * Calculates the approximate current load of the cache node A higher value
	 * means a higher load.
	 * 
	 * @return a double representing the load
	 */
	public double getLoad() {
		final long currentTime = System.currentTimeMillis();
		while (!queryProcessTimes.isEmpty()) {
			if (currentTime - queryProcessTimes.peek() > 1000) {
				queryProcessTimes.poll();
			} else {
				break;
			}
		}
		return (double) queryProcessTimes.size() / MAX_QUERIES_PER_SECOND;
	}

	/**
	 * Standalone main() for running a cacheNode from the command line
	 * 
	 * @param args
	 *            the ip address of the admin and the port, the admin is
	 *            listening on
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("please provide the host and the port of the admin node");
		}
		try {
			new CacheNode(args[0], Integer.parseInt(args[1]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
