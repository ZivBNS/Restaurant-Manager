package messages;


public enum MessageType {
    // --- Authorization and User Management ---
    LOGIN_REQUEST_SUB,          // Client sends subscriber credentials 
    LOGIN_REQUEST_GUEST,          // Client sends guest credentials 
    LOGIN_SUCCESS_SUB,          // Server confirms authentication
    LOGIN_SUCCESS_GUEST,          // Server confirms guest authentication
    LOGIN_FAILED_SUB,			// Server denies authentication for subscriber
    LOGIN_FAILED_GUEST,           // Server denies authentication for guest
    LOGOUT_REQUEST,         // Client requests disconnection
    
    // --- Reservations and Waitlist ---
    CREATE_RESERVATION,     // Client requests creation of a new Reservation
    CANCEL_RESERVATION,     // Client requests to cancel a Reservation
    JOIN_WAITLIST,          // Client requests to join the waitlist
    GET_RESERVATIONS_LIST,  // Client requests list of active bookings
    GET_RESERVATIONS_BY_USER,
    RETURN_RESERVATIONS_BY_USER,
    RESERVATION_FAILED_NO_TABLE,
    RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED,
    RESERVATION_FAILED,
    RESERVATION_CONFIRMED,
    RESERVATION_CANCELED,
    RESERVATION_CANCEL_FAILED,
    RESERVATION_UPDATE_SUCCESS,
    RESERVATION_UPDATE_FAILED,
    UPDATE_RESERVATION_REQUEST,
    UPDATE_RESERVATION_SUCCESS,
    GET_ALL_PENDING_RESERVATIONS,
    RETURN_ALL_PENDING_RESERVATIONS,
    ADMIN_UPDATE_RESERVATION,
    ADMIN_UPDATE_SUCCESS,
    //***********************************added by oshri***//
    // --- Register new user account ---
    REGISTER_USER_REQUEST,
    REGISTER_USER_SUCCESS,
    REGISTER_USER_FAILED,
    
    // --- get and set user details ---
    GET_USER_DETAILS,
    RETURN_USER_DETAILS,
    UPDATE_USER_DETAILS_REQUEST,
    UPDATE_USER_DETAILS_SUCCESS,
    
    // --- restaurant waitlist management ---
    WAITLIST_JOINED_SUCCESS,
    WAITLIST_JOINED_FAILED,
    WAITLIST_TABLE_READY,
    WAITLIST_CANCEL_REQUEST,
    
    // ---check in and out messages ---
    RESERVATION_CHECK_IN,
    RESERVATION_CHECK_OUT,
    CHECK_IN_REQUEST,
    CHECK_IN_COMPLETED,
    
    // --- bill request ---
    BILL_REQUEST,
    BILL_RETURN_DETAILS,
    BILL_PAYMENT_SUCCESS,
    BILL_PAYMENT_FAILED,
    
    //--- Opening hours ---
    GET_OPENING_HOURS,
    RETURN_OPENING_HOURS,
    
  //---Table Managment---
    ADD_TABLE_REQUEST, 
    UPDATE_TABLE_REQUEST,
    GET_ALL_TABLES,
    RETURN_ALL_TABLES,
    DELETE_TABLE_REQUEST,
    TABLE_OPERATION_SUCCESS,
    TABLE_OPERATION_FAILED,
    //***************************************************//

    
    // --- Restaurant Management and Status ---
    GET_TABLES_STATUS,      // Client requests current status of all tables
    UPDATE_TABLE_STATUS,    // Rep/Manager changes a table's status
    
    // --- Transactions and Reports ---
    PAY_BILL_REQUEST,       // Client sends payment details
    GET_REPORTS,            // Manager requests performance reports
    
    // --- System Responses ---
    ERROR_RESPONSE,         // Server sends an error message (Content=String)
    SUCCESS_RESPONSE,        // Server confirms successful operation
    
    TEXT_MESSAGE,
    
}