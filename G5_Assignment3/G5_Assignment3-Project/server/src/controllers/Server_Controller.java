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

public class Server_Controller extends AbstractServer {

	final public static int DEFAULT_PORT = 5555;
	private Server_GUI gui;
	private final User_Controller userController;
	private ServerWatchdog watchdog;
	// The Command Map
	private Map<MessageType, Command> commands;

	public Server_Controller(int port, Server_GUI gui) {
		super(port);
		this.gui = gui;
		this.userController = new User_Controller();
		this.watchdog = new ServerWatchdog(); 
		initializeCommands();
	}

	/**
	 * Registers all command handlers in the HashMap.
	 */
	private void initializeCommands() {
		commands = new HashMap<>();

		// --- Authentication ---
		Command loginCmd = new LoginCommand();
		commands.put(MessageType.LOGIN_REQUEST_SUB, loginCmd);
		commands.put(MessageType.LOGIN_REQUEST_GUEST, loginCmd);

		
		// --- check in ---
		Command ckInCmd = new CheckInCommand();
		commands.put(MessageType.CHECK_IN_REQUEST, ckInCmd);
		
		// --- Reservations ---
		Command resCmd = new ReservationCommand();
		commands.put(MessageType.CREATE_INSTANT_RESERVATION, resCmd);
		commands.put(MessageType.CREATE_RESERVATION, resCmd);
		commands.put(MessageType.CANCEL_RESERVATION, resCmd);
		commands.put(MessageType.CANCEL_RESERVATION_BY_CODE, resCmd);
		commands.put(MessageType.GET_RESERVATIONS_BY_USER, resCmd);
		commands.put(MessageType.UPDATE_RESERVATION_REQUEST, resCmd);
		commands.put(MessageType.GET_ALL_PENDING_RESERVATIONS, resCmd);
		commands.put(MessageType.ADMIN_UPDATE_RESERVATION, resCmd);
		commands.put(MessageType.GET_LATEST_RESERVATION_BY_PHONE, resCmd);

		// --- Tables ---
		Command tableCmd = new TableCommand();
		commands.put(MessageType.GET_ALL_TABLES, tableCmd);
		commands.put(MessageType.ADD_TABLE_REQUEST, tableCmd);
		commands.put(MessageType.UPDATE_TABLE_REQUEST, tableCmd);
		commands.put(MessageType.DELETE_TABLE_REQUEST, tableCmd);

		// --- Users ---
		UserCommand userCmd = new UserCommand(this.userController);
		commands.put(MessageType.GET_ALL_USERS_REQUEST, userCmd);
		commands.put(MessageType.ADD_USER_REQUEST, userCmd);
		commands.put(MessageType.EDIT_USER_REQUEST, userCmd);
		commands.put(MessageType.DELETE_USER_REQUEST, userCmd);
		commands.put(MessageType.GET_USER_DETAILS, userCmd);
		commands.put(MessageType.UPDATE_USER_DETAILS_REQUEST, userCmd);
		// --- Payment & Bills ---
		Command payCmd = new PaymentCommand();
		commands.put(MessageType.GET_LATEST_BILL_BY_PHONE, payCmd);
		commands.put(MessageType.BILL_REQUEST, payCmd);
		commands.put(MessageType.GET_BILL_BY_RESERVATION_ID, payCmd);
		commands.put(MessageType.BILL_PAYMENT_REQUEST, payCmd);
		commands.put(MessageType.GET_ALL_BILLS, payCmd);
		commands.put(MessageType.CREATE_BILL, payCmd);
		commands.put(MessageType.DELETE_BILL, payCmd);

		// --- waitlists ---
		Command WaitlistCmd = new WaitlistCommand();
		commands.put(MessageType.CANCEL_WAITLIST_AND_RESERVATION_BY_CODE, WaitlistCmd);
		commands.put(MessageType.JOIN_WAITLIST, WaitlistCmd);

		// --- Reporting ---
		Command reportCmd = new ReportCommand();
		commands.put(MessageType.GET_MONTHLY_REPORT, reportCmd);
		commands.put(MessageType.GENERATE_NEW_REPORT, reportCmd);
		commands.put(MessageType.DELETE_REPORT, reportCmd);
		// --- Opening Hours Management ---
		Command ohCmd = new OpeningHoursCommand();
		commands.put(MessageType.GET_OPENING_HOURS, ohCmd);
		commands.put(MessageType.UPDATE_REGULAR_HOURS, ohCmd);
		commands.put(MessageType.ADD_SPECIAL_HOUR, ohCmd);
		commands.put(MessageType.DELETE_REGULAR_HOURS, ohCmd);
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

	@Override
	protected void serverStarted() {
		log("Server listening for connections on port " + getPort());
		if (watchdog != null) {
	        watchdog.start(); // Start the monitoring thread
	    }
		ServerScheduler.startReportScheduler();
	}

	@Override
	protected void serverStopped() {
		log("Server has stopped listening for connections.");
		if (watchdog != null) {
	        watchdog.stop(); // Stop the monitoring thread gracefully
	    }
		ServerScheduler.stopScheduler();
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