package messages;


public enum MessageType {
    // --- Authorization and User Management ---
    LOGIN_REQUEST_SUB,          // Client sends subscriber credentials 
    LOGIN_SUCCESS_SUB,          // Server confirms authentication
    LOGIN_FAILED_SUB,			// Server denies authentication for subscriber
    
    LOGIN_REQUEST_GUEST,          // Client sends guest credentials 
    LOGIN_SUCCESS_GUEST,          // Server confirms guest authentication
    LOGIN_FAILED_GUEST,           // Server denies authentication for guest
    
    LOGIN_REQUEST_EMP,
    LOGIN_SUCCESS_EMP,
    LOGIN_FAILED_EMP,
    
    LOGOUT_REQUEST,         // Client requests disconnection
    
    // --- Reservations and Waitlist ---
    CREATE_RESERVATION,     // Client requests creation of a new Reservation
    CANCEL_RESERVATION_BY_CODE,     // Client requests to cancel a Reservation
    CANCEL_RESERVATION,     // Client requests to cancel a Reservation
    
    JOIN_WAITLIST,          // Client requests to join the waitlist
    GET_RESERVATIONS_LIST,  // Client requests list of active bookings
    GET_RESERVATIONS_BY_USER,
    RETURN_RESERVATIONS_BY_USER,
    RESERVATION_FAILED_NO_TABLE,
    RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED,
    RESERVATION_FAILED,
    RESERVATION_FAILED_ALREADY_BOOKED,
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
    GET_ALL_PENDING_AND_ACTIVE_RESERVATIONS,
    //for instant reservaionts
    CREATE_INSTANT_RESERVATION,
    INSTANT_RESERVATION_FAILED,
    INSTANT_RESERVATION_SUCCESS,

    //***********************************added by oshri***//
    // --- Register new user account ---
    REGISTER_USER_REQUEST,
    REGISTER_USER_SUCCESS,
    REGISTER_USER_FAILED,
    
    // --- get and set user details ---
    GET_ALL_USERS_REQUEST,
    GET_ALL_USERS_RESPONSE,
    ADD_USER_REQUEST,
    ADD_USER_RESPONSE_OK,
    ADD_USER_RESPONSE_ERR,
    EDIT_USER_REQUEST,
    EDIT_USER_RESPONSE_OK,
    EDIT_USER_RESPONSE_ERR,
    DELETE_USER_REQUEST,
    DELETE_USER_RESPONSE_OK,
    DELETE_USER_RESPONSE_ERR,
    USERS_ERROR,
    GET_USER_DETAILS,
    RETURN_USER_DETAILS,
    UPDATE_USER_DETAILS_REQUEST,
    UPDATE_USER_DETAILS_RESPONSE_OK,
    UPDATE_USER_DETAILS_RESPONSE_ERR,
    UPDATE_USER_PROFILE,
    USER_NOT_FOUND,
    
    // --- restaurant waitlist management ---
    WAITLIST_JOINED_SUCCESS,
    WAITLIST_JOINED_FAILED,
    CANCEL_WAITLIST_AND_RESERVATION_BY_CODE,
    CANCEL_WAITLIST_AND_RESERVATION_FAILED, 
    WAITLIST_AND_RESERVATION_CANCELED, 
    GET_ALL_ACTIVE_WAITLISTS,
    RETURN_ALL_ACTIVE_WAITLISTS,
    
    // ---check in and out messages ---
    RESERVATION_CHECK_IN,
    RESERVATION_CHECK_OUT,
    CHECK_IN_REQUEST,
    CHECK_IN_COMPLETED,
    CHECK_IN_FAIL,
    CHECK_IN_UNMATCH_CODE,
    CHECK_IN_NO_TABLE,
    CHECK_IN_FROM_WAITLIST_FAIL,
    
    // --- bill request ---
    BILL_REQUEST,
    BILL_REQUEST_FAILED,
    BILL_REQUEST_SUCCESS,
    BILL_RETURN_DETAILS,
    BILL_PAYMENT_SUCCESS,
    BILL_PAYMENT_FAILED,
    GET_LATEST_RESERVATION_BY_PHONE,
    RETURN_LATEST_RESERVATION_BY_PHONE,
    GET_BILL_BY_RESERVATION_ID,
    RETURN_BILL_BY_RESERVATION_ID,
    BILL_PAYMENT_REQUEST, 
    GET_LATEST_BILL_BY_PHONE,
    GET_ALL_BILLS,
    RETURN_ALL_BILLS,
    CREATE_BILL,
    DELETE_BILL,

    
    //--- Opening hours ---
    GET_OPENING_HOURS,
    RETURN_OPENING_HOURS,
    DELETE_REGULAR_HOURS,
    ADD_SPECIAL_HOUR,
    UPDATE_REGULAR_HOURS,
    DELETE_SPECIAL_HOUR,
    
  //---Table Managment---
    ADD_TABLE_REQUEST, 
    UPDATE_TABLE_REQUEST,
    GET_ALL_TABLES,
    RETURN_ALL_TABLES,
    DELETE_TABLE_REQUEST,
    TABLE_OPERATION_SUCCESS,
    TABLE_OPERATION_FAILED,
    
    //---Report Managment---
    REPORT_DATA_SUCCESS,
    REPORT_ERROR,
    GET_MONTHLY_REPORT,
    GENERATE_NEW_REPORT,
    DELETE_REPORT,
    //***************************************************//
    //---forgot Code---
    FORGOT_CODE,
    FORGOT_CODE_FOUND,
    FORGOT_CODE_NOT_FOUND,
    
    // --- Restaurant Management and Status ---
    GET_TABLES_STATUS,      // Client requests current status of all tables
    UPDATE_TABLE_STATUS,    // Rep/Manager changes a table's status
    
    // --- Transactions and Reports ---
    PAY_BILL_REQUEST,       // Client sends payment details
    GET_REPORTS,            // Manager requests performance reports
    
    // --- System Responses ---
    ERROR_RESPONSE,         // Server sends an error message (Content=String)
    SUCCESS_RESPONSE,       // Server confirms successful operation
    
    
}