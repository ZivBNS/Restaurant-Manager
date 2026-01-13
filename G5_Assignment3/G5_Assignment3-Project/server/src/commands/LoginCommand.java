package commands;

import controllers.Login_Controller;
import entities.LoginData;
import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for processing login requests.
 * This class identifies the type of login (Subscriber, Guest, or Employee) 
 * and delegates the authentication process to the Login_Controller.
 */
public class LoginCommand implements Command {
    
    /**
     * Executes the login command based on the message type and provided credentials.
     * * @param msg    The message containing the login credentials (LoginData) and the request type.
     * @param client The connection to the client attempting to log in.
     * @return A Message object containing the login response or an error message if the data is invalid.
     */
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        if (msg.getContent() instanceof LoginData) {
            LoginData data = (LoginData) msg.getContent();
            
            if (msg.getType() == MessageType.LOGIN_REQUEST_SUB) {
                return Login_Controller.handleSubLogin(data);
            } else if (msg.getType() == MessageType.LOGIN_REQUEST_GUEST) {
                return Login_Controller.handleGuestLogin(data);
            } else if (msg.getType() == MessageType.LOGIN_REQUEST_EMP) {
            	return Login_Controller.handleEmployeeLogin(data);
            }
        }
        return new Message(MessageType.ERROR_RESPONSE, "Invalid Login Data");
    }
}