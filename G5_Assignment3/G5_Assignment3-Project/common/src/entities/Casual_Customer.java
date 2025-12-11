package entities;

import java.io.Serializable;

public class Casual_Customer implements Serializable   {

	private static final long serialVersionUID = 1L;
	private String phone;
    private String email;

    public Casual_Customer(String phone, String email) {
        this.phone = phone;
        this.email = email;
    }

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "Casual_Customer [phone=" + phone + ", email=" + email + "]";
	}
    
}
