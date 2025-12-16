package entities;

public class Subscribed_Customer extends Casual_Customer implements User {
	public static int subscriberCodeGenerator=100000;

	private static final long serialVersionUID = 1L;
	
	private String firstName;
	private String lastName;
	private String username;
	private String password;
	private String digitalCard;
	private int subscriberCode;

	public Subscribed_Customer(String firstName, String lastName, String phone, String email, String username,String password) {
		super(phone, email);
		this.username = username;
		this.password = password;
		this.subscriberCode = subscriberCodeGenerator++;
		this.digitalCard = generateDigitalCard(subscriberCode);
		this.firstName=firstName;
		this.lastName=lastName;
	}

	private String generateDigitalCard(int id) {
		return "00" + subscriberCode;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	public int getSubscriberCode() {
		return subscriberCode++;
	}


	public String getDigitalCard() {
		return digitalCard;
	}

	public void setDigitalCard(String digitalCard) {
		this.digitalCard = digitalCard;
	}

	@Override
	public String toString() {
		return "Subscribed_Customer [username=" + username + ", password=" + password + ", subscriberCode="
				+ subscriberCode + ", digitalCard=" + digitalCard + "]";
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}


}
