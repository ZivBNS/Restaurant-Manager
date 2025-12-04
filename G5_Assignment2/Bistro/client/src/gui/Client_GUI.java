package gui;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client_GUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

    	
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_GUI.fxml"));
        Parent root = loader.load();
        

        primaryStage.setTitle("Restaurant Client");
        primaryStage.setScene(new Scene(root, 450, 450));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);   
    }
}
