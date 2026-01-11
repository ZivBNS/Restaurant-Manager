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
import entities.ReservationStatus;
import entities.Restaurant;
import entities.UserRecord;

/**
 * Controller responsible for handling all reservation-related logic on the server side.
 */
public class Reservation_Controller {

    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Table_Repository tableRepository = Table_Repository.getInstance();

    public static Message handleMessage(Message msg) {
    	
        switch (msg.getType()) {
        	case CREATE_INSTANT_RESERVATION: return createInstantReservation(msg);
            case CREATE_RESERVATION: return createReservation(msg);
            case UPDATE_RESERVATION_REQUEST: return updateReservation(msg);
            case GET_RESERVATIONS_BY_USER: return getReservationsByUser(msg);
            case CANCEL_RESERVATION: return cancelReservation(msg);
            case GET_ALL_PENDING_RESERVATIONS: return fetchAllPending(msg);
            case GET_ALL_PENDING_AND_ACTIVE_RESERVATIONS: return fetchAllPendingAndActive(msg);
            case ADMIN_UPDATE_RESERVATION: return processAdminUpdate(msg);
            case GET_LATEST_RESERVATION_BY_PHONE: return getLatestReservationById(msg);
            case GET_LATEST_RESERVATION_BY_EMAIL: return getLatestReservationByEmail(msg);
            case GET_RESERVATION_HISTORY: return getReservationHistory(msg);
            default: return null;
        }
    }

    private static Message getLatestReservationById(Message msg) {
    	String phone = (String) msg.getContent();
        Reservation r = Reservation_Repository.getInstance().getLatestReservationByPhone(phone);
        return new Message(
                MessageType.RETURN_LATEST_RESERVATION_BY_PHONE,r);
	}
    private static Message getLatestReservationByEmail(Message msg) {
        String email = (String) msg.getContent();
        System.out.println("[SERVER DEBUG] Incoming request: GET_LATEST_RESERVATION_BY_EMAIL for: " + email);
        
        Reservation r = Reservation_Repository.getInstance().getLatestReservationByEmail(email);
        
        if (r != null) {
            System.out.println("[SERVER DEBUG] Success! Found Reservation ID: " + r.getId() + " for email: " + email);
        } else {
            System.out.println("[SERVER DEBUG] Failed! No ACTIVE reservation found in DB for: " + email);
        }
        
        return new Message(MessageType.RETURN_LATEST_RESERVATION_BY_EMAIL, r);
    }
    
	private static Message createInstantReservation(Message msg) {
		
		Message createInstantReservation = createReservation(msg);
		if (createInstantReservation.getType().equals(MessageType.RESERVATION_CONFIRMED)) {
			return new Message(MessageType.INSTANT_RESERVATION_SUCCESS,createInstantReservation.getContent());			
		}	
		if (createInstantReservation.getType().equals(MessageType.RESERVATION_FAILED_NO_TABLE) || createInstantReservation.getType().equals(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED))
			return new Message(MessageType.INSTANT_RESERVATION_FAILED,null);
		return new Message(MessageType.INSTANT_RESERVATION_FAILED,createInstantReservation.getContent());
	}


	/**
     * Handles the creation of a new reservation for both Casual and Subscribed customers.
     * Includes logic to identify subscribers via userId and ensure data integrity.
     * * @param msg The message containing the Reservation object from the client.
     * @return A Message indicating success (with confirmation code) or failure.
     */
    private static Message createReservation(Message msg) {
        try {
            // 1. Validation: Check if the content is valid
            if (!(msg.getContent() instanceof Reservation)) {
                System.err.println("[Error] Message content is NOT a Reservation object.");
                return new Message(MessageType.RESERVATION_FAILED, "Invalid data format received.");
            }
     
            Reservation reservation = (Reservation) msg.getContent();
            
            // If the reservation has a UserID, treat it as a Subscriber booking.

            // 2. Time Slot Calculation
            LocalDateTime startTime = reservation.getOrderStartTime();
            // Default dining duration is 2 hours
            LocalDateTime endTime = startTime.plusHours(2);
            
            // 3. must check: if the person invite more orders to the same time - result: block him
//            List<Reservation> checkReservations = tableRepository.getOverlappingReservationsList(startTime, endTime, reservation.getId());
//            for (Reservation r:checkReservations) {
//                boolean sameUserId = (r.getUserId() != null && r.getUserId().equals(reservation.getUserId()));
//                boolean sameEmail = (r.getEmail() != null && !r.getEmail().isEmpty() && r.getEmail().equalsIgnoreCase(reservation.getEmail()));                
//                boolean samePhone = (r.getPhone() != null && !r.getPhone().isEmpty() && r.getPhone().equals(reservation.getPhone()));
//                if (sameUserId || sameEmail || samePhone)
//                	return new Message(MessageType.RESERVATION_FAILED_ALREADY_BOOKED, "It looks like you're already booked with us for this time!\nPlease use your existing confirmation code, or cancel the previous reservation to make a new one.");
//            }
            

            // 4. Capacity Check
            // Check availability for requested time using the Table Repository
            if (!tableRepository.isCapacityAvailable(startTime, endTime, reservation.getNumberOfDiners(), null)) {
                
                System.out.println("[Reservation_Controller] Slot " + startTime + " is full. Searching for alternative...");
                
                // Smart Feature: Find nearest available 30-minute slot
                LocalDateTime suggestedTime = findNextAvailableSlot(startTime, reservation.getNumberOfDiners(), null);
                
                if (suggestedTime == null) {
                    System.out.println("[Reservation_Controller] No alternative slots found for today.");
                    return new Message(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED, null);
                }
                System.out.println("[Reservation_Controller] Suggesting alternative: " + suggestedTime);
                return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggestedTime);
            }

            // 5. Finalize Reservation Data
            // Logical Seating: TableID is NULL until actual arrival (Check-in)
            reservation.setTableId(null); 
            reservation.setOrderEndTime(endTime);
            reservation.setConfirmationCode(reservationRepository.getNextConfirmationCode());
            reservation.setStatus("Pending");
            
            //mid check: if invited less then 2 hours from closing then start his time from helf hour down
            if (reservation.getOrderStartTime().toLocalTime().isAfter(Restaurant.getInstance().getOpeningHours().getRegularSchedule().get(LocalDateTime.now().getDayOfWeek()).getCloseTime().minusMinutes(90))) {
            	int mnt = LocalDateTime.now().getMinute() / 30 * 30;
            	reservation.setOrderStartTime(LocalDateTime.now().withMinute(mnt).withSecond(0).withNano(0));
            	reservation.setOrderEndTime(LocalDateTime.now().withMinute(mnt).withSecond(0).withNano(0).plusHours(2));
            }
            
            // 6. Asynchronous Notification
            // Check if email exists before trying to send to prevent errors
            if (reservation.getEmail() != null && !reservation.getEmail().isEmpty()) {
                System.out.println("[Email Service] Sending Confirmation Email to: " + reservation.getEmail());
                integration.EmailService.sendConfirmationEmail(reservation);
            } else {
                System.out.println("[Warning] No email provided. Skipping notification.");
            }
            
            // 7. Database Persistence
            if (reservationRepository.set(reservation)) {
                System.out.println("[Success] Reservation created. Code: " + reservation.getConfirmationCode());
                return new Message(MessageType.RESERVATION_CONFIRMED, reservation.getConfirmationCode());
            } else {
                System.err.println("[Database Error] Failed to insert reservation.");
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

    private static Message getReservationsByUser(Message msg) {
        try {
            List<Reservation> reservations;
            Object content = msg.getContent();

            if (content instanceof Integer) {
                reservations = reservationRepository.getByUserId((int) content);
            } 
            else if (content instanceof UserRecord) {
                UserRecord user = (UserRecord) content;
                reservations = reservationRepository.getByUserId(user.getId());
            }
            else {
                reservations = reservationRepository.getByContactInfo((String) content);
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

    private static Message fetchAllPending(Message msg) {
        return new Message(MessageType.RETURN_ALL_PENDING_RESERVATIONS, reservationRepository.getAllPendingReservations());
    }
    private static Message fetchAllPendingAndActive(Message msg) {
        return new Message(MessageType.GET_ALL_PENDING_AND_ACTIVE_RESERVATIONS, reservationRepository.getPendingAndActiveReservations());
    }

    private static Message processAdminUpdate(Message msg) {
        Reservation updatedRes = (Reservation) msg.getContent();
        updatedRes.setOrderEndTime(updatedRes.getOrderStartTime().plusHours(2));
        return reservationRepository.updateByEmployee(updatedRes) ? 
            new Message(MessageType.ADMIN_UPDATE_SUCCESS, null) : 
            new Message(MessageType.ERROR_RESPONSE, "Update failed.");
    }
    /**
     * Handles the request to fetch reservation history for a user.
     * * @param msg The message containing the User ID (Integer).
     * @return A message containing the list of reservations.
     */
    private static Message getReservationHistory(Message msg) {
        try {
            int userId = (int) msg.getContent();
            List<Reservation> history = reservationRepository.getHistoryByUserId(userId);
            return new Message(MessageType.RETURN_RESERVATION_HISTORY, history);
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Failed to fetch history: " + e.getMessage());
        }
    }
}