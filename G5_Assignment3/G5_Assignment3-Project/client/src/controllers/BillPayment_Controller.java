package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class BillPayment_Controller {

    @FXML
    private TextField paymentField;

    /**
     * Called when the user clicks "Pay".
     */
    @FXML
    private void onPay(ActionEvent event) {
        String amountText = paymentField.getText().trim();

        if (amountText.isEmpty()) {
            showAlert("Error", "Please enter an amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                showAlert("Error", "Amount must be greater than zero.");
                return;
            }

            System.out.println("Payment submitted: " + amount);

            showAlert("Success", "Payment of " + amount + " was processed.");

            paymentField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Amount must be a valid number.");
        }
    }

    /**
     * Called when clicking "Back".
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            Parent previousScreen = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(previousScreen));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Helper method for showing popup alerts.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
