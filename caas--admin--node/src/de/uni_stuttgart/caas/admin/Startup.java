package de.uni_stuttgart.caas.admin;

import java.io.IOException;

public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AdminNode admin = null;

		try {
			if (args.length == 0) {
				admin = new AdminNode();
			} else if (args.length == 2) {

				final int portNumber = Integer.parseInt(args[0]);
				final int initialCapacity = Integer.parseInt(args[1]);

				try {
					admin = new AdminNode(portNumber, initialCapacity);
				} catch (IllegalArgumentException e) {
					System.out.println("Invalid argument");
					e.printStackTrace();
				}
			} else {
				System.out.println("Invalid number of arguments. Please enter port number and initial grid capacity again.");
			}
		} catch (IOException e) {
			System.out.println("Failed to launch AdminNode (2)");
			e.printStackTrace();
			return;
		}

		assert admin != null;

		// TODO: benchmarking has technically nothing to do with the admin. Why
		// is it not a separate application, and why was the benchmarking code
		// mangled into the admin in the first place?

		// parse command line arguments
		int benchmark = -1;
		int k = -1;
		for (String s : args) {
			if (s.startsWith("-benchmark=")) {
				final String type = s.substring(11);
				if (type.equals("uniform")) {
					benchmark = 1;
				} else if (type.equals("hotspot")) {
					benchmark = 2;
				} else {
					System.out.println("Unknown benchmark type: " + type);
					return;
				}
			} else if (s.startsWith("k=")) {
				try {
					k = Integer.parseInt(s.substring(2));
				} catch (NumberFormatException e) {
					System.out.println("Invalid value for request rate k");
					return;
				}
			}
		}

		if (k == -1) {
			System.out.println("Request rate k not set, assuming 10");
			k = 10;
		}

		if (benchmark == 1) {
			admin.generateQueriesUniformlyDistributed(k);
		} else if (benchmark == 2) {
			admin.generateQueriesUniformlyDistributedHotspot(k);
		}
	}
}
