package controllers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import Data.OpeningHours_Repository;
import Data.Reservation_Repository;
import entities.Opening_Hours;
import entities.Restaurant;
import messages.Message;
import messages.MessageType;

/**
 * Controller responsible for handling logic related to restaurant opening
 * hours. It interfaces between the network commands and the stored restaurant
 * entities. Handles retrieval, updating regular hours, adding special events,
 * and deactivating slots.
 * * <p>Update: Includes validation against existing reservations to prevent
 * operational conflicts.</p>
 */
public class OpeningHours_Controller {

    /**
     * Main entry point for handling opening hours messages. Routes the message to
     * specific logic based on the MessageType.
     * * @param msg The message received from the client.
     * @return A response message containing the requested data or operation status.
     */
    public static Message handle(Message msg) {
        switch (msg.getType()) {
            case GET_OPENING_HOURS: return getOpeningHours();
            case UPDATE_REGULAR_HOURS: return handleBatchUpdate(msg);
            case ADD_SPECIAL_HOUR: return handleAddSpecialHour(msg);
            case DELETE_SPECIAL_HOUR: return handleDeleteSpecialHour(msg);
            default:
                return new Message(MessageType.ERROR_RESPONSE, "Unknown Opening Hours request.");
        }
    }

    /**
     * Retrieves the current opening hours from the Restaurant singleton. The
     * repository populates this singleton during system initialization.
     * * @return A message containing the Opening_Hours object or an error message.
     */
    private static Message getOpeningHours() {
        Opening_Hours hours = Restaurant.getInstance().getOpeningHours();
        if (hours != null)
            return new Message(MessageType.RETURN_OPENING_HOURS, hours);
        return new Message(MessageType.ERROR_RESPONSE, "Data unavailable");
    }

    /**
     * Handles a batch update message containing data for all days.
     * <p><strong>Validation:</strong> Checks if any proposed change conflicts with
     * existing future reservations (e.g., closing a day that has bookings).</p>
     * * @param msg Message containing a Map of DayOfWeek to [OpenTime, CloseTime, IsActive].
     * @return Success message with updated data, or Error message with conflict details.
     */
    private static Message handleBatchUpdate(Message msg) {
        try {
            @SuppressWarnings("unchecked")
            Map<DayOfWeek, Object[]> batchData = (Map<DayOfWeek, Object[]>) msg.getContent();
            
            // --- VALIDATION STEP ---
            Reservation_Repository resRepo = Reservation_Repository.getInstance();
            
            // Iterate through the proposed changes to check for conflicts
            for (Map.Entry<DayOfWeek, Object[]> entry : batchData.entrySet()) {
                DayOfWeek day = entry.getKey();
                LocalTime open = (LocalTime) entry.getValue()[0];
                LocalTime close = (LocalTime) entry.getValue()[1];
                boolean active = (Boolean) entry.getValue()[2];

                // Query the Reservation Repository to see if this change affects active bookings
                LocalDate conflictDate = resRepo.findConflictForRegularUpdate(day, open, close, active);
                
                if (conflictDate != null) {
                    return new Message(MessageType.OPENING_HOURS_UPDATE_CONFLICT_ERROR, 
                        "Cannot update " + day + " hours.\n" +
                        "Existing reservations conflict with this change.\n" +
                        "Latest conflict found on: " + conflictDate);
                }
            }
            // -----------------------

            // Proceed with update if validation passes
            boolean success = OpeningHours_Repository.getInstance().updateAllDays(batchData);
            
            if (success) {
                // Reload data into Singleton before returning to ensure client gets fresh data
                OpeningHours_Repository.getInstance().init();
                return new Message(MessageType.RETURN_OPENING_HOURS, Restaurant.getInstance().getOpeningHours());
            }
            return new Message(MessageType.ERROR_RESPONSE, "Batch update failed in DB.");
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Data formatting error.");
        }
    }

    /**
     * Handles the request to add a special hour exception (e.g., holiday or event).
     * <p><strong>Validation:</strong> Checks if there are reservations on the specific date
     * that fall outside the new special hours.</p>
     * * @param msg Message containing a Map with keys: "date", "openTime", "closeTime", "description".
     * @return Status message indicating the result of the operation.
     */
    private static Message handleAddSpecialHour(Message msg) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) msg.getContent();
            LocalDate date = (LocalDate) data.get("date");
            LocalTime open = (LocalTime) data.get("openTime"); // Can be null for 'Closed'
            LocalTime close = (LocalTime) data.get("closeTime");
            String desc = (String) data.get("description");

            // --- VALIDATION STEP ---
            if (Reservation_Repository.getInstance().hasConflictForSpecialDate(date, open, close)) {
                return new Message(MessageType.SPECIAL_HOURS_UPDATE_CONFLICT_ERROR, 
                    "Cannot set special hours for " + date + ".\n" +
                    "There are existing reservations outside the proposed time range.");
            }
            // -----------------------

            boolean success = OpeningHours_Repository.getInstance().addSpecialHour(date, open, close, desc);

            if (success) {
                OpeningHours_Repository.getInstance().init();
                return new Message(MessageType.RETURN_OPENING_HOURS, Restaurant.getInstance().getOpeningHours());
            }
            // Specific error code for duplicate dates handled by client
            return new Message(MessageType.OPENING_HOURS_ONLY_ONE_A_DAY_ERROR, "Failed to add special hours.");
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Error processing special hours request.");
        }
    }

    /**
     * Handles the deletion of a special hour entry based on the provided date.
     * * @param msg Message containing the LocalDate of the special hour to delete.
     * @return Updated opening hours if successful, otherwise an error.
     */
    private static Message handleDeleteSpecialHour(Message msg) {
        try {
            LocalDate date = (LocalDate) msg.getContent();
            boolean success = OpeningHours_Repository.getInstance().deleteSpecialHour(date);

            if (success) {
                OpeningHours_Repository.getInstance().init(); // Refresh cache
                return new Message(MessageType.RETURN_OPENING_HOURS, Restaurant.getInstance().getOpeningHours());
            }
            return new Message(MessageType.ERROR_RESPONSE, "Failed to delete special hour.");
        } catch (Exception e) {
            return new Message(MessageType.ERROR_RESPONSE, "Error processing delete request.");
        }
    }
}