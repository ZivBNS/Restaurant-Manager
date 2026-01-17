package gui;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import utils.DataChecker;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import entities.Bill;
import entities.LoginData;
import entities.Reservation;
import entities.UserRecord;

/**
 * Controller class for the Terminal GUI. This class manages the terminal
 * interface for user interactions such as check-in, instant booking, bill
 * payment, and reservation cancellation.
 */
public class Terminal_GUI {

    public static Terminal_GUI instance;
    private UserRecord loggedInUser = null;
    private int confiCode=0;
    private Bill currentBillToPay=null;
    private int maxDinnersTableSize=8;
    private LocalTime closeTime= LocalTime.of(23, 00); 

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

    /**
     *
     * Initializes the controller class.
     * Sets up event handlers, UI components, and initial states.
     */
    @FXML
    public void initialize() {
        instance = this;
        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
        refreshOHAndMaxCapacity();
        instDinersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 2));
        btnCancelRes.setText("CANCEL ORDER/\nEXIT WAITLIST");
        btnCancelRes.setStyle("-fx-text-alignment: center;");
        
        btnFetchBill.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        hlForgotCode.setVisible(false);

        if (lblScanBarcode != null) {
            lblScanBarcode.setOnMouseClicked(event -> {
                lblScanBarcode.setText("Waiting for scan...");
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    ConnectToServer_GUI.clientController.sendSubscriberLoginRequest(new LoginData("aaaaa", "123456"));
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
        
        // Instant Booking Button Handler
        // Fills in phone/email if logged in, otherwise clears fields
        // Also checks for closing time to disable new reservations
        btnInstantBooking.setOnAction(event -> {
            toggleForm(instantForm);
            highlightButton(btnInstantBooking);
            if (LocalTime.now().isAfter(closeTime.minusMinutes(90))) {
                for (Node node : instantForm.getChildren()) {
                    node.setVisible(false); 
                    node.setManaged(false);
                }
                instStatusLabel.setText("Closing soon: New reservations are closed for today.");
                instStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                instStatusLabel.setVisible(true);
                instStatusLabel.setManaged(true);
                return;
            }
            
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

    /**
     * Sets up the reservations table with appropriate cell value factories, styles, and formatting.
     */
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

    /**
     * Toggles the visibility of the specified form and resets other forms and fields.
     * @param formToShow The form VBox to show; if null, hides all forms.
     */
    private void toggleForm(VBox formToShow) {
    	refreshOHAndMaxCapacity();
    	VBox[] forms = {checkInForm, instantForm, payBillForm, cancelForm};
        for (VBox f : forms) f.setVisible(false);
        if (formToShow != null) formToShow.setVisible(true);
        checkInCodeField.setVisible(true);
        btnSubmitCheckIn.setVisible(true);        
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

    /**
     * Handles the submission of an instant booking request.
     * Validates input and sends the request to the server.
     */
    private void handleInstantBookingSubmit() {
        int diners = instDinersSpinner.getValue();
        String phone;
        String email;
        
        if (loggedInUser == null) {
        	phone = instPhoneField.getText().trim();
        	email = instEmailField.getText().trim();

        	if (email.isEmpty() && phone.isEmpty()) {
        		instStatusLabel.setText("Error: Please enter a phone or email");
                instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                instStatusLabel.setVisible(true);
                return;
        	}
        	
        	if (!phone.isEmpty()) {
        		if(!DataChecker.validateContactInfo(null, phone)) {
        			instStatusLabel.setText("Error: Please enter a valid phone");
                    instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    instStatusLabel.setVisible(true);
                    return;
        		}
        	}
    		if (!email.isEmpty()) {
    			if(!DataChecker.validateContactInfo(email, null)) {
        			instStatusLabel.setText("Error: Please enter a valid email");
                    instStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    instStatusLabel.setVisible(true);
                    return;
        		}
    		}

    		
        }else {
        	phone = loggedInUser.getPhone();
        	email = loggedInUser.getEmail();
        }


        try {
            Reservation instantRes = new Reservation((loggedInUser == null ? null : loggedInUser.getId()), phone, email, LocalDateTime.now(), LocalDateTime.now().plusHours(2), diners);
            ConnectToServer_GUI.clientController.sendNewInstantReservationRequest(instantRes);
        } catch (Exception e) {
            instStatusLabel.setText("Error processing request.");
        }
    }
    
    /**
	 * Handles the submission of a check-in request.
	 * Validates input and sends the request to the server.
	 * @param field The TextField containing the confirmation code.
	 * @param statusLabel The Label to display status messages.
	 */
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
    
    /**
	 * Highlights the selected button and resets styles for others.
	 * @param selected The button to highlight; if null, resets all buttons.
	 */
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
    
    /**
     * Displays the terminal view with a greeting.
     * @param name The name of the logged-in user; if null, shows a generic greeting.
     */
    private void showTerminal(String name) {
        lblUserGreeting.setText(name == null ? "Please choose an action" : "Hello, " + name);
        welcomeView.setVisible(false);
        terminalView.setVisible(true);
        welcomeErrorLabel.setVisible(false);
        toggleForm(checkInForm);
        highlightButton(btnCheckIn);
        hlForgotCode.setVisible(true);
    }
    
    /**
     * Resets the terminal to the welcome view, clearing user data and forms.
     */
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
    
    /**
     * Handles the subscriber login process.
     */
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

    /**
     * Handles the "Forgot Code" link click event.
     */
    private void handleForgotCodeClick() {
        if (loggedInUser != null) {
        	if (!todayReservationsTable.getItems().isEmpty()) {
            	toggleForm(checkInForm);
                highlightButton(btnCheckIn);
                checkInStatusLabel.setText("You are logged in. Your confirmation codes for today are listed in the table above.");
                checkInStatusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); 
                checkInStatusLabel.setVisible(true);        		
        	}
        	else {
        		toggleForm(instantForm);
                highlightButton(btnInstantBooking);
        		instStatusLabel.setText("You have no active reservations for today.");
                instStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                instStatusLabel.setVisible(true);
        	}
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

    
    /**
     * Loads a new screen based on the provided FXML file.
     * @param fxml The FXML file to load.
     */
    private void loadScreen(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ((Stage) backBtn.getScene().getWindow()).setScene(new Scene(root));
            instance=null;
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    /**
     * Resets the cancellation status label after a short delay.
     * Used to clear messages after displaying them.
     */
    public void onCancellationResponse(String response, boolean isCanceled) {
        Platform.runLater(() -> {
            cancelStatusLabel.setText(response);
            if (!isCanceled) cancelStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            else {
            	cancelStatusLabel.setStyle("-fx-text-fill: #27ae60;");
            	resetWithDelay();
            }
    
            cancelCodeField.clear(); 
            cancelStatusLabel.setVisible(true);
        });
    }
    
    /**
     * Resets the terminal view after a short delay.
     * Used after successful cancellation to return to the welcome screen.
     */
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
    
    /**
     * Handles the reception of daily reservations from the server.
     * @param reservations The list of reservations for the day.
     */
    public void onDailyReservationsReceived(List<Reservation> reservations) {
        Platform.runLater(() -> {
            String currentText = checkInStatusLabel.getText();
            boolean isSuccessMessageShown = currentText != null && 
                                          (currentText.contains("Successful") || 
                                           currentText.contains("Table Number") || 
                                           currentText.contains("Approved"));
            
            if (isSuccessMessageShown) {
                return; 
            }
            if (reservations != null && !reservations.isEmpty()) {
                reservations.sort(Comparator.comparing(Reservation::getOrderStartTime));
                todayReservationsTable.getItems().setAll(reservations);
                
                if (checkInForm.isVisible()) {
                    todayReservationsTable.setVisible(true);
                    todayReservationsTable.setManaged(true);
                    
                    checkInStatusLabel.setText("");
                    checkInStatusLabel.setVisible(false);
                }
            }
            else {
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
    
    /**
     * Resets opening hours and max table capacity from the server.
     * Used after successful operations to return to the welcome screen.
     */
    public void refreshOHAndMaxCapacity() {    	
        try{
        	maxDinnersTableSize= ConnectToServer_GUI.clientController.refreshMaxTableCapacity();
        	if (maxDinnersTableSize <= 0) {
                maxDinnersTableSize = 8;
            }
        }catch (Exception e) {
        	maxDinnersTableSize=8;
        }
        try {
            closeTime = ConnectToServer_GUI.clientController.refreshOH();
        } catch (Exception e) {
            closeTime = LocalTime.of(21, 30); // Default fallback on error
        }
        
        if (instDinersSpinner!=null && instDinersSpinner.getValueFactory() != null) {
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) instDinersSpinner.getValueFactory()).setMax(maxDinnersTableSize);
        }
    }

    /**
     *  handles the response for a failed instant reservation attempt.
     *  @param s The error message; if null, indicates waitlist proposal.
     */
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
    
    /**
     * Handles the response for a successful instant reservation attempt.
     * @param confirmationCode The confirmation code for the reservation.
     */
    public void onInstantReservationSuccessResponse(int confirmationCode) {
        Platform.runLater(() -> {
            confiCode=confirmationCode;
            toggleForm(checkInForm);
            
            todayReservationsTable.setVisible(false);
            todayReservationsTable.setManaged(false);

            highlightButton(btnCheckIn);
            checkInStatusLabel.setText("Reservation Approved! Auto-processing Check-In...");
            checkInStatusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
            checkInStatusLabel.setVisible(true);
        });
    }

    /**
 	 * Handles the response for a successful check-in attempt.
 	 * @param tableNumber The assigned table number for the check-in.
     */
    public void onCheckInSuccessResponse(int tableNumber) {
        Platform.runLater(() -> {
            toggleForm(checkInForm);
            
            todayReservationsTable.setVisible(false);
            todayReservationsTable.setManaged(false);
            checkInCodeField.setVisible(false);
            btnSubmitCheckIn.setVisible(false); 

            highlightButton(btnCheckIn);
            
            if (confiCode == 0) 
                checkInStatusLabel.setText("Check-In Successful!\nPlease proceed to Table Number: " + tableNumber);
            else 
                checkInStatusLabel.setText("Reservation Approved! Your code is: " + confiCode + "\nPlease proceed to Table Number: " + tableNumber);
            
            confiCode = 0;
            checkInStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
            checkInStatusLabel.setVisible(true);
            checkInCodeField.clear();
            
            resetWithDelay(); 
            });
    }

    /**
 	 * Handles the response for a failed check-in attempt.
 	 * @param s The error message to display.
     */
    public void onCheckInFailedResponse(String s) {
        Platform.runLater(() -> {
            checkInStatusLabel.setText(s);
            checkInStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            checkInStatusLabel.setVisible(true);
            checkInCodeField.clear();
        });
    }

    /**
 	 * Handles the response for a failed waitlist join attempt.
 	 * @param msg The error message to display.
     */
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

    /**
 	 * Handles the response for a successful waitlist join attempt.
 	 * @param content The confirmation code for the waitlist entry.
     */
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
            resetWithDelay();
        });
    }

    /**
 	 * Handles the response for a successful bill retrieval.
 	 * @param bill The retrieved Bill object.
     */
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
                String finalAmount= String.format("%.1f", (bill.calculateFinalAmount()));
                if (bill.getBillDetails()==null || bill.getBillDetails().isEmpty()) bill.setBillDetails("");
                String isUser = (currentBillToPay.getDiscountRate()!=0)? "\nUser Discount - 10%": "";
                payBillStatusLabel.setText(bill.getBillDetails() + isUser + "\n\nTotal to Pay: " + finalAmount + "₪");
                payBillStatusLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
                payBillStatusLabel.setVisible(true);
                
                billDetailsBox.setVisible(true);
                billDetailsBox.setManaged(true);
            }
        });
    }

    /**
 	 * Handles the response for a failed bill retrieval.
 	 * @param reason The reason for the failure.
     */
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

    /**
 	 * Handles the response for a bill payment attempt.
 	 * @param success True if payment was successful, false otherwise.
     */
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
                resetWithDelay();
            } else {
                payBillStatusLabel.setText("Payment Failed. Please try again or contact staff.");
                payBillStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                payBillStatusLabel.setVisible(true);
                
                billDetailsBox.setVisible(true);
                billDetailsBox.setManaged(true);
            }
        });
    }

    /**
 	 * Handles the response for a "Forgot Code" request.
 	 * @param response The response message to display.
     */
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
    
    /**
 	 * Resets the terminal view to the welcome screen.
     */
    private void resetWithDelay() {
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> resetToWelcome());
        pause.play();
    }
}