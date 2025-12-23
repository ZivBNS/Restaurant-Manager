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
            }
        }
        return new Message(MessageType.LOGIN_FAILED_SUB, "Invalid Login Data");
    }
}