package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;


public class Terminal_GUI {

    // Root Pane
    @FXML private BorderPane terminalRoot;

    // Buttons
    @FXML private Button btnJoinWaitlist;
    @FXML private Button btnPayBill;
    @FXML private Button btnLeaveWaitlist;
    @FXML private Button btnCancelRes;
    @FXML private Button backBtn;

    // Login Section
    @FXML private TextField termUserField;
    @FXML private PasswordField termPassField;
    @FXML private Button termLoginBtn;
    @FXML private Label termErrorLabel;

    /**
     * Called automatically by JavaFX after the FXML is loaded.
     * We use this to bind actions to the buttons.
     */
    @FXML
    public void initialize() {
        System.out.println("Terminal Screen Initialized.");

        // --- Bind Button Actions (Using Anonymous Inner Classes as requested) ---

        // 1. Join Waitlist
        btnJoinWaitlist.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Action: Join Waitlist clicked.");
                // TODO: Open a popup dialog to get Name/Phone/Diners
                // Client_Controller.getInstance().sendToServer(new Message(MessageType.JOIN_WAITLIST, data));
            }
        });

        // 2. Pay Bill
        btnPayBill.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Action: Pay Bill clicked.");
                // TODO: Open a popup to enter Table Number
            }
        });

        // 3. Leave Waitlist
        btnLeaveWaitlist.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Action: Leave Waitlist clicked.");
                // TODO: Request phone number to identify the waiter
            }
        });

        // 4. Cancel Reservation
        btnCancelRes.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Action: Cancel Reservation clicked.");
                // TODO: Ask for Reservation ID
            }
        });

        // 5. Back Button
        backBtn.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
            public void handle(ActionEvent event) {
                System.out.println("Logging out...");
                try {
                    //MainScreen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) backBtn.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // 6. Subscriber Login Logic
        termLoginBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleSubscriberLogin();
            }
        });
    }

    /**
     * Logic for the Subscriber Login card.
     */
    private void handleSubscriberLogin() {
        String username = termUserField.getText();
        String password = termPassField.getText();

        // 1. Client-Side Validation
        if (username.isEmpty() || password.isEmpty()) {
            termErrorLabel.setText("Please enter both username and password.");
            termErrorLabel.setVisible(true);
            return;
        }

        // 2. Clear error
        termErrorLabel.setVisible(false);
        System.out.println("Attempting Login for Subscriber: " + username);

        // 3. Create User Object (Mock)
        // User loginUser = new User(username, password);

        // 4. Send to Server
        // Client_Controller.getInstance().sendToServer(new Message(MessageType.LOGIN_REQUEST, loginUser));
    }
}