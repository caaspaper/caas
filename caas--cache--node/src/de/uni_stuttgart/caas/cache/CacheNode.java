package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.io.ObjectOutputStream;
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
 * Class representing the cache node
 */
public class CacheNode {

	/**
	 * Number of connections per second allowed before the node starts tasking
	 * neighbors with the query
	 */
	public final int MAX_QUERIES_PER_SECOND = 20;

	public static final int DEFAULT_LOG_RECEIVER_PORT = 43215;

	/**
	 * Cache id, which is unique within the grid and assigned during init
	 * handshake
	 */
	public long id = -1;

	/**
	 * List containing timestamps of the last requests to calculate the load on
	 * the cache node
	 */
	private volatile LinkedBlockingQueue<Long> queryProcessTimes;

	/**
	 * position of this node
	 */
	private LocationOfNode position;

	/**
	 * neighboring nodes
	 */
	private HashMap<NodeInfo, NeighborConnector> neighborConnectors;

	/**
	 * current state - volatile because it is read and written to concurrently.
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
	 * ServerSocket for connections with other CacheNodes
	 */
	private ServerSocket serverSocket;

	/**
	 * listens for new querys from clients
	 */
	private QueryListener queryListener;

	private final Object activationMonitor = new Object();

	/**
	 * Construct a new cache node given the address of the admin node
	 * 
	 * @param addr
	 *            the address of the admin node
	 * @throws IOException
	 */
	public CacheNode(InetSocketAddress addr) throws IOException {
		queryProcessTimes = new LinkedBlockingQueue<>();
		logger = new LogSender(new InetSocketAddress("localhost", DEFAULT_LOG_RECEIVER_PORT));

		if (addr.isUnresolved()) {
			throw new IllegalArgumentException("unresolved host: " + addr);
		}

		logger.write("Cache node started");

		// bind it to a random port since more than one CacheNode might reside
		// on the same Computer
		serverSocket = new ServerSocket(0);
		queryListener = new QueryListener(this, logger);
		(new Thread(queryListener)).start();
		int port = queryListener.getPort();
		logger.write("listening for queries on port" + port);
		try {
			connectionToAdmin = new AdminConnector(addr);
		} catch (IOException e) {
			throw new IOException("Could not connect to server", e);
		}

		connectionToAdmin.sendMessageAsync(new JoinMessage(new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort()),
				new InetSocketAddress(serverSocket.getInetAddress(), port)), new IResponseHandler() {

			@Override
			public void onResponseReceived(IMessage response) {
				process(response);
			}

			@Override
			public void onConnectionAborted() {
				logger.write("cache node: connection to admin was closed");
			}
		});
	}

	/**
	 * Constructs a new CacheNode given a host and a port
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
	 * Add a new neighboring node
	 * 
	 * @param newNode
	 *            The new node to add
	 */
	private void addNeighbor(NodeInfo newNode) {

		if (!neighborConnectors.containsKey(newNode)) {
			neighborConnectors.put(newNode, null);
			// TODO: establish a connection with that node
		}
	}

	/**
	 * Remove a neighboring node
	 * 
	 * @param node
	 *            The node to remove
	 */
	private void removeNeighbor(NodeInfo node) {
		neighborConnectors.remove(node);
		// TODO: kill connection with that node
	}

	public IMessage process(IMessage message) {
		if (currentState == CacheNodeState.DEAD) {
			return null;
		}

		MessageType type = message.getMessageType();

		logger.write("cache node: received: " + type);

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
	 * Used to stop an active cache node
	 */
	public void close() {
		if (currentState == CacheNodeState.DEAD) {
			return;
		}
		System.out.println("cache node: shutting down");
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
		// to them
		neighborConnectors = new HashMap<>();
		for (NodeInfo info : neighboringNodesSource) {
			neighborConnectors.put(info, null);
		}
	}

	/**
	 * Determine the number of neighbor connections for which this node has the
	 * server role.
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
				processQuery((QueryMessage) message);
				return new ConfirmationMessage(1, "message processed");
			} else if (kind == MessageType.PUBLISH_ID) {
				onReceiveId((PublishIdMessage) message);
				return new ConfirmationMessage(0, "id received");
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
	 * This class is responsible for the interaction with the adminNode
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
		}

		@Override
		public IMessage processIncomingMessage(IMessage message) {
			final IMessage response = process(message);
			assert response != null;
			return response;
		}

	}

	/**
	 * Method that processes the queries it receives and forwards them to the
	 * right neighbor
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

		logger.write("processing Query");

		assert currentState == CacheNodeState.ACTIVE;

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
			closestNodeToQuery.getValue().sendMessageAsync(message);
		} else {

			/*
			 * TODO process node locally if the current load is to high, send
			 * query to a close neighbor
			 */
			if (getLoad() > 1) {
				logger.write("******Load exeeded allowed value*******");
			} else {
				processQueryLocally(message);
			}
		}
	}

	/**
	 * Processes an incoming query from outside the network and wraps it's
	 * information into a QueryMessage that is used internally
	 * 
	 * @param query
	 *            the actual query
	 * @param ip
	 *            , port The address to send the response to
	 */
	public void processIncomingQueryToAdaptItToNetwork(Object query, String ip, int port) {

		LocationOfNode randomLocation = new LocationOfNode((int) Math.random() * 2000000000, (int) Math.random() * 2000000000);
		QueryMessage newMessage = new QueryMessage(randomLocation, ip, port);
		logger.write("received new query from " + ip + ":" + port + " for " + randomLocation);
		processQuery(newMessage);
	}

	/**
	 * This method processes the query on this node. Since we are not really
	 * serving the cache, we can simulate the cache by sleeping for some time
	 * TODO DO NOT SLEEP REPLACE WITH GETTING THE DATA FROM THE CACHE
	 * 
	 * @param message
	 */
	private void processQueryLocally(QueryMessage message) {

		// remember the message's time of processing so we can calculate the
		// load
		queryProcessTimes.add(System.currentTimeMillis());

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		message.appendToDebuggingInfo("Node " + position + ": processed Locally");

		sendResultToClient(message);
	}

	private void sendResultToClient(QueryMessage message) {

		try {
			Socket client = new Socket(message.CLIENT_IP, message.CLIENT_PORT);

			ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

			out.writeObject(new QueryResult(message.getDebuggingInfo()));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			out.close();
			client.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to calculate the distance between a queryLocation and a
	 * CacheNode center
	 * 
	 * @param node
	 *            the cacheNode
	 * @param queryLocation
	 *            the queryLocation
	 * @return the distance between the node and the location of the query
	 */
	private double calculateDistance(NodeInfo node, LocationOfNode queryLocation) {

		return calculateDistance(node.getLocationOfNode(), queryLocation);
	}

	/**
	 * Helper method to calculate the distance between to points
	 * 
	 * @param a
	 *            first point
	 * @param b
	 *            second point
	 * @return the distance between the two points
	 */
	private double calculateDistance(LocationOfNode a, LocationOfNode b) {

		return Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2);
	}

	/**
	 * Calculates the load of the cache node A higher value means a higher load
	 * 
	 * NOTE: when the calculation changes, processQuery() has to be changed
	 * accordingly!!!
	 * 
	 * @return a double representing the load
	 */
	public double getLoad() {

		long currentTime = System.currentTimeMillis();

		while (!queryProcessTimes.isEmpty()) {
			if (currentTime - queryProcessTimes.peek() > 1000) {
				queryProcessTimes.poll();
			} else {
				break;
			}
		}
		return queryProcessTimes.size() / MAX_QUERIES_PER_SECOND;
	}

	/**
	 * For starting the cacheNode from the command line
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
