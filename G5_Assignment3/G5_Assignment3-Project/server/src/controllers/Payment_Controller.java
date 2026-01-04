package controllers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import Data.Bill_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;
import messages.Message;
import messages.MessageType;
import entities.Bill;
import entities.Reservation;
import entities.ReservationStatus;

public class Payment_Controller {

    private static final Bill_Repository billRepo = Bill_Repository.getInstance();
    private static final Reservation_Repository reservationRepo = Reservation_Repository.getInstance();
    private static final Waitlist_Repository waitlistRepo = Waitlist_Repository.getInstance();
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
        	        	
            // -----------------------------------------------------------
            // Get bill by reservation id
            // Content: Integer reservationId
            // Response: RETURN_BILL_BY_RESERVATION_ID (Bill or null)
            // -----------------------------------------------------------
            case GET_BILL_BY_RESERVATION_ID: return getBillByReservationId(msg);

            // -----------------------------------------------------------
            // Optional: mark bill as paid
            // Content: Integer billId
            // Response: BILL_PAYMENT_SUCCESS / BILL_PAYMENT_FAILED
            // -----------------------------------------------------------
            case BILL_PAYMENT_REQUEST: return billPayRequest(msg);

                                                                            //billRepo.getAllBills() return type: List<Bill>
             case GET_ALL_BILLS: return new Message(MessageType.RETURN_ALL_BILLS, billRepo.getAllBills());

             case CREATE_BILL: return createBill(msg);
                 
             case DELETE_BILL: return deleteBill(msg);
                 
            default:
                return new Message(MessageType.ERROR_RESPONSE, "Unknown payment command: " + msg.getType());
        }
    }


	private static Message deleteBill(Message msg) {
        int idToDelete = (int) msg.getContent();
        boolean deleted = billRepo.deleteById(idToDelete);
        if (deleted) {
            
            return new Message(MessageType.RETURN_ALL_BILLS, billRepo.getAllBills());
        } else {
            return new Message(MessageType.ERROR_RESPONSE, "Failed to delete bill.");
        }
	}


	private static Message createBill(Message msg) {
	    Bill tempBill = (Bill) msg.getContent();
	    int inputCode = tempBill.getReservationId(); 

	    System.out.println("[SERVER DEBUG] Received Discount from Client: " + tempBill.getDiscountRate());

	    Reservation res = reservationRepo.getByConfirmationCode(inputCode);
	    if (res == null) res = reservationRepo.getById(inputCode);
	    if (res == null) return new Message(MessageType.ERROR_RESPONSE, "Order not found!");

	    Bill existingBill = billRepo.getBillByReservationId(res.getId());
	    
	    if (existingBill != null) {
	        // קריטי: מעדכנים את האובייקט שיישלח ל-Repository
	        existingBill.setTotalAmount(tempBill.getTotalAmount());
	        existingBill.setBillDetails(tempBill.getBillDetails());
	        existingBill.setDiscountRate(tempBill.getDiscountRate()); // השורה הזו הייתה חסרה!
	        existingBill.setStatus("Unpaid");

	        boolean updated = billRepo.updateBillData(existingBill);
	        return updated ? new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, existingBill) 
	                       : new Message(MessageType.ERROR_RESPONSE, "Failed to update MySQL.");
	    }

	    // יצירת חשבון חדש אם לא קיים
	    Bill finalBill = new Bill(res.getId(), res.getId(), tempBill.getBillDetails(), 
	                              tempBill.getTotalAmount(), "Unpaid", tempBill.getDiscountRate());

	    boolean created = billRepo.set(finalBill);
	    return created ? new Message(MessageType.RETURN_ALL_BILLS, billRepo.getAllBills()) 
	                   : new Message(MessageType.ERROR_RESPONSE, "Database Error");
	}

	private static Message billPayRequest(Message msg) {
        int billId = (int) msg.getContent();
        billRepo.markBillAsPaid(billId);
        int reservationId = billRepo.getReservationIdByBillId(billId);
        if (reservationId == -1) 
            return new Message(MessageType.BILL_PAYMENT_FAILED, null);
        Reservation r=reservationRepo.getById(reservationId);    	
        reservationRepo.markReservationAsCompleted(reservationId);
        LocalDateTime now= LocalDateTime.now();
        if (tableRepository.findBestAvailableTable(now, now.plusHours(2), r.getNumberOfDiners())!=null)
        	Waitlist_Controller.onTableReleased(r.getNumberOfDiners());      
        return new Message(MessageType.BILL_PAYMENT_SUCCESS, null);
	}


	private static Message getBillByReservationId(Message msg) {
        Integer reservationId = (Integer) msg.getContent();

        if (reservationId == null || reservationId <= 0) {
            return new Message(MessageType.ERROR_RESPONSE, "Invalid reservation id");
        }
        
        System.out.println("SERVER: fetching bill for reservationId = " + reservationId);
        Bill bill = billRepo.getBillByReservationId(reservationId);
        System.out.println("SERVER: bill found = " + bill);
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

