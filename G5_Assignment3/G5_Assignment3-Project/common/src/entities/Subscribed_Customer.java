package entities;

public class Subscribed_Customer extends Casual_Customer implements User {
	public static int subscriberCodeGenerator=100000;
	private int id=0;
	private String firstName;
	private String lastName;
	private String username;
	private String password;
	private String digitalCard;
	private Integer subscriberCode;
	private String identity = null;

	public Subscribed_Customer(String firstName, String lastName, String phone, String email, String username,String password) {
		super(phone, email);
		this.username = username;
		this.password = password;
		this.subscriberCode = subscriberCodeGenerator++;
		this.digitalCard = generateDigitalCard(subscriberCode);
		this.firstName=firstName;
		this.lastName=lastName;
	}
	//constructor for get data from db , added recently
	public Subscribed_Customer(int id, String firstName, String lastName, String phone, String email, String username,String password,int subcriberCode,String whatHeIs) {
		super(phone, email);
		this.id=id;
		this.username = username;
		this.password = password;
		this.subscriberCode = (this.id!=0) ? subcriberCode:subscriberCodeGenerator++;
		this.digitalCard = generateDigitalCard(subscriberCode);
		this.firstName=firstName;
		this.lastName=lastName;
		this.setIdentity(whatHeIs);
	}
	
	public int getUserId() {
		return id;
	}

	public void setUserId(int id) {
		this.id=id;
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
		return subscriberCode;
	}


	public String getDigitalCard() {
		return digitalCard;
	}

	public void setDigitalCard(String digitalCard) {
		this.digitalCard = digitalCard;
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

	@Override
	public String toString() {
		return "Subscribed_Customer [firstName=" + firstName + ", lastName=" + lastName + ", username=" + username
				+ ", password=" + password + ", digitalCard=" + digitalCard + ", subscriberCode=" + subscriberCode
				+ "]";
	}
	public String getIdentity() {
		return identity;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	


}
