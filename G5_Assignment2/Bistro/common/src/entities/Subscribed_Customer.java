package entities;

public abstract class Subscribed_Customer extends Casual_Customer implements User {

	private String username;
	private String password;
	private int subscriberCode;
	private String digitalCard;

	public Subscribed_Customer(int id, String firstName, String lastName, String phone, String email, String username,
			String password, int subscriberCode) {
		super(phone, email);
		this.username = username;
		this.password = password;
		this.subscriberCode = subscriberCode;
		this.digitalCard = generateDigitalCard(subscriberCode);
	}

	private String generateDigitalCard(int id) {
		return "CARD-" + id;
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

	public void setSubscriberCode(int subscriberCode) {
		this.subscriberCode = subscriberCode;
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

}
