package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Run multithreaded server and obey the following protocol:
 * 
 * JOIN -- send to admin node by a node wishing to join the grid. Empty
 * messages, admin records source ip+port and sends back: OK if state is
 * INITIAL_SIGNUP_PHASE, grid initial capacity not exceeded and node didn't sign
 * up before FAIL otherwise
 * 
 * ADD_TO_GRID -- send to a cache node to inform it about its spot in the grid,
 * contains a 2D position, initial cache radius and list of neighbor cache nodes
 * identified by (ip+port). Expect response: OK
 * 
 * ACTIVATE -- send to a cache node to activate it. Upon activation, a cache
 * node starts processing client requests.
 * 
 */
public class AdminNode {

	private final int INITIAL_CAPACITY;
	
	/** Current state of the admin node */
	private AdminNodeState state;

	/** Used during INITIAL_SIGNUP_PHASE to keep track of all nodes who requested to JOIN */
	private JoinRequestManager joinRequests;

	public AdminNodeState getState() {
		return state;
	}

	public AdminNode(int initialCapacity) {
		state = AdminNodeState.INITIAL_SIGNUP_PHASE;

		INITIAL_CAPACITY = initialCapacity;
		assert initialCapacity > 0;

		joinRequests = new JoinRequestManager(initialCapacity);
	}

	// TODO: run multithreaded server
}
