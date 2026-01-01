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

    // Special Hours Table
    @FXML private TableView<SpecialHourRow> tableSpecialHours;
    @FXML private TableColumn<SpecialHourRow, String> colDate;
    @FXML private TableColumn<SpecialHourRow, String> colOpen;
    @FXML private TableColumn<SpecialHourRow, String> colClose;
    @FXML private TableColumn<SpecialHourRow, String> colDesc;

    // Special Hour Form
    @FXML private DatePicker dpSpecialDate;
    @FXML private ComboBox<String> cbSpecialOpen;
    @FXML private ComboBox<String> cbSpecialClose;
    @FXML private TextField txtSpecialDesc;
    @FXML private Button btnAddSpecial;
    @FXML private Button btnDeleteSpecial;
    
    // NEW BUTTON
    @FXML private Button btnMarkClosed;

    /** Map to keep track of the UI controls for each day. */
    private Map<DayOfWeek, DayRowControls> dayControlsMap = new HashMap<DayOfWeek, DayRowControls>();

    @FXML
    public void initialize() {
        instance = this;
        setupTableColumns();
        
        populateTimeComboBox(cbSpecialOpen);
        populateTimeComboBox(cbSpecialClose);
        
        // --- Listener: Enable "Mark Closed" only if date is selected ---
        dpSpecialDate.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldVal, LocalDate newVal) {
                btnMarkClosed.setDisable(newVal == null);
            }
        });

        // --- Action: Mark as Closed ---
        btnMarkClosed.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Set logic: 06:00 to 06:30 implies "Closed" logic on server side or just invalid range
                cbSpecialOpen.setValue("06:00");
                cbSpecialClose.setValue("06:30");
                txtSpecialDesc.setText("Closed");
            }
        });

        // --- 1. Batch Save Action ---
        btnSaveRegular.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onSaveBatch();
            }
        });

        // --- 2. Add Exception Action ---
        btnAddSpecial.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onAddSpecialHour();
            }
        });

        // --- 3. Delete Exception Action ---
        btnDeleteSpecial.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onDeleteSpecialHour();
            }
        });

        // --- 4. Navigation ---
        btnBack.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onBackClicked(event);
            }
        });

        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
    }

    private void populateTimeComboBox(ComboBox<String> cb) {
        cb.getItems().clear();
        for (int h = 0; h < 24; h++) {
            cb.getItems().add(String.format("%02d:00", h));
            cb.getItems().add(String.format("%02d:30", h));
        }
    }

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
        ConnectToServer_GUI.clientController.sendBatchUpdateHours(batchData);
    }

    public void refreshUI(final Opening_Hours oh) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateRegularHoursUI(oh);
                updateSpecialHoursTable(oh);
            }
        });
    }

    private void updateRegularHoursUI(Opening_Hours oh) {
        vboxRegularHours.getChildren().clear();
        dayControlsMap.clear();

        DayOfWeek[] customOrder = {
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, 
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };

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

    private void onAddSpecialHour() {
        try {
            LocalDate date = dpSpecialDate.getValue();
            if (date == null || cbSpecialOpen.getValue() == null || cbSpecialClose.getValue() == null) {
                showAlert("Missing Data", "Please fill all fields.");
                return;
            }
            LocalTime open = LocalTime.parse(cbSpecialOpen.getValue());
            LocalTime close = LocalTime.parse(cbSpecialClose.getValue());
            String desc = txtSpecialDesc.getText();

            ConnectToServer_GUI.clientController.sendAddSpecialHourRequest(date, open, close, desc);
            clearSpecialForm();
        } catch (Exception e) { showAlert("Error", "Invalid selection."); }
    }

    private void onDeleteSpecialHour() {
        SpecialHourRow selected = tableSpecialHours.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ConnectToServer_GUI.clientController.sendDeleteSpecialHourRequest(LocalDate.parse(selected.getDate()));
        } else {
            showAlert("Missing Selection", "Select a Special Hour(row) to delete.");
        }
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("date"));
        colOpen.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("open"));
        colClose.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("close"));
        colDesc.setCellValueFactory(new PropertyValueFactory<SpecialHourRow, String>("description"));
    }
    private void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void clearSpecialForm() {
        dpSpecialDate.setValue(null);
        cbSpecialOpen.setValue(null);
        cbSpecialClose.setValue(null);
        txtSpecialDesc.clear();
        btnMarkClosed.setDisable(true);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class DayRowControls {
        ComboBox<String> open, close;
        CheckBox active;
        DayRowControls(ComboBox<String> o, ComboBox<String> c, CheckBox a) {
            this.open = o; this.close = c; this.active = a;
        }
    }

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