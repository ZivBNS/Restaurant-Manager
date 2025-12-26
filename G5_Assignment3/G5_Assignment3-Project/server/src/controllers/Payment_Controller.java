package controllers;

import Data.Bill_Repository;
import Data.Reservation_Repository;
import messages.Message;
import messages.MessageType;
import entities.Bill;
import entities.Reservation;

public class Payment_Controller {

    private static final Bill_Repository billRepo = Bill_Repository.getInstance();
    private static Reservation_Repository reservationRepo = Reservation_Repository.getInstance();


    /**
     * Main entry point for payment-related messages.
     */
    public static Message handleMessage(Message msg) {

        if (msg == null || msg.getType() == null) {
            return new Message(MessageType.ERROR_RESPONSE, "Invalid payment message");
        }

        switch (msg.getType()) {

            // -----------------------------------------------------------
            // Get bill by reservation id
            // Content: Integer reservationId
            // Response: RETURN_BILL_BY_RESERVATION_ID (Bill or null)
            // -----------------------------------------------------------
            case GET_BILL_BY_RESERVATION_ID: {
                Integer reservationId = (Integer) msg.getContent();

                if (reservationId == null || reservationId <= 0) {
                    return new Message(MessageType.ERROR_RESPONSE, "Invalid reservation id");
                }
                
                System.out.println("SERVER: fetching bill for reservationId = " + reservationId);
                Bill bill = billRepo.getBillByReservationId(reservationId);
                System.out.println("SERVER: bill found = " + bill);
                return new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, bill);
            }

            // -----------------------------------------------------------
            // Optional: mark bill as paid
            // Content: Integer billId
            // Response: BILL_PAYMENT_SUCCESS / BILL_PAYMENT_FAILED
            // -----------------------------------------------------------
            case BILL_PAYMENT_REQUEST:

                int billId = (int) msg.getContent();

                billRepo.markBillAsPaid(billId);

                int reservationId = billRepo.getReservationIdByBillId(billId);

                if (reservationId != -1) {
                	reservationRepo.markReservationAsCompleted(reservationId);
                }

                return new Message(MessageType.BILL_PAYMENT_SUCCESS, null);


            default:
                return new Message(MessageType.ERROR_RESPONSE, "Unknown payment command: " + msg.getType());
        }
    }
    
    
}

