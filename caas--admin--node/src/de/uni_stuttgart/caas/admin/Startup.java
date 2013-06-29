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
