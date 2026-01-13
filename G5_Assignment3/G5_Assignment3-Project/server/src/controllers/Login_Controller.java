package controllers;

import Data.User_Repository;
import entities.LoginData;
import entities.UserRecord;
import messages.Message;
import messages.MessageType;

/**
 * Controller responsible for managing user authentication and login processes.
 * Handles different login flows for employees, subscribed users, and guests
 * by interacting with the User_Repository.
 */
public class Login_Controller {

	private static final User_Repository userRepository = User_Repository.getInstance();

	/**
	 * Authenticates an employee user.
	 * Validates the provided username and password against the employee records in the database.
	 * * @param data The login credentials containing username and password.
	 * @return A Message object with LOGIN_SUCCESS_EMP and user data on success, 
	 * or LOGIN_FAILED_EMP with an error description on failure.
	 */
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
    
    /**
     * Authenticates a subscribed user (subscriber).
     * Validates the provided username and password against the subscriber records.
     * * @param data The login credentials containing username and password.
     * @return A Message object with LOGIN_SUCCESS_SUB and user data on success, 
     * or LOGIN_FAILED_SUB with an error description on failure.
     */
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
    
    /**
     * Handles login/identification for a casual guest user.
     * Checks if the guest's email or phone number already exists in the system to prevent duplicates.
     * * @param data The guest's contact information (email and phone number).
     * @return A Message object with LOGIN_SUCCESS_GUEST if the guest is new, 
     * or LOGIN_FAILED_GUEST if the contact information already exists.
     */
    public static Message handleGuestLogin(LoginData data) {
        if (data == null || (data.getEmail() == null && data.getPhoneNumber().isEmpty())) {
            return new Message(MessageType.LOGIN_FAILED_GUEST, "Invalid Credentials");
        }
        String email = data.getEmail(); 
        String phone = data.getPhoneNumber(); 

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