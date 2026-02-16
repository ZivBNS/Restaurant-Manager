package gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import Reports.MonthlyFullReportData;
import Reports.SubscriberDailyData;
import Reports.TimeDailyData;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/**
 * Controller for the Reports Dashboard.
 * Handles visualization of monthly operational data.
 */
public class Reports_GUI {

    public static Reports_GUI instance;

    @FXML private TabPane reportsTabPane;
    @FXML private BarChart<String, Number> timeBarChart;
    @FXML private LineChart<String, Number> ordersLineChart;
    @FXML private CategoryAxis timeXAxis;
    @FXML private CategoryAxis ordersXAxis;
    
    // Month/Year Selection Controls
    @FXML private ComboBox<String> monthPicker;
    @FXML private ComboBox<Integer> yearPicker;

    /**
     * Initializes the controller class. sets up the month and year pickers with appropriate values.
     */
    @FXML
    public void initialize() {
        instance = this;
        
        // Initialize Month Picker
        monthPicker.getItems().addAll(
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        );
        // Default to current month
        monthPicker.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);

        // Initialize Year Picker (Current year +/- 5 years)
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            yearPicker.getItems().add(i);
        }
        yearPicker.setValue(currentYear);
    }

    /**
     * Triggered by the "Show Report" button.
     */
    @FXML
    private void onShowReportClicked() {
        int month = monthPicker.getSelectionModel().getSelectedIndex() + 1;
        int year = yearPicker.getValue();
        
        // Send request to server via Client Controller
        ConnectToServer_GUI.clientController.sendGetMonthlyReportRequest(month, year);
    }
    
    /**
     * Handles the "Back to Menu" button click.
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

            stage.show();
            instance=null;
        } catch (IOException e) {
            System.err.println("Error: Could not load Workers.fxml");
            e.printStackTrace();
        }
    }

    /**
     * Called by Client_Controller when report data arrives.
     * Updates both charts on the JavaFX thread.
     */
    public void updateReportView(MonthlyFullReportData reportData) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                populateTimeChart(reportData.getTimeDetails());
                populateSubscriberChart(reportData.getSubscriberDetails());
            }
        });
    }

    /**
     * Populates the time-related bar chart with average lateness and overstay data.
     * @param dataList List of TimeDailyData for the month.
     */
    @SuppressWarnings("unchecked")
	private void populateTimeChart(List<TimeDailyData> dataList) {
        timeBarChart.getData().clear();
        
        XYChart.Series<String, Number> latenessSeries = new XYChart.Series<>();
        latenessSeries.setName("Avg Arrival Lateness (Min)");
        
        XYChart.Series<String, Number> overstaySeries = new XYChart.Series<>();
        overstaySeries.setName("Avg Departure Overstay (Min)");

        for (TimeDailyData day : dataList) {
            String dayLabel = String.valueOf(day.getDayIndex());
            latenessSeries.getData().add(new XYChart.Data<>(dayLabel, day.getAvgLateness()));
            overstaySeries.getData().add(new XYChart.Data<>(dayLabel, day.getAvgOverstay()));
        }

        timeBarChart.getData().addAll(latenessSeries, overstaySeries);
    }

    /**
	 * Populates the subscriber-related line chart with total orders and waiting list entries.
	 * @param dataList List of SubscriberDailyData for the month.
	 */
    @SuppressWarnings("unchecked")
	private void populateSubscriberChart(List<SubscriberDailyData> dataList) {
        ordersLineChart.getData().clear();
        
        XYChart.Series<String, Number> ordersSeries = new XYChart.Series<>();
        ordersSeries.setName("Total Orders");
        
        XYChart.Series<String, Number> waitListSeries = new XYChart.Series<>();
        waitListSeries.setName("Waiting List Entries");

        for (SubscriberDailyData day : dataList) {
            String dayLabel = String.valueOf(day.getDayIndex());
            ordersSeries.getData().add(new XYChart.Data<>(dayLabel, day.getTotalOrders()));
            waitListSeries.getData().add(new XYChart.Data<>(dayLabel, day.getWaitingListCount()));
        }

        ordersLineChart.getData().addAll(ordersSeries, waitListSeries);
    }
}