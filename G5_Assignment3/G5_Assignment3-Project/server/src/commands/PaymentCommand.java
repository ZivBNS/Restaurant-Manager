package commands;

import controllers.Payment_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class PaymentCommand implements Command {
	@Override
    public Message execute(Message msg, ConnectionToClient client) {
        // Delegate to the existing controller static method
        return Payment_Controller.handleMessage(msg);

    }
}
