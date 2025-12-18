package gui;

import entities.Subscribed_Customer;
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
        Subscribed_Customer user = User_Session.getLoggedInUser();
        if (user != null) {
            welcomeLabel.setText("Welcome Back, " + user.getFirstName() + "!");
        }
    }

    @FXML
    void onNewReservation(ActionEvent event) {
        loadScreen(event, "/gui/AddReservation.fxml", "Create Reservation");
    }

    @FXML
    void onManageReservations(ActionEvent event) {
        // Points to the consolidated screen we built earlier
        loadScreen(event, "/gui/ViewReservations.fxml", "Manage My Reservations");
    }

    @FXML
    void onUpdateProfile(ActionEvent event) {
        loadScreen(event, "/gui/UpdateProfile.fxml", "Personal Details");
    }

    @FXML
    void onPayBill(ActionEvent event) {
        loadScreen(event, "/gui/BillPayment.fxml", "Order Payment");
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