package gui;


import java.io.InputStream;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Barcode_GUI {
	@FXML
    private ImageView barcodeImage;
	
	@FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // Load barcode image from resources
        String path = "/images/barcode.png";

        InputStream stream = getClass().getResourceAsStream(path);

        if (stream == null) {
            // Image not found , show error
            barcodeImage.setVisible(false);
            errorLabel.setText("Barcode image not found.");
            errorLabel.setVisible(true);
            return;
        }

        Image image = new Image(stream);
        barcodeImage.setImage(image);
        errorLabel.setVisible(false);
    }


    
    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            String screen = "/gui/SubscribedCustomer.fxml";

            Parent root = FXMLLoader.load(getClass().getResource(screen));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
