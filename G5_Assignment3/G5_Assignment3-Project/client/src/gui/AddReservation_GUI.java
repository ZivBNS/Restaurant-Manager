package gui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import entities.Opening_Hours;
import entities.Opening_Hours.TimeRange;
import entities.Reservation;
import entities.Restaurant;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;
import utils.User_Session;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Controller for the New Reservation screen. Handles dynamic time slot
 * generation, date restrictions, guest capacity management, and dark mode UI components.
 * This class ensures that users can only pick valid operating hours.
 */
public class AddReservation_GUI {

	/** Static instance allowing the Client_Controller to interact with the active UI */
	public static AddReservation_GUI instance;
    /** Stores the FXML path of the previous screen for navigation purposes */
	public static String previousScreen;

	@FXML
	private DatePicker datePicker;
	@FXML
	private ComboBox<String> timeCombo;

	/** Spinner used to select the number of guests, replacing standard text input */
	@FXML
	private Spinner<Integer> guestsSpinner;

	/**
	 * Initializes the UI components.
     * Sets up the date range restrictions, initializes the guest spinner,
     * and requests current restaurant data (tables and hours) from the server.
	 */
	@FXML
	public void initialize() {
		instance = this;
		restrictDatePickerRange();
		setupSpinnerWithMax(10); // Default max until updated by server
		ConnectToServer_GUI.clientController.sendGetAllTablesRequest();
		ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
		
		// Set default date to Today and load available hours
		datePicker.setValue(LocalDate.now());
		loadDynamicHours(LocalDate.now());

		// Listener to refresh time slots whenever the selected date changes
		datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
			@Override
			public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue,
					LocalDate newValue) {
				if (newValue != null) {
					loadDynamicHours(newValue);
				}
			}
		});
        
        // Custom cell factory for styling the ComboBox dropdown in dark mode
		timeCombo.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> param) {
				return new ListCell<String>() {
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (item == null || empty) {
							setText(null);
						} else {
							setText(item);
							setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");
						}
					}
				};
			}
		});

        // Custom styling for the selected item in the ComboBox
		timeCombo.setButtonCell(new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setText(null);
				} else {
					setText(item);
					setStyle("-fx-text-fill: #f8fafc;");
				}
			}
		});
	}

	/**
     * Updates the maximum capacity allowed in the guests spinner.
     * Called by Client_Controller once table data is received from the DB.
     * @param realMaxCapacity The actual maximum table size found in the restaurant.
     */
    public void updateSpinnerLimit(int realMaxCapacity) {
        System.out.println("[GUI] Updating spinner max to: " + realMaxCapacity);
        setupSpinnerWithMax(realMaxCapacity);
    }

    /**
     * Configures the SpinnerValueFactory with the specified maximum value.
     * @param max The upper limit for the number of guests.
     */
    private void setupSpinnerWithMax(int max) {
        SpinnerValueFactory<Integer> factory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, 2);
        guestsSpinner.setValueFactory(factory);
        
        // Ensure manual text input is committed when focus is lost
        guestsSpinner.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    guestsSpinner.increment(0); 
                }
            }
        });
    }

	/**
	 * Limits the DatePicker to a valid range: [Today, Today + 1 Month].
     * Disables and styles dates outside of this window.
	 */
	private void restrictDatePickerRange() {
		final LocalDate minDate = LocalDate.now();
		final LocalDate maxDate = LocalDate.now().plusMonths(1);

		datePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
			@Override
			public DateCell call(final DatePicker datePicker) {
				return new DateCell() {
					@Override
					public void updateItem(LocalDate item, boolean empty) {
						super.updateItem(item, empty);
						if (item.isBefore(minDate) || item.isAfter(maxDate)) {
							setDisable(true);
							// Apply dark mode styling for disabled dates
							setStyle(
									"-fx-background-color: #4a4a4a; -fx-control-inner-background: #4a4a4a; -fx-text-fill: white;");
						}
					}
				};
			}
		});
	}

	/**
	 * Dynamically generates time slots for the ComboBox based on restaurant hours.
	 * Accounts for:
     * 1. Date-specific exceptions (Holidays/Events).
     * 2. Regular weekly schedules.
     * 3. Same-day buffer (1 hour ahead + 30 min rounding).
     * 4. Midnight crossing shifts (e.g., 20:00 to 02:00).
	 * @param selectedDate The date chosen by the user.
	 */
	public void loadDynamicHours(final LocalDate selectedDate) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				timeCombo.setValue(null);
				timeCombo.getItems().clear();
				Opening_Hours oh = Restaurant.getInstance().getOpeningHours();

				if (oh == null) {
					ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
					return;
				}

				LocalTime open = null;
				LocalTime close = null;

				// Step 1: Identify opening/closing window for the selected date
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
                    // The last booking slot must be at least 2 hours before closing
					LocalTime lastSlot = close.minusHours(2);
					boolean isMidnightCrossing = !close.isAfter(open);

					// Step 2: Handle Same-Day logic (Current Time Buffer)
					if (selectedDate.equals(LocalDate.now())) {
						LocalTime now = LocalTime.now();

						// Check if we missed the last slot for standard daytime shifts
						if (!isMidnightCrossing && now.isAfter(lastSlot)) {
							timeCombo.setPromptText("Closed Today");
							return; 
						}

                        // Calculate 1-hour buffer and round to nearest 30-min mark
						LocalTime bufferTime = roundToNext30Min(now.plusHours(1));
						boolean shouldSkipToBuffer = false;

						if (isMidnightCrossing) {
							// For night shifts (e.g. 20:00 to 02:00)
							if (bufferTime.isAfter(open) || bufferTime.isBefore(close)) {
								shouldSkipToBuffer = true;
							}
						} else {
							// For standard daytime shifts
							if (bufferTime.isBefore(now)) { // Wrapped around midnight
								timeCombo.setPromptText("Closed Today");
								return;
							}
							if (bufferTime.isAfter(open)) {
								shouldSkipToBuffer = true;
							}
						}

						if (shouldSkipToBuffer)
							firstAvailable = bufferTime;
					}

					// Step 3: Populate the ComboBox with 30-minute intervals
					LocalTime t = firstAvailable;
					int safetyBreaker = 0; // Prevent infinite loops
					while (safetyBreaker < 48) {
						boolean isValidTime = isMidnightCrossing ? (!t.isBefore(open) || !t.isAfter(lastSlot))
								: (!t.isBefore(open) && !t.isAfter(lastSlot));

						if (isValidTime) {
							timeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
						} else {
							// If we hit an invalid time after valid ones in a standard shift, we are done
							if (!isMidnightCrossing && !timeCombo.getItems().isEmpty())
								break;
						}

						if (t.equals(lastSlot))
							break;
						t = t.plusMinutes(30);
						safetyBreaker++;
					}
					timeCombo.setPromptText(timeCombo.getItems().isEmpty() ? "Closed Today" : "Select Time");
				} else {
					timeCombo.setPromptText("Closed Today");
				}
			}
		});
	}

	/**
	 * Rounds a given time up to the next 30-minute increment.
	 * @param time The time to round.
	 * @return Rounded LocalTime object.
	 */
	private LocalTime roundToNext30Min(LocalTime time) {
		int minutes = time.getMinute();
		if (minutes == 0)
			return time.withSecond(0).withNano(0);
		if (minutes <= 30)
			return time.withMinute(30).withSecond(0).withNano(0);
		return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
	}

	/**
	 * Navigates back to the previous screen (defaulting to CasualCustomer if none specified).
	 */
	@FXML
	private void onBackClicked() {
		instance = null;
		try {
			String screen = previousScreen != null ? previousScreen : "/gui/CasualCustomer.fxml";

			Parent root = FXMLLoader.load(getClass().getResource(screen));
			Stage stage = (Stage) timeCombo.getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
			stage.setTitle("Bistro");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the "Save Order" action. Validates input, adjusts for midnight crossing,
     * and sends a new Reservation object to the server.
	 * @param event The action event triggered by the button.
	 */
	@FXML
	void onSaveOrderClicked(ActionEvent event) {
		if (datePicker.getValue() == null || timeCombo.getValue() == null) {
			showErrorAlert("Input Error", "Please select a date and a time.");
			return;
		}

		int diners = guestsSpinner.getValue();

		try {
			LocalDate selectedDate = datePicker.getValue();
			LocalTime time = LocalTime.parse(timeCombo.getValue());
			Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
			LocalTime openTime = null;

			if (oh != null) {
				if (oh.getExceptionSchedule().containsKey(selectedDate)) {
					openTime = oh.getExceptionSchedule().get(selectedDate).getOpenTime();
				} else {
					openTime = oh.getRegularSchedule().get(selectedDate.getDayOfWeek()).getOpenTime();
				}
			}

			// Adjust date if the selected time belongs to the "after-midnight" portion of the shift
			if (openTime != null && time.isBefore(openTime)) {
				selectedDate = selectedDate.plusDays(1);
			}

			LocalDateTime startDateTime = LocalDateTime.of(selectedDate, time);
			LocalDateTime endDateTime = startDateTime.plusHours(2);

			Reservation newRes;
            // Determine if the reservation is for a logged-in user or a guest
			if (User_Session.getLoggedInUser() != null) {
				System.out.println(User_Session.getLoggedInUser().getSubscriberCode());
				newRes = new Reservation(1, User_Session.getLoggedInUser().getPhone(),
						User_Session.getLoggedInUser().getEmail(), startDateTime, endDateTime, diners);
			} else {
				newRes = new Reservation(null, User_Session.getCasualPhone(), User_Session.getCasualEmail(),
						startDateTime, endDateTime, diners);
			}

			ConnectToServer_GUI.clientController.sendNewReservationRequest(newRes);

		} catch (Exception e) {
			System.err.println("Error creating reservation: " + e.getMessage());
		}
	}

    /** Displays a generic warning alert to the user */
	private void showErrorAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

    /** Displays the unique reservation code upon successful booking */
	public void showSuccessAlert(int code) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Reservation Confirmed");
		alert.setHeaderText("Success!");
		alert.setContentText("Your reservation code is: " + code);
		alert.showAndWait();
	}

	/**
	 * Triggered when the requested time is full. Offers the user an alternative time slot.
	 * @param suggested The nearest available LocalDateTime suggested by the server.
	 */
	public void showNoTableAlert(final LocalDateTime suggested) {
		if (suggested == null) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Fully Booked");
			alert.setContentText(
					"No available tables for the requested date. Please try another date.");
			alert.showAndWait();
			return;
		}

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		String displayString = suggested.format(dateFormatter) + " at " + suggested.format(timeFormatter);

		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("No Tables Available");
		alert.setHeaderText("Restaurant is full.");
		alert.setContentText("Switch to nearest available: " + displayString + "?");

		alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
			@Override
			public void accept(ButtonType response) {
				if (response == ButtonType.OK) {
					updateFormFields(suggested);
				}
			}
		});
	}

	/**
	 * Helper method to automatically update form inputs when an alternative time is accepted.
	 */
	private void updateFormFields(LocalDateTime dateTime) {
		datePicker.setValue(dateTime.toLocalDate());
		timeCombo.setValue(String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute()));
	}

    /** @return The datePicker component */
	public DatePicker getDatePicker() {
		return datePicker;
	}
}