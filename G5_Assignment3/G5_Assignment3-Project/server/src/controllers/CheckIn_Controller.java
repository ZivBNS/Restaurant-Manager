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
import entities.Waitlist;
import messages.Message;
import messages.MessageType;
/*******************************************************************
 * waitlist states are: PWAITING, WAITING, NOTIFIED, COMPLETED, CANCELED
 *******************************************************************/
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
		//step 1: verify that the reservation is relevant for the check in
        Reservation reservation=reservationRepository.getByConfirmationCode(confCode);
        if (reservation == null || !reservation.getStatus().equalsIgnoreCase(ReservationStatus.PENDING.toString()) || reservation.getOrderStartTime().isBefore(now.minusHours(2))) {
            return new Message(MessageType.CHECK_IN_UNMATCH_CODE, reservation);
        }
        //step 2: block waitlist that not notified yet from enter the restaurant
		Waitlist w = WaitlistRepository.getByReservationId(reservation.getId());
        if (w!=null && !w.getStatus().equalsIgnoreCase("NOTIFIED")) return new Message(MessageType.CHECK_IN_FROM_WAITLIST_FAIL, reservation);
		//step 3: search for empty space in the misada
        Integer tableID=tableRepository.findBestAvailableTable(now, now.plusHours(2), reservation.getNumberOfDiners());
		if (tableID==null) return new Message(MessageType.CHECK_IN_NO_TABLE);
		//if there is free table then update order status and table
		if (!reservationRepository.updateReservationForCheckIn(confCode, tableID, ReservationStatus.ACTIVE)) {
			return new Message(MessageType.CHECK_IN_FAIL,"update");
			}
		//step 4: get the table with this ID,  RELEVANT ONLY IF WE SEPERATE TABLE ID AND TABLE NUMBER
		/*rt=tableRepository.getById(tableID);
		if (rt==null) return new Message(MessageType.CHECK_IN_FAIL,"update");*/
		//step 5: create new bill after checked in
		boolean isBillCreated = createBillWhenCheckedInSuccesfully(reservation.getId(),reservation.getUserId()!=null);
		if (!isBillCreated) return new Message(MessageType.CHECK_IN_FAIL,"bill");
		
		//if there is waitlist related to this order or the order was ordered to long time ago
		if(w!=null || reservation.getOrderStartTime().isBefore(now.minusHours(1))) {
			if (w!=null) WaitlistRepository.updateStatusByReservationId(reservation.getId(), "COMPLETED");//waitlist ended
			int roundedMinutes = (now.getMinute() / 30) * 30; //make the time round(example- from 10:10-12:10 to 10:00-12:00)
			LocalDateTime newArrivalTime = now.withMinute(roundedMinutes).withSecond(0).withNano(0);
			reservation.setOrderStartTime(newArrivalTime);
			reservation.setOrderEndTime(newArrivalTime.plusHours(2));
			reservationRepository.update(reservation);
		}
		//finished check in
        return new Message(MessageType.CHECK_IN_COMPLETED, tableID);
	}

	private static boolean createBillWhenCheckedInSuccesfully(int id, boolean isSubscriber) {
			Bill newBill = new Bill(id,isSubscriber);
			return billRepository.set(newBill);
	}
}
