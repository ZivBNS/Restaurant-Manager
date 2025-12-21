package gui;

import entities.Subscribed_Customer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;


public class ManageUsers_GUI {



	@FXML private TextField searchField;
    @FXML private TableView<Subscribed_Customer> usersTable;
    @FXML private TableColumn<Subscribed_Customer, Integer> colId;
    //@FXML private TableColumn<Subscribed_Customer, String> colFirstName;
    //@FXML private TableColumn<Subscribed_Customer, String> colLastName;
    @FXML private TableColumn<Subscribed_Customer, String> colFullName;
    @FXML private TableColumn<Subscribed_Customer, String> colPhone;
    @FXML private TableColumn<Subscribed_Customer, String> colEmail;
    @FXML private TableColumn<Subscribed_Customer, String> colUsername;
    @FXML private TableColumn<Subscribed_Customer, Integer> colSubscriberCode;
    @FXML private TableColumn<Subscribed_Customer, String> colIdentity;

    @FXML private TextField idField;
    @FXML private TextField firstNameField, lastNameField, phoneField, emailField, usernameField, subscriberCodeField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> identityCombo;

    @FXML private Label formErrorLabel;
    @FXML private Button deleteBtn;

    private final ObservableList<Subscribed_Customer> master = FXCollections.observableArrayList();
    private Subscribed_Customer selected = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        //colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        //colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colSubscriberCode.setCellValueFactory(new PropertyValueFactory<>("subscriberCode"));
        colIdentity.setCellValueFactory(new PropertyValueFactory<>("identity"));

        colFullName.setCellValueFactory(cell ->
        	new SimpleStringProperty(cell.getValue().getFirstName() + " " + cell.getValue().getLastName())
        );
        usersTable.setItems(master);

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selected = newV;
            deleteBtn.setDisable(newV == null);
            if (newV == null) clearForm();
            else fillForm(newV);
        });

        if (identityCombo.getItems().contains("Subscriber")) {
            identityCombo.getSelectionModel().select("Subscriber");
        }

        loadUsers();
    }

    private void loadUsers() {
        // TODO: request list from server and replace master
        // master.setAll(receivedUsers);

        master.clear(); // placeholder
    }

    @FXML
    private void onRefresh() {
        loadUsers();
    }

    @FXML
    private void onAddNew() {
        usersTable.getSelectionModel().clearSelection();
        selected = null;
        clearForm();
        idField.setText(""); // new user
    }

    @FXML
    private void onSave() {
        formErrorLabel.setVisible(false);

        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText(); // don't trim passwords
        String identity = identityCombo.getValue();
        String codeText = subscriberCodeField.getText().trim();

        if (first.isEmpty() || last.isEmpty() || username.isEmpty()) {
            showError("First name, last name, and username are required.");
            return;
        }
        if (email.isEmpty() && phone.isEmpty()) {
            showError("Enter at least email or phone.");
            return;
        }
        if (identity == null || identity.isBlank()) {
            showError("Identity is required.");
            return;
        }

        Integer subscriberCode = null;
        if (!codeText.isEmpty()) {
            try {
                subscriberCode = Integer.parseInt(codeText);
            } catch (NumberFormatException e) {
                showError("Subscriber code must be a number.");
                return;
            }
        }

        if (selected == null) {
            // ADD MODE
            if (password == null || password.isEmpty()) {
                showError("Password is required for a new user.");
                return;
            }

            // Build your object (adjust constructor / setters to your actual class!)
            Subscribed_Customer newUser = new Subscribed_Customer(first, last, phone, email, username, password);

            // TODO: set identity + subscriberCode if your class supports it (setters)
            // newUser.setIdentity(identity);
            // if (subscriberCode != null) newUser.setSubscriberCode(subscriberCode);

            // TODO: send ADD message to server

            // On success:
            master.add(newUser);
            usersTable.getSelectionModel().select(newUser);

        } else {
            // EDIT MODE
            // If password empty -> keep old password (server-side recommended)
            Subscribed_Customer updated = new Subscribed_Customer(first, last, phone, email, username,
                    (password == null || password.isEmpty()) ? selected.getPassword() : password);

            // updated.setId(selected.getId());
            // updated.setIdentity(identity);
            // updated.setSubscriberCode(subscriberCode);

            // TODO: send UPDATE message to server

            int idx = master.indexOf(selected);
            if (idx >= 0) {
                master.set(idx, updated);
                usersTable.getSelectionModel().select(updated);
                selected = updated;
            }
        }

        passwordField.clear();
    }

    @FXML
    private void onDelete() {
        if (selected == null) return;

        // TODO: send DELETE message to server (by id or username)
        master.remove(selected);
        selected = null;
        clearForm();
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    @FXML
    private void onBack() {
        // TODO: navigate back
    }

    private void fillForm(Subscribed_Customer u) {
        //idField.setText(String.valueOf(u.ge));
        firstNameField.setText(safe(u.getFirstName()));
        lastNameField.setText(safe(u.getLastName()));
        phoneField.setText(safe(u.getPhone()));
        emailField.setText(safe(u.getEmail()));
        usernameField.setText(safe(u.getUsername()));
        subscriberCodeField.setText(String.valueOf(u.getSubscriberCode()));
        //identityCombo.getSelectionModel().select(safe(u.getIdentity()));
        passwordField.clear(); // don't show current password
    }

    private void clearForm() {
        idField.clear();
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        emailField.clear();
        usernameField.clear();
        subscriberCodeField.clear();
        passwordField.clear();
        formErrorLabel.setVisible(false);
        if (identityCombo.getItems().contains("Subscriber")) {
            identityCombo.getSelectionModel().select("Subscriber");
        }
    }

    private void showError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
