package entities;


public class Restaurant_Table {

    public static final String STATUS_AVAILABLE = "AVAILABLE"; 
    public static final String STATUS_OCCUPIED = "OCCUPIED";   
    public static final String STATUS_RESERVED = "RESERVED";

    private int id;        
    private int tableNumber;    
    private int seats;         
    private String status;

    public Restaurant_Table(int id, int tableNumber, int seats) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.seats = seats;
        this.status = STATUS_AVAILABLE; // כשיוצרים שולחן, הוא מתחיל כפנוי
    }

    public Restaurant_Table(int id, int tableNumber, int seats, String status) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.seats = seats;
        this.status = status;
    }

    public Restaurant_Table() {}


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(this.status);
    }

    @Override
    public String toString() {
        return "Table " + tableNumber + " [Seats: " + seats + ", Status: " + status + "]";
    }
}