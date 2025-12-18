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
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Controller for the Manual Reservation Popup used by staff.
 * Includes local input validation, UI locking, and automated availability checks.
 */
public class AddManualReservation_GUI {
    public static AddManualReservation_GUI instance; 

    @FXML private TextField mUserID, mPhone, mEmail, mGuests;
    @FXML private DatePicker mDatePicker;
    @FXML private ComboBox<String> mTimeCombo;

    @FXML
    public void initialize() {
        instance = this;
        
        mDatePicker.setValue(LocalDate.now());
        
        restrictDatePickerRange();
        
        // Request opening hours to ensure dynamic data is ready
        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
        loadManualDynamicHours(LocalDate.now(), null);

        /**
         * Listener for date changes using Anonymous Inner Class.
         * Refreshes available time slots whenever the staff changes the date.
         */
        mDatePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldV, LocalDate newV) {
                if (newV != null) {
                    loadManualDynamicHours(newV, null);
                }
            }
        });
    }

    /**
     * Restricts the calendar range to between today and exactly one month in the future.
     */
    private void restrictDatePickerRange() {
        final LocalDate minDate = LocalDate.now();
        final LocalDate maxDate = LocalDate.now().plusMonths(1);

        mDatePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(final DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && (item.isBefore(minDate) || item.isAfter(maxDate))) {
                            setDisable(true);
                            setStyle("-fx-background-color: #eeeeee;");
                        }
                    }
                };
            }
        });
    }

    /**
     * Populates hours based on the restaurant's schedule (Regular and Exceptions).
     * Synchronizes selection within Platform.runLater to avoid UI vanishing bugs.
     * @param selectedDate The chosen date.
     * @param timeToSelect Optional time string to auto-select (HH:mm).
     */
    public void loadManualDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mTimeCombo.getItems().clear();
                Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
                if (oh == null) return;

                LocalTime open = null, close = null;
                // Check for special date exceptions first, then regular weekly schedule.
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    TimeRange range = oh.getExceptionSchedule().get(selectedDate);
                    if (range != null) { open = range.getOpenTime(); close = range.getCloseTime(); }
                } else {
                    DayOfWeek day = selectedDate.getDayOfWeek();
                    if (oh.getRegularSchedule().containsKey(day)) {
                        TimeRange range = oh.getRegularSchedule().get(day);
                        if (range != null) { open = range.getOpenTime(); close = range.getCloseTime(); }
                    }
                }

                if (open != null && close != null) {
                    LocalTime firstAvailable = open;
                    LocalTime lastSlot = close.minusHours(2); // 2-hour dining window
                    boolean isMidnightCrossing = !close.isAfter(open);

                    // 1-hour buffer for same-day reservations.
                    if (selectedDate.equals(LocalDate.now())) {
                        LocalTime buffer = roundToNext30Min(LocalTime.now().plusHours(1));
                        boolean skip = isMidnightCrossing ? (buffer.isAfter(open) || buffer.isBefore(close)) : buffer.isAfter(open);
                        if (skip) firstAvailable = buffer;
                    }

                    LocalTime t = firstAvailable;
                    int safety = 0;
                    while (safety < 48) {
                        boolean isValid = isMidnightCrossing ? (!t.isBefore(open) || !t.isAfter(lastSlot)) : (!t.isBefore(open) && !t.isAfter(lastSlot));
                        if (isValid) {
                            mTimeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
                        } else break;
                        
                        if (t.equals(lastSlot)) break;
                        t = t.plusMinutes(30);
                        safety++;
                    }
                    
                    if (timeToSelect != null) {
                        mTimeCombo.setValue(timeToSelect);
                    }
                }
            }
        });
    }

    private LocalTime roundToNext30Min(LocalTime time) {
        int min = time.getMinute();
        if (min == 0) return time.withSecond(0).withNano(0);
        if (min <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Validates input locally before sending the request to the server.
     */
    @FXML
    void onSave(ActionEvent event) {
        String phone = mPhone.getText().trim();
        String email = mEmail.getText().trim();
        String guestsStr = mGuests.getText().trim();

        // 1. Validation: At least one contact method must be provided.
        if (phone.isEmpty() && email.isEmpty()) {
            showErrorAlert("Validation Error", "Please provide either a Phone number or an Email address.");
            return;
        }

        // 2. Validation: Diners and Time are mandatory.
        if (guestsStr.isEmpty() || mTimeCombo.getValue() == null) {
            showErrorAlert("Validation Error", "Number of guests and arrival time are required.");
            return;
        }

        int diners;
        try {
            // 3. Validation: Guests count must be numeric (No letters).
            diners = Integer.parseInt(guestsStr);
        } catch (NumberFormatException e) {
            showErrorAlert("Input Error", "The 'Guests' field must contain numbers only.");
            return;
        }

        // 4. Validation: Minimum 1 diner.
        if (diners < 1) {
            showErrorAlert("Validation Error", "The number of diners must be at least 1.");
            return;
        }

        try {
            Integer userId = mUserID.getText().trim().isEmpty() ? null : Integer.parseInt(mUserID.getText());
            LocalTime time = LocalTime.parse(mTimeCombo.getValue());
            LocalDateTime start = LocalDateTime.of(mDatePicker.getValue(), time);
            
            Reservation newRes = new Reservation(userId, phone, email, start, start.plusHours(2), diners);
            
            // Trigger the server's availability algorithm
            ConnectToServer_GUI.clientController.sendNewReservationRequest(newRes);
        } catch (Exception e) {
            showErrorAlert("System Error", "Failed to process input: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void showSuccessAlert(int code) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reservation Confirmed!\nCode: " + code, ButtonType.OK);
        alert.showAndWait();
        onCancel(null);
    }

    /**
     * Notifies staff if the requested slot is full and automatically 
     * updates the form with the suggested alternative.
     */
    public void showNoTableAlert(LocalDateTime suggested) {
        if (suggested != null) {
            String timeStr = suggested.format(DateTimeFormatter.ofPattern("HH:mm"));
            String msg = "Requested slot is FULL. Nearest available: " 
                         + suggested.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                         + "\n\nThe form has been updated to this suggestion.";
            
            Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
            alert.showAndWait();

            // Auto-update UI
            mDatePicker.setValue(suggested.toLocalDate());
            loadManualDynamicHours(suggested.toLocalDate(), timeStr);
        } else {
            showErrorAlert("Fully Booked", "The restaurant is fully booked for this date.");
        }
    }

    @FXML
    void onCancel(ActionEvent event) {
        instance = null;
        ((Stage) mPhone.getScene().getWindow()).close();
    }
}