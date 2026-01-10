package gui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import entities.Opening_Hours;
import entities.Opening_Hours.TimeRange;
import entities.Reservation;
import entities.Restaurant;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import utils.User_Session;

/**
 * Controller for viewing and managing user reservations. 
 * Provides features for listing existing reservations in a TableView and 
 * performing dynamic edits with real-time validation against restaurant capacity and hours.
 */
public class ViewReservations_GUI {

    /** Static instance to allow the Client_Controller to access this UI controller */
	public static ViewReservations_GUI instance;
    /** Path to the previous screen for navigation purposes */
	public static String previousScreen;

	@FXML
	private TableView<Reservation> reservationsTable;
	@FXML
	private TableColumn<Reservation, Integer> colCode;
	@FXML
	private TableColumn<Reservation, String> colDate, colTime;
	@FXML
	private TableColumn<Reservation, Integer> colGuests;

	@FXML
	private DatePicker editDatePicker;
	@FXML
	private ComboBox<String> editTimeCombo;

	/** Spinner replacing the old TextField to manage guest count with constraints */
	@FXML
	private Spinner<Integer> editGuestsSpinner;

    /** Observable list that holds the reservation data for the TableView */
	private ObservableList<Reservation> masterData = FXCollections.observableArrayList();

    /** The current maximum capacity allowed for a single reservation, updated by the server */
	private int maxRestaurantCapacity = 10; // Default fallback

	/**
	 * Initializes the controller. Sets up the TableView, date restrictions, 
     * guest spinner, and requests the latest operational data from the server.
	 */
	@FXML
	public void initialize() {
		instance = this;
		setupTable();
		restrictDatePickerRange();
		setupGuestSpinner(); // Initialize the spinner

		// 1. Request current opening hours to validate edit slots
		ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();

		// 2. Request Table Data to determine the maximum seating capacity for validation
		ConnectToServer_GUI.clientController.sendGetAllTablesRequest();

		refreshTableData();

		// Listener for table selection to populate the edit form automatically
		reservationsTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Reservation>() {
			@Override
			public void changed(ObservableValue<? extends Reservation> obs, Reservation oldVal, Reservation newVal) {
				if (newVal != null) {
					populateEditFields(newVal);
				}
			}
		});

		// Listener for DatePicker changes to refresh available time slots for the selected date
		editDatePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
			@Override
			public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldVal, LocalDate newVal) {
				if (newVal != null) {
					// Load hours without a specific selection (for manual date change)
					loadDynamicHours(newVal, null);
				}
			}
		});
	}

	/**
	 * Configures the guest spinner with a default range. 
     * Includes a focus listener to ensure manual text inputs are committed properly.
	 */
	private void setupGuestSpinner() {
		// Default range 1-10 until server updates with actual restaurant data
		SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2);
		editGuestsSpinner.setValueFactory(factory);

		// Commit manual edits when the user clicks away from the spinner
		editGuestsSpinner.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) { // Focus lost
					editGuestsSpinner.increment(0); // Triggers manual value commit
				}
			}
		});
	}

	/**
	 * Called by Client_Controller when table data arrives. 
     * Dynamically updates the Spinner's maximum limit to match the largest table in the restaurant.
	 * @param realMax The maximum table size found in the database.
	 */
	public void updateMaxCapacity(int realMax) {
		this.maxRestaurantCapacity = realMax;
		System.out.println("[ViewReservations] Max capacity updated to: " + realMax);

		if (editGuestsSpinner != null) {
			SpinnerValueFactory<Integer> currentFactory = editGuestsSpinner.getValueFactory();
			if (currentFactory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
				((SpinnerValueFactory.IntegerSpinnerValueFactory) currentFactory).setMax(realMax);
			}
		}
	}

	/**
	 * Configures the TableView columns and binds them to the Reservation properties.
	 */
	private void setupTable() {
		reservationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
		colDate.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
		colTime.setCellValueFactory(new PropertyValueFactory<>("formattedTime"));
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		reservationsTable.setItems(masterData);
	}

	/**
	 * Dynamically populates the time ComboBox based on the restaurant's schedule.
     * Accounts for same-day booking buffers and midnight-crossing shifts.
	 * @param selectedDate The date chosen for the reservation.
	 * @param timeToSelect Optional: A specific time string to pre-select (used when loading an existing reservation).
	 */
	public void loadDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Reset ComboBox state
				editTimeCombo.setValue(null);
				editTimeCombo.getItems().clear();
				editTimeCombo.setPromptText("");

				Opening_Hours oh = Restaurant.getInstance().getOpeningHours();

				if (oh == null) {
					ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
					return;
				}

				LocalTime open = null;
				LocalTime close = null;

				// Determine opening/closing times (Check exceptions first, then regular schedule)
				if (oh.getExceptionSchedule().containsKey(selectedDate)) {
					TimeRange range = oh.getExceptionSchedule().get(selectedDate);
					if (range != null && range.isActive()) {
						open = range.getOpenTime();
						close = range.getCloseTime();
					}
				} else {
					DayOfWeek day = selectedDate.getDayOfWeek();
					if (oh.getRegularSchedule().containsKey(day)) {
						TimeRange range = oh.getRegularSchedule().get(day);
						if (range != null && range.isActive()) {
							open = range.getOpenTime();
							close = range.getCloseTime();
						}
					}
				}

				if (open != null && close != null) {
					LocalTime firstAvailable = open;
					LocalTime lastSlot = close.minusHours(2);
					boolean isMidnightCrossing = !close.isAfter(open);

					// --- Logic for "Today" (Handling past times) ---
					if (selectedDate.equals(LocalDate.now())) {
						LocalTime now = LocalTime.now();

						// Check if we already missed the last possible booking slot for a standard shift
						if (!isMidnightCrossing && now.isAfter(lastSlot)) {
							if (timeToSelect == null) {
								editTimeCombo.setPromptText("Closed Today"); 
								return;
							}
						}

						LocalTime bufferTime = roundToNext30Min(now.plusHours(1));
						boolean shouldSkipToBuffer = false;

						if (isMidnightCrossing) {
							if (bufferTime.isAfter(open) || bufferTime.isBefore(close)) {
								shouldSkipToBuffer = true;
							}
						} else {
							if (bufferTime.isBefore(now)) {
								if (timeToSelect == null) {
									editTimeCombo.setPromptText("Closed"); 
									return;
								}
							}
							if (bufferTime.isAfter(open)) {
								shouldSkipToBuffer = true;
							}
						}

						if (shouldSkipToBuffer)
							firstAvailable = bufferTime;
					}

					// --- Populate the time intervals (30 min increments) ---
					LocalTime t = firstAvailable;
					int safety = 0;
					while (safety < 48) {
						boolean isValid = isMidnightCrossing ? (!t.isBefore(open) || !t.isAfter(lastSlot))
								: (!t.isBefore(open) && !t.isAfter(lastSlot));

						if (isValid) {
							editTimeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
						} else {
							if (!isMidnightCrossing && !editTimeCombo.getItems().isEmpty())
								break;
						}

						if (t.equals(lastSlot))
							break;
						t = t.plusMinutes(30);
						safety++;
					}

					// Update UI prompt text based on availability
					if (editTimeCombo.getItems().isEmpty() && timeToSelect == null) {
						editTimeCombo.setPromptText("Closed Today");
					} else if (editTimeCombo.getPromptText().isEmpty()) {
						editTimeCombo.setPromptText("Select Time");
					}

					if (timeToSelect != null) {
						editTimeCombo.setValue(timeToSelect);
					}
				} else {
					editTimeCombo.setPromptText("Closed Today");
				}
			}
		});
	}

    /** Rounds the current time to the next 30-minute window for slot generation */
	private LocalTime roundToNext30Min(LocalTime time) {
		int min = time.getMinute();
		if (min == 0)
			return time.withSecond(0).withNano(0);
		if (min <= 30)
			return time.withMinute(30).withSecond(0).withNano(0);
		return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
	}

	/**
	 * Populates the edit form fields when a reservation row is selected in the table.
	 * @param res The Reservation object to be edited.
	 */
	private void populateEditFields(Reservation res) {
		editDatePicker.setValue(res.getOrderStartTime().toLocalDate());
		String timeStr = String.format("%02d:%02d", res.getOrderStartTime().getHour(),
				res.getOrderStartTime().getMinute());

		loadDynamicHours(res.getOrderStartTime().toLocalDate(), timeStr);

		// Update Spinner with the current number of diners
		editGuestsSpinner.getValueFactory().setValue(res.getNumberOfDiners());
	}

	/**
     * Validates updated input from the form and sends an update request to the server.
     * Checks for empty fields and ensures the group size does not exceed max capacity.
     * @param event The ActionEvent from the update button.
     */
    @FXML
    void onUpdateClicked(ActionEvent event) {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        
        // Ensure a row is selected for modification
        if (selected == null) {
            showErrorAlert("No Selection", "Please select a reservation from the list to update.");
            return;
        }

        // Validation: Check for empty input fields
        if (editDatePicker.getValue() == null || editTimeCombo.getValue() == null || editGuestsSpinner.getValue() == null) {
        	showErrorAlert("Input Error", "All fields are required for the update.");
            return;
        }

        int diners = editGuestsSpinner.getValue();

        // Safety check against maximum restaurant capacity
        if (diners > maxRestaurantCapacity) {
            showErrorAlert("Capacity Error",
                    "We cannot accommodate a group of " + diners + ".\nMax capacity is " + maxRestaurantCapacity + ".");
            return;
        }

        try {
            LocalDate selectedDate = editDatePicker.getValue();
            LocalTime selectedTime = LocalTime.parse(editTimeCombo.getValue());

            // Handle date adjustment for shifts crossing midnight
            Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
            LocalTime openTime = null;

            if (oh != null) {
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    openTime = oh.getExceptionSchedule().get(selectedDate).getOpenTime();
                } else {
                    openTime = oh.getRegularSchedule().get(selectedDate.getDayOfWeek()).getOpenTime();
                }
            }

            if (openTime != null && selectedTime.isBefore(openTime)) {
                selectedDate = selectedDate.plusDays(1);
            }

            LocalDateTime newStart = LocalDateTime.of(selectedDate, selectedTime);

            // Update the local object before sending to server
            selected.setOrderStartTime(newStart);
            selected.setOrderEndTime(newStart.plusHours(2)); // Standard 2-hour duration
            selected.setNumberOfDiners(diners);

            // Forward the updated reservation to the server via ClientController
            ConnectToServer_GUI.clientController.sendUpdateReservationRequest(selected);

        } catch (Exception e) {
            showErrorAlert("System Error", "Failed to process update: " + e.getMessage());
        }
    }

    /** Displays a warning alert to the user */
	private void showErrorAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * Offers an alternative time slot if the restaurant is full during the requested time.
	 * @param suggested The alternative LocalDateTime provided by the server.
	 */
	public void showNoTableAlert(final LocalDateTime suggested) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (suggested == null) {
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Fully Booked");
					alert.setHeaderText("No Availability Found");
					alert.setContentText("We are sorry, but there are no available tables for the rest of the day.");
					alert.showAndWait();
					return;
				}

				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
				String displayString = suggested.format(dateFormatter) + " at " + suggested.format(timeFormatter);

				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Capacity Reached");
				alert.setHeaderText("The restaurant is full at the selected time.");
				alert.setContentText("The nearest available slot is: " + displayString
						+ ".\n\nWould you like to update the form to this time?");

				alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
					@Override
					public void accept(ButtonType response) {
						if (response == ButtonType.OK) {
							updateFormFields(suggested);
						}
					}
				});
			}
		});
	}

	/** Updates form fields to match a suggested alternative time slot */
	private void updateFormFields(LocalDateTime dateTime) {
		if (editDatePicker != null) {
			editDatePicker.setValue(dateTime.toLocalDate());
		}

		if (editTimeCombo != null) {
			String timeStr = String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
			editTimeCombo.setValue(timeStr);
		}
	}

    /** Confirms a successful update to the user */
	public void showSuccessAlert() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reservation updated successfully!", ButtonType.OK);
		alert.showAndWait();
	}

	/** Sets visual restrictions on the DatePicker to allow bookings only for the next 30 days */
	private void restrictDatePickerRange() {
		final LocalDate minDate = LocalDate.now();
		final LocalDate maxDate = LocalDate.now().plusMonths(1);

		editDatePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
			@Override
			public DateCell call(final DatePicker datePicker) {
				return new DateCell() {
					@Override
					public void updateItem(LocalDate item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null && (item.isBefore(minDate) || item.isAfter(maxDate))) {
							setDisable(true);
							setStyle(
									"-fx-background-color: #4a4a4a; -fx-control-inner-background: #4a4a4a; -fx-text-fill: white;");
						} else {
							setStyle("-fx-text-fill: white;");
						}
					}
				};
			}
		});
	}

	/** Requests the initial reservation list from the server based on the current user session */
	private void refreshTableData() {
        Object id = User_Session.getLoggedInUser();

        if (id == null) {
            String casualId = User_Session.getCasualIdentifier();
            if (casualId != null && !casualId.isEmpty()) {
                id = casualId;
            }
        }

        if (id != null) {
            System.out.println("[ViewReservations] Sending request for ID: " + id);
            ConnectToServer_GUI.clientController.sendGetReservationsRequest(id);
        } else {
            System.err.println("[ViewReservations] ERROR: Session is empty. Cannot fetch data.");
        }
    }

	/** Handles the cancellation of a selected reservation with a confirmation prompt */
	@FXML
	void onDeleteClicked(ActionEvent event) {
		final Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirm cancellation?");
		alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
			@Override
			public void accept(ButtonType r) {
				if (r == ButtonType.OK) {
					ConnectToServer_GUI.clientController.sendCancelReservationRequest(selected.getId());
				}
			}
		});
	}

	/** Navigates back to the previous screen */
	@FXML
	private void onBackClicked(ActionEvent event) {
		instance = null;
		try {
			String screen = previousScreen != null ? previousScreen : "/gui/CasualCustomer.fxml";

			Parent root = FXMLLoader.load(getClass().getResource(screen));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
			stage.setTitle("Bistro");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Updates the TableView data. Called by Client_Controller when the list is received */
	public void updateTable(List<Reservation> list) {
		masterData.setAll(list);
	}
}