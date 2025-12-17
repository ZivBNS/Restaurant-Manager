package gui;

public class DataChecker {

    // --- Private Validation Methods ---

    private static boolean checkEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        
        int atIndex = email.indexOf('@');
        int dotIndex = email.lastIndexOf('.');
        
        // בודק שיש @, שיש נקודה אחרי ה-@, ושיש לפחות תו אחד ביניהם וביניהם לקצוות
        return (atIndex > 0 && 
                dotIndex > atIndex + 1 && 
                dotIndex < email.length() - 1);
    }

    private static boolean checkPhone(String phone) {
        if (phone == null || phone.length() < 9 || phone.length() > 11) return false;
        if (!phone.startsWith("0")) return false;
        
        // בודק שכל התווים הם ספרות בלבד
        for (int i = 0; i < phone.length(); i++) {
            if (!Character.isDigit(phone.charAt(i))) return false;
        }
        return true;
    }

    private static boolean checkPassword(String password) {
        return password != null && password.length() >= 4;
    }

    private static boolean checkUsername(String username) {
        return username != null && username.length() >= 6;
    }

    private static boolean checkConfirmationCode(String code) {
        if (code == null || code.length() < 6) return false;
        
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isDigit(code.charAt(i))) return false;
        }
        return true;
    }

    // --- Public Static Combination Methods ---

    public static boolean validateLogin(String username, String password) {
        return checkUsername(username) && checkPassword(password);
    }

    public static boolean validateContactInfo(String email, String phone) {
        // בודק לפחות אחד מהם תקין
        if (email==null) return checkPhone(phone);
        if (phone==null) return checkEmail(email);
    	return checkEmail(email) && checkPhone(phone);
    }

    public static boolean validateCode(String code) {
        return checkConfirmationCode(code);
    }
}