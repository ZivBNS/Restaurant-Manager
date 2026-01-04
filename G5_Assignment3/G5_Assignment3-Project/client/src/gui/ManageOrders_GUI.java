package gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import entities.Opening_Hours;
import entities.Opening_Hours.TimeRange;
import entities.Reservation;
import entities.Restaurant;
import entities.UserRecord;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import messages.Message;
import messages.MessageType;

/**
 * Unified Controller for Managing Reservations (Admin View). Handles table
 * display, manual creation, and administrative updates. Styled with a dark
 * theme to match the AddReservation screen.
 */
public class ManageOrders_GUI {

	/** Static instance for external access from Client_Controller. */
	public static ManageOrders_GUI instance;

	// --- Table Components ---
	@FXML
	private TableView<Reservation> adminTable;
	@FXML
	private TableColumn<Reservation, Integer> colCode, colTable, colGuests, colUserID;
	@FXML
	private TableColumn<Reservation, String> colPhone, colEmail, colDate, colTime, colStatus;

	// --- Form Components ---
	@FXML
	private TextField txtOrderID, txtUserID, txtPhone, txtEmail, txtTable;
	@FXML
	private DatePicker datePicker;
	@FXML
	private ComboBox<String> timeCombo;
	@FXML
	private ComboBox<String> cbStatus;
	@FXML
	private Spinner<Integer> spGuests;

	// Buttons
	@FXML
	private Button btnAdd; // Added for disabling during edit mode
	@FXML
	private Button btnUpdate, btnCancel;

	/** Stores the database ID of the user fetched via lookup. */
	private Integer activeInternalUserId = null;

	private ObservableList<Reservation> masterData = FXCollections.observableArrayList();

	/**
	 * Initializes the controller. Sets up table columns, spinner, status dropdown,
	 * and dark-themed component styling.
	 */
	@FXML
	public void initialize() {
		instance = this;
		setupAdminTable();
		restrictDatePickerRange();
		setupUserIDListener();
		setupComboBoxStyling();

		// Initialize Status choices
		cbStatus.getItems().addAll("Pending", "Active", "Completed", "No_show", "Canceled");
		cbStatus.setValue("Pending");
		cbStatus.setDisable(true); // Locked for new reservations by default

		// Initialize Guests Spinner
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
		spGuests.setValueFactory(valueFactory);

		// Fetch operational data from server
		ConnectToServer_GUI.clientController.sendGetOpeningHoursRequest();
		refreshAdminData();

		// Listener for table row selection
		adminTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Reservation>() {
			@Override
			public void changed(ObservableValue<? extends Reservation> obs, Reservation oldV, Reservation newV) {
				if (newV != null) {
					populateForm(newV);
					setEditMode(true);
				}
			}
		});

		// Listener for DatePicker changes to reload time slots
		datePicker.valueProperty().addListener(new ChangeListener<LocalDate>() {
			@Override
			public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldV, LocalDate newV) {
				if (newV != null) {
					loadDynamicHours(newV, null);
				}
			}
		});

		// Listener to reset verification if UserID text is edited manually
		txtUserID.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.equals(oldValue)) {
					activeInternalUserId = null;
				}
			}
		});
	}

	/**
	 * Applies dark mode styling to the Time ComboBox cells to match AddReservation
	 * screen.
	 */
	private void setupComboBoxStyling() {
		timeCombo.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> param) {
				return new ListCell<String>() {
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (item == null || empty) {
							setText(null);
						} else {
							setText(item);
							setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;");
						}
					}
				};
			}
		});

		timeCombo.setButtonCell(new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null || empty) {
					setText(null);
				} else {
					setText(item);
					setStyle("-fx-text-fill: #f8fafc;");
				}
			}
		});
	}

	/**
	 * Sets up the UserID field listener to handle Enter key for subscriber lookup.
	 */
	private void setupUserIDListener() {
		txtUserID.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					String idStr = txtUserID.getText().trim();
					if (!idStr.isEmpty()) {
						try {
							int subCode = Integer.parseInt(idStr);
							Message msg = new Message(MessageType.GET_USER_DETAILS, subCode);
							ConnectToServer_GUI.clientController.sendComplexObject(msg);
							System.out.println("Sent request for User ID: " + subCode);
						} catch (NumberFormatException e) {
							showAlert("Input Error", "Subscriber Code must be numeric.");
						}
					}
				}
			}
		});
	}

	/**
	 * Fills the form with details of a found subscriber.
	 * 
	 * @param user The user record returned from the server.
	 */
	public void fillUserDetails(final UserRecord user) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (user != null) {
					activeInternalUserId = user.getId();
					txtPhone.setText(user.getPhone());
					txtEmail.setText(user.getEmail());
				} else {
					showAlert("User Not Found", "No subscriber found with this Code.");
					activeInternalUserId = null;
					txtPhone.clear();
					txtEmail.clear();
				}
			}
		});
	}

	/**
	 * Maps table columns and sets resize policy.
	 */
	private void setupAdminTable() {
		adminTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		colCode.setCellValueFactory(new PropertyValueFactory<Reservation, Integer>("confirmationCode"));
		colUserID.setCellValueFactory(new PropertyValueFactory<Reservation, Integer>("userId"));
		colPhone.setCellValueFactory(new PropertyValueFactory<Reservation, String>("phone"));
		colEmail.setCellValueFactory(new PropertyValueFactory<Reservation, String>("email"));
		colDate.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedDate"));
		colTime.setCellValueFactory(new PropertyValueFactory<Reservation, String>("formattedTime"));
		colGuests.setCellValueFactory(new PropertyValueFactory<Reservation, Integer>("numberOfDiners"));
		colTable.setCellValueFactory(new PropertyValueFactory<Reservation, Integer>("tableId"));
		colStatus.setCellValueFactory(new PropertyValueFactory<Reservation, String>("status"));
		adminTable.setItems(masterData);
	}

	/**
	 * Restricts the DatePicker range and applies DARK styling to cells.
	 */
	private void restrictDatePickerRange() {
		final LocalDate minDate = LocalDate.now();
		final LocalDate maxDate = LocalDate.now().plusMonths(1);
		datePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
			@Override
			public DateCell call(final DatePicker datePicker) {
				return new DateCell() {
					@Override
					public void updateItem(LocalDate item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null && (item.isBefore(minDate) || item.isAfter(maxDate))) {
							setDisable(true);
							// Use dark mode style for disabled dates to match AddReservation screen
							setStyle(
									"-fx-background-color: #4a4a4a; -fx-control-inner-background: #4a4a4a; -fx-text-fill: white;");
						} else {
							setStyle("-fx-text-fill: white;");
						}
					}
				};
			}
		});
	}

	/**
	 * Populates the editor form with data from a selected reservation.
	 * 
	 * @param res The reservation to edit.
	 */
	private void populateForm(Reservation res) {
		txtOrderID.setText(String.valueOf(res.getConfirmationCode()));
		txtUserID.setText(res.getUserId() != null ? String.valueOf(res.getUserId()) : "");
		this.activeInternalUserId = res.getUserId();

		txtPhone.setText(res.getPhone());
		txtEmail.setText(res.getEmail());
		spGuests.getValueFactory().setValue(res.getNumberOfDiners());
		txtTable.setText(res.getTableId() != null ? String.valueOf(res.getTableId()) : "Auto");

		String status = res.getStatus();
		if (status != null)
			cbStatus.setValue(status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase());

		cbStatus.setDisable(false); // Allow status editing for existing orders
		datePicker.setValue(res.getOrderStartTime().toLocalDate());
		String timeStr = String.format("%02d:%02d", res.getOrderStartTime().getHour(),
				res.getOrderStartTime().getMinute());
		loadDynamicHours(res.getOrderStartTime().toLocalDate(), timeStr);
	}

	/**
	 * Resets the form to a "New Order" state.
	 */
	@FXML
	void onClearClicked(ActionEvent event) {
		adminTable.getSelectionModel().clearSelection();
		txtOrderID.setText("New");
		txtUserID.setDisable(false);
		txtUserID.clear();
		this.activeInternalUserId = null;
		txtPhone.clear();
		txtEmail.clear();
		spGuests.getValueFactory().setValue(2);
		txtTable.setText("Auto");
		cbStatus.setValue("Pending");
		cbStatus.setDisable(true); // Locked for new orders
		datePicker.setValue(LocalDate.now());
		loadDynamicHours(LocalDate.now(), null);
		setEditMode(false);
	}

	/**
	 * Toggles interaction buttons based on selection. Disables the "Create" button
	 * if an order is being updated.
	 */
	private void setEditMode(boolean isEdit) {
		btnUpdate.setDisable(!isEdit);
		btnCancel.setDisable(!isEdit);
		txtUserID.setDisable(isEdit); // Lock ID during editing

		// --- NEW CHANGE: Disable Create Order button during Edit mode ---
		btnAdd.setDisable(isEdit);
	}

	/**
	 * Validates and submits a new reservation request.
	 */
	@FXML
	void onAddClicked(ActionEvent event) {
		if (!validateInput())
			return;
		try {
			Reservation newRes = buildReservationFromForm();
			newRes.setStatus("Pending");
			ConnectToServer_GUI.clientController.sendNewReservationRequest(newRes);
		} catch (RuntimeException e) {
			showAlert("Verification Required", e.getMessage());
		} catch (Exception e) {
			showAlert("Error", "Failed to create: " + e.getMessage());
		}
	}

	/**
	 * Validates and submits an update request for an existing reservation.
	 */
	@FXML
	void onUpdateClicked(ActionEvent event) {
		Reservation selected = adminTable.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;
		if (!validateInput())
			return;
		try {
			Reservation updatedRes = buildReservationFromForm();
			updatedRes.setId(selected.getId());
			updatedRes.setConfirmationCode(selected.getConfirmationCode());
			updatedRes.setStatus(cbStatus.getValue());
			Message msg = new Message(MessageType.ADMIN_UPDATE_RESERVATION, updatedRes);
			ConnectToServer_GUI.clientController.sendComplexObject(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Confirms and sends a cancellation request.
	 */
	@FXML
	void onDeleteClicked(ActionEvent event) {
		final Reservation selected = adminTable.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
				"Cancel Reservation #" + selected.getConfirmationCode() + "?", ButtonType.YES, ButtonType.NO);
		alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
			@Override
			public void accept(ButtonType response) {
				if (response == ButtonType.YES) {
					Message msg = new Message(MessageType.CANCEL_RESERVATION, selected.getId());
					ConnectToServer_GUI.clientController.sendComplexObject(msg);
				}
			}
		});
	}

	private boolean validateInput() {
		if (txtPhone.getText().isEmpty() && txtEmail.getText().isEmpty()) {
			showAlert("Validation Error", "Please fill Phone or Email.");
			return false;
		}
		if (datePicker.getValue() == null || timeCombo.getValue() == null) {
			showAlert("Validation Error", "Please select Date and Time.");
			return false;
		}
		return true;
	}

	/**
	 * Logic to create a Reservation object from form fields, adjusting for shifts
	 * crossing midnight.
	 */
	private Reservation buildReservationFromForm() {
		if (!txtUserID.getText().trim().isEmpty() && activeInternalUserId == null) {
			throw new RuntimeException("Please press ENTER in User ID box to verify the subscriber first.");
		}
		LocalDate d = datePicker.getValue();
		LocalTime t = LocalTime.parse(timeCombo.getValue());
		Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
		LocalTime openTime = null;
		if (oh != null) {
			if (oh.getExceptionSchedule().containsKey(d)) {
				openTime = oh.getExceptionSchedule().get(d).getOpenTime();
			} else {
				openTime = oh.getRegularSchedule().get(d.getDayOfWeek()).getOpenTime();
			}
		}
		if (openTime != null && t.isBefore(openTime)) {
			d = d.plusDays(1);
		}
		LocalDateTime start = LocalDateTime.of(d, t);
		return new Reservation(activeInternalUserId, txtPhone.getText().trim(), txtEmail.getText().trim(), start,
				start.plusHours(2), spGuests.getValue());
	}

	/**
	 * Dynamically loads available time slots based on restaurant hours.
	 */
	public void loadDynamicHours(final LocalDate selectedDate, final String timeToSelect) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				timeCombo.getItems().clear();
				Opening_Hours oh = Restaurant.getInstance().getOpeningHours();
				if (oh == null)
					return;

				LocalTime open = null, close = null;
				if (oh.getExceptionSchedule().containsKey(selectedDate)) {
					TimeRange range = oh.getExceptionSchedule().get(selectedDate);
					if (range.isActive()) {
						open = range.getOpenTime();
						close = range.getCloseTime();
					}
				} else {
					TimeRange range = oh.getRegularSchedule().get(selectedDate.getDayOfWeek());
					if (range.isActive()) {
						open = range.getOpenTime();
						close = range.getCloseTime();
					}
				}

				if (open != null && close != null) {
					LocalTime first = open;
					LocalTime last = close.minusHours(2);
					boolean isMidnightCrossing = !close.isAfter(open);

					if (selectedDate.equals(LocalDate.now())) {
						LocalTime buffer = roundToNext30Min(LocalTime.now().plusHours(1));
						if (isMidnightCrossing ? (buffer.isAfter(open) || buffer.isBefore(close))
								: buffer.isAfter(open))
							first = buffer;
					}

					LocalTime curr = first;
					int safety = 0;
					while (safety < 48) {
						boolean valid = isMidnightCrossing ? (!curr.isBefore(open) || !curr.isAfter(last))
								: (!curr.isBefore(open) && !curr.isAfter(last));
						if (valid)
							timeCombo.getItems().add(String.format("%02d:%02d", curr.getHour(), curr.getMinute()));
						else
							break;
						if (curr.equals(last))
							break;
						curr = curr.plusMinutes(30);
						safety++;
					}
					if (timeToSelect != null)
						timeCombo.setValue(timeToSelect);
				}
			}
		});
	}

	private LocalTime roundToNext30Min(LocalTime time) {
		int min = time.getMinute();
		if (min == 0)
			return time.withSecond(0).withNano(0);
		if (min <= 30)
			return time.withMinute(30).withSecond(0).withNano(0);
		return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
	}

	@FXML
	public void refreshAdminData() {
		ConnectToServer_GUI.clientController.sendGetAllPendingAndActiveReservationsRequest();
	}

	@FXML
	void onCreateBillClicked(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/BillManager.fxml"));
			Stage s = new Stage();
			s.setTitle("Bill Manager");
			s.initModality(Modality.WINDOW_MODAL);
			s.initOwner(adminTable.getScene().getWindow());
			s.setScene(new Scene(loader.load()));
			s.showAndWait();
			refreshAdminData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onBackClicked(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/gui/Workers.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
			stage.centerOnScreen();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateAdminUI(final List<Reservation> list) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				masterData.setAll(list);
			}
		});
	}

	private void showAlert(final String title, final String content) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				new Alert(Alert.AlertType.WARNING, content).showAndWait();
			}
		});
	}

	public void showSuccessAlert(final int code) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				new Alert(Alert.AlertType.INFORMATION, "Reservation created successfully.\nCode: " + code)
						.showAndWait();
				refreshAdminData();
				onClearClicked(null);
			}
		});
	}

	public void showNoTableAlert(final LocalDateTime suggested) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (suggested == null) {
					new Alert(Alert.AlertType.ERROR, "Fully Booked").show();
					return;
				}
				String msg = "Full. Nearest: " + suggested.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
						+ ". Update form?";
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg);
				alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
					@Override
					public void accept(ButtonType response) {
						if (response == ButtonType.OK) {
							datePicker.setValue(suggested.toLocalDate());
							loadDynamicHours(suggested.toLocalDate(),
									String.format("%02d:%02d", suggested.getHour(), suggested.getMinute()));
						}
					}
				});
			}
		});
	}
}