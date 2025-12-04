package controllers;


import entities.Reservation;
import entities.User;
import gui.Server_GUI;
import messages.Message;
import messages.MessageType;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class Server_Controller extends AbstractServer {

    final public static int DEFAULT_PORT = 5555;
    private Server_GUI gui;


    public Server_Controller(int port, Server_GUI gui) {
        super(port); 
        this.gui = gui;
    }

    
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        // 1. Input Validation: Check if message is valid
        if (msg == null || !(msg instanceof Message)) {
            log("Error: Invalid message format from " + client);
            return;
        }

        Message clientMsg = (Message) msg;
        Message serverResponse = null;

        log("Processing command: " + clientMsg.getType());

        try {
            // 2. Switch Case: Handle specific commands
            switch (clientMsg.getType()) {

                // --- Authentication ---
                case LOGIN_REQUEST:
                    // Extract User object from message content
                    User user = (User) clientMsg.getContent();
                    // TODO: Check credentials in DB (LoginController)
                    if ("admin".equals(user.getUsername())) { 
                        serverResponse = new Message(MessageType.LOGIN_SUCCESS, "Welcome!");
                    } else {
                        serverResponse = new Message(MessageType.LOGIN_FAILED, "Invalid Pass");
                    }
                    break;

                case LOGOUT_REQUEST:
                    log("Client disconnected: " + client.getInetAddress());
                    // TODO: Update 'isLoggedIn' status in DB
                    break;

                // --- Reservation Management ---
                case CREATE_RESERVATION:
                    Reservation res = (Reservation) clientMsg.getContent();
                    // TODO: Check table availability (ReservationController)
                    log("New reservation request for " + res.getNumDiners() + " people.");
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Reservation Confirmed");
                    break;

                case CANCEL_RESERVATION:
                    // TODO: Update reservation status to 'CANCELLED' in DB
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Reservation Cancelled");
                    break;
                
                case GET_RESERVATIONS_LIST:
                    // TODO: get reservation status to  in DB
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Reservation Cancelled");
                    break;
                case GET_RESERVATIONS_BY_USER:
                	System.out.println("switch GET_RESERVATIONS_BY_USER - Server_Controller");
                    Reservation_Controller.handleMessage(clientMsg, client);
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Recived Reservation By User");
                    break;
                
                case UPDATE_RESERVATION:
                    // TODO: Update reservation information
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Reservation Updated");
                    break;

                // --- Waitlist Management ---
                case JOIN_WAITLIST:
                    // TODO: Add WaitlistEntry to DB (WaitlistController)
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Added to Waitlist");
                    break;

                // --- Restaurant Status (Live Map) ---
                case GET_TABLES_STATUS:
                    // TODO: Fetch all tables and their status from DB
                    // List<RestaurantTable> tables = TableRepository.getAll();
                    serverResponse = new Message(MessageType.TEXT_MESSAGE, "List of Tables (Mock)");
                    break;

                case UPDATE_TABLE_STATUS:
                    // TODO: Change table status (e.g., OCCUPIED -> AVAILABLE)
                    break;
                
                // Reports (Manager Only)
                case GET_REPORTS:
                    // TODO: Generate report (ReportController)
                    break;

                default:
                    log("Warning: Unknown command received.");
                    serverResponse = new Message(MessageType.ERROR_RESPONSE, "Unknown Command");
            }

            // 3. Send Response: If a response object was created, send it back
            if (serverResponse != null) {
                client.sendToClient(serverResponse);
            }

        } catch (Exception e) {
            log("Critical Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    protected void serverStarted() {
        log("Server listening for connections on port " + getPort());
    }

    @Override
    protected void serverStopped() {
        log("Server has stopped listening for connections.");
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        log("Client connected: " + client);
    }

    private void log(String message) {
        if (gui != null) {
            gui.appendLog(message);
        } else {
            System.out.println(message);
        }
    }
}