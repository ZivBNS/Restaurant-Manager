package entities;

import java.time.LocalDateTime;

/**
 * Represents an entry in the restaurant's waitlist.
 * This class tracks customers waiting for a table when the restaurant is at full capacity,
 * linking them to a reservation and monitoring timing for table availability.
 */
public class Waitlist {

    /** Unique identifier for the waitlist record in the database */
    private int id=-1;
    /** The ID of the reservation associated with this waitlist entry */
    private int reservationID;
    /** The current status of the entry (e.g., WAITING, NOTIFIED, EXPIRED) */
    private String status;
    /** The timestamp when the customer was added to the waitlist */
    private LocalDateTime creationTime;
    /** The timestamp when a suitable table became free for this entry */
    private LocalDateTime tableFreedTime;

    /**
     * Constructor for retrieving an existing waitlist record from the database.
     * * @param id The waitlist internal ID.
     * @param reservationID The associated reservation ID.
     * @param status The current status string.
     * @param creationTime When the entry was created.
     * @param tableFreedTime When a table became available (nullable).
     */
    public Waitlist(int id, int reservationID, String status, LocalDateTime creationTime, LocalDateTime tableFreedTime) {
        this.id = id;
        this.reservationID = reservationID;
        this.status = status;
        this.creationTime = creationTime;
        this.tableFreedTime = tableFreedTime;
    }

    /**
     * Constructor for creating a new waitlist entry to be inserted into the database.
     * Initializes the status to WAITING and sets the creation time to now.
     * * @param reservationID The ID of the reservation to be placed on the waitlist.
     */
    public Waitlist(int reservationID) {
        this.reservationID = reservationID;
        // Default status for new entries
        this.status = WaitlistStatus.WAITING.toString();
        this.creationTime = LocalDateTime.now();
        this.tableFreedTime = null;
    }
    
    
    // --- Getters and Setters ---

    /** @return The waitlist record ID */
    public int getId() {
        return id;
    }

    /** @param id Set the waitlist record ID */
    public void setId(int id) {
        this.id = id;
    }

    /** @return The associated reservation ID */
    public int getReservation() {
        return reservationID;
    }

    /** @param reservationID Set the associated reservation ID */
    public void setReservation(int reservationID) {
        this.reservationID = reservationID;
    }
    
    /** @return The current status of the waitlist entry */
    public String getStatus() {
        return status;
    }

    /** @param status Set the status of the waitlist entry */
    public void setStatus(String status) {
        this.status = status;
    }

    /** @return The timestamp of entry creation */
    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    /** @param creationTime Set the creation timestamp */
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    /** @return The timestamp when a table was freed for this entry */
    public LocalDateTime getTableFreedTime() {
        return tableFreedTime;
    }

    /** @param tableFreedTime Set the timestamp for when a table is freed */
    public void setTableFreedTime(LocalDateTime tableFreedTime) {
        this.tableFreedTime = tableFreedTime;
    }

    /**
     * Returns a string summary of the waitlist entry.
     */
    @Override
    public String toString() {
        return "Waitlist [ID=" + id + ", OrderID=" + (reservationID != -1 ? reservationID : "N/A") + ", Status=" + status + "]";
    }
}