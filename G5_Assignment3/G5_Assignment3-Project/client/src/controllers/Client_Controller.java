package controllers;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.KryoUtil;
import gui.*;
import entities.*;
import messages.Message;
import messages.MessageType;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * The main controller for the client-side application.
 * It manages the network connection to the server and routes incoming responses
 * to the appropriate GUI screens using a Handler Map pattern.
 */
public class Client_Controller implements ChatIF {

    final public static int DEFAULT_PORT = 5555;
    ChatClient client;
    
    // Map to store response handlers for each message type
    private Map<MessageType, ResponseHandler> responseHandlers;
    
    // Reference to specific GUIs that need direct updates
    private ManageUsers_GUI manageUsers_GUI;

    public Client_Controller(String host, int port) throws IOException {
        try {
            System.out.println("Connecting to host: " + host);
            client = new ChatClient(host, port, this);
            
            // Initialize the response handler map
            initializeHandlers();
            
        } catch (IOException exception) {
            System.out.println("Error: Can't setup connection! Terminating client.");
            System.exit(1);
        }
    }

    /**
     * Sets up the mapping between MessageTypes and their specific handling logic.
     * This uses Anonymous Inner Classes to avoid Lambda expressions.
     */
    private void initializeHandlers() {
        responseHandlers = new HashMap<>();

        // -----------------------------------------------------------
        // Authentication Handlers
        // -----------------------------------------------------------

        responseHandlers.put(MessageType.LOGIN_SUCCESS_GUEST, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (MainScreen_GUI.instance != null) {
                    MainScreen_GUI.instance.onGuestLoginSuccess();
                }
            }
        });

        responseHandlers.put(MessageType.LOGIN_SUCCESS_SUB, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                Subscribed_Customer sub = (Subscribed_Customer) msg.getContent();
                User_Session.setLoggedInUser(sub);
                if (MainScreen_GUI.instance != null) {
                    MainScreen_GUI.instance.onSubLoginSuccess(sub);
                }
            }
        });

        responseHandlers.put(MessageType.LOGIN_FAILED_GUEST, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (MainScreen_GUI.instance != null) {
                    MainScreen_GUI.instance.onGuestLoginFailure(msg);
                }
            }
        });

        responseHandlers.put(MessageType.LOGIN_FAILED_SUB, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (MainScreen_GUI.instance != null) {
                    MainScreen_GUI.instance.onSubLoginFailure();
                }
            }
        });

        // -----------------------------------------------------------
        // Reservation Queries
        // -----------------------------------------------------------

        responseHandlers.put(MessageType.RETURN_RESERVATIONS_BY_USER, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                @SuppressWarnings("unchecked")
                List<Reservation> resList = (List<Reservation>) msg.getContent();
                if (ViewReservations_GUI.instance != null) {
                    ViewReservations_GUI.instance.updateTable(resList);
                }
            }
        });

        responseHandlers.put(MessageType.RETURN_ALL_PENDING_RESERVATIONS, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                @SuppressWarnings("unchecked")
                List<Reservation> adminList = (List<Reservation>) msg.getContent();
                if (ManageOrders_GUI.instance != null) {
                    ManageOrders_GUI.instance.updateAdminUI(adminList);
                }
            }
        });

        // -----------------------------------------------------------
        // Reservation Actions
        // -----------------------------------------------------------

        responseHandlers.put(MessageType.RESERVATION_CONFIRMED, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                int confirmationCode = (Integer) msg.getContent();
                if (AddReservation_GUI.instance != null) {
                    AddReservation_GUI.instance.showSuccessAlert(confirmationCode);
                } else if (AddManualReservation_GUI.instance != null) {
                    AddManualReservation_GUI.instance.showSuccessAlert(confirmationCode);
                }
            }
        });

        responseHandlers.put(MessageType.RESERVATION_FAILED_NO_TABLE, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                LocalDateTime suggestedTime = (LocalDateTime) msg.getContent();
                if (AddReservation_GUI.instance != null) {
                    AddReservation_GUI.instance.showNoTableAlert(suggestedTime);
                } else if (AddManualReservation_GUI.instance != null) {
                    AddManualReservation_GUI.instance.showNoTableAlert(suggestedTime);
                } else if (ViewReservations_GUI.instance != null) {
                    ViewReservations_GUI.instance.showNoTableAlert(suggestedTime);
                }
            }
        });

        responseHandlers.put(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (AddReservation_GUI.instance != null) {
                    AddReservation_GUI.instance.showNoTableAlert(null);
                } else if (ViewReservations_GUI.instance != null) {
                    ViewReservations_GUI.instance.showNoTableAlert(null);
                } else if (AddManualReservation_GUI.instance != null) {
                    AddManualReservation_GUI.instance.showNoTableAlert(null);
                }
            }
        });

        responseHandlers.put(MessageType.RESERVATION_FAILED, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                String errorMsg = (String) msg.getContent();
                System.err.println("Server Error: " + errorMsg);
            }
        });

        // -----------------------------------------------------------
        // Updates, Cancellations & Admin Actions
        // -----------------------------------------------------------

        responseHandlers.put(MessageType.RESERVATION_UPDATE_SUCCESS, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (ViewReservations_GUI.instance != null) {
                    ViewReservations_GUI.instance.showSuccessAlert();
                    refreshUserReservations();
                }
            }
        });

        responseHandlers.put(MessageType.ADMIN_UPDATE_SUCCESS, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (ManageOrders_GUI.instance != null) {
                    ManageOrders_GUI.instance.refreshAdminData();
                }
            }
        });

        responseHandlers.put(MessageType.RESERVATION_CANCELED, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (ViewReservations_GUI.instance != null) {
                    refreshUserReservations();
                } else if (ManageOrders_GUI.instance != null) {
                    ManageOrders_GUI.instance.refreshAdminData();
                }
            }
        });

        // -----------------------------------------------------------
        // General Data (Opening Hours)
        // -----------------------------------------------------------

        responseHandlers.put(MessageType.RETURN_OPENING_HOURS, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                Opening_Hours oh = (Opening_Hours) msg.getContent();
                Restaurant.getInstance().setOpeningHours(oh);
                if (AddReservation_GUI.instance != null) {
                    LocalDate currentDate = AddReservation_GUI.instance.getDatePicker().getValue();
                    if (currentDate != null) {
                        AddReservation_GUI.instance.loadDynamicHours(currentDate);
                    }
                }
            }
        });

        // -----------------------------------------------------------
        // User Management
        // Reused handler for multiple message types to avoid code duplication
        // -----------------------------------------------------------

        ResponseHandler userHandler = new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (manageUsers_GUI != null) {
                    manageUsers_GUI.handle(msg);
                }
            }
        };

        responseHandlers.put(MessageType.GET_ALL_USERS_RESPONSE, userHandler);
        responseHandlers.put(MessageType.ADD_USER_RESPONSE_OK, userHandler);
        responseHandlers.put(MessageType.ADD_USER_RESPONSE_ERR, userHandler);
        responseHandlers.put(MessageType.EDIT_USER_RESPONSE_OK, userHandler);
        responseHandlers.put(MessageType.EDIT_USER_RESPONSE_ERR, userHandler);
        responseHandlers.put(MessageType.DELETE_USER_RESPONSE_OK, userHandler);
        responseHandlers.put(MessageType.DELETE_USER_RESPONSE_ERR, userHandler);

        // -----------------------------------------------------------
        // Table Management
        // -----------------------------------------------------------

        responseHandlers.put(MessageType.RETURN_ALL_TABLES, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                @SuppressWarnings("unchecked")
                List<Restaurant_Table> tables = (List<Restaurant_Table>) msg.getContent();
                if (ManageTables_GUI.instance != null) {
                    ManageTables_GUI.instance.loadTables(tables);
                }
            }
        });

        responseHandlers.put(MessageType.TABLE_OPERATION_FAILED, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                final String error = (String) msg.getContent();
                // Alert must be shown on JavaFX thread (handled by display wrapper)
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Table Operation Failed");
                alert.setHeaderText(null);
                alert.setContentText(error != null ? error : "Operation failed");
                alert.showAndWait();
            }
        });

        responseHandlers.put(MessageType.TABLE_OPERATION_SUCCESS, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (ManageTables_GUI.instance != null) {
                    // Refresh table list
                    sendComplexObject(new Message(MessageType.GET_ALL_TABLES, null));
                }
            }
        });
    }

    /**
     * Helper method to refresh reservations for the currently logged-in user.
     * Used after updates or cancellations.
     */
    private void refreshUserReservations() {
        Object user;
        if (User_Session.getLoggedInUser() != null) {
            user = User_Session.getLoggedInUser();
        } else {
            user = User_Session.getCasualPhone();
        }
        sendGetReservationsRequest(user);
    }

    /**
     * Handles messages received from the server. Deserializes the byte array and
     * routes the response to the appropriate handler using the map.
     * All UI updates are executed on the JavaFX Application Thread.
     * * @param message The serialized message (byte array) received from the server.
     */
    @Override
    public void display(Object message) {
        if (message instanceof byte[]) {
            Object obj = KryoUtil.deserialize((byte[]) message);
            
            if (obj instanceof Message) {
                final Message receivedMsg = (Message) obj;
                MessageType type = receivedMsg.getType();

                // Check if a handler is registered for this message type
                if (responseHandlers.containsKey(type)) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                responseHandlers.get(type).handle(receivedMsg);
                            } catch (Exception e) {
                                System.err.println("Error in handler for type: " + type);
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    System.out.println("Client_Controller: Received unhandled message type: " 
                            + type + " Content: " + receivedMsg.getContent());
                }
            }
        }
    }

    
    // ----------------------------------------------------------------------
    // Request Sending Methods 
    // ----------------------------------------------------------------------

    public void setManageUsersGUI(ManageUsers_GUI manageUsers_GUI) {
        this.manageUsers_GUI = manageUsers_GUI;
    }

    public void sendGetReservationsRequest(Object identifier) {
        try {
            Message message;
            if (identifier instanceof Subscribed_Customer) {
                System.out.println("Client_Controller: Requesting reservations for Subscriber: "
                        + ((Subscribed_Customer) identifier).getSubscriberCode());
                message = new Message(MessageType.GET_RESERVATIONS_BY_USER, identifier);
            } else if (identifier instanceof String) {
                System.out.println("Client_Controller: Requesting reservations for Casual Customer: " + identifier);
                message = new Message(MessageType.GET_RESERVATIONS_BY_USER, (String) identifier);
            } else {
                System.err.println("Client_Controller: Unknown identifier type!");
                return;
            }
            sendComplexObject(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void sendUpdateReservationRequest(Reservation reservationToUpdate) {
        try {
            Message msg = new Message(MessageType.UPDATE_RESERVATION_REQUEST, reservationToUpdate);
            sendComplexObject(msg);
            System.out.println("Update request sent for reservation ID: " + reservationToUpdate.getId());
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void sendNewReservationRequest(Reservation newRes) {
        try {
            Message msg = new Message(MessageType.CREATE_RESERVATION, newRes);
            sendComplexObject(msg);
            System.out.println("Client_Controller: Reservation request sent for: " + 
                (newRes.getUserId() != null ? "Subscriber " + newRes.getUserId() : "Casual Customer"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void sendGetOpeningHoursRequest() {
        try {
            Message msg = new Message(MessageType.GET_OPENING_HOURS, null);
            sendComplexObject(msg);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void sendGetAllPendingReservationsRequest() {
        try {
            System.out.println("Client_Controller: Requesting all pending reservations for management.");
            Message message = new Message(MessageType.GET_ALL_PENDING_RESERVATIONS, null);
            sendComplexObject(message);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void sendComplexObject(Object obj) {
        try {
            byte[] payload = KryoUtil.serialize(obj);
            client.handleMessageFromClientUI(payload);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void logout() {
        try {
            Message msg = new Message(MessageType.LOGOUT_REQUEST, null);
            sendComplexObject(msg);
            System.out.println("Client_Controller: Logout request sent to server.");
            client.quit();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void sendSubscriberLoginRequest(LoginData loginData) {
        System.out.println("Subscriber Login Attempt: " + loginData.getUsername());
        Message loginMessage = new Message(MessageType.LOGIN_REQUEST_SUB, loginData);
        if (ConnectToServer_GUI.clientController != null) {
            try { sendComplexObject(loginMessage); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void sendGuestLoginRequest(LoginData loginData) {
        System.out.println("Guest Login Attempt: " + (loginData.getEmail() != null ? loginData.getEmail() : loginData.getPhoneNumber()));
        Message loginMessage = new Message(MessageType.LOGIN_REQUEST_GUEST, loginData);
        if (ConnectToServer_GUI.clientController != null) {
            try { sendComplexObject(loginMessage); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void sendGetAllUsersRequest() {
        System.out.println("Get all users attempt");
        Message message = new Message(MessageType.GET_ALL_USERS_REQUEST);
        if (ConnectToServer_GUI.clientController != null) {
            try { sendComplexObject(message); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void sendAddUserRequest(UserRecord newUser) {
        System.out.println("Add new user attempt");
        Message message = new Message(MessageType.ADD_USER_REQUEST, newUser);
        if (ConnectToServer_GUI.clientController != null) {
            try { sendComplexObject(message); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void sendEditUserRequest(UserRecord user) {
        System.out.println("Add new user attempt");
        Message message = new Message(MessageType.EDIT_USER_REQUEST, user);
        if (ConnectToServer_GUI.clientController != null) {
            try { sendComplexObject(message); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void sendRemoveUserRequest(UserRecord user) {
        System.out.println("Add new user attempt");
        Message message = new Message(MessageType.DELETE_USER_REQUEST, user);
        if (ConnectToServer_GUI.clientController != null) {
            try { sendComplexObject(message); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}