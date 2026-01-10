package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Restaurant entity. 
 * This class implements the **Singleton Pattern**, ensuring that only one 
 * instance of the Restaurant object exists throughout the application's lifecycle.
 * It holds the global state of the restaurant, including tables and opening hours.
 */
public class Restaurant {

    // --- Singleton Instance ---
    /** The single static instance of the Restaurant class */
    private static Restaurant instance;

    // --- Static Fields ---
    /** Stores the capacity of the largest table (used for UI limits and validation) */
    private static int biggestTableSize;

    // --- Instance Fields ---
    /** The unique identifier for the restaurant */
    private int id;
    /** The name of the restaurant */
    private String name;

    /** * List of all physical tables in the restaurant.
     * IMPORTANT: Initialize with an empty ArrayList to prevent NullPointerException
     */
    private List<Restaurant_Table> tables = new ArrayList<>();
    
    /** The operating hours and schedule for the restaurant */
    private Opening_Hours openingHours;

    // --- Private Constructor (Singleton) ---
    
    /**
     * Private constructor to prevent direct instantiation from other classes.
     * Initializes default values such as the restaurant name.
     */
    private Restaurant() {
        this.name = "Bistro"; // Default name
        this.tables = new ArrayList<>(); // Double safety: ensure list is not null
    }

    // --- Static Accessor Method ---

    /**
     * Returns the single instance of the Restaurant class.
     * Creates the instance if it does not exist yet (Lazy Initialization).
     * @return The Singleton Restaurant instance.
     */
    public static Restaurant getInstance() {
        if (instance == null) {
            instance = new Restaurant();
        }
        return instance;
    }

    // --- Business Logic Methods ---

    /**
     * Adds a new table to the restaurant's list.
     * Performs a null check on the tables list before adding.
     * @param table The Restaurant_Table object to add.
     */
    public void addTable(Restaurant_Table table) {
        if (this.tables == null) {
            this.tables = new ArrayList<>();
        }
        tables.add(table);
    }

    /**
     * Searches for a table by its visible table number.
     * @param tableNumber The specific table number to search for.
     * @return The Restaurant_Table object if found, otherwise null.
     */
    public Restaurant_Table getTableByNumber(int tableNumber) {
        if (tables == null) return null;
        
        for (Restaurant_Table table : tables) {
            if (table.getTableNumber() == tableNumber) {
                return table;
            }
        }
        return null;
    }

    // --- Getters and Setters ---

    /** @return The restaurant's internal ID */
    public int getId() {
        return id;
    }

    /** @param id Set the restaurant's internal ID */
    public void setId(int id) {
        this.id = id;
    }

    /** @return The name of the restaurant */
    public String getName() {
        return name;
    }

    /** @param name Set the name of the restaurant */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the list of tables.
     * Includes defensive coding to ensure a null list is never returned.
     * @return A List of Restaurant_Table objects.
     */
    public List<Restaurant_Table> getTables() {
        // Defensive coding: Ensure we never return null to the GUI or other layers
        if (tables == null) {
            tables = new ArrayList<>();
        }
        return tables;
    }

    /** @param tables Set the list of restaurant tables */
    public void setTables(List<Restaurant_Table> tables) {
        this.tables = tables;
    }

    /** @return The current Opening_Hours object */
    public Opening_Hours getOpeningHours() {
        return openingHours;
    }

    /** @param openingHours Set the restaurant's opening hours */
    public void setOpeningHours(Opening_Hours openingHours) {
        this.openingHours = openingHours;
    }

    /** @return The size/capacity of the largest table in the system */
    public static int getBiggestTableSize() {
        return biggestTableSize;
    }

    /** @param biggestTableSize Set the maximum table size for the system */
    public static void setBiggestTableSize(int biggestTableSize) {
        Restaurant.biggestTableSize = biggestTableSize;
    }

    /**
     * Returns a summary of the restaurant, including name and total table count.
     */
    @Override
    public String toString() {
        int count = (tables != null) ? tables.size() : 0;
        return "Restaurant: " + name + " [Total Tables: " + count + "]";
    }
}