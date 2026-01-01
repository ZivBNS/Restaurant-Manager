package controllers;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * The main controller for the client-side application. It manages the network
 * connection to the server and routes incoming responses to the appropriate GUI
 * screens using a Handler Map pattern.
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
	@SuppressWarnings("unchecked")
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
				UserRecord userRecord = (UserRecord) msg.getContent();
				User_Session.setLoggedInUser(userRecord);
				/*
				 * Subscribed_Customer sub = new Subscribed_Customer( userRecord.getFirstName(),
				 * userRecord.getLastName(), userRecord.getPhone(), userRecord.getEmail(),
				 * userRecord.getUsername(), userRecord.getPassword() );
				 */
				if (Terminal_GUI.instance != null) {
					// sub.setUserId(userRecord.getId());
					Terminal_GUI.instance.handleMessageIfLoggedIn(userRecord);
				} else if (MainScreen_GUI.instance != null) {
					MainScreen_GUI.instance.onSubLoginSuccess(userRecord);
				}
			}
		});

		responseHandlers.put(MessageType.LOGIN_FAILED_GUEST, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.handleMessageIfLoggedIn(null);
				else if (MainScreen_GUI.instance != null) {
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
				List<Reservation> resList = (List<Reservation>) msg.getContent();
				if (ViewReservations_GUI.instance != null) {
					ViewReservations_GUI.instance.updateTable(resList);
				}
			}
		});

		responseHandlers.put(MessageType.RETURN_ALL_PENDING_RESERVATIONS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				List<Reservation> adminList = (List<Reservation>) msg.getContent();
				if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.updateAdminUI(adminList);
				}
			}
		});

		// -----------------------------------------------------------
		// Check in and out Actions
		// -----------------------------------------------------------

		// check in

		responseHandlers.put(MessageType.CHECK_IN_COMPLETED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null) {
					int tableNumber = (int) msg.getContent();
					Terminal_GUI.instance.onCheckInSuccessResponse(tableNumber);
				}
			}
		});

		responseHandlers.put(MessageType.CHECK_IN_FAIL, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null) {
					String s = (String) msg.getContent();
					Terminal_GUI.instance.onCheckInFailedResponse(s);
				}
			}
		});

		// -----------------------------------------------------------
		// Reservation Actions
		// -----------------------------------------------------------

		// instant reservation from terminal

		responseHandlers.put(MessageType.INSTANT_RESERVATION_FAILED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null) {
					String s = (String) msg.getContent();
					Terminal_GUI.instance.onInstantReservationFailedResponse(s);
				}
			}
		});

		responseHandlers.put(MessageType.INSTANT_RESERVATION_SUCCESS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null) {
					int code = (int) msg.getContent();
					Terminal_GUI.instance.onInstantReservationSuccessResponse(code);
					sendComplexObject(new Message(MessageType.CHECK_IN_REQUEST, code));
				}
			}
		});

		responseHandlers.put(MessageType.RESERVATION_CONFIRMED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				int confirmationCode = (Integer) msg.getContent();
				if (AddReservation_GUI.instance != null) {
					AddReservation_GUI.instance.showSuccessAlert(confirmationCode);
				} else if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.showSuccessAlert(confirmationCode);
				}
			}
		});

		responseHandlers.put(MessageType.RESERVATION_FAILED_NO_TABLE, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				LocalDateTime suggestedTime = (LocalDateTime) msg.getContent();
				if (AddReservation_GUI.instance != null) {
					AddReservation_GUI.instance.showNoTableAlert(suggestedTime);
				} else if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.showNoTableAlert(suggestedTime);
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
				} else if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.showNoTableAlert(null);
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

		responseHandlers.put(MessageType.RETURN_LATEST_RESERVATION_BY_PHONE, msg -> {

			Reservation r = (Reservation) msg.getContent();

			if (r != null) {
				sendComplexObject(new Message(MessageType.GET_BILL_BY_RESERVATION_ID, r.getId()));
			} else if (BillPayment_GUI.instance != null) {
				Platform.runLater(() -> BillPayment_GUI.instance.showNoReservationFound());
			}
		});

		responseHandlers.put(MessageType.RETURN_BILL_BY_RESERVATION_ID, new ResponseHandler() {
			@Override
			public void handle(Message msg) {

				System.out.println("CLIENT: received bill = " + msg.getContent());
				Bill bill = (Bill) msg.getContent();

				if (BillPayment_GUI.instance != null) {
					Platform.runLater(() -> BillPayment_GUI.instance.displayBill(bill));
				} else if (Terminal_GUI.instance != null) {
					Terminal_GUI.instance.onGetBillSuccess(bill);
				}
			}
		});

		responseHandlers.put(MessageType.BILL_REQUEST_FAILED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				String s = (String) msg.getContent();
				if (Terminal_GUI.instance != null) {
					Terminal_GUI.instance.onGetBillFailure(s);
				}
			}
		});

		responseHandlers.put(MessageType.BILL_PAYMENT_SUCCESS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (BillPayment_GUI.instance != null) {
					Platform.runLater(() -> BillPayment_GUI.instance.onPaymentSuccess());
				} else if (Terminal_GUI.instance != null) {
					Terminal_GUI.instance.onPaymentSuccessResponse(true);
				}
			}
		});

		responseHandlers.put(MessageType.RETURN_ALL_BILLS, msg -> {
			if (BillManager_GUI.instance != null) {
				BillManager_GUI.instance.updateBillsTable((List<Bill>) msg.getContent());
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
				} else if (Terminal_GUI.instance != null) {
					Terminal_GUI.instance.onCancellationResponse("approved r");
				}
			}
		});

		responseHandlers.put(MessageType.RESERVATION_CANCEL_FAILED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (ViewReservations_GUI.instance != null) {
					refreshUserReservations();
				} else if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.refreshAdminData();
				} else if (Terminal_GUI.instance != null) {

					Terminal_GUI.instance.onCancellationResponse((String) msg.getContent());
				}
			}
		});
		// -----------------------------------------------------------
		// Report Handling
		// -----------------------------------------------------------

		responseHandlers.put(MessageType.REPORT_DATA_SUCCESS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Reports_GUI.instance != null) {
					// Cast content to MonthlyFullReportData and update GUI
					Reports.MonthlyFullReportData data = (Reports.MonthlyFullReportData) msg.getContent();
					Reports_GUI.instance.updateReportView(data);
				}
			}
		});

		responseHandlers.put(MessageType.REPORT_ERROR, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				String error = (String) msg.getContent();
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Report Error");
					alert.setHeaderText("Could not fetch report");
					alert.setContentText(error);
					alert.showAndWait();
				});
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

				// Refresh management screen if open
				if (ManageHours_GUI.instance != null) {
					ManageHours_GUI.instance.refreshUI(oh);
				}

				// Refresh reservation screen if open
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
		responseHandlers.put(MessageType.RETURN_USER_DETAILS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (ManageOrders_GUI.instance != null) {
					UserRecord user = (UserRecord) msg.getContent();
					ManageOrders_GUI.instance.fillUserDetails(user);
				}
			}
		});
		responseHandlers.put(MessageType.USER_NOT_FOUND, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				// Check if the GUI is currently active
				if (ManageOrders_GUI.instance != null) {
					// Passing null triggers the "User Not Found" alert in the GUI
					ManageOrders_GUI.instance.fillUserDetails(null);
				}
			}
		});
		// -----------------------------------------------------------
		// Update User Profile
		// -----------------------------------------------------------

		responseHandlers.put(MessageType.UPDATE_USER_DETAILS_RESPONSE_OK, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				User_Session.setLoggedInUser((UserRecord) msg.getContent());
				if (UpdateProfile_GUI.instance != null) {
					UpdateProfile_GUI.instance.onRefresh();
				}
			}
		});

		responseHandlers.put(MessageType.UPDATE_USER_DETAILS_RESPONSE_ERR, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				User_Session.setLoggedInUser((UserRecord) msg.getContent());
				if (UpdateProfile_GUI.instance != null) {
					UpdateProfile_GUI.instance.onError();
				}
			}
		});
		// -----------------------------------------------------------
		// Table Management
		// -----------------------------------------------------------

		responseHandlers.put(MessageType.RETURN_ALL_TABLES, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
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
					sendComplexObject(new Message(MessageType.GET_ALL_TABLES, null));
				}
			}
		});

		responseHandlers.put(MessageType.WAITLIST_CANCELED_FAILED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onCancellationResponse((String) msg.getContent());
			}
		});

		responseHandlers.put(MessageType.WAITLIST_CANCELED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onCancellationResponse("approved w");
			}
		});
		responseHandlers.put(MessageType.WAITLIST_JOINED_FAILED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onJoinWaitlistFailedResponse((String) msg.getContent());
			}
		});

		responseHandlers.put(MessageType.WAITLIST_JOINED_SUCCESS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onJoinWaitlistSucceedResponse((int) msg.getContent());
			}
		});
	}

	/**
	 * Helper method to refresh reservations for the currently logged-in user.
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

	@Override
	public void display(Object message) {
		if (message instanceof byte[]) {
			Object obj = KryoUtil.deserialize((byte[]) message);

			if (obj instanceof Message) {
				final Message receivedMsg = (Message) obj;
				final MessageType type = receivedMsg.getType();

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
					System.out.println("Client_Controller: Received unhandled message type: " + type + " Content: "
							+ receivedMsg.getContent());
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	// Request Sending Methods (Opening Hours)
	// ----------------------------------------------------------------------

	/**
	 * Sends a request to update the closing time of a regular operating day.
	 * 
	 * @param day          The day of the week.
	 * @param openTime     The original opening time (Primary Key).
	 * @param newCloseTime The new closing time.
	 */
	public void sendUpdateRegularHoursRequest(DayOfWeek day, LocalTime openTime, LocalTime newCloseTime) {
		Map<String, Object> data = new HashMap<>();
		data.put("day", day);
		data.put("openTime", openTime);
		data.put("newCloseTime", newCloseTime);
		sendComplexObject(new Message(MessageType.UPDATE_REGULAR_HOURS, data));
	}

	/**
	 * Sends a request to add a special hour exception for a specific date.
	 * 
	 * @param date  The date for the exception.
	 * @param open  The opening time (null if closed).
	 * @param close The closing time.
	 * @param desc  Description/Reason for the exception.
	 */
	public void sendAddSpecialHourRequest(LocalDate date, LocalTime open, LocalTime close, String desc) {
		Map<String, Object> data = new HashMap<>();
		data.put("date", date);
		data.put("openTime", open);
		data.put("closeTime", close);
		data.put("description", desc);
		sendComplexObject(new Message(MessageType.ADD_SPECIAL_HOUR, data));
	}

	/**
	 * Sends a request to logically delete (deactivate) a regular hours slot.
	 * 
	 * @param day      The day of the week.
	 * @param openTime The opening time slot.
	 */
	public void sendUpdateRegularHoursRequest(DayOfWeek day, LocalTime openTime, LocalTime newCloseTime,
			boolean isActive) {
		Map<String, Object> data = new HashMap<>();
		data.put("day", day);
		data.put("openTime", openTime);
		data.put("newCloseTime", newCloseTime);
		data.put("isActive", isActive); // Add the boolean flag

		sendComplexObject(new Message(MessageType.UPDATE_REGULAR_HOURS, data));
	}

	// ----------------------------------------------------------------------
	// Existing Request Methods
	// ----------------------------------------------------------------------

	public void setManageUsersGUI(ManageUsers_GUI manageUsers_GUI) {
		this.manageUsers_GUI = manageUsers_GUI;
	}

	public void sendGetReservationsRequest(Object identifier) {
		try {
			Message message;
			if (identifier instanceof Subscribed_Customer) {
				message = new Message(MessageType.GET_RESERVATIONS_BY_USER, identifier);
			} else {
				message = new Message(MessageType.GET_RESERVATIONS_BY_USER, (String) identifier);
			}
			sendComplexObject(message);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Sends a batch update request for the weekly schedule.
	 * 
	 * @param batchData Map of Day to [Open, Close, IsActive]
	 */
	public void sendBatchUpdateHours(Map<DayOfWeek, Object[]> batchData) {
		sendComplexObject(new Message(MessageType.UPDATE_REGULAR_HOURS, batchData));
	}

	public void sendUpdateReservationRequest(Reservation reservationToUpdate) {
		sendComplexObject(new Message(MessageType.UPDATE_RESERVATION_REQUEST, reservationToUpdate));
	}

	public void sendNewReservationRequest(Reservation newRes) {
		sendComplexObject(new Message(MessageType.CREATE_RESERVATION, newRes));
	}

	public void sendNewInstantReservationRequest(Reservation newRes) {
		sendComplexObject(new Message(MessageType.CREATE_INSTANT_RESERVATION, newRes));
	}

	public void sendGetOpeningHoursRequest() {
		sendComplexObject(new Message(MessageType.GET_OPENING_HOURS, null));
	}

	public void sendDeleteSpecialHourRequest(LocalDate date) {
		sendComplexObject(new Message(MessageType.DELETE_SPECIAL_HOUR, date));
	}

	public void sendGetAllPendingReservationsRequest() {
		sendComplexObject(new Message(MessageType.GET_ALL_PENDING_RESERVATIONS, null));
	}

	public void sendComplexObject(Object obj) {
		try {
			byte[] payload = KryoUtil.serialize(obj);
			client.handleMessageFromClientUI(payload);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void logout() {
		sendComplexObject(new Message(MessageType.LOGOUT_REQUEST, null));
		client.quit();
	}

	public void sendSubscriberLoginRequest(LoginData loginData) {
		sendComplexObject(new Message(MessageType.LOGIN_REQUEST_SUB, loginData));
	}

	public void sendGuestLoginRequest(LoginData loginData) {
		sendComplexObject(new Message(MessageType.LOGIN_REQUEST_GUEST, loginData));
	}

	public void sendGetAllUsersRequest() {
		sendComplexObject(new Message(MessageType.GET_ALL_USERS_REQUEST));
	}

	public void sendAddUserRequest(UserRecord newUser) {
		sendComplexObject(new Message(MessageType.ADD_USER_REQUEST, newUser));
	}

	public void sendEditUserRequest(UserRecord user) {
		sendComplexObject(new Message(MessageType.EDIT_USER_REQUEST, user));
	}

	public void sendRemoveUserRequest(UserRecord user) {
		sendComplexObject(new Message(MessageType.DELETE_USER_REQUEST, user));
	}

	public void sendCancelReservationOrWaitlistRequestFromTerminal(int code) {
		sendComplexObject(new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_BY_CODE, code));
	}

	public void sendCheckInRequest(int confiCode) {
		sendComplexObject(new Message(MessageType.CHECK_IN_REQUEST, confiCode));
	}

	public void sendJoinWaitlistRequest(Reservation waitlistReq) {
		sendComplexObject(new Message(MessageType.JOIN_WAITLIST, waitlistReq));
	}

	public void sendGetBillRequest(int code) {
		sendComplexObject(new Message(MessageType.BILL_REQUEST, code));
	}

	public void sendPayBillRequest(Bill currentBillToPay) {
		sendComplexObject(new Message(MessageType.BILL_PAYMENT_REQUEST, currentBillToPay.getId()));

	}
	/**
	 * Sends a request to fetch the report for a specific month and year.
	 * @param month The month (1-12).
	 * @param year The year (e.g. 2026).
	 */
	public void sendGetMonthlyReportRequest(int month, int year) {
	    int[] payload = new int[]{month, year};
	    sendComplexObject(new Message(MessageType.GET_MONTHLY_REPORT, payload));
	}
	/**
	 * Sends a request to fetch user details by ID for auto-filling forms.
	 * 
	 * @param userId The ID of the subscriber to look up.
	 */
	public void sendGetUserDetailsRequest(int userId) {
		sendComplexObject(new Message(MessageType.GET_USER_DETAILS, userId));
	}
}