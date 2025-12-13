package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
	        stage.show();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
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
