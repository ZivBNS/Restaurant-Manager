package gui;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import controllers.Client_Controller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Customer_GUI {
    // root and panes
    @FXML
    private Parent rootPane;

    @FXML
    private VBox connectPane;

    @FXML
    private VBox mainPane;

    // connect controls
    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Label errorLabel;

    // Adapter and controller
    private JavaFX_Adapter adapter;
    private Client_Controller controller;

    @FXML
    private void initialize() {
        // defaults
    	if (ipField != null) {
            ipField.setText("localhost");
        }
        if (portField != null) {
            portField.setText("5555");
            showConnectPane();
        }

        
    }

    private void showConnectPane() {
        connectPane.setVisible(true);
        connectPane.setManaged(true);
        mainPane.setVisible(false);
        mainPane.setManaged(false);
    }

    private void showMainPane() {
        connectPane.setVisible(false);
        connectPane.setManaged(false);
        mainPane.setVisible(true);
        mainPane.setManaged(true);
    }

    @FXML
    private void onConnectClicked() {
        String host = ipField.getText().trim();
        String portText = portField.getText().trim();
        int port;

        if (host.isEmpty()) {
            errorLabel.setText("IP cannot be empty");
            return;
        }

        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            errorLabel.setText("Port must be a number");
            return;
        }

        try {
            // create controller and adapter once
            controller = new Client_Controller(host, port);
            adapter = new JavaFX_Adapter(controller);
            adapter.attachCustomerGUI(this);
            
            // switch UI
            showMainPane();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle("Restaurant Client - " + host + ":" + port);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Failed to connect: " + e.getMessage());
        }
    }


	@FXML
	private void onViewReservationsClicked() {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_Reservations.fxml"));
	        Parent newView = loader.load();

	        Stage stage = (Stage) rootPane.getScene().getWindow();
	        stage.getScene().setRoot(newView);   
	        
	        adapter.requestShowReservations();
	        
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
	        stage.getScene().setRoot(originalView);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
