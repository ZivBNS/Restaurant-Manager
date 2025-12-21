package controllers;

import java.io.IOException;
import utils.KryoUtil;
import entities.LoginData;
import entities.Opening_Hours;
import entities.Restaurant;
import entities.User;
import gui.Server_GUI;
import messages.Message;
import messages.MessageType;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * Main Server Controller that handles OCSF connections and Kryo serialization.
 * Acts as the primary router for all client requests.
 */
public class Server_Controller extends AbstractServer {

    final public static int DEFAULT_PORT = 5555;
    private Server_GUI gui;

    public Server_Controller(int port, Server_GUI gui) {
        super(port); 
        this.gui = gui;
    }

    /**
     * Handles incoming byte arrays from clients, deserializes them, 
     * and routes them to the appropriate logic controller.
     */
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        // 1. Deserialization using Kryo
        Object receivedMessageDeserialized = KryoUtil.deserialize((byte[]) msg);
        
        // 2. Input Validation
        if (receivedMessageDeserialized == null || !(receivedMessageDeserialized instanceof Message)) {
            log("Error: Invalid message format from " + client);
            return;
        }

        Message clientMsg = (Message) receivedMessageDeserialized;
        Message serverResponse = null;

        log("Processing command: " + clientMsg.getType());

        try {
            // 3. Routing Logic: Handle specific commands based on MessageType
            switch (clientMsg.getType()) {

                // --- Authentication ---
                case LOGIN_REQUEST_SUB:
                    if(clientMsg.getContent() instanceof LoginData) {
                    	serverResponse = Login_Controller.handleSubLogin((LoginData)clientMsg.getContent());
                    	break;
                    }else {
                    	serverResponse = new Message(MessageType.LOGIN_FAILED_SUB, "Unexpected user data");
                    	break;
                    }
                    
                case LOGIN_REQUEST_GUEST:
                    if(clientMsg.getContent() instanceof LoginData) {
                    	serverResponse = Login_Controller.handleGuestLogin((LoginData)clientMsg.getContent());
                    	break;
                    }else {
                    	serverResponse = new Message(MessageType.LOGIN_FAILED_GUEST, "Unexpected user data");
                    	break;
                    }
                    

                case LOGOUT_REQUEST:
                    log("Client disconnected: " + client.getInetAddress());
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                // --- Reservation Management ---
                case CREATE_RESERVATION:
                case CANCEL_RESERVATION:
                case GET_RESERVATIONS_BY_USER:
                case UPDATE_RESERVATION_REQUEST:
                case GET_ALL_PENDING_RESERVATIONS: 
                case ADMIN_UPDATE_RESERVATION:    
                    log("Handling Reservation Request: " + clientMsg.getType());
                    // Static call to the logic controller as per your existing pattern
                    serverResponse = Reservation_Controller.handleMessage(clientMsg);
                    break;
                    
                // --- Waitlist Management ---
                case JOIN_WAITLIST:
                    // Logic to be implemented in Waitlist_Controller
                    serverResponse = new Message(MessageType.SUCCESS_RESPONSE, "Added to Waitlist");
                    break;

                // --- Restaurant Status ---
                case GET_TABLES_STATUS:
                    serverResponse = new Message(MessageType.TEXT_MESSAGE, "Table status list (Mock)");
                    break;

                case UPDATE_TABLE_STATUS:
                    // Logic to update bistro_tables
                    break;
                
                // Reports (Manager Only)
                case GET_REPORTS:
                    // Logic to be implemented in Report_Controller
                    break;
                case GET_OPENING_HOURS:
                    // The OpeningHours_Repository has already loaded the hours into the Restaurant singleton during init()
                    Opening_Hours hours = Restaurant.getInstance().getOpeningHours();  
                    serverResponse= new Message(MessageType.RETURN_OPENING_HOURS, hours);
                    break;
                    
                 // --- Tables Management ---
                case GET_ALL_TABLES:
                case ADD_TABLE_REQUEST:
                case UPDATE_TABLE_REQUEST:
                case DELETE_TABLE_REQUEST:
                    serverResponse = Table_Controller.handle(clientMsg);
                    break;


                case GET_USER_DETAILS:
                    
                    break;
                    
                default:
                    log("Warning: Unknown command received: " + clientMsg.getType());
                    serverResponse = new Message(MessageType.ERROR_RESPONSE, "Unknown Command");
            }

            // 4. Send Response: Serialize and send back to the specific client
            if (serverResponse != null) {
            	log("Sending message to client: " + serverResponse.getType());
                try {
                    byte[] payload = KryoUtil.serialize(serverResponse);
                    client.sendToClient(payload); 
                } catch (IOException e) {
                    log("Error sending response to client: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            log("Critical Error during message processing: " + e.getMessage());
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