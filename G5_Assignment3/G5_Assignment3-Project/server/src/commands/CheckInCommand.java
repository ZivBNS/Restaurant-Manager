package commands;

import controllers.CheckIn_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class CheckInCommand implements Command {

	@Override
	public Message execute(Message msg, ConnectionToClient client) {
		return CheckIn_Controller.handleMessage(msg);
	}
}
