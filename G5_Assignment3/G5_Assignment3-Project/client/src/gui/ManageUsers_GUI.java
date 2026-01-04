package gui;

import java.io.IOException;
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

/**
 * Controller for the Manage Users screen. 
 * Supports CRUD operations for restaurant subscribers and employees.
 */
public class ManageUsers_GUI {

    // --- FXML UI Bindings ---
    @FXML private TextField searchField;
    @FXML private TableView<UserRecord> usersTable;
    @FXML private TableColumn<UserRecord, Integer> colId;
    @FXML private TableColumn<UserRecord, String> colFullName;
    @FXML private TableColumn<UserRecord, String> colPhone;
    @FXML private TableColumn<UserRecord, String> colEmail;
    @FXML private TableColumn<UserRecord, String> colUsername;
    @FXML private TableColumn<UserRecord, Integer> colSubscriberCode;
    @FXML private TableColumn<UserRecord, String> colIdentity;

    @FXML private TextField idField, firstNameField, lastNameField, phoneField, emailField, usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> identityCombo;
    @FXML private Label formErrorLabel;
    @FXML private Button deleteBtn, backBtn, saveBtn;

    // --- Local Data Model ---
    private final ObservableList<UserRecord> masterList = FXCollections.observableArrayList();
    private UserRecord selectedUser = null;

    public static ManageUsers_GUI instance;

    /**
     * Called by JavaFX when the FXML is loaded. 
     * Sets up table columns and selection listeners without using lambdas.
     */
    @FXML
    public void initialize() {
        instance = this;
        ConnectToServer_GUI.clientController.setManageUsersGUI(this);
        
        // Define how data is mapped to columns
        colId.setCellValueFactory(new PropertyValueFactory<UserRecord, Integer>("id"));
        colPhone.setCellValueFactory(new PropertyValueFactory<UserRecord, String>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<UserRecord, String>("email"));
        colUsername.setCellValueFactory(new PropertyValueFactory<UserRecord, String>("username"));
        colSubscriberCode.setCellValueFactory(new PropertyValueFactory<UserRecord, Integer>("subscriberCode"));
        colIdentity.setCellValueFactory(new PropertyValueFactory<UserRecord, String>("identity"));

        // Full Name Cell Factory using Anonymous Class
        colFullName.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<UserRecord,String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<UserRecord, String> cell) {
                UserRecord u = cell.getValue();
                return new SimpleStringProperty(u.getFirstName() + " " + u.getLastName());
            }
        });

        usersTable.setItems(masterList);

        // Selection Listener using Anonymous Class
        usersTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<UserRecord>() {
            @Override
            public void changed(ObservableValue<? extends UserRecord> obs, UserRecord oldV, UserRecord newV) {
                selectedUser = newV;
                deleteBtn.setDisable(newV == null);
                if (newV == null) {
                    clearForm();
                    saveBtn.setText("Save New");
                } else {
                    fillForm(newV);
                    saveBtn.setText("Update User");
                }
            }
        });

        // Initialize Roles
        identityCombo.setItems(FXCollections.observableArrayList("Subscriber", "Employee", "Manager"));
        identityCombo.getSelectionModel().select("Subscriber");

        // Fetch data from server on startup
        refreshData();
    }
    
    /**
     * Routes incoming server messages to the appropriate UI update logic.
     * @param msg The message from the server.
     */
    public void handle(Message msg) {
        try {
             switch (msg.getType()) {
                case MessageType.GET_ALL_USERS_RESPONSE : 
                    @SuppressWarnings("unchecked") 
                    List<UserRecord> list = (List<UserRecord>) msg.getContent();
                    masterList.setAll(list);
                    break;
                case MessageType.ADD_USER_RESPONSE_OK : 
                    showError("Operation Successful.");
                    refreshData();
                    break;
                case MessageType.ADD_USER_RESPONSE_ERR : 
                    showError("Operation Failed.");
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

    @FXML private void onRefresh() { refreshData(); }

    @FXML
    private void onSave() {
        formErrorLabel.setVisible(false);

        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        String role = identityCombo.getValue();

        if (fName.isEmpty() || lName.isEmpty() || user.isEmpty()) {
            showError("First Name, Last Name, and Username are required.");
            return;
        }

        if (selectedUser == null) {
            // Addition Mode
            if (pass.isEmpty()) { showError("Password required for new user."); return; }
            UserRecord newUser = new UserRecord(0, fName, lName, phoneField.getText(), emailField.getText(), user, pass, role, null);
            ConnectToServer_GUI.clientController.sendAddUserRequest(newUser);
        } else {
            // Update Mode
            String finalPass = pass.isEmpty() ? selectedUser.getPassword() : pass;
            UserRecord updated = new UserRecord(selectedUser.getId(), fName, lName, phoneField.getText(), emailField.getText(), user, finalPass, role, selectedUser.getSubscriberCode());
            ConnectToServer_GUI.clientController.sendEditUserRequest(updated);
        }
    }

    @FXML
    private void onDelete() {
        if (selectedUser != null) {
            ConnectToServer_GUI.clientController.sendRemoveUserRequest(selectedUser);
            onClear();
            refreshData();
        }
    }

    @FXML
    private void onClear() {
        usersTable.getSelectionModel().clearSelection();
        selectedUser = null;
        clearForm();
    }

    /**
     * Returns to the main workers screen and centers the window on the screen.
     */
    @FXML
    private void onBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Workers Dashboard");
            
            // Re-center window after scene change
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillForm(UserRecord u) {
        firstNameField.setText(u.getFirstName());
        lastNameField.setText(u.getLastName());
        phoneField.setText(u.getPhone());
        emailField.setText(u.getEmail());
        usernameField.setText(u.getUsername());
        identityCombo.getSelectionModel().select(u.getIdentity());
    }

    private void clearForm() {
        firstNameField.clear(); lastNameField.clear(); phoneField.clear(); 
        emailField.clear(); usernameField.clear(); passwordField.clear();
        formErrorLabel.setVisible(false);
    }

    private void showError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
    }
}