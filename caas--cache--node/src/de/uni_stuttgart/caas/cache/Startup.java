package de.uni_stuttgart.caas.cache;

import java.net.InetSocketAddress;



public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CacheNode n = new CacheNode(new InetSocketAddress("", 0));
	}

}
