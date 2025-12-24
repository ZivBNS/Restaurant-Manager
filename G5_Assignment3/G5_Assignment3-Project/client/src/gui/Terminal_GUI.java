package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.time.LocalDateTime;

import controllers.Client_Controller;
import entities.Reservation;

/**
 * Controller for the Restaurant Terminal (Self-Service Kiosk).
 * Handles check-ins, instant bookings, bill payments, and cancellations.
 */
public class Terminal_GUI {

    public static Terminal_GUI instance;

    @FXML private BorderPane terminalRoot;
    @FXML private AnchorPane welcomeView;
    @FXML private VBox terminalView;
    @FXML private Label lblUserGreeting;
    @FXML private StackPane actionFormsContainer;

    @FXML private VBox checkInForm, instantForm, payBillForm, cancelForm, billDetailsBox;
    @FXML private Button btnCheckIn, btnInstantBooking, btnPayBill, btnCancelRes, backBtn;

    @FXML private TextField checkInCodeField, instPhoneField, instEmailField, payBillCodeField, cancelCodeField;
    @FXML private Label checkInStatusLabel, instStatusLabel, cancelStatusLabel, payBillStatusLabel;
    @FXML private Spinner<Integer> instDinersSpinner;
    @FXML private Button btnGoToPayment, btnSubmitCheckIn, btnSubmitInstant, btnFetchBill, btnSubmitCancel;

    @FXML private TextField welcomeUserField;
    @FXML private PasswordField welcomePassField;
    @FXML private Button btnWelcomeLogin, btnContinueAsGuest;

    /**
     * Initializes the terminal controller. 
     * Sets up event handlers and UI defaults.
     */
    @FXML
    public void initialize() {
        instance = this;
        instDinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 2));

        // Navigation Handlers using Anonymous Inner Classes
        btnContinueAsGuest.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showTerminal(null);
            }
        });

        btnWelcomeLogin.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleSubscriberLogin();
            }
        });

        // Form Toggling Handlers
        btnCheckIn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                toggleForm(checkInForm);
                highlightButton(btnCheckIn);
            }
        });

        btnInstantBooking.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                toggleForm(instantForm);
                highlightButton(btnInstantBooking);
            }
        });

        btnPayBill.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                toggleForm(payBillForm);
                highlightButton(btnPayBill);
            }
        });

        btnCancelRes.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                toggleForm(cancelForm);
                highlightButton(btnCancelRes);
            }
        });

        // Submission Logic Handlers
        btnSubmitCheckIn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleStatus(checkInCodeField, checkInStatusLabel, "Success! Welcome to our restaurant.");
            }
        });

        btnSubmitInstant.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleInstantBookingSubmit();
            }
        });

        btnFetchBill.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!payBillCodeField.getText().isEmpty()) {
                    billDetailsBox.setVisible(true);
                    payBillStatusLabel.setVisible(false);
                }
            }
        });

        btnSubmitCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String codeText = cancelCodeField.getText().trim();
                if (codeText.isEmpty()) {
                    cancelStatusLabel.setText("Please enter confirmation code!");
                    cancelStatusLabel.setVisible(true);
                    return;
                }
                if (!codeText.matches("\\d+")) {
                    cancelStatusLabel.setText("Error: Code must contain numbers only!");
                    cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    cancelStatusLabel.setVisible(true);
                    return;
                }
                try {
                    int code = Integer.parseInt(codeText);            
                    ConnectToServer_GUI.clientController.sendCancelReservationOrWaitlistRequestFromTerminal(code);
                } catch (NumberFormatException e) {
                    cancelStatusLabel.setText("ERROR - try again");
                    cancelStatusLabel.setVisible(true);
                }
            }
        });

        btnGoToPayment.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                openCreditCardPopup();
            }
        });

        backBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (welcomeView.isVisible()) loadScreen("MainScreen.fxml");
                else resetToWelcome();
            }
        });
    }

    /**
     * Logic for walk-in customers to book a table for "Now".
     * Requires at least one contact method (Phone or Email).
     */
    private void handleInstantBookingSubmit() {
        String phone = instPhoneField.getText().trim();
        String email = instEmailField.getText().trim();
        int diners = instDinersSpinner.getValue();

        // Validate: At least one contact method required
        if (phone.isEmpty() && email.isEmpty()) {
            instStatusLabel.setText("Error: Provide Phone or Email!");
            instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            instStatusLabel.setVisible(true);
            return;
        }

        try {
            // Create a reservation starting "Now"
            Reservation instantRes = new Reservation(null, phone, email, LocalDateTime.now(), LocalDateTime.now().plusHours(2), diners);
            
            instStatusLabel.setText("Checking availability...");
            instStatusLabel.setStyle("-fx-text-fill: #e67e22;");
            instStatusLabel.setVisible(true);
            
            // Send request to server
            ConnectToServer_GUI.clientController.sendNewReservationRequest(instantRes);
            
        } catch (Exception e) {
            instStatusLabel.setText("Error processing request.");
        }
    }

    /**
     * Updates the status label for generic operations.
     */
    private void handleStatus(TextField field, Label statusLabel, String successMsg) {
        if (field.getText().isEmpty()) {
            statusLabel.setText("Input required.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        } else {
            statusLabel.setText(successMsg);
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        }
        statusLabel.setVisible(true);
    }

    /**
     * Highlights the active action button in the terminal.
     * @param selected The button to highlight.
     */
    private void highlightButton(Button selected) {
        Button[] btns = {btnCheckIn, btnInstantBooking, btnPayBill, btnCancelRes};
        for (Button b : btns) b.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        selected.setStyle("-fx-background-color: #dcdde1; -fx-background-radius: 10; -fx-border-color: #34495e; -fx-border-width: 2;");
    }

    private void toggleForm(VBox formToShow) {
        VBox[] forms = {checkInForm, instantForm, payBillForm, cancelForm};
        for (VBox f : forms) f.setVisible(false);
        formToShow.setVisible(true);
        billDetailsBox.setVisible(false);
    }

    private void showTerminal(String name) {
        lblUserGreeting.setText(name == null ? "Please choose an action" : "Hello, " + name);
        welcomeView.setVisible(false);
        terminalView.setVisible(true);
        toggleForm(checkInForm);
        highlightButton(btnCheckIn);
    }

    private void resetToWelcome() {
        welcomeView.setVisible(true);
        terminalView.setVisible(false);
        welcomeUserField.clear();
        welcomePassField.clear();
    }

    private void handleSubscriberLogin() {
        if (!welcomeUserField.getText().isEmpty() && !welcomePassField.getText().isEmpty()) {
            showTerminal(welcomeUserField.getText());
        }
    }

    private void openCreditCardPopup() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("CreditCardPopup.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadScreen(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ((Stage) backBtn.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

	public void onCancellationResponse(int i) {
	       // WAITLIST_CANCELED_FAILED - 0
	       /// WAITLIST_CANCELED - 1
	       // RESERVATION_CANCEL_FAILED - 2
	       // RESERVATION_CANCELED - 3
		if (i==3) {
	        cancelStatusLabel.setText("Your Reservation is canceled");
	        cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
	        cancelCodeField.clear(); 
	    }
		else if (i==1) {
	        cancelStatusLabel.setText("Your Waitlist is canceled");
	        cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
	        cancelCodeField.clear(); 
	    } 
		else if (i==0){
	        cancelStatusLabel.setText("The waitlist is no longer exist in the system, or wrong code");
	        cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
	    }
		else {
			cancelStatusLabel.setText("The reservation is no longer exist in the system, or wrong code");
	        cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
	    }
		cancelStatusLabel.setVisible(true);
	}
}