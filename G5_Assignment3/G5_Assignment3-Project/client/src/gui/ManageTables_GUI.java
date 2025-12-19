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
import entities.TableSize;
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
    @FXML private ComboBox<TableSize> sizeCombo;
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

        sizeCombo.getItems().addAll(TableSize.values());

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
                    sizeCombo.setValue(selected.getTableSize());
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

        if (sizeCombo.getValue() == null) {
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

        TableSize size = sizeCombo.getValue();
        boolean active = activeCheckBox.isSelected();

        Restaurant_Table selected =
            tablesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            // ADD
            Restaurant_Table newTable =
                new Restaurant_Table(-1, tableNumber, size, active);

            clientController.sendComplexObject(
            	    new Message(MessageType.ADD_TABLE_REQUEST, newTable)
            	);


        } else {
            // UPDATE
            selected.setTableNumber(tableNumber);
            selected.setTableSize(size);
            selected.setActive(active);

            clientController.sendComplexObject(
            	    new Message(MessageType.UPDATE_TABLE_REQUEST, selected)
            	);

        }

        tablesTable.getSelectionModel().clearSelection();
        tableNumberField.clear();
        sizeCombo.setValue(null);
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

