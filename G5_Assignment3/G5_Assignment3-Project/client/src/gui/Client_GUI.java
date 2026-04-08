package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for the Bistro Client GUI.
 * Launches the JavaFX application and initializes the connection screen.
 */
public class Client_GUI extends Application {

	/**
	 * The main entry point for the JavaFX application.
	 * @param args Command line arguments.
	 */
    public static void main(String[] args) {
        launch(args);
    }

    
	/**
	 * Starts the JavaFX application by loading the initial connection screen.
	 * @param primaryStage The primary stage for this application.
	 * @throws Exception If an error occurs during loading the FXML file.
	 */
    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Load the Connection Screen first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ConnectToServer.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            // Attempt to load the new/modern stylesheet at startup if present
            try {
                scene.getStylesheets().add(getClass().getResource("/Theme/application_modern.css").toExternalForm());
            } catch (Exception ex) {
                // If the modern stylesheet is not found, fall back silently (keeps existing behavior)
                System.err.println("Client_GUI: application_modern.css not found on classpath: " + ex.getMessage());
            }
            primaryStage.setTitle("Bistro - Connect to Server");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}