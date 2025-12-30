package controllers;

import java.time.LocalDateTime;

import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Restaurant_Table;
import messages.Message;
import messages.MessageType;

public class CheckIn_Controller {
    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();
    private static final Waitlist_Repository WaitlistRepository = Waitlist_Repository.getInstance();

    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
        	case CHECK_IN_REQUEST: return checkInRequest(msg);
		default: return null;
        }
    }

	private static Message checkInRequest(Message msg) {
		int confCode= (int) msg.getContent();
		Restaurant_Table rt;
		Reservation reservation=reservationRepository.getByConfirmationCode(confCode);
        if (reservation == null || !reservation.getStatus().equalsIgnoreCase(ReservationStatus.PENDING.toString())) {
            return new Message(MessageType.CHECK_IN_UNMATCH_CODE, "Invalid status for check-in.");
        }
        LocalDateTime now= LocalDateTime.now();
		Integer tableID=tableRepository.findBestAvailableTable(now, now.plusHours(2), reservation.getNumberOfDiners());
		if (tableID!=null) {
			if (!reservationRepository.updateReservationForCheckIn(confCode, tableID, ReservationStatus.ACTIVE)) {
				return new Message(MessageType.CHECK_IN_FAIL,"update");
				}
		rt=tableRepository.getById(tableID);
        return new Message(MessageType.CHECK_IN_COMPLETED, rt.getTableNumber());
		}
        return new Message(MessageType.CHECK_IN_NO_TABLE);
	}

	
	
	
	/*//main for testing
	public static void main(String[] args) {
		int confCode=100104;
		boolean update1 =reservationRepository.updateReservationForCheckIn(confCode, 9, ReservationStatus.ACTIVE);
		//boolean update2 = reservationRepository.updateActualArrivalTimeOnly(100101, LocalDateTime.now());
		
		if (update1) System.out.println("SUCCEED");
		else System.out.println("faited");
	}*/
}
