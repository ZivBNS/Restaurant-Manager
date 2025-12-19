package controllers;

import Data.User_Repository;
import entities.LoginData;
import entities.Subscribed_Customer;
import messages.Message;
import messages.MessageType;

public class Login_Controller {

	private static final User_Repository userRepository = User_Repository.getInstance();


    
    public static Message handleLogin(LoginData data) {


        if (data == null || data.getUsername() == null || data.getPassword() == null) {
            return new Message(MessageType.LOGIN_FAILED, "Invalid Credentials");
        }
        String username = data.getUsername();
        String password = data.getPassword();

    	System.out.println("[Login_Controller] Recieved login request from user: "+username);
        Subscribed_Customer user = userRepository.getByUsername(username, password);

        if (user != null) {
            return new Message(MessageType.LOGIN_SUCCESS, user);
        } else {
        	return new Message(MessageType.LOGIN_FAILED, "Invalid username or password.");
        }
    }
}
