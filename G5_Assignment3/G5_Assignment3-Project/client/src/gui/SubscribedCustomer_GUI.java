package gui;

import entities.UserRecord;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller for the Subscribed Customer dashboard.
 */
public class SubscribedCustomer_GUI {

    @FXML private Label welcomeLabel;
    /**
     * Called automatically when the FXML is loaded.
     */
    @FXML
    public void initialize() {
        UserRecord user = User_Session.getLoggedInUser();
        if (user != null) {
            welcomeLabel.setText("Welcome Back, " + user.getFirstName() + "!");
        }
    }

    @FXML
    void onNewReservation(ActionEvent event) {
    	AddReservation_GUI.previousScreen = "/gui/SubscribedCustomer.fxml";
        loadScreen(event, "/gui/AddReservation.fxml", "New Reservation");
    }

    @FXML
    void onManageReservations(ActionEvent event) {
    	ViewReservations_GUI.previousScreen = "/gui/SubscribedCustomer.fxml";
        loadScreen(event, "/gui/ViewReservations.fxml", "My Reservations");
    }
    
    @FXML 
    void onUpdateProfile(ActionEvent event) {
    	loadScreen(event, "/gui/UpdateProfile.fxml", "Personal Details");
    }

   /* @FXML
    void onUpdateProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/UpdateProfile.fxml"));
            Parent root = loader.load();

            UpdateProfile_GUI controller = loader.getController();
            controller.setUser(currentUser);   // pass the logged-in subscriber

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Personal Details");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


    @FXML
    void onPayBill(ActionEvent event) {
    	BillPayment_GUI.previousScreen = "/gui/SubscribedCustomer.fxml";
        loadScreen(event, "/gui/BillPayment.fxml", "Bistro - Bill Payment");
    }

    @FXML
    void onLogout(ActionEvent event) {
        User_Session.clear();
        loadScreen(event, "/gui/MainScreen.fxml", "Bistro - Login");
    }

    /**
     * Helper method for switching scenes.
     */
    private void loadScreen(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}