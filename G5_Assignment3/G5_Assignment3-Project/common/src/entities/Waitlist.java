package entities;

import java.time.LocalDateTime;

public class Waitlist {

    private int id=-1;
    private Reservation reservation;
    private String status;
    private LocalDateTime creationTime;
    private LocalDateTime tableFreedTime;

    //read from db
    public Waitlist(int id, Reservation reservation, String status, LocalDateTime creationTime, LocalDateTime tableFreedTime) {
        this.id = id;
        this.reservation = reservation;
        this.status = status;
        this.creationTime = creationTime;
        this.tableFreedTime = tableFreedTime;
    }

    //create new to insert to db
    public Waitlist(Reservation reservation) {
        this.reservation = reservation;
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

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
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
        return "Waitlist [ID=" + id + ", OrderID=" + (reservation != null ? reservation.getId() : "N/A") + ", Status=" + status + "]";
    }
}




/*

    @Override
    public String toString() {
        return "WaitlistEntry [ID=" + waitlistId + ", Diners=" + numDiners + ", Status=" + status + "]";
    }
}
*/