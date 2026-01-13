package commands;

import controllers.User_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for user-related operations.
 * Utilizes a specific instance of User_Controller to process requests.
 */
public class UserCommand implements Command {
    private final User_Controller userController;

    /**
     * Constructs a UserCommand with a specific controller instance.
     * * @param userController The controller instance used to handle user logic.
     */
    // Constructor accepts the controller instance
    public UserCommand(User_Controller userController) {
        this.userController = userController;
    }

    /**
     * Executes the user command by calling the instance method of the associated User_Controller.
     * * @param msg    The message containing the user-related request.
     * @param client The connection from the client.
     * @return A Message object representing the controller's response.
     */
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // Now calling the instance method correctly
        return userController.handle(msg);
    }
}