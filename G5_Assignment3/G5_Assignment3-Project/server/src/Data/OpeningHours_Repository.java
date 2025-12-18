package Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import entities.Opening_Hours;
import entities.Restaurant;

/**
 * Repository class responsible for loading and managing restaurant operating hours.
 * It handles standard weekly schedules and specific date exceptions (holidays/events).
 * This class follows the Singleton pattern and implements the Repository_Interface.
 */
public class OpeningHours_Repository implements Repository_Interface<Opening_Hours> {
    
    private DB_Controller db = DB_Controller.getInstance();
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
     * Initializes the restaurant's operating hours by fetching data from the database.
     * It populates both the regular weekly schedule and special date exceptions.
     * Once loaded, the data is stored in the Restaurant singleton for global access.
     */
    @Override
    public void init() {
        Opening_Hours oh = new Opening_Hours();
        
        // 1. Load standard weekly operating hours
        String sqlRegular = "SELECT DayOfWeek, OpenTime, CloseTime FROM OpeningHours";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sqlRegular)) {
            
            while (rs.next()) {
                // Convert DB string to DayOfWeek enum and SQL time to LocalTime
                DayOfWeek day = DayOfWeek.valueOf(rs.getString("DayOfWeek").toUpperCase());
                LocalTime open = rs.getTime("OpenTime").toLocalTime();
                LocalTime close = rs.getTime("CloseTime").toLocalTime();
                oh.setRegularHour(day, open, close);
            }
        } catch (SQLException e) {
            System.err.println("Database Error: Failed to load regular opening hours.");
            e.printStackTrace();
        }

        // 2. Load special date exceptions (e.g., Holidays or adjusted days)
        String sqlSpecial = "SELECT Date, OpenTime, CloseTime FROM SpecialHours";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sqlSpecial)) {
            
            while (rs.next()) {
                LocalDate date = rs.getDate("Date").toLocalDate();
                
                // If OpenTime is NULL, the restaurant is considered closed for that day
                if (rs.getTime("OpenTime") != null) {
                    LocalTime open = rs.getTime("OpenTime").toLocalTime();
                    LocalTime close = rs.getTime("CloseTime").toLocalTime();
                    oh.setException(date, open, close);
                } else {
                    // Set as closed by passing null values to the entity
                    oh.setException(date, null, null); 
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error: Failed to load special date exceptions.");
            e.printStackTrace();
        }

        // 3. Update the Restaurant singleton with the newly loaded hours
        Restaurant.getInstance().setOpeningHours(oh);
        System.out.println("OpeningHours_Repository: Successfully loaded hours into Restaurant instance.");
        System.out.println(oh.toString());
    }
    
    /**
     * Persists a new Opening_Hours object to the database.
     * @param objToSet The Opening_Hours object to save.
     * @return true if successful, false otherwise.
     */
    @Override
    public boolean set(Opening_Hours objToSet) {
        // Implementation for adding new hours to DB can be added here
        return false;
    }

    /**
     * Updates existing opening hour records in the database.
     * @param objToUpdate The Opening_Hours object with updated data.
     * @return true if successful, false otherwise.
     */
    @Override
    public boolean update(Opening_Hours objToUpdate) {
        // Implementation for updating hours in DB can be added here
        return false;
    }

    /**
     * Deletes a specific hours record based on an ID or confirmation code.
     * @param id The identifier of the record to delete.
     * @return true if successful, false otherwise.
     */
    @Override
    public boolean deleteById(int id) {
        return false;
    }

    /**
     * Retrieves an Opening_Hours record by its unique database ID.
     * @param id The unique ID of the record.
     * @return The Opening_Hours object if found, null otherwise.
     */
    @Override
    public Opening_Hours getById(int id) {
        return null;
    }
}