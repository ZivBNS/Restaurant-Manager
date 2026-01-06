package entities;

public class LoginData {
    private String username;
    private String password;
    private String email;
    private String phoneNumber;

    public LoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public LoginData() {
    }
    public void setEmail(String email) {
    	this.email = email;
    }
    public void setPhone(String phone) {
    	this.phoneNumber = phone;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}
