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

package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.net.InetSocketAddress;



public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			CacheNode n = new CacheNode(new InetSocketAddress("localhost", 5007));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

/* vi: set shiftwidth=4 tabstop=4: */ 
