package controllers;


import java.io.*;
import java.util.ArrayList;


public class Client_Controller implements ChatIF {
	// Class variables *************************************************

	/**
	 * The default port to connect on.
	 */
	final public static int DEFAULT_PORT = 5555;

	// Instance variables **********************************************

	/**
	 * The instance of the client that created this ConsoleChat.
	 */
	ChatClient client;

	// Constructors ****************************************************

	/**
	 * Constructs an instance of the ClientConsole UI.
	 *
	 * @param host The host to connect to.
	 * @param port The port to connect on.
	 */
	public Client_Controller(String host, int port) {
//		try {
//			client = new ChatClient(host, port, this);
//		} catch (IOException exception) {
//			System.out.println("Error: Can't setup connection!" + " Terminating client.");
//			System.exit(1);
//		}
	}

	// Instance methods ************************************************

	/**
	 * This method waits for input from the console. Once it is received, it sends
	 * it to the client's message handler.
	 */
	public void accept() {
		try {
			BufferedReader fromConsole = new BufferedReader(new InputStreamReader(System.in));
			// Object message;
//			Car car1 = new Car("hunday","i10",1990);
//			client.handleMessageFromClientUI(car1);

			ArrayList<String> message = new ArrayList<>();
			while (true) {
				String inputFromConsole = fromConsole.readLine();
				message.add(inputFromConsole);
				client.handleMessageFromClientUI(message);
			}
		} catch (Exception ex) {
			System.out.println("Unexpected error while reading from console!");
		}
	}
	
	public void onEditReservationRequest(String reservationId) {
	    System.out.println("Editing reservation: " + reservationId);
	}
	
	public void onShowReservationsRequest() {
		
	}

	/**
	 * This method overrides the method in the ChatIF interface. It displays a
	 * message onto the screen.
	 *
	 * @param message The string to be displayed.
	 */
	public void display(Object message) {
		
		if (message instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<String> list = (ArrayList<String>) message;
			int lastItem = list.size();
			String recived = list.get(lastItem - 1);
			System.out.println("> " + recived);
		}
//		else if(message instanceof Car) {
//			Car receivedCar = (Car) message;
//			System.out.println("Car received: " +
//								" Make: " + receivedCar.getMake() +
//								", Model: " + receivedCar.getModel() +
//								", from client: " + client.toString());
//		}
		else
		 System.out.println("> " + message);

	}


	public static void main(String[] args) {
		String host = "";

		try {
			host = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			host = "localhost";
		}
		Client_Controller chat = new Client_Controller(host, DEFAULT_PORT);
		chat.accept(); // Wait for console data
	}


}
