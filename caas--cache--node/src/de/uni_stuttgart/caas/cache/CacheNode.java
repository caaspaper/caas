package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.base.FullDuplexMPI.IResponseHandler;
import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.*;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;

/**
 * Class representing the cache node
 */
public class CacheNode {
	
	/**
	 * Number of connections per second allowed before the node starts tasking neighbors with the query
	 */
	public final int MAX_QUERIES_PER_SECOND = 20;
	

	/**
	 * reference to the actual cache TODO
	 */
	
	/**
	 * List containing timestamps of the last requests
	 * to calculate the load on the cache node
	 */
	private volatile LinkedBlockingQueue<Long> queryProcessTimes;

	/**
	 * position of this node
	 */
	private LocationOfNode position;

	/**
	 * reference rest of data TODO
	 */

	/**
	 * neighboring nodes
	 */
	private TreeSet<NodeInfo> neighboringNodes;

	/**
	 * current state - volatile because it is read and written to concurrently.
	 */
	private volatile CacheNodeState currentState = CacheNodeState.INITIAL_STATE;

	/**
	 * Holds the connection to the admin node
	 */
	private AdminConnector connectionToAdmin;

	/**
	 * Construct a new cache node given the address of the admin node
	 * 
	 * @param addr
	 *            the address of the admin node
	 * @throws IOException
	 */
	public CacheNode(InetSocketAddress addr) throws IOException {
		if (addr.isUnresolved()) {
			throw new IllegalArgumentException("unresolved host: " + addr);
		}

		// System.out.println("cache node: connecting to " + addr.getAddress() +
		// ":" + addr.getPort());

		try {
			connectionToAdmin = new AdminConnector(addr);
		} catch (IOException e) {
			throw new IOException("Could not connect to server", e);
		}

		connectionToAdmin.sendMessageAsync(new JoinMessage(), new IResponseHandler() {

			@Override
			public void onResponseReceived(IMessage response) {
				process(response);
			}

			@Override
			public void onConnectionAborted() {
				System.out.println("cache node: connection to admin was closed");
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
	 * initialize node and generate JOIN-Message
	 * 
	 * @return The newly generated JOIN-Message
	 */
	public IMessage initializeNode() {

		currentState = CacheNodeState.AWAITING_DATA;
		return new JoinMessage();
	}

	/**
	 * Add a new neighboring node
	 * 
	 * @param newNode
	 *            The new node to add
	 */
	private void addNeighbor(NodeInfo newNode) {

		if (!neighboringNodes.contains(newNode)) {
			neighboringNodes.add(newNode);
		}
	}

	/**
	 * Remove a neighboring node
	 * 
	 * @param node
	 *            The node to remove
	 */
	private void removeNeighbor(NodeInfo node) {
		neighboringNodes.remove(node);
	}

	public IMessage process(IMessage message) {
		if (currentState == CacheNodeState.DEAD) {
			return null;
		}

		MessageType type = message.getMessageType();

		System.out.println("cache node: received: " + type);

		switch (currentState) {

		case INITIAL_STATE:
			if (type != MessageType.CONFIRM) {
				System.out.println("Error in Protocol");
			}
			ConfirmationMessage confirm = (ConfirmationMessage) message;
			if (confirm.STATUS_CODE != 0) {
				System.out.println("cache node: failure, reveived message was: " + confirm.MESSAGE);
				// not so graceful shutdown
				close();
				return null;
			}
			currentState = CacheNodeState.AWAITING_DATA;
			break;

		case AWAITING_DATA:

			if (type != MessageType.ADD_TO_GRID) {
				System.out.println("Error in Protocol");
			}
			proccessAddToGridMessage((AddToGridMessage) message);
			currentState = CacheNodeState.AWAITING_ACTIVATION;
			return new ConfirmationMessage(0, "Added neighbors");

		case AWAITING_ACTIVATION:

			if (type != MessageType.ACTIVATE) {
				System.out.println("Error in Protocol");
			}
			currentState = CacheNodeState.ACTIVE;
			return new ConfirmationMessage(0, "cache node is now active");

		case ACTIVE:
			// TODO what comes here?
			break;

		default:
			System.out.println("Error in Protocol");
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
	private void addNeighboringNodes(Collection<NodeInfo> neighboringNodes) {

		neighboringNodes.addAll(neighboringNodes);
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
	 * Method that processes the queries it receives and forwards them to
	 * the right neighbor
	 */
	public void processQuery(QueryMessage message) {

		assert currentState == CacheNodeState.ACTIVE;
		
		LocationOfNode queryLocation = message.QUERY_LOCATION;
		NodeInfo closestNodeToQuery = null, tempNode;
		
		Iterator<NodeInfo> iterator = neighboringNodes.iterator();
		closestNodeToQuery = iterator.next();
		
		// initialize minimum distance with distance between location of
		// this node and the query.
		double minDistance = calculateDistance(position, queryLocation), tempDistance;
		while (iterator.hasNext()) {
			tempNode = iterator.next();
			tempDistance = calculateDistance(tempNode, null);
			if (tempDistance < minDistance) {
				minDistance = tempDistance;
				closestNodeToQuery = tempNode;
			}
		}
		
		if (minDistance < calculateDistance(position, queryLocation)) {
			// TODO forward query to closestNodeToQuery
		} else {

			/*
			 * TODO process node locally if the current load is to high,
			 * send query to a close neighbor
			 */
			if (getLoad() > 1) {
				
			} else {
				processQueryLocally(message);
			}
		}
	}
	
	/**
	 * Processes an incoming query from outside the network and wraps it's information into a QueryMessage that is used internally
	 * @param query the actual query
	 */
	public void processIncomingQueryToAdaptItToNetwork(Object query) {
		
		
		LocationOfNode randomLocation = new LocationOfNode((int) Math.random() * 2000000000, (int) Math.random()* 2000000000);
		QueryMessage newMessage  = new QueryMessage(randomLocation, new InetSocketAddress(connectionToAdmin.getLocalAddress(), 5000), -1);
		
		processQuery(newMessage);
	}
	
	
	/**
	 * This method processes the query on this node.
	 * Since we are not really serving the cache,
	 * we can simulate the cache by sleeping for some time
	 * TODO DO NOT SLEEP REPLACE WITH GETTING THE DATA FROM THE CACHE
	 * @param message 
	 */
	private void processQueryLocally(QueryMessage message) {
		
		// remember the message's time of processing so we can calculate the load
		queryProcessTimes.add(System.currentTimeMillis());
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
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
	 * Calculates the load of the cache node
	 * A higher value means a higher load
	 * 
	 * NOTE: when the calculation changes, processQuery() has to be changed accordingly!!!
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
	 *            the ip address of the admin and the port, the admin is listening on
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
