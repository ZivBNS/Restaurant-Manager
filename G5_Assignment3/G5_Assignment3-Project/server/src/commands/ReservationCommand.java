package commands;

import controllers.Reservation_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class ReservationCommand implements Command {
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // Delegate to the existing controller static method
        return Reservation_Controller.handleMessage(msg);
    }
}