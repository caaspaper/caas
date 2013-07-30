package de.uni_stuttgart.caas.admin;

import java.io.IOException;

public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {

			try {
				new AdminNode();
			} catch (IOException e) {
				System.out.println("Failed to launch AdminNode (1)"); 
				e.printStackTrace();
			}
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
				} catch (IOException e) {
					System.out.println("Failed to launch AdminNode (2)"); 
					e.printStackTrace();
				}
			} else {
				System.out
						.println("Invalid number of arguments. Please enter port number and initial grid capacity again.");
			}
		}
	}
}
