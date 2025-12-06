package controllers;

import java.io.*;
import gui.Customer_GUI;
import entities.Reservation;
import entities.Subscribed_Customer;
import messages.Message;
import messages.MessageType;

public class Client_Controller implements ChatIF {

	final public static int DEFAULT_PORT = 5555;
	ChatClient client;

	public Client_Controller(String host, int port) throws IOException {
		try {
			System.out.println(host);
			client = new ChatClient(host, port, this);
		} catch (IOException exception) {
			System.out.println("Error: Can't setup connection!" + " Terminating client");
			System.exit(1);
		}
	}

	public void onEditReservationRequest(String reservationId) {
		System.out.println("Editing reservation: " + reservationId);
	}

	public void onShowReservationsRequest() {
		Subscribed_Customer S_customer = new Subscribed_Customer(212121212, "Dor", "Shamo", "0502377735",
				"test@test.com", "DorShamo", "123456", 0);
		try {
			Message message = new Message(MessageType.GET_RESERVATIONS_BY_USER, S_customer);
			client.handleMessageFromClientUI(message);

		} catch (Exception ex) {
			System.out.println("Unexpected error while sending a reservation request!");
		}
	}
	public void sendUpdateReservationRequest(Reservation reservationToUpdate) {
        try {
            Message msg = new Message(MessageType.UPDATE_RESERVATION_REQUEST, reservationToUpdate);
            client.handleMessageFromClientUI(msg);
            System.out.println("Update request sent for reservation ID: " + reservationToUpdate.getId());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	public void logout() {
	    try {
	        Message msg = new Message(MessageType.LOGOUT_REQUEST, null);
	        client.handleMessageFromClientUI(msg); 
	        
	        System.out.println("Logout request sent to server.");
	        client.quit();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	public void display(Object message) {

		if (message instanceof Message) {
			Message recivedMessage = (Message) message;
			try {
				switch (recivedMessage.getType()) {

				case RETURN_RESERVATIONS_BY_USER:
					System.out.println("Reservations received: " + recivedMessage.getContent());

					if (Customer_GUI.instance != null) {
						Customer_GUI.instance.updateReservationsList(recivedMessage.getContent());
					}
					break;
				case RESERVATION_UPDATE_SUCCESS:
	                System.out.println("Update success! Refreshing list...");
	                onShowReservationsRequest(); 
	                break;
				case LOGOUT_REQUEST:
					break;

				default:
					break;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
			System.out.println("> " + message);

	}

}
