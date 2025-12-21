package controllers;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import utils.KryoUtil;
import gui.AddManualReservation_GUI;
import gui.AddReservation_GUI;
import gui.ConnectToServer_GUI;
import gui.MainScreen_GUI;
import gui.ManageOrders_GUI;
import gui.User_Session;
import gui.ViewReservations_GUI;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import entities.Casual_Customer;
import entities.LoginData;
import entities.Opening_Hours;
import entities.Reservation;
import entities.Restaurant;
import entities.Subscribed_Customer;
import messages.Message;
import messages.MessageType;

public class Client_Controller implements ChatIF {

	final public static int DEFAULT_PORT = 5555;
	ChatClient client;

	public Client_Controller(String host, int port) throws IOException {
		try {
			System.out.println(host);
			client = new ChatClient(host, port, this);
		} catch (IOException exception) {
			System.out.println("Error: Can't setup connection!" + " Terminating client");
			System.exit(1);
		}
	}

	/**
	 * Requests the list of reservations from the server based on the provided
	 * identifier. The identifier can be a Subscribed_Customer object or a String
	 * (Phone/Email). * @param identifier The user identification (Subscriber object
	 * or String contact info).
	 */
	public void sendGetReservationsRequest(Object identifier) {
		try {
			Message message;

			if (identifier instanceof Subscribed_Customer) {
				// Identifier is a member - sending the whole object or just the code
				System.out.println("Client_Controller: Requesting reservations for Subscriber: "
						+ ((Subscribed_Customer) identifier).getSubscriberCode());
				message = new Message(MessageType.GET_RESERVATIONS_BY_USER, identifier);
			} else if (identifier instanceof String) {
				// Identifier is a casual customer's phone/email
				System.out.println("Client_Controller: Requesting reservations for Casual Customer: " + identifier);
				message = new Message(MessageType.GET_RESERVATIONS_BY_USER, (String) identifier);
			} else {
				System.err.println("Client_Controller: Unknown identifier type!");
				return;
			}

			// Send the serialized message via Kryo/OCSF
			sendComplexObject(message);

		} catch (Exception ex) {
			System.err.println("Unexpected error while sending a reservation request: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void sendUpdateReservationRequest(Reservation reservationToUpdate) {
		try {
			Message msg = new Message(MessageType.UPDATE_RESERVATION_REQUEST, reservationToUpdate);
			// client.handleMessageFromClientUI(msg);
			sendComplexObject(msg);
			System.out.println("Update request sent for reservation ID: " + reservationToUpdate.getId());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a new reservation request to the server. Use this for both Casual and
	 * Subscribed customers as it passes a Reservation object.
	 * 
	 * @param newRes The reservation entity to be persisted.
	 */
	public void sendNewReservationRequest(Reservation newRes) {
		try {
			Message msg = new Message(MessageType.CREATE_RESERVATION, newRes);
			sendComplexObject(msg);
			System.out.println("Client_Controller: Reservation request sent for: "
					+ (newRes.getUserId() != null ? "Subscriber " + newRes.getUserId() : "Casual Customer"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendGetOpeningHoursRequest() {
		try {
			Message msg = new Message(MessageType.GET_OPENING_HOURS, null);
			sendComplexObject(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a request to the server to retrieve all reservations currently in
	 * 'PENDING' status. This is used specifically for the Employee/Manager
	 * management dashboard.
	 */
	public void sendGetAllPendingReservationsRequest() {
		try {
			System.out.println("Client_Controller: Requesting all pending reservations for management.");

			// Create a message with the specific management type
			Message message = new Message(MessageType.GET_ALL_PENDING_RESERVATIONS, null);

			// Send via the existing serialized OCSF pipeline
			sendComplexObject(message);

		} catch (Exception e) {
			System.err.println("Error while sending request for pending reservations: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void sendComplexObject(Object obj) {
		try {
			// Convert to bytes using Kryo
			byte[] payload = KryoUtil.serialize(obj);

			// Send the byte array using OCSF
			client.handleMessageFromClientUI(payload);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void logout() {
		try {
			Message msg = new Message(MessageType.LOGOUT_REQUEST, null);
			sendComplexObject(msg);

			System.out.println("Client_Controller: Logout request sent to server.");
			client.quit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendSubscriberLoginRequest(LoginData loginData) {
		System.out.println("Subscriber Login Attempt: " + loginData.getUsername());
		Message loginMessage = new Message(MessageType.LOGIN_REQUEST_SUB, loginData);
        if (ConnectToServer_GUI.clientController != null) {
            try {
                sendComplexObject(loginMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //ConnectToServer_GUI.clientController = null;
        }
	}
	
	public void sendGuestLoginRequest(LoginData loginData) {
		System.out.println("Guest Login Attempt: " + loginData.getEmail() != null ? loginData.getEmail() : loginData.getPhoneNumber());
		Message loginMessage = new Message(MessageType.LOGIN_REQUEST_GUEST, loginData);
        if (ConnectToServer_GUI.clientController != null) {
            try {
                sendComplexObject(loginMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	public void sendGetAllUsersRequest() {
		System.out.println("Get all users Attempt");
		Message message = new Message(MessageType.GET_USER_DETAILS);
        if (ConnectToServer_GUI.clientController != null) {
            try {
                sendComplexObject(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
	

	/**
	 * Handles messages received from the server. Deserializes the byte array using Kryo 
	 * and routes the response to the active GUI instance.
	 * All UI updates are wrapped in Platform.runLater using Anonymous Inner Classes 
	 * to ensure thread safety on the JavaFX Application Thread.
	 * * @param message The serialized message (byte array) received from the server.
	 */
	@Override
	public void display(Object message) {
		if (message instanceof byte[]) {
			// Deserialization using the utility class
			Object receivedMessageDeserialized = KryoUtil.deserialize((byte[]) message);
			
			if (!(receivedMessageDeserialized instanceof Message)) {
				return;
			}
			
			final Message recivedMessage = (Message) receivedMessageDeserialized;

			try {
				switch (recivedMessage.getType()) {
				
				case LOGIN_SUCCESS_GUEST:
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (MainScreen_GUI.instance != null) {
								MainScreen_GUI.instance.onGuestLoginSuccess();
							}
						}});
					break;
				
				case LOGIN_SUCCESS_SUB:
					
					Subscribed_Customer sub = (Subscribed_Customer)recivedMessage.getContent();
					User_Session.setLoggedInUser(sub);
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (MainScreen_GUI.instance != null) {
								MainScreen_GUI.instance.onSubLoginSuccess(sub);
							}
						}});
					break;
					

				case LOGIN_FAILED_GUEST:
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (MainScreen_GUI.instance != null) {
								MainScreen_GUI.instance.onGuestLoginFailure(recivedMessage);
							}
						}});
					
					break;
					
					
				case LOGIN_FAILED_SUB:
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (MainScreen_GUI.instance != null) {
								MainScreen_GUI.instance.onSubLoginFailure();
							}
						}});
					
					break;

				case RETURN_RESERVATIONS_BY_USER:
					@SuppressWarnings("unchecked")
					final List<Reservation> resList = (List<Reservation>) recivedMessage.getContent();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (ViewReservations_GUI.instance != null) {
								ViewReservations_GUI.instance.updateTable(resList);
							}
						}
					});
					break;

				case RETURN_ALL_PENDING_RESERVATIONS:
					@SuppressWarnings("unchecked")
					final List<Reservation> adminList = (List<Reservation>) recivedMessage.getContent();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (ManageOrders_GUI.instance != null) {
								ManageOrders_GUI.instance.updateAdminUI(adminList);
							}
						}
					});
					break;

				case RESERVATION_CONFIRMED:
					final int confirmationCode = (Integer) recivedMessage.getContent();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (AddReservation_GUI.instance != null) {
								AddReservation_GUI.instance.showSuccessAlert(confirmationCode);
							} else if (AddManualReservation_GUI.instance != null) {
								AddManualReservation_GUI.instance.showSuccessAlert(confirmationCode);
							}
						}
					});
					break;

				case RESERVATION_FAILED_NO_TABLE:
					/**
					 * The server found no availability for the requested time but provided a suggestion.
					 * Content: LocalDateTime (the suggested alternative slot).
					 */
					final LocalDateTime suggestedTime = (LocalDateTime) recivedMessage.getContent();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// Route to Add Reservation Screen
							if (AddReservation_GUI.instance != null) {
								AddReservation_GUI.instance.showNoTableAlert(suggestedTime);
							} 
							// Route to Staff Manual Entry Screen
							else if (AddManualReservation_GUI.instance != null) {
								AddManualReservation_GUI.instance.showNoTableAlert(suggestedTime);
							}
							// Route to View/Edit Screen (Crucial for existing order updates)
							else if (ViewReservations_GUI.instance != null) {
								ViewReservations_GUI.instance.showNoTableAlert(suggestedTime);
							}
						}
					});
					break;

				case RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED:
					/**
					 * The server found no availability and no suggestions for the rest of the day.
					 */
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (AddReservation_GUI.instance != null) {
								AddReservation_GUI.instance.showNoTableAlert(null);
							} else if (ViewReservations_GUI.instance != null) {
								ViewReservations_GUI.instance.showNoTableAlert(null);
							} else if (AddManualReservation_GUI.instance != null) {
								AddManualReservation_GUI.instance.showNoTableAlert(null);
							}
						}
					});
					break;

				case RESERVATION_FAILED:
					/**
					 * General failure (e.g., Database error).
					 */
					final String errorMsg = (String) recivedMessage.getContent();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							// Display generic error to whichever screen initiated the request
							System.err.println("Server Error: " + errorMsg);
						}
					});
					break;

				case RESERVATION_UPDATE_SUCCESS:
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (ViewReservations_GUI.instance != null) {
								ViewReservations_GUI.instance.showSuccessAlert();
								sendGetReservationsRequest(
										User_Session.getLoggedInUser() != null ? User_Session.getLoggedInUser()
												: User_Session.getCasualPhone());
							}
						}
					});
					break;

				case ADMIN_UPDATE_SUCCESS:
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (ManageOrders_GUI.instance != null) {
								ManageOrders_GUI.instance.refreshAdminData();
							}
						}
					});
					break;

				case RESERVATION_CANCELED:
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (ViewReservations_GUI.instance != null) {
								sendGetReservationsRequest(
										User_Session.getLoggedInUser() != null ? User_Session.getLoggedInUser()
												: User_Session.getCasualPhone());
							} else if (ManageOrders_GUI.instance != null) {
								ManageOrders_GUI.instance.refreshAdminData();
							}
						}
					});
					break;

				case RETURN_OPENING_HOURS:
					final Opening_Hours oh = (Opening_Hours) recivedMessage.getContent();
					Restaurant.getInstance().setOpeningHours(oh);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (AddReservation_GUI.instance != null) {
								LocalDate currentDate = AddReservation_GUI.instance.getDatePicker().getValue();
								if (currentDate != null) {
									AddReservation_GUI.instance.loadDynamicHours(currentDate);
								}
							}
						}
					});
					break;

				default:
					System.out.println("Client_Controller: Received unhandled message type: " + recivedMessage.getType());
					break;
				}
			} catch (Exception e) {
				System.err.println("Client_Controller: Critical error while processing server response.");
				e.printStackTrace();
			}
		}
	}

}
