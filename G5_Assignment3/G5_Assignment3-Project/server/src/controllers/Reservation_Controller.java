package controllers;

import messages.Message;
import messages.MessageType;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalTime;

import Data.Reservation_Repository;
import Data.Table_Repository;
import entities.Opening_Hours;
import entities.Reservation;
import entities.Restaurant;
import entities.Subscribed_Customer;

/**
 * Controller for handling reservation-related requests on the server.
 * Coordinates between table allocation and reservation data persistence.
 */
public class Reservation_Controller {

	private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
	private static final Table_Repository tableRepository = Table_Repository.getInstance();
	/**
	 * Main handler that routes incoming reservation messages to specific logic.
	 * * @param msg The incoming message from the client.
	 * 
	 * @return A response message to be sent back to the client.
	 */
	public static Message handleMessage(Message msg) {
		switch (msg.getType()) {
		case CREATE_RESERVATION:
			return createReservation(msg);
		case CANCEL_RESERVATION:
			return cancelReservation(msg);
		case GET_RESERVATIONS_BY_USER:
			return getReservationsByUser(msg);
		case UPDATE_RESERVATION_REQUEST:
			return updateReservation(msg);
		case GET_ALL_PENDING_RESERVATIONS:
            return fetchAllPending(msg);
        case ADMIN_UPDATE_RESERVATION:
            return processAdminUpdate(msg);
		default:
			System.out.println("Reservation_Controller: Unknown message type: " + msg.getType());
			return null;
		}
	}

	/**
	 * Processes a new reservation request and handles table allocation.
	 * If no table is found, it attempts to find a suggestion. 
	 * If no suggestion is found (null), it returns a specific failure message.
	 */
	private static Message createReservation(Message msg) {
	    try {
	        Reservation reservation = (Reservation) msg.getContent();
	        LocalDateTime startTime = reservation.getOrderStartTime();
	        LocalDateTime endTime = startTime.plusHours(2);
	        
	        Integer assignedTableId = tableRepository.findBestAvailableTable(startTime, endTime, reservation.getNumberOfDiners());

	        if (assignedTableId == null) {
	            LocalDateTime suggestedTime = findNextAvailableSlot(startTime, reservation.getNumberOfDiners());
	            
	            // Check if a suggestion was actually found
	            if (suggestedTime == null) {
	                return new Message(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED, "The restaurant is fully booked for the remainder of the day.");
	            }
	            
	            return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggestedTime);
	        }

	        reservation.setTableId(assignedTableId);
	        reservation.setOrderEndTime(endTime);
	        reservation.setConfirmationCode(reservationRepository.getNextConfirmationCode());

	        if (reservationRepository.set(reservation)) {
	            return new Message(MessageType.RESERVATION_CONFIRMED, reservation.getConfirmationCode());
	        } else {
	            return new Message(MessageType.RESERVATION_FAILED, "Database Error");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return new Message(MessageType.RESERVATION_FAILED, "Server Error");
	    }
	}

    /**
     * Iterates through future time slots in 30-minute increments to find availability.
     * Dynamic Closing: Fetches the closing time from the Restaurant entity.
     * @param requestedTime The original time requested by the user.
     * @param guests The number of diners to accommodate.
     * @return The next available LocalDateTime, or null if no slots are found that day.
     */
    private static LocalDateTime findNextAvailableSlot(LocalDateTime requestedTime, int guests) {
        LocalDateTime suggestion = requestedTime;
        
        // 1. Get the dynamic schedule loaded by OpeningHours_Repository
        Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
        if (oh == null) return null; 

        // 2. Determine the specific closing time for this specific date (Exception or Regular)
        LocalTime closingTime;
        if (oh.getExceptionSchedule().containsKey(requestedTime.toLocalDate())) {
            closingTime = oh.getExceptionSchedule().get(requestedTime.toLocalDate()).getCloseTime();
        } else {
            closingTime = oh.getRegularSchedule().get(requestedTime.getDayOfWeek()).getCloseTime();
        }

        if (closingTime == null) return null; // Restaurant is closed on this date

        // 3. Search for slots in 30-minute increments
        while (suggestion.toLocalTime().isBefore(closingTime)) {
            suggestion = suggestion.plusMinutes(30); 
            
            // Ensure the 2-hour meal fits before closing
            if (suggestion.toLocalTime().plusHours(2).isAfter(closingTime)) {
                break; 
            }
            
            LocalDateTime sugEnd = suggestion.plusHours(2);
            Integer tableId = tableRepository.findBestAvailableTable(suggestion, sugEnd, guests);
            
            if (tableId != null) {
                return suggestion; 
            }
        }
        return null; 
    }

	/**
	 * Fetches reservations associated with a specific user (Subscriber or Guest).
	 */
	private static Message getReservationsByUser(Message msg) {
		try {
			List<Reservation> reservations;
			Object content = msg.getContent();

			if (content instanceof Subscribed_Customer) {
				int subCode = ((Subscribed_Customer) content).getSubscriberCode();
				reservations = reservationRepository.getByUserId(subCode);
			} else if (content instanceof String) {
				reservations = reservationRepository.getByContactInfo((String) content);
			} else {
				return new Message(MessageType.ERROR_RESPONSE, "Invalid identifier.");
			}
			return new Message(MessageType.RETURN_RESERVATIONS_BY_USER, reservations);
		} catch (Exception e) {
			return new Message(MessageType.ERROR_RESPONSE, "Server Error: " + e.getMessage());
		}
	}

	/**
	 * Processes a request to update an existing reservation.
	 * It validates table availability for the new time/guests and searches for 
	 * suggestions if the requested slot is unavailable.
	 * * @param msg The message containing the updated Reservation entity.
	 * @return A response message indicating success, failure with a suggestion, or error.
	 */
	private static Message updateReservation(Message msg) {
	    try {
	        Reservation updatedInfo = (Reservation) msg.getContent();
	        
	        // 1. Calculate new end time (in case the start time was changed)
	        LocalDateTime startTime = updatedInfo.getOrderStartTime();
	        LocalDateTime endTime = startTime.plusHours(2); // Standard 2-hour duration
	        updatedInfo.setOrderEndTime(endTime);

	        // 2. Check if a table is available for the new time and guest count.
	        // This logic searches for an optimal table; if it's the same table already 
	        // assigned and it is free, it will be reassigned.
	        Integer assignedTableId = tableRepository.findBestAvailableTable(
	            startTime, endTime, updatedInfo.getNumberOfDiners()
	        );

	        // 3. If no table is available, search for the nearest alternative slot.
	        if (assignedTableId == null) {
	            LocalDateTime suggested = findNextAvailableSlot(startTime, updatedInfo.getNumberOfDiners());
	            // Return failure message with the suggested LocalDateTime as content
	            return new Message(MessageType.RESERVATION_FAILED_NO_TABLE, suggested);
	        }

	        // 4. Table found - update the assigned TableID and persist changes.
	        updatedInfo.setTableId(assignedTableId);
	        
	        if (reservationRepository.update(updatedInfo)) {
	            return new Message(MessageType.RESERVATION_UPDATE_SUCCESS, updatedInfo);
	        } else {
	            return new Message(MessageType.RESERVATION_UPDATE_FAILED, "Database Update Error");
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        return new Message(MessageType.ERROR_RESPONSE, "Server Error: " + e.getMessage());
	    }
	}

	/**
	 * Cancels/Deletes a reservation by its unique database ID.
	 */
	private static Message cancelReservation(Message msg) {
		try {
			int reservationId = (int) msg.getContent();
			boolean success = reservationRepository.deleteById(reservationId);
			MessageType type = success ? MessageType.RESERVATION_CANCELED : MessageType.RESERVATION_CANCEL_FAILED;
			return new Message(type, reservationId);
		} catch (Exception e) {
			return null;
		}
	}
	/**
	 * Fetches every reservation in the system with a 'PENDING' status.
	 * This provides the data for the Employee Management Dashboard.
	 */
	private static Message fetchAllPending(Message msg) {
	    try {
	        List<Reservation> pendingList = reservationRepository.getAllPendingReservations();
	        return new Message(MessageType.RETURN_ALL_PENDING_RESERVATIONS, pendingList);
	    } catch (Exception e) {
	        return new Message(MessageType.ERROR_RESPONSE, "Failed to fetch pending orders: " + e.getMessage());
	    }
	}

	/**
	 * Processes a full administrative update. Employees have the authority to 
	 * override table assignments, contact info, and reservation status.
	 */
	private static Message processAdminUpdate(Message msg) {
	    try {
	        Reservation updatedRes = (Reservation) msg.getContent();
	        
	        // Recalculate end time in case the start time was changed manually
	        updatedRes.setOrderEndTime(updatedRes.getOrderStartTime().plusHours(2));

	        // Use the repository method specifically designed for administrative overrides
	        if (reservationRepository.updateByEmployee(updatedRes)) {
	            System.out.println("Admin: Successfully updated reservation ID " + updatedRes.getId());
	            return new Message(MessageType.ADMIN_UPDATE_SUCCESS, null);
	        } else {
	            return new Message(MessageType.ERROR_RESPONSE, "Database update failed.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return new Message(MessageType.ERROR_RESPONSE, "Server logic error: " + e.getMessage());
	    }
	}
}