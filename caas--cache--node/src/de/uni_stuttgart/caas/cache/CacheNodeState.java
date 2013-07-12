///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [CacheNodeState.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.cache;


/**
 *  Enum to represent the current state of a cache node
 */
public enum CacheNodeState {

	/**
	 *  The state a node starts with
	 */
	INITIAL_STATE,
	
	/**
	 *  In this state the cache node, send a JOIN message to the admin, and awaits its location data
	 */
	AWAITING_DATA,
	
	/**
	 *  In this state the cache node received the location data, and awaits activation
	 */
	AWAITING_ACTIVATION,
	
	/**
	 * This is the active running state of a cache node
	 */
	ACTIVE
	
}

/* vi: set shiftwidth=4 tabstop=4: */ 