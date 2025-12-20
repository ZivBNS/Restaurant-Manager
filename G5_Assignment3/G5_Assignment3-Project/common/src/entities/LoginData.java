package entities;

public class LoginData {
    private String username;
    private String password;
    private String email;
    private int phoneNumber;

    public LoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public LoginData(String email) {
    	this.email = email;
    }
    public LoginData(int phoneNumber) {
    	this.phoneNumber = phoneNumber;
    }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public int getPhoneNumber() { return phoneNumber; }
}
