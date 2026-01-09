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
public class Table_Repository {
    
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
    /**
     * CORE LOGIC: Seating Simulation (Bin Packing).
     * Decoupled from DB/Singleton so it can be used for "What-If" analysis.
     * * @param reservations List of reservations to seat.
     * @param availableTables List of tables available for this simulation.
     * @return true if everyone fits.
     */
    private boolean runSeatingSimulation(List<Reservation> reservations, List<Restaurant_Table> availableTables) {
        
        // Sort Groups DESCENDING (Biggest groups first)
        Collections.sort(reservations, new Comparator<Reservation>() {
            @Override
            public int compare(Reservation r1, Reservation r2) {
                return Integer.compare(r2.getNumberOfDiners(), r1.getNumberOfDiners());
            }
        });

        // Sort Tables ASCENDING (Smallest tables first - Best Fit)
        Collections.sort(availableTables, new Comparator<Restaurant_Table>() {
            @Override
            public int compare(Restaurant_Table t1, Restaurant_Table t2) {
                return Integer.compare(t1.getSize(), t2.getSize());
            }
        });

        Set<Integer> occupiedInSimulation = new HashSet<>();

        for (Reservation group : reservations) {
            boolean assigned = false;
            for (Restaurant_Table table : availableTables) {
                if (!occupiedInSimulation.contains(table.getId()) && table.getSize() >= group.getNumberOfDiners()) {
                    occupiedInSimulation.add(table.getId());
                    assigned = true;
                    break; 
                }
            }
            if (!assigned) return false;
        }
        return true;
    }

   
    
    /**
     * Helper to avoid duplicate objects in the list based on ID.
     */
    private boolean containsId(List<Reservation> list, int id) {
        for (Reservation r : list) {
            if (r.getId() == id) return true;
        }
        return false;
    }

    /**
     * DB Helper: Fetch all Pending/Active reservations that are currently active or in the future.
     * Logic Fix: Uses ReservationEndTime > NOW() to include ongoing meals started before midnight.
     */
    private List<Reservation> getAllFutureReservations() {
        List<Reservation> list = new ArrayList<>();
        // Fix: Look for anything that hasn't ENDED yet.
        String sql = "SELECT * FROM reservations WHERE Status IN ('Pending', 'Active') AND ReservationEndTime > NOW()";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while(rs.next()) {
                    Reservation r = new Reservation();
                    r.setId(rs.getInt("ID"));
                    r.setOrderStartTime(rs.getTimestamp("ReservationStartTime").toLocalDateTime());
                    r.setOrderEndTime(rs.getTimestamp("ReservationEndTime").toLocalDateTime());
                    r.setNumberOfDiners(rs.getInt("NumberOfDiners"));
                    r.setCreationTime(rs.getTimestamp("CreationTime").toLocalDateTime());
                    r.setConfirmationCode(rs.getInt("ConfirmationCode")); 
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return list;
    }

    /**
     * Analyzes if a specific table can be safely deleted or updated.
     * Updated Logic: 
     * - If deleting: Simulate world without the table.
     * - If updating size UP: Safe (skip check or simulate with larger size).
     * - If updating size DOWN: Simulate world with smaller table.
     * * @param targetTable The table object being modified (contains new size/status).
     * @param isDeleteOperation True if we are deleting, false if updating.
     * @return List of conflicts.
     */
    public List<Reservation> findImpactedReservations(Restaurant_Table targetTable, boolean isDeleteOperation) {
        List<Reservation> problematicReservations = new ArrayList<>();
        
        // 1. Prepare Hypothetical Table List
        List<Restaurant_Table> hypotheticalTables = new ArrayList<>();
        
        // Get the current real state
        for (Restaurant_Table t : Restaurant.getInstance().getTables()) {
            if (!t.isActive()) continue; // Skip already inactive tables

            if (t.getTableNumber() == targetTable.getTableNumber()) {
                // This is the table we are modifying
                if (isDeleteOperation) {
                    // Scenario A: Deletion -> Do not add to list (Remove it)
                    continue; 
                } else {
                    // Scenario B: Update
                    if (!targetTable.isActive()) {
                        // Making inactive -> Remove it
                        continue;
                    }
                    // Scenario C: Resizing
                    // We add the table but with the NEW properties (New Size)
                    Restaurant_Table modifiedTable = new Restaurant_Table(
                        t.getId(), 
                        targetTable.getTableNumber(), 
                        targetTable.getSize(), // Use NEW size
                        true
                    );
                    hypotheticalTables.add(modifiedTable);
                }
            } else {
                // Keep other tables as they are
                hypotheticalTables.add(t);
            }
        }

        // Optimization: If it's an update and size increased, strictly speaking, we don't need to check 
        // unless there's a risk of changing Table Numbers causing simulated ID clashes. 
        // But running the simulation is safer to catch edge cases.

        // 2. Get All relevant reservations (Future + Ongoing)
        List<Reservation> allFuture = getAllFutureReservations();
        
        // 3. Run Simulation Loop (Same as before)
        for (Reservation res : allFuture) {
            if (problematicReservations.contains(res)) continue;

            List<Reservation> timeSlotPeers = getOverlappingReservationsList(
                res.getOrderStartTime(), 
                res.getOrderEndTime(), 
                null 
            );

            // Important: Ensure timeSlotPeers have full data (Start Time) for reporting!
            // The getOverlapping helper we fixed earlier does this.

            if (!runSeatingSimulation(new ArrayList<>(timeSlotPeers), new ArrayList<>(hypotheticalTables))) {
                
                // Sort by Creation Time (Newest First)
                Collections.sort(timeSlotPeers, new Comparator<Reservation>() {
                    @Override
                    public int compare(Reservation r1, Reservation r2) {
                        return r2.getCreationTime().compareTo(r1.getCreationTime());
                    }
                });

                List<Reservation> currentSlotMock = new ArrayList<>(timeSlotPeers);
                
                while (!runSeatingSimulation(currentSlotMock, new ArrayList<>(hypotheticalTables)) && !currentSlotMock.isEmpty()) {
                    Reservation removed = currentSlotMock.remove(0); 
                    if (!containsId(problematicReservations, removed.getId())) {
                        problematicReservations.add(removed);
                    }
                }
            }
        }
        
        return problematicReservations;
    }
    /**
     * Retrieves a list of reservations that overlap with a specific time window.
     * Fix: Included 'CreationTime' in the SELECT statement to prevent 
     * NullPointerException during the conflict sorting logic.
     * * @param start The window start time.
     * @param end The window end time.
     * @param excludeId ID to ignore (used for updates).
     * @return A list of populated Reservation objects.
     */
    public List<Reservation> getOverlappingReservationsList(LocalDateTime start, LocalDateTime end, Integer excludeId) {
    	return Reservation_Repository.getInstance().getOverlappingReservationsList(start, end, excludeId);
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
     * Deletes a table from the database using its TableNumber.
     * Logic: Executes the SQL DELETE command and returns true if at least one row was affected.
     * * @param tableNumber The logical number of the table to remove.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteById(int tableNumber) {
        String sql = "DELETE FROM tables WHERE TableNumber = ?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, tableNumber);
                int affectedRows = pstmt.executeUpdate();
                
                System.out.println("[Table_Repository] Attempting to delete Table #" + tableNumber);
                System.out.println("[Table_Repository] Database affected rows: " + affectedRows);
                
                boolean success = affectedRows > 0;
                
                if (success) {
                    init(); // Reload tables from DB to Restaurant instance
                }
                
                return success;
            }
        } catch (SQLException e) {
            System.err.println("[Table_Repository] SQL Error during deletion: " + e.getMessage());
            return false;
        } finally {
            if (pConn != null) {
                db.releaseConnection(pConn);
            }
        }
    }

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