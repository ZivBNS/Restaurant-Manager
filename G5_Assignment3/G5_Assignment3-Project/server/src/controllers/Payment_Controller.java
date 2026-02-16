package controllers;

import java.time.LocalDateTime;
import java.util.List;

import Data.Bill_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;
import messages.Message;
import messages.MessageType;
import entities.Bill;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Restaurant_Table;

/**
 * Controller responsible for managing billing and payment processes.
 * It coordinates between bill records, reservation statuses, and table availability 
 * upon payment completion.
 */
public class Payment_Controller {

    private static final Bill_Repository billRepo = Bill_Repository.getInstance();
    private static final Reservation_Repository reservationRepo = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();

    /**
     * Main entry point for payment-related messages.
     * Routes incoming messages to specific handlers based on the MessageType.
     * * @param msg The message containing the payment-related command and data.
     * @return A Message object containing the result or response to the request.
     */
    public static Message handleMessage(Message msg) {

        if (msg == null || msg.getType() == null) {
            return new Message(MessageType.ERROR_RESPONSE, "Invalid payment message");
        }

        switch (msg.getType()) {
        	case BILL_REQUEST: return billRequest(msg);
        	case GET_BILL_BY_RESERVATION_ID: return getBillByReservationId(msg);
        	case BILL_PAYMENT_REQUEST: return billPayRequest(msg);
            case GET_ALL_BILLS: return new Message(MessageType.RETURN_ALL_BILLS, billRepo.getAllBills());
            case DELETE_BILL: return deleteBill(msg);
                 
            default:
                return new Message(MessageType.ERROR_RESPONSE, "Unknown payment command: " + msg.getType());
        }
    }

    /**
     * Deletes a specific bill from the repository and returns the updated list of all bills.
     * * @param msg Message containing the ID of the bill to delete.
     * @return A Message with the updated list of bills, or an error response if deletion fails.
     */
	private static Message deleteBill(Message msg) {
        int idToDelete = (int) msg.getContent();
        boolean deleted = billRepo.deleteById(idToDelete);
        if (deleted) {
        	List<Bill> bills = billRepo.getAllBills();
            return new Message(MessageType.RETURN_ALL_BILLS, bills);
        } else {
            return new Message(MessageType.ERROR_RESPONSE, "Failed to delete bill.");
        }
	}

    /**
     * Processes a bill payment request.
     * Marks the bill as paid, completes the associated reservation, and triggers 
     * waitlist notifications if table capacity is released. 
     * The arrival time is rounded to the nearest 30-minute interval for system consistency.
     * * @param msg Message containing the ID of the bill being paid.
     * @return A success message if processed, or a failure message if no open reservation is found.
     */
	private static Message billPayRequest(Message msg) {
        int billId = (int) msg.getContent();
        billRepo.markBillAsPaid(billId);
        int reservationId = billRepo.getReservationIdByBillId(billId);
        if (reservationId == -1) 
            return new Message(MessageType.BILL_PAYMENT_FAILED, "There is no open reservation");
        Reservation r=reservationRepo.getById(reservationId); 
        Restaurant_Table table = tableRepository.getById(r.getTableId());
        int actualTableCapacity = (table != null) ? table.getSize() : r.getNumberOfDiners();        reservationRepo.markReservationAsCompleted(reservationId);
        LocalDateTime now= LocalDateTime.now();
		int roundedMinutes = (now.getMinute() / 30) * 30; //make the time round(example- from 10:10-12:10 to 10:00-12:00)
		LocalDateTime newArrivalTime = now.withMinute(roundedMinutes).withSecond(0).withNano(0);
        if (tableRepository.isCapacityAvailable(newArrivalTime, newArrivalTime.plusHours(2), actualTableCapacity, null) )
        	Waitlist_Controller.onTableReleased(actualTableCapacity);
        return new Message(MessageType.BILL_PAYMENT_SUCCESS, null);
	}

    /**
     * Retrieves a bill based on the provided reservation ID.
     * * @param msg Message containing the reservation ID.
     * @return A Message object containing the found Bill, or an error if the ID is invalid.
     */
	private static Message getBillByReservationId(Message msg) {
        Integer reservationId = (Integer) msg.getContent();
        if (reservationId == null || reservationId <= 0) {
            return new Message(MessageType.ERROR_RESPONSE, "Invalid reservation id");
        }        
        Bill bill = billRepo.getBillByReservationId(reservationId);
        return new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, bill);
    }

    /**
     * Processes a request for a bill using a reservation confirmation code.
     * Validates that the reservation is currently ACTIVE before returning the bill.
     * * @param msg Message containing the reservation confirmation code.
     * @return A Message object with the Bill data, or a failure message if the code is invalid or not active.
     */
	private static Message billRequest(Message msg) {
        Integer reservationCode = (Integer) msg.getContent();
        Reservation r = reservationRepo.getByConfirmationCode(reservationCode);
        if (r==null || r.getId()<=0 || !r.getStatus().equals(ReservationStatus.ACTIVE.toString())) return new Message(MessageType.BILL_REQUEST_FAILED,"Your code is invalid");
        Bill bill = billRepo.getBillByReservationId(r.getId());
        return new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, bill);	
	}
}