package controllers;

import Data.Bill_Repository;
import messages.Message;
import messages.MessageType;
import entities.Bill;

public class Payment_Controller {

    private static final Bill_Repository billRepo = Bill_Repository.getInstance();

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

                Bill bill = billRepo.getBillByReservationId(reservationId);
                return new Message(MessageType.RETURN_BILL_BY_RESERVATION_ID, bill);
            }

            // -----------------------------------------------------------
            // Optional: mark bill as paid
            // Content: Integer billId
            // Response: BILL_PAYMENT_SUCCESS / BILL_PAYMENT_FAILED
            // -----------------------------------------------------------
            case BILL_PAYMENT_REQUEST: {
                Integer billId = (Integer) msg.getContent();

                if (billId == null || billId <= 0) {
                    return new Message(MessageType.BILL_PAYMENT_FAILED, "Invalid bill id");
                }

                boolean ok = billRepo.markBillAsPaid(billId); // implement if you want
                return ok
                    ? new Message(MessageType.BILL_PAYMENT_SUCCESS, null)
                    : new Message(MessageType.BILL_PAYMENT_FAILED, "Failed to mark bill as paid");
            }

            default:
                return new Message(MessageType.ERROR_RESPONSE, "Unknown payment command: " + msg.getType());
        }
    }
    
    
}

