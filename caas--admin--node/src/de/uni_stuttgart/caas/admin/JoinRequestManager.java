package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of a wait list of nodes wishing to join the grid during its
 * initial signup phase.
 * 
 * The caller can use TryAdd() to add nodes to the list and IsComplete() to
 * check whether the list has been filled, in which case grid construction may
 * proceed.
 * */
public class JoinRequestManager {

	private final int INITIAL_CAPACITY;

	/** Holds the list of cache nodes who requested to be signed up to the grid. */
	private List<JoinRequest> joinRequests;

	/**
	 * Constructs a JoinRequestManager that holds a wait list with a specified
	 * size.
	 * */
	public JoinRequestManager(int initialCapacity) {
		assert initialCapacity > 0;
		INITIAL_CAPACITY = initialCapacity;
		joinRequests = new ArrayList<>(INITIAL_CAPACITY);
	}

	/**
	 * Checks whether the wait list is full and grid construction can therefore
	 * begin.
	 * 
	 * This method is threadsafe.
	 */
	public boolean IsComplete() {
		synchronized (joinRequests) {
			return joinRequests.size() == INITIAL_CAPACITY;
		}
	}

	/**
	 * Tries to add a node to the list of nodes to be signed up to the grid.
	 * 
	 * This method is threadsafe.
	 * 
	 * @param rw
	 *            non-null join request instance to be added to the list.
	 * @throw IllegalStateException if the initial grid capacity is exceeded
	 *        (note that a check against IsComplete() does not prevent this as
	 *        multiple calls to TryAdd may race against each other.)
	 * @throw IllegalArgumentException if the join request is for a node that is
	 *        already on the list.
	 */
	public void TryAdd(JoinRequest rq) {
		assert rq != null;

		synchronized (joinRequests) {
			assert joinRequests.size() <= INITIAL_CAPACITY;
			if (joinRequests.size() == INITIAL_CAPACITY) {
				throw new IllegalStateException();
			}
		
			if (joinRequests.contains(rq)) {
				throw new IllegalArgumentException("rq");
			}

			joinRequests.add(rq);
		}
	}

	public static class JoinRequest {

		public JoinRequest(InetSocketAddress address) {
			assert address != null;
			ADDRESS = address;
		}

		public final InetSocketAddress ADDRESS;
	}

}
