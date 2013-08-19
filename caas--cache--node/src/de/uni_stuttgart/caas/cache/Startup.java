package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.net.InetSocketAddress;



public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int port, numOfNodes;
		String host;
		if (args.length != 3) {
			throw new IllegalArgumentException("wrong number of arguments");
		}
		try {
			host = args[0];
			port = Integer.parseInt(args[1]);
			numOfNodes = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("wrong arguments");
		}
		try {
			for (int i = 0; i < numOfNodes; i++) {
				new CacheNode(host, port);
			}
		} catch (IOException e) {
			
			throw new IllegalArgumentException("Could not connect to admin with these arguments");
		}
	}

}
