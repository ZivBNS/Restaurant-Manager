package commands;

import controllers.Login_Controller;
import entities.LoginData;
import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;

public class LoginCommand implements Command {
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