///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [AdminNodeState.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.admin;

/** Enumerates possible states of the administration node */
public enum AdminNodeState {

	/** The admin node is waiting until all nodes have signed up. */
	INITIAL_SIGNUP_PHASE,
	
	/** Grid has been initialized. */
	GRID_RUNNING, 
	
	/** Admin node is notifying individual nodes in grid of shutdown */
	SHUTDOWN,
	
}

/* vi: set shiftwidth=4 tabstop=4: */ 