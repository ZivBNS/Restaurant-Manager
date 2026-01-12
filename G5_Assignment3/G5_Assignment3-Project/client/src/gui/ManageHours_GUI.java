package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import entities.Opening_Hours;
import entities.Opening_Hours.TimeRange;

/**
 * Controller for the Opening Hours Management screen.
 * Provides a GUI for batch updating weekly schedules and managing special exceptions.
 */
public class ManageHours_GUI {

    public static ManageHours_GUI instance;

    @FXML private VBox vboxRegularHours;
    @FXML private Button btnSaveRegular;
    @FXML private Button btnBack;

    @FXML private TableView<SpecialHourRow> tableSpecialHours;
    @FXML private TableColumn<SpecialHourRow, String> colDate;
    @FXML private TableColumn<SpecialHourRow, String> colOpen;
    @FXML private TableColumn<SpecialHourRow, String> colClose;
    @FXML private TableColumn<SpecialHourRow, String> colDesc;

    @FXML private DatePicker dpSpecialDate;
    @FXML private ComboBox<String> cbSpecialOpen;
    @FXML private ComboBox<String> cbSpecialClose;
    @FXML private TextField txtSpecialDesc;
    @FXML private Button btnAddSpecial;
    @FXML private Button btnDeleteSpecial;
    @FXML private Button btnMarkClosed;

    private Map<DayOfWeek, DayRowControls> dayControlsMap = new HashMap<DayOfWeek, DayRowControls>();
    
    /** Flag to distinguish between initial load and user updates */
    private boolean updatePending = false;

    @FXML
    public void initialize() {
        instance = this;
        setupTableColumns();
        
        populateTimeComboBox(cbSpecialOpen);
        populateTimeComboBox(cbSpecialClose);
        
        dpSpecialDate.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldVal, LocalDate newVal) {
                btnMarkClosed.setDisable(newVal == null);
            }
        });

        btnMarkClosed.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                cbSpecialOpen.setValue("06:00");
                cbSpecialClose.setValue("06:30");
                txtSpecialDesc.setText("Closed");
            }
        });

        btnSaveRegular.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onSaveBatch();
            }
        });

        btnAddSpecial.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onAddSpecialHour();
            }
        });

        btnDeleteSpecial.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onDeleteSpecialHour();
            }
        });

        btnBack.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onBackClicked(event);
            }
        });

        // This request will NOT trigger a success alert because updatePending is false
        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
    }
    
    /**
     * Sets the flag indicating a user-initiated update is in progress.
     * @param pending true if waiting for server response after an action.
     */
    public void setUpdatePending(boolean pending) {
        this.updatePending = pending;
    }

    public boolean isUpdatePending() {
        return updatePending;
    }

    private void populateTimeComboBox(ComboBox<String> cb) {
        cb.getItems().clear();
        for (int h = 0; h < 24; h++) {
            cb.getItems().add(String.format("%02d:00", h));
            cb.getItems().add(String.format("%02d:30", h));
        }
    }
    
    /**
     * Gathers the batch update data from the UI and sends it to the server.
     * Validates input before sending.
     */
    private void onSaveBatch() {
        Map<DayOfWeek, Object[]> batchData = new HashMap<DayOfWeek, Object[]>();
        for (Map.Entry<DayOfWeek, DayRowControls> entry : dayControlsMap.entrySet()) {
            DayRowControls controls = entry.getValue();
            try {
                LocalTime open = LocalTime.parse(controls.open.getValue());
                LocalTime close = LocalTime.parse(controls.close.getValue());
                boolean active = controls.active.isSelected();
                batchData.put(entry.getKey(), new Object[]{ open, close, active });
            } catch (Exception e) {
                showAlert("Error", "Please select times for " + entry.getKey());
                return;
            }
        }
        // Set flag before sending
        this.updatePending = true;
        ConnectToServer_GUI.clientController.sendBatchUpdateHours(batchData);
    }
    
    /**
     * Refreshes the UI with the latest Opening_Hours data. 
     * @param oh The Opening_Hours object containing the latest schedule data.
     */
    public void refreshUI(final Opening_Hours oh) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateRegularHoursUI(oh);
                updateSpecialHoursTable(oh);
            }
        });
    }
    
    /**
     * Updates the regular hours section of the UI.
     * @param oh The Opening_Hours object containing the regular schedule.
     */
    private void updateRegularHoursUI(Opening_Hours oh) {
        vboxRegularHours.getChildren().clear();
        dayControlsMap.clear();

        DayOfWeek[] customOrder = {
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, 
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };

        // Create rows for each day in custom order
        for (final DayOfWeek day : customOrder) {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 5;");

            Label lblDay = new Label(day.toString());
            lblDay.setPrefWidth(100);
            lblDay.setStyle("-fx-text-fill: white;");

            final ComboBox<String> cbOpen = new ComboBox<String>();
            final ComboBox<String> cbClose = new ComboBox<String>();
            populateTimeComboBox(cbOpen);
            populateTimeComboBox(cbClose);
            cbOpen.setPrefWidth(120);
            cbClose.setPrefWidth(120);

            final CheckBox cbActive = new CheckBox();
            cbActive.setStyle("-fx-mark-color: #3498db;");

            TimeRange range = oh.getRegularSchedule().get(day);
            if (range != null) {
                cbOpen.setValue(range.getOpenTime().toString());
                cbClose.setValue(range.getCloseTime().toString());
                cbActive.setSelected(range.isActive());
                cbOpen.setDisable(!range.isActive());
                cbClose.setDisable(!range.isActive());
            }

            cbActive.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                    cbOpen.setDisable(!newVal);
                    cbClose.setDisable(!newVal);
                }
            });

            row.getChildren().addAll(lblDay, cbOpen, cbClose, cbActive);
            vboxRegularHours.getChildren().add(row);
            dayControlsMap.put(day, new DayRowControls(cbOpen, cbClose, cbActive));
        }
    }
    
    /**
	 * Updates the special hours table with the latest exception schedule.
	 * @param oh The Opening_Hours object containing the exception schedule.
	 */
    private void updateSpecialHoursTable(Opening_Hours oh) {
        ObservableList<SpecialHourRow> specialRows = FXCollections.observableArrayList();
        for (Map.Entry<LocalDate, TimeRange> entry : oh.getExceptionSchedule().entrySet()) {
            TimeRange range = entry.getValue();
            specialRows.add(new SpecialHourRow(
                entry.getKey().toString(),
                range.getOpenTime() != null ? range.getOpenTime().toString() : "Closed",
                range.getCloseTime() != null ? range.getCloseTime().toString() : "N/A",
                range.getDescription() != null ? range.getDescription() : "Manual Override"
            ));
        }
        tableSpecialHours.setItems(specialRows);
    }
    
    /**
     * Gathers the special hour data from the UI and sends an add request to the server.
	 * Validates input and checks for duplicate dates before sending.
     */
    private void onAddSpecialHour() {
        try {
            LocalDate date = dpSpecialDate.getValue();
            if (date == null || cbSpecialOpen.getValue() == null || cbSpecialClose.getValue() == null) {
                showAlert("Missing Data", "Please fill all fields.");
                return;
            }

            // Check if date already exists
            String dateString = date.toString();
            for (SpecialHourRow row : tableSpecialHours.getItems()) {
                if (row.getDate().equals(dateString)) {
                    showAlert("Duplicate Date", "Error: You can only have one special schedule per date.\nPlease delete the existing entry first.");
                    return;
                }
            }

            LocalTime open = LocalTime.parse(cbSpecialOpen.getValue());
            LocalTime close = LocalTime.parse(cbSpecialClose.getValue());
            String desc = txtSpecialDesc.getText();

            // Set flag before sending
            this.updatePending = true;
            ConnectToServer_GUI.clientController.sendAddSpecialHourRequest(date, open, close, desc);
            clearSpecialForm();
        } catch (Exception e) { 
            showAlert("Error", "Invalid selection."); 
        }
    }
    
    /**
     * Gathers the selected special hour from the table and sends a delete request to the server.
     * Validates selection before sending.
     */
    private void onDeleteSpecialHour() {
        SpecialHourRow selected = tableSpecialHours.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Set flag before sending
            this.updatePending = true;
            ConnectToServer_GUI.clientController.sendDeleteSpecialHourRequest(LocalDate.parse(selected.getDate()));
        } else {
            showAlert("Missing Selection", "Select a Special Hour(row) to delete.");
        }
    }
    
    /**
     * Configures the table columns to map to SpecialHourRow properties.
     */
    private void setupTableColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("date"));
        colOpen.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("open"));
        colClose.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("close"));
        colDesc.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("description"));
    }
    
    /**
     * Handles the action when the back button is clicked.
     * @param event The action event triggered by clicking the back button.
     */
    private void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        	instance = null;
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    /**
     * Clears the special hour input form.
     */
    private void clearSpecialForm() {
        dpSpecialDate.setValue(null);
        cbSpecialOpen.setValue(null);
        cbSpecialClose.setValue(null);
        txtSpecialDesc.clear();
        btnMarkClosed.setDisable(true);
    }
    
    /**
     * Displays an alert dialog with the given title and content.
     * @param title The title of the alert dialog.
     * @param content The content message of the alert dialog.
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Helper class to group controls for each day's row.
     * Contains ComboBoxes for open/close times and a CheckBox for active status.
     */
    private static class DayRowControls {
        ComboBox<String> open, close;
        CheckBox active;
        DayRowControls(ComboBox<String> o, ComboBox<String> c, CheckBox a) {
            this.open = o; this.close = c; this.active = a;
        }
    }
    
    /**
	 * Data model for a row in the Special Hours table.
	 * Contains date, open time, close time, and description.
	 * Used for TableView display and data binding.
	 */
    public static class SpecialHourRow {
        private String date, open, close, description;
        public SpecialHourRow(String d, String o, String c, String desc) {
            this.date = d; this.open = o; this.close = c; this.description = desc;
        }
        public String getDate() { return date; }
        public String getOpen() { return open; }
        public String getClose() { return close; }
        public String getDescription() { return description; }
    }
}