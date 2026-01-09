package utils;

import entities.UserRecord;

/**
 * Manages the current user session on the client side.
 * Stores whether the user is a Subscriber or a Casual Customer.
 */
public class User_Session {
    private static UserRecord loggedInUser = null; // For Subscribers/Employees
    private static String casualPhone = null; // For Casual Customers
    private static String casualEmail = null; // For Casual Customers

    public static void setLoggedInUser(UserRecord user) { loggedInUser = user; }
    public static UserRecord getLoggedInUser() { return loggedInUser; }
    
    /**
     * Checks if the user is a Subscriber.
     * Uses class name comparison to avoid inheritance type errors.
     */
    public static boolean isSubscriber() {
        if (loggedInUser == null) return false;
        // בדיקה אם המחרוזת של סוג המשתמש מכילה את המילה "SUB"
        return loggedInUser.toString().toUpperCase().contains("SUB");
    }

    public static void setCasualData(String phone, String email) {
        casualPhone = phone;
        casualEmail = email;
    }

    public static String getCasualPhone() { return casualPhone; }
    public static String getCasualEmail() { return casualEmail; }
    
 // בתוך מחלקת User_Session
    public static String getActivePhone() {
        // אם מחובר מנוי, ניקח את הטלפון מהפרופיל שלו
        if (loggedInUser != null) {
            return loggedInUser.getPhone(); // וודא שב-UserRecord יש getPhone()
        }
        // אם לא, ניקח את הטלפון שהוזן ידנית
        return casualPhone;
    }

    /**
     * Clears session data upon logout or disconnection.
     */
    public static void clear() {
        loggedInUser = null;
        casualPhone = null;
        casualEmail = null;
    }
}