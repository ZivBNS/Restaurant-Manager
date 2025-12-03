package controllers;

import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;
import Data.Reservation_Repository;
import entities.Reservation;

public class Reservation_Controller {
	private static final Reservation_Repository reservationRepository =
            Reservation_Repository.getInstance();
    public static void handleMessage(Message msg, ConnectionToClient client) {

        switch (msg.getType()) {

            case CREATE_RESERVATION:
                createReservation(msg, client);
                break;

            case CANCEL_RESERVATION:
                cancelReservation(msg, client);
                break;

            case GET_RESERVATIONS_BY_USER:
                getReservationsByUser(msg, client);
                break;

            default:
                System.out.println("Reservation_Controller: Unknown message type: " + msg.getType());
                break;
        }
    }

    // ------------------------------------------------------
    // CREATE RESERVATION
    // ------------------------------------------------------
    private static void createReservation(Message msg, ConnectionToClient client) {
        try {

            Reservation reservation = (Reservation) msg.getContent();

            boolean success = reservationRepository.set(reservation);

            if (success) {
                client.sendToClient(
                    new Message(MessageType.RESERVATION_CONFIRMED, reservation)
                );
            } else {
                client.sendToClient(
                    new Message(MessageType.RESERVATION_FAILED, null)
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // GET RESERVATIONS BY USER
    // ------------------------------------------------------
    private static void getReservationsByUser(Message msg, ConnectionToClient client) {
        try {
            int userId = (int) msg.getContent();

            var reservations = reservationRepository.getById(userId);

            client.sendToClient(
                new Message(MessageType.RETURN_RESERVATIONS_BY_USER, reservations)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // CANCEL RESERVATION
    // ------------------------------------------------------
    private static void cancelReservation(Message msg, ConnectionToClient client) {
        try {
            int reservationId = (int) msg.getContent();

            boolean success = reservationRepository.deleteById(reservationId);

            MessageType responseType = success
                    ? MessageType.RESERVATION_CANCELED
                    : MessageType.RESERVATION_CANCEL_FAILED;

            client.sendToClient(
                new Message(responseType, reservationId)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



