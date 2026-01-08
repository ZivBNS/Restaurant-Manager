package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import java.util.List;
import entities.Restaurant_Table;

/**
 * Controller class for the Manage Tables screen.
 * Handles the creation, update, deletion, and viewing of restaurant tables.
 */
public class ManageTables_GUI {

    @FXML
    private TableView<Restaurant_Table> tablesTable;
    @FXML
    private TableColumn<Restaurant_Table, Integer> colTableNumber;
    @FXML
    private TableColumn<Restaurant_Table, Integer> colTableSize;
    @FXML
    private TableColumn<Restaurant_Table, String> colActive;

    @FXML
    private TextField tableNumberField;
    @FXML
    private TextField tableSizeField;
    @FXML
    private CheckBox activeCheckBox;

    public static ManageTables_GUI instance;

    /**
     * Initializes the controller class.
     * Sets up the table columns, selection listeners, and fetches initial data.
     */
    @FXML
    public void initialize() {
        instance = this;

        // --- Column Setup (Using Anonymous Inner Classes instead of Lambdas) ---

        // Table Number Column
        colTableNumber.setCellValueFactory(new Callback<CellDataFeatures<Restaurant_Table, Integer>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(CellDataFeatures<Restaurant_Table, Integer> param) {
                return new SimpleIntegerProperty(param.getValue().getTableNumber()).asObject();
            }
        });

        // Table Size Column
        colTableSize.setCellValueFactory(new Callback<CellDataFeatures<Restaurant_Table, Integer>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(CellDataFeatures<Restaurant_Table, Integer> param) {
                return new SimpleIntegerProperty(param.getValue().getSize()).asObject();
            }
        });

        // Active Status Column
        colActive.setCellValueFactory(new Callback<CellDataFeatures<Restaurant_Table, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Restaurant_Table, String> param) {
                return new SimpleStringProperty(param.getValue().isActive() ? "Yes" : "No");
            }
        });

        // --- Table Selection Listener ---
        tablesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Restaurant_Table>() {
            @Override
            public void changed(ObservableValue<? extends Restaurant_Table> observable, Restaurant_Table oldValue, Restaurant_Table newValue) {
                if (newValue != null) {
                    tableNumberField.setText(String.valueOf(newValue.getTableNumber()));
                    tableSizeField.setText(String.valueOf(newValue.getSize()));
                    activeCheckBox.setSelected(newValue.isActive());
                }
            }
        });

        // --- Initial Data Load ---
        refreshData();
    }

    /**
     * Sends a request to the server to get the current list of tables.
     * Uses the centralized method in Client_Controller.
     */
    private void refreshData() {
        ConnectToServer_GUI.clientController.sendGetAllTablesRequest();
    }

    /**
     * Event handler for the Refresh button.
     * Reloads data from the server and clears the form.
     */
    @FXML
    private void onRefreshClicked() {
        refreshData();
        onClearClicked();
    }

    /**
     * Clears all input fields and deselects any table from the TableView.
     */
    @FXML
    private void onClearClicked() {
        tablesTable.getSelectionModel().clearSelection();
        tableNumberField.clear();
        tableSizeField.clear();
        activeCheckBox.setSelected(false);
    }

    /**
     * Handles the creation of a new table.
     * Validates input, checks for duplicates locally, and sends a request to the server.
     */
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

        // --- Duplicate check (CLIENT SIDE - Standard Loop) ---
        boolean exists = false;
        for (Restaurant_Table t : tablesTable.getItems()) {
            if (t.getTableNumber() == tableNumber) {
                exists = true;
                break;
            }
        }

        if (exists) {
            showAlert("Table number " + tableNumber + " already exists.");
            return;
        }

        // --- Create Object and Send Request ---
        Restaurant_Table newTable = new Restaurant_Table(-1, tableNumber, tableSize, active);

        // Uses the centralized method in Client_Controller
        ConnectToServer_GUI.clientController.sendAddTableRequest(newTable);

        onClearClicked();
        refreshData();
    }

    /**
     * Handles the update of an existing table.
     * Validates input and ensures table number uniqueness.
     */
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

        // --- Duplicate check (CLIENT SIDE - Standard Loop) ---
        // Ensure we don't accidentally assign a Table Number that belongs to ANOTHER table
        boolean existsForOther = false;
        for (Restaurant_Table t : tablesTable.getItems()) {
            if (t.getTableNumber() == tableNumber && t.getId() != selected.getId()) {
                existsForOther = true;
                break;
            }
        }

        if (existsForOther) {
            showAlert("Table number " + tableNumber + " is already taken by another table.");
            return;
        }

        // --- Update Object ---
        selected.setTableNumber(tableNumber);
        selected.setTableSize(tableSize);
        selected.setActive(active);

        // --- Send Request ---
        // Uses the centralized method in Client_Controller
        ConnectToServer_GUI.clientController.sendUpdateTableRequest(selected);

        onClearClicked();
        refreshData();
    }

    /**
     * Handles the deletion of the selected table.
     */
    @FXML
    private void onDeleteClicked() {
        Restaurant_Table selected = tablesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a table to delete.");
            return;
        }

        int tableNumber = selected.getTableNumber();

        // Optional: Confirmation Dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete Table #" + tableNumber + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() != ButtonType.YES) {
            return;
        }

        onClearClicked();

        // Uses the centralized method in Client_Controller
        ConnectToServer_GUI.clientController.sendDeleteTableRequest(tableNumber);
        
        refreshData();
    }

    /**
     * Navigates back to the Workers Main Dashboard.
     * Ensures the window remains maximized if previously set.
     * @param event The action event triggered by the back button.
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
    	try {
			Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    /**
     * Displays an error alert to the user.
     * @param msg The message to display.
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    /**
     * Updates the TableView with a new list of tables.
     * Called by the ClientController when a response is received from the server.
     * @param tables The list of tables to display.
     */
    public void loadTables(List<Restaurant_Table> tables) {
        tablesTable.getItems().setAll(tables);
    }
}