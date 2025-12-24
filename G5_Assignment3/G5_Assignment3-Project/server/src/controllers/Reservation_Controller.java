package controllers;

import messages.Message;
import messages.MessageType;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;

import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;
import entities.Opening_Hours;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Restaurant;
import entities.Subscribed_Customer;
import entities.Waitlist;

/**
 * Controller responsible for handling all reservation-related logic on the server side.
 */
public class Reservation_Controller {

    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();
    private static final Waitlist_Repository WaitlistRepository = Waitlist_Repository.getInstance();

    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
            case CREATE_RESERVATION: return createReservation(msg);
            case UPDATE_RESERVATION_REQUEST: return updateReservation(msg);
            case GET_RESERVATIONS_BY_USER: return getReservationsByUser(msg);
            case CANCEL_RESERVATION: return cancelReservation(msg);
            case CANCEL_RESERVATION_BY_CODE: return cancelReservationByCode(msg);
            case GET_ALL_PENDING_RESERVATIONS: return fetchAllPending(msg);
            case ADMIN_UPDATE_RESERVATION: return processAdminUpdate(msg);
            case CHECK_IN_REQUEST: return handleCheckIn(msg);
            case GET_LATEST_RESERVATION_BY_PHONE: return getLatestReservationById(msg);
            default: return null;
        }
    }

    private static Message getLatestReservationById(Message msg) {
    	String phone = (String) msg.getContent();
        Reservation r = Reservation_Repository.getInstance().getLatestReservationByPhone(phone);
        return new Message(
                MessageType.RETURN_LATEST_RESERVATION_BY_PHONE,r);
	}

	private static Message createReservation(Message msg) {
        try {
            Reservation reservation = (Reservation) msg.getContent();
            LocalDateTime startTime = reservation.getOrderStartTime();
            LocalDateTime endTime = startTime.plusHours(2);
            
            // Check availability for requested time
            if (!tableRepository.isCapacityAvailable(startTime, endTime, reservation.getNumberOfDiners(), null)) {
                
                System.out.println("[Reservation_Controller] Slot " + startTime + " is full. Searching for alternative...");
                
                // Find nearest available 30-minute slot
                LocalDateTime suggestedTime = findNextAvailableSlot(startTime, reservation.getNumberOfDiners(), null);
                
                if (suggestedTime == null) {
                    System.out.println("[Reservation_Controller] No alternative slots found for today.");
                    return new Message(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED, null);
                }
                
                System.out.println("[Reservation_Controller] Suggesting: " + suggestedTime);
                return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggestedTime);
            }

            reservation.setTableId(null); 
            reservation.setOrderEndTime(endTime);
            reservation.setConfirmationCode(reservationRepository.getNextConfirmationCode());
            reservation.setStatus("Pending");

            if (reservationRepository.set(reservation)) {
                return new Message(MessageType.RESERVATION_CONFIRMED, reservation.getConfirmationCode());
            } else {
                return new Message(MessageType.RESERVATION_FAILED, "Database Error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Message(MessageType.RESERVATION_FAILED, "Server Error: " + e.getMessage());
        }
    }

    /**
     * Searches for the next available time slot if the requested time is full.
     * Increments the time by 30 minutes and checks capacity until closing time.
     * Fix: Handles "Midnight Crossing" AND "24-Hour Shifts" (e.g., 08:00 to 08:00).
     * * @param requestedTime The original time requested by the user.
     * @param guests The number of diners.
     * @param excludeId The ID to exclude (for updates).
     * @return The next available LocalDateTime or null if fully booked.
     */
    private static LocalDateTime findNextAvailableSlot(LocalDateTime requestedTime, int guests, Integer excludeId) {
        LocalDateTime suggestion = requestedTime;
        LocalDate date = requestedTime.toLocalDate();
        Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
        
        if (oh == null) {
            System.err.println("[Error] Opening Hours not loaded in Server.");
            return null; 
        }

        // 1. Get closing time
        LocalTime closingLocalTime = oh.getExceptionSchedule().containsKey(date) ?
            oh.getExceptionSchedule().get(date).getCloseTime() :
            oh.getRegularSchedule().get(date.getDayOfWeek()).getCloseTime();

        if (closingLocalTime == null) return null;

        // 2. Get opening time to compare
        LocalTime openingLocalTime = oh.getExceptionSchedule().containsKey(date) ?
                oh.getExceptionSchedule().get(date).getOpenTime() :
                oh.getRegularSchedule().get(date.getDayOfWeek()).getOpenTime();

        // 3. Set exact closing DateTime
        LocalDateTime closingDateTime = LocalDateTime.of(date, closingLocalTime);
        
        // If close time is BEFORE open time (e.g. 03:00 < 08:00) 
        // OR EQUAL to open time (e.g. 08:00 == 08:00, meaning 24 hours), 
        // it means the shift ends the next day.
        if (!closingLocalTime.isAfter(openingLocalTime)) {
            closingDateTime = closingDateTime.plusDays(1);
        }

        // 4. Search Loop
        // We add a small buffer (e.g., 1 minute) to suggestion to ensure we don't start checking 
        // exactly at closing time if the loop logic is tight.
        while (suggestion.isBefore(closingDateTime)) {
            suggestion = suggestion.plusMinutes(30); 
            
            // Validation: Ensure the meal (2 hours) finishes before or exactly at closing
            if (suggestion.plusHours(2).isAfter(closingDateTime)) {
                break; 
            }
            
            // Check capacity
            if (tableRepository.isCapacityAvailable(suggestion, suggestion.plusHours(2), guests, excludeId)) {
                return suggestion; 
            }
        }
        return null; 
    }

    private static Message updateReservation(Message msg) {
        try {
            Reservation updatedInfo = (Reservation) msg.getContent();
            LocalDateTime startTime = updatedInfo.getOrderStartTime();
            LocalDateTime endTime = startTime.plusHours(2);
            updatedInfo.setOrderEndTime(endTime);

            if (!tableRepository.isCapacityAvailable(startTime, endTime, updatedInfo.getNumberOfDiners(), updatedInfo.getId())) {
                LocalDateTime suggested = findNextAvailableSlot(startTime, updatedInfo.getNumberOfDiners(), updatedInfo.getId());
                return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggested);
            }

            updatedInfo.setTableId(null); 
            if (reservationRepository.update(updatedInfo)) {
                return new Message(MessageType.RESERVATION_UPDATE_SUCCESS, updatedInfo);
            }
            return new Message(MessageType.RESERVATION_UPDATE_FAILED, "Database Error");
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Server Error");
        }
    }

    private static Message handleCheckIn(Message msg) {
        try {
            int code = (int) msg.getContent();
            Reservation res = reservationRepository.getByConfirmationCode(code);
            
            if (res == null || !res.getStatus().equalsIgnoreCase(ReservationStatus.PENDING.toString())) {
                return new Message(MessageType.ERROR_RESPONSE, "Invalid status for check-in.");
            }

            Integer tableId = tableRepository.findBestAvailableTable(res.getOrderStartTime(), res.getOrderEndTime(), res.getNumberOfDiners());
            
            if (tableId != null) {
                res.setTableId(tableId);
                res.setStatus(ReservationStatus.ACTIVE.toString());
                reservationRepository.updateByEmployee(res);
                return new Message(MessageType.CHECK_IN_COMPLETED, tableId);
            } else {
                return new Message(MessageType.ERROR_RESPONSE, "No tables available now.");
            }
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Check-in failed.");
        }
    }

    private static Message getReservationsByUser(Message msg) {
        try {
            List<Reservation> reservations;
            if (msg.getContent() instanceof Subscribed_Customer) {
                reservations = reservationRepository.getByUserId(((Subscribed_Customer) msg.getContent()).getSubscriberCode());
            } else {
                reservations = reservationRepository.getByContactInfo((String) msg.getContent());
            }
            return new Message(MessageType.RETURN_RESERVATIONS_BY_USER, reservations);
        } catch (Exception e) { 
            return new Message(MessageType.ERROR_RESPONSE, e.getMessage()); 
        }
    }

    private static Message cancelReservation(Message msg) {
        int reservationId = (int) msg.getContent();
        boolean success = reservationRepository.updateStatusByID(reservationId, ReservationStatus.CANCELED);
        return new Message(success ? MessageType.RESERVATION_CANCELED : MessageType.RESERVATION_CANCEL_FAILED, reservationId);
    }
    private static Message cancelReservationByCode(Message msg) {
        int ConfirmationCode = (int) msg.getContent();
        Message succeed = new Message(MessageType.RESERVATION_CANCELED, ConfirmationCode);
        Message notSucceed = new Message(MessageType.RESERVATION_CANCEL_FAILED, ConfirmationCode);
        
        Reservation r= reservationRepository.getByConfirmationCode(ConfirmationCode);
        if (r==null || r.getStatus().equals("Completed") || r.getStatus().equals("Canceled")) return notSucceed;
        //boolean WasInWaitlist= WaitlistRepository.cancelByReservationId(int r.getId());
        boolean canceled = reservationRepository.updateStatusByConfirmationCode(ConfirmationCode, ReservationStatus.CANCELED);
        
        if (canceled) return succeed;
        return notSucceed;
    }

    private static Message fetchAllPending(Message msg) {
        return new Message(MessageType.RETURN_ALL_PENDING_RESERVATIONS, reservationRepository.getAllPendingReservations());
    }

    private static Message processAdminUpdate(Message msg) {
        Reservation updatedRes = (Reservation) msg.getContent();
        updatedRes.setOrderEndTime(updatedRes.getOrderStartTime().plusHours(2));
        return reservationRepository.updateByEmployee(updatedRes) ? 
            new Message(MessageType.ADMIN_UPDATE_SUCCESS, null) : 
            new Message(MessageType.ERROR_RESPONSE, "Update failed.");
    }
}