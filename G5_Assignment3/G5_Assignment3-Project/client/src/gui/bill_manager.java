package gui;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;

public class bill_manager extends Application {

    // -------- Model inside same file --------
    public static class Bill {
        StringProperty name = new SimpleStringProperty();
        ObjectProperty<BigDecimal> amount = new SimpleObjectProperty<>();
        ObjectProperty<LocalDate> due = new SimpleObjectProperty<>();
        StringProperty category = new SimpleStringProperty();
        BooleanProperty paid = new SimpleBooleanProperty(false);
    }

    // -------- FXML fields --------
    @FXML private TextField nameField, amountField, categoryField;
    @FXML private DatePicker duePicker;

    @FXML private TableView<Bill> table;
    @FXML private TableColumn<Bill,String> nameCol, categoryCol;
    @FXML private TableColumn<Bill,BigDecimal> amountCol;
    @FXML private TableColumn<Bill,LocalDate> dueCol;
    @FXML private TableColumn<Bill,Boolean> paidCol;

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("bill_manager.fxml"));
        stage.setScene(new Scene(loader.load(), 650, 450));
        stage.setTitle("Bill Manager");
        stage.show();
    }

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(d -> d.getValue().name);
        amountCol.setCellValueFactory(d -> d.getValue().amount);
        dueCol.setCellValueFactory(d -> d.getValue().due);
        categoryCol.setCellValueFactory(d -> d.getValue().category);
        paidCol.setCellValueFactory(d -> d.getValue().paid);

        table.setItems(bills);
        duePicker.setValue(LocalDate.now());
    }

    @FXML
    private void addBill() {
        Bill b = new Bill();
        b.name.set(nameField.getText());
        b.amount.set(new BigDecimal(amountField.getText()));
        b.due.set(duePicker.getValue());
        b.category.set(categoryField.getText());
        bills.add(b);

        nameField.clear();
        amountField.clear();
        categoryField.clear();
    }

    @FXML
    private void deleteBill() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) bills.remove(selected);
    }

    @FXML
    private void markPaid() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) selected.paid.set(true);
    }

    public static void main(String[] args) {
        launch();
    }
}


