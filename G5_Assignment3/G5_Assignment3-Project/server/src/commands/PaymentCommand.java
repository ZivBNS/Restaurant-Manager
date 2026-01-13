package commands;

import controllers.Payment_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for processing payment-related operations.
 * This class acts as a bridge to the Payment_Controller for handling payment logic.
 */
public class PaymentCommand implements Command {
	/**
	 * Executes the payment logic by delegating the received message to the Payment_Controller.
	 * * @param msg    The message received from the client containing payment details.
	 * @param client The connection to the client that initiated the payment request.
	 * @return A Message object representing the response from the Payment_Controller.
	 */
	@Override
    public Message execute(Message msg, ConnectionToClient client) {
        return Payment_Controller.handleMessage(msg);

    }
}