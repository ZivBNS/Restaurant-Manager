package gui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.io.IOException;

public class EditCasualCustomerOrder_GUI {

    @FXML
    private TextField guestsField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<String> timeCombo;

    @FXML
    public void initialize() {
        // Demo data
        guestsField.setText("4");

        timeCombo.getItems().addAll(
                "18:00", "18:30", "19:00", "19:30", "20:00"
        );
        timeCombo.setValue("19:30");
    }

    @FXML
    private void onSaveChanges(ActionEvent event) {
        System.out.println("Changes saved (demo)");
        // later: update DB
    }

    @FXML
    private void onBackClicked(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource("/gui/CasualCustomer.fxml")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Casual Customer Menu");
        stage.show();
    }
}
