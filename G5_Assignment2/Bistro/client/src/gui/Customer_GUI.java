package gui;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import controllers.Client_Controller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Customer_GUI implements Initializable {
	public static Customer_GUI instance;
	private static Client_Controller controller;
	private JavaFX_Adapter adapter;

	// root and panes
	@FXML private Parent rootPane;
	@FXML private VBox connectPane;
	@FXML private VBox mainPane;
	// connect controls
	@FXML private TextField ipField;
	@FXML private TextField portField;
	@FXML private Label errorLabel;
	@FXML private ListView<String> statusList;

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

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		if (ipField != null)
			ipField.setText("localhost");
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
	private void onConnectClicked() throws IOException {
		String host = ipField.getText().trim();
		String portText = portField.getText().trim();
		int port;

		if (host.isEmpty()) {
			errorLabel.setText("IP cannot be empty");
			return;
		}

		try {
			port = Integer.parseInt(portText);
			controller = new Client_Controller(host, port);
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
	@FXML
	private void onUpdateClicked() {
//		try {
//			FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_GUI.fxml"));
//			Parent originalView = loader.load();
//
//			Stage stage = (Stage) rootPane.getScene().getWindow();
//			stage.getScene().setRoot(originalView);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	public void updateReservationsList(Object data) {
	    Platform.runLater(new Runnable() {
	        @Override
	        public void run() {
	            if (statusList != null) {
	                statusList.getItems().clear(); 
	                
	                if (data instanceof ArrayList) {
	                    ArrayList<?> list = (ArrayList<?>) data;
	                    for (Object item : list) {
	                        statusList.getItems().add(item.toString());
	                    }
	                } else {
	                    statusList.getItems().add(data.toString());
	                }
	            }
	        }
	    });
	}
}
