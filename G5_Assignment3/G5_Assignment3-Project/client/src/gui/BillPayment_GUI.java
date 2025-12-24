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

        String amountText = paymentField.getText().trim();

        if (amountText.isEmpty()) {
            showAlert("Error", "Please enter payment amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                showAlert("Error", "Invalid amount.");
                return;
            }

            showAlert("Success", "Payment of " + amount + "₪ was completed.");
            paymentField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Amount must be a number.");
        }
    }

    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            Parent previousScreen = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(previousScreen));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

