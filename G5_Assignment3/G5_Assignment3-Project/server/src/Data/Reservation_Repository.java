package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import entities.Reservation;
import entities.ReservationStatus;

/**
 * Repository for reservation data.
 * Updated to support Logical Seating simulation by storing TableID as NULL.
 */
public class Reservation_Repository {

    private DB_Controller db = DB_Controller.getInstance();
    private static Reservation_Repository reservationRepositoryInstance = new Reservation_Repository();
    private static int confirmationCodeGenerator = 100000;

    private Reservation_Repository() {}

    public static Reservation_Repository getInstance() { return reservationRepositoryInstance; }

    /**
     * Fetches a reservation by its unique confirmation code.
     */
    public Reservation getByConfirmationCode(int code) {
        String sql = "SELECT * FROM reservations WHERE ConfirmationCode = ?";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, code);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return extractReservationFromResultSet(rs);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { if (pConn != null) db.releaseConnection(pConn); }
        return null;
    }

    public void init() { /* Same logic using Pool and MAX(ConfirmationCode) */ 
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            String query = "SELECT MAX(ConfirmationCode) FROM reservations";
            try (Statement stmt = pConn.getConnection().createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) confirmationCodeGenerator = Math.max(100000, rs.getInt(1) + 1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { if (pConn != null) db.releaseConnection(pConn); }
    }

    public synchronized int getNextConfirmationCode() { return confirmationCodeGenerator++; }

    /**
     * Saves a new reservation. TableID is explicitly set to NULL on creation.
     */
    public boolean set(Reservation res) {
        String sql = "INSERT INTO reservations (UserID, TableID, Phone, Email, ReservationStartTime, "
                   + "ReservationEndTime, NumberOfDiners, ConfirmationCode, Status, CreationTime) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                if (res.getUserId() != null) pstmt.setInt(1, res.getUserId());
                else pstmt.setNull(1, java.sql.Types.INTEGER);

                // IMPORTANT: Always NULL to prevent fragmentation until arrival
                pstmt.setNull(2, java.sql.Types.INTEGER);

                pstmt.setString(3, res.getPhone());
                pstmt.setString(4, res.getEmail());
                pstmt.setTimestamp(5, Timestamp.valueOf(res.getOrderStartTime()));
                pstmt.setTimestamp(6, Timestamp.valueOf(res.getOrderEndTime()));
                pstmt.setInt(7, res.getNumberOfDiners());
                pstmt.setInt(8, res.getConfirmationCode());
                pstmt.setString(9, res.getStatus());
                pstmt.setTimestamp(10, Timestamp.valueOf(res.getCreationTime()));

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) { return false; }
        finally { if (pConn != null) db.releaseConnection(pConn); }
    }

    public boolean update(Reservation res) {
        String sql = "UPDATE reservations SET NumberOfDiners = ?, ReservationStartTime = ?, "
                   + "ReservationEndTime = ?, Status = ? WHERE ID = ?";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, res.getNumberOfDiners());
                pstmt.setTimestamp(2, Timestamp.valueOf(res.getOrderStartTime()));
                pstmt.setTimestamp(3, Timestamp.valueOf(res.getOrderEndTime()));
                pstmt.setString(4, res.getStatus());
                pstmt.setInt(5, res.getId());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) { return false; }
        finally { if (pConn != null) db.releaseConnection(pConn); }
    }

    public boolean updateByEmployee(Reservation res) {
        String sql = "UPDATE reservations SET NumberOfDiners = ?, ReservationStartTime = ?, "
                   + "ReservationEndTime = ?, Status = ?, TableID = ?, Phone = ?, Email = ? WHERE ID = ?";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, res.getNumberOfDiners());
                pstmt.setTimestamp(2, Timestamp.valueOf(res.getOrderStartTime()));
                pstmt.setTimestamp(3, Timestamp.valueOf(res.getOrderEndTime()));
                pstmt.setString(4, res.getStatus());
                if (res.getTableId() != null) pstmt.setInt(5, res.getTableId());
                else pstmt.setNull(5, java.sql.Types.INTEGER);
                pstmt.setString(6, res.getPhone());
                pstmt.setString(7, res.getEmail());
                pstmt.setInt(8, res.getId());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) { return false; }
        finally { if (pConn != null) db.releaseConnection(pConn); }
    }

    /**
     * Retrieves all reservations associated with a specific subscriber ID
     * that are currently 'Pending' or 'Active'.
     * * @param userId The unique subscriber code.
     * @return A list of filtered reservations.
     */
    public List<Reservation> getByUserId(int userId) {
        List<Reservation> results = new ArrayList<Reservation>();
        // Updated SQL to filter only relevant statuses for the customer view
        String sql = "SELECT * FROM reservations WHERE UserID = ? AND Status IN ('Pending', 'Active')";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(extractReservationFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return results;
    }

    /**
     * Retrieves all reservations for a casual customer by phone or email
     * that are currently 'Pending' or 'Active'.
     * * @param contact The phone number or email string.
     * @return A list of filtered reservations.
     */
    public List<Reservation> getByContactInfo(String contact) {
        List<Reservation> results = new ArrayList<Reservation>();
        // Updated SQL to filter only relevant statuses for the customer view
        String sql = "SELECT * FROM reservations WHERE (Phone = ? OR Email = ?) AND Status IN ('Pending', 'Active')";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, contact);
                pstmt.setString(2, contact);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(extractReservationFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return results;
    }

    public List<Reservation> getAllPendingReservations() { /* Patterned pool fetch... */ 
        List<Reservation> results = new ArrayList<>();
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM reservations WHERE Status = 'Pending'")) {
                while (rs.next()) results.add(extractReservationFromResultSet(rs));
            }
        } catch (SQLException e) { } finally { if (pConn != null) db.releaseConnection(pConn); }
        return results;
    }

    /**
     * Hard deletes a reservation from the database.
     * RESERVED FOR ADMIN USE ONLY.
     * * @param id The reservation ID to delete.
     * @return true if deleted.
     */
    public boolean deleteById(int id) {
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("DELETE FROM reservations WHERE ID = ?")) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }


    private Reservation extractReservationFromResultSet(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("ID"), (Integer) rs.getObject("UserID"), (Integer) rs.getObject("TableID"),
            rs.getString("Phone"), rs.getString("Email"),
            rs.getTimestamp("ReservationStartTime").toLocalDateTime(), rs.getTimestamp("ReservationEndTime").toLocalDateTime(),
            rs.getTimestamp("ActualArrivalTime") != null ? rs.getTimestamp("ActualArrivalTime").toLocalDateTime() : null,
            rs.getTimestamp("ActualDepartureTime") != null ? rs.getTimestamp("ActualDepartureTime").toLocalDateTime() : null,
            rs.getInt("NumberOfDiners"), rs.getInt("ConfirmationCode"), rs.getString("Status"),
            rs.getTimestamp("CreationTime").toLocalDateTime()
        );
    }
    /**
     * Updates only the status of a specific reservation.
     * Used for cancellations, check-ins, and completions.
     * * @param reservationId The ID of the reservation.
     * @param newStatus The new status enum value.
     * @return true if the update was successful.
     */
    public boolean updateStatusByID(int reservationId, ReservationStatus newStatus) {
        String sql = "UPDATE reservations SET Status = ? WHERE ID = ?";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, newStatus.toString()); // Converts enum to "Pending"/"Canceled" etc.
                pstmt.setInt(2, reservationId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    public boolean updateStatusByConfirmationCode(int confCode, ReservationStatus newStatus) {
    	String sql = "UPDATE Reservations SET Status = '" + newStatus.toString() + "' WHERE ConfirmationCode = " + confCode;
    	PooledConnection pConn = null;
        try {
        	pConn = db.getConnection();
            pConn.getConnection().setAutoCommit(true);
            Statement stmt = pConn.getConnection().createStatement();
                int x=stmt.executeUpdate(sql);                
                if (x==0) return false;
                return true;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    
    public String getStatusByConfirmationCode(int confCode) {
        String sql = "SELECT Status FROM Reservations WHERE ConfirmationCode = " + confCode;
        PooledConnection pConn = null;        
        try {
            pConn = db.getConnection();
            Statement stmt = pConn.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                String status = rs.getString("Status");
                System.out.println("Found status: " + status + " for code: " + confCode);
                return status;
            } else return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    
    public Reservation getLatestReservationByPhone(String phone) {

    	String sql =
    		    "SELECT ID, Phone, ReservationStartTime, NumberOfDiners, TableID, Status " +
    		    "FROM reservations " +
    		    "WHERE Phone = ? " +
    		    "AND status = 'ACTIVE'"+
    		    "ORDER BY ReservationStartTime ASC " +
    		    "LIMIT 1";

        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, phone);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
            	return new Reservation(
            		    rs.getInt("ID"),
            		    rs.getString("Phone"),
            		    rs.getTimestamp("ReservationStartTime").toLocalDateTime(),
            		    rs.getInt("NumberOfDiners"),
            		    (Integer) rs.getObject("TableID"),
            		    rs.getString("Status")
            		);
            }

        } catch (Exception e) {
            System.out.println("getLatestReservationByPhone ERROR: " + e.getMessage());
        } finally {
            if (pConn != null)
                db.releaseConnection(pConn);
        }

        return null;
    }
    
    public void markReservationAsCompleted(int reservationId) {

        String sql = "UPDATE reservations SET Status = 'COMPLETED' WHERE ID = ?";

        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);
            ps.executeUpdate();

        } catch (Exception e) {
            System.out.println("markReservationAsCompleted ERROR: " + e.getMessage());
        } finally {
            if (pConn != null)
                db.releaseConnection(pConn);
        }
    }

public Reservation getById(int id) {
    String sql = "SELECT * FROM reservations WHERE ID = ?";
    PooledConnection pConn = null;
    
    try {
        pConn = db.getConnection();
        java.sql.Connection conn = pConn.getConnection();
        java.sql.PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        
        java.sql.ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
          
            return new Reservation(
                rs.getInt("ID"),
                (Integer) rs.getObject("UserID"), 
                (Integer) rs.getObject("TableID"),
                rs.getString("Phone"),
                rs.getString("Email"),
                rs.getTimestamp("ReservationStartTime").toLocalDateTime(),
                rs.getTimestamp("ReservationEndTime") != null ? rs.getTimestamp("ReservationEndTime").toLocalDateTime() : null,
                rs.getTimestamp("ActualArrivalTime") != null ? rs.getTimestamp("ActualArrivalTime").toLocalDateTime() : null, 
                rs.getTimestamp("ActualDepartureTime") != null ? rs.getTimestamp("ActualDepartureTime").toLocalDateTime() : null,
                rs.getInt("NumberOfDiners"),
                rs.getInt("ConfirmationCode"),
                rs.getString("Status"),
                rs.getTimestamp("creationTime").toLocalDateTime()
            );
        }
    } catch (Exception e) {
        System.out.println("Error in getById: " + e.getMessage());
    } finally {
        if (pConn != null) db.releaseConnection(pConn);
    }
    return null; // לא נמצא
}
}