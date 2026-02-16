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
import utils.User_Session;

/**
 * Controller for the Subscribed Customer dashboard.
 */
public class SubscribedCustomer_GUI {

    @FXML private Label welcomeLabel;
    
    @FXML private Label IDLabel;
    /**
     * Called automatically when the FXML is loaded.
     * Initializes the welcome message and subscriber code display.
     */
    @FXML
    public void initialize() {
        UserRecord user = User_Session.getLoggedInUser();
        if (user != null) {
            welcomeLabel.setText("Welcome Back, " + user.getFirstName() + "!");
            IDLabel.setText("Subscriber Code : " + Integer.toString(user.getSubscriberCode()));
        }
    }
    
    /**
     * Navigates to the screen for creating a new reservation.
     */
    @FXML
    void onNewReservation(ActionEvent event) {
    	AddReservation_GUI.previousScreen = "/gui/SubscribedCustomer.fxml";
        loadScreen(event, "/gui/AddReservation.fxml", "New Reservation");
    }

    /**
	 * Navigates to the combined View/Edit reservations screen.
	 * This screen allows users to see their list, update details, or cancel.
	 */
    @FXML
    void onManageReservations(ActionEvent event) {
    	ViewReservations_GUI.previousScreen = "/gui/SubscribedCustomer.fxml";
        loadScreen(event, "/gui/ViewReservations.fxml", "My Reservations");
    }
    
    /**
     * Navigates to the profile update screen.
     * Allows the subscriber to update their personal details.
     */
    @FXML 
    void onUpdateProfile(ActionEvent event) {
    	loadScreen(event, "/gui/UpdateProfile.fxml", "Personal Details");
    }
    
    /**
	 * Navigates to the barcode display screen.
	 * Shows the subscriber's unique barcode.
	 */
    @FXML
    private void onShowBarcode(ActionEvent event) {
    	loadScreen(event, "/gui/BarcodeView.fxml", "Subscriber Barcode");
    }
    
    /**
	 * Navigates to the order history screen.
	 * Displays the subscriber's past orders.
	 */
    @FXML
    void onOrderHistory(ActionEvent event) {
        loadScreen(event, "/gui/OrderHistory.fxml", "My Order History");
    }

    /**
	 * Navigates to the bill payment screen.
	 * Allows the subscriber to pay their bills.
	 */
    @FXML
    void onPayBill(ActionEvent event) {
    	BillPayment_GUI.previousScreen = "/gui/SubscribedCustomer.fxml";
        loadScreen(event, "/gui/BillPayment.fxml", "Bistro - Bill Payment");
    }

    /**
	 * Logs out the current user and returns to the main login screen.
	 */
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