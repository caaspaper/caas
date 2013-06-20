package de.uni_stuttgart.caas.cache;


/**
 *  Enum to represent the current state of a cache node
 */
public enum CacheNodeState {

	
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
