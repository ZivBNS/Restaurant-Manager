package gui;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import entities.Bill;
import entities.LoginData;
import entities.Reservation;
import entities.UserRecord;

public class Terminal_GUI {

    public static Terminal_GUI instance;
    private UserRecord loggedInUser = null;
    private int confiCode=0;
    private Bill currentBillToPay=null;
    private Time closeTime=null; 
    private int maxDinnersTableSize=0;

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
    @FXML private Label forgotStatusLabel;
    
    @FXML private TableView<Reservation> todayReservationsTable;
    @FXML private TableColumn<Reservation, Integer> colCode;
    @FXML private TableColumn<Reservation, Integer> colDiners;
    @FXML private TableColumn<Reservation, LocalDateTime> colStart;
    @FXML private TableColumn<Reservation, LocalDateTime> colEnd;
    
    @FXML private Label lblScanBarcode;

    @FXML
    public void initialize() {
        instance = this;
        instDinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 2));
        btnCancelRes.setText("CANCEL ORDER/\nEXIT WAITLIST");
        btnCancelRes.setStyle("-fx-text-alignment: center;");
        
        btnFetchBill.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        hlForgotCode.setVisible(false);

        if (lblScanBarcode != null) {
            lblScanBarcode.setOnMouseClicked(event -> {
                lblScanBarcode.setText("Waiting for scan...");
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    ConnectToServer_GUI.clientController.sendSubscriberLoginRequest(new LoginData("a", "1"));
                    lblScanBarcode.setText("Scan Barcode");
                });
                pause.play();
            });
        }

        setupReservationsTable();

        todayReservationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                checkInCodeField.setText(String.valueOf(newVal.getConfirmationCode()));
            }
        });

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
                instStatusLabel.setText("");
                instStatusLabel.setVisible(false);
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
        btnSubmitForgot.setOnAction(event -> {
            forgotPhoneField.setStyle("");
            forgotEmailField.setStyle("");
            if (forgotStatusLabel != null) {
                forgotStatusLabel.setVisible(false);
            }
            String phone = forgotPhoneField.getText().trim();
            String email = forgotEmailField.getText().trim();

            if (phone.isEmpty() && email.isEmpty()) {
                    forgotPhoneField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 5;");
                    forgotEmailField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 5;");
                if (forgotStatusLabel != null) {
                    forgotStatusLabel.setText("Please provide at least one: Phone or Email!");
                    forgotStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); 
                    forgotStatusLabel.setVisible(true);
                }
                return;
            }
            ConnectToServer_GUI.clientController.sendRecoverCodesRequest(phone, email);
            });
        
        btnGoToPayment.setOnAction(event -> {
            if (currentBillToPay != null) {
                ConnectToServer_GUI.clientController.sendPayBillRequest(currentBillToPay);
            }
        });
        
        btnCloseForgot.setOnAction(event -> {
            forgotCodeView.setVisible(false);
            forgotPhoneField.clear();
            forgotEmailField.clear();
            forgotStatusLabel.setText("");
            forgotPhoneField.setStyle("");
            forgotEmailField.setStyle("");
            if (forgotStatusLabel != null) {
                forgotStatusLabel.setVisible(false);
            }
            toggleForm(checkInForm);
            highlightButton(btnCheckIn);
        });
        
        hlForgotCode.setOnAction(event -> handleForgotCodeClick());
        
        btnFetchBill.setOnAction(event -> {
            String codeStr = payBillCodeField.getText().trim();            
            if (codeStr.isEmpty()) {
                payBillStatusLabel.setText("Please enter a code.");
                payBillStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                payBillStatusLabel.setVisible(true);
                return;
            }

            try {
                int code = Integer.parseInt(codeStr);
                ConnectToServer_GUI.clientController.sendGetBillRequest(code);
                payBillStatusLabel.setVisible(false);
            } catch (NumberFormatException e) {
                payBillStatusLabel.setText("Code must be a number.");
                payBillStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                payBillStatusLabel.setVisible(true);
            }
        });

        btnSubmitCancel.setOnAction(event -> {
            String codeText = cancelCodeField.getText().trim();
            if (codeText.isEmpty() || !codeText.matches("\\d+")) {
                cancelStatusLabel.setText("Please enter a code.");
                cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); 
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

    private void setupReservationsTable() {        
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colDiners.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("orderStartTime"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("orderEndTime"));

        colCode.setStyle("-fx-alignment: CENTER;");
        colDiners.setStyle("-fx-alignment: CENTER;");
        colStart.setStyle("-fx-alignment: CENTER;");
        colEnd.setStyle("-fx-alignment: CENTER;");
        
        for (TableColumn<?, ?> col : todayReservationsTable.getColumns()) {
            col.setResizable(false);
            col.setReorderable(false);
            col.setSortable(false);
        }

        colCode.prefWidthProperty().bind(todayReservationsTable.widthProperty().divide(4));
        colDiners.prefWidthProperty().bind(todayReservationsTable.widthProperty().divide(4));
        colStart.prefWidthProperty().bind(todayReservationsTable.widthProperty().divide(4));
        colEnd.prefWidthProperty().bind(todayReservationsTable.widthProperty().divide(4));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        colStart.setCellFactory(column -> new TableCell<Reservation, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(timeFormatter));
                }
            }
        });

        colEnd.setCellFactory(column -> new TableCell<Reservation, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(timeFormatter));
                }
            }
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
        forgotStatusLabel.setText("");
        checkInCodeField.clear();
        cancelCodeField.clear();
        
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
        
        if (formToShow == checkInForm) {
            if (loggedInUser == null) {
                todayReservationsTable.setVisible(false);
                todayReservationsTable.setManaged(false);
            } else {
                if (!todayReservationsTable.getItems().isEmpty()) {
                    todayReservationsTable.setVisible(true);
                    todayReservationsTable.setManaged(true);
                }
            }
        }
        
        instStatusLabel.setText("");
        instStatusLabel.setVisible(false);
        checkInStatusLabel.setText("");
        payBillStatusLabel.setText("");
        cancelStatusLabel.setText("");
        cancelStatusLabel.setVisible(false);
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
        
        for (Button b : btns) {
            b.setScaleX(1.0);
            b.setScaleY(1.0);
            
            if (selected != null && b != selected) {
                b.setOpacity(0.6); 
                b.setEffect(null); 
                b.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-width: 0;");
            } else {
                b.setOpacity(1.0); 
                if (selected == null) {
                    b.setEffect(new javafx.scene.effect.DropShadow(5, javafx.scene.paint.Color.rgb(0, 0, 0, 0.2)));
                    b.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-width: 0;");
                }
            }
        }

        if (selected != null) {
            selected.setScaleX(1.05);
            selected.setScaleY(1.05);
            
            String colorHex = "#34495e"; 
            if (selected == btnCheckIn) colorHex = "#27ae60";        
            else if (selected == btnInstantBooking) colorHex = "#e67e22"; 
            else if (selected == btnPayBill) colorHex = "#2980b9";   
            else if (selected == btnCancelRes) colorHex = "#c0392b"; 

            selected.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; " +
                              "-fx-border-color: " + colorHex + "; " +
                              "-fx-border-width: 3; -fx-border-radius: 10;");
            
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(javafx.scene.paint.Color.web(colorHex, 0.6)); 
            glow.setRadius(20); 
            glow.setSpread(0.2); 
            selected.setEffect(glow);
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
        todayReservationsTable.getItems().clear();
        todayReservationsTable.setVisible(false);
        todayReservationsTable.setManaged(false);
        
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
        if (loggedInUser != null) {
            toggleForm(checkInForm);
            highlightButton(btnCheckIn);
            checkInStatusLabel.setText("You are logged in. Your confirmation codes for today are listed in the table above.");
            checkInStatusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); 
            checkInStatusLabel.setVisible(true);
            return;
        }

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

    public void onCancellationResponse(String response, boolean isCanceled) {
        Platform.runLater(() -> {
            cancelStatusLabel.setText(response);
            if (!isCanceled) cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            else cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
    
            cancelCodeField.clear(); 
            cancelStatusLabel.setVisible(true);
        });
    }
    
    public void handleMessageIfLoggedIn(UserRecord userRecord) {
        Platform.runLater(() -> {
            if (userRecord != null) {
                loggedInUser = userRecord;
                showTerminal(loggedInUser.getFirstName());
                ConnectToServer_GUI.clientController.sendGetDailyReservationsRequest(loggedInUser.getId());
            } else {
                welcomeErrorLabel.setText("Invalid username or password.");
                welcomeErrorLabel.setStyle("-fx-text-fill: #e74c3c;");
                welcomeErrorLabel.setVisible(true);
            }
        });
    }
    
    public void onDailyReservationsReceived(List<Reservation> reservations) {
        Platform.runLater(() -> {
            if (reservations != null && !reservations.isEmpty()) {
                reservations.sort(Comparator.comparing(Reservation::getOrderStartTime));
                
                todayReservationsTable.getItems().setAll(reservations);
                if (checkInForm.isVisible()) {
                    todayReservationsTable.setVisible(true);
                    todayReservationsTable.setManaged(true);
                    
                    checkInStatusLabel.setText("");
                    checkInStatusLabel.setVisible(false);
                }
            } else {
                todayReservationsTable.setVisible(false);
                todayReservationsTable.setManaged(false);
                
                if (checkInForm.isVisible()) {
                    checkInStatusLabel.setText("You have no active reservations for today.");
                    checkInStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
                    checkInStatusLabel.setVisible(true);
                }
            }
        });
    }

    public void getRefreshedOPAndMaxCapacity() {
    	ConnectToServer_GUI.clientController.refreshOH();
    	ConnectToServer_GUI.clientController.refreshMaxTableCapacity();
    }

    
    public void onInstantReservationFailedResponse(String s) {
        Platform.runLater(() -> {
            if (s == null) {
                highlightButton(null);
                
                for (Node node : instantForm.getChildren()) {
                    if (node != waitlistProposalBox) {
                        node.setVisible(false);
                        node.setManaged(false);
                    }
                }
                if (waitlistProposalBox != null) {
                    waitlistProposalBox.setVisible(true);
                    waitlistProposalBox.setManaged(true);
                }
            }
            else {
                instStatusLabel.setText(s);
                instStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                instStatusLabel.setVisible(true);
                instStatusLabel.setManaged(true);
                if (waitlistProposalBox != null) {
                    waitlistProposalBox.setVisible(false);
                    waitlistProposalBox.setManaged(false);
                }
            }
        });
    }
    
    public void onInstantReservationSuccessResponse(int confirmationCode) {
        Platform.runLater(() -> {
            confiCode=confirmationCode;
            toggleForm(checkInForm);
            
            todayReservationsTable.setVisible(false);
            todayReservationsTable.setManaged(false);

            if (loggedInUser != null) {
                ConnectToServer_GUI.clientController.sendGetDailyReservationsRequest(loggedInUser.getId());
            }

            highlightButton(btnCheckIn);
            checkInStatusLabel.setText("Reservation Approved! Auto-processing Check-In...");
            checkInStatusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
            checkInStatusLabel.setVisible(true);
        });
    }

    public void onCheckInSuccessResponse(int tableNumber) {
        Platform.runLater(() -> {
            toggleForm(checkInForm);
            
            todayReservationsTable.setVisible(false);
            todayReservationsTable.setManaged(false);
            
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
            checkInStatusLabel.setText(s);
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
            payBillCodeField.setVisible(false);
            payBillCodeField.setManaged(false);
            btnFetchBill.setVisible(false);
            btnFetchBill.setManaged(false);

            if (bill.calculateFinalAmount() == 0) {
                payBillStatusLabel.setText("No payment needed. Thanks for checking out.");
                payBillStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
                payBillStatusLabel.setVisible(true);
                billDetailsBox.setVisible(false); 
                billDetailsBox.setManaged(false);
            } else {
                if(bill.getBillDetails()==null||bill.getBillDetails().isEmpty()) bill.setBillDetails("Pancakes - 4$");
                payBillStatusLabel.setText(bill.getBillDetails() + "\nSitting - 3$\nTips - 3$" + "\n\nTotal to Pay: " + (bill.calculateFinalAmount()+6) + "$");
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

    public void onForgotCodeHandle(String response, boolean isFound) {
        Platform.runLater(() -> {
            forgotStatusLabel.setText(response);
            forgotStatusLabel.setVisible(true);
            forgotStatusLabel.setManaged(true);
            if (isFound) forgotStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
            else forgotStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
            forgotPhoneField.clear();
            forgotEmailField.clear();
        });
    }
}