package gui;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.stage.Stage;

public class CasualCustomer_GUI {

	@FXML
	private void openNewOrder(ActionEvent event) {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CasualOrderForm.fxml"));
	        Parent root = loader.load();

	        // Get current window (Stage)
	        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

	        // Replace the scene in the same window
	        stage.setScene(new Scene(root));
	        stage.setTitle("Create New Order");
	        stage.show();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	@FXML
	private void onBillPaymentClicked(ActionEvent event) {
	    try {
	        Parent paymentScreen = FXMLLoader.load(getClass().getResource("/gui/BillPayment.fxml"));

	        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
	        stage.setScene(new Scene(paymentScreen));
	        stage.setTitle("Order Payment");
	        stage.show();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	@FXML
	private void onViewOrderClicked(ActionEvent event) throws IOException {
	    FXMLLoader loader = new FXMLLoader(
	        getClass().getResource("/gui/ViewCasualCustomerOrder.fxml")
	    );
	    Parent root = loader.load();

	    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
	    stage.setScene(new Scene(root));
	    stage.setTitle("View Order");
	    stage.show();
	}
	
	@FXML
	private void onEditOrderClicked(ActionEvent event) throws IOException {
	    Parent root = FXMLLoader.load(
	            getClass().getResource("/gui/EditCasualCustomerOrder.fxml")
	    );

	    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
	    stage.setScene(new Scene(root));
	    stage.setTitle("Edit Order");
	    stage.show();
	}
	
	@FXML
	private void onCancelOrderClicked(ActionEvent event) {
	    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
	    confirmAlert.setTitle("Cancel Order");
	    confirmAlert.setHeaderText("Cancel Order Confirmation");
	    confirmAlert.setContentText("Are you sure you want to cancel this order?");

	    ButtonType yesButton = new ButtonType("Yes");
	    ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

	    confirmAlert.getButtonTypes().setAll(yesButton, noButton);

	    confirmAlert.showAndWait().ifPresent(response -> {
	        if (response == yesButton) {

	            // DEMO behavior (no DB yet)
	            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
	            successAlert.setTitle("Order Cancelled");
	            successAlert.setHeaderText(null);
	            successAlert.setContentText("Your order has been cancelled successfully.");

	            successAlert.showAndWait();
	        }
	    });
	}



	
	@FXML
	private void onBackClicked(ActionEvent event) {
		try {
			Parent previousScreen = FXMLLoader.load(getClass().getResource("/gui/MainScreen.fxml"));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(previousScreen));
            stage.show();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}


}
