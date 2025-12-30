package gui;

import entities.Subscribed_Customer;
import entities.User;
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
    
    @FXML
    public void initialize() {
    	instance = this;
    	setUser(User_Session.getLoggedInUser());
    }
    

    public void setUser(UserRecord user) {
        this.currentUser = user;

       	txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtPhone.setText(user.getPhone());
        txtEmail.setText(user.getEmail());
        txtUsername.setText(user.getUsername());
        
    }
    

    @FXML
    private void onSaveClicked(ActionEvent event) {

        if (currentUser == null) return;

        if (txtFirstName.getText().isEmpty() || txtLastName.getText().isEmpty()
                || txtPhone.getText().isEmpty() || txtEmail.getText().isEmpty()) {
            lblError.setText("Please fill all fields.");
            lblError.setVisible(true);
            return;
        }

//        currentUser.setFirstName(txtFirstName.getText());
//        currentUser.setLastName(txtLastName.getText());
//        currentUser.setPhone(txtPhone.getText());
//        currentUser.setEmail(txtEmail.getText());
//        currentUser.setUsername(txtUsername.getText());
        

//        if (!txtPassword.getText().isEmpty()) {
//            currentUser.setPassword(txtPassword.getText());
//        }

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
    


    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            String screen = previousScreen != null
                    ? previousScreen
                    : "/gui/SubscribedCustomer.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(screen));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onRefresh() {
    	
    	setUser(User_Session.getLoggedInUser());
        lblError.setText("Success!");
        lblError.setStyle("-fx-text-fill: green;");
        lblError.setVisible(true);
    }
    
    public void onError() {
        lblError.setText("Error");
        lblError.setStyle("-fx-text-fill: red;");
        lblError.setVisible(true);
    }
}





