package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Restaurant entity. Implements the Singleton pattern because
 * there is only one restaurant instance in the system.
 */
public class Restaurant {

	// The single instance of the class
	private static Restaurant instance;

	private int id;
	private String name;

	private List<Restaurant_Table> tables;
	private Opening_Hours openingHours;

	// --- Private Constructor (Singleton) ---
	private Restaurant() {
		this.name = "Bistro"; // Default name
		this.tables = new ArrayList<>();
		this.openingHours = new Opening_Hours();
	}

	// --- Static Accessor Method ---
	public static Restaurant getInstance() {
		if (instance == null) {
			instance = new Restaurant();
		}
		return instance;
	}

	public void addTable(Restaurant_Table table) {
		this.tables.add(table);
	}

	public Restaurant_Table getTableByNumber(int tableNumber) {
		for (Restaurant_Table table : tables) {
			if (table.getTableNumber() == tableNumber) {
				return table;
			}
		}
		return null;
	}

	// --- Getters and Setters ---

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Restaurant_Table> getTables() {
		return tables;
	}

	public void setTables(List<Restaurant_Table> tables) {
		this.tables = tables;
	}

	public Opening_Hours getOpeningHours() {
		return openingHours;
	}

	public void setOpeningHours(Opening_Hours openingHours) {
		this.openingHours = openingHours;
	}

	@Override
	public String toString() {
		return "Restaurant: " + name + " [Total Tables: " + tables.size() + "]";
	}
}