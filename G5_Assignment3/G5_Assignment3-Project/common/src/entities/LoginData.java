package entities;

/**
 * Represents the login credentials and contact information for a user.
 * This class is used to transport authentication data and basic contact details.
 */
public class LoginData {
    /** The unique username for authentication */
    private String username;
    /** The password associated with the account */
    private String password;
    /** The user's email address */
    private String email;
    /** The user's contact phone number */
    private String phoneNumber;

    /**
     * Constructs a LoginData object with username and password.
     * * @param username The user's identifier.
     * @param password The user's secret credential.
     */
    public LoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Default constructor for creating an empty LoginData object.
     */
    public LoginData() {
    }

    /**
     * Sets the user's email address.
     * * @param email The email to be set.
     */
    public void setEmail(String email) {
    	this.email = email;
    }

    /**
     * Sets the user's phone number.
     * * @param phone The phone number to be set.
     */
    public void setPhone(String phone) {
    	this.phoneNumber = phone;
    }

    /** @return The username used for login */
    public String getUsername() { return username; }
    
    /** @return The user's password */
    public String getPassword() { return password; }
    
    /** @return The user's email address */
    public String getEmail() { return email; }
    
    /** @return The user's phone number */
    public String getPhoneNumber() { return phoneNumber; }
}