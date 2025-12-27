package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Controller for the Casual Customer Menu.
 * Provides navigation to reservation creation, management, and billing.
 */
public class CasualCustomer_GUI {

    /**
     * Navigates to the screen for creating a new reservation.
     * @param event The action event triggered by the "New Reservation" button.
     */
    @FXML
    private void openNewOrder(ActionEvent event) {
    	AddReservation_GUI.previousScreen = "/gui/CasualCustomer.fxml";
        loadScreen(event, "/gui/AddReservation.fxml", "New Reservation");
    }

    /**
     * Navigates to the combined View/Edit reservations screen.
     * This screen allows users to see their list, update details, or cancel.
     * @param event The action event triggered by the "View & Edit" button.
     */
    @FXML
    private void onViewReservations(ActionEvent event) {
    	ViewReservations_GUI.previousScreen = "/gui/CasualCustomer.fxml";
        loadScreen(event, "/gui/ViewReservations.fxml", "My Reservations");
    }

    /**
     * Navigates to the bill payment screen.
     * @param event The action event triggered by the "Pay Bill" button.
     */
    @FXML
    private void onBillPaymentClicked(ActionEvent event) {
        BillPayment_GUI.previousScreen = "/gui/CasualCustomer.fxml";
        loadScreen(event, "/gui/BillPayment.fxml", "Bistro - Bill Payment");
    }

    /**
     * Returns to the main screen and clears the current user session.
     * Acts as a logout for the guest/subscriber session.
     * @param event The action event triggered by the "Sign Out" or "Back" button.
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
        // Clear static session data to prevent data leakage between users
        User_Session.clear(); 
        loadScreen(event, "/gui/MainScreen.fxml", "Bistro - Main Menu");
    }

    /**
     * Helper method to handle screen transitions within the same stage.
     * * @param event The ActionEvent to identify the current window.
     * @param fxmlPath The path to the FXML file to be loaded.
     * @param title The title of the new window/scene.
     */
    private void loadScreen(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Get the current Stage from the source node of the event
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Create new scene and set it to the stage
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
            
            System.out.println("Navigated to: " + title);

        } catch (Exception e) {
            System.err.println("Failed to load screen: " + fxmlPath);
            e.printStackTrace();
        }
    }
}