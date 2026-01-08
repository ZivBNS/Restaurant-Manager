package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import messages.Message;
import messages.MessageType;
import entities.Bill;
import javafx.event.ActionEvent;

public class BillPayment_GUI {

    public static BillPayment_GUI instance;

    private int currentBillId = -1;
    private double expectedAmount = 0;
    public static String previousScreen;


    @FXML private TextField paymentField;
    @FXML private Button btnPay;
    @FXML private Label lblReservationId;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblBillDetails;
    @FXML private Label lblOriginalAmount;
    @FXML private Label lblDiscountInfo;


    @FXML
    public void initialize() {
        instance = this;

        String phoneToSend = User_Session.getActivePhone();

        if (phoneToSend != null) {
        	ConnectToServer_GUI.clientController.sendGetLatestBillByPhoneRequest(phoneToSend);
        } else {
            System.out.println("DEBUG: No phone found for this session!");
        }
    }
    
    public void displayBill(Bill bill) {
        if (bill == null) {
            showAlert("Error", "No bill found.");
            return;
        }

        currentBillId = bill.getId();
        
        double originalAmount = bill.getTotalAmount();
        double discount = bill.getDiscountRate();
        expectedAmount = bill.calculateFinalAmount();

        lblReservationId.setText(String.valueOf(bill.getReservationId()));
        lblBillDetails.setText(bill.getBillDetails());

        if (discount > 0) {
            lblOriginalAmount.setText(String.format("Original Price: ₪%.2f", originalAmount));
            lblOriginalAmount.setVisible(true);
            lblOriginalAmount.setManaged(true);

            lblDiscountInfo.setText(String.format("Member Discount Applied: %.0f%%", discount));
            lblDiscountInfo.setVisible(true);
            lblDiscountInfo.setManaged(true);

            lblTotalAmount.setText(String.format("Total to Pay: ₪%.2f", expectedAmount));
        } else {
            lblOriginalAmount.setVisible(false);
            lblOriginalAmount.setManaged(false);
            lblDiscountInfo.setVisible(false);
            lblDiscountInfo.setManaged(false);

            lblTotalAmount.setText(String.format("Total to Pay: ₪%.2f", originalAmount));
        }

        btnPay.setDisable("PAID".equalsIgnoreCase(bill.getStatus()));
    }


    @FXML
    private void onPay(ActionEvent event) {

        if (currentBillId == -1) {
            showAlert("Error", "No bill loaded.");
            return;
        }
        ConnectToServer_GUI.clientController.sendBillPaymentRequest(currentBillId);
    }

    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            String screen = previousScreen != null
                    ? previousScreen
                    : "/gui/CasualCustomer.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(screen));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    
    public void onPaymentSuccess() {
        showAlert("Success", "Payment completed successfully!");
        btnPay.setDisable(true);
    }

    public void showNoReservationFound() {
        showAlert("No Reservation Found", "No reservation found by phone.");
    }
}




