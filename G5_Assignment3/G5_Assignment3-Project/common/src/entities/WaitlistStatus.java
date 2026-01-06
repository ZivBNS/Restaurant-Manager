//* waitlist states are: PWAITING, WAITING, NOTIFIED, COMPLETED, CANCELED
package entities;

public enum WaitlistStatus {
    WAITING,
    PWAITING,
    NOTIFIED,
    COMPLETED,
    CANCELED;

    /**
     * Returns the status formatted for the Database (e.g., "Pending").
     */
    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}