package controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import entities.WaitlistStatus;
import messages.Message;
import messages.MessageType;

/**
 * Controller class responsible for handling check-in operations. This class
 * processes check-in requests, verifies reservations, manages waitlists,
 * assigns tables, and initializes bills.
 */
public class CheckIn_Controller {
    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();
    private static final Waitlist_Repository WaitlistRepository = Waitlist_Repository.getInstance();
    private static final Bill_Repository billRepository = Bill_Repository.getInstance();
    /*******************************************************************
     * waitlist states are: PWAITING, WAITING, NOTIFIED, COMPLETED, CANCELED
     *******************************************************************/
    /**
     * Routes the incoming message to the appropriate check-in logic based on its type.
     * * @param msg The message received from the client.
     * @return A Message object containing the response or result of the check-in attempt.
     */
    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
        	case CHECK_IN_REQUEST: return checkInRequest(msg);
		default: return null;
        }
    }

	/**
	 * Processes a check-in request. 
	 * The process includes:
	 * 1. Verifying the confirmation code and reservation relevance.
	 * 2. Blocking waitlisted users who haven't been notified yet.
	 * 3. Searching for available capacity and assigning the best table.
	 * 4. Handling priority waitlist placement if the restaurant is full.
	 * 5. Initializing a new bill upon successful check-in.
	 * * @param msg Message containing the confirmation code.
	 * @return Message indicating CHECK_IN_COMPLETED with table ID on success, or CHECK_IN_FAIL with a reason.
	 */
	private static Message checkInRequest(Message msg) {
		int confCode= (int) msg.getContent();
		Restaurant_Table rt;
        LocalDateTime now= LocalDateTime.now();
		//step 1: verify that the reservation is relevant for the check in
        Reservation reservation=reservationRepository.getByConfirmationCode(confCode);
        if (reservation == null)
            return new Message(MessageType.CHECK_IN_FAIL, "The code is invalid, please try again");        
        if (reservation.getStatus().equalsIgnoreCase(ReservationStatus.ACTIVE.toString())) {
            return new Message(MessageType.CHECK_IN_FAIL, "Your reservation is already in progress!");
        }
        if (!reservation.getStatus().equalsIgnoreCase(ReservationStatus.PENDING.toString())) {
            return new Message(MessageType.CHECK_IN_FAIL, "Your reservation is no longer exist in the system, please try again");
        }
        if (reservation.getOrderStartTime().isBefore(now.withHour(0).withMinute(0).withSecond(0))) {
            return new Message(MessageType.CHECK_IN_FAIL, "Your reservation is for another day\nPlease use another confirmation code or make new reservation for check-in");
        }
        
        //step 2: block waitlist that not notified yet from enter the restaurant
		Waitlist w = WaitlistRepository.getByReservationId(reservation.getId());
        if (w!=null && !w.getStatus().equalsIgnoreCase(WaitlistStatus.NOTIFIED.toString())) 
        	return new Message(MessageType.CHECK_IN_FAIL, "There is no space available in the restaurant\nPlease wait until we notify you when seat has become available");
		
        //step 3: search for empty space in the misada
        Integer tableID=tableRepository.findBestAvailableTable(now, now.plusHours(2), reservation.getNumberOfDiners());
        if (!tableRepository.isCapacityAvailable(now, now.plusHours(2), reservation.getNumberOfDiners(), null)) tableID=null;
		if (tableID==null) {
			//if there is no place and the person reserved before, make him be first to be notified to get table(max 15 minutes wait)
			if (reservation.getOrderStartTime().isBefore(now.plusHours(1)) && reservation.getOrderStartTime().isAfter(now.minusMinutes(15))) { //האם הזמין ונמצא בין שעה לפני לרבע שעה אחרי שעת ההזמנה המקורית
				Waitlist priorityWaitlist= new Waitlist(reservation.getId());
				priorityWaitlist.setStatus(WaitlistStatus.PWAITING.toString());
				WaitlistRepository.set(priorityWaitlist);
				return new Message(MessageType.CHECK_IN_FAIL,"We are sorry for the delay, but the restaurant is full right now.\nYour table will be available in about fifteen minutes.\nWe will remind you when the table becomes available.");	
				}
			else return new Message(MessageType.CHECK_IN_FAIL,"the restaurant is full right now. You have reserved table for: "+reservation.getOrderStartTime().toLocalTime().toString()  +"\nIf you prefer, join waitlist or come back later. thank you");	
		}
		
		//if there is free table then update order status and table
		if (!reservationRepository.updateReservationForCheckIn(confCode, tableID, ReservationStatus.ACTIVE)) {
			return new Message(MessageType.CHECK_IN_FAIL,"System error");
			}
		
		//step 4: get the table with this ID,  RELEVANT ONLY IF WE SEPERATE TABLE ID AND TABLE NUMBER
		rt=tableRepository.getById(tableID);
		if (rt==null) return new Message(MessageType.CHECK_IN_FAIL,"System error");
		
		//step 5: create new bill after checked in
		boolean isBillCreated = createBillWhenCheckedInSuccesfully(reservation.getId(),reservation.getUserId()!=null);
		if (!isBillCreated) return new Message(MessageType.CHECK_IN_FAIL,"System error");
		
		//if there is waitlist related to this order or the order was ordered to long time ago
		if(w!=null || reservation.getOrderStartTime().minusHours(1).isAfter(now)) {
			if (w!=null) WaitlistRepository.updateStatusByReservationId(reservation.getId(), WaitlistStatus.COMPLETED.toString()); //waitlist ended
			int roundedMinutes = (now.getMinute() / 30) * 30; //make the time round(example- from 10:10-12:10 to 10:00-12:00)
			
			LocalDateTime newArrivalTime = now.withMinute(roundedMinutes).withSecond(0).withNano(0);
			reservation.setOrderStartTime(newArrivalTime);
			reservation.setOrderEndTime(newArrivalTime.plusHours(2));
			reservation.setStatus(ReservationStatus.ACTIVE.toString());
			reservationRepository.update(reservation);
		}
		//finished check in
        return new Message(MessageType.CHECK_IN_COMPLETED, tableID);
	}

	/**
	 * Creates a new bill record for the reservation upon successful check-in.
	 * Generates a random base amount for simulation purposes and adds fixed costs.
	 * * @param id           The reservation ID associated with the bill.
	 * @param isSubscriber Whether the customer is a registered subscriber.
	 * @return true if the bill was successfully saved in the repository, false otherwise.
	 */
	private static boolean createBillWhenCheckedInSuccesfully(int id, boolean isSubscriber) {
		Bill newBill = new Bill(id,isSubscriber);
	    double min = 0;
	    double max = 100.0;
	    double rawRandom = min + (Math.random() * (max - min));
	    double baseAmount = BigDecimal.valueOf(rawRandom).setScale(1, RoundingMode.HALF_UP).doubleValue();
	    newBill.setTotalAmount(baseAmount+5);
	    newBill.setStatus("Unpaid");
	    String details = String.format("Food & Drinks - %.1f₪\nTips - 3₪\nSitting - 2₪", baseAmount);
	    newBill.setBillDetails(details);

	    return billRepository.set(newBill);
	}
}