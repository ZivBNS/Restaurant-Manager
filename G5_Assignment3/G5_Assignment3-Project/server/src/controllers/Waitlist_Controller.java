package controllers;

import messages.Message;
import messages.MessageType;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;

import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;
import entities.Opening_Hours;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Restaurant;
import entities.Subscribed_Customer;
import entities.Waitlist;

/**
 * Controller responsible for handling all reservation-related logic on the server side.
 */

/*******************************************************************
 * waitlist states are: PWAITING, WAITING, NOTIFIED, COMPLETED, CANCELED
 *******************************************************************/
public class Waitlist_Controller {

    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Waitlist_Repository WaitlistRepository = Waitlist_Repository.getInstance();

    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
        	case CANCEL_WAITLIST_AND_RESERVATION_BY_CODE: return CancelWitlistAndReservationByCode(msg);
        	case JOIN_WAITLIST: return joinWaitlist(msg);

            default: return null;
        }
    }
    
    
    private static Message joinWaitlist(Message msg) {
		Reservation createResForWaitlist = (Reservation)msg.getContent();
		if (WaitlistRepository.isCustomerAlreadyInWaitlist(createResForWaitlist.getPhone(), createResForWaitlist.getEmail())) {
	        return new Message(MessageType.WAITLIST_JOINED_FAILED, "You are already in the waitlist!");
	    }
		createResForWaitlist.setConfirmationCode(reservationRepository.getNextConfirmationCode());
		if (!reservationRepository.set(createResForWaitlist)) return new Message(MessageType.WAITLIST_JOINED_FAILED, "System Error: Unable to join Waitlist at this time.\\nPlease try again later.");
		createResForWaitlist = reservationRepository.getLastReservationByContact(createResForWaitlist.getPhone(),createResForWaitlist.getEmail());
		if (createResForWaitlist==null) return new Message(MessageType.WAITLIST_JOINED_FAILED, "System Error: Unable to join Waitlist at this time.\\nPlease try again later.");
		Waitlist newWaitlist = new Waitlist(createResForWaitlist.getId());
		if (!WaitlistRepository.set(newWaitlist)) {
			reservationRepository.deleteById(createResForWaitlist.getId());
			return new Message(MessageType.WAITLIST_JOINED_FAILED, "System Error: Unable to join Waitlist at this time.\\nPlease try again later.");
		}
		return new Message(MessageType.WAITLIST_JOINED_SUCCESS, createResForWaitlist.getConfirmationCode());
	}

    
	private static Message CancelWitlistAndReservationByCode(Message msg) {
        int confirmationCode = (int) msg.getContent();
        
        Reservation r = reservationRepository.getByConfirmationCode(confirmationCode);
        
        if (r == null || r.getStatus().equalsIgnoreCase("Completed") || r.getStatus().equalsIgnoreCase("Canceled") || r.getStatus().equalsIgnoreCase("Active")) {
            return new Message(MessageType.RESERVATION_CANCEL_FAILED, (r==null)?null:"reservation");
        }

        boolean waitlistExists = false;
        boolean iswaitlistCanceled = false;
        //waitlist states are: WAITING, NOTIFIED, COMPLETED, CANCELED
        Waitlist w = WaitlistRepository.getByReservationId(r.getId());
        if (w!=null && (w.getStatus().equalsIgnoreCase("Completed") || w.getStatus().equalsIgnoreCase("Canceled"))) {
            return new Message(MessageType.RESERVATION_CANCEL_FAILED, "waitlist");
        }
        if (w != null) {
            waitlistExists = true;
            iswaitlistCanceled = WaitlistRepository.cancelWaitlistById(w.getId());
            if (!iswaitlistCanceled) {
                return new Message(MessageType.WAITLIST_CANCELED_FAILED, "error");
            }
        } 
        boolean reservationCanceled = reservationRepository.updateStatusByConfirmationCode(confirmationCode, ReservationStatus.CANCELED);
        if (reservationCanceled) {
            if (waitlistExists) {
                return new Message(MessageType.WAITLIST_CANCELED, null);
            }
            return new Message(MessageType.RESERVATION_CANCELED, null);
        }
        return new Message(MessageType.RESERVATION_CANCEL_FAILED, "error");
    }
    
}