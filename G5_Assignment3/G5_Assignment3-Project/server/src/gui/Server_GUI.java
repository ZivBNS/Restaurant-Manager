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
import Data.OpeningHours_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;

/**
 * The graphical user interface for the Bistro Server.
 * Responsible for starting the OCSF server, initializing database repositories,
 * and managing the lifecycle of the connection pool.
 */
public class Server_GUI extends Application {

	private Server_Controller serverController;
	private TextArea logArea;
	private TextField portField;
	private Button connectBtn;
	private Button exitBtn;

	/**
	 * Sets up the primary JavaFX stage and UI components for the server management tool.
	 */
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

		// Handle server connection attempt
		connectBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				handleConnectAction(event);
			}
		});

		// Handle manual exit via button
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

		// Handle window close (X button) via same cleanup logic
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				closeProgram();
			}
		});

		primaryStage.show();
	}

	/**
	 * Gracefully shuts down the OCSF server and closes the Database Connection Pool.
	 * This ensures no orphan connections remain open in the DB.
	 */
	private void closeProgram() {
		if (serverController != null) {
			try {
				System.out.println("Server_GUI: Closing server and DB connection pool...");
				
				// Close the pool instead of a single connection
				DB_Controller.getInstance().closePool(); 
				
				serverController.close();
			} catch (IOException ex) {
				System.err.println("Server_GUI: Error closing server: " + ex.getMessage());
			}
		}
		Platform.exit();
		System.exit(0); 
	}

	/**
	 * Validates the port, starts the OCSF server listener, and initializes 
	 * the database repositories.
	 * @param e The connection action event.
	 */
	private void handleConnectAction(ActionEvent e) {
		int port;
		try {
			port = Integer.parseInt(portField.getText());
		} catch (NumberFormatException ex) {
			appendLog("Error: Invalid Port Number.");
			return;
		}

		// Create the OCSF controller
		serverController = new Server_Controller(port, this);

		try {
			// Start listening for client connections
			serverController.listen();

			// Update UI components to reflect connected state
			connectBtn.setDisable(true);
			exitBtn.setDisable(false); 
			portField.setDisable(true);
			appendLog("Server is online and listening on port " + port);

		} catch (IOException ex) {
			appendLog("Error: Could not listen on port " + port);
			serverController = null;
			return;
		}
		
		// Initialize all data layers using the newly formed pool
		OpeningHours_Repository.getInstance().init();
		Waitlist_Repository.getInstance().init();
		Reservation_Repository.getInstance().init();
		Table_Repository.getInstance().init();
	}

	/**
	 * Appends a message to the UI log area in a thread-safe manner.
	 * @param str The message to log.
	 */
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