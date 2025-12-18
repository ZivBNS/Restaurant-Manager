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
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class AddReservation_GUI {
	// Allows the Client_Controller to access the active screen
    public static AddReservation_GUI instance;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private TextField guestsField;

    @FXML
    public void initialize() {
        instance = this;
        restrictDatePickerRange();

        // Set default date to Today
        datePicker.setValue(LocalDate.now());
        loadDynamicHours(LocalDate.now());

        // Listener for future changes
        datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) {
                if (newValue != null) {
                    loadDynamicHours(newValue);
                }
            }
        });
    }

    /**
     * Restricts the DatePicker to allow selection only from today's date
     * up until exactly one month in the future.
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
                        // Disable dates before today or after one month from today
                        if (item.isBefore(minDate) || item.isAfter(maxDate)) {
                            setDisable(true);
                            setStyle("-fx-background-color: #eeeeee;"); // Gray out disabled dates
                        }
                    }
                };
            }
        });
    }

    /**
     * Dynamically populates the time ComboBox based on the restaurant's opening hours.
     * Fixed: Correctly enforces 1-hour buffer for today even with midnight-crossing.
     * @param selectedDate The date picked from the DatePicker.
     */
    public void loadDynamicHours(LocalDate selectedDate) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                timeCombo.getItems().clear();
                Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
                
                if (oh == null) {
                    System.out.println("DEBUG: Opening Hours data is MISSING on Client side!");
                    ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
                    return;
                }

                LocalTime open = null;
                LocalTime close = null;

                // 1. Determine opening/closing times for the selected date
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    TimeRange range = oh.getExceptionSchedule().get(selectedDate);
                    if (range != null) {
                        open = range.getOpenTime();
                        close = range.getCloseTime();
                    }
                } else {
                    DayOfWeek day = selectedDate.getDayOfWeek();
                    if (oh.getRegularSchedule().containsKey(day)) {
                        TimeRange range = oh.getRegularSchedule().get(day);
                        open = range.getOpenTime();
                        close = range.getCloseTime();
                    }
                }

                // 2. Populate slots if the restaurant is open
                if (open != null && close != null) {
                    LocalTime firstAvailable = open;
                    LocalTime lastSlot = close.minusHours(2); // Latest start time for a 2-hour meal
                    boolean isMidnightCrossing = !close.isAfter(open);

                    // 3. Apply 1-hour buffer logic for Today
                    if (selectedDate.equals(LocalDate.now())) {
                        LocalTime bufferTime = roundToNext30Min(LocalTime.now().plusHours(1));
                        
                        // Check if bufferTime is "ahead" of opening time in business context
                        boolean shouldSkipToBuffer = false;
                        if (isMidnightCrossing) {
                            // In a day like 08:00 to 02:00, 23:00 or 01:00 are both after 08:00
                            if (bufferTime.isAfter(open) || bufferTime.isBefore(close)) {
                                shouldSkipToBuffer = true;
                            }
                        } else {
                            // Standard day: 08:00 to 22:00
                            if (bufferTime.isAfter(open)) {
                                shouldSkipToBuffer = true;
                            }
                        }

                        if (shouldSkipToBuffer) {
                            firstAvailable = bufferTime;
                        }
                    }

                    // 4. Fill ComboBox (Handles both standard and midnight crossing)
                    LocalTime t = firstAvailable;
                    int safetyBreaker = 0; 
                    
                    // Check if the current 't' is still valid within the business day
                    while (safetyBreaker < 48) { 
                        // Validation: t is between open and close (handling midnight)
                        boolean isValidTime;
                        if (isMidnightCrossing) {
                            isValidTime = (!t.isBefore(open) || !t.isAfter(lastSlot));
                        } else {
                            isValidTime = (!t.isBefore(open) && !t.isAfter(lastSlot));
                        }

                        if (isValidTime) {
                            timeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
                        } else {
                            // If firstAvailable was already past lastSlot, this will stop immediately
                            break; 
                        }
                        
                        if (t.equals(lastSlot)) break; // Reached the end
                        
                        t = t.plusMinutes(30);
                        safetyBreaker++;
                    }

                    if (timeCombo.getItems().isEmpty()) {
                        timeCombo.setPromptText("No slots available");
                    } else {
                        timeCombo.setPromptText("Select Time");
                    }
                } else {
                    timeCombo.setPromptText("Closed Today");
                }
            }
        });
    }

    /**
     * Rounds a given time up to the next available 30-minute interval.
     * @param time The calculated buffer time.
     * @return Rounded LocalTime for the UI.
     */
    private LocalTime roundToNext30Min(LocalTime time) {
        int minutes = time.getMinute();
        if (minutes == 0) return time.withSecond(0).withNano(0);
        if (minutes <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

	@FXML
	private void onBackClicked() {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));

			Stage stage = (Stage) timeCombo.getScene().getWindow();

			stage.setScene(new Scene(root));
			stage.setTitle("Casual Customer Menu");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the creation of a new reservation. Uses session data to link the
	 * reservation to the correct customer.
	 * 
	 * @param event The button click event.
	 */
	/**
	 * Handles the creation of a new reservation. Performs local validation checks
	 * before sending the request to the server.
	 * * @param event The button click event.
	 */
	@FXML
	void onSaveOrderClicked(ActionEvent event) {
		// 1. Local Validation: Check if all fields are filled
		if (datePicker.getValue() == null || timeCombo.getValue() == null || guestsField.getText().trim().isEmpty()) {
			showErrorAlert("Input Error", "All fields must be filled out before saving.");
			return;
		}

		int diners;
		try {
			// 2. Local Validation: Check if guests count is a valid number (no letters)
			diners = Integer.parseInt(guestsField.getText().trim());
		} catch (NumberFormatException e) {
			showErrorAlert("Input Error", "Number of guests must be a numeric value.");
			return;
		}

		// 3. Local Validation: Check if number of diners is at least 1
		if (diners < 1) {
			showErrorAlert("Input Error", "The number of diners must be at least 1.");
			return;
		}

		try {
			// Gather time data
			String selectedTime = timeCombo.getValue();
			LocalTime time = LocalTime.parse(selectedTime);
			LocalDateTime startDateTime = LocalDateTime.of(datePicker.getValue(), time);

			// Per requirements, reservations are for a 2-hour duration
			LocalDateTime endDateTime = startDateTime.plusHours(2);

			// Create the entity using session data
			Reservation newRes;
			if (User_Session.getLoggedInUser() != null) {
				// User is a Subscriber
				System.out.println("AddReservation_GUI: "+User_Session.getLoggedInUser());
				newRes = new Reservation(User_Session.getLoggedInUser().getSubscriberCode(), User_Session.getLoggedInUser().getPhone(), User_Session.getLoggedInUser().getEmail(), startDateTime, endDateTime,
						diners);
			} else {
				// User is a Casual Customer
				newRes = new Reservation(null, User_Session.getCasualPhone(), User_Session.getCasualEmail(),
						startDateTime, endDateTime, diners);
			}

			// 3. Send to Client_controller 
			ConnectToServer_GUI.clientController.sendNewReservationRequest(newRes);

		} catch (Exception e) {
			System.err.println("Error creating reservation: " + e.getMessage());
		}
	}
	/**
	 * Helper method to display error alerts for local validation.
	 * @param title The title of the alert.
	 * @param content The error message content.
	 */
	private void showErrorAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
	/**
     * Displays a success alert with the confirmation code to the user.
     * @param code The confirmation code received from the server.
     */
    public void showSuccessAlert(int code) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reservation Confirmed");
        alert.setHeaderText("Success!");
        alert.setContentText("Your reservation has been saved.\nConfirmation Code: " + code);
        alert.showAndWait();
    }

    /**
     * Displays an alert when no tables are available. 
     * If a suggestion is provided, it offers the alternative time to the user.
     * @param suggested The suggested LocalDateTime, which can be null.
     */
    public void showNoTableAlert(final LocalDateTime suggested) {
        // Null check to prevent the NullPointerException
        if (suggested == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fully Booked");
            alert.setHeaderText("No Availability Found");
            alert.setContentText("We are sorry, but there are no available tables for the rest of the day.");
            alert.showAndWait();
            return;
        }

        // Runs if 'suggested' is not null
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String displayString = suggested.format(dateFormatter) + " at " + suggested.format(timeFormatter);
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("No Tables Available");
        alert.setHeaderText("The restaurant is full at your requested time.");
        alert.setContentText("The nearest available time is: " + displayString + ".\nWould you like to switch to this time?");

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
     * Updates the GUI fields with the suggested time data.
     * @param dateTime The suggested date and time to fill in.
     */
    private void updateFormFields(LocalDateTime dateTime) {
        datePicker.setValue(dateTime.toLocalDate());
        
        // Format time to match ComboBox format (HH:mm)
        String timeStr = String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
        timeCombo.setValue(timeStr);
    }
    /**
     * Provides access to the date picker component.
     * @return The DatePicker object used in the GUI.
     */
    public DatePicker getDatePicker() {
        return datePicker;
    }

}
