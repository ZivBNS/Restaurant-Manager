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
        return loggedInUser.toString().toUpperCase().contains("SUB");
    }

    public static void setCasualData(String phone, String email) {
        casualPhone = phone;
        casualEmail = email;
    }

    public static String getCasualPhone() { return casualPhone; }
    public static String getCasualEmail() { return casualEmail; }
    
    
	/**
	 * Returns the active phone number for the session.
	 * If a user is logged in, returns their phone number.
	 * Otherwise, returns the casual customer's phone number.
	 * @return The active phone number as a String.
	 */
    public static String getActivePhone() {
        if (loggedInUser != null) {
            return loggedInUser.getPhone(); 
        }
        return casualPhone;
    }
	/**
	 * Returns the casual customer's identifier (phone or email).
	 * Gives priority to phone if both are available.
	 * @return The casual customer's phone number or email, or null if neither is set.
	 */
    public static String getCasualIdentifier() {
        if (casualPhone != null && !casualPhone.isEmpty()) {
            return casualPhone;
        }
        if (casualEmail != null && !casualEmail.isEmpty()) {
            return casualEmail;
        }
        return null;
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