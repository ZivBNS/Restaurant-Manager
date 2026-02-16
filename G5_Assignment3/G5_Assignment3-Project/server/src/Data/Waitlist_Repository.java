package Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.Waitlist;
import entities.WaitlistStatus;


/**
 * This class handles all database operations related to the Waitlist.
 * It manages how customers join the line and how they are matched to tables.
 */
public class Waitlist_Repository {
    
    private DB_Controller db = DB_Controller.getInstance();
    private static Waitlist_Repository waitlistRepositoryInstance = new Waitlist_Repository();

    private Waitlist_Repository(){
    }

    /**
     * Gets the single instance of this repository (Singleton).
     * @return The active Waitlist_Repository instance.
     */
    public static Waitlist_Repository getInstance() {
        return waitlistRepositoryInstance;
    }
    
    /**
     * Saves a new waitlist entry to the database.
     * @param objToSet The waitlist object containing reservation and status details.
     * @return true if the record was successfully saved, false otherwise.
     */
    public boolean set(Waitlist objToSet) {
        String creationTimeStr = Timestamp.valueOf(LocalDateTime.now()).toString();
        String sql = "INSERT INTO Waitlist (ReservationID, Status, creationTime, TableFreedTime) VALUES (" +
                     objToSet.getReservation() + ", '" + 
                     objToSet.getStatus() + "', '" + 
                     creationTimeStr + "', NULL)";
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            pConn.getConnection().setAutoCommit(true);
            try (Statement stmt = pConn.getConnection().createStatement()) {
                return stmt.executeUpdate(sql) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Updates an existing waitlist record in the database.
     * @param objToUpdate The waitlist object with updated status or table freed time.
     * @return true if the update was successful.
     */
    public boolean update(Waitlist objToUpdate) {
        String freedTimeStr = (objToUpdate.getTableFreedTime() != null) ? 
                              "'" + Timestamp.valueOf(objToUpdate.getTableFreedTime()).toString() + "'" : "NULL";
        
        String sql = "UPDATE Waitlist SET Status = '" + objToUpdate.getStatus() + "', " +
                     "TableFreedTime = " + freedTimeStr + " " +
                     "WHERE ID = " + objToUpdate.getId();
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            pConn.getConnection().setAutoCommit(true);
            try (Statement stmt = pConn.getConnection().createStatement()) {
                return stmt.executeUpdate(sql) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Deletes a waitlist entry by its unique ID.
     * @param id The ID of the entry to remove.
     * @return true if the deletion was successful.
     */
    public boolean deleteById(int id) {
        String sql = "DELETE FROM Waitlist WHERE ID = " + id;        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement()) {
                return stmt.executeUpdate(sql) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Finds a waitlist entry using its primary key ID.
     * @param id The waitlist ID.
     * @return A Waitlist object if found, or null.
     */
    public Waitlist getById(int id) {
        String sql = "SELECT * FROM Waitlist WHERE ID = " + id;
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return extractWaitlistFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Finds a waitlist entry using the linked Reservation ID.
     * @param rid The ID of the reservation.
     * @return A Waitlist object if found, or null.
     */
    public Waitlist getByReservationId(int rid) {
        String sql = "SELECT * FROM Waitlist WHERE ReservationID = " + rid;
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return extractWaitlistFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }
    
    /**
     * Sets the status of a specific waitlist entry to 'CANCELED'.
     * @param waitlistId The ID of the waitlist entry.
     * @return true if the status was successfully changed.
     */
    public boolean cancelWaitlistById(int waitlistId) {
        String query = "UPDATE Waitlist SET Status = '"+WaitlistStatus.CANCELED.toString() +"' WHERE ID = ?";
        
        PooledConnection pConn = null;
        PreparedStatement stmt = null;

        try {
            pConn = db.getConnection();
            stmt = pConn.getConnection().prepareStatement(query);
            
            stmt.setInt(1, waitlistId);

            int rowsAffected = stmt.executeUpdate();
            
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error canceling waitlist entry: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) { e.printStackTrace(); }
            
            if (pConn != null) {
                db.releaseConnection(pConn);
            }
        }
    }
    
    /**
     * Checks if a customer is already in the waitlist using their contact details.
     * Only looks for entries with a 'WAITING' status.
     * @param phone Customer's phone number.
     * @param email Customer's email address.
     * @return true if the customer is already waiting, false otherwise.
     */
    public boolean isCustomerAlreadyInWaitlist(String phone, String email) {
        String query = "SELECT w.ID FROM waitlist w " +
                       "JOIN reservations r ON w.ReservationID = r.ID " +
                       "WHERE w.Status = '"+WaitlistStatus.WAITING.toString()+"' AND " +
                       "(" +
                           "(r.Phone = ? AND ? IS NOT NULL AND ? <> '') OR " +
                           "(r.Email = ? AND ? IS NOT NULL AND ? <> '')" +
                       ")";
        
        PooledConnection pConn = null;
        PreparedStatement pstmt = null;
        boolean exists = false;

        try {
            pConn = db.getConnection();
            pstmt = pConn.getConnection().prepareStatement(query);
            String cleanPhone = (phone != null) ? phone.trim() : null;
            String cleanEmail = (email != null) ? email.trim() : null;

            if (cleanPhone != null && cleanPhone.isEmpty()) cleanPhone = null;
            if (cleanEmail != null && cleanEmail.isEmpty()) cleanEmail = null;

            pstmt.setString(1, cleanPhone); // r.Phone = ?
            pstmt.setString(2, cleanPhone); // ? IS NOT NULL
            pstmt.setString(3, cleanPhone); // ? <> ''

            pstmt.setString(4, cleanEmail); // r.Email = ?
            pstmt.setString(5, cleanEmail); // ? IS NOT NULL
            pstmt.setString(6, cleanEmail); // ? <> ''

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
        
        return exists;
    }
    
    /**
     * Updates the status of a waitlist entry using the Reservation ID instead of the waitlist ID.
     * @param reservationId The ID of the reservation.
     * @param newStatus The new status string to apply.
     * @return true if at least one row was updated.
     */
    public boolean updateStatusByReservationId(int reservationId, String newStatus) {
        String query = "UPDATE waitlist SET Status = ? WHERE ReservationID = ?";
        
        PooledConnection pConn = null;
        PreparedStatement pstmt = null;

        try {
            pConn = db.getConnection();
            pstmt = pConn.getConnection().prepareStatement(query);
            
            pstmt.setString(1, newStatus);    
            pstmt.setInt(2, reservationId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating waitlist status by res ID: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Sets the status of a waitlist entry to 'NOTIFIED' and records the current time.
     * Used when a table becomes free for the customer.
     * @param waitlistId The ID of the waitlist entry.
     */
    public void markAsNotified(int waitlistId) {
        String query = "UPDATE waitlist SET Status = '" + WaitlistStatus.NOTIFIED.toString() + "', TableFreedTime = NOW() WHERE ID = ?";
        try {
            PooledConnection pConn = db.getConnection();
            PreparedStatement pstmt = pConn.getConnection().prepareStatement(query);
            pstmt.setInt(1, waitlistId);
            pstmt.executeUpdate();
            pstmt.close();
            db.releaseConnection(pConn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Finds the next customer in line whose group size fits the released table.
     * It checks Priority waiting (PWAITING) first, then regular waiting (WAITING).
     * @param tableCapacity The number of seats available at the free table.
     * @return A Waitlist object representing the best match, or null.
     */
    public Waitlist findFirstMatch(int tableCapacity) {
        String queryPriority = "SELECT w.* FROM waitlist w " +
                       "JOIN reservations r ON w.ReservationID = r.ID " +
                       "WHERE w.Status = '" + WaitlistStatus.PWAITING.toString() + "' " +
                       "AND r.NumberOfDiners <= ? " +
                       "ORDER BY w.creationTime ASC " +
                       "LIMIT 1";
        String query = "SELECT w.* FROM waitlist w " +
                "JOIN reservations r ON w.ReservationID = r.ID " +
                "WHERE w.Status = '" + WaitlistStatus.WAITING.toString() + "' " +
                "AND r.NumberOfDiners <= ? " +
                "ORDER BY w.creationTime ASC " +
                "LIMIT 1";
        PooledConnection pConn = null;
        PreparedStatement pstmt = null;

        try {
            pConn = db.getConnection();
            pstmt = pConn.getConnection().prepareStatement(queryPriority);
            pstmt.setInt(1, tableCapacity);
            ResultSet rsPriority = pstmt.executeQuery();
            if (rsPriority.next()) {
                return extractWaitlistFromResultSet(rsPriority);
            }

            pstmt = pConn.getConnection().prepareStatement(query);
            
            pstmt.setInt(1, tableCapacity);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractWaitlistFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding match in waitlist: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }

        return null;
    }
    
    /**
     * Converts a row from the database result set into a Waitlist object.
     * @param rs The ResultSet containing waitlist data.
     * @return A populated Waitlist object.
     * @throws SQLException If data cannot be retrieved from the ResultSet.
     */
    private Waitlist extractWaitlistFromResultSet(ResultSet rs) throws SQLException {
        return new Waitlist(rs.getInt("ID"), rs.getInt("ReservationID"),
                rs.getString("Status"), rs.getTimestamp("creationTime").toLocalDateTime(),
                rs.getTimestamp("TableFreedTime") != null ? rs.getTimestamp("TableFreedTime").toLocalDateTime() : null
            );

    }

    /**
     * Retrieves a list of waitlist entries that were notified but did not show up in time.
     * @param minutes The grace period allowed before the entry is considered expired.
     * @return A list of Waitlist objects that have exceeded the time limit.
     */
    public List<Waitlist> getExpiredNotifiedWaitlists(int minutes) {
        List<Waitlist> expiredList = new ArrayList<>();
                String query = "SELECT * FROM waitlist WHERE Status = ? AND TableFreedTime < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(query)) {                
                pstmt.setString(1, WaitlistStatus.NOTIFIED.toString());
                pstmt.setInt(2, minutes);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        expiredList.add(extractWaitlistFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null)
                db.releaseConnection(pConn);
        }
        return expiredList;
    }

    /**
     * Retrieves all active waitlist entries for management and display.
     * Includes information from both the Waitlist and Reservations tables.
     * @return A list of Maps, each representing an active entry with details like confirmation code and status.
     */
    public List<Map<String, Object>> getAllActiveWaitlists() {
        List<Map<String, Object>> activeWaitlists = new ArrayList<>();
        
        String sql = "SELECT r.ConfirmationCode, r.NumberOfDiners, w.creationTime, w.Status, w.ID " +
                     "FROM waitlist w " +
                     "JOIN reservations r ON w.ReservationID = r.ID " +
                     "WHERE w.Status IN ('WAITING', 'PWAITING', 'NOTIFIED') " +
                     "ORDER BY w.creationTime ASC";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("confCode", rs.getInt("ConfirmationCode"));
                    row.put("guests", rs.getInt("NumberOfDiners"));
                    row.put("created", rs.getTimestamp("creationTime").toString());
                    row.put("status", rs.getString("Status"));
                    row.put("waitlistId", rs.getInt("ID"));
                    
                    activeWaitlists.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Waitlist Repository] Error fetching active waitlists: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pConn != null) {
                db.releaseConnection(pConn);
            }
        }
        return activeWaitlists;
    }
}