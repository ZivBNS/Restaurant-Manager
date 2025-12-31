package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import entities.Waitlist;

public class Waitlist_Repository implements Repository_Interface<Waitlist> {
    
    private DB_Controller db = DB_Controller.getInstance();
    private static Waitlist_Repository waitlistRepositoryInstance = new Waitlist_Repository();
    private List<Waitlist> activeWaitlist = new ArrayList<>();

    private Waitlist_Repository(){
    }

    public static Waitlist_Repository getInstance() {
        return waitlistRepositoryInstance;
    }

    @Override
    public void init() {
        String sql = "SELECT ID, ReservationID, Status, creationTime, TableFreedTime " +
                     "FROM Waitlist " +
                     "WHERE TableFreedTime IS NULL AND Status = 'WAITING' " +
                     "ORDER BY ID ASC";

        PooledConnection pConn = null;
        try {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    
    @Override
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

    @Override
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

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM Waitlist WHERE ID = " + id;        
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

    @Override
    public Waitlist getById(int id) {
        String sql = "SELECT * FROM Waitlist WHERE ID = " + id;
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    LocalDateTime creation = rs.getTimestamp("creationTime").toLocalDateTime();
                    LocalDateTime freed = (rs.getTimestamp("TableFreedTime") != null) ? 
                                           rs.getTimestamp("TableFreedTime").toLocalDateTime() : null;
                    return new Waitlist(rs.getInt("ID"), rs.getInt("ReservationID"), rs.getString("Status"), creation, freed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }

    public List<Waitlist> getWaitlistToday() {
        return activeWaitlist;
    }

    public void setWaitlistToday(List<Waitlist> waitlistToday) {
        this.activeWaitlist = waitlistToday;
    }

    public Waitlist getByReservationId(int rid) {
        String sql = "SELECT * FROM Waitlist WHERE ReservationID = " + rid;
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    LocalDateTime creation = rs.getTimestamp("creationTime").toLocalDateTime();
                    LocalDateTime freed = (rs.getTimestamp("TableFreedTime") != null) ? 
                                           rs.getTimestamp("TableFreedTime").toLocalDateTime() : null;
                    return new Waitlist(rs.getInt("ID"), rs.getInt("ReservationID"), rs.getString("Status"), creation, freed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }
    

    public boolean cancelWaitlistById(int waitlistId) {
        String query = "UPDATE Waitlist SET Status = 'CANCELED' WHERE ID = ?";
        
        PooledConnection pConn = null;
        PreparedStatement stmt = null;

        try {
            pConn = db.getConnection();
            stmt = pConn.getConnection().prepareStatement(query);
            
            stmt.setInt(1, waitlistId);

            int rowsAffected = stmt.executeUpdate();
            
            // אם מספר השורות שהושפעו גדול מ-0, סימן שהעדכון הצליח
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
    
    public boolean isCustomerAlreadyInWaitlist(String phone, String email) {
        String query = "SELECT w.ID FROM waitlist w " +
                       "JOIN reservations r ON w.ReservationID = r.ID " +
                       "WHERE w.Status = 'WAITING' AND " +
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
    
    //FOR TIMER
    public boolean updateStatusByReservationId(int reservationId, String newStatus) {
        String query = "UPDATE waitlist SET Status = ? WHERE ReservationID = ?";
        
        PooledConnection pConn = null;
        PreparedStatement pstmt = null;

        try {
            pConn = db.getConnection();
            pstmt = pConn.getConnection().prepareStatement(query);
            
            pstmt.setString(1, newStatus);      // הסטטוס החדש (למשל "EXPIRED" או "CANCELED")
            pstmt.setInt(2, reservationId);     // ה-ID של ההזמנה

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

    //FOR TIMER
    public void markAsNotified(int waitlistId) {
        String query = "UPDATE waitlist SET Status = 'NOTIFIED', TableFreedTime = NOW() WHERE ID = ?";
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
    
    public List<Waitlist> getExpiredNotifiedCustomers(int minutes) {
        List<Waitlist> expired = new ArrayList<>();
        String query = "SELECT * FROM waitlist WHERE Status = 'NOTIFIED' AND TableFreedTime < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        
        PooledConnection pConn = null;
        PreparedStatement pstmt = null;

        try {
            pConn = db.getConnection();
            pstmt = pConn.getConnection().prepareStatement(query);
            pstmt.setInt(1, minutes);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime creation = rs.getTimestamp("creationTime").toLocalDateTime();
                    LocalDateTime freed = (rs.getTimestamp("TableFreedTime") != null) ? rs.getTimestamp("TableFreedTime").toLocalDateTime() : null;                
                    Waitlist w = new Waitlist(rs.getInt("ID"), rs.getInt("ReservationID"),rs.getString("status"),creation,freed); 
                    expired.add(w);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
        return expired;
    }
    
    //FOR TIMER / order finish
    public Waitlist findFirstMatch(int tableCapacity) {
        Waitlist candidate = null;
        String query = "SELECT w.* FROM waitlist w " +
                       "JOIN reservations r ON w.ReservationID = r.ID " +
                       "WHERE w.Status = 'WAITING' " +
                       "AND r.NumberOfDiners <= ? " +
                       "ORDER BY w.creationTime ASC " +
                       "LIMIT 1";

        PooledConnection pConn = null;
        PreparedStatement pstmt = null;

        try {
            pConn = db.getConnection();
            pstmt = pConn.getConnection().prepareStatement(query);
            
            pstmt.setInt(1, tableCapacity);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    candidate = new Waitlist(
                        rs.getInt("ID"),
                        rs.getInt("ReservationID"),
                        rs.getString("Status"),
                        rs.getTimestamp("creationTime").toLocalDateTime(),
                        rs.getTimestamp("TableFreedTime") != null ? rs.getTimestamp("TableFreedTime").toLocalDateTime() : null
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding match in waitlist: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }

        return candidate;
    }
}