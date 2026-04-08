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
import utils.User_Session;
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

	   
	/**
	 * Constructor to create a Client_Controller with specified host and port.
	 * 
	 * @param host The server host address.
	 * @param port The server port number.
	 * @throws IOException If there is an error setting up the connection.
	 */
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
					MainScreen_GUI.instance.onGuestLoginSuccess((UserRecord) msg.getContent());
				}
			}
		});

		responseHandlers.put(MessageType.LOGIN_SUCCESS_SUB, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				UserRecord userRecord = (UserRecord) msg.getContent();
				User_Session.setLoggedInUser(userRecord);

				if (Terminal_GUI.instance != null) {
					// sub.setUserId(userRecord.getId());
					Terminal_GUI.instance.handleMessageIfLoggedIn(userRecord);
					if (userRecord!=null) {
						int userId=userRecord.getId();
						sendComplexObject(new Message(MessageType.GET_RESERVATIONS_BY_USER,userId));
					}
				}
				else if (MainScreen_GUI.instance != null) {
					MainScreen_GUI.instance.onSubLoginSuccess(userRecord);
				}
			}
		});
		
		responseHandlers.put(MessageType.LOGIN_SUCCESS_EMP, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				UserRecord userRecord = (UserRecord) msg.getContent();
				User_Session.setLoggedInUser(userRecord);
				if (Terminal_GUI.instance != null) {
					Terminal_GUI.instance.handleMessageIfLoggedIn(userRecord);
				} else if (MainScreen_GUI.instance != null) {
					MainScreen_GUI.instance.onEmployeeLoginSuccess(userRecord);
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
				if (Terminal_GUI.instance!=null) Terminal_GUI.instance.handleMessageIfLoggedIn(null);
			}
		});
		
		responseHandlers.put(MessageType.LOGIN_FAILED_EMP, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (MainScreen_GUI.instance != null) {
					MainScreen_GUI.instance.onEmployeeLoginFailure();
				}
				if (Terminal_GUI.instance!=null) Terminal_GUI.instance.handleMessageIfLoggedIn(null);

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
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onDailyReservationsReceived(resList);				
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
		responseHandlers.put(MessageType.GET_ALL_PENDING_AND_ACTIVE_RESERVATIONS, new ResponseHandler() {
		    @Override
		    public void handle(Message msg) {
		        List<Reservation> list = (List<Reservation>) msg.getContent();
		        
		        if (ManageOrders_GUI.instance != null) {
		            ManageOrders_GUI.instance.updateAdminUI(list);
		        }
		    }
		});
		responseHandlers.put(MessageType.RETURN_VISIT_HISTORY, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                Object content = msg.getContent();
                
                if (content instanceof List) {
                    List<Reservation> visits = (List<Reservation>) content;
                    if (OrderHistory_GUI.instance != null) {
                        OrderHistory_GUI.instance.updateVisitTable(visits);
                    }
                } else {
                    System.err.println("[Client Error] Expected List<Reservation> but received: " + content.getClass().getSimpleName());
                    System.err.println("[Server Message]: " + content);
                }
            }
        });
        
        // Ensure the existing handler updates the ORDER table
        responseHandlers.put(MessageType.RETURN_RESERVATION_HISTORY, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                List<Reservation> history = (List<Reservation>) msg.getContent();
                if (OrderHistory_GUI.instance != null) {
                    OrderHistory_GUI.instance.updateOrderTable(history); // Updated method name
                }
            }
        });

        responseHandlers.put(MessageType.RESERVATION_DATA_UPDATED_BROADCAST, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        // 1. Admin: Manage Orders Screen
                        if (ManageOrders_GUI.instance != null) {
                            ManageOrders_GUI.instance.refreshAdminData();
                        }

                        // 2. Client: Add Reservation Screen (Refresh time slots availability)
                        if (AddReservation_GUI.instance != null) {
                            AddReservation_GUI.instance.refreshHours();
                        }

                        // 3. Client: View/Edit Reservations Screen
                        if (ViewReservations_GUI.instance != null) {
                            ViewReservations_GUI.instance.refreshTableData();
                        }

                        // 4. Terminal: Refresh Main View
                        if (Terminal_GUI.instance != null) {
                            // Ensure a user is logged in before requesting specific user data
                            if (User_Session.getLoggedInUser() != null) {
                                int userId = User_Session.getLoggedInUser().getId();
                                sendGetDailyReservationsRequest(userId);
                            }
                        } 
                    }
                });
            }
        });
		// -----------------------------------------------------------
		// Check in and out Actions
		// -----------------------------------------------------------


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

		

		// -----------------------------------------------------------
		// Billing & Final Payment Handlers (Standardized Format)
		// -----------------------------------------------------------

		responseHandlers.put(MessageType.RETURN_BILL_BY_RESERVATION_ID, new ResponseHandler() {
		    @Override
		    public void handle(Message msg) {
		        Bill b = (Bill) msg.getContent();
		            // Display the finalized bill in the GUI
		            if (BillPayment_GUI.instance != null) {
		                Platform.runLater(new Runnable() {
		                    @Override
		                    public void run() {
		                        BillPayment_GUI.instance.displayBill(b);
		                    }
		                });
		            }
		            if (Terminal_GUI.instance != null) {
		                Terminal_GUI.instance.onGetBillSuccess(b);
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
				
				// Priority 1: If Admin screen is open, show alert there
				if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.showSuccessAlert(confirmationCode);
				}
				// Priority 2: If Admin is NOT open, check Client screen
				else if (AddReservation_GUI.instance != null) {
					AddReservation_GUI.instance.showSuccessAlert(confirmationCode);
				}
			}
		});

		responseHandlers.put(MessageType.RESERVATION_FAILED_NO_TABLE, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				LocalDateTime suggestedTime = (LocalDateTime) msg.getContent();
				if (AddReservation_GUI.instance != null) {
					AddReservation_GUI.instance.showNoTableAlert(suggestedTime);
				}
				if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.showNoTableAlert(suggestedTime);
				}
				if (ViewReservations_GUI.instance != null) {
					ViewReservations_GUI.instance.showNoTableAlert(suggestedTime);
				}
			}
		});

		responseHandlers.put(MessageType.RESERVATION_FAILED_NO_TABLE_FULLY_BOOKED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (AddReservation_GUI.instance != null) {
					AddReservation_GUI.instance.showNoTableAlert(null);
				}
				if (ViewReservations_GUI.instance != null) {
					ViewReservations_GUI.instance.showNoTableAlert(null);
				}
				if (ManageOrders_GUI.instance != null) {
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

		responseHandlers.put(MessageType.RETURN_LATEST_RESERVATION_BY_PHONE, new ResponseHandler() {
		    @Override
		    public void handle(Message msg) {
		        Reservation r = (Reservation) msg.getContent();

		        if (r != null && r.getStatus().equals(ReservationStatus.ACTIVE.toString())) {
		            sendComplexObject(new Message(MessageType.GET_BILL_BY_RESERVATION_ID, r.getId()));
		        } else if (BillPayment_GUI.instance != null) {
		            Platform.runLater(new Runnable() {
		                @Override
		                public void run() {
		                    BillPayment_GUI.instance.showNoReservationFound();
		                }
		            });
		        }
		    }
		});
		
		responseHandlers.put(MessageType.RETURN_LATEST_RESERVATION_BY_EMAIL, new ResponseHandler() {
		    @Override
		    public void handle(Message msg) {
		        Reservation r = (Reservation) msg.getContent();
		        if (r != null && r.getStatus().equals(ReservationStatus.ACTIVE.toString())) {
		            sendComplexObject(new Message(MessageType.GET_BILL_BY_RESERVATION_ID, r.getId()));
		        } else if (BillPayment_GUI.instance != null) {
		            Platform.runLater(new Runnable() {
		                @Override
		                public void run() {
		                    BillPayment_GUI.instance.showNoReservationFound();
		                }
		            });
		        }
		    }
		});
		responseHandlers.put(MessageType.RESERVATION_UPDATE_SUCCESS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (ViewReservations_GUI.instance != null) {
					ViewReservations_GUI.instance.showSuccessAlert();
					refreshUserReservations();
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
		responseHandlers.put(MessageType.RESERVATION_CANCEL_FAILED, new ResponseHandler() {
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
		// Updates, Cancellations & Admin Actions
		// -----------------------------------------------------------


		responseHandlers.put(MessageType.ADMIN_UPDATE_SUCCESS, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (ManageOrders_GUI.instance != null) {
					ManageOrders_GUI.instance.refreshAdminData();
				}
			}
		});

		
		// Inside initializeHandlers in Client_Controller.java

        responseHandlers.put(MessageType.ADMIN_UPDATE_SUCCESS, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (ManageOrders_GUI.instance != null) {
                    // Refresh data first
                    ManageOrders_GUI.instance.refreshAdminData();
                    // Show success alert
                    ManageOrders_GUI.instance.showUpdateSuccessAlert();
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

		// Inside initializeHandlers() method in Client_Controller.java

		responseHandlers.put(MessageType.RETURN_OPENING_HOURS, new ResponseHandler() {
		    @Override
		    public void handle(Message msg) {
		        Opening_Hours oh = (Opening_Hours) msg.getContent();
		        // 1. Update global data so any screen accessing it gets the latest information
		        Restaurant.getInstance().setOpeningHours(oh);

		        Platform.runLater(new Runnable() {
		            @Override
		            public void run() {
		                // --- Manage Hours Screen (Main screen for this action) ---
		                if (ManageHours_GUI.instance != null) {
		                    ManageHours_GUI.instance.refreshUI(oh);

		                    // Show "Success" alert only to the user who performed the action
		                    if (ManageHours_GUI.instance.isUpdatePending()) {
		                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
		                        alert.setTitle("Success");
		                        alert.setHeaderText(null);
		                        alert.setContentText("Operation completed successfully!");
		                        alert.showAndWait();
		                        ManageHours_GUI.instance.setUpdatePending(false);
		                    }
		                }

		                // --- New Reservation Screen ---
		                if (AddReservation_GUI.instance != null) {
		                    // Calls the function to refresh time slots based on the selected date
		                    AddReservation_GUI.instance.refreshHours();
		                }

		                // --- View Reservations Screen (Client) ---
		                if (ViewReservations_GUI.instance != null) {
		                    // If the client is editing, the opening hours list will update
		                    ViewReservations_GUI.instance.refreshHours();
		                }

		                // --- Manage Orders Screen (Admin) ---
		                if (ManageOrders_GUI.instance != null) {
		                    // If the admin is editing/approving, this will refresh the hours
		                    ManageOrders_GUI.instance.refreshHours();
		                }
		            }
		        });
		    }
		});
		responseHandlers.put(MessageType.OPENING_HOURS_ONLY_ONE_A_DAY_ERROR, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                if (ManageHours_GUI.instance != null) {
                    
                    // 1. Reset the pending flag since the update failed
                    ManageHours_GUI.instance.setUpdatePending(false);

                    // 2. Show Error Alert on JavaFX Thread
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Operation Failed");
                            alert.setHeaderText("Duplicate Date");
                            alert.setContentText("You cannot define two special schedules for the same date.\nPlease delete the existing entry first or edit it.");
                            alert.showAndWait();
                        }
                    });
                }
            }
        });
		/**
         * Handles the specific error when an opening hours update fails due to
         * existing reservations conflicting with the new schedule.
         */
		responseHandlers.put(MessageType.OPENING_HOURS_UPDATE_CONFLICT_ERROR, new ResponseHandler() {
	        @Override
	        public void handle(Message msg) {
	            // The server now sends the LocalDate object directly
	            LocalDate conflictDate = (LocalDate) msg.getContent();

	            if (ManageHours_GUI.instance != null) {
	                ManageHours_GUI.instance.setUpdatePending(false);
	                
	                Platform.runLater(new Runnable() {
	                    @Override
	                    public void run() {
	                        ManageHours_GUI.instance.showConflictDialog(conflictDate);
	                    }
	                });
	            }
	        }
	    });
        /**
         * Handles the specific error when an opening hours update fails due to
         * existing reservations conflicting with the new schedule.
         */
        responseHandlers.put(MessageType.SPECIAL_HOURS_UPDATE_CONFLICT_ERROR, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                String conflictDetails = (String) msg.getContent();

                // Check if the management screen is currently active
                if (ManageHours_GUI.instance != null) {
                    
                    // 1. Critical: Reset the 'pending' flag so the UI logic doesn't get stuck
                    ManageHours_GUI.instance.setUpdatePending(false);

                    // 2. Display the conflict details in an Error Alert
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Update Rejected");
                            alert.setHeaderText("Schedule Conflict Detected - Cannot add special hours");
                            alert.setContentText(conflictDetails); // Displays the text sent from Server
                            alert.showAndWait();
                            sendGetOpeningHoursRequest();
                            }
                    });
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

		
		responseHandlers.put(MessageType.ADD_USER_RESPONSE_OK, userHandler);
		responseHandlers.put(MessageType.ADD_USER_RESPONSE_ERR, userHandler);
		responseHandlers.put(MessageType.EDIT_USER_RESPONSE_OK, userHandler);
		responseHandlers.put(MessageType.EDIT_USER_RESPONSE_ERR, userHandler);
		responseHandlers.put(MessageType.DELETE_USER_RESPONSE_OK, userHandler);
		responseHandlers.put(MessageType.DELETE_USER_RESPONSE_ERR, userHandler);
		// -----------------------------------------------------------
        // User List Broadcast Handler
        // -----------------------------------------------------------
		responseHandlers.put(MessageType.GET_ALL_USERS_RESPONSE, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                List<UserRecord> allUsers = (List<UserRecord>) msg.getContent();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        // 1. Refresh Admin Screen
                        if (manageUsers_GUI != null) {
                            manageUsers_GUI.handle(msg);
                        }

                        // 2. Refresh Personal Profile Screen 
                        if (UpdateProfile_GUI.instance != null && User_Session.getLoggedInUser() != null) {
                            int myId = User_Session.getLoggedInUser().getId();
                            
                            for (UserRecord u : allUsers) {
                                if (u.getId() == myId) {
                                    User_Session.setLoggedInUser(u);
                                    
                                    UpdateProfile_GUI.instance.onBroadcastRefresh(); 
                                    break;
                                }
                            }
                        }
                    }
                });
            }
        });
		
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
                // Update the Session with the fresh data from the server
                User_Session.setLoggedInUser((UserRecord) msg.getContent());

                // Ensure we call onRefresh to display the success message
                if (UpdateProfile_GUI.instance != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            UpdateProfile_GUI.instance.onRefresh();
                        }
                    });
                }
            }
        });

		responseHandlers.put(MessageType.UPDATE_USER_DETAILS_RESPONSE_ERR, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (UpdateProfile_GUI.instance != null) {
					UpdateProfile_GUI.instance.onError(msg.getContent().toString());
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
                
                // Update the local Singleton cache with fresh data
                Restaurant.getInstance().setTables(tables);

                // 1. Update Manage Tables Screen if currently open
				if (ManageTables_GUI.instance != null) {
					ManageTables_GUI.instance.loadTables(tables);
				}
				// Update Tables view if open
				if (gui.TablesView_GUI.instance != null) {
					gui.TablesView_GUI.instance.loadTables(tables);
				}
                
                // --- Calculate Max Capacity Logic ---
                // We iterate through active tables to find the largest table size.
                // This is used to set limits on guest input fields in other screens.
                int maxCap = 0;
                for (Restaurant_Table t : tables) {
                    if (t.isActive() && t.getSize() > maxCap) {
                        maxCap = t.getSize();
                    }
                }
                
                // Fallback: If no tables exist or max is 0, default to 10
                if (maxCap == 0) {
                    maxCap = 10;
                }
                
                final int calculatedMax = maxCap;

                // 2. Update Add Reservation Screen (Using Anonymous Inner Class)
                if (AddReservation_GUI.instance != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            AddReservation_GUI.instance.updateSpinnerLimit(calculatedMax);
                        }
                    });
                }

                // 3. Update View Reservations Screen (Using Anonymous Inner Class)
                if (ViewReservations_GUI.instance != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            ViewReservations_GUI.instance.updateMaxCapacity(calculatedMax);
                        }
                    });
                }
                // 4. Update Manage Orders Screen (Admin)
                if (ManageOrders_GUI.instance != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            ManageOrders_GUI.instance.updateMaxCapacity(calculatedMax);
                        }
                    });
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
                // 1. Extract the success message sent from the server
                String successMsg = (String) msg.getContent();

                if (ManageTables_GUI.instance != null) {
                    
                    // 2. Show Success Alert on the JavaFX Application Thread
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Success");
                            alert.setHeaderText(null);
                            alert.setContentText(successMsg);
                            alert.showAndWait();
                        }
                    });

                    // 3. Refresh the table list to show changes
                    sendComplexObject(new Message(MessageType.GET_ALL_TABLES, null));
                }
            }
        });
		responseHandlers.put(MessageType.TABLE_DATA_UPDATED_BROADCAST, new ResponseHandler() {
            @Override
            public void handle(Message msg) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        // Only refresh if the Manage Tables screen is currently open
                        if (ManageTables_GUI.instance != null) {
                            sendGetAllTablesRequest(); 
                        }
                    }
                });
            }
        });
		// -----------------------------------------------------------
		// Waitlist
		// -----------------------------------------------------------
		
		responseHandlers.put(MessageType.WAITLIST_AND_RESERVATION_CANCELED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onCancellationResponse((String) msg.getContent(), true);
				
				if (ManageWaitlist_GUI.instance != null) {
		            ManageWaitlist_GUI.instance.showSuccessAndRefresh((String) msg.getContent());
		        }
			}
			
		});
		responseHandlers.put(MessageType.CANCEL_WAITLIST_AND_RESERVATION_FAILED, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (ManageWaitlist_GUI.instance != null) {
		            ManageWaitlist_GUI.instance.showErrorAlert((String) msg.getContent());
		        }
				
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onCancellationResponse((String) msg.getContent(), false);
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
		
		responseHandlers.put(MessageType.RETURN_ALL_ACTIVE_WAITLISTS, new ResponseHandler() {
	        @Override
	        public void handle(Message msg) {
	            List<Map<String, Object>> list = (List<Map<String, Object>>) msg.getContent();
	            // Update the GUI if it is currently open
	            if (gui.ManageWaitlist_GUI.instance != null) {
	                gui.ManageWaitlist_GUI.instance.updateTable(list);
	            }
	        }
	    });
		// -----------------------------------------------------------
		// Forgot code(in terminal)
		// -----------------------------------------------------------
		responseHandlers.put(MessageType.FORGOT_CODE_NOT_FOUND, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onForgotCodeHandle((String) msg.getContent(),false);
			}
		});
		responseHandlers.put(MessageType.FORGOT_CODE_FOUND, new ResponseHandler() {
			@Override
			public void handle(Message msg) {
				if (Terminal_GUI.instance != null)
					Terminal_GUI.instance.onForgotCodeHandle((String) msg.getContent(),true);
			}
		});
	}

	/**
     * Helper method to refresh reservations for the currently logged-in user.
     * uses getCasualIdentifier() to support Email logins.
     */
    private void refreshUserReservations() {
        Object user;
        
        // 1. Check for Subscriber
        if (User_Session.getLoggedInUser() != null) {
            user = User_Session.getLoggedInUser();
        } else {
            // 2. Check for Casual (Phone OR Email) using the new helper
            user = User_Session.getCasualIdentifier();
        }
        
        // 3. Null Safety Check before sending
        if (user != null) {
            sendGetReservationsRequest(user);
        } else {
            System.err.println("[Client_Controller] Cannot refresh reservations: No active session found.");
        }
    }

   //** 
    /**
	 * Displays incoming messages by deserializing and routing them to appropriate handlers.
	 */
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
	/**
	 * Reference to the Manage Users GUI for handling user management responses.
	 */
	public void setManageUsersGUI(ManageUsers_GUI manageUsers_GUI) {
		this.manageUsers_GUI = manageUsers_GUI;
	}

	/**
     * Resolves the user identifier (Phone or Email) and sends the request to the server.
     * This version supports login via Email by falling back to the email field if phone is missing.
     * Updated to handle String identifiers properly.
     */
	public void sendGetReservationsRequest(Object user) {
        String identifier = null;

        if (user == null) {
            System.err.println("[Client_Controller] sendGetReservationsRequest received NULL!");
            return;
        }

        System.out.println("[Client_Controller] Processing request for user object: " + user.toString());

        // Case 1: Subscriber (UserRecord)
        if (user instanceof entities.UserRecord) {
            entities.UserRecord record = (entities.UserRecord) user;
            identifier = record.getPhone();
            
            // Priority 2: Email (fallback)
            if (identifier == null || identifier.trim().isEmpty()) {
                identifier = record.getEmail();
            }
        } 
        // Case 2: Casual Customer (String)
        else if (user instanceof String) {
            identifier = (String) user;
        }

        // Validation and Sending
        if (identifier != null && !identifier.trim().isEmpty()) {
            System.out.println("[Client_Controller] Sending request for identifier: " + identifier);
            sendComplexObject(new Message(MessageType.GET_RESERVATIONS_BY_USER, identifier));
        } else {
            System.err.println("[Client_Controller] Error: Could not resolve a valid Phone or Email from the object.");
        }
    }

	/**
     * Sends a batch update request.
     * Wraps the schedule data and the force flag into a single map.
     * * @param batchData Map of Day to [Open, Close, IsActive]
     * @param force     True to overwrite existing reservations, False to validate.
     */
    public void sendBatchUpdateHours(Map<DayOfWeek, Object[]> batchData, boolean force) {
        Map<String, Object> wrapper = new HashMap<String, Object>();
        wrapper.put("schedule", batchData);
        wrapper.put("force", force);
        sendComplexObject(new Message(MessageType.UPDATE_REGULAR_HOURS, wrapper));
    }

    /**
	 * Sends a update reservation request to the server.
	 * 
	 * @param newRes The reservation details for the new reservation.
	 */
	public void sendUpdateReservationRequest(Reservation reservationToUpdate) {
		sendComplexObject(new Message(MessageType.UPDATE_RESERVATION_REQUEST, reservationToUpdate));
	}

	/**
	 * Sends a new reservation request to the server.
	 * 
	 * @param newRes The reservation details for the new reservation.
	 */
	public void sendNewReservationRequest(Reservation newRes) {
		sendComplexObject(new Message(MessageType.CREATE_RESERVATION, newRes));
	}

	/**
	 * Sends a new instant reservation request to the server.
	 * 
	 * @param newRes The reservation details for the instant reservation.
	 */
	public void sendNewInstantReservationRequest(Reservation newRes) {
		sendComplexObject(new Message(MessageType.CREATE_INSTANT_RESERVATION, newRes));
	}

	/**
	 * Sends a request to fetch the current opening hours from the server.
	 * 
	 */
	public void sendGetOpeningHoursRequest() {
		sendComplexObject(new Message(MessageType.GET_OPENING_HOURS, null));
	}

	/**
	 * Sends a request to delete a special hour entry for a specific date.
	 * 
	 * @param date The date of the special hour to delete.
	 */
	public void sendDeleteSpecialHourRequest(LocalDate date) {
		sendComplexObject(new Message(MessageType.DELETE_SPECIAL_HOUR, date));
	}

	/**
	 * Sends a request to fetch all tables from the server.
	 * 
	 */
	public void sendGetAllPendingReservationsRequest() {
		sendComplexObject(new Message(MessageType.GET_ALL_PENDING_RESERVATIONS, null));
	}

	/**
	 * Sends a request to fetch all tables from the server.
	 * 
	 */
	public void sendGetAllPendingAndActiveReservationsRequest() {
		sendComplexObject(new Message(MessageType.GET_ALL_PENDING_AND_ACTIVE_RESERVATIONS, null));
	}

	/**
	 * Sends a complex object to the server by serializing it with Kryo.
	 */
	public void sendComplexObject(Object obj) {
		try {
			byte[] payload = KryoUtil.serialize(obj);
			client.handleMessageFromClientUI(payload);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logs out the current user and closes the client connection.
	 * 
	 */
	public void logout() {
		sendComplexObject(new Message(MessageType.LOGOUT_REQUEST, null));
		client.quit();
	}

	/**
	 * Sends a login request for a subscriber user.
	 * @param loginData The login credentials.
	 */
	public void sendSubscriberLoginRequest(LoginData loginData) {
		sendComplexObject(new Message(MessageType.LOGIN_REQUEST_SUB, loginData));
	}
	
	/**
	 * Sends a login request for an employee user.
	 * @param loginData The login credentials.
	 * 
	 */
	public void sendEmployeeLoginRequest(LoginData loginData) {
		sendComplexObject(new Message(MessageType.LOGIN_REQUEST_EMP, loginData));
	}

	/**
	 * Sends a login request for a guest user.
	 * @param loginData The login credentials.
	 */
	public void sendGuestLoginRequest(LoginData loginData) {
		sendComplexObject(new Message(MessageType.LOGIN_REQUEST_GUEST, loginData));
	}
	// ----------------------------------------------------------------------
	// User Management Request Methods
	// ----------------------------------------------------------------------
	
	/**
	 * Sends a request to fetch all user records from the server.
	 * 
	 */
	public void sendGetAllUsersRequest() {
		sendComplexObject(new Message(MessageType.GET_ALL_USERS_REQUEST));
	}
	
	/**
	 * Sends a request to add a new user record to the server.
	 * @param newUser The new user record to add.
	 */
	public void sendAddUserRequest(UserRecord newUser) {
		sendComplexObject(new Message(MessageType.ADD_USER_REQUEST, newUser));
	}

	/**
	 * Sends a request to edit an existing user record on the server.
	 * @param user The user record with updated details.
	 */
	public void sendEditUserRequest(UserRecord user) {
		sendComplexObject(new Message(MessageType.EDIT_USER_REQUEST, user));
	}

	/**
	 * Sends a request to remove a user record from the server.
	 * @param user The user record to remove.
	 */
	public void sendRemoveUserRequest(UserRecord user) {
		sendComplexObject(new Message(MessageType.DELETE_USER_REQUEST, user));
	}

	// ----------------------------------------------------------------------
	// Additional Request Methods
	// ----------------------------------------------------------------------
	
	/**
	 * Sends a cancellation request for a reservation or waitlist entry from the terminal.
	 * @param code The 6-digit reservation/waitlist code.
	 */
	public void sendCancelReservationOrWaitlistRequestFromTerminal(int code) {
		sendComplexObject(new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_BY_CODE, code));
	}

	/**
	 * Sends a check-in request for a reservation using its confirmation code.
	 * @param confiCode The 6-digit reservation confirmation code.
	 */
	public void sendCheckInRequest(int confiCode) {
		sendComplexObject(new Message(MessageType.CHECK_IN_REQUEST, confiCode));
	}

	/**
	 * Sends a join waitlist request to the server.
	 * @param waitlistReq The reservation object containing waitlist details.
	 */
	public void sendJoinWaitlistRequest(Reservation waitlistReq) {
		sendComplexObject(new Message(MessageType.JOIN_WAITLIST, waitlistReq));
	}

	/**
	 * Sends a request to fetch a bill by its unique code.
	 * @param code The unique bill code.
	 */
	public void sendGetBillRequest(int code) {
		sendComplexObject(new Message(MessageType.BILL_REQUEST, code));
	}
	
	/**
	 * Sends a request to pay a specific bill.
	 * @param currentBillToPay The bill object to be paid.
	 */
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

	/**
	 * Sends a request to recover reservation codes using phone and/or email.
	 * 
	 * @param phone The user's phone number.
	 * @param email The user's email address.
	 */
	public void sendRecoverCodesRequest(String phone, String email) {
		sendComplexObject(new Message(MessageType.FORGOT_CODE,new UserRecord(phone,email)));		
	}

	/**
	 * Sends a request to get daily reservations for a specific user.
	 * @param userId The subscriber ID.
	 */
	public void sendGetDailyReservationsRequest(int userId) {
		sendComplexObject(new Message(MessageType.GET_RESERVATIONS_BY_USER,userId));
	}
	/**
     * Sends a request to the server to get all active waitlist entries.
     */
    public void sendGetAllActiveWaitlistsRequest() {
        sendComplexObject(new Message(MessageType.GET_ALL_ACTIVE_WAITLISTS, null));
    }
    /**
     * Sends a request to get the full reservation history for a logged-in user.
     * @param userId The subscriber ID.
     */
    public void sendGetReservationHistoryRequest(int userId) {
        sendComplexObject(new Message(MessageType.GET_RESERVATION_HISTORY, userId));
    }
    
    
    //------Table Management -----
    /**
     * Sends a request to get all restaurant tables.
     */
    public void sendGetAllTablesRequest() {
        sendComplexObject(new Message(MessageType.GET_ALL_TABLES, null));
    }

    /**
     * Sends a request to add a new table.
     * @param table The new table object to add.
     */
    public void sendAddTableRequest(Restaurant_Table table) {
        sendComplexObject(new Message(MessageType.ADD_TABLE_REQUEST, table));
    }

    /**
     * Sends a request to update an existing table.
     * @param table The table object with updated details.
     */
    public void sendUpdateTableRequest(Restaurant_Table table) {
        sendComplexObject(new Message(MessageType.UPDATE_TABLE_REQUEST, table));
    }

	 /**
	  * Requests the latest reservation/bill associated with a phone number.
	  * @param phone The customer's phone number string.
	  */
	 public void sendGetLatestBillByPhoneRequest(String phone) {
	     // We use the specific MessageType the server expects for this lookup
	     sendComplexObject(new Message(MessageType.GET_LATEST_RESERVATION_BY_PHONE, phone));
	 }
	 
	 /**
	  * Requests the latest reservation/bill associated with a phone number.
	  * @param phone The customer's phone number string.
	  */
	 public void sendGetLatestBillByEmailRequest(String email) {
	     // We use the specific MessageType the server expects for this lookup
	     sendComplexObject(new Message(MessageType.GET_LATEST_RESERVATION_BY_EMAIL, email));
	 }
	
	 /**
	  * Sends a request to finalize a payment for a specific bill ID.
	  * @param billId The database ID of the bill.
	  */
	 public void sendBillPaymentRequest(int billId) {
	     sendComplexObject(new Message(MessageType.BILL_PAYMENT_REQUEST, billId));
	 }
	
	 /**
	  * Sends a request to cancel a waitlist entry using its confirmation code.
	  * @param confCode The 6-digit reservation code.
	  */
	 public void sendCancelWaitlistRequest(int confCode) {
	     sendComplexObject(new Message(MessageType.CANCEL_WAITLIST_AND_RESERVATION_BY_CODE, confCode));
	 }
	
	 /**
	  * Sends a request to delete a table by its physical table number.
	  * @param tableNumber The number assigned to the table.
	  */
	 public void sendDeleteTableRequest(int tableNumber) {
	     sendComplexObject(new Message(MessageType.DELETE_TABLE_REQUEST, tableNumber));
	 }
	 
	 /**
	  * Requests the full list of bills from the server for the management table.
	  */
	 public void sendGetAllBillsRequest() {
	     sendComplexObject(new Message(MessageType.GET_ALL_BILLS, null));
	 }
	
	 /**
	  * Sends a request to delete a specific bill from the system.
	  * @param billId The database ID of the bill record.
	  */
	 public void sendDeleteBillRequest(int billId) {
	     sendComplexObject(new Message(MessageType.DELETE_BILL, billId));
	 }
	
	 /**
	  * Sends a request to mark a bill as paid.
	  * @param billId The database ID of the bill.
	  */
	 public void sendMarkBillAsPaidRequest(int billId) {
	     sendComplexObject(new Message(MessageType.BILL_PAYMENT_REQUEST, billId));
	 }
	 
	 /**
	  * Sends a request to cancel a reservation by its ID.
	  * @param reservationId The ID of the reservation to remove.
	  */
	 public void sendCancelReservationRequest(int reservationId) {
	     sendComplexObject(new Message(MessageType.CANCEL_RESERVATION, reservationId));
	 }
	 /**
	     * Sends a request to get the completed visit history (with bills) for a user.
	     * @param userId The subscriber ID.
	     */
	    public void sendGetVisitHistoryRequest(int userId) {
	        sendComplexObject(new Message(MessageType.GET_VISIT_HISTORY, userId));
	    }

	 /**
	  * asks for the restaurant entity to get the time of the close for today, to refresh at the terminal.
	  * @return LocalTime the close time
	  */
	public LocalTime refreshOH() { 
		return Restaurant.getInstance().getOpeningHours().getRegularSchedule().get(LocalDateTime.now().getDayOfWeek()).getCloseTime();
	}

	 /**
	  * Sends a request to refresh max table size from terminal gui.
	 * @return int the biggest table size
	  */
	public int refreshMaxTableCapacity() {
	     return Restaurant.getBiggestTableSize();	
	}

	
}