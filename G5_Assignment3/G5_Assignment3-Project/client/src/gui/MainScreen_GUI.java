package gui;

import java.io.IOException;

import entities.Casual_Customer;
import entities.LoginData;
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
import messages.Message;
import messages.MessageType;

public class MainScreen_GUI {
	public static MainScreen_GUI instance;

    @FXML private BorderPane mainRoot;
    @FXML private VBox employeeLoginBox;
    @FXML private Button employeeToggleBtn;
    @FXML private TextField empUserField;
    @FXML private PasswordField empPassField;
    @FXML private Button empSubmitBtn;
    
    @FXML private TextField guestContactField;
    @FXML private ToggleGroup guestIdentGroup;
    @FXML private Button guestLoginBtn;
    @FXML private Label casualErrorLabel;
    @FXML private RadioButton radioEmail;
    @FXML private RadioButton radioPhone;
    @FXML private Button subLoginBtn;
    @FXML private Label subErrorLabel;
    @FXML private PasswordField subPasswordField;
    @FXML private TextField subUsernameField;
    @FXML private Button terminalBtn;
    
    private Stage stage;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void onEmployeeToggleClick(ActionEvent event) {
        boolean isVisible = employeeLoginBox.isVisible();
        employeeLoginBox.setVisible(!isVisible);
    }
    
    @FXML
    public void initialize() {
    	instance = this;
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
     */

    @FXML
    void onGuestLoginClick(ActionEvent event) {
        String contactInfo = guestContactField.getText().trim();

        if (contactInfo.isEmpty()) {
            System.out.println("Error: Please enter a phone or email.");
            casualErrorLabel.setVisible(true);
            return;
        }
        
        if (ConnectToServer_GUI.clientController == null) {
            subErrorLabel.setText("Not connected to server. Please reconnect.");
            subErrorLabel.setVisible(true);
            return;
        }
        LoginData loginData;
        if (radioPhone.isSelected()) {
        	int phone = Integer.parseInt(contactInfo);
        	loginData = new LoginData(phone);	//if phone is selected
        	User_Session.setCasualData(contactInfo, null);
		}else
		{
			loginData = new LoginData(contactInfo);	//if email is selected
			User_Session.setCasualData(null, contactInfo);
		}
        
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); //save the stage
        ConnectToServer_GUI.clientController.sendGuestLoginRequest(loginData);

    }
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
        
        
        if (ConnectToServer_GUI.clientController == null) {
            subErrorLabel.setText("Not connected to server. Please reconnect.");
            subErrorLabel.setVisible(true);
            return;
        }
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        LoginData loginData = new LoginData(username, password);
        ConnectToServer_GUI.clientController.sendSubscriberLoginRequest(loginData);


    }
    
    /**
     * Triggered when a subscriber clicks the login button.
     * Handles authentication and navigation to the Subscriber Dashboard.
     */
    public void onSubLoginSuccess(Subscribed_Customer user) {
    	try {
        	
            // 3. Load the Subscribed Customer Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscribedCustomer.fxml"));
            Parent root = loader.load();
            
            // Switch scene
            stage.setScene(new Scene(root));
            stage.setTitle("Casual Customer Menu");
            stage.show();

            System.out.println("Login Success: Switched to Subscriber Dashboard");

        } catch (IOException e) {
            System.err.println("Error loading SubscribedCustomer.fxml");
            e.printStackTrace();
            subErrorLabel.setText("System error loading dashboard.");
            subErrorLabel.setVisible(true);
        }
    } 
    
    public void onSubLoginFailure() {
    	subErrorLabel.setText("Invalid username or password.");
        subErrorLabel.setVisible(true);
    }
    
    public void onGuestLoginSuccess() {
    	try {
        	
            // 3. Load the Casual Customer Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CasualCustomer.fxml"));
            Parent root = loader.load();
            
            // Switch scene
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Member Dashboard");
            stage.show();

            System.out.println("MainScreen: Casual Session Started ");

        } catch (IOException e) {
            System.err.println("Error loading SubscribedCustomer.fxml");
            e.printStackTrace();
            casualErrorLabel.setText("System error loading dashboard.");
            casualErrorLabel.setVisible(true);
        }
    } 
    
    public void onGuestLoginFailure(Message msg) {
    	String errorMsg;
    	if (msg.getContent() == null) {
    		errorMsg = "Phone or Email already exsist!\nPlease use subscriber login";
    	}else errorMsg = (String) msg.getContent();
    	
    	casualErrorLabel.setText(errorMsg);
    	casualErrorLabel.setVisible(true);
    }
}