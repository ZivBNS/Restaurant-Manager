package commands;

import controllers.Reservation_Controller;
import controllers.Waitlist_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class WaitlistCommand implements Command {

	@Override
	public Message execute(Message msg, ConnectionToClient client) {
        return Waitlist_Controller.handleMessage(msg);
	}

}
