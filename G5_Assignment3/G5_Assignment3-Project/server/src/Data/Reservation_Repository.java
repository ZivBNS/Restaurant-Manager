package Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import entities.Reservation;

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

    public List<Reservation> getByUserId(int userId) { /* Patterned pool fetch... */ 
        List<Reservation> results = new ArrayList<>();
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("SELECT * FROM reservations WHERE UserID = ?")) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) results.add(extractReservationFromResultSet(rs)); }
            }
        } catch (SQLException e) { } finally { if (pConn != null) db.releaseConnection(pConn); }
        return results;
    }

    public List<Reservation> getByContactInfo(String contact) { /* Patterned pool fetch... */ 
        List<Reservation> results = new ArrayList<>();
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("SELECT * FROM reservations WHERE Phone = ? OR Email = ?")) {
                pstmt.setString(1, contact); pstmt.setString(2, contact);
                try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) results.add(extractReservationFromResultSet(rs)); }
            }
        } catch (SQLException e) { } finally { if (pConn != null) db.releaseConnection(pConn); }
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

    public boolean deleteById(int id) {
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("DELETE FROM reservations WHERE ID = ?")) {
                pstmt.setInt(1, id); return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) { return false; } finally { if (pConn != null) db.releaseConnection(pConn); }
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
}