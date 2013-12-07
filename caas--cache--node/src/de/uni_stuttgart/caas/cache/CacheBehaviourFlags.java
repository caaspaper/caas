package de.uni_stuttgart.caas.cache;

public enum CacheBehaviourFlags {

	/**
	 * LoadBalancing: if a node is set to process a given query, but its own
	 * load exceeds a threshold value, the work is propagated to a random
	 * neighbor node (which is forced to process the query).
	 */
	NEIGHBOR_PROPAGATION,

	/**
	 * Enable automatic Scale-in. If global load exceeds some threshold, more
	 * cache nodes are added dynamically
	 */
	SCALEIN,

	/** TODO - not implemented yet */
	SCALEOUT

}
