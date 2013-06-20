package de.uni_stuttgart.caas.admin;

/**
 * Run multithreaded server and obey the following protocol:
 * 
 * INITIAL_SIGNUP_PHASE, grid initial capacity not exceeded and node didn't sign
 * up before FAIL otherwise

 * 
 */
public class AdminNode {

	private final int INITIAL_CAPACITY;

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
	}

	// TODO: run multithreaded server
}
