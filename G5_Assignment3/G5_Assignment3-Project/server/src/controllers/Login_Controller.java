package controllers;

import Data.User_Repository;
import entities.LoginData;
import entities.UserRecord;
import messages.Message;
import messages.MessageType;

public class Login_Controller {

	private static final User_Repository userRepository = User_Repository.getInstance();


    public static Message handleEmployeeLogin(LoginData data) {


        if (data == null || data.getUsername() == null || data.getPassword() == null) {
            return new Message(MessageType.LOGIN_FAILED_EMP, "Invalid Credentials");
        }
        String username = data.getUsername();
        String password = data.getPassword();

    	System.out.println("[Login_Controller] Recieved login request from employee: "+username);
        UserRecord user = userRepository.getEmpByUsername(username, password);

        if (user != null) {
            return new Message(MessageType.LOGIN_SUCCESS_EMP, user);
        } else {
        	return new Message(MessageType.LOGIN_FAILED_EMP, "Invalid username or password.");
        }
    }
    
    public static Message handleSubLogin(LoginData data) {


        if (data == null || data.getUsername() == null || data.getPassword() == null) {
            return new Message(MessageType.LOGIN_FAILED_SUB, "Invalid Credentials");
        }
        String username = data.getUsername();
        String password = data.getPassword();

    	System.out.println("[Login_Controller] Recieved login request from subscribed user: "+username);
        UserRecord user = userRepository.getByUsername(username, password);

        if (user != null) {
            return new Message(MessageType.LOGIN_SUCCESS_SUB, user);
        } else {
        	return new Message(MessageType.LOGIN_FAILED_SUB, "Invalid username or password.");
        }
    }
    
    public static Message handleGuestLogin(LoginData data) {
        if (data == null || (data.getEmail() == null && data.getPhoneNumber() == 0)) {
            return new Message(MessageType.LOGIN_FAILED_GUEST, "Invalid Credentials");
        }
        String email = data.getEmail(); 
        int phone = data.getPhoneNumber(); 

    	System.out.println("[Login_Controller] Recieved login request from casual user: ");
        boolean userFound = userRepository.getByEmailOrPhone(email, phone);
        if (!userFound) {
        	return new Message(MessageType.LOGIN_SUCCESS_GUEST);
        }else {
        	System.out.println("[Login_Controller] Email or Phone for guest user already exist in database ");
        	return new Message(MessageType.LOGIN_FAILED_GUEST); // message string is intentionally empty 
        }
    	
    }
}
