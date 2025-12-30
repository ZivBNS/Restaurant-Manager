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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import Data.DB_Controller;
import Data.OpeningHours_Repository;
import Data.Reservation_Repository;
import Data.Table_Repository;
import Data.Waitlist_Repository;

public class Server_GUI extends Application {

    private Server_Controller serverController;
    private TextArea logArea;
    private TextField portField;
    private Button connectBtn;
    private Button exitBtn;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Bistro System | Server Management");

        // --- Header Section ---
        Label titleLabel = new Label("SERVER CONTROL PANEL");
        titleLabel.getStyleClass().add("header-title"); // שימוש ב-CSS
        
        VBox header = new VBox(titleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        // --- Connection Bar ---
        HBox connectionBar = new HBox(15);
        connectionBar.setAlignment(Pos.CENTER_LEFT);
        connectionBar.setPadding(new Insets(20)); // ריווח פנימי לכרטיס
        connectionBar.getStyleClass().add("card"); // עיצוב כרטיס מה-CSS

        Label portLabel = new Label("Port:");
        portLabel.getStyleClass().add("card-label"); // טקסט תווית מה-CSS

        portField = new TextField("5555");
        portField.setPrefWidth(80);
        portField.setPrefHeight(35);
        // TextField מקבל עיצוב אוטומטי מה-CSS הכללי

        connectBtn = new Button("START SERVER");
        connectBtn.setPrefHeight(35);
        HBox.setHgrow(connectBtn, Priority.ALWAYS);
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.getStyleClass().addAll("button", "btn-primary"); // כפתור ראשי (זהב)

        exitBtn = new Button("SHUTDOWN");
        exitBtn.setPrefHeight(35);
        exitBtn.setDisable(true);
        exitBtn.setMinWidth(120);
        exitBtn.getStyleClass().addAll("button", "btn-danger"); // כפתור סכנה (אדום)

        // הוספת הרכיבים לבר העליון
        connectionBar.getChildren().addAll(portLabel, portField, connectBtn, exitBtn);

        // --- Log Section ---
        Label logLabel = new Label("LIVE SERVER LOG");
        logLabel.getStyleClass().add("card-label");
        logLabel.setPadding(new Insets(10, 0, 5, 5));

        logArea = new TextArea();
        logArea.setEditable(false);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logArea.getStyleClass().add("console-log"); // מחלקה ייעודית ללוג (ראה CSS למטה)

        // --- Main Layout Assembly ---
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        // Root מקבל אוטומטית את צבע הרקע מה-CSS כי הוספנו את ה-Stylesheet ל-Scene

        root.getChildren().addAll(header, connectionBar, logLabel, logArea);

        // --- Event Handlers (ללא שינוי) ---
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

        Scene scene = new Scene(root, 500, 600);
        // *** טעינת קובץ ה-CSS ***
        scene.getStylesheets().add(getClass().getResource("/Theme/application.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Logic Methods (ללא שינוי) ---

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