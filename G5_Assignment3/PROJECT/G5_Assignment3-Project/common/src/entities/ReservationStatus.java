package entities;

/**
 * Defines the lifecycle states of a reservation in the restaurant system.
 * This enum is used to track the progress of a booking from creation to completion.
 * * <ul>
 * <li><b>PENDING:</b> Future reservation, approved but customer hasn't arrived.</li>
 * <li><b>ACTIVE:</b> Customer has checked in and is currently seated.</li>
 * <li><b>COMPLETED:</b> Bill paid, customer has left.</li>
 * <li><b>CANCELED:</b> Reservation was cancelled before arrival.</li>
 * <li><b>NO_SHOW:</b> Customer did not arrive (optional for future use).</li>
 * </ul>
 */
public enum ReservationStatus {
    /** The reservation is confirmed and waiting for the customer to arrive. */
    PENDING,
    
    /** The customer has arrived and is currently occupied at a table. */
    ACTIVE,
    
    /** The meal is finished, the bill is settled, and the customer has departed. */
    COMPLETED,
    
    /** The reservation was voided or cancelled by the customer or staff. */
    CANCELED,
    
    /** The customer failed to arrive for their scheduled reservation time. */
    NO_SHOW;

    /**
     * Returns the status formatted for the Database and UI display.
     * Converts the enum name to Title Case (e.g., "PENDING" becomes "Pending").
     * * @return A capitalized string representation of the status.
     */
    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }  
}