package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.sql.Date;
import java.sql.Time;

import entities.Opening_Hours;
import entities.Restaurant;

/**
 * Repository class responsible for loading and managing restaurant operating hours.
 * It handles standard weekly schedules and specific date exceptions (holidays/events).
 * This class follows the Singleton pattern and utilizes a custom Connection Pool 
 * for optimized database access.
 */
public class OpeningHours_Repository implements Repository_Interface<Opening_Hours> {
    
    /** The database controller managing the connection pool. */
    private DB_Controller db = DB_Controller.getInstance();
    
    /** Singleton instance of the repository. */
    private static OpeningHours_Repository OpeningHoursInstance = new OpeningHours_Repository();

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private OpeningHours_Repository() {
    }

    /**
     * Provides access to the single instance of the OpeningHours_Repository.
     * @return The singleton instance of the repository.
     */
    public static OpeningHours_Repository getInstance() {
        return OpeningHoursInstance;
    }
    /**
     * Converts Java DayOfWeek (MONDAY) to DB format (Monday).
     */
    private String formatDayForDB(DayOfWeek day) {
        String name = day.name().toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    /**
     * Initializes the restaurant's operating hours by fetching active data from the database.
     * It populates both the regular weekly schedule (where IsActive = 1) and special date exceptions.
     * Data is stored in the Restaurant singleton for global access.
     */
    @Override
    public void init() {
        Opening_Hours oh = new Opening_Hours();
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            // Load all rows including the IsActive status
            String sqlRegular = "SELECT DayOfWeek, OpenTime, CloseTime, IsActive FROM openinghours";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlRegular)) {
                
                while (rs.next()) {
                    DayOfWeek day = DayOfWeek.valueOf(rs.getString("DayOfWeek").toUpperCase());
                    LocalTime open = rs.getTime("OpenTime").toLocalTime();
                    LocalTime close = rs.getTime("CloseTime").toLocalTime();
                    boolean active = rs.getBoolean("IsActive");
                    
                    // Populate the entity with the status from the DB
                    oh.setRegularHour(day, open, close, active);
                }
            }

            // Load special date exceptions
            String sqlSpecial = "SELECT Date, OpenTime, CloseTime, Description FROM specialhours";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSpecial)) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("Date").toLocalDate();
                    String desc = rs.getString("Description");
                    if (rs.getTime("OpenTime") != null) {
                        oh.setException(date, rs.getTime("OpenTime").toLocalTime(), 
                                        rs.getTime("CloseTime").toLocalTime(), desc);
                    } else {
                        oh.setException(date, null, null, desc); 
                    }
                }
            }

            Restaurant.getInstance().setOpeningHours(oh);
            System.out.println("OpeningHours_Repository: Restaurant Instance Sync Complete.");
            
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    /**
     * Performs a batch update for all 7 days of the week.
     * @param batchData Map containing DayOfWeek and an array [LocalTime open, LocalTime close, Boolean active].
     * @return true if the batch was committed successfully.
     */
    public boolean updateAllDays(Map<DayOfWeek, Object[]> batchData) {
        String sql = "UPDATE openinghours SET OpenTime = ?, CloseTime = ?, IsActive = ? WHERE DayOfWeek = ?";
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<DayOfWeek, Object[]> entry : batchData.entrySet()) {
                    ps.setTime(1, Time.valueOf((LocalTime) entry.getValue()[0]));
                    ps.setTime(2, Time.valueOf((LocalTime) entry.getValue()[1]));
                    ps.setInt(3, (Boolean) entry.getValue()[2] ? 1 : 0);
                    ps.setString(4, formatDayForDB(entry.getKey()));
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit(); // Commit all changes at once
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    /**
     * Adds a new special hour exception to the database.
     * @param date The date for the exception.
     * @param open The opening time (null if closed).
     * @param close The closing time.
     * @param description A reason for the special hours.
     * @return true if the record was inserted successfully, false otherwise.
     */
    public boolean addSpecialHour(LocalDate date, LocalTime open, LocalTime close, String description) {
        String sql = "INSERT INTO specialhours (Date, OpenTime, CloseTime, Description) VALUES (?, ?, ?, ?)";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(date));
                ps.setTime(2, open != null ? Time.valueOf(open) : null);
                ps.setTime(3, close != null ? Time.valueOf(close) : null);
                ps.setString(4, description);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    public boolean deleteSpecialHour(LocalDate date) {
        String sql = "DELETE FROM specialhours WHERE Date = ?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(date));
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    /**
     * Persists the current state of Opening_Hours into the database.
     * Note: This implementation is usually handled via specific add/update methods 
     * due to the composite nature of the hours table.
     * @param objToSet The entity to save.
     * @return false as individual updates are preferred for this entity.
     */
    @Override
    public boolean set(Opening_Hours objToSet) {
        return false;
    }
    /**
     * Unified Update Method: Updates OpenTime, CloseTime AND IsActive status.
     */
    public boolean updateDayFull(DayOfWeek day, LocalTime open, LocalTime close, boolean isActive) {
        String sql = "UPDATE openinghours SET OpenTime=?, CloseTime=?, IsActive=? WHERE DayOfWeek=?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setTime(1, Time.valueOf(open));
                ps.setTime(2, Time.valueOf(close));
                ps.setInt(3, isActive ? 1 : 0); // המרה ל-TinyInt
                ps.setString(4, formatDayForDB(day)); // "Sunday"
                
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Updates a regular opening hour record in the database.
     * @param day The day of the week to update.
     * @param openTime The original open time (primary key).
     * @param newCloseTime The new closing time to set.
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateRegularHours(DayOfWeek day, LocalTime newOpenTime, LocalTime newCloseTime) {
        // Changed SQL: Identify by DayOfWeek only, and update both times.
        String sql = "UPDATE openinghours SET OpenTime = ?, CloseTime = ? WHERE DayOfWeek = ?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setTime(1, Time.valueOf(newOpenTime));
                ps.setTime(2, Time.valueOf(newCloseTime));
                ps.setString(3, formatDayForDB(day));
                
                int rowsAffected = ps.executeUpdate();
                System.out.println("Update Regular Hours: " + rowsAffected + " rows updated for " + day);
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Placeholder update for the Repository interface.
     * @param objToUpdate The object to update.
     * @return false.
     */
    @Override
    public boolean update(Opening_Hours objToUpdate) {
        return false;
    }

    /**
     * Deactivates a regular hour record by setting IsActive to 0.
     * Since the table uses a composite key, this method targets the day ordinal as a fallback.
     * It is recommended to use deactivateByDayAndSlot instead.
     * @param id The ordinal of the DayOfWeek (1 for Sunday, etc).
     * @return true if deactivated, false otherwise.
     */
    @Override
    public boolean deleteById(int id) {
        if (id < 1 || id > 7) return false;
        DayOfWeek day = DayOfWeek.of(id == 7 ? 7 : id); // Mapping logic
        
        String sql = "UPDATE openinghours SET IsActive = 0 WHERE DayOfWeek = ?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setString(1, day.toString());
                boolean success = ps.executeUpdate() > 0;
                if(success) init(); // Refresh cache
                return success;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Sets a specific regular hour slot to inactive.
     * @param day The day of the week.
     * @param openTime The specific opening time slot.
     * @return true if successful.
     */
    public boolean deactivateByDayAndSlot(DayOfWeek day, LocalTime openTime) {
        // Updated to rely primarily on DayOfWeek formatting
        String sql = "UPDATE openinghours SET IsActive = 0 WHERE DayOfWeek = ?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setString(1, formatDayForDB(day));
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Retrieves the opening hours data.
     * @param id The ID (not used for this specific entity).
     * @return null.
     */
    @Override
    public Opening_Hours getById(int id) {
        return null;
    }
}