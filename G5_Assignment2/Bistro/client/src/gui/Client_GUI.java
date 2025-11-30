package gui;

import controllers.Client_Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client_GUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create central controller and adapter
        Client_Controller controller = new Client_Controller("localhost",5555);
        JavaFX_Adapter adapter = new JavaFX_Adapter(controller);

        // For now, just show the Customer window to test things
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_GUI.fxml"));
        Parent root = loader.load();

        // connect adapter to Customer_GUI controller
        Customer_GUI customerGui = loader.getController();
        adapter.attachCustomerGUI(customerGui);

        primaryStage.setTitle("Restaurant Client");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);   
    }
}
