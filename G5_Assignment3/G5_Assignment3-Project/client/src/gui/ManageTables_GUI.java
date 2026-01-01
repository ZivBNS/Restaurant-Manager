package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import entities.Restaurant_Table;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import messages.Message;
import messages.MessageType;

public class ManageTables_GUI {

    @FXML private TableView<Restaurant_Table> tablesTable;
    @FXML private TableColumn<Restaurant_Table, Integer> colTableNumber;
    @FXML private TableColumn<Restaurant_Table, Integer> colTableSize;
    @FXML private TableColumn<Restaurant_Table, String> colActive;

    @FXML private TextField tableNumberField;
    @FXML private TextField tableSizeField;
    @FXML private CheckBox activeCheckBox;
    
    public static ManageTables_GUI instance;

    @FXML
    public void initialize() {
        instance = this;

        // הגדרת העמודות
        colTableNumber.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getTableNumber()).asObject()
        );

        colTableSize.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getSize()).asObject()
        );

        colActive.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No")
        );

        // האזנה לבחירה בטבלה למילוי השדות
        tablesTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSelection, selected) -> {
                if (selected != null) {
                    tableNumberField.setText(String.valueOf(selected.getTableNumber()));
                    tableSizeField.setText(String.valueOf(selected.getSize()));
                    activeCheckBox.setSelected(selected.isActive());
                }
            });
        
        // טעינת נתונים מהשרת
        ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.GET_ALL_TABLES, null)
        );
    }

    /**
     * מנקה את השדות ואת הבחירה בטבלה
     */
    @FXML
    private void onClearClicked() {
        tablesTable.getSelectionModel().clearSelection();
        tableNumberField.clear();
        tableSizeField.clear();
        activeCheckBox.setSelected(false);
    }

    @FXML
    private void onSaveNewClicked() {
        // --- Validation ---
        if (tableNumberField.getText().isEmpty() || tableSizeField.getText().isEmpty()) {
            showAlert("Please enter both table number and size.");
            return;
        }

        int tableNumber;
        int tableSize;
        try {
            tableNumber = Integer.parseInt(tableNumberField.getText());
            tableSize = Integer.parseInt(tableSizeField.getText());
        } catch (NumberFormatException e) {
            showAlert("Table number and size must be valid numbers.");
            return;
        }
        
        if ((tableNumber < 0) || (tableSize < 0)) {
            showAlert("Table number and size cannot be negative.");
            return;
        }

        boolean active = activeCheckBox.isSelected();

        // --- Duplicate check (CLIENT SIDE) ---
        boolean exists = tablesTable.getItems().stream()
            .anyMatch(t -> t.getTableNumber() == tableNumber);

        if (exists) {
            showAlert("Table number " + tableNumber + " already exists.");
            return;
        }

        // --- Create and send ADD request ---
        Restaurant_Table newTable = new Restaurant_Table(-1, tableNumber, tableSize, active);

        ConnectToServer_GUI.clientController.sendComplexObject(
            new Message(MessageType.ADD_TABLE_REQUEST, newTable)
        );

        onClearClicked();
    }

    @FXML
    private void onUpdateClicked() {
        Restaurant_Table selected = tablesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a table from the list to update.");
            return;
        }

        // --- Validation ---
        int tableNumber;
        int tableSize;
        try {
            tableNumber = Integer.parseInt(tableNumberField.getText());
            tableSize = Integer.parseInt(tableSizeField.getText());
        } catch (NumberFormatException e) {
            showAlert("Values must be numbers.");
            return;
        }

        boolean active = activeCheckBox.isSelected();

        // --- Duplicate check (CLIENT SIDE) ---
        // מוודא שאנחנו לא משנים מספר שולחן למספר שכבר קיים אצל שולחן אחר
        boolean existsForOther = tablesTable.getItems().stream()
            .anyMatch(t -> t.getTableNumber() == tableNumber && t.getId() != selected.getId());

        if (existsForOther) {
            showAlert("Table number " + tableNumber + " is already taken by another table.");
            return;
        }

        // --- Update object ---
        selected.setTableNumber(tableNumber);
        selected.setTableSize(tableSize);
        selected.setActive(active);

        // --- Send UPDATE request ---
        ConnectToServer_GUI.clientController.sendComplexObject(
            new Message(MessageType.UPDATE_TABLE_REQUEST, selected)
        );

        onClearClicked();
    }

    @FXML
    private void onDeleteClicked() {
        Restaurant_Table selected = tablesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a table to delete.");
            return;
        }

        int tableNumber = selected.getTableNumber();

        // אופציונלי: דיאלוג אישור מחיקה
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete Table #" + tableNumber + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() != ButtonType.YES) {
            return;
        }

        onClearClicked();

        ConnectToServer_GUI.clientController.sendComplexObject(
            new Message(MessageType.DELETE_TABLE_REQUEST, tableNumber)
        );
    }

    @FXML
    private void onBackClicked() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            Stage stage = (Stage) tablesTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    public void loadTables(List<Restaurant_Table> tables) {
        tablesTable.getItems().setAll(tables);
    }
}