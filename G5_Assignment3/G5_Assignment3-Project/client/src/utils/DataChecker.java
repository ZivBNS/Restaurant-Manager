package utils;

/**
 * Utility class for validating user input data such as email, phone number,
 * password, username, and confirmation codes.
 */
public class DataChecker {

    // --- Private Validation Methods ---
	/**
	 * Validates the format of an email address.
	 * @param email The email address to validate.
	 */
    private static boolean checkEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        
        int atIndex = email.indexOf('@');
        int dotIndex = email.lastIndexOf('.');
        
        
        return (atIndex > 0 && 
                dotIndex > atIndex + 1 && 
                dotIndex < email.length() - 1);
    }

	/**
	 * Validates the format of a phone number.
	 * @param phone The phone number to validate.
	 */
    private static boolean checkPhone(String phone) {
        if (phone == null || phone.length() < 9 || phone.length() > 11) return false;
        if (!phone.startsWith("0")) return false;
        
        for (int i = 0; i < phone.length(); i++) {
            if (!Character.isDigit(phone.charAt(i))) return false;
        }
        return true;
    }

	/**
	 * Validates the format of a password.
	 * @param password The password to validate.
	 * @return true if valid, false otherwise.
	 */
    private static boolean checkPassword(String password) {
        return password != null && password.length() >= 4;
    }

	/**
	 * Validates the format of a username.
	 * @param username The username to validate.
	 * @return true if valid, false otherwise.
	 */
    private static boolean checkUsername(String username) {
        return username != null && username.length() >= 6;
    }

	/**
	 * Validates the format of a confirmation code.
	 * @param code The confirmation code to validate.
	 * @return true if valid, false otherwise.
	 */
    private static boolean checkConfirmationCode(String code) {
        if (code == null || code.length() < 6) return false;
        
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isDigit(code.charAt(i))) return false;
        }
        return true;
    }

    // --- Public Static Combination Methods ---

	/**
	 * Validates login credentials (username and password).
	 * @param username The username to validate.
	 * @param password The password to validate.
	 */
    public static boolean validateLogin(String username, String password) {
        return checkUsername(username) && checkPassword(password);
    }

	/**
	 * Validates contact information (email and phone).
	 * @param email The email address to validate.
	 * @param phone The phone number to validate.
	 */
    public static boolean validateContactInfo(String email, String phone) {
        if (email==null) return checkPhone(phone);
        if (phone==null) return checkEmail(email);
    	return checkEmail(email) && checkPhone(phone);
    }

	/**
	 * Validates a confirmation code.
	 * @param code The confirmation code to validate.
	 * @return true if valid, false otherwise.
	 */
    public static boolean validateCode(String code) {
        return checkConfirmationCode(code);
    }
}