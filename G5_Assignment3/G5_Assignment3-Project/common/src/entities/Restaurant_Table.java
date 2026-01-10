package entities;

/**
 * Represents a physical table within the restaurant.
 * This class tracks the table's capacity, identification numbers, and its 
 * operational status (whether it is currently active/available for use).
 */
public class Restaurant_Table {

    /** Unique internal database identifier for the table */
    private int id=-1;
    /** The specific number assigned to the table in the restaurant layout */
    private int tableNumber=-1;
    /** The maximum number of diners that can be seated at this table */
    private int size;        
    /** Flag indicating if the table is currently active and available for reservations */
    private boolean isActive = true;
    
    /**
     * Full constructor for retrieving a table record from the database.
     * * @param id The internal ID.
     * @param tableNumber The display number of the table.
     * @param size The seating capacity.
     * @param isActive The operational status.
     */
    public Restaurant_Table(int id,int tableNumber, int size,boolean isActive) {
        this.id = id;
        this.setTableNumber(tableNumber);
        this.size=size;
        this.isActive=isActive;
    }

    /**
     * Minimal constructor for creating a new table with a specific size.
     * * @param size The seating capacity of the table.
     */
    public Restaurant_Table(int size) {
        this.size = size;
    }
    
    /**
     * Updates the seating capacity of the table.
     * * @param size The new size to be set.
     */
    public void setTableSize(int size) {
        this.size = size;
    }
        
    /** @return The unique internal ID of the table */
    public int getId() {
		return id;
	}

	/** @param id Set the internal ID of the table */
	public void setId(int id) {
		this.id = id;
	}

	/** @return The seating capacity of the table */
	public int getSize() {
		return size;
	}

	/** @return True if the table is active, false otherwise */
	public boolean isActive() {
		return isActive;
	}

	/** @param isActive Set the operational status of the table */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * Returns a string representation of the table including ID, size, and status.
	 * Note: Displays the internal ID as the table identification in this format.
	 */
	@Override
    public String toString() {
        return "Table number " + id + " [Seats: " + size + ", Status: " + ((isActive)?"active":"not active") + "]";
    }

	/** @return The designated table number */
	public int getTableNumber() {
		return tableNumber;
	}

	/** @param tableNumber Set the designated table number */
	public void setTableNumber(int tableNumber) {
		this.tableNumber = tableNumber;
	}
}