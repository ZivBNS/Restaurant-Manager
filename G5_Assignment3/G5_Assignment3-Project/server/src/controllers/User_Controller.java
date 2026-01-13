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

/**
 * Controller responsible for managing user-related operations, including 
 * registration, profile management, and retrieving forgotten confirmation codes.
 */
public class User_Controller {

    private final User_Repository repo;

    /**
     * Constructs a User_Controller and initializes the user repository.
     */
    public User_Controller() {
        this.repo = User_Repository.getInstance();
    }

    /**
     * Main handle method for dispatching user-related messages.
     * Routes incoming requests to the specific handler based on the message type.
     * * @param msg The message received from the client.
     * @return A response message indicating success or an error message.
     */
    public Message handle(Message msg) {
        try {
        	Message response;
            switch (msg.getType()) {
            	case MessageType.FORGOT_CODE:
            		response = forgotCodeLogic((UserRecord) msg.getContent());
            		break;
            	
                case MessageType.GET_ALL_USERS_REQUEST:
                	response = handleGetAll();
                	break;

                case MessageType.ADD_USER_REQUEST:
                	response = handleAdd((UserRecord) msg.getContent());
                	break;

                case MessageType.EDIT_USER_REQUEST:
                	response = handleUpdate((UserRecord) msg.getContent());
                	break;

                case MessageType.DELETE_USER_REQUEST:
                	response = handleDelete((UserRecord) msg.getContent()); 
                	break;
                
                case MessageType.UPDATE_USER_DETAILS_REQUEST:
                	response = handleUpdateAsUser((UserRecord) msg.getContent());
                	break;

                case MessageType.GET_USER_DETAILS:
                	response = handleGetUser((int) msg.getContent());
                	break;
                
                default:
                	response = new Message(MessageType.USERS_ERROR, "Unknown user action: " + msg.getType());
            };
            
            return response;
            
        } catch (ClassCastException e) {
        	e.printStackTrace();
            return new Message(MessageType.USERS_ERROR, "Bad request (wrong data type).");
        } catch (Exception e) {
            e.printStackTrace();
            return new Message(MessageType.USERS_ERROR, "Server error.");
        }
    }

    /**
     * Processes logic for a customer who forgot their reservation confirmation code.
     * Checks if a reservation exists for today using the provided phone or email,
     * and sends the code via email if found.
     * * @param userRecord A record containing contact details (phone/email).
     * @return A message confirming the code was sent, or a NOT_FOUND error.
     */
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

    /**
     * Retrieves all subscribed customers from the repository.
     * * @return A message containing a list of UserRecord objects.
     */
	private Message handleGetAll() {
        List<UserRecord> users = repo.getAllSubscribedCustomers();
        return new Message(MessageType.GET_ALL_USERS_RESPONSE, users);
    }

	/**
	 * Handles the addition of a new user. Performs validation and checks for existing username, email, or phone.
	 * * @param u The user record to be added.
	 * @return A success or error response message.
	 */
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
        
        if (repo.getByEmailOrPhone(u.getEmail(), null)) {
            return new Message(MessageType.ADD_USER_RESPONSE_ERR, "Email already exists.");
        }
        if (repo.getByEmailOrPhone(null, phone)) {
            return new Message(MessageType.ADD_USER_RESPONSE_ERR, "Phone already exists.");
        }

        boolean ok = repo.addNewUser(u);
        return ok
            ? new Message(MessageType.ADD_USER_RESPONSE_OK, "User added.")
            : new Message(MessageType.ADD_USER_RESPONSE_ERR, "Failed to add user.");
    }

    /**
     * Processes an update request for an existing user record.
     * * @param u The updated user record.
     * @return A success or error response message.
     */
    private Message handleUpdate(UserRecord u) {
        String err = validateForUpdate(u);
        if (err != null) return new Message(MessageType.EDIT_USER_RESPONSE_ERR, err);

        boolean ok = repo.updateUser(u);
        return ok
            ? new Message(MessageType.EDIT_USER_RESPONSE_OK, "User updated.")
            : new Message(MessageType.EDIT_USER_RESPONSE_ERR, "Failed to update user.");
    }

    /**
     * Handles the deletion of a user record from the system.
     * * @param u The user record to delete.
     * @return A success or error response message.
     */
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
    
    /**
     * Updates user details as requested by the user themselves.
     * Returns the updated user record on success.
     * * @param u The updated user record.
     * @return A success message with the updated UserRecord, or an error message.
     */
    private Message handleUpdateAsUser(UserRecord u) {
        String err = validateForUpdate(u);
        if (err != null) return new Message(MessageType.UPDATE_USER_DETAILS_RESPONSE_ERR, err);

        boolean ok = repo.updateUser(u);
        UserRecord userToReturn = repo.getByID(u.getId());
        return ok
            ? new Message(MessageType.UPDATE_USER_DETAILS_RESPONSE_OK, userToReturn)
            : new Message(MessageType.UPDATE_USER_DETAILS_RESPONSE_ERR, userToReturn);
    }

    /**
     * Validates that the required fields are present for adding a new user.
     * * @param u The user record to validate.
     * @return An error string if validation fails, or null if it passes.
     */
    private String validateForAdd(UserRecord u) {
        if (u == null) return "Missing user data.";

        if (isBlank(u.getFirstName()) || isBlank(u.getLastName())) return "First/last name required.";
        if (isBlank(u.getUsername())) return "Username required.";
        if (isBlank(u.getPassword())) return "Password required.";
        if (isBlank(u.getEmail()) && isBlank(u.getPhone())) return "Email or phone required.";

        return null;
    }

    /**
     * Handles the request to fetch a user by their ID (subscriber code).
     * * @param id The ID provided by the client.
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

    /**
     * Validates the user record for an update operation.
     * * @param u The user record to validate.
     * @return An error string if validation fails, or null if it passes.
     */
    private String validateForUpdate(UserRecord u) {
        if (u == null) return "Missing user data.";

        if (isBlank(u.getUsername())) return "Username required.";
        if (isBlank(u.getEmail()) && isBlank(u.getPhone())) return "Email or phone required.";

        return null;
    }

    /**
     * Utility method to check if a string is null, empty, or only contains whitespace.
     * * @param s The string to check.
     * @return true if the string is blank, false otherwise.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}