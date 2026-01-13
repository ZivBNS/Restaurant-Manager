package commands;

import controllers.Reservation_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for handling restaurant reservation requests.
 * This class delegates reservation-related tasks to the Reservation_Controller.
 */
public class ReservationCommand implements Command {
    
    /**
     * Executes the reservation command.
     * Delegates the processing of the reservation message to the existing controller static method.
     * * @param msg    The message containing reservation data or requests.
     * @param client The connection to the client that sent the request.
     * @return A Message object containing the result of the reservation operation.
     */
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // Delegate to the existing controller static method
        return Reservation_Controller.handleMessage(msg);
    }
}