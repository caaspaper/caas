package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import de.uni_stuttgart.caas.admin.JoinRequestManager.JoinRequest;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.ActivateNodeMessage;
import de.uni_stuttgart.caas.messages.AddToGridMessage;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;

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
public class AdminNode implements Runnable {

	/** Current state of the admin node */
	private AdminNodeState state;

	public final static int DEFAULT_INITIAL_CAPACITY = 64;
	public final int INITIAL_CAPACITY;

	public final static int DEFAULT_PORT_NUMBER = 5007;
	public final int PORT_NUMBER;

	/** List of nodes requesting to join grid */
	private JoinRequestManager joinRequests;

	/** So that NodeConnector threads wait until grid is initialized */
	private Object monitor = new Object();

	/**
	 * Only one thread should be able to initialize Grid.
	 * 
	 * @note Declared as volatile in order to ensure correctness of
	 */
	private Grid grid = null;
	
	private CountDownLatch activationCountDown;
	
	
	Thread serverThread;

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
		activationCountDown = new CountDownLatch(initialCapacity);
		serverThread = new Thread(this);
		serverThread.start();
	}
	
	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
		} catch (IOException e) {
			System.out.println("Could not listen on PORT_NUMBER");
			e.printStackTrace();
		}

		assert serverSocket != null;

		while (!serverThread.isInterrupted()) {
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
		
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("server is being interrupted during an interrupt... should not happen");
			e.printStackTrace();
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
		
		
		private void writeToRemote(IMessage m, ObjectOutputStream out) {
			try {
				out.writeObject(m);
			} catch (IOException e) {
				System.out.println("error while sending to node");
				e.printStackTrace();
			}
		}

		/**
		 * Threadstarter
		 */
		public void run() {
			ObjectInputStream in = null;
			ObjectOutputStream out = null;

			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());
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
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("Read failed");
					e.printStackTrace();
				}
		
				IMessage response = process(message, this.clientAddress);
				if (response != null) {
					writeToRemote(response, out);
					
				}
				
				if (message.getMessageType() == MessageType.JOIN) {
					initGrid(out);
				}	
				
			}
		}
		
		
		private void initGrid(ObjectOutputStream out) {
			synchronized (monitor) {
				while (joinRequests != null && !joinRequests.IsComplete()) {
					// to avoid spurious wake ups
					try {
						monitor.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				ensureGridInitialized();

				// on other objects, will be an empty operation
				// because nobody should be waiting.
				monitor.notifyAll();
			}
			
			assert state == AdminNodeState.GRID_RUNNING;
			assert grid != null;
			//now send back messages to cache nodes
			writeToRemote(addNodeToGrid(this.clientAddress), out);
			
			activationCountDown.countDown();
			
			
			try {
				activationCountDown.await();
			} catch (InterruptedException e) {
				System.out.println("interrupted while waiting to activate node, not responding");
				e.printStackTrace();
			}
			
			writeToRemote(new ActivateNodeMessage(), out);
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
		switch (message.getMessageType()) {
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
	 * Called by the last thread that is added to joinRequest during initial
	 * sign-up phase.
	 * 
	 * @note
	 * 
	 */
	private void ensureGridInitialized() {
		if (state != AdminNodeState.GRID_RUNNING) {
			assert grid == null;
			grid = new Grid(joinRequests);
			state = AdminNodeState.GRID_RUNNING;
			// joinRequest is no longer needed
			joinRequests = null;
		}
	}

	/**
	 * Adds its cache node to the grid. Queries grid for relevant information on
	 * its node's neighbors.
	 * 
	 * @param addressOfNode
	 *            Contains IP + port of the cache node that is to be added to
	 *            grid
	 * @return An AddToGridMessage containing information about nodes
	 *         neighboring the cache node
	 * 
	 * @note This method is only called during the initialization of the grid.
	 *       All other later nodes that want to join the grid will be handled
	 *       directly by the grid's methods.
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
	 * TODO Alex
	 * Shut down all connected nodes and then shutdown admin
	 */
	public void shutDownSystem() {
		
		// TODO shut everything down
		
		// close server
		serverThread.interrupt();
	}	
	
}
