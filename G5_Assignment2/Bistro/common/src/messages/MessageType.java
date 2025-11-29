package messages;


public enum MessageType {
    // --- Authorization and User Management ---
    LOGIN_REQUEST,          // Client sends user credentials (User object)
    LOGIN_SUCCESS,          // Server confirms authentication
    LOGIN_FAILED,           // Server denies authentication
    LOGOUT_REQUEST,         // Client requests disconnection
    
    // --- Reservations and Waitlist ---
    CREATE_RESERVATION,     // Client requests creation of a new Reservation
    CANCEL_RESERVATION,     // Client requests to cancel a Reservation
    JOIN_WAITLIST,          // Client requests to join the waitlist
    GET_RESERVATIONS_LIST,  // Client requests list of active bookings
    
    // --- Restaurant Management and Status ---
    GET_TABLES_STATUS,      // Client requests current status of all tables
    UPDATE_TABLE_STATUS,    // Rep/Manager changes a table's status
    
    // --- Transactions and Reports ---
    PAY_BILL_REQUEST,       // Client sends payment details
    GET_REPORTS,            // Manager requests performance reports
    
    // --- System Responses ---
    ERROR_RESPONSE,         // Server sends an error message (Content=String)
    SUCCESS_RESPONSE,        // Server confirms successful operation
    
    TEXT_MESSAGE
}