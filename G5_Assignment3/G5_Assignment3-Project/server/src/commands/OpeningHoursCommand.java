package commands;

import controllers.OpeningHours_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class OpeningHoursCommand implements Command {
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        return OpeningHours_Controller.handle(msg);
    }
}