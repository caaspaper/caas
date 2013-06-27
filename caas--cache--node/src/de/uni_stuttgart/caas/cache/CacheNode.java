package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.TreeSet;
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
	private Thread connectionToAdmin = null;
	
	/**
	 * Construct a new cache node given the address of the admin node
	 * 
	 * @param addr
	 *            the address of the admin node
	 */
	public CacheNode(InetSocketAddress addr) {

		try {
			connectionToAdmin = new Thread(new AdminConnector(addr));
		} catch (IOException e) {
			System.out.println("Could not connect to server");
		}
		connectionToAdmin.start();
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

		MessageType type = message.GetMessage();

		switch (currentState) {

		case INITIAL_STATE:
			if (type != MessageType.CONFIRM) {
				System.out.println("Error in Protocol");
				System.exit(-1);
			}
			currentState = CacheNodeState.AWAITING_DATA;
			break;

		case AWAITING_DATA:

			if (type != MessageType.ADD_TO_GRID) {
				System.out.println("Error in Protocol");
				System.exit(-1);
			}
			addNeighboringNodes(((AddToGridMessage) message)
					.getNeighboringNodes());
			currentState = CacheNodeState.AWAITING_ACTIVATION;
			return new ConfirmationMessage(0, "Added neighbors");

		case AWAITING_ACTIVATION:

			if (type != MessageType.ACTIVATE) {
				System.out.println("Error in Protocol");
				System.exit(-1);
			}
			currentState = CacheNodeState.ACTIVE;
			return new ConfirmationMessage(0, "cache node is now active");

		case ACTIVE:
			// TODO what comes here?
			break;

		default:
			System.out.println("Error in Protocol");
			System.exit(-1);
		}

		return null;
	}

	/**
	 * Used to stop an active cache node
	 */
	public void stopNode() {
		connectionToAdmin.interrupt();
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
	private class AdminConnector implements Runnable {

		/**
		 * Reference to the serverSocket
		 */
		private final Socket serverSocket;

		/**
		 * Construct a new connector class
		 * 
		 * @param address
		 *            the address of the admin node to connect to
		 * @throws IOException
		 *             if the Socket can't be created, pass the error up
		 */
		public AdminConnector(InetSocketAddress address) throws IOException {
			
			serverSocket = new Socket(address.getAddress(), address.getPort());
		}

		@Override
		public void run() {

			ObjectInputStream in = null;
			ObjectOutputStream out = null;

			try {
				in = new ObjectInputStream(serverSocket.getInputStream());
				out = new ObjectOutputStream(serverSocket.getOutputStream());
			} catch (IOException e) {
				System.out
						.println("Could not initiate input and output with server");
				System.exit(-1);
			}
			
			try {
				
				// initiate connection by sending a join message
				out.writeObject(new JoinMessage());

				// read a message from the admin and respond to it
				while (true) {
					IMessage message = null;
					try {
						message = (IMessage) in.readObject();
					} catch (ClassNotFoundException e) {
						System.out.println("received unknown class!");
						System.exit(-1);
					} catch (ClassCastException e) {
						System.out.println("error while casting to IMessage");
						System.exit(-1);
					}
					IMessage responce = process(message);
					
					// as not to Confirm confirm messages
					if (responce != null) {
						out.writeObject(responce);
					}
				}

			} catch (IOException e) {
				System.out.println("Error while sending/ receiving data");
				System.exit(-1);
			}
		}

	}

}
