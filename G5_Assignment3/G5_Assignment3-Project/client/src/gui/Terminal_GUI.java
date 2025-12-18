package gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.IOException;

public class Terminal_GUI {

    @FXML private BorderPane terminalRoot;
    @FXML private AnchorPane welcomeView;
    @FXML private VBox terminalView;
    @FXML private Label lblUserGreeting, lblScanCode;
    @FXML private StackPane actionFormsContainer;

    @FXML private VBox checkInForm, instantForm, payBillForm, cancelForm, billDetailsBox;
    @FXML private Button btnCheckIn, btnInstantBooking, btnPayBill, btnCancelRes, backBtn;

    @FXML private TextField checkInCodeField, instNameField, instPhoneField, instEmailField, payBillCodeField, cancelCodeField;
    @FXML private Label checkInStatusLabel, instStatusLabel, cancelStatusLabel, payBillStatusLabel, lblBillInfo;
    @FXML private Spinner<Integer> instDinersSpinner;
    @FXML private Button btnGoToPayment, btnSubmitCheckIn, btnSubmitInstant, btnFetchBill, btnSubmitCancel;

    @FXML private TextField welcomeUserField;
    @FXML private PasswordField welcomePassField;
    @FXML private Button btnWelcomeLogin, btnContinueAsGuest;

    private Timeline inactivityTimer;

    @FXML
    public void initialize() {
        instDinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));

        // Navigation
        btnContinueAsGuest.setOnAction(e -> showTerminal(null));
        btnWelcomeLogin.setOnAction(e -> handleSubscriberLogin());

        // Form Toggling & Highlighting
        btnCheckIn.setOnAction(e -> { toggleForm(checkInForm); highlightButton(btnCheckIn); });
        btnInstantBooking.setOnAction(e -> { toggleForm(instantForm); highlightButton(btnInstantBooking); });
        btnPayBill.setOnAction(e -> { toggleForm(payBillForm); highlightButton(btnPayBill); });
        btnCancelRes.setOnAction(e -> { toggleForm(cancelForm); highlightButton(btnCancelRes); });

        // Submit Actions
        btnSubmitCheckIn.setOnAction(e -> handleStatus(checkInCodeField, checkInStatusLabel, "Success! Please go to Table 5."));
        btnSubmitInstant.setOnAction(e -> handleInstantBookingSubmit());
        btnFetchBill.setOnAction(e -> {
            if (payBillCodeField.getText().isEmpty()) {
                handleStatus(payBillCodeField, payBillStatusLabel, "");
            } else {
                billDetailsBox.setVisible(true);
                payBillStatusLabel.setVisible(false);
            }
        });
        btnSubmitCancel.setOnAction(e -> handleStatus(cancelCodeField, cancelStatusLabel, "Reservation successfully cancelled."));
        btnGoToPayment.setOnAction(e -> openCreditCardPopup());

        // Back Button
        backBtn.setOnAction(e -> {
            if (welcomeView.isVisible()) {
                loadScreen("MainScreen.fxml");
            } else {
                resetToWelcome();
            }
        });

        // Inactivity Timer
      //  setupTimer();
    }

    private void handleInstantBookingSubmit() {
        String name = instNameField.getText().trim();
        boolean hasContact = !instPhoneField.getText().trim().isEmpty() || !instEmailField.getText().trim().isEmpty();

        if (name.isEmpty()) {
            handleStatus(instNameField, instStatusLabel, "");
            return;
        }

        if (!hasContact) {
            instStatusLabel.setText("Error: Provide Phone or Email!");
            instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            instStatusLabel.setVisible(true);
        } else {
            instStatusLabel.setText("Booking Successful! Code: 8821. Wait for SMS/Email.");
            instStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            instStatusLabel.setVisible(true);
        }
    }

    private void handleStatus(TextField field, Label statusLabel, String successMsg) {
        if (field.getText().isEmpty()) {
            statusLabel.setText("Please wait... Checking data...");
            statusLabel.setStyle("-fx-text-fill: #e67e22;");
        } else {
            statusLabel.setText(successMsg);
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        }
        statusLabel.setVisible(true);
    }

    private void highlightButton(Button selected) {
        Button[] btns = {btnCheckIn, btnInstantBooking, btnPayBill, btnCancelRes};
        for (Button b : btns) b.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        selected.setStyle("-fx-background-color: #dcdde1; -fx-background-radius: 10; -fx-border-color: #34495e; -fx-border-width: 2; -fx-cursor: hand;");
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

    @SuppressWarnings("unused")
	private void setupTimer() {
        inactivityTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> resetToWelcome()));
        inactivityTimer.setCycleCount(1);
        terminalRoot.addEventFilter(MouseEvent.ANY, e -> inactivityTimer.playFromStart());
        terminalRoot.addEventFilter(KeyEvent.ANY, e -> inactivityTimer.playFromStart());
        inactivityTimer.play();
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
}