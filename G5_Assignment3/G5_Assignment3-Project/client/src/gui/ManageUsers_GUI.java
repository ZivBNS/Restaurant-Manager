package gui;

import java.util.List;

import entities.UserRecord;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import messages.Message;
import messages.MessageType;

public class ManageUsers_GUI {

	// --- FXML UI Bindings ---
	// searchField Removed
	@FXML
	private TableView<UserRecord> usersTable;
	@FXML
	private TableColumn<UserRecord, Integer> colId;
	@FXML
	private TableColumn<UserRecord, String> colFullName;
	@FXML
	private TableColumn<UserRecord, String> colPhone;
	@FXML
	private TableColumn<UserRecord, String> colEmail;
	@FXML
	private TableColumn<UserRecord, String> colUsername;
	@FXML
	private TableColumn<UserRecord, Integer> colSubscriberCode;
	@FXML
	private TableColumn<UserRecord, String> colIdentity;

	@FXML
	private TextField idField, firstNameField, lastNameField, phoneField, emailField, usernameField,
			subscriberCodeField;
	@FXML
	private PasswordField passwordField;
	@FXML
	private ComboBox<String> identityCombo;
	@FXML
	private Label formErrorLabel;

	// Updated Button references for 2x2 grid
	@FXML
	private Button deleteBtn, backBtn, saveBtn, updateBtn;

	// --- Local Data Model ---
	private final ObservableList<UserRecord> masterList = FXCollections.observableArrayList();
	private UserRecord selectedUser = null;

	public static ManageUsers_GUI instance;

	@FXML
	public void initialize() {
		instance = this;
		ConnectToServer_GUI.clientController.setManageUsersGUI(this);

		// Define Columns
		colId.setCellValueFactory(new PropertyValueFactory<>("id"));
		colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
		colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
		colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
		colSubscriberCode.setCellValueFactory(new PropertyValueFactory<>("subscriberCode"));
		colIdentity.setCellValueFactory(new PropertyValueFactory<>("identity"));

		colFullName.setCellValueFactory(new Callback<CellDataFeatures<UserRecord, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<UserRecord, String> cell) {
				UserRecord u = cell.getValue();
				return new SimpleStringProperty(u.getFirstName() + " " + u.getLastName());
			}
		});

		usersTable.setItems(masterList);

		// Selection Listener - Controls button states
		usersTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<UserRecord>() {
			@Override
			public void changed(ObservableValue<? extends UserRecord> obs, UserRecord oldV, UserRecord newV) {
				selectedUser = newV;
				if (newV == null) {
					// No Selection -> "Add Mode"
					clearForm();
					saveBtn.setDisable(false); // Can add
					updateBtn.setDisable(true); // Can't update
					deleteBtn.setDisable(true); // Can't delete
				} else {
					// Selection Exists -> "Edit Mode"
					fillForm(newV);
					saveBtn.setDisable(true); // Can't add new over existing selection
					updateBtn.setDisable(false); // Can update
					deleteBtn.setDisable(false); // Can delete
				}
			}
		});

		// Initialize Roles
		identityCombo.setItems(FXCollections.observableArrayList("Subscriber", "Employee", "Manager"));
		identityCombo.getSelectionModel().select("Subscriber");

		// Initial State
		saveBtn.setDisable(false);
		updateBtn.setDisable(true);
		deleteBtn.setDisable(true);

		refreshData();
	}

	public void handle(Message msg) {
		try {
			switch (msg.getType()) {
			case MessageType.GET_ALL_USERS_RESPONSE:
				@SuppressWarnings("unchecked")
				List<UserRecord> list = (List<UserRecord>) msg.getContent();
				masterList.setAll(list);
				break;
			case MessageType.ADD_USER_RESPONSE_OK:
				showError("User Added Successfully.");
				refreshData();
				onClear(); // Reset to add mode
				break;
			case MessageType.EDIT_USER_RESPONSE_OK:
				showError("User Updated Successfully.");
				refreshData();
				break;
			case MessageType.ADD_USER_RESPONSE_ERR:
			case MessageType.EDIT_USER_RESPONSE_ERR:
				showError((String) msg.getContent());
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void refreshData() {
		ConnectToServer_GUI.clientController.sendGetAllUsersRequest();
	}

	@FXML
	private void onRefresh() {
		refreshData();
	}

	/**
	 * Handles both Save (New) and Update based on selection state.
	 */
	@FXML
	private void onSave() {
		formErrorLabel.setVisible(false);
		clearAllFieldStyles();

		String fName = firstNameField.getText().trim();
		String lName = lastNameField.getText().trim();
		String user = usernameField.getText().trim();
		String pass = passwordField.getText();
		String role = identityCombo.getValue();

		// Validation
		if (fName.isEmpty() || lName.isEmpty() || user.isEmpty()) {
			showError("First Name, Last Name, and Username are required.");
			return;
		}

		if (!isValidName(fName)) {
			markInvalid(firstNameField);
			showError("Invalid first name");
			return;
		}
		if (!isValidName(lName)) {
			markInvalid(lastNameField);
			showError("Invalid last name");
			return;
		}
		if (!isValidPhone(phoneField.getText())) {
			markInvalid(phoneField);
			showError("Invalid phone number");
			return;
		}
		if (!isValidEmail(emailField.getText())) {
			markInvalid(emailField);
			showError("Invalid email address");
			return;
		}
		if (!isValidUsername(user)) {
			markInvalid(usernameField);
			showError("Invalid username");
			return;
		}

		if (role == null) {
			showError("Role is required for a user.");
			return;
		}

		if (selectedUser == null) {
			// --- CREATE NEW USER ---
			if (pass.isEmpty()) {
				showError("Password required for new user.");
				markInvalid(passwordField);
				return;
			}
			UserRecord newUser = new UserRecord(0, fName, lName, phoneField.getText(), emailField.getText(), user, pass,
					role, null);
			ConnectToServer_GUI.clientController.sendAddUserRequest(newUser);
		} else {
			// --- UPDATE EXISTING USER ---
			// If password field is empty, keep the old password
			String finalPass = pass.isEmpty() ? selectedUser.getPassword() : pass;
			UserRecord updated = new UserRecord(selectedUser.getId(), fName, lName, phoneField.getText(),
					emailField.getText(), user, finalPass, role, selectedUser.getSubscriberCode());
			ConnectToServer_GUI.clientController.sendEditUserRequest(updated);
		}
	}

	@FXML
	private void onDelete() {
		if (selectedUser != null) {
			ConnectToServer_GUI.clientController.sendRemoveUserRequest(selectedUser);
			onClear(); // Reset form and buttons
			refreshData();
		}
	}

	@FXML
	private void onClear() {
		usersTable.getSelectionModel().clearSelection();
		selectedUser = null;
		clearAllFieldStyles();
		clearForm();

		// Reset Button States manually (though listener handles it too)
		saveBtn.setDisable(false);
		updateBtn.setDisable(true);
		deleteBtn.setDisable(true);
	}

	@FXML
	private void onBack(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fillForm(UserRecord u) {
		idField.setText(String.valueOf(u.getId()));
		firstNameField.setText(u.getFirstName());
		lastNameField.setText(u.getLastName());
		phoneField.setText(u.getPhone());
		emailField.setText(u.getEmail());
		usernameField.setText(u.getUsername());
		passwordField.clear(); // Security: Don't show password
		subscriberCodeField.setText(u.getSubscriberCode() != null ? String.valueOf(u.getSubscriberCode()) : "N/A");
		identityCombo.getSelectionModel().select(u.getIdentity());
	}

	private void clearForm() {
		idField.clear();
		firstNameField.clear();
		lastNameField.clear();
		phoneField.clear();
		emailField.clear();
		usernameField.clear();
		passwordField.clear();
		subscriberCodeField.clear();
		formErrorLabel.setVisible(false);
	}

	private void showError(String msg) {
		formErrorLabel.setText(msg);
		formErrorLabel.setVisible(true);
	}

	// Validation Helpers
	private boolean isValidEmail(String email) {
		return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}

	private boolean isValidName(String name) {
		return name != null && !name.trim().isEmpty() && name.matches("[A-Za-z ]+");
	}

	private boolean isValidPhone(String phone) {
		return phone != null && phone.matches("\\d{9,10}");
	}

	private boolean isValidUsername(String username) {
		return username != null && !username.trim().isEmpty() && username.length() >= 4 && !username.contains(" ");
	}

	private void markInvalid(TextField field) {
		field.setStyle("-fx-border-color: red;");
	}

	private void clearAllFieldStyles() {
		firstNameField.setStyle("");
		lastNameField.setStyle("");
		phoneField.setStyle("");
		emailField.setStyle("");
		usernameField.setStyle("");
		passwordField.setStyle("");
	}
}