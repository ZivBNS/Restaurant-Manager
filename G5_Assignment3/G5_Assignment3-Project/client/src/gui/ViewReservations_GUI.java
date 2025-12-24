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
import javafx.stage.Stage;
import javafx.util.Callback;
import messages.Message;
import messages.MessageType;

/**
 * Controller for viewing and managing user reservations. 
 * Provides dynamic editing with local validation and real-time availability checks.
 */
public class ViewReservations_GUI {
	public static ViewReservations_GUI instance;

	@FXML private TableView<Reservation> reservationsTable;
	@FXML private TableColumn<Reservation, Integer> colCode;
	@FXML private TableColumn<Reservation, String> colDate, colTime;
	@FXML private TableColumn<Reservation, Integer> colGuests;

	@FXML private DatePicker editDatePicker;
	@FXML private ComboBox<String> editTimeCombo;
	@FXML private TextField editGuestsField;

	private ObservableList<Reservation> masterData = FXCollections.observableArrayList();
	@FXML
	public void initialize() {
		instance = this;
		setupTable();
		restrictDatePickerRange();

		// Request opening hours from the server
		ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
		refreshTableData();

		// Listener for table selection using Anonymous Inner Class
		reservationsTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Reservation>() {
			@Override
			public void changed(ObservableValue<? extends Reservation> obs, Reservation oldVal, Reservation newVal) {
				if (newVal != null) {
					populateEditFields(newVal);
				}
			}
		});

		// Listener for DatePicker changes to update available slots
		editDatePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
			@Override
			public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldVal, LocalDate newVal) {
				if (newVal != null) {
					// Load hours without a specific selection (for manual date change)
					loadDynamicHours(newVal, null);
				}
			}
		});
	}

	/**
	 * Maps TableView columns to Reservation entity properties.
	 */
	private void setupTable() {
		reservationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
		colDate.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
		colTime.setCellValueFactory(new PropertyValueFactory<>("formattedTime"));
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfDiners"));
		reservationsTable.setItems(masterData);
	}

	/**
	 * Populates the hours ComboBox based on DB hours.
	 * Fixed: Now accepts an optional timeToSelect to handle selection during Platform.runLater cycle.
	 */
	public void loadDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				editTimeCombo.getItems().clear();
				Opening_Hours oh = Restaurant.getInstance().getOpeningHours();

				if (oh == null) {
					ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
					return;
				}

				LocalTime open = null, close = null;
				if (oh.getExceptionSchedule().containsKey(selectedDate)) {
					TimeRange range = oh.getExceptionSchedule().get(selectedDate);
					if (range != null) { open = range.getOpenTime(); close = range.getCloseTime(); }
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
						LocalTime bufferTime = roundToNext30Min(LocalTime.now().plusHours(1));
						boolean skip = isMidnightCrossing ? (bufferTime.isAfter(open) || bufferTime.isBefore(close)) : bufferTime.isAfter(open);
						if (skip) firstAvailable = bufferTime;
					}

					LocalTime t = firstAvailable;
					int safety = 0;
					while (safety < 48) {
						boolean isValid = isMidnightCrossing ? (!t.isBefore(open) || !t.isAfter(lastSlot)) : (!t.isBefore(open) && !t.isAfter(lastSlot));
						if (isValid) {
							editTimeCombo.getItems().add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
						} else break;
						if (t.equals(lastSlot)) break;
						t = t.plusMinutes(30);
						safety++;
					}
					
					// Selection fix: Set value only after items are re-added
					if (timeToSelect != null) {
						editTimeCombo.setValue(timeToSelect);
					}
				}
			}
		});
	}

	private LocalTime roundToNext30Min(LocalTime time) {
		int min = time.getMinute();
		if (min == 0) return time.withSecond(0).withNano(0);
		if (min <= 30) return time.withMinute(30).withSecond(0).withNano(0);
		return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
	}

	/**
	 * Populates edit fields when an order is selected.
	 */
	private void populateEditFields(Reservation res) {
		editDatePicker.setValue(res.getOrderStartTime().toLocalDate());
		String timeStr = String.format("%02d:%02d", res.getOrderStartTime().getHour(), res.getOrderStartTime().getMinute());
		
		// Load hours and pass the specific time to select
		loadDynamicHours(res.getOrderStartTime().toLocalDate(), timeStr);
		editGuestsField.setText(String.valueOf(res.getNumberOfDiners()));
	}

	/**
     * Validates updated input and dynamically adjusts the reservation date for 
     * post-midnight time slots before sending the update request to the server.
     * This ensures chronological consistency if a user moves a booking to a time
     * that falls after midnight in a single business shift.
     * * @param event The action event triggered by the Update button.
     */
    @FXML
    void onUpdateClicked(ActionEvent event) {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // 1. Validation: Check for empty fields
        if (editDatePicker.getValue() == null || editTimeCombo.getValue() == null || editGuestsField.getText().trim().isEmpty()) {
            showErrorAlert("Input Error", "All fields are required for the update.");
            return;
        }

        int diners;
        try {
            // 2. Validation: Numeric check
            diners = Integer.parseInt(editGuestsField.getText().trim());
            if (diners < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showErrorAlert("Input Error", "Number of guests must be a valid positive number.");
            return;
        }

        try {
            // --- START OF DYNAMIC DATE ADJUSTMENT LOGIC ---
            
            LocalDate selectedDate = editDatePicker.getValue();
            LocalTime selectedTime = LocalTime.parse(editTimeCombo.getValue());
            
            // Fetch opening hours to determine the start of the business day
            Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
            LocalTime openTime = null;

            if (oh != null) {
                // Determine opening time for the specific day
                if (oh.getExceptionSchedule().containsKey(selectedDate)) {
                    openTime = oh.getExceptionSchedule().get(selectedDate).getOpenTime();
                } else {
                    openTime = oh.getRegularSchedule().get(selectedDate.getDayOfWeek()).getOpenTime();
                }
            }

            /**
             * Dynamic Midnight Fix:
             * If the selected time is chronologically BEFORE the opening time, 
             * it belongs to the following calendar day (e.g., Selected 01:00 AM 
             * when opening is 16:00 PM).
             */
            if (openTime != null && selectedTime.isBefore(openTime)) {
                selectedDate = selectedDate.plusDays(1);
            }

            LocalDateTime newStart = LocalDateTime.of(selectedDate, selectedTime);
            
            // --- END OF DYNAMIC DATE ADJUSTMENT LOGIC ---

            // Apply changes to the selected object
            selected.setOrderStartTime(newStart);
            selected.setOrderEndTime(newStart.plusHours(2)); // Standard 2-hour duration
            selected.setNumberOfDiners(diners);

            // Send updated object to server via the pipeline
            Message msg = new Message(MessageType.UPDATE_RESERVATION_REQUEST, selected);
            ConnectToServer_GUI.clientController.sendComplexObject(msg);

        } catch (Exception e) {
            showErrorAlert("System Error", "Failed to process update: " + e.getMessage());
        }
    }

	/**
	 * Helper method to display error alerts.
	 */
	private void showErrorAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * Displays an alert when the requested update cannot be fulfilled due to capacity.
	 * If a suggestion is provided, it updates the input fields so the user can review 
	 * and manually submit the update again if they agree.
	 * * @param suggested The alternative LocalDateTime suggested by the server.
	 */
	public void showNoTableAlert(final LocalDateTime suggested) {
	    // Ensure UI updates happen on the JavaFX Application Thread
	    Platform.runLater(new Runnable() {
	        @Override
	        public void run() {
	            if (suggested == null) {
	                Alert alert = new Alert(Alert.AlertType.ERROR);
	                alert.setTitle("Fully Booked");
	                alert.setHeaderText("No Availability Found");
	                alert.setContentText("We are sorry, but there are no available tables for the rest of the day.");
	                alert.showAndWait();
	                return;
	            }

	            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
	            String displayString = suggested.format(dateFormatter) + " at " + suggested.format(timeFormatter);

	            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
	            alert.setTitle("Capacity Reached");
	            alert.setHeaderText("The restaurant is full at the selected time.");
	            alert.setContentText("The nearest available slot is: " + displayString 
	                                + ".\n\nWould you like to update the form to this time?");

	            // Use showAndWait and handle the result via Anonymous Inner Class consumer
	            alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
	                @Override
	                public void accept(ButtonType response) {
	                    if (response == ButtonType.OK) {
	                        // Only update the UI fields. 
	                        // DO NOT call the update/save method here to prevent loops.
	                        updateFormFields(suggested);
	                    }
	                }
	            });
	        }
	    });
	}
	/**
	 * Updates the date and time selection fields in the GUI with the suggested values.
	 * * @param dateTime The suggested date and time to be filled into the fields.
	 */
	private void updateFormFields(LocalDateTime dateTime) {
	    // Assuming these are the names of your fields in ViewReservations_GUI
	    if (editDatePicker != null) {
	        editDatePicker.setValue(dateTime.toLocalDate());
	    }
	    
	    if (editTimeCombo != null) {
	        String timeStr = String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
	        editTimeCombo.setValue(timeStr);
	    }
	}
	public void showSuccessAlert() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reservation updated successfully!", ButtonType.OK);
		alert.showAndWait();
	}

	private void restrictDatePickerRange() {
		final LocalDate minDate = LocalDate.now();
		final LocalDate maxDate = LocalDate.now().plusMonths(1);
		editDatePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
			@Override
			public DateCell call(final DatePicker datePicker) {
				return new DateCell() {
					@Override
					public void updateItem(LocalDate item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null && (item.isBefore(minDate) || item.isAfter(maxDate))) {
							setDisable(true);
							setStyle("-fx-background-color: #eeeeee;");
						}
					}
				};
			}
		});
	}

	private void refreshTableData() {
		Object id = (User_Session.getLoggedInUser() != null) ? User_Session.getLoggedInUser() : User_Session.getCasualPhone();
		ConnectToServer_GUI.clientController.sendGetReservationsRequest(id);
	}

	@FXML
	void onDeleteClicked(ActionEvent event) {
		final Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
		if (selected == null) return;
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirm cancellation?");
		alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
			@Override
			public void accept(ButtonType r) {
				if (r == ButtonType.OK) {
					Message msg = new Message(MessageType.CANCEL_RESERVATION , selected.getId());
					ConnectToServer_GUI.clientController.sendComplexObject(msg);
				}
			}
		});
	}

	@FXML
	void onBackClicked(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/gui/CasualCustomer.fxml"));
			Stage stage = (Stage) reservationsTable.getScene().getWindow();
			stage.setScene(new Scene(root));
		} catch (Exception e) { e.printStackTrace(); }
	}

	public void updateTable(List<Reservation> list) {
		masterData.setAll(list);
	}
}