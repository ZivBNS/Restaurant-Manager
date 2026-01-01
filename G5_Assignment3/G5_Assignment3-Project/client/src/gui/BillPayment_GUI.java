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


    @FXML
    public void initialize() {
        instance = this;

        String phone = User_Session.getCasualPhone();

        ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.GET_LATEST_RESERVATION_BY_PHONE, phone)
        );
    }
    
    public void displayBill(Bill bill) {

        if (bill == null) {
            showAlert("Error", "No bill found.");
            return;
        }

        currentBillId = bill.getId();
        expectedAmount = bill.calculateFinalAmount();

        lblReservationId.setText(String.valueOf(bill.getReservationId()));
        lblTotalAmount.setText("₪" + expectedAmount);
        lblBillDetails.setText(bill.getBillDetails());

        btnPay.setDisable("PAID".equalsIgnoreCase(bill.getStatus()));
    }


    @FXML
    private void onPay(ActionEvent event) {

        if (currentBillId == -1) {
            showAlert("Error", "No bill loaded.");
            return;
        }

        ConnectToServer_GUI.clientController.sendComplexObject(
            new Message(MessageType.BILL_PAYMENT_REQUEST, currentBillId)
        );
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




