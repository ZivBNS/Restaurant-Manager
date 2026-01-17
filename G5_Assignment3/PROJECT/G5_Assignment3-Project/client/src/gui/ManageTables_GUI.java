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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import java.util.List;
import entities.Restaurant_Table;

/**
 * Controller class for the Manage Tables screen. Handles the creation, update,
 * deletion, and viewing of restaurant tables. GUI Update: Removed Active status
 * column and checkbox (Soft delete logic will be server-side).
 */
public class ManageTables_GUI {

	@FXML
	private TableView<Restaurant_Table> tablesTable;
	@FXML
	private TableColumn<Restaurant_Table, Integer> colTableNumber;
	@FXML
	private TableColumn<Restaurant_Table, Integer> colTableSize;
	@FXML
	private TextField tableNumberField;
	@FXML
	private TextField tableSizeField;
	@FXML
	private Button btnSaveNew;
	@FXML
	private Button btnUpdate;
	@FXML
	private Button btnDelete;
	@FXML
	private Button btnClear;

	public static ManageTables_GUI instance;

	/**
	 * Initializes the controller class. Sets up the table columns, selection
	 * listeners, and fetches initial data.
	 */
	@FXML
	public void initialize() {
		instance = this;

		// --- Column Setup ---

		// Table Number Column
		colTableNumber.setCellValueFactory(
				new Callback<CellDataFeatures<Restaurant_Table, Integer>, ObservableValue<Integer>>() {
					@Override
					public ObservableValue<Integer> call(CellDataFeatures<Restaurant_Table, Integer> param) {
						return new SimpleIntegerProperty(param.getValue().getTableNumber()).asObject();
					}
				});

		// Table Size Column
		colTableSize.setCellValueFactory(
				new Callback<CellDataFeatures<Restaurant_Table, Integer>, ObservableValue<Integer>>() {
					@Override
					public ObservableValue<Integer> call(CellDataFeatures<Restaurant_Table, Integer> param) {
						return new SimpleIntegerProperty(param.getValue().getSize()).asObject();
					}
				});

		// --- Table Selection Listener ---
		tablesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Restaurant_Table>() {
			@Override
			public void changed(ObservableValue<? extends Restaurant_Table> observable, Restaurant_Table oldValue,
					Restaurant_Table newValue) {
				if (newValue != null) {
					tableNumberField.setText(String.valueOf(newValue.getTableNumber()));
					tableSizeField.setText(String.valueOf(newValue.getSize()));
					setEditMode(true);
				}
			}
		});

		// --- Initial Data Load ---
		setEditMode(false);
		refreshData();
	}

	/**
	 * Sets the GUI to either Edit mode or New Entry mode. In Edit mode, Update and
	 * Delete buttons are enabled, Save New is disabled. In New Entry mode, Save New
	 * is enabled, Update and Delete are disabled.
	 * 
	 * @param isEdit true for Edit mode, false for New Entry mode.
	 */
	private void setEditMode(boolean isEdit) {
		if (btnSaveNew != null) {
			btnSaveNew.setDisable(isEdit);
		}
		if (btnUpdate != null) {
			btnUpdate.setDisable(!isEdit);
		}
		if (btnDelete != null) {
			btnDelete.setDisable(!isEdit);
		}
	}

	/**
	 * Sends a request to the server to get the current list of tables.
	 */
	private void refreshData() {
		ConnectToServer_GUI.clientController.sendGetAllTablesRequest();
	}

	/**
	 * Event handler for the Refresh button.
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
		setEditMode(false);
	}

	/**
	 * Handles the creation of a new table.
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
		// New tables are ALWAYS active by default (true)
		Restaurant_Table newTable = new Restaurant_Table(-1, tableNumber, tableSize, true);

		ConnectToServer_GUI.clientController.sendAddTableRequest(newTable);

		onClearClicked();
		refreshData();
	}

	/**
	 * Handles the update of an existing table.
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
		// Maintain existing active status (usually true if it's visible here)
		selected.setActive(selected.isActive());

		// --- Send Request ---
		ConnectToServer_GUI.clientController.sendUpdateTableRequest(selected);

		onClearClicked();
		refreshData();
	}

	/**
	 * Handles the deletion of the selected table. Note: Server will handle if this
	 * is a "Soft Delete" (marking inactive) or "Hard Delete".
	 */
	@FXML
	private void onDeleteClicked() {
		Restaurant_Table selected = tablesTable.getSelectionModel().getSelectedItem();

		if (selected == null) {
			showAlert("Please select a table to delete.");
			return;
		}

		int tableNumber = selected.getTableNumber();

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
				"Are you sure you want to delete Table #" + tableNumber + "?", ButtonType.YES, ButtonType.NO);
		confirm.showAndWait();
		if (confirm.getResult() != ButtonType.YES) {
			return;
		}

		onClearClicked();

		ConnectToServer_GUI.clientController.sendDeleteTableRequest(tableNumber);

		refreshData();
	}

	/**
	 * Handles the action when the back button is clicked. Navigates back to the
	 * Workers screen.
	 */
	@FXML
	private void onBackClicked(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
			stage.centerOnScreen();
			instance = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Displays an alert dialog with the given message.
	 * 
	 * @param msg The message to display in the alert dialog.
	 */
	private void showAlert(String msg) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

	/**
	 * Loads the list of tables into the TableView.
	 * 
	 * @param tables The list of Restaurant_Table objects to display.
	 */
	public void loadTables(List<Restaurant_Table> tables) {
		tablesTable.getItems().setAll(tables);
	}
}