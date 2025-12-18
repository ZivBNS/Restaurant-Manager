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
 * Repository class for managing database operations for the 'reservations' table.
 * Implements the Singleton pattern to ensure a single point of access to reservation data.
 */
public class Reservation_Repository {

    private DB_Controller db = DB_Controller.getInstance();
    private static Reservation_Repository reservationRepositoryInstance = new Reservation_Repository();
    private List<Reservation> activeReservations = new ArrayList<>();
    
    // Server-side counter for unique confirmation codes
    private static int confirmationCodeGenerator = 100000;

    /**
     * Private constructor for Singleton pattern.
     */
    private Reservation_Repository() {}

    /**
     * @return The single instance of Reservation_Repository.
     */
    public static Reservation_Repository getInstance() {
        return reservationRepositoryInstance;
    }

    /**
     * Initializes the repository by setting the next available confirmation code 
     * and loading active reservations for the current day.
     */
    public void init() {
        // 1. Fetch the maximum confirmation code currently in the DB
        int maxCode = 100000;
        String query = "SELECT MAX(ConfirmationCode) FROM reservations";

        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                maxCode = rs.getInt(1);
            }
            // Start generating from the next available number
            confirmationCodeGenerator = (maxCode != 0) ? maxCode + 1 : 100000;
            
            System.out.println("Reservation System Initialized: Next confirmation code will be " + confirmationCodeGenerator);

        } catch (SQLException e) {
            System.err.println("Init Error: Failed to fetch MAX code: " + e.getMessage());
        }

        // 2. Load today's active reservations into memory
        String sql = "SELECT * FROM reservations WHERE DATE(ReservationStartTime) = CURDATE() " 
                   + "AND ActualArrivalTime IS NULL ORDER BY ReservationStartTime ASC";

        try (Statement stmt = db.getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            activeReservations.clear();
            while (rs.next()) {
                activeReservations.add(extractReservationFromResultSet(rs));
            }
            System.out.println("Successfully loaded today's reservations into cache.");
        } catch (SQLException e) {
            System.err.println("Init Error: Failed to load daily reservations: " + e.getMessage());
        }       
    }

    /**
     * Generates a new unique confirmation code in a thread-safe manner.
     * @return A unique integer to be used as a confirmation code.
     */
    public synchronized int getNextConfirmationCode() {
        return confirmationCodeGenerator++;
    }

    /**
     * Inserts a new reservation record into the database.
     * Includes the TableID assigned by the Server logic.
     * * @param res The reservation object to be saved.
     * @return true if the record was inserted successfully.
     */
    public boolean set(Reservation res) {
        // 1. Added TableID to the column list and an extra '?' placeholder
        String sql = "INSERT INTO reservations (UserID, TableID, Phone, Email, ReservationStartTime, "
                   + "ReservationEndTime, NumberOfDiners, ConfirmationCode, Status, CreationTime) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
            // 2. Map UserID (Subscriber ID or null for casual customers)
            if (res.getUserId() != null) pstmt.setInt(1, res.getUserId());
            else pstmt.setNull(1, java.sql.Types.INTEGER);

            // 3. Map TableID (The table assigned by Table_Repository)
            if (res.getTableId() != null) pstmt.setInt(2, res.getTableId());
            else pstmt.setNull(2, java.sql.Types.INTEGER);

            // 4. Map remaining fields
            pstmt.setString(3, res.getPhone());
            pstmt.setString(4, res.getEmail());
            pstmt.setTimestamp(5, Timestamp.valueOf(res.getOrderStartTime()));
            pstmt.setTimestamp(6, Timestamp.valueOf(res.getOrderEndTime()));
            pstmt.setInt(7, res.getNumberOfDiners());
            pstmt.setInt(8, res.getConfirmationCode());
            pstmt.setString(9, res.getStatus());
            pstmt.setTimestamp(10, Timestamp.valueOf(res.getCreationTime()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database Error: Failed to save reservation with TableID: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an existing reservation in the database.
     * @param res The updated reservation object.
     * @return true if the update was successful.
     */
    public boolean update(Reservation res) {
        String sql = "UPDATE reservations SET NumberOfDiners = ?, ReservationStartTime = ?, "
                   + "ReservationEndTime = ?, Status = ? WHERE ID = ?";

        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, res.getNumberOfDiners());
            pstmt.setTimestamp(2, Timestamp.valueOf(res.getOrderStartTime()));
            pstmt.setTimestamp(3, Timestamp.valueOf(res.getOrderEndTime()));
            pstmt.setString(4, res.getStatus());
            pstmt.setInt(5, res.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database Error: Update failed for ID " + res.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a reservation from the database by its ID.
     * @param id The primary key ID of the reservation.
     * @return true if the deletion was successful.
     */
    public boolean deleteById(int id) {
        String sql = "DELETE FROM reservations WHERE ID = ?";

        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Database Error: Delete failed for ID " + id + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all reservations associated with a specific user/subscriber.
     * @param userId The unique subscriber/user ID.
     * @return A list of found Reservation objects.
     */
    public List<Reservation> getByUserId(int userId) {
        List<Reservation> results = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE UserID = ?";
        
        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(extractReservationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error: Failed to fetch user reservations: " + e.getMessage());
        }
        return results;
    }
    /**
     * Fetches reservations for casual customers using phone or email.
     * @param contact The contact string (Phone or Email).
     * @return List of matching reservations.
     */
    public List<Reservation> getByContactInfo(String contact) {
        List<Reservation> results = new ArrayList<>();
        // Search in both Phone and Email columns
        String sql = "SELECT * FROM reservations WHERE Phone = ? OR Email = ?";
        
        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, contact);
            pstmt.setString(2, contact);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(extractReservationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error: Fetch by Contact failed: " + e.getMessage());
        }
        return results;
    }
    /**
     * Fetches all reservations with a 'PENDING' status for the employee management panel.
     * Uses the existing helper method to map database rows to objects.
     * @return A list of all pending reservations in the restaurant.
     */
    public List<Reservation> getAllPendingReservations() {
        List<Reservation> results = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE Status = 'Pending' ORDER BY ReservationStartTime ASC";
        
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                // Using your existing helper method
                results.add(extractReservationFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Database Error: Failed to fetch pending reservations: " + e.getMessage());
        }
        return results;
    }

    /**
     * Advanced update for employees, allowing modification of contact info and assigned tables.
     * @param res The reservation with updated fields.
     * @return true if the update was successful.
     */
    public boolean updateByEmployee(Reservation res) {
        String sql = "UPDATE reservations SET NumberOfDiners = ?, ReservationStartTime = ?, "
                   + "ReservationEndTime = ?, Status = ?, TableID = ?, Phone = ?, Email = ? WHERE ID = ?";

        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
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
        } catch (SQLException e) {
            System.err.println("Database Error: Employee update failed: " + e.getMessage());
            return false;
        }
    }
    /**
     * Private helper method to map a database row to a Reservation object.
     * @param rs The ResultSet currently pointing to a row.
     * @return A mapped Reservation entity.
     */
    private Reservation extractReservationFromResultSet(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("ID"),
            (Integer) rs.getObject("UserID"),
            (Integer) rs.getObject("TableID"),
            rs.getString("Phone"),
            rs.getString("Email"),
            rs.getTimestamp("ReservationStartTime").toLocalDateTime(),
            rs.getTimestamp("ReservationEndTime").toLocalDateTime(),
            rs.getTimestamp("ActualArrivalTime") != null ? rs.getTimestamp("ActualArrivalTime").toLocalDateTime() : null,
            rs.getTimestamp("ActualDepartureTime") != null ? rs.getTimestamp("ActualDepartureTime").toLocalDateTime() : null,
            rs.getInt("NumberOfDiners"),
            rs.getInt("ConfirmationCode"),
            rs.getString("Status"),
            rs.getTimestamp("CreationTime").toLocalDateTime()
        );
    }
    
}