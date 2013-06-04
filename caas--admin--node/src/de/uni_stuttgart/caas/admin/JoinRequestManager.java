package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.List;

public class JoinRequestManager {

	private final int INITIAL_CAPACITY;
	

	/** Holds the list of cache nodes who requested to be signed up to the grid. */
	private List<JoinRequest> joinRequests;
	
	
	public JoinRequestManager(int initialCapacity) {
		assert initialCapacity > 0;
		INITIAL_CAPACITY = initialCapacity;
	}
	
	
	
	//public void Add()
	private static class JoinRequest
	{
		
		public JoinRequest(InetSocketAddress address) {
			assert address != null;
			ADDRESS = address;
		}
			
		public final InetSocketAddress ADDRESS;
	}
	
}
