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
    @FXML private Button btnManageOpeningHours;
    @FXML private Button btnManageOrders;
    @FXML private Button btnManageTables;
    @FXML private Button btnManageUsers;
    @FXML private Button btnManageWaitlist;
    @FXML private Button btnReports;

    @FXML
    public void initialize() {
        System.out.println("Manager Dashboard Initialized.");

        // --- Permission logic for reports button ---

        System.out.println(" LOGIN" + User_Session.getLoggedInUser().getIdentity());
        if(User_Session.getLoggedInUser().getIdentity().equals("Manager")) {
        	System.out.println("MANAGER LOGIN");
        	btnReports.setVisible(true);
            btnReports.setManaged(true);
        }
        

        // --- Event Handlers ---

        btnManageOpeningHours.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Opening & Special Hours Management Screen");
                try {
                    // Load the FXML for the Opening Hours management panel
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ManageHours.fxml"));
                    Parent root = loader.load();

                    // Retrieve the current stage from the button and switch scenes
                    Stage stage = (Stage) btnManageOpeningHours.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();
                    stage.setTitle("Employee Dashboard - Opening Hours Management");
                    stage.show();
                    
                } catch (IOException e) {
                    System.err.println("Navigation Error: Failed to load ManageHours.fxml");
                    e.printStackTrace();
                }
            }
        });

        btnManageTables.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Manage Tables Screen");

                try {
                    FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/gui/ManageTables.fxml")
                    );
                    Parent root = loader.load();

                    Stage stage = (Stage) btnManageTables.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        btnManageUsers.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Manage Users Screen");
                try {
                    // Load the FXML for the management panel
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ManageUsers.fxml"));
                    Parent root = loader.load();

                    // Retrieve the current stage from the button and switch scenes
                    Stage stage = (Stage) btnManageOrders.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();
                    stage.setTitle("Employee Dashboard - User Management");
                    stage.show();
                    
                } catch (IOException e) {
                    System.err.println("Navigation Error: Failed to load ManageUsers.fxml");
                    e.printStackTrace();
                }
            }
        });

        btnReports.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
            public void handle(ActionEvent event) {
                System.out.println("Go to: Manage Report Screen");
                try {
                    // Load the FXML for the management panel
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/reports.fxml"));
                    Parent root = loader.load();

                    // Retrieve the current stage from the button and switch scenes
                    Stage stage = (Stage) btnManageOrders.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();
                    stage.setTitle("Employee Dashboard - reports Management");
                    stage.show();
                    
                } catch (IOException e) {
                    System.err.println("Navigation Error: Failed to load reports.fxml");
                    e.printStackTrace();
                }
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
                    stage.centerOnScreen();
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
                    stage.centerOnScreen();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
//    public void setAdminPermission(boolean isAdmin) {
//        this.isCurrentUserAdmin = isAdmin;
//        if (!isAdmin) {
//            btnReports.setVisible(false);
//            btnReports.setManaged(false);
//        } else {
//            btnReports.setVisible(true);
//            btnReports.setManaged(true);
//        }
//    }
}