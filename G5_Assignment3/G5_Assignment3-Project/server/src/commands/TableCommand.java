package commands;

import controllers.Table_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class TableCommand implements Command {
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        return Table_Controller.handle(msg);
    }
}