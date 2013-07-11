package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.TreeSet;
import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.base.FullDuplexMPI.IResponseHandler;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.*;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;
import delaunay.Point;

/**
 * Class representing the cache node
 */
public class CacheNode {

	/**
	 * reference to the actual cache TODO
	 */

	/**
	 * position of this node
	 */
	private Point position;

	/**
	 * reference rest of data TODO
	 */

	/**
	 * neighboring nodes
	 */
	private TreeSet<NodeInfo> neighboringNodes;

	/**
	 * current state
	 */
	private CacheNodeState currentState = CacheNodeState.INITIAL_STATE;

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
			throw new IllegalArgumentException("unresolved host: "
					+ addr.getHostString());
		}

		System.out.println("cache node: connecting to " + addr.getAddress() + ":"
				+ addr.getPort());

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
	public CacheNode(String host, String port) throws IOException {

		this(new InetSocketAddress(host, Integer.parseInt(port)));
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

		MessageType type = message.getMessageType();
		
		System.out.println("cache node: received: " + type);
		
		switch (currentState) {

		case INITIAL_STATE:
			if (type != MessageType.CONFIRM) {
				System.out.println("Error in Protocol");
			}
			currentState = CacheNodeState.AWAITING_DATA;
			break;

		case AWAITING_DATA:

			if (type != MessageType.ADD_TO_GRID) {
				System.out.println("Error in Protocol");
			}
			addNeighboringNodes(((AddToGridMessage) message)
					.getNeighboringNodes());
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
	 * Used to stop an active cache node
	 */
	public void stopNode() {
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
			super(new Socket(address.getAddress(), address.getPort()), System.out);
		}


		@Override
		public IMessage processIncomingMessage(IMessage message) {
			final IMessage response = process(message);
			assert response != null;
			return response;
		}
	}
	
	
	public static void main(String[] args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("please provide the host and the port of the admin node");
		}
		try {
			new CacheNode(args[0], args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
