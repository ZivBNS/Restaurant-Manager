package entities;


public class Restaurant_Table {

    private int id=-1;
    private int tableNumber=-1;
    private int size;        
    private boolean isActive = true;
    
    public Restaurant_Table(int id,int tableNumber, int size,boolean isActive) {
        this.id = id;
        this.setTableNumber(tableNumber);
        this.size=size;
        this.isActive=isActive;
    }

    public Restaurant_Table(int size) {
        this.size = size;
    }
    
    public void setTableSize(int size) {
        this.size = size;
    }
        
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSize() {
		return size;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
    public String toString() {
        return "Table number " + id + " [Seats: " + size + ", Status: " + ((isActive)?"active":"not active") + "]";
    }

	public int getTableNumber() {
		return tableNumber;
	}

	public void setTableNumber(int tableNumber) {
		this.tableNumber = tableNumber;
	}
}