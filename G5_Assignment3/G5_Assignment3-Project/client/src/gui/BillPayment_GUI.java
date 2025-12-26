package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import messages.Message;
import messages.MessageType;
import entities.Bill;
import entities.Reservation;
import javafx.event.ActionEvent;

public class BillPayment_GUI {

    public static BillPayment_GUI instance;
    private Reservation currentReservation;

    @FXML private TextField paymentField;
    @FXML private Label lblDate;
    @FXML private Label lblTime;
    @FXML private Label lblGuests;
    @FXML private Label lblTable;
    @FXML private Label lblBill;

    @FXML
    public void initialize() {
        instance = this;

        String phone = User_Session.getCasualPhone();

        ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.GET_LATEST_RESERVATION_BY_PHONE, phone)
        );
    }

    public void displayReservation(Reservation r) {

        currentReservation = r;

        if (r == null) {
            lblDate.setText("No upcoming reservation");
            lblTime.setText("-");
            lblGuests.setText("-");
            lblTable.setText("-");
            return;
        }

        lblDate.setText(r.getOrderStartTime().toLocalDate().toString());
        lblTime.setText(r.getOrderStartTime().toLocalTime().toString());
        lblGuests.setText(String.valueOf(r.getNumberOfDiners()));
        lblTable.setText(r.getTableId() == null ? "-" : String.valueOf(r.getTableId()));
    }

    @FXML
    private void onPay(ActionEvent event) {

        if (currentReservation == null) {
            showAlert("Error", "No reservation found to pay.");
            return;
        }

        ConnectToServer_GUI.clientController.sendComplexObject(
            new Message(MessageType.GET_BILL_BY_RESERVATION_ID, currentReservation.getId())
        );
        
        double amount = Double.parseDouble(paymentField.getText());

        if (amount != expectedAmount) {
            showAlert("Error", "Incorrect amount.");
            return;
        }
    }
    
    private double expectedAmount;

    public void displayBill(Bill bill) {

        if (bill == null) {
            showAlert("Error", "No bill found.");
            return;
        }

        expectedAmount = bill.getTotalAmount();
        showAlert("Bill", "Total amount: " + expectedAmount + "₪");
    }


    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
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
}



