package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public class Workers_GUI {

    @FXML private BorderPane managerRoot;
    @FXML private Button btnLogout;
    @FXML private Button btnManageDiners;
    @FXML private Button btnManageOrders;
    @FXML private Button btnManageTables;
    @FXML private Button btnManageUsers;
    @FXML private Button btnManageWaitlist;
    @FXML private Button btnReports;

    private boolean isCurrentUserAdmin = true; 

    @FXML
    public void initialize() {
        System.out.println("Manager Dashboard Initialized.");

        // --- Permission Logic ---
        if (!isCurrentUserAdmin) {
            btnReports.setVisible(false);
            btnReports.setManaged(false); 
        }

        // --- Event Handlers ---

        btnManageDiners.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Manage Customers Screen");
                // TODO: loadScreen("CustomerManagement.fxml");
            }
        });

        btnManageTables.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Manage Tables Screen");
                // TODO: loadScreen("TableManagement.fxml");
            }
        });

        btnManageUsers.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Manage Users Screen");
            }
        });

        btnReports.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Reports Screen");
            }
        });
        
        btnManageOrders.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                /**
                 * Navigates the employee to the Order Management Dashboard.
                 * Loads the ManageOrders.fxml created for administrative CRUD operations.
                 */
                System.out.println("Go to: Orders Management Dashboard");
                try {
                    // Load the FXML for the management panel
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ManageOrders.fxml"));
                    Parent root = loader.load();

                    // Retrieve the current stage from the button and switch scenes
                    Stage stage = (Stage) btnManageOrders.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Employee Dashboard - Order Management");
                    stage.show();
                    
                } catch (IOException e) {
                    System.err.println("Navigation Error: Failed to load ManageOrders.fxml");
                    e.printStackTrace();
                }
            }
        });

        btnManageWaitlist.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Waitlist Screen");
            }
        });

        // --- Logout Logic ---
        btnLogout.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Logging out...");
                try {
                    //MainScreen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) btnLogout.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    // פונקציה חיצונית לקביעת הרשאות (תקרא לה אחרי הלוגין)
    public void setAdminPermission(boolean isAdmin) {
        this.isCurrentUserAdmin = isAdmin;
        if (!isAdmin) {
            btnReports.setVisible(false);
            btnReports.setManaged(false);
        } else {
            btnReports.setVisible(true);
            btnReports.setManaged(true);
        }
    }
}