package controllers;

import java.io.IOException;
import java.util.ArrayList;
import ocsf.server.*;


public class Server_Controller extends AbstractServer {

	final public static int DEFAULT_PORT = 5555;

	public Server_Controller(int port) {
		super(port);
	}

	public void handleMessageFromClient(Object message, ConnectionToClient client) {
		if (message instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<String> list = (ArrayList<String>) message;
			int lastItem = list.size();
			String recived = list.get(lastItem - 1);
			System.out.println("message " + recived);
		}
//		else if(message instanceof Car) {
//			Car receivedCar = (Car) message;
//			System.out.println("Car received: " +
//								" Make: " + receivedCar.getMake() +
//								", Model: " + receivedCar.getModel() +
//								", from client: " + client.toString());
//		}

		else
			System.out.println("Message received: " + message + " from " + client);
		
		try {
	        client.sendToClient(message); 
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	/**
	 * This method overrides the one in the superclass. Called when the server stops
	 * listening for connections.
	 */
	protected void serverStopped() {
		System.out.println("Server has stopped listening for connections.");
	}

	// Class methods ***************************************************

	/**
	 * This method is responsible for the creation of the server instance (there is
	 * no UI in this phase).
	 *
	 * @param args[0] The port number to listen on. Defaults to 5555 if no argument
	 *                is entered.
	 */
	public static void main(String[] args) {
		int port = 0; // Port to listen on

		try {
			port = Integer.parseInt(args[0]); // Get port from command line
		} catch (Throwable t) {
			port = DEFAULT_PORT; // Set port to 5555
		}

		Server_Controller sv = new Server_Controller(port);

		try {
			sv.listen(); // Start listening for connections
		} catch (Exception ex) {
			System.out.println("ERROR - Could not listen for clients!");
		}
	}
}
//End of EchoServer class
