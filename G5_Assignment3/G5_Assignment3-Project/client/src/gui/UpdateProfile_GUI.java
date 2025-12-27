package gui;

import entities.Subscribed_Customer;
import entities.User;
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
    
    private Subscribed_Customer currentUser;

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

   /* public void setUser(Subscribed_Customer user) {
        this.currentUser = user;

       txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtPhone.setText(user.getPhone());
        txtEmail.setText(user.getEmail());
        txtUsername.setText(user.getUsername());
        
    }
    */

   /* @FXML
    private void onSaveClicked(ActionEvent event) {

        if (currentUser == null) return;

        if (txtFirstName.getText().isEmpty() || txtLastName.getText().isEmpty()
                || txtPhone.getText().isEmpty() || txtEmail.getText().isEmpty()) {
            lblError.setText("Please fill all fields.");
            lblError.setVisible(true);
            return;
        }

        currentUser.setFirstName(txtFirstName.getText());
        currentUser.setLastName(txtLastName.getText());
        currentUser.setPhone(txtPhone.getText());
        currentUser.setEmail(txtEmail.getText());
        currentUser.setUsername(txtUsername.getText());
        

        if (!txtPassword.getText().isEmpty()) {
            currentUser.setPassword(txtPassword.getText());
        }

        ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.UPDATE_USER_PROFILE, currentUser)
        );
    }*/
    
    @FXML
    private void onSaveClicked(ActionEvent event) {
        // temporary – screen only
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
}





