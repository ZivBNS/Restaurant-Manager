package gui;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import entities.Restaurant;
import entities.Restaurant_Table;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class TablesView_GUI {

	public static TablesView_GUI instance;

	@FXML private FlowPane tablesContainer;
	@FXML private Button btnBack;

	@FXML
	public void initialize() {
		instance = this;
		System.out.println("TablesView_GUI Initialized.");

		// If we already have cached tables, show them immediately
		if (Restaurant.getInstance().getTables() != null && !Restaurant.getInstance().getTables().isEmpty()) {
			loadTables(Restaurant.getInstance().getTables());
		}

		// Request fresh data from server; UI will be updated when response arrives
		if (ConnectToServer_GUI.clientController != null) {
			ConnectToServer_GUI.clientController.sendGetAllTablesRequest();
		}
	}

	/**
	 * Called by the client controller when the RETURN_ALL_TABLES response arrives.
	 */
	public void loadTables(java.util.List<Restaurant_Table> tables) {
		javafx.application.Platform.runLater(() -> {
			if (tablesContainer == null) return;
			tablesContainer.getChildren().clear();
			System.out.println("Loading tables into view: count=" + (tables == null ? 0 : tables.size()));
			if (tables == null) return;
			for (Restaurant_Table t : tables) {
				StackPane tile = createTableTile(t);
				tablesContainer.getChildren().add(tile);
				System.out.println("Added table tile: " + t.getTableNumber() + " with size " + t.getSize());
			}
		});
	}

	private StackPane createTableTile(Restaurant_Table table) {
		int seats = Math.max(1, table.getSize());
		double radius = 40;
		double seatOffset = 18;
		double seatSize = 12;

		StackPane wrapper = new StackPane();
		wrapper.setPrefSize(140, 140);

		Circle circle = new Circle(radius);
		circle.setFill(Color.web("#2b2f3a"));
		circle.setStroke(Color.web("#d4a358"));
		circle.setStrokeWidth(2);

		Label centerLabel = new Label("T" + table.getTableNumber() + "\n(" + table.getSize() + ")");
		centerLabel.setTextFill(Color.WHITE);
		centerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-alignment: center;");

		Group seatsGroup = new Group();
		for (int i = 0; i < seats; i++) {
			double angle = 2 * Math.PI * i / seats;
			double x = Math.cos(angle) * (radius + seatOffset);
			double y = Math.sin(angle) * (radius + seatOffset);

			Rectangle seat = new Rectangle(seatSize, seatSize);
			seat.setArcWidth(3);
			seat.setArcHeight(3);
			seat.setTranslateX(x);
			seat.setTranslateY(y);
			seat.setFill(Color.web("#e6e6e6")); // free seat color
			seat.setStroke(Color.web("#bdbdbd"));
			seatsGroup.getChildren().add(seat);
		}

		wrapper.getChildren().addAll(circle, seatsGroup, centerLabel);

		wrapper.setOnMouseEntered(evt -> wrapper.setScaleX(1.05));
		wrapper.setOnMouseExited(evt -> wrapper.setScaleX(1.0));

		return wrapper;
	}

	@FXML
	private void onBackClicked(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("Workers.fxml"));
			Stage stage = (Stage) btnBack.getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.centerOnScreen();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
