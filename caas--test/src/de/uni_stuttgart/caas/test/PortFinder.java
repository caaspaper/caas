package de.uni_stuttgart.caas.test;

import java.io.IOException;
import java.net.ServerSocket;

public class PortFinder {

	/**
	 * Find a local port that is free for use.
	 * 
	 * @return Integer index of the port
	 * @throws IOException
	 *             To indicate that there is no open port
	 */
	public static int findOpen() throws IOException {
		// TODO: is there a better way to do this?
		ServerSocket s = new ServerSocket(0);
		s.setReuseAddress(true);
		final int port = s.getLocalPort();
		s.close();
		return port;
	}
}
