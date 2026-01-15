package entities;

/**
 * Represents a user profile within the system.
 * This class stores personal information, credentials, and access roles 
 * for customers, employees, and managers.
 */
public class UserRecord{
    /** Unique identifier for the user in the database */
    private int id;
    /** User's first name */
    private String firstName;
    /** User's last name */
    private String lastName;
    /** User's contact phone number */
    private String phone;
    /** User's email address */
    private String email;
    /** Unique username for system authentication */
    private String username;
    /** Secret password for authentication */
    private String password;
    /** The role/permission level of the user (e.g., "Subscriber", "Worker", "Manager") */
    private String identity;        
    /** Unique code assigned to subscribers; can be null if user is not a subscriber */
    private Integer subscriberCode;

    /**
     * Full constructor for creating a UserRecord, used for employees and subscribers.
     * * @param id The user's unique ID.
     * @param firstName User's first name.
     * @param lastName User's last name.
     * @param phone Contact phone number.
     * @param email Contact email address.
     * @param username Login username.
     * @param password Login password.
     * @param identity Role/identity string.
     * @param subscriberCode Optional subscriber identifier.
     */
    public UserRecord(int id, String firstName, String lastName, String phone,
                      String email, String username, String password, String identity, Integer subscriberCode) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.username = username;
        this.password = password;
        this.identity = identity;
        this.subscriberCode = subscriberCode;
    }

    /**
     * Partial constructor used guest users
     * * @param phone The user's phone number.
     * @param email The user's email address.
     */
    public UserRecord(String phone, String email) {
    	this.phone=phone;
    	this.email=email;
    }

	/** @return The internal user ID */
	public int getId() { return id; }
    /** @return The user's first name */
    public String getFirstName() { return firstName; }
    /** @return The user's last name */
    public String getLastName() { return lastName; }
    /** @return The contact phone number */
    public String getPhone() { return phone; }
    /** @return The user's email address */
    public String getEmail() { return email; }
    /** @return The system username */
    public String getUsername() { return username; }
    /** @return The identity/role string (Subscriber/Worker/Manager) */
    public String getIdentity() { return identity; }
    /** @return The unique subscriber code, or null if not applicable */
    public Integer getSubscriberCode() { return subscriberCode; }

	/** @return The user's password */
	public String getPassword() {
		return password;
	}
}