package commands;

import controllers.Waitlist_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for handling waitlist-related operations.
 * This class acts as a wrapper that delegates the execution logic to the Waitlist_Controller.
 */
public class WaitlistCommand implements Command {

	/**
	 * Executes the waitlist logic by processing the provided message through the Waitlist_Controller.
	 * * @param msg    The message received from the client containing waitlist details.
	 * @param client The connection to the client that sent the message.
	 * @return A Message object containing the response after processing the waitlist request.
	 */
	@Override
	public Message execute(Message msg, ConnectionToClient client) {
        return Waitlist_Controller.handleMessage(msg);
	}

}