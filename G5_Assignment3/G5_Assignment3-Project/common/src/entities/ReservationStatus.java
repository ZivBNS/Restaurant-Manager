package entities;

/**
 * Defines the lifecycle states of a reservation.
 * * <ul>
 * <li><b>PENDING:</b> Future reservation, approved but customer hasn't arrived.</li>
 * <li><b>ACTIVE:</b> Customer has checked in and is currently seated.</li>
 * <li><b>COMPLETED:</b> Bill paid, customer has left.</li>
 * <li><b>CANCELED:</b> Reservation was cancelled before arrival.</li>
 * <li><b>NO_SHOW:</b> Customer did not arrive (optional for future use).</li>
 * </ul>
 */
public enum ReservationStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELED,
    NO_SHOW;

    /**
     * Returns the status formatted for the Database (e.g., "Pending").
     */
    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}