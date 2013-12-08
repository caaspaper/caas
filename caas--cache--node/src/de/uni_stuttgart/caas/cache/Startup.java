package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;



public class Startup {

	/**
	 * Standalone main() for running a cacheNode from the command line
	 * 
	 * @param args
	 *            the ip address of the admin and the port, the admin is
	 *            listening on
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("please provide the host and the port of admin node");
		}

		EnumSet<CacheBehaviourFlags> config = EnumSet.noneOf(CacheBehaviourFlags.class);
		for (String s : args) {
			if (s.equals("-scalein")) {
				config.add(CacheBehaviourFlags.SCALEIN);
			} else if (s.equals("-scaleout")) {
				config.add(CacheBehaviourFlags.SCALEOUT);
			} else if (s.equals("-propagation")) {
				config.add(CacheBehaviourFlags.NEIGHBOR_PROPAGATION);
			} else if (s.equals("-reuseclientconn")) {
				config.add(CacheBehaviourFlags.REUSE_CLIENT_CONN);
			} else if (s.equals("-fakeneighborlatency")) {
				config.add(CacheBehaviourFlags.ADD_FAKE_NEIGHBOR_LATENCY);
			}
		}
		try {
			new CacheNode(args[0], Integer.parseInt(args[1]), config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

