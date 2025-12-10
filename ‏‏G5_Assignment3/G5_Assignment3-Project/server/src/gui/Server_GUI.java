package gui;

import controllers.Server_Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

import Data.DB_Controller;

public class Server_GUI extends Application {

	private Server_Controller serverController;
	private TextArea logArea;
	private TextField portField;
	private Button connectBtn;
	private Button exitBtn;

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Bistro Server Management");

		Label titleLabel = new Label("Bistro Server");
		titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
		portField = new TextField("5555");
		portField.setMaxWidth(200);

		connectBtn = new Button("Connect");
		exitBtn = new Button("Exit");
		exitBtn.setDisable(true);

		logArea = new TextArea();
		logArea.setEditable(false);
		logArea.setPrefHeight(300);

		connectBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				handleConnectAction(event);
			}
		});

		exitBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				closeProgram();
			}
		});

		VBox root = new VBox(10);
		root.setPadding(new Insets(20));
		root.setAlignment(Pos.TOP_CENTER);
		root.getChildren().addAll(titleLabel, new Label("Port:"), portField, connectBtn, exitBtn,
				new Label("Server Log:"), logArea);

		Scene scene = new Scene(root, 450, 600);
		primaryStage.setScene(scene);

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				closeProgram();
			}
		});

		primaryStage.show();
	}

	private void closeProgram() {
		if (serverController != null) {
			try {
				System.out.println("Closing server and DB connection...");
				DB_Controller.getInstance().closeConnection();
				serverController.close();
			} catch (IOException ex) {
				System.out.println("Error closing server: " + ex.getMessage());
			}
		}
		Platform.exit();
		System.exit(0); 
	}

	private void handleConnectAction(ActionEvent e) {
		int port;
		try {
			port = Integer.parseInt(portField.getText());
		} catch (NumberFormatException ex) {
			appendLog("Error: Invalid Port Number.");
			return;
		}

		// Create and bind the Controller (which is also the Server)
		serverController = new Server_Controller(port, this);

		// Start the Server (OCSF listen command)
		try {
			serverController.listen();

			// Update UI state upon success
			connectBtn.setDisable(true);
			exitBtn.setDisable(false); 
			portField.setDisable(true);

		} catch (IOException ex) {
			appendLog("Error: Could not listen on port " + port);
			serverController = null;
		}
	}

	public void appendLog(String str) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				logArea.appendText(str + "\n");
			}
		});
	}

	public static void main(String[] args) {
		launch(args);
	}
}