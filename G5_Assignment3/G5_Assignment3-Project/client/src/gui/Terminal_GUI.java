package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;

import entities.Bill;
import entities.LoginData;
import entities.Reservation;
import entities.UserRecord;

public class Terminal_GUI {

    public static Terminal_GUI instance;
    private UserRecord loggedInUser = null;
    private int confiCode=0;
    private Bill currentBillToPay=null; 

    @FXML private BorderPane terminalRoot;
    @FXML private AnchorPane welcomeView;
    @FXML private VBox terminalView;
    @FXML private Label lblUserGreeting;
    @FXML private StackPane actionFormsContainer;

    @FXML private VBox checkInForm, instantForm, payBillForm, cancelForm, billDetailsBox;
    
    @FXML private VBox waitlistProposalBox; 
    @FXML private Button btnJoinWaitlist, btnReturnWaitlist;

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
        btnCancelRes.setText("CANCEL ORDER/\nEXIT WAITLIST");
        btnCancelRes.setStyle("-fx-text-alignment: center;");
        hlForgotCode.setVisible(false);

        if (waitlistProposalBox != null) {
            waitlistProposalBox.setVisible(false);
            waitlistProposalBox.setManaged(false);
        }

        if (btnReturnWaitlist != null) {
            btnReturnWaitlist.setOnAction(event -> {
                waitlistProposalBox.setVisible(false);
                waitlistProposalBox.setManaged(false);
                
                for (Node node : instantForm.getChildren()) {
                    if (node != waitlistProposalBox) {
                        node.setVisible(true);
                        node.setManaged(true);
                    }
                }
                
                if (loggedInUser == null) {
                    instPhoneField.clear();
                    instEmailField.clear();
                }
                instDinersSpinner.getValueFactory().setValue(2);
                highlightButton(btnInstantBooking);
            });
        }

        if (btnJoinWaitlist != null) {
            btnJoinWaitlist.setOnAction(event -> {
                int diners = instDinersSpinner.getValue();
                String phone, email;
                Integer userId = null;

                if (loggedInUser != null) {
                    phone = loggedInUser.getPhone();
                    email = loggedInUser.getEmail();
                    userId = loggedInUser.getId(); 
                } else {
                    phone = instPhoneField.getText().trim();
                    email = instEmailField.getText().trim();
                }
                Reservation waitlistReq = new Reservation(userId,phone,email,LocalDateTime.now(),LocalDateTime.now().plusHours(2),diners);
                ConnectToServer_GUI.clientController.sendJoinWaitlistRequest(waitlistReq);
                waitlistProposalBox.setVisible(false);
                waitlistProposalBox.setManaged(false);
            });
        }

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
            
            if (waitlistProposalBox != null) {
                waitlistProposalBox.setVisible(false);
                waitlistProposalBox.setManaged(false);
            }
            
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
        
        btnGoToPayment.setOnAction(event -> {
        	if (currentBillToPay != null) {
        		ConnectToServer_GUI.clientController.sendPayBillRequest(currentBillToPay);
        	}
        });
        
        btnCloseForgot.setOnAction(event -> forgotCodeView.setVisible(false));
        hlForgotCode.setOnAction(event -> handleForgotCodeClick());
        
        btnFetchBill.setOnAction(event -> {
            String codeStr = payBillCodeField.getText().trim();            
            if (codeStr.isEmpty()) {
                payBillStatusLabel.setText("Please enter a code.");
                payBillStatusLabel.setVisible(true);
                return;
            }

            try {
                int code = Integer.parseInt(codeStr);
                ConnectToServer_GUI.clientController.sendGetBillRequest(code);
                payBillStatusLabel.setVisible(false);
            } catch (NumberFormatException e) {
                payBillStatusLabel.setText("Code must be a number.");
                payBillStatusLabel.setVisible(true);
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
        billDetailsBox.setManaged(false);
        payBillCodeField.setVisible(true);
        payBillCodeField.setManaged(true);
        btnFetchBill.setVisible(true);
        btnFetchBill.setManaged(true);
        payBillCodeField.clear();
        payBillCodeField.setEditable(true);
        currentBillToPay = null;
        
        if (forgotCodeView != null) forgotCodeView.setVisible(false);
        
        if (waitlistProposalBox != null) {
            waitlistProposalBox.setVisible(false);
            waitlistProposalBox.setManaged(false);
        }

        for (Node node : instantForm.getChildren()) {
            if (node != waitlistProposalBox) {
                node.setVisible(true);
                node.setManaged(true);
            }
        }
        
        instStatusLabel.setText("");
        checkInStatusLabel.setText("");
        payBillStatusLabel.setText("");
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
            Reservation instantRes = new Reservation((loggedInUser == null ? null : loggedInUser.getId()), phone, email, LocalDateTime.now(), LocalDateTime.now().plusHours(2), diners);
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
        int confiCode = Integer.parseInt(code);
        ConnectToServer_GUI.clientController.sendCheckInRequest(confiCode);
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

    private void loadScreen(String fxml) {
        try {
            instance = null;
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ((Stage) backBtn.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void onCancellationResponse(String response) {
    	if (response==null|| response.equals("error")) {
    		cancelStatusLabel.setText("Operation failed or wrong code");
    		cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
    	}
    	else if (response.equals("approved r")) {
            cancelStatusLabel.setText("Your Reservation is canceled");
            cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            cancelCodeField.clear(); 
        } else if (response.equals("approved w")) {
            cancelStatusLabel.setText("Your Waitlist is canceled");
            cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            cancelCodeField.clear(); 
        }
        else {
            cancelStatusLabel.setText("The "+ response +" is not longer exist in the system");
            cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");        	
        }
        cancelStatusLabel.setVisible(true);
    }
    
    public void handleMessageIfLoggedIn(UserRecord userRecord) {
        Platform.runLater(() -> {
            if (userRecord != null) {
                loggedInUser = userRecord;
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
            
            if (waitlistProposalBox != null) {
                waitlistProposalBox.setVisible(true);
                waitlistProposalBox.setManaged(true);
            }
        });
    }
    
    public void onInstantReservationSuccessResponse(int confirmationCode) {
        Platform.runLater(() -> {
    		confiCode=confirmationCode;
            toggleForm(checkInForm);
            highlightButton(btnCheckIn);
            checkInStatusLabel.setText("Reservation Approved! Auto-processing Check-In...");
            checkInStatusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
            checkInStatusLabel.setVisible(true);
        });
    }

    public void onCheckInSuccessResponse(int tableNumber) {
        Platform.runLater(() -> {
            toggleForm(checkInForm);
            highlightButton(btnCheckIn);
            if (confiCode==0) checkInStatusLabel.setText( "Check-In Successful!" + "\nPlease proceed to Table Number: " + tableNumber);
            else checkInStatusLabel.setText("Reservation Approved! your code is: " + confiCode + "\nPlease proceed to Table Number: " + tableNumber);
            confiCode=0;
            checkInStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
            checkInStatusLabel.setVisible(true);
            checkInCodeField.clear();
        });
    }

    public void onCheckInFailedResponse(String s) {
        Platform.runLater(() -> {
            checkInStatusLabel.setText("Check-In FAILED");
            checkInStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            checkInStatusLabel.setVisible(true);
            checkInCodeField.clear();
        });
    }

    public void onJoinWaitlistFailedResponse(String msg) {
        Platform.runLater(() -> {
            if (waitlistProposalBox != null) {
                waitlistProposalBox.setVisible(false);
                waitlistProposalBox.setManaged(false);
            }            
            for (Node node : instantForm.getChildren()) {
                if (node != waitlistProposalBox) {
                    node.setVisible(true);
                    node.setManaged(true);
                }
            }
            instStatusLabel.setText((msg!=null)? msg : "System Error, refresh the page");
            instStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); 
            instStatusLabel.setVisible(true);
        });
    }

    public void onJoinWaitlistSucceedResponse(int content) {
        Platform.runLater(() -> {
            if (waitlistProposalBox != null) {
                waitlistProposalBox.setVisible(false);
                waitlistProposalBox.setManaged(false);
            }

            for (Node node : instantForm.getChildren()) {
                if (node != waitlistProposalBox && node != instStatusLabel) {
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
            if (loggedInUser == null) {
                instPhoneField.clear();
                instEmailField.clear();
            }
            String successMsg = "You have successfully joined the Waitlist!\n" +
                                "Your Confirmation Code is: " + content + "\n" +
                                "We will notify you immediately when a table becomes free.\n" +
                                "Please keep this code safe for check-in.";
            
            instStatusLabel.setText(successMsg);
            instStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 15px;");
            instStatusLabel.setVisible(true);
            instStatusLabel.setManaged(true); 
        });
    }

    public void onGetBillSuccess(Bill bill) {
        Platform.runLater(() -> {
            this.currentBillToPay = bill;
            double total = bill.calculateFinalAmount()+10;

            payBillCodeField.setVisible(false);
            payBillCodeField.setManaged(false);
            btnFetchBill.setVisible(false);
            btnFetchBill.setManaged(false);

            if (total == 0) {
                payBillStatusLabel.setText("No payment needed. Thanks for checking out.");
                payBillStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
                payBillStatusLabel.setVisible(true);
                billDetailsBox.setVisible(false); 
                billDetailsBox.setManaged(false);
            } else {
            	bill.setBillDetails("Pancakes - 4$");
                String details = (bill.getBillDetails() == null) ? "" : bill.getBillDetails();
                payBillStatusLabel.setText(details + "\nSitting - 3$\nTips - 3$" + "\n\nTotal to Pay: " + total + "$");
                payBillStatusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
                payBillStatusLabel.setVisible(true);
                
                billDetailsBox.setVisible(true);
                billDetailsBox.setManaged(true);
            }
        });
    }

    public void onGetBillFailure(String reason) {
        Platform.runLater(() -> {
            billDetailsBox.setVisible(false);
            billDetailsBox.setManaged(false);
            payBillStatusLabel.setText(reason);
            payBillStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            payBillStatusLabel.setVisible(true);
            
            payBillCodeField.setVisible(true);
            payBillCodeField.setManaged(true);
            btnFetchBill.setVisible(true);
            btnFetchBill.setManaged(true);
        });
    }

    public void onPaymentSuccessResponse(boolean success) {
        Platform.runLater(() -> {
            if (success) {
                billDetailsBox.setVisible(false);
                billDetailsBox.setManaged(false);
                
                payBillCodeField.clear();
                payBillCodeField.setVisible(true);
                payBillCodeField.setManaged(true);
                btnFetchBill.setVisible(true);
                btnFetchBill.setManaged(true);
                
                payBillStatusLabel.setText("Payment Successful! Thank you for dining with us.");
                payBillStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
                payBillStatusLabel.setVisible(true);
                
                currentBillToPay = null;
                
            } else {
                payBillStatusLabel.setText("Payment Failed. Please try again or contact staff.");
                payBillStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                payBillStatusLabel.setVisible(true);
                
                billDetailsBox.setVisible(true);
                billDetailsBox.setManaged(true);
            }
        });
    }
}