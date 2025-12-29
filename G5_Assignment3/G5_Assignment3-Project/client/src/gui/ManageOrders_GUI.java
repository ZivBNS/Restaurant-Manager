package gui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import entities.Opening_Hours;
import entities.Opening_Hours.TimeRange;
import entities.Reservation;
import entities.Restaurant;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import messages.Message;
import messages.MessageType;

/**
 * Advanced Employee Management Controller.
 * Manages reservations with restricted administrative editing and optimized table display.
 */
public class ManageOrders_GUI {
    public static ManageOrders_GUI instance;

    @FXML private TableView<Reservation> adminTable;
    @FXML private TableColumn<Reservation, Integer> colCode, colTable, colGuests, colUserID;
    @FXML private TableColumn<Reservation, String> colPhone, colEmail, colDate, colTime, colStatus;

    @FXML private TextField txtPhone, txtEmail, txtGuests, txtTable, txtStatus, txtUserID, txtCreationTime, txtArrivalTime;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;

    private ObservableList<Reservation> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @FXML
    public void initialize() {
        instance = this;
        setupAdminTable();
        
        // Load opening hours for dynamic ComboBox population
        ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
        refreshAdminData();

        /**
         * Listener for selecting an order to populate read-only and editable fields.
         */
        adminTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Reservation>() {
            @Override
            public void changed(ObservableValue<? extends Reservation> obs, Reservation oldV, Reservation newV) {
                if (newV != null) {
                    populateEmployeeEditFields(newV);
                }
            }
        });

        /**
         * Listener to update available time slots based on the selected date.
         */
        datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldV, LocalDate newV) {
                if (newV != null) {
                    loadDynamicHours(newV, null);
                }
            }
        });
    }

    /**
     * Maps the TableView columns and applies constrained resize policy to remove empty space.
     */
    private void setupAdminTable() {
        adminTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colUserID.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("formattedTime"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        adminTable.setItems(masterData);
    }

    public void loadDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                timeCombo.getItems().clear();
                Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
                if (oh == null) return;

                LocalTime open = null, close = null;
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    TimeRange range = oh.getExceptionSchedule().get(selectedDate);
                    open = range.getOpenTime(); close = range.getCloseTime();
                } else {
                    DayOfWeek day = selectedDate.getDayOfWeek();
                    if (oh.getRegularSchedule().containsKey(day)) {
                        TimeRange range = oh.getRegularSchedule().get(day);
                        open = range.getOpenTime(); close = range.getCloseTime();
                    }
                }

                if (open != null && close != null) {
                    LocalTime firstAvailable = open;
                    LocalTime lastSlot = close.minusHours(2);
                    boolean isMidnightCrossing = !close.isAfter(open);

                    if (selectedDate.equals(LocalDate.now())) {
                        LocalTime buffer = roundToNext30Min(LocalTime.now().plusHours(1));
                        boolean skip = isMidnightCrossing ? (buffer.isAfter(open) || buffer.isBefore(close)) : buffer.isAfter(open);
                        if (skip) firstAvailable = buffer;
                    }

                    LocalTime t = firstAvailable;
                    int safety = 0;
                    while (safety < 48) {
                        boolean isValid = isMidnightCrossing ? (!t.isBefore(open) || !t.isAfter(lastSlot)) : (!t.isBefore(open) && !t.isAfter(lastSlot));
                        if (isValid) {
                            timeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
                        } else break;
                        
                        if (t.equals(lastSlot)) break;
                        t = t.plusMinutes(30);
                        safety++;
                    }
                    
                    if (timeToSelect != null) {
                        timeCombo.setValue(timeToSelect);
                    }
                }
            }
        });
    }

    private LocalTime roundToNext30Min(LocalTime time) {
        int minutes = time.getMinute();
        if (minutes == 0) return time.withSecond(0).withNano(0);
        if (minutes <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    @FXML
    public void refreshAdminData() {
        ConnectToServer_GUI.clientController.sendGetAllPendingReservationsRequest();
    }

    /**
     * Fills editor fields when an order is selected. Locked fields are non-editable.
     */
    private void populateEmployeeEditFields(Reservation res) {
        txtUserID.setText(res.getUserId() != null ? String.valueOf(res.getUserId()) : "N/A");
        txtPhone.setText(res.getPhone());
        txtEmail.setText(res.getEmail());
        txtGuests.setText(String.valueOf(res.getNumberOfDiners()));
        txtTable.setText(res.getTableId() != null ? String.valueOf(res.getTableId()) : "Unassigned");
        txtStatus.setText(res.getStatus());
        datePicker.setValue(res.getOrderStartTime().toLocalDate());
        
        String timeStr = String.format("%02d:%02d", res.getOrderStartTime().getHour(), res.getOrderStartTime().getMinute());
        loadDynamicHours(res.getOrderStartTime().toLocalDate(), timeStr);
        
        txtCreationTime.setText(res.getCreationTime().format(displayFormatter));
    }

    /**
     * Sends the administrative update. UserID, TableID, and Status are NOT updated.
     */
    @FXML
    void onAdminSaveClicked(ActionEvent event) {
        Reservation selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null || timeCombo.getValue() == null || datePicker.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "No order selected.").show();
            return;
        }

        try {
            // Only update editable fields
            selected.setPhone(txtPhone.getText());
            selected.setEmail(txtEmail.getText());
            selected.setNumberOfDiners(Integer.parseInt(txtGuests.getText()));

            LocalTime time = LocalTime.parse(timeCombo.getValue());
            selected.setOrderStartTime(LocalDateTime.of(datePicker.getValue(), time));
            selected.setOrderEndTime(selected.getOrderStartTime().plusHours(2));

            Message msg = new Message(MessageType.ADMIN_UPDATE_RESERVATION, selected);
            ConnectToServer_GUI.clientController.sendComplexObject(msg);
            
            System.out.println("Admin Update: Edit sent for " + selected.getConfirmationCode());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid Diners count.").show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onManualAddClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/AddManualReservation.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("Manual Entry");
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner(adminTable.getScene().getWindow());
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshAdminData();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    @FXML
    void onCreateBillClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/BillManager.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("Bill Manager");
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner(adminTable.getScene().getWindow());
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshAdminData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    void onDeleteClicked(ActionEvent event) {
        final Reservation selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirm Deletion?");
        alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
            @Override
            public void accept(ButtonType response) {
                if (response == ButtonType.OK) {
                    Message msg = new Message(MessageType.CANCEL_RESERVATION, selected.getId());
                    ConnectToServer_GUI.clientController.sendComplexObject(msg);
                }
            }
        });
    }

    @FXML
    void onBackClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
            Stage stage = (Stage) adminTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateAdminUI(final List<Reservation> list) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() { masterData.setAll(list); }
        });
    }
}