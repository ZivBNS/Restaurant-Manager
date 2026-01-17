package gui;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.MapValueFactory;
import javafx.stage.Stage;

/**
 * Controller class for the Manage Waitlist screen. Handles displaying and
 * managing waitlist entries for reservations.
 */
public class ManageWaitlist_GUI {

    public static ManageWaitlist_GUI instance;

    @FXML private TableView<Map<String, Object>> waitlistTable;
    @SuppressWarnings("rawtypes")
	@FXML private TableColumn<Map, Integer> colConfCode;
    @SuppressWarnings("rawtypes")
	@FXML private TableColumn<Map, Integer> colGuests;
    @SuppressWarnings("rawtypes")
	@FXML private TableColumn<Map, String> colCreated;
    @SuppressWarnings("rawtypes")
	@FXML private TableColumn<Map, String> colStatus;

    private ObservableList<Map<String, Object>> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;
        setupTableColumns();
        refreshData();
    }

    private void setupTableColumns() {
        colConfCode.setCellValueFactory(new MapValueFactory<>("confCode"));
        colGuests.setCellValueFactory(new MapValueFactory<>("guests"));
        colCreated.setCellValueFactory(new MapValueFactory<>("created"));
        colStatus.setCellValueFactory(new MapValueFactory<>("status"));
        
        waitlistTable.setItems(masterData);
    }

    /**
     * Handles the cancellation of a selected waitlist entry.
     * Extracts the waitlist ID from the selected Map and sends a request to the server.
     */
    @FXML
    void onCancelClicked(ActionEvent event) {
        // 1. Get the selected item from the table
        final Map<String, Object> selected = waitlistTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showWarning("Selection Error", "Please select a waitlist entry to cancel.");
            return;
        }

        // 2. Extract the IDs needed for cancellation
        //final Integer waitlistId = (Integer) selected.get("waitlistId");
        final Integer confCode = (Integer) selected.get("confCode");

        // 3. Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Are you sure you want to cancel waitlist for order #" + confCode + "?", 
            ButtonType.YES, ButtonType.NO);
        
        alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
            @Override
            public void accept(ButtonType response) {
                if (response == ButtonType.YES) {
                    // Send message to server to cancel this specific waitlist entry
                    // You can use a specific MessageType if you have one for canceling by ID
                	ConnectToServer_GUI.clientController.sendCancelWaitlistRequest(confCode);
                }
            }
        });
    }
    
    /**
     * Refreshes the waitlist data by sending a request to the server.
     * Called on initialization and when the refresh button is clicked.
     */
    private void refreshData() {
        if (ConnectToServer_GUI.clientController != null) {
            ConnectToServer_GUI.clientController.sendGetAllActiveWaitlistsRequest();
        }
    }
    
    /**
     * Updates the table with new waitlist data received from the server.
     * @param list The list of waitlist entries as Maps.
     */
    public void updateTable(final List<Map<String, Object>> list) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                masterData.setAll(list);
            }
        });
    }
    
    /**
     * Handles the refresh button click event to reload waitlist data.
     * @param event The action event triggered by clicking the refresh button.
     */
    @FXML
    void onRefreshClicked(ActionEvent event) {
        refreshData();
    }
    
    /**
	 * Handles the back button click event to return to the Workers dashboard.
	 * @param event The action event triggered by clicking the back button.
	 */
    @FXML
    void onBackClicked(ActionEvent event) {
        instance = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.setTitle("Bistro - Worker Dashboard");
            instance=null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a warning alert with the given title and content.
     * @param title The title of the warning alert.
     */
    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    /**
     * Displays a success alert and refreshes the table data.
     * Called by Client_Controller upon successful cancellation.
     * @param message The success message from the server.
     */
    public void showSuccessAndRefresh(final String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.showAndWait();
                
                // Refresh the table to show the entry has been removed/updated
                onRefreshClicked(null);
            }
        });
    }

    /**
     * Displays an error alert if the cancellation fails.
     * @param message The error message from the server.
     */
    public void showErrorAlert(final String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.ERROR, message);
                alert.setTitle("Cancellation Failed");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        });
    }
}