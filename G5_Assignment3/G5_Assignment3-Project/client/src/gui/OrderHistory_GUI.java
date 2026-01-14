package gui;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import entities.Reservation;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import utils.User_Session;

/**
 * Controller for viewing the User's Order History.
 * Displays a list of past completed reservations including their final bill amount.
 */
public class OrderHistory_GUI implements Initializable {

    public static OrderHistory_GUI instance;

    @FXML private TableView<Reservation> historyTable;
    @FXML private TableColumn<Reservation, String> colDate;
    @FXML private TableColumn<Reservation, String> colTime;
    @FXML private TableColumn<Reservation, Integer> colDiners;
    @FXML private TableColumn<Reservation, String> colTable;
    @FXML private TableColumn<Reservation, String> colBill;

    // Added reference to the Back Button to manipulate its visibility
    @FXML private Button btnBack;

    // Local data list to bind with the TableView
    private ObservableList<Reservation> dataList = FXCollections.observableArrayList();

    /**
     * Called automatically when the FXML is loaded.
     * Initializes table columns, fetches history data, and handles button visibility.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        // --- Column Setup (Using Anonymous Inner Classes instead of Lambdas) ---

        // Date Column
        colDate.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedDate"));

        // Time Column
        colTime.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedTime"));

        // Diners/Guests Column
        colDiners.setCellValueFactory(new Callback<CellDataFeatures<Reservation, Integer>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(CellDataFeatures<Reservation, Integer> param) {
                return new SimpleIntegerProperty(param.getValue().getNumberOfDiners()).asObject();
            }
        });
        
        // Table ID Column (Handles null or 0 values)
        colTable.setCellValueFactory(new Callback<CellDataFeatures<Reservation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Reservation, String> param) {
                Integer tableId = param.getValue().getTableId();
                if (tableId == null || tableId == 0) {
                    return new SimpleStringProperty("TBD");
                }
                return new SimpleStringProperty(String.valueOf(tableId));
            }
        });

        // Bill Total Column (Calculates final price from the Bill object)
        colBill.setCellValueFactory(new Callback<CellDataFeatures<Reservation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Reservation, String> param) {
                Reservation res = param.getValue();
                
                // Check if the reservation has an attached bill
                if (res.getBill() != null) {
                    // Calculate final price (including subscriber discount if applicable)
                    double finalPrice = res.getBill().calculateFinalAmount();
                    // Format to 2 decimal places with currency symbol
                    return new SimpleStringProperty(String.format("%.2f ₪", finalPrice));
                }
                
                return new SimpleStringProperty("N/A"); // Should not happen for 'Completed' orders
            }
        });

        // Bind data list to table
        historyTable.setItems(dataList);

        // --- Role Based Logic ---
        if (User_Session.getLoggedInUser() != null) {
            String role = User_Session.getLoggedInUser().getIdentity();
            
            // Case 1: Subscriber viewing their own dashboard
            if ("Subscriber".equals(role)) {
                // Auto-fetch data
                ConnectToServer_GUI.clientController.sendGetReservationHistoryRequest(User_Session.getLoggedInUser().getId());
                // Button remains visible (default)
            } 
            // Case 2: Employee/Manager viewing via Manage Users Popup
            else {
                // Hide the back button because this is a popup window
                if (btnBack != null) {
                    btnBack.setVisible(false);
                    // Optional: remove it from layout calculations so it doesn't take up space
                    btnBack.setManaged(false); 
                }
                // Note: We do NOT auto-fetch here. The ManageUsers_GUI triggers the fetch for the specific user ID.
            }
        }
    }

    /**
     * Updates the table with data received from the server.
     * Uses Platform.runLater to ensure thread safety.
     * @param history The list of reservations.
     */
    public void updateTable(final List<Reservation> history) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                dataList.setAll(history);
            }
        });
    }

    /**
     * Navigates back to the Subscriber Dashboard.
     * @param event The button click event.
     */
    @FXML
    void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/SubscribedCustomer.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro Member Dashboard");
            stage.centerOnScreen();
            instance=null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}