package commands;

import controllers.User_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

public class UserCommand implements Command {
    private final User_Controller userController;

    // Constructor accepts the controller instance
    public UserCommand(User_Controller userController) {
        this.userController = userController;
    }

    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // Now calling the instance method correctly
        return userController.handle(msg);
    }
}