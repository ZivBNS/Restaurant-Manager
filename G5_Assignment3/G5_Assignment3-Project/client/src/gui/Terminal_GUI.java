package gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
        btnContinueAsGuest.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loggedInUser = null;
                showTerminal(null);
            }
        });

        btnWelcomeLogin.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleSubscriberLogin();
            }
        });

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

        hlForgotCode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleForgotCodeClick();
            }
        });

        btnCloseForgot.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                forgotCodeView.setVisible(false);
                
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
        hlForgotCode.setVisible(false);
    }

    private void handleInstantBookingSubmit() {
        int diners = instDinersSpinner.getValue();

        if (loggedInUser != null) {
            System.out.println("SYSO: Instant Booking requested by Subscriber: " + loggedInUser.getUsername() + " for " + diners + " diners.");
            return;
        }

        String phone = instPhoneField.getText().trim();
        String email = instEmailField.getText().trim();

        if (phone.isEmpty() && email.isEmpty()) {
            instStatusLabel.setText("Error: Provide Phone or Email!");
            instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            instStatusLabel.setVisible(true);
            return;
        }

        try {
            Reservation instantRes = new Reservation(null, phone, email, LocalDateTime.now(), LocalDateTime.now().plusHours(2), diners);
            instStatusLabel.setText("Checking availability...");
            instStatusLabel.setStyle("-fx-text-fill: #e67e22;");
            instStatusLabel.setVisible(true);
            ConnectToServer_GUI.clientController.sendNewReservationRequest(instantRes);
        } catch (Exception e) {
            instStatusLabel.setText("Error processing request.");
        }
    }

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

    private void highlightButton(Button selected) {
        Button[] btns = {btnCheckIn, btnInstantBooking, btnPayBill, btnCancelRes};
        for (Button b : btns) b.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        if (selected != null) {
            selected.setStyle("-fx-background-color: #dcdde1; -fx-background-radius: 10; -fx-border-color: #34495e; -fx-border-width: 2;");
        }
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
        instStatusLabel.setText("");
    }

    private void showTerminal(String name) {
    	System.out.println("IS THERE SUBSCRIBER? ");
    	if (name!=null) System.out.println(loggedInUser.toString());
    	else System.out.println("GUEST");
        lblUserGreeting.setText(name == null ? "Please choose an action" : "Hello, " + name);
        welcomeView.setVisible(false);
        terminalView.setVisible(true);
        welcomeErrorLabel.setVisible(false);
        toggleForm(checkInForm);
        highlightButton(btnCheckIn);
        hlForgotCode.setVisible(true);
    }

    private void resetToWelcome() {
    	loggedInUser=null;
        welcomeView.setVisible(true);
        terminalView.setVisible(false);
        welcomeUserField.clear();
        welcomePassField.clear();
        welcomeErrorLabel.setVisible(false);
        loggedInUser = null;
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

        LoginData loginData = new LoginData(username, password);
        ConnectToServer_GUI.clientController.sendSubscriberLoginRequest(loginData);
    }

    private void handleForgotCodeClick() {
        toggleForm(null); 
        highlightButton(null); 
        
        forgotCodeView.setVisible(true);
        forgotCodeView.toFront();
        
        if (loggedInUser != null) {
            forgotPhoneField.setVisible(false);
            forgotEmailField.setVisible(false);
            btnSubmitForgot.setVisible(false);
            System.out.println("SYSO: Fetching today's reservations for subscriber: " + loggedInUser.getUsername());
        } else {
            forgotPhoneField.setVisible(true);
            forgotEmailField.setVisible(true);
            btnSubmitForgot.setVisible(true);
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
        	instance = null;
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ((Stage) backBtn.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void onCancellationResponse(int i) {
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
    public void handleMessageIfLoggedIn(Subscribed_Customer msg) {
        javafx.application.Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if(msg != null) {
                    loggedInUser = msg;
                    showTerminal(loggedInUser.getFirstName());
                } else {
                    welcomeErrorLabel.setText("Invalid username or password.");
                    welcomeErrorLabel.setVisible(true);
                }
            }
        });
	}
    
	public void onInstantReservationFailedResponse() {
        Platform.runLater(() -> {
            instStatusLabel.setText(""); 

            if (waitlistProposalBox == null) {
                waitlistProposalBox = new VBox(10);
                waitlistProposalBox.setAlignment(Pos.CENTER);
                waitlistProposalBox.setStyle("-fx-background-color: #fff3cd; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #ffeeba; -fx-border-width: 1;");

                Label msg = new Label("No tables available at the moment.\nWould you like to join the Waitlist?");
                msg.setWrapText(true);
                msg.setTextAlignment(TextAlignment.CENTER);
                msg.setStyle("-fx-text-fill: #856404; -fx-font-weight: bold; -fx-font-size: 13px;");

                HBox buttonsBox = new HBox(15);
                buttonsBox.setAlignment(Pos.CENTER);

                Button btnJoin = new Button("Join Waitlist");
                btnJoin.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                btnJoin.setOnAction(e -> {
                    System.out.println("Entered Waitlist");
                    waitlistProposalBox.setVisible(false);
                    waitlistProposalBox.setManaged(false);
                });

                Button btnBack = new Button("Return");
                btnBack.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-cursor: hand;");
                btnBack.setOnAction(e -> {
                    waitlistProposalBox.setVisible(false);
                    waitlistProposalBox.setManaged(false);
                });

                buttonsBox.getChildren().addAll(btnJoin, btnBack);
                waitlistProposalBox.getChildren().addAll(msg, buttonsBox);

                instantForm.getChildren().add(waitlistProposalBox);
            }

            waitlistProposalBox.setVisible(true);
            waitlistProposalBox.setManaged(true);
        });
	}
	
	public void onInstantReservationSuccessResponse(int confirmationCodeMsg) {
		
	}
}