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
