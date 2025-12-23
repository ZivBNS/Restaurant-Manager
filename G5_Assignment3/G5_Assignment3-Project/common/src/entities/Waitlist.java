package entities;

import java.time.LocalDateTime;

public class Waitlist {

    private int id=-1;
    private int reservationID;
    private String status;
    private LocalDateTime creationTime;
    private LocalDateTime tableFreedTime;

    //Waitlist (ID INT , ReservationID INT, Status VARCHAR(25),creationTime DATETIME, TableFreedTime DATETIME
    //read from db
    public Waitlist(int id, int reservationID, String status, LocalDateTime creationTime, LocalDateTime tableFreedTime) {
        this.id = id;
        this.reservationID = reservationID;
        this.status = status;
        this.creationTime = creationTime;
        this.tableFreedTime = tableFreedTime;
    }

    //create new to insert to db
    public Waitlist(int reservationID) {
        this.reservationID = reservationID;
        this.status = "WAITING";
        this.creationTime = LocalDateTime.now();
        this.tableFreedTime = null;
    }
    
    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReservation() {
        return reservationID;
    }

    public void setReservation(int reservationID) {
        this.reservationID = reservationID;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getTableFreedTime() {
        return tableFreedTime;
    }

    public void setTableFreedTime(LocalDateTime tableFreedTime) {
        this.tableFreedTime = tableFreedTime;
    }

    @Override
    public String toString() {
        return "Waitlist [ID=" + id + ", OrderID=" + (reservationID != -1 ? reservationID : "N/A") + ", Status=" + status + "]";
    }
}




/*

    @Override
    public String toString() {
        return "WaitlistEntry [ID=" + waitlistId + ", Diners=" + numDiners + ", Status=" + status + "]";
    }
}
*/