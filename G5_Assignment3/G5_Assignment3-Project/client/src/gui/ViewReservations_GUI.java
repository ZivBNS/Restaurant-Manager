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
import messages.Message;
import messages.MessageType;

/**
 * Controller for viewing and managing user reservations.
 * Provides dynamic editing with local validation and real-time availability checks.
 */
public class ViewReservations_GUI {
    
    public static ViewReservations_GUI instance;
    public static String previousScreen;

    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, Integer> colCode;
    @FXML private TableColumn<Reservation, String> colDate, colTime;
    @FXML private TableColumn<Reservation, Integer> colGuests;

    @FXML private DatePicker editDatePicker;
    @FXML private ComboBox<String> editTimeCombo;
    @FXML private TextField editGuestsField;

    private ObservableList<Reservation> masterData = FXCollections.observableArrayList();

    /**
     * Initializes the controller class.
     * Sets up the table, date restrictions, and listeners for user interaction.
     */
    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        restrictDatePickerRange();

        // Request opening hours from the server
        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
        refreshTableData();

        // Listener for table selection using Anonymous Inner Class
        reservationsTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Reservation>() {
            @Override
            public void changed(ObservableValue<? extends Reservation> obs, Reservation oldVal, Reservation newVal) {
                if (newVal != null) {
                    populateEditFields(newVal);
                }
            }
        });

        // Listener for DatePicker changes to update available slots
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
     * Maps TableView columns to Reservation entity properties.
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
     * Populates the hours ComboBox based on DB hours.
     * Handles "Today" logic correctly to prevent showing past hours.
     * * @param selectedDate The date selected in the DatePicker.
     * @param timeToSelect Optional: A specific time string to select after loading (e.g., when editing).
     */
    public void loadDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // Clear previous state
                editTimeCombo.setValue(null);
                editTimeCombo.getItems().clear();
                
                Opening_Hours oh = Restaurant.getInstance().getOpeningHours();

                if (oh == null) {
                    ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
                    return;
                }

                LocalTime open = null;
                LocalTime close = null;

                // Determine opening/closing times (Exception vs Regular)
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

                    // --- Logic for "Today" ---
                    if (selectedDate.equals(LocalDate.now())) {
                        LocalTime now = LocalTime.now();

                        // 1. Check if we missed the last slot already (for standard shifts)
                        if (!isMidnightCrossing && now.isAfter(lastSlot)) {
                            // Restaurant effectively closed for new orders today
                            if (timeToSelect == null) return; 
                        }

                        LocalTime bufferTime = roundToNext30Min(now.plusHours(1));
                        boolean shouldSkipToBuffer = false;

                        if (isMidnightCrossing) {
                            // Logic for shifts crossing midnight (e.g. 20:00 to 02:00)
                            if (bufferTime.isAfter(open) || bufferTime.isBefore(close)) {
                                shouldSkipToBuffer = true;
                            }
                        } else {
                            // Logic for standard shifts (e.g. 08:00 to 23:00)
                            // Check if buffer wrapped around to next day
                            if (bufferTime.isBefore(now)) {
                                // Wrapped around -> definitely closed for today
                                if (timeToSelect == null) return;
                            }

                            if (bufferTime.isAfter(open)) {
                                shouldSkipToBuffer = true;
                            }
                        }

                        if (shouldSkipToBuffer) firstAvailable = bufferTime;
                    }

                    // --- Populate Time Slots ---
                    LocalTime t = firstAvailable;
                    int safety = 0;
                    while (safety < 48) {
                        boolean isValid = isMidnightCrossing ? 
                                          (!t.isBefore(open) || !t.isAfter(lastSlot)) : 
                                          (!t.isBefore(open) && !t.isAfter(lastSlot));
                        
                        if (isValid) {
                            editTimeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
                        } else {
                             // Optimization: if strictly standard shift and we hit invalid after valid, break
                             if (!isMidnightCrossing && !editTimeCombo.getItems().isEmpty()) break;
                        }
                        
                        if (t.equals(lastSlot)) break;
                        t = t.plusMinutes(30);
                        safety++;
                    }
                    
                    // Set value only after items are re-added (if specific time requested)
                    if (timeToSelect != null) {
                        // We set it even if it's not in the list (e.g. past time being viewed)
                        editTimeCombo.setValue(timeToSelect); 
                    }
                }
            }
        });
    }

    /**
     * Rounds a given time to the next 30-minute interval.
     * @param time The time to round.
     * @return The rounded LocalTime.
     */
    private LocalTime roundToNext30Min(LocalTime time) {
        int min = time.getMinute();
        if (min == 0) return time.withSecond(0).withNano(0);
        if (min <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Populates edit fields when an order is selected from the table.
     * @param res The selected Reservation object.
     */
    private void populateEditFields(Reservation res) {
        editDatePicker.setValue(res.getOrderStartTime().toLocalDate());
        String timeStr = String.format("%02d:%02d", res.getOrderStartTime().getHour(), res.getOrderStartTime().getMinute());
        
        // Load hours and pass the specific time to select so it appears in the ComboBox
        loadDynamicHours(res.getOrderStartTime().toLocalDate(), timeStr);
        editGuestsField.setText(String.valueOf(res.getNumberOfDiners()));
    }

    /**
     * Validates updated input and dynamically adjusts the reservation date for 
     * post-midnight time slots before sending the update request to the server.
     * This ensures chronological consistency if a user moves a booking to a time
     * that falls after midnight in a single business shift.
     * * @param event The action event triggered by the Update button.
     */
    @FXML
    void onUpdateClicked(ActionEvent event) {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // 1. Validation: Check for empty fields
        if (editDatePicker.getValue() == null || editTimeCombo.getValue() == null || editGuestsField.getText().trim().isEmpty()) {
            showErrorAlert("Input Error", "All fields are required for the update.");
            return;
        }

        int diners;
        try {
            // 2. Validation: Numeric check
            diners = Integer.parseInt(editGuestsField.getText().trim());
            if (diners < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showErrorAlert("Input Error", "Number of guests must be a valid positive number.");
            return;
        }

        try {
            // --- START OF DYNAMIC DATE ADJUSTMENT LOGIC ---
            
            LocalDate selectedDate = editDatePicker.getValue();
            LocalTime selectedTime = LocalTime.parse(editTimeCombo.getValue());
            
            // Fetch opening hours to determine the start of the business day
            Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
            LocalTime openTime = null;

            if (oh != null) {
                // Determine opening time for the specific day
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    openTime = oh.getExceptionSchedule().get(selectedDate).getOpenTime();
                } else {
                    openTime = oh.getRegularSchedule().get(selectedDate.getDayOfWeek()).getOpenTime();
                }
            }

            /*
             * Dynamic Midnight Fix:
             * If the selected time is chronologically BEFORE the opening time, 
             * it belongs to the following calendar day (e.g., Selected 01:00 AM 
             * when opening is 16:00 PM).
             */
            if (openTime != null && selectedTime.isBefore(openTime)) {
                selectedDate = selectedDate.plusDays(1);
            }

            LocalDateTime newStart = LocalDateTime.of(selectedDate, selectedTime);
            
            // --- END OF DYNAMIC DATE ADJUSTMENT LOGIC ---

            // Apply changes to the selected object
            selected.setOrderStartTime(newStart);
            selected.setOrderEndTime(newStart.plusHours(2)); // Standard 2-hour duration
            selected.setNumberOfDiners(diners);

            // Send updated object to server via the pipeline
            ConnectToServer_GUI.clientController.sendUpdateReservationRequest(selected);

        } catch (Exception e) {
            showErrorAlert("System Error", "Failed to process update: " + e.getMessage());
        }
    }

    /**
     * Helper method to display error alerts.
     * @param title The title of the alert.
     * @param content The message body.
     */
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Displays an alert when the requested update cannot be fulfilled due to capacity.
     * If a suggestion is provided, it updates the input fields so the user can review 
     * and manually submit the update again if they agree.
     * * @param suggested The alternative LocalDateTime suggested by the server.
     */
    public void showNoTableAlert(final LocalDateTime suggested) {
        // Ensure UI updates happen on the JavaFX Application Thread
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

                // Use showAndWait and handle the result via Anonymous Inner Class consumer
                alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
                    @Override
                    public void accept(ButtonType response) {
                        if (response == ButtonType.OK) {
                            // Only update the UI fields. 
                            // DO NOT call the update/save method here to prevent loops.
                            updateFormFields(suggested);
                        }
                    }
                });
            }
        });
    }

    /**
     * Updates the date and time selection fields in the GUI with the suggested values.
     * @param dateTime The suggested date and time to be filled into the fields.
     */
    private void updateFormFields(LocalDateTime dateTime) {
        if (editDatePicker != null) {
            editDatePicker.setValue(dateTime.toLocalDate());
        }
        
        if (editTimeCombo != null) {
            String timeStr = String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
            editTimeCombo.setValue(timeStr);
        }
    }

    /**
     * Shows a success alert when an update is confirmed.
     */
    public void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reservation updated successfully!", ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * Restricts the DatePicker to allow selection only from today's date
     * up until exactly one month in the future. Applies dark-mode styling 
     * to disabled cells to maintain visual consistency with the theme.
     */
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
                        
                        // Logic: Disable and style dates outside the [Today, Today+1Month] range
                        if (item != null && (item.isBefore(minDate) || item.isAfter(maxDate))) {
                            setDisable(true);
                            setStyle("-fx-background-color: #4a4a4a; " +
                                     "-fx-control-inner-background: #4a4a4a; " +
                                     "-fx-text-fill: white;");
                        } else {
                            // Ensure enabled dates have white text for readability against dark background
                            setStyle("-fx-text-fill: white;");
                        }
                    }
                };
            }
        });
    }

    /**
     * Refreshes the table data based on the logged-in user.
     */
    private void refreshTableData() {
        Object id = null;

        if (User_Session.getLoggedInUser() != null) {
            // Subscriber is logged in
            id = User_Session.getLoggedInUser();
        } else {
            // Casual customer: try phone, then fallback to email if implemented in your User_Session
            id = User_Session.getCasualPhone(); 
            
            if (id == null) {
                id = User_Session.getCasualEmail();
            }
            
        }

        if (id == null) {
            System.err.println("[ViewReservations] Error: No identifier found in session.");
            return;
        }

        ConnectToServer_GUI.clientController.sendGetReservationsRequest(id);
    }

    /**
     * Handles the deletion (cancellation) of a reservation.
     * @param event The button click event.
     */
    @FXML
    void onDeleteClicked(ActionEvent event) {
        final Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
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

    /**
     * Navigates back to the previous screen.
     * @param event The button click event.
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
        instance = null;
        try {
            String screen = previousScreen != null
                    ? previousScreen
                    : "/gui/CasualCustomer.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(screen));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.setTitle("Bistro");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the TableView with new data received from the server.
     * @param list The list of reservations.
     */
    public void updateTable(List<Reservation> list) {
        masterData.setAll(list);
    }
}