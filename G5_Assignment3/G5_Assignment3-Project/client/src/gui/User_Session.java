package gui;

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

    public static void setCasualData(String phone, String email) {
        casualPhone = phone;
        casualEmail = email;
    }

    public static String getCasualPhone() { return casualPhone; }
    public static String getCasualEmail() { return casualEmail; }

    /**
     * Clears session data upon logout or disconnection.
     */
    public static void clear() {
        loggedInUser = null;
        casualPhone = null;
        casualEmail = null;
    }
}