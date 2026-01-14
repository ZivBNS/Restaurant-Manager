package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import commands.*;
import utils.KryoUtil;
import gui.Server_GUI;
import messages.Message;
import messages.MessageType;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * The central controller for the server, extending the OCSF AbstractServer.
 * It manages client connections, deserializes incoming messages using Kryo, 
 * and dispatches them to specific command handlers using a Command Pattern.
 */
public class Server_Controller extends AbstractServer {

	/** The default port on which the server listens. */
	final public static int DEFAULT_PORT = 5555;
	private Server_GUI gui;
	private final User_Controller userController;
	private ServerWatchdog watchdog;
    /** The mapping of MessageTypes to their corresponding Command execution objects. */
	// The Command Map
	private Map<MessageType, Command> commands;

	/**
	 * Constructs the server controller and initializes internal components.
	 * * @param port The port to listen on.
	 * @param gui  The graphical user interface for logging server events.
	 */
	public Server_Controller(int port, Server_GUI gui) {
		super(port);
		this.gui = gui;
		this.userController = new User_Controller();
		this.watchdog = new ServerWatchdog(); 
		initializeCommands();
	}

	/**
	 * Registers all command handlers in the HashMap.
	 * Groups commands by category: Authentication, Check-in, Reservations, Tables, 
	 * Users, Payment, Waitlists, Reporting, and Opening Hours.
	 * Includes a special lambda handler for logout requests.
	 */
	private void initializeCommands() {
		commands = new HashMap<>();

		// --- Authentication ---
		Command loginCmd = new LoginCommand();
		commands.put(MessageType.LOGIN_REQUEST_SUB, loginCmd);
		commands.put(MessageType.LOGIN_REQUEST_GUEST, loginCmd);
		commands.put(MessageType.LOGIN_REQUEST_EMP, loginCmd);
		
		// --- check in ---
		Command ckInCmd = new CheckInCommand();
		commands.put(MessageType.CHECK_IN_REQUEST, ckInCmd);
		
		// --- Reservations ---
		Command resCmd = new ReservationCommand();
		commands.put(MessageType.CREATE_INSTANT_RESERVATION, resCmd);
		commands.put(MessageType.CREATE_RESERVATION, resCmd);
		commands.put(MessageType.CANCEL_RESERVATION, resCmd);
		commands.put(MessageType.GET_RESERVATIONS_BY_USER, resCmd);
		commands.put(MessageType.UPDATE_RESERVATION_REQUEST, resCmd);
		commands.put(MessageType.GET_ALL_PENDING_RESERVATIONS, resCmd);
        commands.put(MessageType.GET_ALL_PENDING_AND_ACTIVE_RESERVATIONS, resCmd);
		commands.put(MessageType.ADMIN_UPDATE_RESERVATION, resCmd);
		commands.put(MessageType.GET_LATEST_RESERVATION_BY_PHONE, resCmd);
		commands.put(MessageType.GET_LATEST_RESERVATION_BY_EMAIL, resCmd);
		commands.put(MessageType.GET_RESERVATION_HISTORY, resCmd);
		
		// --- Tables ---
		Command tableCmd = new TableCommand();
		commands.put(MessageType.GET_ALL_TABLES, tableCmd);
		commands.put(MessageType.ADD_TABLE_REQUEST, tableCmd);
		commands.put(MessageType.UPDATE_TABLE_REQUEST, tableCmd);
		commands.put(MessageType.DELETE_TABLE_REQUEST, tableCmd);

		// --- Users ---
		UserCommand userCmd = new UserCommand(this.userController,this);
		commands.put(MessageType.GET_ALL_USERS_REQUEST, userCmd);
		commands.put(MessageType.ADD_USER_REQUEST, userCmd);
		commands.put(MessageType.EDIT_USER_REQUEST, userCmd);
		commands.put(MessageType.DELETE_USER_REQUEST, userCmd);
		commands.put(MessageType.GET_USER_DETAILS, userCmd);
		commands.put(MessageType.UPDATE_USER_DETAILS_REQUEST, userCmd);
		commands.put(MessageType.FORGOT_CODE, userCmd);		
		
		// --- Payment & Bills ---
		Command payCmd = new PaymentCommand();
		commands.put(MessageType.BILL_REQUEST, payCmd);
		commands.put(MessageType.GET_BILL_BY_RESERVATION_ID, payCmd);
		commands.put(MessageType.BILL_PAYMENT_REQUEST, payCmd);
		commands.put(MessageType.GET_ALL_BILLS, payCmd);
		commands.put(MessageType.DELETE_BILL, payCmd);

		// --- waitlists ---
		Command WaitlistCmd = new WaitlistCommand();
		commands.put(MessageType.CANCEL_WAITLIST_AND_RESERVATION_BY_CODE, WaitlistCmd);
		commands.put(MessageType.JOIN_WAITLIST, WaitlistCmd);
		commands.put(MessageType.GET_ALL_ACTIVE_WAITLISTS, WaitlistCmd);
		
		// --- Reporting ---
		Command reportCmd = new ReportCommand();
		commands.put(MessageType.GET_MONTHLY_REPORT, reportCmd);
		
		// --- Opening Hours Management ---
		Command ohCmd = new OpeningHoursCommand(this);
		commands.put(MessageType.GET_OPENING_HOURS, ohCmd);
		commands.put(MessageType.UPDATE_REGULAR_HOURS, ohCmd);
		commands.put(MessageType.ADD_SPECIAL_HOUR, ohCmd);
		commands.put(MessageType.DELETE_SPECIAL_HOUR, ohCmd);

		// LOGOUT is special, handle directly or via command that returns null
		commands.put(MessageType.LOGOUT_REQUEST, (msg, client) -> {
			log("Client disconnected: " + client.getInetAddress());
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null; // No response needed
		});
	}

	/**
	 * Handles raw byte arrays received from clients.
	 * Deserializes the data into a Message object, executes the corresponding command, 
	 * and sends the serialized response back to the client.
	 * * @param msg    The serialized byte array received from the client.
	 * @param client The connection to the client that sent the message.
	 */
	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		// 1. Deserialization
		Object receivedMessageDeserialized = KryoUtil.deserialize((byte[]) msg);

		if (!(receivedMessageDeserialized instanceof Message)) {
			log("Error: Invalid message format from " + client);
			return;
		}

		Message clientMsg = (Message) receivedMessageDeserialized;
		MessageType type = clientMsg.getType();

		log("Processing command: " + type);

		// 2. Command Execution via HashMap
		if (commands.containsKey(type)) {
			try {
				// Execute the specific command
				Message response = commands.get(type).execute(clientMsg, client);

				// 3. Send Response
				if (response != null) {
					client.sendToClient(KryoUtil.serialize(response));
				}
			} catch (Exception e) {
				log("Error executing command " + type + ": " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			log("Warning: No command registered for " + type);
			try {
				client.sendToClient(KryoUtil.serialize(new Message(MessageType.ERROR_RESPONSE, "Unknown Command")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Triggered when the server starts listening.
	 * Initializes the watchdog monitor and the report scheduler.
	 */
	@Override
	protected void serverStarted() {
		log("Server listening for connections on port " + getPort());
		if (watchdog != null) {
	        watchdog.start(); // Start the monitoring thread
	    }
		ServerScheduler.startReportScheduler();
	}

	/**
	 * Triggered when the server stops listening.
	 * Gracefully stops the watchdog monitor and the report scheduler.
	 */
	@Override
	protected void serverStopped() {
		log("Server has stopped listening for connections.");
		if (watchdog != null) {
	        watchdog.stop(); // Stop the monitoring thread gracefully
	    }
		ServerScheduler.stopScheduler();
	}

	/**
	 * Triggered when a new client connects to the server.
	 * * @param client The connection to the newly connected client.
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		log("Client connected: " + client);
	}

	/**
	 * Internal helper for logging messages.
	 * Routes logs to the Server_GUI if available; otherwise, prints to the standard system output.
	 * * @param message The string message to log.
	 */
	private void log(String message) {
		if (gui != null) {
			gui.appendLog(message);
		} else {
			System.out.println(message);
		}
	}
	
	/**
     * Broadcasts a message to all connected clients to trigger a UI refresh.
     * This is called by Commands after a successful database update.
     * * @param message The message object containing the new data or a refresh signal.
     */
    public void broadcastToAllClients(Message message) {
        // Serialize the message using Kryo before sending
        sendToAllClients(KryoUtil.serialize(message));
    }
}