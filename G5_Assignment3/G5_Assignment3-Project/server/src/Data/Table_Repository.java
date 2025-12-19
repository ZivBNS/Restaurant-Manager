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

import entities.Restaurant;
import entities.Restaurant_Table;
import entities.TableSize;

/**
 * Repository class for managing Table data and logical capacity availability.
 * Implements the Singleton pattern and utilizes a custom Connection Pool.
 * Logic: Availability is determined by total seat capacity minus occupied seats 
 * to prevent table fragmentation.
 */
public class Table_Repository implements Repository_Interface<Restaurant_Table> {
    
    private DB_Controller db = DB_Controller.getInstance();
    private static Table_Repository TableRepositoryInstance = new Table_Repository();

    private Table_Repository() {}

    public static Table_Repository getInstance() {
        return TableRepositoryInstance;
    }

    /**
     * Initializes the restaurant table configuration and caches it.
     */
    @Override
    public void init() {
        int maxTableSize = 0;
        List<Restaurant_Table> tablesList = new ArrayList<>();
        String sql = "SELECT ID, TableNumber, Size, IsActive FROM Tables";
        Restaurant.getInstance().setTables(tablesList);

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (Statement stmt = conn.createStatement(); 
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int id = rs.getInt("ID");
                    int tableNumber = rs.getInt("TableNumber");
                    TableSize size = TableSize.fromSeats(rs.getInt("Size"));
                    boolean isActive = rs.getBoolean("IsActive");

                    Restaurant_Table table = new Restaurant_Table(id, tableNumber, size, isActive);
                    if (table.getSize() > maxTableSize) {
                        maxTableSize = table.getSize();
                    }
                    tablesList.add(table);
                }
                Restaurant.getInstance().setTables(tablesList);
                Restaurant.setBiggestTableSize(maxTableSize);
            }
        } catch (SQLException e) {
            System.err.println("Init Error: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Checks if the restaurant has enough total seat capacity for a new or updated reservation.
     * Calculation: (Total Seats) - (Occupied seats excluding the current reservation if updating).
     * * @param start The requested start time.
     * @param end The calculated end time.
     * @param guests Requested guest count.
     * @param excludeId The ID of the reservation to ignore (use null for new reservations).
     * @return true if capacity exists, false otherwise.
     */
    public boolean isCapacityAvailable(LocalDateTime start, LocalDateTime end, int guests, Integer excludeId) {
        int totalSeats = 0;
        for (Restaurant_Table t : Restaurant.getInstance().getTables()) {
            if (t.isActive()) {
                totalSeats += t.getSize();
            }
        }

        // Pass the excludeId to the helper method
        int occupiedSeats = getOccupiedSeatCount(start, end, excludeId);

        return (totalSeats - occupiedSeats) >= guests;
    }

    /**
     * Helper method to sum NumberOfDiners for overlapping reservations, 
     * optionally excluding a specific reservation ID to prevent self-collision during updates.
     * * @param start Search window start.
     * @param end Search window end.
     * @param excludeId The ID to exclude from the sum (can be null).
     * @return Total occupied seats in the time window.
     */
    private int getOccupiedSeatCount(LocalDateTime start, LocalDateTime end, Integer excludeId) {
        int occupied = 0;
        
        // Build SQL query dynamically to handle the optional exclusion
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT SUM(NumberOfDiners) FROM reservations ");
        sql.append("WHERE Status != 'Canceled' ");
        sql.append("AND (ReservationStartTime < ? AND ReservationEndTime > ?) ");
        
        if (excludeId != null) {
            sql.append("AND ID != ?");
        }

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql.toString())) {
                pstmt.setTimestamp(1, Timestamp.valueOf(end));
                pstmt.setTimestamp(2, Timestamp.valueOf(start));
                
                if (excludeId != null) {
                    pstmt.setInt(3, excludeId);
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        occupied = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error in getOccupiedSeatCount: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return occupied;
    }

    /**
     * Finds a specific physical table only during arrival.
     */
    public Integer findBestAvailableTable(LocalDateTime start, LocalDateTime end, int guests) {
        String sql = "SELECT ID FROM Tables " 
                   + "WHERE Size >= ? AND IsActive = 1 " 
                   + "AND ID NOT IN ("
                   + "    SELECT TableID FROM reservations " 
                   + "    WHERE TableID IS NOT NULL "
                   + "    AND Status != 'Canceled' " 
                   + "    AND (ReservationStartTime < ? AND ReservationEndTime > ?)"
                   + ") " 
                   + "ORDER BY Size ASC LIMIT 1";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, guests);
                pstmt.setTimestamp(2, Timestamp.valueOf(end));
                pstmt.setTimestamp(3, Timestamp.valueOf(start));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("ID");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }

    @Override
    public boolean set(Restaurant_Table table) {

        String sql =
            "INSERT INTO Tables (TableNumber, Size, IsActive) VALUES (?, ?, ?)";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            PreparedStatement ps =
                pConn.getConnection().prepareStatement(sql);

            ps.setInt(1, table.getTableNumber());
            ps.setInt(2, table.getSize()); // INT (2,4,6,8,10,12)
            ps.setBoolean(3, table.isActive());

            boolean success = ps.executeUpdate() > 0;

            if (success) {
                init();
            }

            return success;

        } catch (SQLException e) {
            System.err.println("Set Table Error: " + e.getMessage());
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    @Override
    public boolean update(Restaurant_Table table) {

        String sql =
            "UPDATE Tables SET TableNumber = ?, Size = ?, IsActive = ? WHERE ID = ?";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            PreparedStatement ps =
                pConn.getConnection().prepareStatement(sql);

            ps.setInt(1, table.getTableNumber());
            ps.setInt(2, table.getSize());
            ps.setBoolean(3, table.isActive());
            ps.setInt(4, table.getId());

            boolean success = ps.executeUpdate() > 0;

            if (success) {
                init();
            }

            return success;

        } catch (SQLException e) {
            System.err.println("Update Table Error: " + e.getMessage());
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    @Override
    public boolean deleteById(int id) {

        String sql = "DELETE FROM tables WHERE ID = ?";

        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            System.err.println("Delete table error: " + e.getMessage());
            return false;

        } finally {
            if (pConn != null)
                db.releaseConnection(pConn);
        }
    }

    @Override
    public Restaurant_Table getById(int id) {

        String sql =
            "SELECT ID, TableNumber, Size, IsActive FROM Tables WHERE ID = ?";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            PreparedStatement ps =
                pConn.getConnection().prepareStatement(sql);

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int tableNumber = rs.getInt("TableNumber");
                    TableSize size =
                        TableSize.fromSeats(rs.getInt("Size"));
                    boolean isActive = rs.getBoolean("IsActive");

                    return new Restaurant_Table(
                        id, tableNumber, size, isActive
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Get Table By ID Error: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }

        return null;
    }
}