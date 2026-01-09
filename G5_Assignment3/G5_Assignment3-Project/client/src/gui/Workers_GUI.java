package gui;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import utils.User_Session;

/**
 * Controller for the Employee/Manager Dashboard.
 * Manages navigation to various management modules and handles permissions.
 */
public class Workers_GUI {

    @FXML private BorderPane managerRoot;
    @FXML private Button btnLogout;
    @FXML private Button btnManageOpeningHours;
    @FXML private Button btnManageOrders;
    @FXML private Button btnManageTables;
    @FXML private Button btnManageUsers;
    @FXML private Button btnManageWaitlist;
    @FXML private Button btnReports;

    /**
     * Initializes the dashboard, sets up permissions, and assigns unified 
     * navigation handlers to all management buttons.
     */
    @FXML
    public void initialize() {
        System.out.println("Manager Dashboard Initialized.");

        // --- Permission Logic ---
        // Only users with 'Manager' identity can see and access the Reports section
        if (User_Session.getLoggedInUser().getIdentity().equals("Manager")) {
            btnReports.setVisible(true);
            btnReports.setManaged(true);
        }

        // --- Event Handlers (Using Unified Navigation) ---

        btnManageOpeningHours.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                navigateTo("/gui/ManageHours.fxml", "Bistro - Opening Hours Management", event);
            }
        });

        btnManageTables.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                navigateTo("/gui/ManageTables.fxml", "Bistro - Tables Management", event);
            }
        });

        btnManageUsers.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                navigateTo("/gui/ManageUsers.fxml", "Bistro - User Management", event);
            }
        });

        btnReports.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                navigateTo("/gui/reports.fxml", "Bistro - Reports Management", event);
            }
        });

        btnManageOrders.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                navigateTo("/gui/ManageOrders.fxml", "Bistro - Reservation Management", event);
            }
        });

        btnManageWaitlist.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                navigateTo("/gui/ManageWaitlist.fxml", "Bistro - Waitlist Management", event);
            }
        });

        // --- Logout Logic (Returns to standard window size) ---
        btnLogout.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Logging out...");
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) btnLogout.getScene().getWindow();
                    
                    // Reset maximized state before returning to the main screen
                    stage.setMaximized(false);
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();
                    stage.setTitle("Bistro - Main");
                } catch (IOException e) {
                    System.err.println("Logout Error: Failed to load MainScreen.fxml");
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Unified navigation method to ensure consistent Full Screen (Maximized) behavior.
     * This method handles loading FXML, setting the scene, and maximizing the stage.
     * * @param fxmlPath The path to the FXML file.
     * @param title    The title of the new window.
     * @param event    The ActionEvent triggered by the button click.
     */
    private void navigateTo(String fxmlPath, String title, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(title);

            stage.setResizable(true);

            if (stage.isMaximized()) {
                stage.setMaximized(false);
            }
            stage.setMaximized(true);
            
            stage.show();

            System.out.println("Navigated to: " + title);

        } catch (IOException e) {
            System.err.println("Navigation Error: Failed to load " + fxmlPath);
            e.printStackTrace();
        }
    }
}