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

public class ConnectToServer_GUI {

    // שמירת ה-Controller באופן סטטי כדי שיהיה זמין לכל המסכים
    public static Client_Controller clientController;

    @FXML private VBox connectPane;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectBtn;
    @FXML private Label errorLabel;

    @FXML
    void onConnectClicked(ActionEvent event) {
        String host = ipField.getText().trim();
        String portStr = portField.getText().trim();
        int port;

        // ולידציה בסיסית
        if (host.isEmpty()) {
            errorLabel.setText("IP cannot be empty.");
            return;
        }
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Port must be a number.");
            return;
        }

        // ניסיון התחברות
        try {
            if (clientController == null) {
                clientController = new Client_Controller(host, port);
            }
            
            // אם הגענו לפה - ההתחברות הצליחה!
            System.out.println("Connected successfully. Switching to Main Screen...");
            openMainScreen();

        } catch (IOException e) {
            errorLabel.setText("Connection Failed: Server not reachable.");
            e.printStackTrace();
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openMainScreen() {
        try {
            // טעינת המסך הראשי
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
            Parent mainRoot = loader.load();

            // קבלת החלון הנוכחי והחלפת הסצנה
            Stage stage = (Stage) connectBtn.getScene().getWindow();
            Scene scene = new Scene(mainRoot);
            stage.setScene(scene);
            stage.setTitle("Bistro - Main Menu");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            errorLabel.setText("Error loading MainScreen.fxml");
            e.printStackTrace();
        }
    }
}