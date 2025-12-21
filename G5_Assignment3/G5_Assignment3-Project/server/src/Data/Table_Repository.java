package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import entities.Reservation;
import entities.Restaurant;
import entities.Restaurant_Table;

/**
 * Repository class for managing Table data and simulating seating capacity.
 * Implements "Best-Fit" Bin Packing with robust Debug Logging.
 */
public class Table_Repository implements Repository_Interface<Restaurant_Table> {
    
    private DB_Controller db = DB_Controller.getInstance();
    private static Table_Repository TableRepositoryInstance = new Table_Repository();

    private Table_Repository() {}

    public static Table_Repository getInstance() {
        return TableRepositoryInstance;
    }

    /**
     * Initializes the restaurant table configuration.
     * Includes Debug prints to verify tables are loaded correctly.
     */
    @Override
    public void init() {
        int maxTableSize = 0;
        List<Restaurant_Table> tablesList = new ArrayList<>();
        // Note: Using lowercase 'tables' to match standard MySQL on most systems.
        // If your DB uses 'Tables', change this back.
        String sql = "SELECT ID, TableNumber, Size, IsActive FROM tables"; 

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (Statement stmt = conn.createStatement(); 
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int id = rs.getInt("ID");
                    int tableNumber = rs.getInt("TableNumber");
                    int size = rs.getInt("Size");
                    boolean isActive = rs.getBoolean("IsActive");

                    Restaurant_Table table = new Restaurant_Table(id, tableNumber, size, isActive);
                    if (table.getSize() > maxTableSize) {
                        maxTableSize = table.getSize();
                    }
                    tablesList.add(table);
                }
                
                // --- DEBUG LOGGING ---
                System.out.println("[Table_Repository] Loaded " + tablesList.size() + " tables from DB.");
                if (tablesList.isEmpty()) {
                    System.err.println("[Table_Repository] CRITICAL WARNING: No tables found! Capacity checks will always fail.");
                } else {
                    for(Restaurant_Table t : tablesList) {
                        System.out.println("[Table_Repository] Table #" + t.getTableNumber() + " (Size: " + t.getSize() + ")");
                    }
                }
                // ---------------------

                Restaurant.getInstance().setTables(tablesList);
                Restaurant.setBiggestTableSize(maxTableSize);
            }
        } catch (SQLException e) {
            System.err.println("[Table_Repository] Init Error: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Advanced capacity check with Debug prints to trace failure reasons.
     */
    public boolean isCapacityAvailable(LocalDateTime start, LocalDateTime end, int guests, Integer excludeId) {
        
        // 1. Fetch active tables from Cache
        List<Restaurant_Table> activeTables = new ArrayList<>();
        List<Restaurant_Table> cachedTables = Restaurant.getInstance().getTables();
        
        if (cachedTables == null || cachedTables.isEmpty()) {
            System.err.println("[Capacity Check] Failed: No tables in Restaurant memory.");
            return false;
        }

        for (Restaurant_Table t : cachedTables) {
            if (t.isActive()) {
                activeTables.add(t);
            }
        }

        // 2. Fetch existing reservations
        List<Reservation> simulatedGroups = getOverlappingReservationsList(start, end, excludeId);
        
        // 3. Add current request
        Reservation currentRequest = new Reservation();
        currentRequest.setNumberOfDiners(guests);
        simulatedGroups.add(currentRequest);

        // 4. Sort Groups (Descending)
        Collections.sort(simulatedGroups, new Comparator<Reservation>() {
            @Override
            public int compare(Reservation r1, Reservation r2) {
                return Integer.compare(r2.getNumberOfDiners(), r1.getNumberOfDiners());
            }
        });

        // 5. Sort Tables (Ascending)
        Collections.sort(activeTables, new Comparator<Restaurant_Table>() {
            @Override
            public int compare(Restaurant_Table t1, Restaurant_Table t2) {
                return Integer.compare(t1.getSize(), t2.getSize());
            }
        });

        // 6. Greedy Assignment Simulation
        Set<Integer> occupiedInSimulation = new HashSet<>();

        for (Reservation group : simulatedGroups) {
            boolean assigned = false;
            
            for (Restaurant_Table table : activeTables) {
                // If table fits AND is free in simulation
                if (!occupiedInSimulation.contains(table.getId()) && table.getSize() >= group.getNumberOfDiners()) {
                    occupiedInSimulation.add(table.getId());
                    assigned = true;
                    break; 
                }
            }

            if (!assigned) {
                // System.out.println("[Capacity Check] Failed to seat group of " + group.getNumberOfDiners() + " at " + start);
                return false; // Simulation failed for this group
            }
        }

        return true; // All groups seated successfully
    }

    private List<Reservation> getOverlappingReservationsList(LocalDateTime start, LocalDateTime end, Integer excludeId) {
        List<Reservation> conflicts = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT NumberOfDiners FROM reservations ");
        sql.append("WHERE Status IN ('Pending', 'Active') ");
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
                    while (rs.next()) {
                        Reservation r = new Reservation();
                        r.setNumberOfDiners(rs.getInt("NumberOfDiners"));
                        conflicts.add(r);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error loading overlapping reservations: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return conflicts;
    }

    public Integer findBestAvailableTable(LocalDateTime start, LocalDateTime end, int guests) {
        String sql = "SELECT ID FROM tables " 
                   + "WHERE Size >= ? AND IsActive = 1 " 
                   + "AND ID NOT IN ("
                   + "    SELECT TableID FROM reservations " 
                   + "    WHERE TableID IS NOT NULL "
                   + "    AND Status = 'Active' " 
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
    
    /**
     * Deletes table by table_number (NOT by DB primary key id).
     * Parameter 'id' represents table_number.
     */
    @Override
    public boolean deleteById(int id) {

    	String sql = "DELETE FROM tables WHERE TableNumber = ?";

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
                    int size = rs.getInt("Size");
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