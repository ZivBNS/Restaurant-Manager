package controllers;

import entities.Opening_Hours;
import entities.Restaurant;
import messages.Message;
import messages.MessageType;

/**
 * Controller responsible for handling logic related to restaurant opening hours.
 * It interfaces between the network commands and the stored restaurant entities.
 */
public class OpeningHours_Controller {

    /**
     * Main entry point for handling opening hours messages.
     * Routes the message to specific logic based on the MessageType.
     * * @param msg The message received from the client.
     * @return A response message containing the requested data or status.
     */
    public static Message handle(Message msg) {
        switch (msg.getType()) {
            case GET_OPENING_HOURS:
                return getOpeningHours();
            
            // Add future cases here (e.g., UPDATE_OPENING_HOURS)
            
            default:
                return new Message(MessageType.ERROR_RESPONSE, "Unknown Opening Hours request.");
        }
    }

    /**
     * Retrieves the current opening hours from the Restaurant singleton.
     * The repository populates this singleton during system initialization.
     * * @return A message containing the Opening_Hours object.
     */
    private static Message getOpeningHours() {
        Opening_Hours hours = Restaurant.getInstance().getOpeningHours();
        if (hours != null) {
            return new Message(MessageType.RETURN_OPENING_HOURS, hours);
        } else {
            return new Message(MessageType.ERROR_RESPONSE, "Opening hours data not available.");
        }
    }
}