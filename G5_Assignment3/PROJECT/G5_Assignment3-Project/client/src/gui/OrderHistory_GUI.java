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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import utils.User_Session;

/**
 * Controller for viewing User History.
 * Separated into two tabs:
 * 1. Order History: All reservation attempts (Completed, Canceled, No-Show).
 * 2. Visit History: Only successful visits with bill and table details.
 */
public class OrderHistory_GUI implements Initializable {

    public static OrderHistory_GUI instance;

    @FXML private Button btnBack;
    @FXML private TabPane historyTabs;
    @FXML private Tab tabOrders;
    @FXML private Tab tabVisits;

    // --- Table 1: Order History (The Plan) ---
    @FXML private TableView<Reservation> orderTable;
    @FXML private TableColumn<Reservation, String> colOrderDate;
    @FXML private TableColumn<Reservation, String> colOrderTime;
    @FXML private TableColumn<Reservation, Integer> colOrderGuests;
    @FXML private TableColumn<Reservation, String> colOrderStatus;

    // --- Table 2: Visit History (The Reality) ---
    @FXML private TableView<Reservation> visitTable;
    @FXML private TableColumn<Reservation, String> colVisitDate;
    @FXML private TableColumn<Reservation, String> colVisitArrival;
    @FXML private TableColumn<Reservation, String> colVisitDeparture;
    @FXML private TableColumn<Reservation, String> colVisitTable;
    @FXML private TableColumn<Reservation, String> colVisitBill;

    // Data Lists
    private ObservableList<Reservation> orderList = FXCollections.observableArrayList();
    private ObservableList<Reservation> visitList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        setupOrderTable();
        setupVisitTable();
        
        // Check Role & Fetch Data
        if (User_Session.getLoggedInUser() != null) {
            String role = User_Session.getLoggedInUser().getIdentity();
            
            if ("Subscriber".equals(role)) {
                int uid = User_Session.getLoggedInUser().getId();
                // Request both lists
                ConnectToServer_GUI.clientController.sendGetReservationHistoryRequest(uid); // For Orders
                ConnectToServer_GUI.clientController.sendGetVisitHistoryRequest(uid);       // For Visits
            } else {
                // Admin View (Popup mode) - Hide Back Button
                if (btnBack != null) {
                    btnBack.setVisible(false);
                    btnBack.setManaged(false);
                }
            }
        }
    }

    /**
     * Configures columns for the Order History table.
     */
    private void setupOrderTable() {
        colOrderDate.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedDate"));
        colOrderTime.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedTime"));
        colOrderStatus.setCellValueFactory(new PropertyValueFactory<Reservation, String>("status"));
        
        colOrderGuests.setCellValueFactory(new Callback<CellDataFeatures<Reservation, Integer>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(CellDataFeatures<Reservation, Integer> param) {
                return new SimpleIntegerProperty(param.getValue().getNumberOfDiners()).asObject();
            }
        });
        
        orderTable.setItems(orderList);
    }

    /**
     * Configures columns for the Visit History table.
     */
    /**
     * Configures columns for the Visit History table.
     * Uses Callbacks to format LocalDateTime objects into Strings.
     */
    private void setupVisitTable() {
        // Date Column
        colVisitDate.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedDate"));
        
        colVisitArrival.setCellValueFactory(new Callback<CellDataFeatures<Reservation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Reservation, String> param) {
                if (param.getValue().getActualArrivalTime() != null) {
                    // Format to HH:mm (e.g., 19:30)
                    String formatted = param.getValue().getActualArrivalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    return new SimpleStringProperty(formatted);
                }
                return new SimpleStringProperty("-");
            }
        });

        colVisitDeparture.setCellValueFactory(new Callback<CellDataFeatures<Reservation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Reservation, String> param) {
                if (param.getValue().getActualDepartureTime() != null) {
                    // Format to HH:mm
                    String formatted = param.getValue().getActualDepartureTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    return new SimpleStringProperty(formatted);
                }
                return new SimpleStringProperty("Active"); // Or "-"
            }
        });

        // Table Column
        colVisitTable.setCellValueFactory(new Callback<CellDataFeatures<Reservation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Reservation, String> param) {
                Integer tId = param.getValue().getTableId();
                return new SimpleStringProperty((tId != null && tId != 0) ? String.valueOf(tId) : "-");
            }
        });

        // Bill Column
        colVisitBill.setCellValueFactory(new Callback<CellDataFeatures<Reservation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Reservation, String> param) {
                Reservation res = param.getValue();
                if (res.getBill() != null) {
                    return new SimpleStringProperty(String.format("%.2f ₪", res.getBill().calculateFinalAmount()));
                }
                return new SimpleStringProperty("N/A");
            }
        });

        visitTable.setItems(visitList);
    }

    /**
     * Updates the Order History table.
     * @param orders List of all reservations.
     */
    public void updateOrderTable(final List<Reservation> orders) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                orderList.setAll(orders);
            }
        });
    }

    /**
     * Updates the Visit History table.
     * @param visits List of completed visits.
     */
    public void updateVisitTable(final List<Reservation> visits) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                visitList.setAll(visits);
            }
        });
    }

    @FXML
    void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/SubscribedCustomer.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            instance=null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}