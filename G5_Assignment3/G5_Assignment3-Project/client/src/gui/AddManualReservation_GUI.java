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
 * Controller for the Manual Reservation Popup used by restaurant staff.
 * Includes input validation, dynamic time slot calculation based on restaurant activity,
 * and automatic date adjustment for post-midnight bookings.
 */
public class AddManualReservation_GUI {
    
    /** Static instance to allow the Client_Controller to update the UI upon server response. */
    public static AddManualReservation_GUI instance; 

    @FXML private TextField mUserID, mPhone, mEmail, mGuests;
    @FXML private DatePicker mDatePicker;
    @FXML private ComboBox<String> mTimeCombo;

    /**
     * Initializes the controller, restricts the date picker range, 
     * and sets up listeners for user input.
     */
    @FXML
    public void initialize() {
        instance = this;
        
        mDatePicker.setValue(LocalDate.now());
        restrictDatePickerRange();
        
        // Ensure opening hours data is fetched from the server
        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
        
        // Initial load for today's hours
        loadManualDynamicHours(LocalDate.now(), null);

        /**
         * Listener for date changes. 
         * Refreshes available time slots whenever a new date is picked.
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
     * Restricts the DatePicker to only allow dates from today up to one month in the future.
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
                            setStyle("-fx-background-color: #eeeeee;"); // Gray out disabled cells
                        }
                    }
                };
            }
        });
    }

    /**
     * Populates the time ComboBox based on the restaurant's operational status.
     * FIX: Now correctly checks the 'isActive' status from the database/entity.
     * @param selectedDate The date selected by the staff member.
     * @param timeToSelect Optional time string to pre-select.
     */
    public void loadManualDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mTimeCombo.getItems().clear();
                Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
                if (oh == null) return;

                LocalTime open = null, close = null;
                
                // 1. Determine if the selected date is active and retrieve times
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    TimeRange range = oh.getExceptionSchedule().get(selectedDate);
                    // Only assign times if the exception day is active
                    if (range != null && range.isActive()) { 
                        open = range.getOpenTime(); 
                        close = range.getCloseTime(); 
                    }
                } else {
                    DayOfWeek day = selectedDate.getDayOfWeek();
                    if (oh.getRegularSchedule().containsKey(day)) {
                        TimeRange range = oh.getRegularSchedule().get(day);
                        // CRITICAL FIX: Only assign times if the regular day is active
                        if (range != null && range.isActive()) { 
                            open = range.getOpenTime(); 
                            close = range.getCloseTime(); 
                        }
                    }
                }

                // 2. Fill the ComboBox if the restaurant is active/open
                if (open != null && close != null) {
                    LocalTime firstAvailable = open;
                    LocalTime lastSlot = close.minusHours(2); // 2-hour window required for dining
                    boolean isMidnightCrossing = !close.isAfter(open);

                    // Apply 1-hour buffer for same-day bookings
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
                    mTimeCombo.setPromptText("Select Time");
                } else {
                    // If isActive was 0, prompt "Closed Today" and prevent selection
                    mTimeCombo.setPromptText("Closed Today");
                }
            }
        });
    }

    /**
     * Rounds the current time up to the nearest 30-minute interval for booking buffers.
     */
    private LocalTime roundToNext30Min(LocalTime time) {
        int min = time.getMinute();
        if (min == 0) return time.withSecond(0).withNano(0);
        if (min <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Validates input and calculates the finalized LocalDateTime, 
     * adjusting for midnight-crossing shifts.
     */
    @FXML
    void onSave(ActionEvent event) {
        String phone = mPhone.getText().trim();
        String email = mEmail.getText().trim();
        String guestsStr = mGuests.getText().trim();

        if ((phone.isEmpty() && email.isEmpty()) || guestsStr.isEmpty() || 
            mDatePicker.getValue() == null || mTimeCombo.getValue() == null) {
            showErrorAlert("Validation Error", "Mandatory fields are missing.");
            return;
        }

        int diners;
        try {
            diners = Integer.parseInt(guestsStr);
            if (diners < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showErrorAlert("Input Error", "Please enter a valid guest count.");
            return;
        }

        try {
            Integer userId = mUserID.getText().trim().isEmpty() ? null : Integer.parseInt(mUserID.getText());
            LocalDate selectedDate = mDatePicker.getValue();
            LocalTime selectedTime = LocalTime.parse(mTimeCombo.getValue());
            
            Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
            LocalTime openTime = null;

            if (oh != null) {
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    openTime = oh.getExceptionSchedule().get(selectedDate).getOpenTime();
                } else {
                    openTime = oh.getRegularSchedule().get(selectedDate.getDayOfWeek()).getOpenTime();
                }
            }

            // Adjust calendar day if the selected time falls after midnight in the business shift
            if (openTime != null && selectedTime.isBefore(openTime)) {
                selectedDate = selectedDate.plusDays(1);
            }

            LocalDateTime start = LocalDateTime.of(selectedDate, selectedTime);
            Reservation newRes = new Reservation(userId, phone, email, start, start.plusHours(2), diners);
            
            ConnectToServer_GUI.clientController.sendNewReservationRequest(newRes);
            
        } catch (NumberFormatException e) {
            showErrorAlert("Input Error", "User ID must be numeric.");
        } catch (Exception e) {
            showErrorAlert("System Error", "Failed to process reservation: " + e.getMessage());
        }
    }

    /** Displays a general error alert. */
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /** Notifies staff of a successful reservation and closes the popup. */
    public void showSuccessAlert(int code) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reservation Confirmed!\nCode: " + code, ButtonType.OK);
        alert.showAndWait();
        onCancel(null);
    }

    /**
     * Handles full booking scenarios by showing a suggested alternative time.
     */
    public void showNoTableAlert(LocalDateTime suggested) {
        if (suggested != null) {
            String timeStr = suggested.format(DateTimeFormatter.ofPattern("HH:mm"));
            String msg = "Slot FULL. Nearest available: " 
                         + suggested.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                         + "\n\nUpdating form to suggestion...";
            
            Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
            alert.showAndWait();

            mDatePicker.setValue(suggested.toLocalDate());
            loadManualDynamicHours(suggested.toLocalDate(), timeStr);
        } else {
            showErrorAlert("Fully Booked", "No availability for this date.");
        }
    }

    /** Resets the instance and closes the window. */
    @FXML
    void onCancel(ActionEvent event) {
        instance = null;
        ((Stage) mPhone.getScene().getWindow()).close();
    }
}