package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import de.uni_stuttgart.caas.admin.JoinRequestManager.JoinRequest;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;

/**
 * Run multithreaded server and obey the following protocol:
 * 
 * INITIAL_SIGNUP_PHASE, grid initial capacity not exceeded and node didn't sign
 * up before FAIL otherwise
 * 
 * 
 */
public class AdminNode {

	private final int INITIAL_CAPACITY;
	private final int PORT_NUMBER = 5007;

	/** Current state of the admin node */
	private AdminNodeState state;

	/**
	 * Used during INITIAL_SIGNUP_PHASE to keep track of all nodes who requested
	 * to JOIN the grid.
	 */
	private JoinRequestManager joinRequests;

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
	 * @param initialCapacity
	 */
	public AdminNode(int initialCapacity) {
		state = AdminNodeState.INITIAL_SIGNUP_PHASE;

		INITIAL_CAPACITY = initialCapacity;
		assert initialCapacity > 0;

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
				NodeConnector nc = new NodeConnector(clientSocket); // declared
																	// this way
																	// bc later
																	// we access
																	// the
																	// nc.clientAddress
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
		 *  Threadstarter
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
	 * @return cm Returns confirmation message if the node was successfully
	 *         joined.
	 *         
	 * @note sc refers to status code of join request success. 
	 * 		If node was added to JoinRequest List, then sc = 0. 
	 * 			Otherwise, sc is non-null.
	 */
	private IMessage respondToJoinRequest(JoinRequest request) {
		int sc = 0;
		String m = null; //optional, may stay null
		try {
			joinRequests.TryAdd(request);
		} catch (IllegalStateException e) {
			sc = -1;
		}
		ConfirmationMessage cm = new ConfirmationMessage(sc, m);
		return cm;
	}

	/**
	 * TODO
	 * @return 
	 * 
	 */
	private IMessage addNodeToGrid() {
		return null;
	}

	/**
	 * TODO
	 */
	private IMessage activateNode() {
		return null;
	}

	/**
	 * TODO close connection
	 */
}
