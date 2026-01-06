package controllers;

import messages.Message;
import messages.MessageType;


import Data.Reservation_Repository;
import Data.User_Repository;
import Data.Waitlist_Repository;
import entities.Reservation;
import entities.ReservationStatus;

import entities.UserRecord;
import entities.Waitlist;
import integration.EmailService;

/**
 * Controller responsible for handling all reservation-related logic on the server side.
 */

/*******************************************************************
 * waitlist states are: PWAITING, WAITING, NOTIFIED, COMPLETED, CANCELED
 *******************************************************************/
public class Waitlist_Controller {

    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Waitlist_Repository waitlistRepository = Waitlist_Repository.getInstance();

    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
        	case CANCEL_WAITLIST_AND_RESERVATION_BY_CODE: return CancelWitlistAndReservationByCode(msg);
        	case JOIN_WAITLIST: return joinWaitlist(msg);

            default: return null;
        }
    }
    
    
    private static Message joinWaitlist(Message msg) {
		Reservation createResForWaitlist = (Reservation)msg.getContent();
		if (waitlistRepository.isCustomerAlreadyInWaitlist(createResForWaitlist.getPhone(), createResForWaitlist.getEmail())) {
	        return new Message(MessageType.WAITLIST_JOINED_FAILED, "You are already in the waitlist!\nWait until we notify you that a seat has become available.");
	    }
		createResForWaitlist.setConfirmationCode(reservationRepository.getNextConfirmationCode());
		if (!reservationRepository.set(createResForWaitlist)) return new Message(MessageType.WAITLIST_JOINED_FAILED, "System Error: Unable to join Waitlist at this time.\nPlease try again later.");
		createResForWaitlist = reservationRepository.getByConfirmationCode(createResForWaitlist.getConfirmationCode());
		if (createResForWaitlist==null) return new Message(MessageType.WAITLIST_JOINED_FAILED, "System Error: Unable to join Waitlist at this time.\nPlease try again later.");
		Waitlist newWaitlist = new Waitlist(createResForWaitlist.getId());
		if (!waitlistRepository.set(newWaitlist)) {
			reservationRepository.deleteById(createResForWaitlist.getId());
			return new Message(MessageType.WAITLIST_JOINED_FAILED, "System Error: Unable to join Waitlist at this time.\nPlease try again later.");
		}
		return new Message(MessageType.WAITLIST_JOINED_SUCCESS, createResForWaitlist.getConfirmationCode());
	}

    
	private static Message CancelWitlistAndReservationByCode(Message msg) {
        int confirmationCode = (int) msg.getContent();
        //STEP 1- get reservation
        Reservation r = reservationRepository.getByConfirmationCode(confirmationCode);
        if (r == null) {
            return new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, "Invalid code, please try different confirmation code");
        }
        if (r.getStatus().equalsIgnoreCase("Completed") || r.getStatus().equalsIgnoreCase("Canceled")) {
            return new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, "Your reservation is already "+r.getStatus().toString() +"!");
        }
        if (r.getStatus().equalsIgnoreCase("Active")) {
            return new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, "Your reservation is in progress, you cannot cancel at this moment.");
        }

        //STEP 1- get waitlist (if exist) and verify that the waitlist is relevant to cancel
        boolean waitlistExists = false;
        boolean iswaitlistCanceled = false;
        Waitlist w = waitlistRepository.getByReservationId(r.getId());
        if (w!=null && (w.getStatus().equalsIgnoreCase("Completed") || w.getStatus().equalsIgnoreCase("Canceled"))) {
            return new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, "The waitlist is no longer exist in the system, or wrong code");
        }
        
        if (w != null) {
            waitlistExists = true;
            iswaitlistCanceled = waitlistRepository.cancelWaitlistById(w.getId());
            if (!iswaitlistCanceled) {
                return new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, "System error, please try again");
            }
        } 
        boolean reservationCanceled = reservationRepository.updateStatusByConfirmationCode(confirmationCode, ReservationStatus.CANCELED);
        if (reservationCanceled) {
            if (waitlistExists) {
                return new Message(MessageType.WAITLIST_AND_RESERVATION_CANCELED, "Your waitlist is canceled successfully");
            }
            return new Message(MessageType.WAITLIST_AND_RESERVATION_CANCELED, "Your reservation is canceled successfully");
        }
        return new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, "System error");
    }
    
	
	
    public static void onTableReleased(int capacity) {
        System.out.println("-> [WAITLIST_CONTROLLER] [Event] Table with " + " (" + capacity + " seats) is FREE. Checking Waitlist...");
        try {
            Waitlist candidate = waitlistRepository.findFirstMatch(capacity);     
            if (candidate != null) {
                System.out.println("->[WAITLIST_CONTROLLER] Match Found! WaitlistID: " + candidate.getId());              
                waitlistRepository.markAsNotified(candidate.getId());
                sendNotificationToCustomer(reservationRepository.getById(candidate.getReservation()));
            }
            
        } catch (Exception e) {
            System.err.println(" [TIMER] Error handling table release: " + e.getMessage());
            e.printStackTrace();
        }
    }

	private static void sendNotificationToCustomer(Reservation reservation) {
		if (reservation.getPhone()!=null) System.out.println("WAITLIST CONTROLLER - messaging number: "+ reservation.getPhone() + "with reminder");
		if (reservation.getUserId()==null) return;
		UserRecord user = User_Repository.getInstance().getByID(reservation.getUserId());
		String phone = user.getPhone();
		if (reservation.getEmail()==null || reservation.getEmail().isEmpty()) reservation.setEmail(user.getEmail());
		EmailService.sendTableReadyNotification(reservation);
		if (phone!=null && !phone.isEmpty()) System.out.println("WAITLIST CONTROLLER - messaging number: "+ phone + "with reminder");;

	}
}