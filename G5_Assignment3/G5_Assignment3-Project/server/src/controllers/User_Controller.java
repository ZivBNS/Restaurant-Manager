package controllers;

import Data.Reservation_Repository;
import Data.User_Repository;
import entities.Reservation;
import entities.UserRecord;
import integration.EmailService;
import messages.Message;
import messages.MessageType;

import java.time.LocalDate;
import java.util.List;

public class User_Controller {

    private final User_Repository repo;

    public User_Controller() {
        this.repo = User_Repository.getInstance();
    }

    /**
     * Main handle method. Changed from static to instance method.
     */
    public Message handle(Message msg) {
        try {
            return switch (msg.getType()) {
            	case MessageType.FORGOT_CODE -> forgotCodeLogic((UserRecord) msg.getContent());
            	
                case MessageType.GET_ALL_USERS_REQUEST -> handleGetAll();

                case MessageType.ADD_USER_REQUEST -> handleAdd((UserRecord) msg.getContent());

                case MessageType.EDIT_USER_REQUEST -> handleUpdate((UserRecord) msg.getContent());

                case MessageType.DELETE_USER_REQUEST -> handleDelete((UserRecord) msg.getContent()); 
                
                case MessageType.UPDATE_USER_DETAILS_REQUEST -> handleUpdateAsUser((UserRecord) msg.getContent());

                case MessageType.GET_USER_DETAILS -> handleGetUser((int) msg.getContent());
                
                default -> new Message(MessageType.USERS_ERROR, "Unknown user action: " + msg.getType());
            };
        } catch (ClassCastException e) {
            return new Message(MessageType.USERS_ERROR, "Bad request (wrong data type).");
        } catch (Exception e) {
            e.printStackTrace();
            return new Message(MessageType.USERS_ERROR, "Server error.");
        }
    }

    //for the terminal, is case someone click on forgot my cod and put details
    private Message forgotCodeLogic(UserRecord userRecord) {
    	String phone=userRecord.getPhone();
    	String email = userRecord.getEmail();
		Reservation res = Reservation_Repository.getInstance().getClosestReservationByContact(phone, email);
    	if (res==null) return new Message(MessageType.FORGOT_CODE_NOT_FOUND,"The confirmation code for reservation with this record is not found!");  	
    	if (!res.getOrderStartTime().toLocalDate().equals(LocalDate.now()))
    		return new Message(MessageType.FORGOT_CODE_NOT_FOUND,"There is no reservation for today with this confirmation code");
    	System.out.println("USER CONTROLLER - forgot Code Logic - messaging number: "+ phone + "with reminder that the code is: "+res.getConfirmationCode());
		System.out.println("NOTE that the reservation is: "+res.toString());
		EmailService.sendForgotCodeNotification(email,res.getConfirmationCode());

    	return new Message(MessageType.FORGOT_CODE_FOUND, "We have sent the confirmation code through email and phone.\nYour confirmation code is: "+res.getConfirmationCode());
	}

	private Message handleGetAll() {
        List<UserRecord> users = repo.getAllSubscribedCustomers();
        return new Message(MessageType.GET_ALL_USERS_RESPONSE, users);
    }

    private Message handleAdd(UserRecord u) {
        String err = validateForAdd(u);
        if (err != null) return new Message(MessageType.ADD_USER_RESPONSE_ERR, err);

        if (repo.existsByUsername(u.getUsername())) {
            return new Message(MessageType.ADD_USER_RESPONSE_ERR, "Username already exists.");
        }
        
        String phone = "";
        try {
            phone = u.getPhone();
        } catch (NumberFormatException e) {
            System.out.println("Not a valid number");
        }
        
        if (repo.getByEmailOrPhone(u.getEmail(), phone)) {
            return new Message(MessageType.ADD_USER_RESPONSE_ERR, "Email or phone already exists.");
        }

        boolean ok = repo.addNewUser(u);
        return ok
            ? new Message(MessageType.ADD_USER_RESPONSE_OK, "User added.")
            : new Message(MessageType.ADD_USER_RESPONSE_ERR, "Failed to add user.");
    }

    private Message handleUpdate(UserRecord u) {
        String err = validateForUpdate(u);
        if (err != null) return new Message(MessageType.EDIT_USER_RESPONSE_ERR, err);

        boolean ok = repo.updateUser(u);
        return ok
            ? new Message(MessageType.EDIT_USER_RESPONSE_OK, "User updated.")
            : new Message(MessageType.EDIT_USER_RESPONSE_ERR, "Failed to update user.");
    }

    private Message handleDelete(UserRecord u) {
        int id = u.getId();
        if (id == 0) {
            return new Message(MessageType.DELETE_USER_RESPONSE_ERR, "ID is required.");
        }

        boolean ok = repo.deleteUserByID(u);
        return ok
            ? new Message(MessageType.DELETE_USER_RESPONSE_OK, "User deleted.")
            : new Message(MessageType.DELETE_USER_RESPONSE_ERR, "Failed to delete user.");
    }
    
    private Message handleUpdateAsUser(UserRecord u) {
        String err = validateForUpdate(u);
        if (err != null) return new Message(MessageType.UPDATE_USER_DETAILS_RESPONSE_ERR, err);

        boolean ok = repo.updateUser(u);
        UserRecord userToReturn = repo.getByID(u.getId());
        return ok
            ? new Message(MessageType.UPDATE_USER_DETAILS_RESPONSE_OK, userToReturn)
            : new Message(MessageType.UPDATE_USER_DETAILS_RESPONSE_ERR, userToReturn);
    }

    private String validateForAdd(UserRecord u) {
        if (u == null) return "Missing user data.";

        if (isBlank(u.getFirstName()) || isBlank(u.getLastName())) return "First/last name required.";
        if (isBlank(u.getUsername())) return "Username required.";
        if (isBlank(u.getPassword())) return "Password required.";
        if (isBlank(u.getEmail()) && isBlank(u.getPhone())) return "Email or phone required.";

        return null;
    }
    /**
     * Handles the request to fetch a user by their ID.
     * @param id The ID provided by the client.
     * @return A message containing the UserRecord or USER_NOT_FOUND.
     */
    private Message handleGetUser(int id) {
        // Try finding by internal ID first
        UserRecord user = repo.getBySubscriberCode(id);
        
        if (user != null) {
            return new Message(MessageType.RETURN_USER_DETAILS, user);
        } else {
            return new Message(MessageType.USER_NOT_FOUND, null);
        }
    }
    private String validateForUpdate(UserRecord u) {
        if (u == null) return "Missing user data.";

        if (isBlank(u.getUsername())) return "Username required.";
        if (isBlank(u.getEmail()) && isBlank(u.getPhone())) return "Email or phone required.";

        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}