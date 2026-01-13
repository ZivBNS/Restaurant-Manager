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
 * Supports "Soft Delete" functionality by marking tables as inactive instead of 
 * removing them from the database, and provides a simulation engine for capacity validation.
 */
public class Table_Repository {
    
    private DB_Controller db = DB_Controller.getInstance();
    private static Table_Repository TableRepositoryInstance = new Table_Repository();

    private Table_Repository() {}

    /**
     * Retrieves the singleton instance of the Table_Repository.
     * @return The active Table_Repository instance.
     */
    public static Table_Repository getInstance() {
        return TableRepositoryInstance;
    }

    /**
     * Initializes the restaurant table configuration by loading ONLY active tables (IsActive = 1) from the database.
     * This ensures that deactivated tables are excluded from the GUI and memory-based capacity calculations.
     * Updates the Restaurant singleton cache and tracks the largest table size.
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
     * Performs a "Soft Delete" on a table by updating its status to inactive (0) in the database.
     * After a successful update, it reloads the internal cache to reflect the change immediately.
     * @param tableNumber The logical number of the table to deactivate.
     * @return true if the deactivation was successful, false otherwise.
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
     * Performs an advanced capacity check for a specific time slot and group size.
     * Uses a simulation that considers existing overlapping reservations and only active tables.
     * @param start     Starting time of the requested slot.
     * @param end       Ending time of the requested slot.
     * @param guests    Number of diners in the current request.
     * @param excludeId Optional reservation ID to ignore (used when updating an existing reservation).
     * @return true if there is enough physical capacity to accommodate all groups in that slot.
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
     * Finds the best physical table ID for a specific time slot based on group size.
     * Prioritizes the smallest available active table that fits the group (Best Fit strategy).
     * @param start  Starting time.
     * @param end    Ending time.
     * @param guests Group size.
     * @return The database ID of the best available table, or null if no table is available.
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

    /**
     * Internal simulation logic to determine if a set of reservations can fit into available tables.
     * Sorts groups by size (descending) and tables by capacity (ascending) to optimize seating.
     * @param reservations    List of reservation groups to be seated.
     * @param availableTables List of tables currently in use for the simulation.
     * @return true if all groups can be assigned a seat.
     */
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

    /**
     * Retrieves all reservations that overlap with a given time range and are not cancelled.
     * @param start     The start time of the range.
     * @param end       The end time of the range.
     * @param excludeId An optional ID to exclude (e.g., the reservation being modified).
     * @return A list of overlapping Reservation objects.
     */
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
     * Strategy: Largest groups get priority (Best Fit) to minimize fragmentation.
     * Processes both reservations with specific assigned tables and floating reservations.
     * @param reservations    List of groups needing seats.
     * @param availableTables List of tables available for seating.
     * @return List of reservations that could not be seated in the current scenario.
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
     * Identifies reservations that will be negatively impacted if a table is modified or deleted.
     * Runs a "Before vs After" simulation to determine which groups would become "homeless" 
     * due to the proposed table change.
     * @param targetTable       The table being modified or deleted.
     * @param isDeleteOperation Set to true if simulating a deletion, false for an update.
     * @return A list of Reservations that would no longer fit in the restaurant after the change.
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

    /**
     * Fetches all reservations with status 'Pending' or 'Active' that end after the current time.
     * @return List of future relevant reservations.
     */
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

    /**
     * Inserts a new table record into the database and refreshes the cache.
     * @param table The Restaurant_Table object to add.
     * @return true if the insertion was successful.
     */
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

    /**
     * Updates an existing table's details in the database and refreshes the cache.
     * @param table The Restaurant_Table object containing updated data.
     * @return true if at least one row was updated.
     */
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

    /**
     * Utility method to check if a list of reservations contains a specific ID.
     * @param list The list to search.
     * @param id   The ID to find.
     * @return true if the ID exists in the list.
     */
    private boolean containsId(List<Reservation> list, int id) {
        for (Reservation r : list) {
            if (r.getId() == id) return true;
        }
        return false;
    }

    /**
     * Helper method to map a ResultSet row to a Reservation object.
     * @param rs The SQL result set cursor.
     * @return A populated Reservation object.
     * @throws SQLException If a database error occurs.
     */
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

    /**
     * Retrieves a table record by its unique internal database ID.
     * @param id The table's primary key ID.
     * @return The Restaurant_Table object if found, otherwise null.
     */
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