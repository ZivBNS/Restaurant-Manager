package commands;

import controllers.CheckIn_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for handling check-in operations.
 * This class acts as a wrapper that delegates the execution logic to the CheckIn_Controller.
 */
public class CheckInCommand implements Command {

	/**
	 * Executes the check-in logic by processing the provided message through the CheckIn_Controller.
	 * * @param msg    The message received from the client containing check-in details.
	 * @param client The connection to the client that sent the message.
	 * @return A Message object containing the response after processing the check-in.
	 */
	@Override
	public Message execute(Message msg, ConnectionToClient client) {
		return CheckIn_Controller.handleMessage(msg);
	}
}