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
import entities.Restaurant_Table;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Controller for the New Reservation screen.
 * Handles dynamic time slot generation, date restrictions, and dark mode UI components.
 */
public class AddReservation_GUI {

    /** Static instance to allow the Client_Controller to access the active screen. */
    public static AddReservation_GUI instance;

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    
    /** Spinner replacing the old TextField for guest count. */
    @FXML private Spinner<Integer> guestsSpinner;

    /**
     * Initializes the UI components, sets up the guest spinner range, 
     * and attaches listeners for date changes.
     */
    @FXML
    public void initialize() {
        instance = this;
        restrictDatePickerRange();
        setupGuestsSpinner();

        // Set default date to Today
        datePicker.setValue(LocalDate.now());
        loadDynamicHours(LocalDate.now());

        // Listener for date selection changes
        datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) {
                if (newValue != null) {
                    loadDynamicHours(newValue);
                }
            }
        });
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
     * Configures the guests spinner with a range from 1 to the largest table capacity.
     */
    private void setupGuestsSpinner() {
        int maxCapacity = 10; // Default fallback
        
        // Dynamically find the largest table capacity in the restaurant
        List<Restaurant_Table> tables = Restaurant.getInstance().getTables();
        if (tables != null && !tables.isEmpty()) {
            for (int i = 0; i < tables.size(); i++) {
                if (tables.get(i).getSize() > maxCapacity) {
                    maxCapacity = tables.get(i).getSize();
                }
            }
        }

        // Initialize factory with min=1, max=maxCapacity, initial=2
        SpinnerValueFactory<Integer> factory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxCapacity, 2);
        guestsSpinner.setValueFactory(factory);
        
        // Ensure manual text input in the spinner is committed correctly
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
                        if (item.isBefore(minDate) || item.isAfter(maxDate)) {
                            setDisable(true);
                            // Dark mode style for disabled dates
                            setStyle("-fx-background-color: #4a4a4a; -fx-control-inner-background: #4a4a4a; -fx-text-fill: white;");
                        }
                    }
                };
            }
        });
    }

    /**
     * Dynamically populates the time ComboBox based on the restaurant's opening hours and active status.
     * @param selectedDate The date picked from the DatePicker.
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

                // Determine opening/closing times based on exceptions or regular schedule
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

                    if (selectedDate.equals(LocalDate.now())) {
                        LocalTime bufferTime = roundToNext30Min(LocalTime.now().plusHours(1));
                        boolean shouldSkipToBuffer = false;
                        if (isMidnightCrossing) {
                            if (bufferTime.isAfter(open) || bufferTime.isBefore(close)) shouldSkipToBuffer = true;
                        } else {
                            if (bufferTime.isAfter(open)) shouldSkipToBuffer = true;
                        }

                        if (shouldSkipToBuffer) firstAvailable = bufferTime;
                    }

                    LocalTime t = firstAvailable;
                    int safetyBreaker = 0; 
                    while (safetyBreaker < 48) { 
                        boolean isValidTime = isMidnightCrossing ? 
                                              (!t.isBefore(open) || !t.isAfter(lastSlot)) : 
                                              (!t.isBefore(open) && !t.isAfter(lastSlot));

                        if (isValidTime) {
                            timeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
                        } else break; 
                        
                        if (t.equals(lastSlot)) break;
                        t = t.plusMinutes(30);
                        safetyBreaker++;
                    }
                    timeCombo.setPromptText(timeCombo.getItems().isEmpty() ? "No slots available" : "Select Time");
                } else {
                    timeCombo.setPromptText("Closed Today");
                }
            }
        });
    }

    private LocalTime roundToNext30Min(LocalTime time) {
        int minutes = time.getMinute();
        if (minutes == 0) return time.withSecond(0).withNano(0);
        if (minutes <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    @FXML
    private void onBackClicked() {
        instance = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));
            Stage stage = (Stage) timeCombo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Casual Customer Menu");
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Finalizes the order and sends the reservation request to the server.
     * Preserves existing midnight-crossing adjustment logic.
     */
    @FXML
    void onSaveOrderClicked(ActionEvent event) {
        if (datePicker.getValue() == null || timeCombo.getValue() == null) {
            showErrorAlert("Input Error", "Please select a date and a time.");
            return;
        }

        // Use the value from the spinner directly
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

            // Midnight Crossing Date Adjustment
            if (openTime != null && time.isBefore(openTime)) {
                selectedDate = selectedDate.plusDays(1);
            }

            LocalDateTime startDateTime = LocalDateTime.of(selectedDate, time);
            LocalDateTime endDateTime = startDateTime.plusHours(2);

            Reservation newRes;
            if (User_Session.getLoggedInUser() != null) {
                newRes = new Reservation(User_Session.getLoggedInUser().getSubscriberCode(), 
                        User_Session.getLoggedInUser().getPhone(), 
                        User_Session.getLoggedInUser().getEmail(), 
                        startDateTime, endDateTime, diners);
            } else {
                newRes = new Reservation(null, User_Session.getCasualPhone(), 
                        User_Session.getCasualEmail(), startDateTime, endDateTime, diners);
            }

            ConnectToServer_GUI.clientController.sendNewReservationRequest(newRes);

        } catch (Exception e) {
            System.err.println("Error creating reservation: " + e.getMessage());
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reservation Confirmed");
        alert.setHeaderText("Success!");
        alert.setContentText("Your reservation code is: " + code);
        alert.showAndWait();
    }

    public void showNoTableAlert(final LocalDateTime suggested) {
        if (suggested == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fully Booked");
            alert.setContentText("No available tables for the requested date.");
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

    private void updateFormFields(LocalDateTime dateTime) {
        datePicker.setValue(dateTime.toLocalDate());
        timeCombo.setValue(String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute()));
    }

    public DatePicker getDatePicker() { return datePicker; }
}