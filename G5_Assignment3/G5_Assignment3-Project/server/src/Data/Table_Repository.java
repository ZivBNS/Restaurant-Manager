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
 * Updated to support "Soft Delete" (Marking tables as inactive instead of deleting).
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
     * UPDATE: Loads ONLY active tables (IsActive = 1).
     * This ensures "Deleted" (Inactive) tables are hidden from the GUI and 
     * excluded from capacity calculations in memory.
     */
    public void init() {
        int maxTableSize = 0;
        List<Restaurant_Table> tablesList = new ArrayList<>();
        
        // SQL CHANGE: Added WHERE IsActive = 1
        String sql = "SELECT ID, TableNumber, Size, IsActive FROM tables WHERE IsActive = 1"; 

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
                
                System.out.println("[Table_Repository] Loaded " + tablesList.size() + " ACTIVE tables from DB.");
                
                // Update the Singleton Cache
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
     * Performs a "Soft Delete" on a table.
     * UPDATE: Instead of DELETE, we UPDATE the status to inactive (0).
     * * @param tableNumber The logical number of the table to deactivate.
     * @return true if the update was successful.
     */
    public boolean deleteById(int tableNumber) {
        // SQL CHANGE: Update IsActive instead of Delete
        String sql = "UPDATE tables SET IsActive = 0 WHERE TableNumber = ?";
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, tableNumber);
                int affectedRows = pstmt.executeUpdate();
                
                System.out.println("[Table_Repository] Soft Deleting (Deactivating) Table #" + tableNumber);
                
                boolean success = affectedRows > 0;
                
                // Reload cache so the table disappears from memory immediately
                if (success) {
                    init(); 
                }
                
                return success;
            }
        } catch (SQLException e) {
            System.err.println("[Table_Repository] SQL Error during soft delete: " + e.getMessage());
            return false;
        } finally {
            if (pConn != null) {
                db.releaseConnection(pConn);
            }
        }
    }

    /**
     * Advanced capacity check.
     * Since 'init()' now only loads Active tables into 'Restaurant.getInstance().getTables()',
     * this function automatically ignores soft-deleted tables for capacity checks.
     */
    public boolean isCapacityAvailable(LocalDateTime start, LocalDateTime end, int guests, Integer excludeId) {
        
        // 1. Fetch active tables from Cache
        List<Restaurant_Table> activeTables = new ArrayList<>();
        List<Restaurant_Table> cachedTables = Restaurant.getInstance().getTables();
        
        if (cachedTables == null || cachedTables.isEmpty()) {
            System.err.println("[Capacity Check] Failed: No active tables in memory.");
            return false;
        }

        // Deep copy active tables for simulation
        for (Restaurant_Table t : cachedTables) {
            // Double check (though init() filters them)
            if (t.isActive()) {
                activeTables.add(t);
            }
        }

        // 2. Fetch existing reservations (overlaps)
        List<Reservation> simulatedGroups = getOverlappingReservationsList(start, end, excludeId);
        
        // 3. Add current request
        Reservation currentRequest = new Reservation();
        currentRequest.setNumberOfDiners(guests);
        simulatedGroups.add(currentRequest);

        // 4. Run Simulation
        return runSeatingSimulation(simulatedGroups, activeTables);
    }

    /**
     * Finds the best physical table ID for a specific slot.
     * UPDATE: Ensures the SQL explicitly checks IsActive = 1.
     */
    public Integer findBestAvailableTable(LocalDateTime start, LocalDateTime end, int guests) {
        String sql = "SELECT ID FROM tables " 
                   + "WHERE Size >= ? AND IsActive = 1 " // Check 1: Must be active
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

    // --- Standard Helper Methods (No Logic Changes Needed, but kept for completeness) ---

    private boolean runSeatingSimulation(List<Reservation> reservations, List<Restaurant_Table> availableTables) {
        // Sort Groups DESCENDING
        Collections.sort(reservations, new Comparator<Reservation>() {
            @Override
            public int compare(Reservation r1, Reservation r2) {
                return Integer.compare(r2.getNumberOfDiners(), r1.getNumberOfDiners());
            }
        });

        // Sort Tables ASCENDING (Best Fit)
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

    public List<Reservation> getOverlappingReservationsList(LocalDateTime start, LocalDateTime end, Integer excludeId) {
        List<Reservation> conflicts = new ArrayList<Reservation>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM reservations ");
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

                ResultSet rs = pstmt.executeQuery();
                while (rs.next())
                    conflicts.add(extractReservationFromResultSet(rs));                         
            }
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return conflicts;
    }
    /**
     * Simulation Engine: Tries to seat a list of reservations into a list of tables.
     * Strategy: Largest groups get priority (Best Fit).
     * * @param reservations List of people needing seats.
     * @param availableTables List of tables available.
     * @return List of reservations that failed to find a seat.
     */
    private List<Reservation> getUnseatableReservations(List<Reservation> reservations, List<Restaurant_Table> availableTables) {
        List<Reservation> failedReservations = new ArrayList<>();
        
        // 1. Sort Reservations: Priority to larger groups to minimize fragmentation
        List<Reservation> sortedRes = new ArrayList<>(reservations); 
        Collections.sort(sortedRes, new Comparator<Reservation>() {
            @Override
            public int compare(Reservation r1, Reservation r2) {
                return Integer.compare(r2.getNumberOfDiners(), r1.getNumberOfDiners());
            }
        });

        // 2. Sort Tables: "Best Fit" (Smallest table that fits the group)
        List<Restaurant_Table> sortedTables = new ArrayList<>(availableTables);
        Collections.sort(sortedTables, new Comparator<Restaurant_Table>() {
            @Override
            public int compare(Restaurant_Table t1, Restaurant_Table t2) {
                return Integer.compare(t1.getSize(), t2.getSize());
            }
        });

        Set<Integer> occupiedTables = new HashSet<>();

        // 3. Assignment Loop
        for (Reservation group : sortedRes) {
            
            // If reservation has a specific TableID assigned, it MUST take that table (or fail if missing)
            if (group.getTableId() != null && group.getTableId() > 0) {
                boolean foundAssigned = false;
                for (Restaurant_Table t : sortedTables) {
                    if (t.getId() == group.getTableId()) {
                        if (occupiedTables.contains(t.getId())) {
                            // Conflict: Table double-booked (shouldn't happen in valid DB)
                            failedReservations.add(group); 
                        } else {
                            occupiedTables.add(t.getId()); // Seat taken
                        }
                        foundAssigned = true;
                        break;
                    }
                }
                if (!foundAssigned) {
                    failedReservations.add(group); // The assigned table doesn't exist in this list
                }
                continue; // Done with this specific reservation
            }

            // Normal Floating Reservation Logic
            boolean seated = false;
            for (Restaurant_Table table : sortedTables) {
                // Skip if table occupied
                if (occupiedTables.contains(table.getId())) continue;
                
                // Check Fit: Table Size >= Group Size
                if (table.getSize() >= group.getNumberOfDiners()) {
                    occupiedTables.add(table.getId());
                    seated = true;
                    break; // Move to next reservation
                }
            }
            
            if (!seated) {
                failedReservations.add(group);
            }
        }
        
        return failedReservations;
    }
    /**
     * Identifies reservations that will be harmed if a table is modified or deleted.
     * FIX: Compares by TableNumber (since ID might be missing in request) and resolves ID for direct checks.
     */
    public List<Reservation> findImpactedReservations(Restaurant_Table targetTable, boolean isDeleteOperation) {
        List<Reservation> impacted = new ArrayList<>();
        
        // 1. Snapshot of Current Tables (Only Active)
        List<Restaurant_Table> currentTables = new ArrayList<>();
        int realTargetId = -1; // We need to find the real DB ID of the table being deleted

        for (Restaurant_Table t : Restaurant.getInstance().getTables()) {
            if (t.isActive()) {
                currentTables.add(t);
                // Resolve the Real ID based on the Table Number provided
                if (t.getTableNumber() == targetTable.getTableNumber()) {
                    realTargetId = t.getId();
                }
            }
        }

        // If we couldn't find the table in our active list, we can't simulate removing it.
        if (realTargetId == -1 && isDeleteOperation) {
            System.err.println("Table #" + targetTable.getTableNumber() + " not found in active list.");
            return impacted; 
        }

        // 2. Build Future Tables List (The "After" Scenario)
        List<Restaurant_Table> futureTables = new ArrayList<>();
        for (Restaurant_Table t : currentTables) {
            // CRITICAL FIX: Compare by TableNumber, not ID (targetTable usually has ID=0 from controller)
            if (t.getTableNumber() == targetTable.getTableNumber()) { 
                if (isDeleteOperation) {
                    continue; // Skip -> This simulates deletion
                } else {
                    // Update simulation: Use the new properties (size/number) but keep the REAL ID
                    futureTables.add(new Restaurant_Table(t.getId(), targetTable.getTableNumber(), targetTable.getSize(), true));
                }
            } else {
                futureTables.add(t); // Keep other tables as is
            }
        }

        // 3. Fetch ALL future pending/active reservations
        List<Reservation> allFuture = getAllFutureReservations();

        // 4. CHECK 1: Direct Assignment Conflict (The "Hard" Check)
        // If a reservation is explicitly assigned to this table ID in the DB, it's a conflict.
        if (realTargetId != -1) {
            for (Reservation res : allFuture) {
                if (res.getTableId() != null && res.getTableId() == realTargetId) {
                    if (isDeleteOperation) {
                        impacted.add(res); // Direct hit: Table is being deleted
                    } else {
                        // If updating, check if new size is too small
                        if (targetTable.getSize() < res.getNumberOfDiners()) {
                            impacted.add(res); // Table shrank
                        }
                    }
                }
            }
        }

        // 5. CHECK 2: Logical/Floating Capacity Simulation (The "Soft" Check)
        Set<Integer> checkedReservationIds = new HashSet<>(); 
        
        for (Reservation candidate : allFuture) {
            if (checkedReservationIds.contains(candidate.getId())) continue; 
            if (containsId(impacted, candidate.getId())) continue; 

            List<Reservation> timeSlotCompetition = getOverlappingReservationsList(
                candidate.getOrderStartTime(), 
                candidate.getOrderEndTime(), 
                null
            );

            for (Reservation r : timeSlotCompetition) checkedReservationIds.add(r.getId());

            // A. Check: Could we seat them BEFORE?
            List<Reservation> homelessBefore = getUnseatableReservations(timeSlotCompetition, currentTables);

            // B. Check: Can we seat them AFTER?
            List<Reservation> homelessAfter = getUnseatableReservations(timeSlotCompetition, futureTables);

            // C. Compare: Who is NEWLY homeless?
            for (Reservation r : homelessAfter) {
                // If they fit BEFORE, but fail AFTER -> This action caused it.
                if (!containsId(homelessBefore, r.getId())) {
                    if (!containsId(impacted, r.getId())) {
                        impacted.add(r);
                    }
                }
            }
        }
        
        return impacted;
    }


    private List<Reservation> getAllFutureReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE Status IN ('Pending', 'Active') AND ReservationEndTime > NOW()";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while(rs.next()) list.add(extractReservationFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return list;
    }

    public boolean set(Restaurant_Table table) {
        String sql = "INSERT INTO Tables (TableNumber, Size, IsActive) VALUES (?, ?, ?)";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, table.getTableNumber());
            ps.setInt(2, table.getSize());
            ps.setBoolean(3, table.isActive()); // Always true for new ones based on GUI

            boolean success = ps.executeUpdate() > 0;
            if (success) init(); // Refresh cache
            return success;
        } catch (SQLException e) {
            System.err.println("Set Table Error: " + e.getMessage());
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    public boolean update(Restaurant_Table table) {
        String sql = "UPDATE Tables SET TableNumber = ?, Size = ?, IsActive = ? WHERE ID = ?";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, table.getTableNumber());
            ps.setInt(2, table.getSize());
            ps.setBoolean(3, table.isActive());
            ps.setInt(4, table.getId());

            boolean success = ps.executeUpdate() > 0;
            if (success) init(); // Refresh cache
            return success;
        } catch (SQLException e) {
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    // --- Private Helpers ---
    private boolean containsId(List<Reservation> list, int id) {
        for (Reservation r : list) {
            if (r.getId() == id) return true;
        }
        return false;
    }

    private Reservation extractReservationFromResultSet(ResultSet rs) throws SQLException {
        return new Reservation(rs.getInt("ID"), (Integer) rs.getObject("UserID"), (Integer) rs.getObject("TableID"),
                rs.getString("Phone"), rs.getString("Email"), rs.getTimestamp("ReservationStartTime").toLocalDateTime(),
                rs.getTimestamp("ReservationEndTime").toLocalDateTime(),
                rs.getTimestamp("ActualArrivalTime") != null ? rs.getTimestamp("ActualArrivalTime").toLocalDateTime() : null,
                rs.getTimestamp("ActualDepartureTime") != null ? rs.getTimestamp("ActualDepartureTime").toLocalDateTime() : null,
                rs.getInt("NumberOfDiners"), rs.getInt("ConfirmationCode"), rs.getString("Status"),
                rs.getTimestamp("CreationTime").toLocalDateTime(), rs.getBoolean("RemindedPreArrival"),
                rs.getBoolean("RemindedDeparture"));
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