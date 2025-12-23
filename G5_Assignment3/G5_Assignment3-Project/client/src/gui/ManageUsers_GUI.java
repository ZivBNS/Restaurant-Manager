package gui;

import java.io.IOException;
import java.util.List;

import entities.UserRecord;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import messages.Message;
import messages.MessageType;


public class ManageUsers_GUI {

	@FXML private TextField searchField;
    @FXML private TableView<UserRecord> usersTable;
    @FXML private TableColumn<UserRecord, Integer> colId;
    //@FXML private TableColumn<Subscribed_Customer, String> colFirstName;
    //@FXML private TableColumn<Subscribed_Customer, String> colLastName;
    @FXML private TableColumn<UserRecord, String> colFullName;
    @FXML private TableColumn<UserRecord, String> colPhone;
    @FXML private TableColumn<UserRecord, String> colEmail;
    @FXML private TableColumn<UserRecord, String> colUsername;
    @FXML private TableColumn<UserRecord, Integer> colSubscriberCode;
    @FXML private TableColumn<UserRecord, String> colIdentity;

    @FXML private TextField idField;
    @FXML private TextField firstNameField, lastNameField, phoneField, emailField, usernameField, subscriberCodeField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> identityCombo;

    @FXML private Label formErrorLabel;
    @FXML private Label statusMessageLabel;
    @FXML private Button deleteBtn;
    @FXML private Button backBtn;

    private final ObservableList<UserRecord> master = FXCollections.observableArrayList();
    private UserRecord selected = null;

    @FXML
    public void initialize() {
    	
    	ConnectToServer_GUI.clientController.setManageUsersGUI(this);
    	
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        //colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        //colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colSubscriberCode.setCellValueFactory(new PropertyValueFactory<>("subscriberCode"));
        colIdentity.setCellValueFactory(new PropertyValueFactory<>("identity"));

        colFullName.setCellValueFactory(cell ->
        	new SimpleStringProperty(cell.getValue().getFirstName() + " " + cell.getValue().getLastName())
        );
        usersTable.setItems(master);

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selected = newV;
            deleteBtn.setDisable(newV == null);
            if (newV == null) clearForm();
            else fillForm(newV);
        });

        identityCombo.setItems(
        		FXCollections.observableArrayList(
                "Subscriber",
                "Employee",
                "Manager"
            ));
        if (identityCombo.getItems().contains("Subscriber")) {
            identityCombo.getSelectionModel().select("Subscriber");
        }

        loadUsersAsk();
    }
    
    public void handle(Message msg) {
        
        try {
             switch (msg.getType()) {
            
                case MessageType.GET_ALL_USERS_RESPONSE : 
                	@SuppressWarnings("unchecked") 
                	List<UserRecord> users = (List<UserRecord>) msg.getContent();
                	loadUsersRecive(users);
                	break;
                

                case MessageType.ADD_USER_RESPONSE_OK : newUserAdded(); break;
                case MessageType.ADD_USER_RESPONSE_ERR : failedToAddUser(); break;
//
//                case MessageType.UPDATE_USER -> handleUpdate((Subscribed_Customer) msg.getContent());
//
//                case MessageType.REMOVE_USER -> handleDelete((String) msg.getContent()); 
//                // e.g. delete by username (or id)

                default:	
                	System.out.println("ManageUsers_GUI: Received unhandled message type: " + msg.getType());
            
            };
        } catch (Exception e) {
            e.printStackTrace();
            //return new Message(MessageType.USERS_ERROR, "Server error.");
        }
    }

    private void loadUsersAsk() {
    	
    	ConnectToServer_GUI.clientController.sendGetAllUsersRequest();
    	//onRefresh();
    }
    private void loadUsersRecive(List<UserRecord> receivedUsers) {
    	master.clear();
        master.setAll(receivedUsers);
    }
    
    private void newUserAdded() {
    	
    	
    	
    	showError("New user added");
    	loadUsersAsk();

    }
    
    private void failedToAddUser() {
    	
    	showError("Error adding user");

    }

    @FXML
    private void onRefresh() {
    	loadUsersAsk();
    }

    @FXML
    private void onAddNew() {
        usersTable.getSelectionModel().clearSelection();
        selected = null;
        clearForm();
        idField.setText(""); // new user
    }

    @FXML
    private void onSave() {
        formErrorLabel.setVisible(false);

        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // don't trim passwords
        String identity = identityCombo.getValue();
        String codeText = subscriberCodeField.getText().trim();

        if (first.isEmpty() || last.isEmpty() || username.isEmpty()) {
            showError("First name, last name, and username are required.");
            return;
        }
        if (email.isEmpty() && phone.isEmpty()) {
            showError("Enter at least email or phone.");
            return;
        }
        if (identity == null || identity.isBlank()) {
            showError("Identity is required.");
            return;
        }

        Integer subscriberCode = null;
        if (!codeText.isEmpty()) {
            try {
                subscriberCode = Integer.parseInt(codeText);
            } catch (NumberFormatException e) {
                showError("Subscriber code must be a number.");
                return;
            }
        }

        if (selected == null) {
            // ADD MODE
            if (password == null || password.isEmpty()) {
                showError("Password is required for a new user.");
                return;
            }

            // Build your object (adjust constructor / setters to your actual class!)
//            int cardCode = 0;
//            try {
//            	cardCode = Integer.parseInt(codeText);
//			} catch (Exception e) {
//				showError("bad card-code from DB!");
//			}
            UserRecord newUser = new UserRecord(0, first, last, phone, email, username, password, identity, subscriberCode);
            ConnectToServer_GUI.clientController.sendAddUserRequest(newUser);
            // TODO: set identity + subscriberCode if your class supports it (setters)
            // newUser.setIdentity(identity);
            // if (subscriberCode != null) newUser.setSubscriberCode(subscriberCode);

            // TODO: send ADD message to server
            onRefresh();
            // On success:
            //master.add(newUser);
            usersTable.getSelectionModel().select(newUser);

        } else {
            // EDIT MODE
            // If password empty -> keep old password (server-side recommended)
            UserRecord updated = new UserRecord(selected.getId(),first, last, phone, email, username,
                    (password == null || password.isEmpty()) ? selected.getPassword() : password, identity, selected.getSubscriberCode());

            // updated.setId(selected.getId());
            // updated.setIdentity(identity);
            // updated.setSubscriberCode(subscriberCode);
            ConnectToServer_GUI.clientController.sendEditUserRequest(updated);
            onRefresh();
            int idx = master.indexOf(selected);
            if (idx >= 0) {
                master.set(idx, updated);
                usersTable.getSelectionModel().select(updated);
                selected = updated;
            }
        }

        passwordField.clear();
    }

    @FXML
    private void onDelete() {
        if (selected == null) return;
        ConnectToServer_GUI.clientController.sendRemoveUserRequest(selected);
        //master.remove(selected);
        selected = null;
        clearForm();
        onRefresh();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    @FXML
    private void onBack() {


    	System.out.println("Go to: Workers Screen");

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/gui/Workers.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Employee Dashboard - Main Menu");
            stage.show();

        } catch (IOException e) {
            System.err.println("Navigation Error: Failed to load Workers.fxml");
            e.printStackTrace();
        }
        
        
    }

    private void fillForm(UserRecord newV) {
        //idField.setText(String.valueOf(u.ge));
        firstNameField.setText(safe(newV.getFirstName()));
        lastNameField.setText(safe(newV.getLastName()));
        phoneField.setText(safe(newV.getPhone()));
        emailField.setText(safe(newV.getEmail()));
        usernameField.setText(safe(newV.getUsername()));
        subscriberCodeField.setText(String.valueOf(newV.getSubscriberCode()));
        identityCombo.getSelectionModel().select(safe(newV.getIdentity()));
        passwordField.clear(); // don't show current password
    }

    private void clearForm() {
        idField.clear();
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        emailField.clear();
        usernameField.clear();
        subscriberCodeField.clear();
        passwordField.clear();
        formErrorLabel.setVisible(false);
        if (identityCombo.getItems().contains("Subscriber")) {
            identityCombo.getSelectionModel().select("Subscriber");
        }
    }

    private void showError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
