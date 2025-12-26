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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import Data.DB_Controller;
import Data.OpeningHours_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;

/**
 * Modern Graphical User Interface for the Bistro Server Management.
 * Refactored for a compact connection header and expanded log area.
 */
public class Server_GUI extends Application {

    private Server_Controller serverController;
    private TextArea logArea;
    private TextField portField;
    private Button connectBtn;
    private Button exitBtn;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Bistro System | Server Management");

        // --- Header Section (Reduced padding) ---
        Label titleLabel = new Label("SERVER CONTROL PANEL");
        titleLabel.setTextFill(Color.web("#38bdf8"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        
        VBox header = new VBox(titleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        // --- Connection Bar (Changed to HBox for compactness) ---
        HBox connectionBar = new HBox(15);
        connectionBar.setAlignment(Pos.CENTER_LEFT);
        connectionBar.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12; -fx-padding: 15;");
        connectionBar.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));

        Label portLabel = new Label("Port:");
        portLabel.setTextFill(Color.web("#f1f5f9"));
        portLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));

        portField = new TextField("5555");
        portField.setPrefWidth(80);
        portField.setPrefHeight(35);
        portField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #334155; -fx-border-radius: 5;");

        connectBtn = new Button("START SERVER");
        connectBtn.setPrefHeight(35);
        HBox.setHgrow(connectBtn, Priority.ALWAYS);
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        
        exitBtn = new Button("SHUTDOWN");
        exitBtn.setPrefHeight(35);
        exitBtn.setDisable(true);
        exitBtn.setMinWidth(120);
        exitBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");

        // Add all to the horizontal bar
        connectionBar.getChildren().addAll(portLabel, portField, connectBtn, exitBtn);

        // --- Log Section (Expanded) ---
        Label logLabel = new Label("LIVE SERVER LOG");
        logLabel.setTextFill(Color.web("#94a3b8"));
        logLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        logLabel.setPadding(new Insets(10, 0, 5, 5));

        logArea = new TextArea();
        logArea.setEditable(false);
        // Using Priority.ALWAYS and increasing initial prefHeight
        logArea.setStyle("-fx-control-inner-background: #0f172a; -fx-text-fill: #10b981; -fx-font-family: 'Consolas'; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // --- Main Layout Assembly ---
        VBox root = new VBox(5);
        root.setStyle("-fx-background-color: #0f172a;");
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.getChildren().addAll(header, connectionBar, logLabel, logArea);

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

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                closeProgram();
            }
        });

        // Adjusted scene size for horizontal layout
        Scene scene = new Scene(root, 750, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

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
            portField.setDisable(true);
            
            appendLog("SUCCESS: Server is listening on port " + port);
            
            OpeningHours_Repository.getInstance().init();
            Waitlist_Repository.getInstance().init();
            Reservation_Repository.getInstance().init();
            Table_Repository.getInstance().init();
            
            appendLog("System: Repositories initialized.");

        } catch (IOException ex) {
            appendLog("ERROR: Port " + port + " is busy.");
            serverController = null;
        }
    }

    public void appendLog(final String str) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                logArea.appendText("[" + timestamp + "] " + str + "\n");
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}