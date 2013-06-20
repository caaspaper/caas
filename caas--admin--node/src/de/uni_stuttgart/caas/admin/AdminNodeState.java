package de.uni_stuttgart.caas.admin;

/** Enumerates possible states of the administration node */
public enum AdminNodeState {

	/** The admin node is waiting until all nodes have signed up. */
	INITIAL_SIGNUP_PHASE,

	/** Cache grid is build and running */
	RUNNING, 
	
	/** Admin node is notifying individual nodes in grid of shutdown */
	SHUTDOWN,
	
}
