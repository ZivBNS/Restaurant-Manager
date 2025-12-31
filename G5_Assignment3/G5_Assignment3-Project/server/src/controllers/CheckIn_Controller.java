package controllers;

import java.time.LocalDateTime;

import Data.Bill_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;
import entities.Bill;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Restaurant_Table;
import messages.Message;
import messages.MessageType;

public class CheckIn_Controller {
    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();
    private static final Waitlist_Repository WaitlistRepository = Waitlist_Repository.getInstance();
    private static final Bill_Repository billRepository = Bill_Repository.getInstance();

    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
        	case CHECK_IN_REQUEST: return checkInRequest(msg);
		default: return null;
        }
    }

	private static Message checkInRequest(Message msg) {
		int confCode= (int) msg.getContent();
		Restaurant_Table rt;
        LocalDateTime now= LocalDateTime.now();
		Reservation reservation=reservationRepository.getByConfirmationCode(confCode);
        if (reservation == null || !reservation.getStatus().equalsIgnoreCase(ReservationStatus.PENDING.toString()) || reservation.getOrderStartTime().isBefore(now.minusHours(2))) {
            return new Message(MessageType.CHECK_IN_UNMATCH_CODE, reservation);
        }
		Integer tableID=tableRepository.findBestAvailableTable(now, now.plusHours(2), reservation.getNumberOfDiners());
		if (tableID==null) return new Message(MessageType.CHECK_IN_NO_TABLE);
		
		if (!reservationRepository.updateReservationForCheckIn(confCode, tableID, ReservationStatus.ACTIVE)) {
			return new Message(MessageType.CHECK_IN_FAIL,"update");
			}
		rt=tableRepository.getById(tableID);
		if (rt==null) return new Message(MessageType.CHECK_IN_FAIL,"update");
		boolean isBillCreated = createBillWhenCheckedInSuccesfully(reservation.getId(),reservation.getUserId()!=null);
		if (!isBillCreated) return new Message(MessageType.CHECK_IN_FAIL,"bill");
        return new Message(MessageType.CHECK_IN_COMPLETED, rt.getTableNumber());
	}

	private static boolean createBillWhenCheckedInSuccesfully(int id, boolean isSubscriber) {
			Bill newBill = new Bill(id,isSubscriber);
			return billRepository.set(newBill);
	}
}
