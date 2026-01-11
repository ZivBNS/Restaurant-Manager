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

public class Payment_Controller {

    private static final Bill_Repository billRepo = Bill_Repository.getInstance();
    private static final Reservation_Repository reservationRepo = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();



    /**
     * Main entry point for payment-related messages.
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

	private static Message billPayRequest(Message msg) {
        int billId = (int) msg.getContent();
        billRepo.markBillAsPaid(billId);
        int reservationId = billRepo.getReservationIdByBillId(billId);
        if (reservationId == -1) 
            return new Message(MessageType.BILL_PAYMENT_FAILED, "There is no open reservation");
        Reservation r=reservationRepo.getById(reservationId); 
        int dinersFreed=r.getNumberOfDiners();
        reservationRepo.markReservationAsCompleted(reservationId);
        LocalDateTime now= LocalDateTime.now();
		int roundedMinutes = (now.getMinute() / 30) * 30; //make the time round(example- from 10:10-12:10 to 10:00-12:00)
		LocalDateTime newArrivalTime = now.withMinute(roundedMinutes).withSecond(0).withNano(0);
        if (tableRepository.findBestAvailableTable(now, newArrivalTime.plusHours(2), dinersFreed) !=null)
        	Waitlist_Controller.onTableReleased(r.getNumberOfDiners());      
        return new Message(MessageType.BILL_PAYMENT_SUCCESS, null);
	}


	private static Message getBillByReservationId(Message msg) {
        Integer reservationId = (Integer) msg.getContent();
        if (reservationId == null || reservationId <= 0) {
            return new Message(MessageType.ERROR_RESPONSE, "Invalid reservation id");
        }        
        Bill bill = billRepo.getBillByReservationId(reservationId);
        return new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, bill);
    }


	private static Message billRequest(Message msg) {
        Integer reservationCode = (Integer) msg.getContent();
        Reservation r = reservationRepo.getByConfirmationCode(reservationCode);
        if (r==null || r.getId()<=0 || !r.getStatus().equals(ReservationStatus.ACTIVE.toString())) return new Message(MessageType.BILL_REQUEST_FAILED,"Your code is invalid");
        Bill bill = billRepo.getBillByReservationId(r.getId());
        return new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, bill);	
	}
}

