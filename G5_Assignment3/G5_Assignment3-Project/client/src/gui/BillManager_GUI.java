package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import entities.Bill;
import entities.Reservation;
import messages.Message;
import messages.MessageType;

public class BillManager_GUI {

    public static BillManager_GUI instance;

    @FXML private TextField reservationIDField, totalAmountField, billDetailsField, discountField;
    @FXML private TableView<Bill> table;
    
    // שים לב לשינוי סוגי העמודות כדי שיתאימו ל-entities.Bill
    @FXML private TableColumn<Bill, Integer> resIdCol; 
    @FXML private TableColumn<Bill, Double> amountCol;
    @FXML private TableColumn<Bill, String> detailsCol;
    @FXML private TableColumn<Bill, Double> discountCol;
    @FXML private TableColumn<Bill, String> statusCol;

    private final ObservableList<Bill> billsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;

        // 1. הגדרת העמודות לפי ה-Getters של entities.Bill
        resIdCol.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("billDetails"));
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discountRate"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.setItems(billsList);

        // 2. שליפת נתונים מהשרת
        refreshTable();
    }

    private void refreshTable() {
        ConnectToServer_GUI.clientController.sendComplexObject(
            new Message(MessageType.GET_ALL_BILLS, null)
        );
    }

    // פונקציה שה-Client_Controller יקרא לה כשחוזרת תשובה מהשרת
    public void updateBillsTable(List<Bill> listFromServer) {
        Platform.runLater(() -> {
            billsList.setAll(listFromServer);
        });
    }

    @FXML
    private void addBill(ActionEvent event) {
        try {
            int resId = Integer.parseInt(reservationIDField.getText());
            double amount = Double.parseDouble(totalAmountField.getText());
            String details = billDetailsField.getText();
            // הערה: כאן נצטרך לוגיקה ליצור Bill חדש. 
            // לצורך הפשטות, אני מניח שיש בנאי מתאים או שיוצרים אובייקט זמני
            // שים לב: ב-DB נדרש אובייקט Reservation, אבל כאן יש לנו רק ID.
            // בשרת תצטרך למצוא את ההזמנה לפי ה-ID.
            
            // נשלח אובייקט מיוחד או שנעטוף ב-Bill דמי
            Bill newBill = new Bill(resId, resId, details, amount, "Unpaid"); 
            // (הנחתי שהוספת בנאי כזה ל-Bill, אם לא - צריך להוסיף ב-entities.Bill)

            ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.CREATE_BILL, newBill)
            );
            
            clearFields();

        } catch (NumberFormatException e) {
            showAlert("Input Error", "Please check your numbers.");
        }
    }

    @FXML
    private void deleteBill(ActionEvent event) {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.DELETE_BILL, selected.getId())
            );
        } else {
            showAlert("Selection Error", "Select a bill to delete.");
        }
    }

    @FXML
    private void markPaid(ActionEvent event) {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ConnectToServer_GUI.clientController.sendComplexObject(
                new Message(MessageType.BILL_PAYMENT_REQUEST, selected.getId())
            );
        }
    }

    private void clearFields() {
        reservationIDField.clear();
        totalAmountField.clear();
        billDetailsField.clear();
        discountField.clear();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}


