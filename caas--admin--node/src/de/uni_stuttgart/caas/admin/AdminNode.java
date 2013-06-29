package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import de.uni_stuttgart.caas.admin.JoinRequestManager.JoinRequest;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.ActivateNodeMessage;
import de.uni_stuttgart.caas.messages.AddToGridMessage;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;
import delaunay.Knuth;
import delaunay.Point;
import delaunay.Segment;

/**
 * AdminNode
 * 
 * Version: 1.0
 * 
 * Run multithreaded server and obey the following protocol:
 * 
 * INITIAL_SIGNUP_PHASE, grid initial capacity not exceeded and node didn't sign
 * up before FAIL otherwise
 * 
 * 
 */
public class AdminNode {

	/** Current state of the admin node */
	private AdminNodeState state;

	public final static int DEFAULT_INITIAL_CAPACITY = 64;
	public final int INITIAL_CAPACITY;

	public final static int DEFAULT_PORT_NUMBER = 5007;
	public final int PORT_NUMBER;

	/** List of nodes requesting to join grid */
	private JoinRequestManager joinRequests;

	/** */
	private Grid grid = null;

	/**
	 * 
	 * @return state
	 */
	public AdminNodeState getState() {
		return state;
	}

	/**
	 * Creates Administrative Node
	 * 
	 * //first instance where Alex has Java-envy. ^_^ Alex.state = denial;
	 * 
	 * @throws Exception
	 */
	public AdminNode() {

		this(DEFAULT_PORT_NUMBER, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates Administrative Node
	 * 
	 * @param initialCapacity
	 * @throws Exception
	 */
	public AdminNode(int portNumber, int initialCapacity) {
		state = AdminNodeState.INITIAL_SIGNUP_PHASE;

		if (portNumber < 1024 || portNumber > 49151) {
			throw new IllegalArgumentException();
		} else {
			PORT_NUMBER = portNumber;
		}

		if (initialCapacity < 0) {
			throw new IllegalArgumentException();
		} else {
			INITIAL_CAPACITY = initialCapacity;
		}

		joinRequests = new JoinRequestManager(initialCapacity);

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
		} catch (IOException e) {
			System.out.println("Could not listen on PORT_NUMBER");
			System.exit(-1);
		}

		assert serverSocket != null;

		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				NodeConnector nc = new NodeConnector(clientSocket);
				Thread t = new Thread(nc);
				t.start();
			} catch (IOException e) {
				System.out.println("Accept failed: PORT_NUMBER");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * 
	 *
	 */
	private class NodeConnector implements Runnable {
		private final Socket clientSocket;
		private final InetSocketAddress clientAddress;

		/**
		 * 
		 * @param cS
		 */
		public NodeConnector(Socket cS) {
			this.clientSocket = cS;
			
			assert cS.getRemoteSocketAddress() instanceof InetSocketAddress;
			this.clientAddress = (InetSocketAddress) cS
					.getRemoteSocketAddress();
		}

		/**
		 * Threadstarter
		 */
		public void run() {
			ObjectInputStream in = null;
			ObjectOutputStream out = null;
			
			try {
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new ObjectOutputStream(clientSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("");
				e.printStackTrace();
			}
			
			while (true) {
				IMessage message = null;
				try { // message, i.e. object, passed over connection
					message = (IMessage) in.readObject(); // casts to one of the
															// types enumerated
															// in IMessage
				} catch (ClassNotFoundException e) {
					System.out.println("Object class not found");
					e.printStackTrace();
				} catch (ClassCastException e) {
					System.out.println("Cast of obj to type IMessage failed");
					System.exit(-1);
				} catch (IOException e) {
					System.out.println("Read failed");
					e.printStackTrace();
				}
				
				IMessage response = process(message, clientAddress);
				if (response != null) { // TODO finish
					try {
						out.writeObject(response);
					} catch (IOException e) {
						System.out.println("Write to client failed");
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param message
	 * @param clientAddress
	 * @return
	 * @note
	 */
	private IMessage process(IMessage message, InetSocketAddress clientAddress) {
		IMessage response = null;
		switch (message.GetMessage()) {
		case CONFIRM:
			break;

		case JOIN:
			JoinRequest jr = new JoinRequest(clientAddress);
			assert jr != null;
			response = respondToJoinRequest(jr);
			return response;

		default:
			break;
		}

		return null;
	}

	/**
	 * @param request
	 *            The individual join request sent by node to admin node. A join
	 *            request contains the IP + Port info of the requesting node
	 * 
	 * @return cm Returns confirmation message if the node was successfully
	 *         added to join list.
	 * 
	 * @note sc refers to status code of join request success. If node was added
	 *       to JoinRequest List, then sc = 0. Otherwise, sc is non-null.
	 */
	private IMessage respondToJoinRequest(JoinRequest request) {
		int sc = 0;
		String m = null; // optional, may stay null
		try {
			joinRequests.TryAdd(request);
		} catch (IllegalStateException e) {
			sc = -1;
		}
		ConfirmationMessage cm = new ConfirmationMessage(sc, m);
		return cm;
	}

	/**
	 * Generates a new AddToGridMessage
	 * 
	 * @return An AddToGridMessage containing information about neighboring
	 *         nodes
	 * 
	 */
	private IMessage addNodeToGrid(InetSocketAddress addressOfNode) {

		Collection<NodeInfo> infoOnNeighbors = grid
				.getNeighborInfo(addressOfNode);
		return new AddToGridMessage(infoOnNeighbors);
	}

	/**
	 * Generate a new activation message
	 * 
	 * @return a new activation message
	 */
	private IMessage activateNode() {
		return new ActivateNodeMessage();
	}

	/**
	 * TODO close connection
	 */
}
