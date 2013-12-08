package de.uni_stuttgart.caas.admin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import de.uni_stuttgart.caas.admin.JoinRequestManager.JoinRequest;
import de.uni_stuttgart.caas.base.FullDuplexMPI;
import de.uni_stuttgart.caas.base.LogSender;
import de.uni_stuttgart.caas.messages.ActivateNodeMessage;
import de.uni_stuttgart.caas.messages.AddToGridMessage;
import de.uni_stuttgart.caas.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.messages.IMessage;
import de.uni_stuttgart.caas.messages.IMessage.MessageType;
import de.uni_stuttgart.caas.messages.JoinMessage;
import delaunay_triangulation.Triangle_dt;

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
public class AdminNode /* implements AutoClosable */{
	private boolean sentActivate = false;

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

	private CountDownLatch activationCountDown, initFinishedCountDown;
	private final Thread acceptingThread;
	private ServerSocket serverSocket;

	private LogSender logger;

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
	 * See AdminNode(int portNumber, int initialCapacity) for more information.
	 */
	public AdminNode() throws IOException {

		this(DEFAULT_PORT_NUMBER, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates administrative Node
	 * 
	 * This creates a new thread to handle incoming node connections. To
	 * shutdown this thread and release all resources associated with the admin
	 * node, call close()
	 * 
	 * @param initialCapacity
	 * @throws IOException
	 *             if the ServerSocket for the given port could not be obtained.
	 */
	public AdminNode(int portNumber, int initialCapacity) throws IOException {
		state = AdminNodeState.INITIAL_SIGNUP_PHASE;

		logger = new LogSender(new InetSocketAddress("localhost", 43215));

		if (portNumber < 1024 || portNumber > 65536) {
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
		initFinishedCountDown = new CountDownLatch(initialCapacity);

		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
			serverSocket.setReuseAddress(true);
		} catch (IOException e) {
			logger.write("Could not listen on PORT_NUMBER");
			e.printStackTrace();
			throw e;
		}

		assert serverSocket != null;
		final ArrayList<NodeConnector> connectors = new ArrayList<>();

		// fire off a thread to accept incoming connections
		acceptingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Socket clientSocket = serverSocket.accept();
						final NodeConnector nc = new NodeConnector(clientSocket);
						connectors.add(nc);

					} catch (IOException e) {
						logger.write("Accept failed: PORT_NUMBER");
						e.printStackTrace();
						break;
					}
				}

				// shutdown: free all connections
				for (NodeConnector con : connectors) {
					con.close();
				}
			}
		});

		acceptingThread.start();

		// fire off a thread to call the onInitComplete() method once we are
		// ready
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					initFinishedCountDown.await();
				} catch (InterruptedException e) {
					logger.write("interrupted while waiting for nodes to be activated");
					e.printStackTrace();
				}

				onInitComplete();
			}

		}).start();
	}

	/**
	 * Invoked once after all cache nodes have signaled that they have completed
	 * their activation sequence. This marks the points where query processing
	 * can start.
	 */
	protected void onInitComplete() {

	}

	private static AtomicInteger idSource = new AtomicInteger();

	/**
	 * 
	 * 
	 *
	 */
	private class NodeConnector extends FullDuplexMPI {
		private final InetSocketAddress clientAddress;
		private volatile long nodeId = -1;
		private int subdivCount = 0;

		/**
		 * 
		 * @param cS
		 * @throws IOException
		 */
		public NodeConnector(Socket cS) throws IOException {
			super(cS, System.out, false);
			nodeId = idSource.getAndIncrement();

			assert cS.getRemoteSocketAddress() instanceof InetSocketAddress;
			this.clientAddress = (InetSocketAddress) cS.getRemoteSocketAddress();

			start();
		}

		@Override
		public IMessage processIncomingMessage(IMessage message) {
			switch (message.getMessageType()) {
			case CONFIRM:
				break;

			case JOIN:
				if (!(message instanceof JoinMessage)) {
					logger.write("Received Message that should be a join message, but it wasn't");
					System.exit(-1);
				}
				JoinMessage m = (JoinMessage) message;
				nodeId = idSource.getAndIncrement();
				JoinRequest jr = new JoinRequest(clientAddress, m.ADDRESS_FOR_CACHENODE_NEIGHBORCONNECTOR, m.ADDRESS_FOR_CACHENODE_QUERYLISTENER, nodeId);
				assert jr != null;
				final ConfirmationMessage response = respondToJoinRequest(jr);

				if (response.STATUS_CODE == 0) {
					// fire off grid construction in a separate thread to have
					// the message pump stay responsive.
					new Thread(new InitGridHelper()).start();
				}
				return response;

			case SUBDIV_COMMIT:
				// TODO: handle subdivision event. Right now, the new CacheNode
				// is spawned locally by whomever initiated the subdivision. In
				// a long-term view, we need the admin to actually deploy a new
				// node.
				System.out.println("admin: grid subdivision no #" + (++subdivCount));
				return new ConfirmationMessage(1
						, "ok");

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
				assert nodeId != -1;

				// now send back messages to cache nodes
				sendMessageAsync(addNodeToGrid(clientAddress, nodeId), new IResponseHandler() {

					@Override
					public void onResponseReceived(IMessage response) {

						assert response.getMessageType() == MessageType.CONFIRM;
						activationCountDown.countDown();
					}

					@Override
					public void onConnectionAborted() {
						logger.write("admin: connection to cache node was aborted");
						// TODO: this is currently a dangling state
					}
				});

				try {
					activationCountDown.await();
				} catch (InterruptedException e) {
					logger.write("interrupted while waiting to activate node, not responding");
					e.printStackTrace();
				}

				sendMessageAsync(activateNode(), new IResponseHandler() {
					@Override
					public void onResponseReceived(IMessage response) {

						assert response.getMessageType() == MessageType.CONFIRM;
						initFinishedCountDown.countDown();
					}

					@Override
					public void onConnectionAborted() {
						logger.write("admin: connection to cache node was aborted while waiting for activation to complete");
						// TODO: this is currently a dangling state
					}
				});
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
	 * @param id
	 *            Unique grid of the node
	 * @return An AddToGridMessage containing information about nodes
	 *         neighboring the cache node
	 * 
	 * @note This method is only called during the initialization of the grid.
	 *       All other later nodes that want to join the grid will be handled
	 *       directly by the grid's methods.
	 * 
	 */
	private IMessage addNodeToGrid(InetSocketAddress addressOfNode, long id) {

		assert sentActivate == false;
		return new AddToGridMessage(grid.getLocationOfNode(addressOfNode), grid.getNeighborInfo(addressOfNode), id);
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

	public Vector<Triangle_dt> getTriangles() {
		return grid.getTriangles();
	}

	/*
	 * Shutdown the admin node, aborting all open connections to nodes
	 */
	public void close() {
		try {
			// this causes accept() to throw
			serverSocket.close();
		} catch (IOException e) {
			// ignore
		}
		try {
			acceptingThread.join();
		} catch (InterruptedException e) {
			// ignore
		}
	}

	public void generateQueriesUniformlyDistributed(final int numOfQueriesPerNode) {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				QuerySender.generateDistributedQueries(numOfQueriesPerNode, grid.getConnectedNodes(), logger, true);
			}
		});
		t.start();
	}

	public void generateQueriesUniformlyDistributedHotspot(final int numOfQueriesPerNode) {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				QuerySender.generateDistributedQueries(numOfQueriesPerNode, grid.getConnectedNodes(), logger, false);
			}
		});
		t.start();
	}
}
