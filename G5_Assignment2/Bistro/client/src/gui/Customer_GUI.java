package gui;

import java.lang.ModuleLayer.Controller;

import controllers.Client_Controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Customer_GUI {
	@FXML
	private Parent rootPane;  
	//private final Client_Controller controller = new Client_Controller(null, 0);
	//JavaFX_Adapter adapter = new JavaFX_Adapter(controller);

	@FXML
	private void onViewReservationsClicked() {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_Reservations.fxml"));
	        Parent newView = loader.load();

	        // Reuse the same Stage and same Scene size
	        Stage stage = (Stage) rootPane.getScene().getWindow();
	        stage.getScene().setRoot(newView);   
	        
	        //adapter.requestShowReservations();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	@FXML
	private void onBackToCustomerClicked() {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_GUI.fxml"));
	        Parent originalView = loader.load();

	        Stage stage = (Stage) rootPane.getScene().getWindow();
	        stage.getScene().setRoot(originalView); // <-- swaps back inside the same window
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
