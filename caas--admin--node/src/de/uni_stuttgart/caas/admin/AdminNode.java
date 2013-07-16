package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import de.uni_stuttgart.caas.admin.JoinRequestManager.JoinRequest;
import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.base.NodeInfo;
import de.uni_stuttgart.caas.messages.ActivateNodeMessage;
import de.uni_stuttgart.caas.messages.AddToGridMessage;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;

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
	
	boolean sentActivate = false;
	
	/** Current state of the admin node */
	private AdminNodeState state;

	public final static int DEFAULT_INITIAL_CAPACITY = 15;
	public final int INITIAL_CAPACITY;

	public final static int DEFAULT_PORT_NUMBER = 5007;
	public final int PORT_NUMBER;

	/** List of nodes requesting to join grid */
	private JoinRequestManager joinRequests;

	/** So that NodeConnector threads wait until grid is initialized */
	private Object monitor = new Object();

	/**
	 * The grid of nodes, created after all join requests are in. Only one
	 * thread should be able to initialize Grid.
	 */
	private Grid grid = null;

	private CountDownLatch activationCountDown;
	
	

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
	
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
		} catch (IOException e) {
			System.out.println("Could not listen on PORT_NUMBER");
			e.printStackTrace();
		}

		assert serverSocket != null;

		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				NodeConnector nc = new NodeConnector(clientSocket);

				// TODO: NodeConnector shutdown
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
	private class NodeConnector extends FullDuplexMPI {
		private final InetSocketAddress clientAddress;

		/**
		 * 
		 * @param cS
		 * @throws IOException
		 */
		public NodeConnector(Socket cS) throws IOException {
			super(cS, System.out);

			assert cS.getRemoteSocketAddress() instanceof InetSocketAddress;
			this.clientAddress = (InetSocketAddress) cS.getRemoteSocketAddress();
		}

		@Override
		public IMessage processIncomingMessage(IMessage message) {
			switch (message.getMessageType()) {
			case CONFIRM:
				break;

			case JOIN:
				JoinRequest jr = new JoinRequest(clientAddress);
				assert jr != null;
				final ConfirmationMessage response = respondToJoinRequest(jr);

				if (response.STATUS_CODE == 0) {
					// fire off grid construction in a separate thread to have
					// the
					// message pump stay responsive.
					new Thread(new InitGridHelper()).start();
				}
				return response;

			default:
				break;
			}

			return new ConfirmationMessage(-3, "unexpected message type: " + message.getMessageType().toString());
		}

		private class InitGridHelper implements Runnable {
			@Override
			public void run() {
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
				// now send back messages to cache nodes
				sendMessageAsync(addNodeToGrid(clientAddress));

				activationCountDown.countDown();

				try {
					activationCountDown.await();
				} catch (InterruptedException e) {
					System.out.println("interrupted while waiting to activate node, not responding");
					e.printStackTrace();
				}

				sendMessageAsync(activateNode());
			}
		}
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
	private ConfirmationMessage respondToJoinRequest(JoinRequest request) {
		if (joinRequests == null) {
			return new ConfirmationMessage(-2, "not in startup phase, no more join requests accepted");
		}

		int sc = 0;
		String m = null; // optional, may stay null

		try {
			joinRequests.TryAdd(request);
		} catch (IllegalStateException e) {
			sc = -1;
			m = "initial grid capacity exchausted";
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

		assert sentActivate == false;
		return new AddToGridMessage(grid.getLocationOfNode(addressOfNode), grid.getNeighborInfo(addressOfNode));
	}

	/**
	 * Generate a new activation message
	 * 
	 * @return a new activation message
	 */
	private IMessage activateNode() {
		sentActivate = true;
		return new ActivateNodeMessage();
	}

	/**
	 * TODO Alex Shut down all connected nodes and then shutdown admin
	 */
	public void shutDownSystem() {

	}

}
