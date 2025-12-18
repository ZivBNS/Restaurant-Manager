package gui;

import java.io.IOException;

import entities.Subscribed_Customer;
import javafx.application.Platform; // Added for Platform.exit()
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent; // Added for WindowEvent

public class MainScreen_GUI {

    @FXML private BorderPane mainRoot;
    @FXML private VBox employeeLoginBox;
    @FXML private Button employeeToggleBtn;
    @FXML private TextField empUserField;
    @FXML private PasswordField empPassField;
    @FXML private Button empSubmitBtn;
    
    @FXML private TextField guestContactField;
    @FXML private ToggleGroup guestIdentGroup;
    @FXML private Button guestLoginBtn;
    @FXML private RadioButton radioEmail;
    @FXML private RadioButton radioPhone;
    @FXML private Button subLoginBtn;
    @FXML private Label subErrorLabel;
    @FXML private PasswordField subPasswordField;
    @FXML private TextField subUsernameField;
    @FXML private Button terminalBtn;

    @FXML
    void onEmployeeToggleClick(ActionEvent event) {
        boolean isVisible = employeeLoginBox.isVisible();
        employeeLoginBox.setVisible(!isVisible);
    }
    
    @FXML
    public void initialize() {
        System.out.println("Main Screen Loaded Successfully");

        // --- Handle "X" Button (Window Close) Logic ---
        // Using runLater to ensure the Stage is loaded before accessing it
        Platform.runLater(() -> {
            if (mainRoot.getScene() != null) {
                Stage stage = (Stage) mainRoot.getScene().getWindow();
                stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent event) {
                        System.out.println("X button clicked. Performing logout and closing...");
                        performLogoutCleanup(); // Send logout to server
                        Platform.exit();        // Close JavaFX
                        System.exit(0);         // Kill process
                    }
                });
            }
        });

        // --- Terminal Button Logic ---
        terminalBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                openTerminalScreen();
            }
        });

        // --- Employee Login Logic (Temporary Bypass) ---
        empSubmitBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // TODO: Add real validation (username/password) here later
                System.out.println("Employee Login Clicked - Bypassing validation...");
                openManagerDashboard();
            }
        });
    }

    /**
     * Helper method to handle the server disconnection logic.
     * This allows us to reuse the code for both the "Logout" button and the "X" button.
     */
    private void performLogoutCleanup() {
        if (ConnectToServer_GUI.clientController != null) {
            try {
                ConnectToServer_GUI.clientController.logout(); // Sends message to server
            } catch (Exception e) {
                e.printStackTrace();
            }
            ConnectToServer_GUI.clientController = null;
        }
    }

    private void openTerminalScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Terminal.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) terminalBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            System.out.println("Switched to Terminal Screen");
        } catch (IOException e) {
            System.out.println("Error loading Terminal.fxml");
            e.printStackTrace();
        }
    }

    private void openManagerDashboard() {
        try {
            // Note: Changed file name to Workers.fxml based on your previous code
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Workers.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) empSubmitBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            System.out.println("Switched to Manager Dashboard");
        } catch (IOException e) {
            System.out.println("Error loading Workers.fxml");
            e.printStackTrace();
        }
    }
    
    // Logic for the Logout BUTTON (Navigates back to Connect screen)
    public void logout(ActionEvent event) {
        try {
            // 1. Perform server disconnection
            performLogoutCleanup();

            // 2. Load Connect Screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ConnectToServer.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) mainRoot.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Bistro - Connect");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Triggered when a guest clicks the login button.
     * Validates the input and saves it to the static User_Session.
     */
    @FXML
    private void openGuestMenu(ActionEvent event) {
        String contactInfo = guestContactField.getText().trim();

        if (contactInfo.isEmpty()) {
            System.out.println("Error: Please enter a phone or email.");
            return;
        }

        // 1. Save data to User_Session based on the selected RadioButton
        if (radioPhone.isSelected()) {
            User_Session.setCasualData(contactInfo, null);
        } else {
            User_Session.setCasualData(null, contactInfo);
        }

        // 2. Proceed to the next screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CasualCustomer.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Casual Customer Menu");
            stage.show();
            
            System.out.println("MainScreen: Casual Session Started: " + contactInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Triggered when a subscriber clicks the login button.
     * Handles authentication and navigation to the Subscriber Dashboard.
     */
    @FXML
    void onSubscriberLoginClick(ActionEvent event) {
        String username = subUsernameField.getText().trim();
        String password = subPasswordField.getText();

        // 1. Basic Validation
        if (username.isEmpty() || password.isEmpty()) {
            subErrorLabel.setText("Please enter username and password.");
            subErrorLabel.setVisible(true);
            return;
        }

        System.out.println("Subscriber Login Attempt: " + username);

        // --- PLACEHOLDER FOR SERVER VALIDATION ---
        // TODO: Send login request to server (e.g., clientController.login(username, password))
        // and wait for a response message from the server.
        // For now, we bypass validation and proceed to the dashboard:
        boolean loginSuccessful = true; 
        // -----------------------------------------

        if (loginSuccessful) {
            try {
            	
                // 2. --------CHANGE THIS AFTER USER VALIDTION----------
                 Subscribed_Customer mockUser = new Subscribed_Customer("User", "Test", "1234567890", "user@test.com", "a", "a");
                 User_Session.setLoggedInUser(mockUser);

                // 3. Load the Subscribed Customer Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscribedCustomer.fxml"));
                Parent root = loader.load();
                
                // Get current window (Stage)
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                
                // Switch scene
                stage.setScene(new Scene(root));
                stage.setTitle("Bistro - Member Dashboard");
                stage.show();

                System.out.println("Login Success: Switched to Subscriber Dashboard");

            } catch (IOException e) {
                System.err.println("Error loading SubscribedCustomer.fxml");
                e.printStackTrace();
                subErrorLabel.setText("System error loading dashboard.");
                subErrorLabel.setVisible(true);
            }
        } else {
            // Handle failed login
            subErrorLabel.setText("Invalid username or password.");
            subErrorLabel.setVisible(true);
        }
    }
}