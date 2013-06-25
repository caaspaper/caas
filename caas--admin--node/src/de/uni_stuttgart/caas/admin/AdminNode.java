package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import de.uni_stuttgart.caas.admin.messages.JoinMessage;

/**
 * Run multithreaded server and obey the following protocol:
 * 
 * INITIAL_SIGNUP_PHASE, grid initial capacity not exceeded and node didn't sign
 * up before FAIL otherwise

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

	public AdminNodeState getState() {
		return state;
	}

	public AdminNode(int initialCapacity) {
		state = AdminNodeState.INITIAL_SIGNUP_PHASE;

		INITIAL_CAPACITY = initialCapacity;
		assert initialCapacity > 0;

		joinRequests = new JoinRequestManager(initialCapacity);

		// TODO: run multithreaded server
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
				Thread t = new Thread(new NodeConnector(clientSocket,
						"NodeJoin"));
				t.start();
			} catch (IOException e) {
				System.out.println("Accept failed: PORT_NUMBER");
				System.exit(-1); // TODO: fix exit (maybe overkill) throw
									// exception instead?
			}
		}
	}

	private class NodeConnector implements Runnable {
		private Socket clientSocket;

		// string attribute?
		public NodeConnector(Socket cs, String s) {
			this.clientSocket = cs;
			// string field?
		}

		@Override
		public void run() {
			ObjectInputStream in = null;
			PrintWriter out = null;
			try {
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new PrintWriter(clientSocket.getOutputStream(), true);
			} catch (IOException e) {
				System.out.println("");
				System.exit(-1);
			}
			while (true) {
				try {
					Object obj = in.readObject();
				} catch (ClassNotFoundException e) {
					System.out.println("Object class not found");
					System.exit(-1); // TODO

				} catch (IOException e) {
					System.out.println("Read failed");
					System.exit(-1); // TODO
				}
			}
		}
	}

	/**
	 * @param message
	 *            The join message sent by node to admin node.
	 */
	private void respondToJoinRequest(JoinMessage message) {

	}

	/**
	 * 
	 */
	private void addNodeToGrid() {

	}

	/**
	 * TODO
	 */
	private void activateNode() {

	}

	/**
	 * TODO close connection
	 */
}
