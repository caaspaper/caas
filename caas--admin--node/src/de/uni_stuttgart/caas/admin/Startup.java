///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [Startup.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.admin;

public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {

			new AdminNode();
		} else {
			if (args.length == 2) {
				//TODO consider: revalidate or catch exception?
				int portNumber = Integer.parseInt(args[0]);
				int initialCapacity = Integer.parseInt(args[1]);
				
				try {
					new AdminNode(portNumber, initialCapacity);
				} catch (IllegalArgumentException e) {
					System.out.println("Invalid argument"); 
					e.printStackTrace();
				}
			} else {
				System.out
						.println("Invalid number of arguments. Please enter port number and initial grid capacity again.");
			}
		}
	}
}

/* vi: set shiftwidth=4 tabstop=4: */ 