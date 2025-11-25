package entities;

import java.time.LocalDateTime;

public class Waitlist {

    private int waitlistId;
    private Casual_Customer customer;
    private int numDiners;        
    private String confirmationCode; 
    private LocalDateTime entryTime;
    private String status;

    public Waitlist(int waitlistId, Casual_Customer customer, int numDiners, String confirmationCode) {
        this.waitlistId = waitlistId;
        this.customer = customer;
        this.numDiners = numDiners;
        this.confirmationCode = confirmationCode;
        this.entryTime = LocalDateTime.now(); 
        this.status = "WAITING";
    }

    // בנאי ריק (מומלץ לעבודה עם DB)
    public Waitlist() {
        this.entryTime = LocalDateTime.now();
        this.status = "WAITING";
    }

    // --- Getters and Setters ---
    
    public int getWaitlistId() {
        return waitlistId;
    }

    public void setWaitlistId(int waitlistId) {
        this.waitlistId = waitlistId;
    }

    public Casual_Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Casual_Customer customer) {
        this.customer = customer;
    }

    public int getNumDiners() {
        return numDiners;
    }

    public void setNumDiners(int numDiners) {
        this.numDiners = numDiners;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "WaitlistEntry [ID=" + waitlistId + ", Diners=" + numDiners + ", Status=" + status + "]";
    }
}