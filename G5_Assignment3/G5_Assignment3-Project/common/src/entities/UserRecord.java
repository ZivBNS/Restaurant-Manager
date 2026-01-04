package entities;

public class UserRecord implements User{
    private int id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String username;
    private String password;
    private String identity;        // "Subscriber", "Worker", "Manager"
    private Integer subscriberCode; // nullable

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

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getIdentity() { return identity; }
    public Integer getSubscriberCode() { return subscriberCode; }

	@Override
	public void setUsername(String username) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return password;
	}

	@Override
	public void setPassword(String password) {
		// TODO Auto-generated method stub
		
	}
}

