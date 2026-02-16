package entities;

/**
 * Defines the various states of a waitlist entry.
 * These states track the customer's journey from joining the waitlist 
 * until they are seated or removed.
 */
public enum WaitlistStatus {
    /** The customer is actively waiting in the queue for an available table. */
    WAITING,
    
    /** The customer with reservation is actively waiting in the queue for an available table. */
    PWAITING,
    
    /** The customer has been notified that a table is now available for them. */
    NOTIFIED,
    
    /** The waitlist process is finished and the customer has been seated. */
    COMPLETED,
    
    /** The waitlist entry was cancelled by the customer or the restaurant. */
    CANCELED;

    /**
     * Returns the status formatted for the Database and UI.
     * Converts the enum name to Title Case (e.g., "WAITING" becomes "Waiting").
     * * @return A capitalized string representation of the status.
     */
    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}