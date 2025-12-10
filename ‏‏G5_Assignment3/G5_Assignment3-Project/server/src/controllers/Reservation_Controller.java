package controllers;

import messages.Message;
import messages.MessageType;
import Data.Reservation_Repository;
import entities.Reservation;
import entities.Subscribed_Customer;

public class Reservation_Controller {
	
	private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
	
    /*
     * HANDLER FOR THE CLIENT REQUESTS.
     */
    public static Message handleMessage(Message msg) {

        switch (msg.getType()) {

            case CREATE_RESERVATION: 
            	return createReservation(msg);

            case CANCEL_RESERVATION: 
            	return cancelReservation(msg);

            case GET_RESERVATIONS_BY_USER: 
            	return getReservationsByUser(msg);
                
            case UPDATE_RESERVATION_REQUEST:
            	return updateReservation(msg);

            default:
                System.out.println("Reservation_Controller: Unknown message type: " + msg.getType());
                return null;
        }
    }
    
    
    /*
     * CREATE RESERVATION.
     */
    private static Message createReservation(Message msg) {
        try {
            Reservation reservation = (Reservation) msg.getContent();
            boolean success = reservationRepository.set(reservation);

            if (success) {
                return new Message(MessageType.RESERVATION_CONFIRMED, reservation);
            } else {
                return new Message(MessageType.RESERVATION_FAILED, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    /*
     * GET RESERVATIONS BY USER.
     */
    private static Message getReservationsByUser(Message msg) {
        try {
        	System.out.println("getReservationsByUser - Reservation_Controller");
            int userId = ((Subscribed_Customer)msg.getContent()).getSubscriberCode();
            var reservations = reservationRepository.getByUserId(userId);
        	System.out.println(reservations != null ? reservations.toString() : "No reservations found");

            return new Message(MessageType.RETURN_RESERVATIONS_BY_USER, reservations);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    /*
     * CANCEL RESERVATION.
     */
    private static Message cancelReservation(Message msg) {
        try {
            int reservationId = (int) msg.getContent();
            boolean success = reservationRepository.deleteById(reservationId);
            MessageType responseType = success ? MessageType.RESERVATION_CANCELED : MessageType.RESERVATION_CANCEL_FAILED;
            return new Message(responseType, reservationId);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    /*
     * UPDATE RESERVATION.
     */
    private static Message updateReservation(Message msg) {
    	try {
    		Reservation reservation = (Reservation) msg.getContent();
    		boolean success = reservationRepository.update(reservation);
    		MessageType responseType = success ? MessageType.RESERVATION_UPDATE_SUCCESS : MessageType.RESERVATION_UPDATE_FAILED;
    		return new Message(responseType, reservation);
    		
    	} catch(Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    }
}