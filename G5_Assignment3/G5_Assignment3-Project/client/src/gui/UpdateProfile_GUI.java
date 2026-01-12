package gui;

import entities.UserRecord;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import messages.Message;
import messages.MessageType;
import utils.User_Session;

/**
 * Controller for the Update Profile screen.
 * Allows users to update their personal details.
 */
public class UpdateProfile_GUI {

    public static String previousScreen;
    public static UpdateProfile_GUI instance; 
    private UserRecord currentUser;

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


        System.out.println( currentUser);
        System.out.println( currentUser.getSubscriberCode());
        UserRecord updated = new UserRecord(
        		currentUser.getId(), txtFirstName.getText(), txtLastName.getText(),
        		txtPhone.getText(), txtEmail.getText(), txtUsername.getText(),
                (txtPassword.getText() == null || txtPassword.getText().isEmpty()) ? currentUser.getPassword() : txtPassword.getText(),
                		currentUser.getIdentity(), currentUser.getSubscriberCode());
        
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
 	 * Called when the profile update is successful.
 	 * Updates the form with the latest user details and shows a success message.
 	 * 
     */
    public void onRefresh() {
    	
    	setUser(User_Session.getLoggedInUser());
        lblError.setText("Success!");
        lblError.setStyle("-fx-text-fill: green;");
        lblError.setVisible(true);
    }
    
    /**
 	 * Called when there is an error updating the profile.
 	 * Displays an error message.
     */
    public void onError() {
        lblError.setText("Error");
        lblError.setStyle("-fx-text-fill: red;");
        lblError.setVisible(true);
    }
}





