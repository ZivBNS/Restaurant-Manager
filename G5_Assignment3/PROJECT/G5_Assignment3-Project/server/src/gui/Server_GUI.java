package gui;

import controllers.Server_Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import Data.DB_Controller;
import Data.Init_All; // Import the Init class
import Data.OpeningHours_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;

/**
 * GUI class for the Bistro System server management.
 * Provides a user interface for starting the server, monitoring logs, and managing connections.
 */
public class Server_GUI extends Application {

    private Server_Controller serverController;
    private TextArea logArea;
    private TextField portField;
    private Button connectBtn;
    private Button exitBtn;
    private Button initBtn; 

    /**
     * Initializes and displays the primary stage with the server control panel.
     * Sets up the UI components, styling, and event handlers.
     * * @param primaryStage the primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Bistro System | Server Management");

        // --- Header Section ---
        Label titleLabel = new Label("SERVER CONTROL PANEL");
        titleLabel.getStyleClass().add("header-title"); 
        
        VBox header = new VBox(titleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        // --- Connection Bar ---
        HBox connectionBar = new HBox(15);
        connectionBar.setAlignment(Pos.CENTER_LEFT);
        connectionBar.setPadding(new Insets(20)); 
        connectionBar.getStyleClass().add("card"); 

        Label portLabel = new Label("Port:");
        portLabel.getStyleClass().add("card-label"); 

        portField = new TextField("5555");
        portField.setPrefWidth(80);
        portField.setPrefHeight(35);

        connectBtn = new Button("START SERVER");
        connectBtn.setPrefHeight(35);
        HBox.setHgrow(connectBtn, Priority.ALWAYS);
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.getStyleClass().addAll("button", "btn-primary"); 

        exitBtn = new Button("SHUTDOWN");
        exitBtn.setPrefHeight(35);
        exitBtn.setDisable(true);
        exitBtn.setMinWidth(120);
        exitBtn.getStyleClass().addAll("button", "btn-danger"); 

        connectionBar.getChildren().addAll(portLabel, portField, connectBtn, exitBtn);

        // --- Log Section Header (Label + Init Button) ---
        HBox logHeaderBox = new HBox(10);
        logHeaderBox.setAlignment(Pos.CENTER_LEFT);
        logHeaderBox.setPadding(new Insets(10, 0, 5, 5));

        Label logLabel = new Label("LIVE SERVER LOG");
        logLabel.getStyleClass().add("card-label");
        
        // Spacer to push button to right or just keep it next to label
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Init DB Button
        initBtn = new Button("INIT DB");
        initBtn.setPrefHeight(30);
        initBtn.getStyleClass().addAll("button", "btn-secondary"); // Style it orange/grey
        initBtn.setTooltip(new Tooltip("Reset Database Data"));

        logHeaderBox.getChildren().addAll(logLabel, spacer, initBtn);

        // --- Log Area ---
        logArea = new TextArea();
        logArea.setEditable(false);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logArea.getStyleClass().add("console-log"); 

        // --- Main Layout Assembly ---
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        root.getChildren().addAll(header, connectionBar, logHeaderBox, logArea);

        // --- Event Handlers ---
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
        
        // Init Action
        initBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleInitAction();
            }
        });

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                closeProgram();
            }
        });

        Scene scene = new Scene(root, 600, 700);
        scene.getStylesheets().add(getClass().getResource("/Theme/application.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Logic Methods ---

    /**
     * Executes the Database Initialization logic.
     * Runs Init_All.main() in a background thread to avoid freezing the UI.
     */
    private void handleInitAction() {
        if (serverController != null) {
            appendLog("WARNING: Cannot init DB while server is running. Please shutdown first.");
            return;
        }

        initBtn.setDisable(true);
        appendLog("System: Starting Database Initialization...");

        // Run in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Call the main method of Init_All
                    Init_All.main(null); 
                    
                    Platform.runLater(() -> {
                        appendLog("System: Database Initialization Completed.");
                        initBtn.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        appendLog("ERROR: Init failed - " + e.getMessage());
                        initBtn.setDisable(false);
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Safely shuts down the server, closes the database connection pool, and exits the application.
     */
    private void closeProgram() {
        if (serverController != null) {
            try {
                appendLog("System: Shutting down server and DB pool...");
                DB_Controller.getInstance().closePool(); 
                serverController.close();
            } catch (IOException ex) {
                System.err.println("Server_GUI Error: " + ex.getMessage());
            }
        }
        Platform.exit();
        System.exit(0); 
    }

    /**
     * Processes the connection attempt. Validates the port number, starts the server,
     * and initializes all system repositories.
     * * @param e the action event triggered by the connection button.
     */
    private void handleConnectAction(ActionEvent e) {
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException ex) {
            appendLog("CRITICAL: Invalid Port Number.");
            return;
        }

        serverController = new Server_Controller(port, this);
        try {
            serverController.listen();
            connectBtn.setDisable(true);
            connectBtn.setText("SERVER ONLINE");
            exitBtn.setDisable(false); 
            initBtn.setDisable(true); // Disable Init while server runs for safety
            portField.setDisable(true);
                        
            OpeningHours_Repository.getInstance().init();
            Reservation_Repository.getInstance().init();
            Table_Repository.getInstance().init();
            
            appendLog("System: Repositories initialized.");

        } catch (IOException ex) {
            appendLog("ERROR: Port " + port + " is busy.");
            serverController = null;
        }
    }

    /**
     * Appends a given string to the server log area with a timestamp.
     * This method is thread-safe and can be called from non-UI threads.
     * * @param str the message to append to the log.
     */
    public void appendLog(final String str) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                logArea.appendText("[" + timestamp + "] " + str + "\n");
            }
        });
    }

    /**
     * Main entry point of the application.
     * * @param args command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}