package gui;

import java.io.IOException;
import controllers.Client_Controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the initial Connection screen.
 * Responsible for establishing a network link with the Bistro Server
 * before allowing access to the main application.
 */
public class ConnectToServer_GUI {

    /** Static reference to the client controller to make it accessible across all scenes. */
    public static Client_Controller clientController;

    @FXML private VBox connectPane;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectBtn;
    @FXML private Label errorLabel;

    /**
     * Handles the 'Connect' button logic. Validates input and attempts to initialize the OCSF client.
     * @param event The action event triggered by the button.
     */
    @FXML
    void onConnectClicked(ActionEvent event) {
        String host = ipField.getText().trim();
        String portStr = portField.getText().trim();
        int port;

        // 1. Basic UI Validation
        if (host.isEmpty()) {
            showError("Host IP address is required.");
            return;
        }
        
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showError("Port must be a valid numerical value.");
            return;
        }

        // 2. Connection Attempt
        try {
            // Only create a new controller if one doesn't exist
            if (clientController == null) {
                clientController = new Client_Controller(host, port);
            }
            
            System.out.println("Network: Handshake successful. Navigating to Main Screen...");
            openMainScreen();

        } catch (IOException e) {
            showError("Connection Failed: Server is unreachable at " + host + ":" + port);
            e.printStackTrace();
        } catch (Exception e) {
            showError("System Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the Main Screen FXML and switches the primary stage scene.
     */
    private void openMainScreen() {
        try {
            // Load the main dashboard/menu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainScreen.fxml"));
            Parent mainRoot = loader.load();

            // Get the current stage from any UI element and swap the scene
            Stage stage = (Stage) connectBtn.getScene().getWindow();
            Scene scene = new Scene(mainRoot);
            
            stage.setScene(scene);
            stage.setTitle("Bistro Restaurant Management System");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            showError("Navigation Error: Could not load MainScreen.fxml");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to display error messages in the UI label.
     * @param message The text to display.
     */
    private void showError(String message) {
        errorLabel.setText(message);
    }
}