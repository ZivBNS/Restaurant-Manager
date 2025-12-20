package gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import controllers.Client_Controller;
import entities.Restaurant;
import entities.Restaurant_Table;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import messages.Message;
import messages.MessageType;

public class ManageTables_GUI {

    @FXML private TableView<Restaurant_Table> tablesTable;
    @FXML private TableColumn<Restaurant_Table, Integer> colTableNumber;
    @FXML private TableColumn<Restaurant_Table, Integer> colTableSize;
    @FXML private TableColumn<Restaurant_Table, String> colActive;

    @FXML private TextField tableNumberField;
    @FXML private TextField  tableSizeField;
    @FXML private CheckBox activeCheckBox;
    
    private Client_Controller clientController;
    
    public void setClientController(Client_Controller clientController) {
        this.clientController = clientController;

        this.clientController.sendComplexObject(
            new Message(MessageType.GET_ALL_TABLES, null)
        );
    }

    @FXML
    public void initialize() {

        colTableNumber.setCellValueFactory(data ->
            new SimpleIntegerProperty(
                data.getValue().getTableNumber()
            ).asObject()
        );

        colTableSize.setCellValueFactory(data ->
            new SimpleIntegerProperty(
                data.getValue().getSize()
            ).asObject()
        );

        colActive.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().isActive() ? "Yes" : "No"
            )
        );

        tablesTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSelection, selected) -> {
                if (selected != null) {
                    tableNumberField.setText(
                        String.valueOf(selected.getTableNumber())
                    );
                    tableNumberField.setText(
                            String.valueOf(selected.getSize())
                        );
                    activeCheckBox.setSelected(selected.isActive());
                }
            });
    }
    
    public void loadTables() {
        tablesTable.getItems().setAll(
            Restaurant.getInstance().getTables()
        );
    }

    @FXML
    private void onSaveClicked() {

        if (tableNumberField.getText().isEmpty()) {
            showAlert("Please enter table number");
            return;
        }

        if (tableSizeField.getText().isEmpty()) {
            showAlert("Please select table size");
            return;
        }

        int tableNumber;
        try {
            tableNumber = Integer.parseInt(tableNumberField.getText());
        } catch (NumberFormatException e) {
            showAlert("Table number must be a number");
            return;
        }

        int tableSize;
        try {
            tableSize = Integer.parseInt(tableSizeField.getText());
        } catch (NumberFormatException e) {
            showAlert("Table Size must be a number");
            return;
        }
        
        boolean active = activeCheckBox.isSelected();

        Restaurant_Table selected =
            tablesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            // ADD
            Restaurant_Table newTable =
                new Restaurant_Table(-1, tableNumber, tableSize, active);

            clientController.sendComplexObject(
            	    new Message(MessageType.ADD_TABLE_REQUEST, newTable)
            	);


        } else {
            // UPDATE
            selected.setTableNumber(tableNumber);
            selected.setTableSize(tableSize);
            selected.setActive(active);

            clientController.sendComplexObject(
            	    new Message(MessageType.UPDATE_TABLE_REQUEST, selected)
            	);

        }

        tablesTable.getSelectionModel().clearSelection();
        tableNumberField.clear();
        tableSizeField.clear();
        activeCheckBox.setSelected(false);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    @FXML
    private void onDeleteClicked() {

        Restaurant_Table selected =
            tablesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a table to delete");
            return;
        }

        clientController.sendComplexObject(
            new Message(
                MessageType.DELETE_TABLE_REQUEST,
                selected.getId()
            )
        );
    }
    @FXML
    private void onBackClicked() {
        try {
            Parent root = FXMLLoader.load(
            		getClass().getResource("/gui/Workers.fxml")
            );
            Stage stage =
                (Stage) tablesTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void loadTables(List<Restaurant_Table> tables) {
        tablesTable.getItems().setAll(tables);
    }
}

