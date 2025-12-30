package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDateTime;

import entities.LoginData;
import entities.Reservation;
import entities.Subscribed_Customer;

public class Terminal_GUI {

    public static Terminal_GUI instance;
    private Subscribed_Customer loggedInUser = null; 
    private VBox waitlistProposalBox;

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
    @FXML private Label welcomeErrorLabel;

    @FXML private Hyperlink hlForgotCode;
    @FXML private VBox forgotCodeView;
    @FXML private TextField forgotPhoneField, forgotEmailField;
    @FXML private Button btnSubmitForgot, btnCloseForgot;
    @FXML private ListView<String> lvReservations;

    @FXML
    public void initialize() {
        instance = this;
        instDinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 2));
        btnCancelRes.setText("CANCEL RES/\nWAITLIST");
        btnCancelRes.setStyle("-fx-text-alignment: center;");
        hlForgotCode.setVisible(false);

        // --- Event Handlers ---
        btnContinueAsGuest.setOnAction(event -> {
            loggedInUser = null;
            showTerminal(null);
        });

        btnWelcomeLogin.setOnAction(event -> handleSubscriberLogin());

        btnCheckIn.setOnAction(event -> {
            toggleForm(checkInForm);
            highlightButton(btnCheckIn);
        });

        btnInstantBooking.setOnAction(event -> {
            toggleForm(instantForm);
            highlightButton(btnInstantBooking);
            if(loggedInUser != null) {
                instPhoneField.setText(loggedInUser.getPhone());
                instEmailField.setText(loggedInUser.getEmail());
                instPhoneField.setVisible(true);
                instEmailField.setVisible(true);
                instPhoneField.setEditable(false);
                instEmailField.setEditable(false);
                instPhoneField.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: #7f8c8d;");
                instEmailField.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: #7f8c8d;");
            } else {
                instPhoneField.clear();
                instEmailField.clear();
                instPhoneField.setVisible(true);
                instEmailField.setVisible(true);
                instPhoneField.setEditable(true);
                instEmailField.setEditable(true);
                instPhoneField.setStyle("");
                instEmailField.setStyle("");
            }
        });

        btnPayBill.setOnAction(event -> {
            toggleForm(payBillForm);
            highlightButton(btnPayBill);
        });

        btnCancelRes.setOnAction(event -> {
            toggleForm(cancelForm);
            highlightButton(btnCancelRes);
        });

        btnSubmitCheckIn.setOnAction(event -> submitCheckIn(checkInCodeField, checkInStatusLabel));
        btnSubmitInstant.setOnAction(event -> handleInstantBookingSubmit());
        btnGoToPayment.setOnAction(event -> openCreditCardPopup());
        btnCloseForgot.setOnAction(event -> forgotCodeView.setVisible(false));
        hlForgotCode.setOnAction(event -> handleForgotCodeClick());
        
        btnFetchBill.setOnAction(event -> {
            if (!payBillCodeField.getText().isEmpty()) {
                billDetailsBox.setVisible(true);
                payBillStatusLabel.setVisible(false);
            }
        });

        btnSubmitCancel.setOnAction(event -> {
            String codeText = cancelCodeField.getText().trim();
            if (codeText.isEmpty() || !codeText.matches("\\d+")) {
                cancelStatusLabel.setText("Please enter valid numeric code!");
                cancelStatusLabel.setVisible(true);
                return;
            }
            ConnectToServer_GUI.clientController.sendCancelReservationOrWaitlistRequestFromTerminal(Integer.parseInt(codeText));
        });

        backBtn.setOnAction(event -> {
            if (welcomeView.isVisible()) loadScreen("MainScreen.fxml");
            else resetToWelcome();
        });
    }

    private void toggleForm(VBox formToShow) {
        VBox[] forms = {checkInForm, instantForm, payBillForm, cancelForm};
        for (VBox f : forms) f.setVisible(false);
        if (formToShow != null) formToShow.setVisible(true);
        
        billDetailsBox.setVisible(false);
        if (forgotCodeView != null) forgotCodeView.setVisible(false);
        
        if (waitlistProposalBox != null) {
            waitlistProposalBox.setVisible(false);
            waitlistProposalBox.setManaged(false);
        }
        for (javafx.scene.Node node : instantForm.getChildren()) {
            if (node != waitlistProposalBox) {
                node.setVisible(true);
                node.setManaged(true);
            }
        }
        instStatusLabel.setText("");
    }

    private void handleInstantBookingSubmit() {
        int diners = instDinersSpinner.getValue();
        String phone = (loggedInUser == null) ? instPhoneField.getText().trim() : loggedInUser.getPhone();
        String email = (loggedInUser == null) ? instEmailField.getText().trim() : loggedInUser.getEmail();

        if (loggedInUser == null && phone.isEmpty() && email.isEmpty()) {
            instStatusLabel.setText("Error: Provide Phone or Email!");
            instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            instStatusLabel.setVisible(true);
            return;
        }

        try {
            Reservation instantRes = new Reservation((loggedInUser == null ? null : loggedInUser.getSubscriberCode()), phone, email, LocalDateTime.now(), LocalDateTime.now().plusHours(2), diners);
            instStatusLabel.setText("Checking availability...");
            instStatusLabel.setStyle("-fx-text-fill: #e67e22;");
            instStatusLabel.setVisible(true);
            ConnectToServer_GUI.clientController.sendNewInstantReservationRequest(instantRes);
        } catch (Exception e) {
            instStatusLabel.setText("Error processing request.");
        }
    }

    private void submitCheckIn(TextField field, Label statusLabel) {
    	String code = field.getText().trim();
    	if (code.isEmpty() ) {
            statusLabel.setText("Input required.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            statusLabel.setVisible(true);
            return;
        }
    	if (!code.matches("\\d+")) {
            statusLabel.setText("Code must contain numbers only!");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            statusLabel.setVisible(true);
            return;
        }
    	int confiCode=Integer.parseInt(code);
        ConnectToServer_GUI.clientController.sendCheckInRequest(confiCode);

        //statusLabel.setText(successMsg);
        //statusLabel.setStyle("-fx-text-fill: #27ae60;");
    }

    private void highlightButton(Button selected) {
        Button[] btns = {btnCheckIn, btnInstantBooking, btnPayBill, btnCancelRes};
        for (Button b : btns) b.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        if (selected != null) {
            selected.setStyle("-fx-background-color: #dcdde1; -fx-background-radius: 10; -fx-border-color: #34495e; -fx-border-width: 2;");
        }
    }

    private void showTerminal(String name) {
        lblUserGreeting.setText(name == null ? "Please choose an action" : "Hello, " + name);
        welcomeView.setVisible(false);
        terminalView.setVisible(true);
        welcomeErrorLabel.setVisible(false);
        toggleForm(checkInForm);
        highlightButton(btnCheckIn);
        hlForgotCode.setVisible(true);
    }

    private void resetToWelcome() {
        loggedInUser = null;
        welcomeView.setVisible(true);
        terminalView.setVisible(false);
        welcomeUserField.clear();
        welcomePassField.clear();
        welcomeErrorLabel.setVisible(false);
        hlForgotCode.setVisible(false);
    }

    private void handleSubscriberLogin() {
        String username = welcomeUserField.getText().trim();
        String password = welcomePassField.getText().trim();
        if (username.isEmpty() || password.isEmpty() || username.length() > 20 || password.length() > 20) {
            welcomeErrorLabel.setText("Invalid username or password.");
            welcomeErrorLabel.setVisible(true);
            return;
        }
        ConnectToServer_GUI.clientController.sendSubscriberLoginRequest(new LoginData(username, password));
    }

    private void handleForgotCodeClick() {
        toggleForm(null); 
        highlightButton(null); 
        forgotCodeView.setVisible(true);
        forgotCodeView.toFront();
        boolean isSubscriber = (loggedInUser != null);
        forgotPhoneField.setVisible(!isSubscriber);
        forgotEmailField.setVisible(!isSubscriber);
        btnSubmitForgot.setVisible(!isSubscriber);
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
            instance = null;
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ((Stage) backBtn.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void onCancellationResponse(int i) {
        if (i == 3) {
            cancelStatusLabel.setText("Your Reservation is canceled");
            cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            cancelCodeField.clear(); 
        } else if (i == 1) {
            cancelStatusLabel.setText("Your Waitlist is canceled");
            cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            cancelCodeField.clear(); 
        } else {
            cancelStatusLabel.setText("Operation failed or wrong code");
            cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
        cancelStatusLabel.setVisible(true);
    }
    
    public void handleMessageIfLoggedIn(Subscribed_Customer msg) {
        Platform.runLater(() -> {
            if (msg != null) {
                loggedInUser = msg;
                showTerminal(loggedInUser.getFirstName());
            } else {
                welcomeErrorLabel.setText("Invalid username or password.");
                welcomeErrorLabel.setVisible(true);
            }
        });
    }

    public void onInstantReservationFailedResponse(String s) {
        Platform.runLater(() -> {
            highlightButton(null);
            for (javafx.scene.Node node : instantForm.getChildren()) {
                if (node != waitlistProposalBox) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
            if (waitlistProposalBox == null) {
                waitlistProposalBox = new VBox(15);
                waitlistProposalBox.setAlignment(Pos.CENTER);
                waitlistProposalBox.setStyle("-fx-background-color: #fff3cd; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #ffeeba; -fx-border-width: 2;");
                Label msgLabel = new Label("No tables available at the moment.\nWould you like to join the Waitlist?");
                msgLabel.setWrapText(true);
                msgLabel.setTextAlignment(TextAlignment.CENTER);
                msgLabel.setStyle("-fx-text-fill: #856404; -fx-font-weight: bold; -fx-font-size: 15px;");
                
                HBox buttonsBox = new HBox(15);
                buttonsBox.setAlignment(Pos.CENTER);
                Button btnJoin = new Button("Join Waitlist");
                btnJoin.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 20;");
                
                Button btnBack = new Button("Return");
                btnBack.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20;");
                btnBack.setOnAction(e -> {
                    toggleForm(instantForm);
                    instPhoneField.clear();
                    instEmailField.clear();
                    instDinersSpinner.getValueFactory().setValue(2);
                    highlightButton(btnInstantBooking);
                });

                buttonsBox.getChildren().addAll(btnJoin, btnBack);
                waitlistProposalBox.getChildren().addAll(msgLabel, buttonsBox);
                instantForm.getChildren().add(waitlistProposalBox);
            }
            waitlistProposalBox.setVisible(true);
            waitlistProposalBox.setManaged(true);
        });
    }
    
    public void onInstantReservationSuccessResponse(int confirmationCodeMsg) {
        Platform.runLater(() -> {
            toggleForm(instantForm);
            instStatusLabel.setText("Reservation Approved! your Confirmation Code is: " + confirmationCodeMsg+".\nInsert the code in the check-in window to complete check-in");
            instStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
            instStatusLabel.setVisible(true);
            if (loggedInUser == null) {
                instPhoneField.clear();
                instEmailField.clear();
            }
        });
    }

    public void onCheckInSuccessResponse(int tableNumber) {
        Platform.runLater(() -> {
            checkInStatusLabel.setText("Check-In Successful! Please proceed to Table Number: " + tableNumber);
            checkInStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
            checkInStatusLabel.setVisible(true);
            checkInCodeField.clear();
            System.out.println("[Terminal] Check-in success. Assigned Table: " + tableNumber);
        });
    }

	public void onCheckInFailedResponse(String s) {
		checkInStatusLabel.setText("Check-In FAILED");
        checkInStatusLabel.setVisible(true);
        checkInCodeField.clear();
        System.out.println("[Terminal] Check-in FAIL");

	}
}