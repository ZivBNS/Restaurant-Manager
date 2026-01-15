package gui;

import entities.UserRecord;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import messages.Message;
import messages.MessageType;
import utils.DataChecker;
import utils.User_Session;

/**
 * Controller for the Update Profile screen.
 * Allows users to update their personal details.
 */
public class UpdateProfile_GUI {

    public static String previousScreen;
    public static UpdateProfile_GUI instance; 
    private UserRecord currentUser;
    private boolean isUpdatePending = false;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    
    /**
 	 * Called automatically when the FXML is loaded.
 	 * Initializes the controller and sets the current user details in the form.
     */
    @FXML
    public void initialize() {
    	instance = this;
    	setUser(User_Session.getLoggedInUser());
    }
    

    /**
 	 * Sets the current user details in the form fields.
 	 * @param user The user whose details are to be displayed and edited.
     */
    public void setUser(UserRecord user) {
        this.currentUser = user;

       	txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtPhone.setText(user.getPhone());
        txtEmail.setText(user.getEmail());
        txtUsername.setText(user.getUsername());
        
    }
    

    /**
 	 * Handles the action when the Save button is clicked.
 	 * Validates input and sends an update request to the server.
     */
    @FXML
    private void onSaveClicked(ActionEvent event) {

        if (currentUser == null) return;

        if (txtFirstName.getText().isEmpty() || txtLastName.getText().isEmpty()
                || txtPhone.getText().isEmpty() || txtEmail.getText().isEmpty()) {
            lblError.setText("Please fill all fields.");
            lblError.setVisible(true);
            return;
        }

        if (!DataChecker.validateContactInfo(txtEmail.getText(), txtPhone.getText())) {
			lblError.setText("Invalid email or phone format.");
			lblError.setVisible(true);
			return;
		}
        
        if (!isValidName(txtFirstName.getText())) {
        	lblError.setText("Invalid first name");
			return;
		}
		if (!isValidName(txtLastName.getText())) {
			lblError.setText("Invalid last name");
			return;
		}

        UserRecord updated = new UserRecord(
        		currentUser.getId(), txtFirstName.getText(), txtLastName.getText(),
        		txtPhone.getText(), txtEmail.getText(), txtUsername.getText(),
                (txtPassword.getText() == null || txtPassword.getText().isEmpty()) ? currentUser.getPassword() : txtPassword.getText(),
                		currentUser.getIdentity(), currentUser.getSubscriberCode());
        this.isUpdatePending = true;
        ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.UPDATE_USER_DETAILS_REQUEST, updated)
        );
    }
    


    /**
 	 * Handles the action when the Back button is clicked.
 	 * Navigates back to the previous screen.
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            String screen = previousScreen != null
                    ? previousScreen
                    : "/gui/SubscribedCustomer.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(screen));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            instance=null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called by Client_Controller when data changes (either by me or by admin broadcast).
     */
    public void onRefresh() {
        // Always update the data fields so they match the server
        setUser(User_Session.getLoggedInUser());
        
        // Hide any previous error messages since the data load was successful
        lblError.setVisible(false);

        // --- Check the flag ---
        if (this.isUpdatePending) {
            // Case A: I clicked Save. Show Success Alert instead of a label.
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Profile updated successfully!");
            alert.showAndWait();
            
            // Reset flag
            this.isUpdatePending = false;
        } 
        
    }
    
    /**
     * Called when a server broadcast updates the user list.
     */
    public void onBroadcastRefresh() {
        // Update fields to match latest server state
        setUser(User_Session.getLoggedInUser());
    }
    
    /**
 	 * Called when there is an error updating the profile.
 	 * Displays an error message.
     */
    public void onError(String errorMessage) {
        lblError.setText(errorMessage);
        lblError.setStyle("-fx-text-fill: red;");
        lblError.setVisible(true);
    }
    
	// Validation Helper
	private boolean isValidName(String name) {
		return name != null && !name.trim().isEmpty() && name.matches("[A-Za-z ]+");
	}

}





