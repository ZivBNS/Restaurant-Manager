package controllers;

import messages.Message;
import messages.MessageType;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;

import Data.Reservation_Repository;
import Data.Table_Repository;
import entities.Opening_Hours;
import entities.Reservation;
import entities.Restaurant;
import entities.Subscribed_Customer;

/**
 * Controller responsible for handling all reservation-related logic on the server side.
 * This class implements the "Total Capacity" logic, ensuring that physical tables
 * are not assigned until the customer actually arrives (Check-In).
 * * Logic Update: Prevents "Self-Collision" during updates by excluding the 
 * current reservation ID from the capacity calculation.
 */
public class Reservation_Controller {

    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();

    /**
     * Routes incoming messages from the server to the specific internal logic handlers.
     * * @param msg The message object received from the client.
     * @return A response Message object containing the result of the operation.
     */
    public static Message handleMessage(Message msg) {
        switch (msg.getType()) {
            case CREATE_RESERVATION:
                return createReservation(msg);
            case UPDATE_RESERVATION_REQUEST:
                return updateReservation(msg);
            case GET_RESERVATIONS_BY_USER:
                return getReservationsByUser(msg);
            case CANCEL_RESERVATION:
                return cancelReservation(msg);
            case GET_ALL_PENDING_RESERVATIONS:
                return fetchAllPending(msg);
            case ADMIN_UPDATE_RESERVATION:
                return processAdminUpdate(msg);
            case CHECK_IN_REQUEST: 
                return handleCheckIn(msg);
            default:
                return null;
        }
    }

    /**
     * Processes a new reservation request.
     * Checks availability using total capacity logic.
     * * @param msg The message containing the Reservation object to be created.
     * @return Message confirming success, suggesting a new time, or reporting failure.
     */
    private static Message createReservation(Message msg) {
        try {
            Reservation reservation = (Reservation) msg.getContent();
            LocalDateTime startTime = reservation.getOrderStartTime();
            LocalDateTime endTime = startTime.plusHours(2);
            
            // For new reservations, excludeId is null
            if (!tableRepository.isCapacityAvailable(startTime, endTime, reservation.getNumberOfDiners(), null)) {
                // Find nearest available 30-minute slot
                LocalDateTime suggestedTime = findNextAvailableSlot(startTime, reservation.getNumberOfDiners(), null);
                
                if (suggestedTime == null) {
                    return new Message(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED, null);
                }
                return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggestedTime);
            }

            // Implementation of Logical Seating: TableID is NULL
            reservation.setTableId(null); 
            reservation.setOrderEndTime(endTime);
            reservation.setConfirmationCode(reservationRepository.getNextConfirmationCode());
            reservation.setStatus("Pending");

            if (reservationRepository.set(reservation)) {
                return new Message(MessageType.RESERVATION_CONFIRMED, reservation.getConfirmationCode());
            } else {
                return new Message(MessageType.RESERVATION_FAILED, "Database Error: Could not save reservation.");
            }
        } catch (Exception e) {
            return new Message(MessageType.RESERVATION_FAILED, "Server Error: " + e.getMessage());
        }
    }

    /**
     * Searches for the next available time slot using capacity logic.
     * Fix: Includes Midnight Crossing logic and handles self-exclusion via excludeId.
     * * @param requestedTime The original time that was found to be full.
     * @param guests The number of diners requested.
     * @param excludeId The ID to exclude from calculations (used for updates).
     * @return The next available LocalDateTime or null if no slots are found.
     */
    private static LocalDateTime findNextAvailableSlot(LocalDateTime requestedTime, int guests, Integer excludeId) {
        LocalDateTime suggestion = requestedTime;
        LocalDate date = requestedTime.toLocalDate();
        Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
        
        if (oh == null) return null; 

        // Get closing time for the specific day
        LocalTime closingLocalTime = oh.getExceptionSchedule().containsKey(date) ?
            oh.getExceptionSchedule().get(date).getCloseTime() :
            oh.getRegularSchedule().get(date.getDayOfWeek()).getCloseTime();

        if (closingLocalTime == null) return null;

        // Convert to LocalDateTime for comparison
        LocalDateTime closingDateTime = LocalDateTime.of(date, closingLocalTime);
        
        // Handle Midnight Crossing (e.g., closing at 03:00 AM the next day)
        LocalTime openingLocalTime = oh.getExceptionSchedule().containsKey(date) ?
                oh.getExceptionSchedule().get(date).getOpenTime() :
                oh.getRegularSchedule().get(date.getDayOfWeek()).getOpenTime();
        
        if (closingLocalTime.isBefore(openingLocalTime)) {
            closingDateTime = closingDateTime.plusDays(1);
        }

        // Increment by 30 minutes until closing
        while (suggestion.isBefore(closingDateTime)) {
            suggestion = suggestion.plusMinutes(30); 
            
            // Check if a 2-hour meal fits before closing
            if (suggestion.plusHours(2).isAfter(closingDateTime)) {
                break; 
            }
            
            // Check capacity for the alternative slot, passing the excludeId
            if (tableRepository.isCapacityAvailable(suggestion, suggestion.plusHours(2), guests, excludeId)) {
                return suggestion; 
            }
        }
        return null; 
    }

    /**
     * Processes a request to update an existing reservation.
     * Fix: Passes the reservation ID to the capacity check to prevent self-collision.
     * * @param msg The message containing the updated Reservation details.
     * @return Message confirming success or providing an alternative suggestion.
     */
    private static Message updateReservation(Message msg) {
        try {
            Reservation updatedInfo = (Reservation) msg.getContent();
            LocalDateTime startTime = updatedInfo.getOrderStartTime();
            LocalDateTime endTime = startTime.plusHours(2);
            updatedInfo.setOrderEndTime(endTime);

            // Pass updatedInfo.getId() to ensure the system ignores current seats during calculation
            if (!tableRepository.isCapacityAvailable(startTime, endTime, updatedInfo.getNumberOfDiners(), updatedInfo.getId())) {
                LocalDateTime suggested = findNextAvailableSlot(startTime, updatedInfo.getNumberOfDiners(), updatedInfo.getId());
                return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggested);
            }

            updatedInfo.setTableId(null); // Keep logical assignment
            if (reservationRepository.update(updatedInfo)) {
                return new Message(MessageType.RESERVATION_UPDATE_SUCCESS, updatedInfo);
            }
            return new Message(MessageType.RESERVATION_UPDATE_FAILED, "Database Error: Update failed.");
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Server Error during update.");
        }
    }

    /**
     * Handles the physical table assignment when a customer arrives.
     * * @param msg Message containing the confirmation code.
     * @return Message with the assigned TableID or an error if no tables fit.
     */
    private static Message handleCheckIn(Message msg) {
        try {
            int code = (int) msg.getContent();
            Reservation res = reservationRepository.getByConfirmationCode(code);
            
            if (res == null || !res.getStatus().equalsIgnoreCase("Pending")) {
                return new Message(MessageType.ERROR_RESPONSE, "Invalid or already active reservation.");
            }

            // Assign the best (smallest) physical table available now
            Integer tableId = tableRepository.findBestAvailableTable(res.getOrderStartTime(), res.getOrderEndTime(), res.getNumberOfDiners());
            
            if (tableId != null) {
                res.setTableId(tableId);
                res.setStatus("Active");
                reservationRepository.updateByEmployee(res);
                return new Message(MessageType.CHECK_IN_COMPLETED, tableId);
            } else {
                return new Message(MessageType.ERROR_RESPONSE, "No suitable tables available at this moment.");
            }
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Check-in failed due to server error.");
        }
    }

    /**
     * Retrieves reservations associated with a specific user.
     * * @param msg Message containing the user identifier.
     * @return Message containing the list of matching Reservation objects.
     */
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
            return new Message(MessageType.ERROR_RESPONSE, "Error fetching user reservations."); 
        }
    }

    /**
     * Deletes a reservation from the system.
     * * @param msg Message containing the reservation ID to delete.
     * @return Message indicating whether cancellation was successful.
     */
    private static Message cancelReservation(Message msg) {
        int reservationId = (int) msg.getContent();
        if (reservationRepository.deleteById(reservationId)) {
            return new Message(MessageType.RESERVATION_CANCELED, reservationId);
        }
        return new Message(MessageType.RESERVATION_CANCEL_FAILED, reservationId);
    }

    /**
     * Fetches all reservations with a 'Pending' status for management purposes.
     * * @param msg Request message.
     * @return Message containing the list of pending reservations.
     */
    private static Message fetchAllPending(Message msg) {
        return new Message(MessageType.RETURN_ALL_PENDING_RESERVATIONS, reservationRepository.getAllPendingReservations());
    }

    /**
     * Processes an update request initiated by a restaurant employee.
     * * @param msg Message containing the reservation details to update.
     * @return Message indicating success or failure of the admin update.
     */
    private static Message processAdminUpdate(Message msg) {
        Reservation updatedRes = (Reservation) msg.getContent();
        updatedRes.setOrderEndTime(updatedRes.getOrderStartTime().plusHours(2));
        if (reservationRepository.updateByEmployee(updatedRes)) {
            return new Message(MessageType.ADMIN_UPDATE_SUCCESS, null);
        }
        return new Message(MessageType.ERROR_RESPONSE, "Administrative update failed.");
    }
}