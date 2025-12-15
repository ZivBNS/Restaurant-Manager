package gui;

import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

public class CasualOrderForm_GUI {

    
    @FXML private DatePicker datePicker;
    @FXML private TextField guestsField;
    @FXML private ComboBox<String> timeCombo;

    @FXML
    public void initialize() {
        loadAvailableHours();
    }

    private void loadAvailableHours() {
        LocalTime open = LocalTime.of(10, 0);
        LocalTime close = LocalTime.of(22, 0);
        
        timeCombo.setPromptText("Select Time");

        for (LocalTime t = open; !t.isAfter(close); t = t.plusMinutes(30)) {
            timeCombo.getItems().add(t.toString());
        }
    }

    @FXML
    private void onSubmitOrder() {
        System.out.println("Order submitted!");
        System.out.println("Date: " + datePicker.getValue());
        System.out.println("Time: " + timeCombo.getValue());
        System.out.println("Guests: " + guestsField.getText());
    }

    @FXML
    private void onBackClicked() {
        try {
        	Parent root = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));

            Stage stage = (Stage) timeCombo.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Casual Customer Menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

