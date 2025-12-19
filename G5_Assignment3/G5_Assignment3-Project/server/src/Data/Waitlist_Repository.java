package Data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import entities.Waitlist;

/**
 * Repository class for managing the restaurant's waitlist.
 * Implements the Singleton pattern and uses a custom Connection Pool 
 * to handle waitlist data for customers who couldn't find an immediate table.
 */
public class Waitlist_Repository implements Repository_Interface<Waitlist> {
    
    /** The database controller managing the connection pool. */
    private DB_Controller db = DB_Controller.getInstance();
    
    /** Singleton instance of the repository. */
    private static Waitlist_Repository waitlistRepositoryInstance = new Waitlist_Repository();
    
    /** Local cache of active waitlist entries. */
    private List<Waitlist> activeWaitlist = new ArrayList<>();

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private Waitlist_Repository(){
    }

    /**
     * Retrieves the singleton instance of the Waitlist_Repository.
     * @return The active Waitlist_Repository instance.
     */
    public static Waitlist_Repository getInstance() {
        return waitlistRepositoryInstance;
    }

    /**
     * Initializes the repository by loading all active 'WAITING' entries from the database.
     * Uses the connection pool to safely borrow and release connections.
     */
    @Override
    public void init() {
        String sql = "SELECT ID, ReservationID, Status, creationTime, TableFreedTime " +
                     "FROM Waitlist " +
                     "WHERE TableFreedTime IS NULL AND Status = 'WAITING' " +
                     "ORDER BY ID ASC";

        PooledConnection pConn = null;
        try {
            // Borrow a connection from the pool
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                activeWaitlist.clear();
                while (rs.next()) {
                    int id = rs.getInt("ID");
                    int resId = rs.getInt("ReservationID");
                    String status = rs.getString("Status");
                    
                    LocalDateTime creationTime = rs.getTimestamp("creationTime").toLocalDateTime();
                    
                    LocalDateTime freedTime = null;
                    Timestamp freedTimestamp = rs.getTimestamp("TableFreedTime");
                    if (freedTimestamp != null) {
                        freedTime = freedTimestamp.toLocalDateTime();
                    }

                    Waitlist waitEntry = new Waitlist(id, resId, status, creationTime, freedTime);
                    activeWaitlist.add(waitEntry);
                }
                System.out.println("Waitlist_Repository: Successfully loaded " + activeWaitlist.size() + " active entries.");
            }
            
        } catch (SQLException e) {
            System.err.println("Database Error: Failed to fetch active waitlist: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always return the connection to the pool
            if (pConn != null) {
                db.releaseConnection(pConn);
            }
        }
    }
    
    /**
     * Persists a new waitlist entry to the database.
     * @param objToSet The Waitlist object to save.
     * @return true if the entry was saved successfully.
     */
    @Override
    public boolean set(Waitlist objToSet) {
        // Implementation for adding a customer to the waitlist goes here
        return false;
    }

    /**
     * Updates an existing waitlist entry (e.g., when a table becomes available).
     * @param objToUpdate The Waitlist entry with updated status or timestamps.
     * @return true if the update was successful.
     */
    @Override
    public boolean update(Waitlist objToUpdate) {
        // Implementation for updating waitlist status goes here
        return false;
    }

    /**
     * Removes an entry from the waitlist by its ID.
     * @param id The unique ID of the waitlist entry.
     * @return true if deleted successfully.
     */
    @Override
    public boolean deleteById(int id) {
        // Implementation for removing from waitlist goes here
        return false;
    }

    /**
     * Retrieves a specific waitlist entry by its unique database ID.
     * @param id The unique ID of the record.
     * @return The Waitlist object if found, null otherwise.
     */
    @Override
    public Waitlist getById(int id) {
        return null;
    }

    /**
     * Gets the current local cache of the waitlist.
     * @return A list of active waitlist entries.
     */
    public List<Waitlist> getWaitlistToday() {
        return activeWaitlist;
    }

    /**
     * Manually sets the local cache for today's waitlist.
     * @param waitlistToday The list to replace the current cache.
     */
    public void setWaitlistToday(List<Waitlist> waitlistToday) {
        this.activeWaitlist = waitlistToday;
    }
}