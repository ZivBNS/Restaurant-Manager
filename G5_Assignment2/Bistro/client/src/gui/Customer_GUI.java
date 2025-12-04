package gui;

import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.ResourceBundle;
import controllers.Client_Controller;
import entities.Reservation;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Customer_GUI implements Initializable {
	public static Customer_GUI instance;
	private static Client_Controller controller;

	// root and panes
	@FXML private Parent rootPane;
	@FXML private VBox connectPane;
	@FXML private VBox mainPane;
	// connect controls
	@FXML private TextField ipField;
	@FXML private TextField portField;
	@FXML private Label errorLabel;
	@FXML private ListView<Reservation> statusList;
	@FXML private DatePicker datePicker;
    @FXML private TextField visitorsField;
	
    

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        if (ipField != null) {
            ipField.setText("localhost");
        }
        if (portField != null) {
            portField.setText("5555");
            showConnectPane();
        }

        if (datePicker != null) {
            datePicker.setEditable(false);

            datePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
                @Override
                public DateCell call(final DatePicker datePicker) {
                    return new DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            LocalDate today = LocalDate.now();
                            LocalDate nextMonth = today.plusMonths(1);

                            if (item.isBefore(today) || item.isAfter(nextMonth)) {
                                setDisable(true);
                                setStyle("-fx-background-color: #A9A9A9;");
                            }
                        }
                    };
                }
            });
        }

        if (statusList != null) {
            statusList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Reservation>() {
                @Override
                public void changed(ObservableValue<? extends Reservation> observable, Reservation oldValue, Reservation newValue) {
                    if (newValue != null) {
                        if (datePicker != null) {
                            datePicker.setValue(newValue.getReservationTime().toLocalDate());
                        }
                        if (visitorsField != null) {
                            visitorsField.setText(String.valueOf(newValue.getNumDiners()));
                        }
                    }
                }
            });
        }
    }

	private void showConnectPane() {
		connectPane.setVisible(true);
		connectPane.setManaged(true);
		mainPane.setVisible(false);
		mainPane.setManaged(false);
	}

	private void showMainPane() {
		connectPane.setVisible(false);
		connectPane.setManaged(false);
		mainPane.setVisible(true);
		mainPane.setManaged(true);
	}

	@FXML
	private void onConnectClicked() throws IOException {
	    String host = ipField.getText().trim();
	    String portText = portField.getText().trim();
	    int port;

	    if (host.isEmpty()) {
	        errorLabel.setText("IP cannot be empty");
	        return;
	    }

	    try {
	        port = Integer.parseInt(portText);
	    } catch (NumberFormatException e) {
	        errorLabel.setText("Port must be a number");
	        return;
	    }

	    try {
	        controller = new Client_Controller(host, port);

	        //switch UI
	        showMainPane();
	        Stage stage = (Stage) rootPane.getScene().getWindow();
	        stage.setTitle("Restaurant Client - " + host + ":" + port);

	    } catch (Exception e) {
	        e.printStackTrace();
	        errorLabel.setText("Failed to connect: " + e.getMessage());
	    }
	}

	@FXML
	private void onViewReservationsClicked() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_Reservations.fxml"));
			Parent newView = loader.load();

			Stage stage = (Stage) rootPane.getScene().getWindow();
			stage.getScene().setRoot(newView);

			controller.onShowReservationsRequest();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onBackToCustomerClicked() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Customer_GUI.fxml"));
			Parent originalView = loader.load();

			Stage stage = (Stage) rootPane.getScene().getWindow();
			stage.getScene().setRoot(originalView);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@FXML
    private void onUpdateReservationClicked() {
        Reservation selectedRes = statusList.getSelectionModel().getSelectedItem();
        if (selectedRes == null) {
            System.out.println("No reservation selected!");
            return;
        }

        try {
            LocalDate newDate = datePicker.getValue();
            
            if (newDate == null) {
                System.out.println("Please select a date.");
                return;
            }
            LocalTime originalTime = selectedRes.getReservationTime().toLocalTime();
            LocalDateTime newDateTime = LocalDateTime.of(newDate, originalTime);
            selectedRes.setReservationTime(newDateTime);
            selectedRes.setNumDiners(Integer.parseInt(visitorsField.getText()));

            if (controller != null) {
                controller.sendUpdateReservationRequest(selectedRes);
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Visitors count must be a number.");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	public void updateReservationsList(Object data) {
	    Platform.runLater(new Runnable() {
	        @Override
	        public void run() {
	            if (statusList != null) {
	                statusList.getItems().clear();

	                if (data instanceof ArrayList) {
	                    ArrayList<?> list = (ArrayList<?>) data;
	                    for (Object item : list) {
	                        if (item instanceof Reservation) {
	                            statusList.getItems().add((Reservation) item);
	                        }
	                    }
	                } else if (data instanceof Reservation) {
	                    statusList.getItems().add((Reservation) data);
	                }
	            }
	        }
	    });
	}
}
