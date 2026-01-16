package Data;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.DayOfWeek;
/**
 * Database Initialization class.
 * This class checks/creates the schema, drops existing tables, creates the schema objects, 
 * and populates the database with default and sample data.
 */
public class Init_All {

	// Connection settings for initialization only
	private static final String DB_URL = "jdbc:mysql://localhost/?serverTimezone=Asia/Jerusalem";
	private static final String DB_USER = "root";
    private static final String DB_PASS = "zaqwsxcde321";
    
    /**
     * Entry point for database initialization.
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        Connection con = null;
        Statement stmt = null;

        try {
            // Connect to server directly (bypassing DB_Controller to avoid "Unknown Database" error)
            System.out.println("Connecting to database server...");
            con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            stmt = con.createStatement();
            
            System.out.println("Starting database initialization...");
            
            // Step 0: Ensure Schema Exists & Select it
            ensureSchema(stmt);
            
            // Step 1: Clear existing data
            dropExistingTables(con, stmt);
            
            createTables(con, stmt);
            initTables(con, stmt);
            initOpeningHours(con, stmt);
            initUsers(con, stmt);
            initReservations(con, stmt); 
            initWaitlists(con, stmt);    
            initMonthlyReports(con, stmt);
            initBills(con, stmt);
            
            System.out.println("Initialization completed successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("ERROR - DATABASE INITIALIZATION FAILED");
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Checks if the schema 'bistro' exists, creates it if not, and selects it.
     * @param stmt the statement object.
     */
    private static void ensureSchema(Statement stmt) {
        try {
            System.out.println("Checking schema 'bistro'...");
            // Create schema if it doesn't exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS bistro");
            System.out.println("Database 'bistro' ensured.");
            
            // Select the schema for subsequent commands
            stmt.executeUpdate("USE bistro");
            System.out.println("Schema 'bistro' selected.");
        } catch (SQLException e) {
            System.err.println("Error creating/selecting schema: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Drops all existing tables in the database to ensure a clean state.
     */
    private static void dropExistingTables(Connection con, Statement stmt) {
        try {
            System.out.println("Dropping existing tables...");
            
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");

            String[] tables = {
                "Bills", 
                "Waitlist", 
                "Reservations", 
                "subscriber_report_details", 
                "time_report_details", 
                "reports_management", 
                "SpecialHours", 
                "OpeningHours", 
                "Tables", 
                "Users"
            };

            for (String table : tables) {
                stmt.executeUpdate("DROP TABLE IF EXISTS " + table);
            }

            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
            
            System.out.println("All existing tables dropped.");
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates all necessary tables for the restaurant system.
     */
    private static void createTables(Connection con, Statement stmt) {
        try {
            System.out.println("Creating tables...");
            stmt.executeUpdate("CREATE TABLE Users (ID INT PRIMARY KEY AUTO_INCREMENT, FirstName VARCHAR(25),LastName VARCHAR(25), Phone VARCHAR(14), Email VARCHAR(35), Username VARCHAR(20) UNIQUE , Password VARCHAR(20), subscriberCode INT, Identity ENUM('Subscriber', 'Manager', 'Employee', 'Deleted') NOT NULL);");
            stmt.executeUpdate("CREATE TABLE Tables (ID INT PRIMARY KEY AUTO_INCREMENT, TableNumber INT , Size INT , IsActive BOOLEAN DEFAULT TRUE);");
            stmt.executeUpdate("CREATE TABLE OpeningHours (DayOfWeek ENUM('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday') NOT NULL, OpenTime TIME, CloseTime TIME,IsActive BOOLEAN DEFAULT TRUE, PRIMARY KEY (DayOfWeek, OpenTime));");
            stmt.executeUpdate("CREATE TABLE SpecialHours (Date DATE PRIMARY KEY, OpenTime TIME, CloseTime TIME, Description TEXT);");
            stmt.executeUpdate("CREATE TABLE Reservations (ID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, TableID INT, Phone VARCHAR(14), Email VARCHAR(35), ReservationStartTime DATETIME, ReservationEndTime DATETIME , ActualArrivalTime DATETIME, ActualDepartureTime DATETIME, NumberOfDiners INT, ConfirmationCode INT, Status VARCHAR(25), CreationTime DATETIME DEFAULT CURRENT_TIMESTAMP,RemindedPreArrival BOOLEAN DEFAULT FALSE,RemindedDeparture BOOLEAN DEFAULT FALSE, FOREIGN KEY (UserID) REFERENCES Users(ID), FOREIGN KEY (TableID) REFERENCES Tables(ID));");
            stmt.executeUpdate("CREATE TABLE Waitlist (ID INT PRIMARY KEY AUTO_INCREMENT, ReservationID INT UNIQUE, Status VARCHAR(25),creationTime DATETIME, TableFreedTime DATETIME, FOREIGN KEY (ReservationID) REFERENCES Reservations(ID));");
            stmt.executeUpdate("CREATE TABLE Bills (ID INT PRIMARY KEY AUTO_INCREMENT, ReservationID INT UNIQUE, TotalAmount DECIMAL(10, 2) NOT NULL, BillDetails TEXT, DiscountPercentage DECIMAL(5, 2) DEFAULT 0.00, Status VARCHAR(25), FOREIGN KEY (ReservationID) REFERENCES Reservations(ID));");
            
            // Report Management Tables
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reports_management ("
                    + "report_id INT AUTO_INCREMENT PRIMARY KEY, " + "report_month INT NOT NULL, "
                    + "report_year INT NOT NULL, " + "date_generated DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS time_report_details (" + "report_id INT, " + "day_index INT NOT NULL, "
                            + "avg_lateness DOUBLE, " + "avg_overstay DOUBLE, " + "PRIMARY KEY (report_id, day_index), "
                            + "FOREIGN KEY (report_id) REFERENCES reports_management(report_id) ON DELETE CASCADE)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subscriber_report_details (" + "report_id INT, "
                    + "day_index INT NOT NULL, " + "total_orders INT, " + "waiting_list_count INT, "
                    + "PRIMARY KEY (report_id, day_index), "
                    + "FOREIGN KEY (report_id) REFERENCES reports_management(report_id) ON DELETE CASCADE)");
            
            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes opening hours based on new requirements:
     * Monday: 08:00 - 03:00 (Next day)
     * Friday: 08:00 - 14:00
     * Saturday: 20:00 - 23:00
     * Others: 08:00 - 23:00
     */
    private static void initOpeningHours(Connection con, Statement stmt) {
        try {
            // Standard Days
            String[] standardDays = { "Sunday", "Tuesday", "Wednesday", "Thursday" };
            for (String day : standardDays) {
                stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('" + day + "', '08:00:00', '23:00:00')");
            }
            
            // Special Days requested
            // Monday: 8 AM to 3 AM
            stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Monday', '08:00:00', '03:00:00')");
            
            // Friday: 8 AM to 2 PM
            stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Friday', '08:00:00', '14:00:00')");
            
            // Saturday: 8 PM to 11 PM
            stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Saturday', '20:00:00', '23:00:00')");
            
            System.out.println("Inserted opening hours (Monday 08:00-03:00 included).");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes 6 specific tables with sizes: 2, 3, 4, 6, 6, 8.
     */
    private static void initTables(Connection con, Statement stmt) {
        try {
            // Specific sizes requested
            int[] sizes = {2, 3, 4, 6, 6, 8};
            
            int tableNum = 1;
            for (int size : sizes) {
                stmt.executeUpdate("INSERT INTO Tables (TableNumber, Size, IsActive) VALUES (" + (tableNum++) + ", " + size + ", true)");
            }
            System.out.println("Inserted 6 tables with sizes: 2, 3, 4, 6, 6, 8.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes Bills for completed AND active reservations.
     * Active reservations get an empty/open bill.
     */
    private static void initBills(Connection con, Statement stmt) {
        // UPDATED: Now selects both Completed AND Active reservations
        // Added 'r.Status' to the selection to distinguish logic inside the loop
        String selectSql = "SELECT r.ID, u.Identity, r.Status " +
                           "FROM Reservations r " +
                           "LEFT JOIN Users u ON r.UserID = u.ID " +
                           "WHERE r.Status IN ('Completed', 'Active')";

        String insertSql = "INSERT INTO Bills (ReservationID, TotalAmount, BillDetails, DiscountPercentage, Status) " +
                           "VALUES (?, ?, ?, ?, ?)";

        try (ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = con.prepareStatement(insertSql)) {

            int count = 0;
            while (rs.next()) {
                int resId = rs.getInt("ID");
                String identity = rs.getString("Identity"); 
                String resStatus = rs.getString("Status"); // Get the status

                double totalAmount;
                String details;
                String billStatus;
                double discount = 0.0;
                
                // Calculate Discount Potential (User Identity)
                if ("Subscriber".equalsIgnoreCase(identity)) {
                    discount = 10.0;
                }

                // --- LOGIC SPLIT ---
                if ("Active".equalsIgnoreCase(resStatus)) {
                    // CASE 1: Active Reservation -> Empty/Open Bill
                    totalAmount = 0.0;
                    details = "Service in progress"; // Or "Open Tab"
                    billStatus = "Unpaid";
                } else {
                    // CASE 2: Completed Reservation -> Generate Full History
                    double rawAmount = 50.0 + (Math.random() * 400.0);
                    totalAmount = Math.round(rawAmount * 100.0) / 100.0;
                    billStatus = "Paid";

                    if (totalAmount < 100) details = "Light Lunch Special + Drinks";
                    else if (totalAmount < 250) details = "Standard Dinner Service (2 Guests)";
                    else details = "Premium Chef's Special + Wine Bottle";
                }

                // Set values to PreparedStatement
                ps.setInt(1, resId);
                ps.setDouble(2, totalAmount);
                ps.setString(3, details);
                ps.setDouble(4, discount);
                ps.setString(5, billStatus); 

                ps.addBatch();
                count++;
            }

            ps.executeBatch();
            System.out.println("Bills: Generated " + count + " bills (History & Active-Open tabs).");

        } catch (SQLException e) {
            System.err.println("Error initializing bills: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes users.
     * Updates: 
     * - Subscriber Username = char repeated 5 times (aaaaa).
     * - Password = numeric (123456).
     * - Added 1 more employee (total 2 employees).
     */
    private static void initUsers(Connection con, Statement stmt) {
        String[] firstNames = { "Oshri", "Dor", "Daniel", "Ziv", "John", "Jennifer", "Michael", "Linda", "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Charles", "Karen" };
        String[] lastNames = { "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin" };
        
        // Added "workerName2" to have 2 employees
        String[] EmployeeFirstNames = { "workerName1", "workerName2" };
        String[] EmployeeLastNames = { "lastName1", "lastName2" };
        
        int subCode = 100000;
        char userChar = 'a';
        try {
            // 1. Insert Subscribers
            for (int i = 0; i < 20; i++) {
                // Generate username: 5 times the letter (e.g., "aaaaa")
                String charStr = String.valueOf(userChar++);
                String username = charStr.repeat(5); 
                String password = "123456"; // Default numeric password

                String sql = String.format("INSERT INTO Users (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity) VALUES ('%s', '%s', '050%d', '%s', '%s', '%s', %d, 'Subscriber')",
                        firstNames[i], lastNames[i], (1000000 + i), firstNames[i].toLowerCase() + "@mail.com", username, password, subCode++);
                stmt.executeUpdate(sql);
            }
            System.out.println("Inserted 20 subscribers (Username: aaaaa, Pass: 123456).");

            // 2. Insert Employees (username '1', password '1' for first, sequential for others if needed, keeping simple)
            for (int i = 0; i < EmployeeFirstNames.length; i++) {
                // Giving specific usernames for employees: 1, 11... or just keeping '1' for the first as requested in previous contexts, 
                // but since unique constraint exists, we'll make them '1', '11'.
                String empUser = (i == 0) ? "11111" : "22222";
                String empPass = (i == 0) ? "1" : "2";
                
                String sql = String.format("INSERT INTO Users (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity) VALUES ('%s', '%s', '050%d', '%s', '%s', '%s', %d, 'Employee')",
                        EmployeeFirstNames[i], EmployeeLastNames[i], (2000000 + i), EmployeeFirstNames[i].toLowerCase() + "@mail.com", empUser, empPass, subCode++);
                stmt.executeUpdate(sql);
            }
            System.out.println("Inserted 2 Employees.");

            // 3. Insert Manager (username '2', password '2')
            String managerSql = String.format("INSERT INTO Users (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %d, 'Manager')",
                    "Manager", "Boss", "0509999999", "manager@bistro.com", "33333", "3", subCode++);
            stmt.executeUpdate(managerSql);
            System.out.println("Inserted Manager (User: 33333, Pass: 3).");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes reservations with various states:
     * 1. Historical (Completed) - From previous month.
     * 2. Future (Pending) - Scheduled for the next 12 days.
     * 3. Active (Live) - Currently sitting in the restaurant.
     * 4. Waitlist Candidates - Scheduled for now but without a table.
     * * @param con  The database connection.
     * @param stmt The statement object.
     */
    private static void initReservations(Connection con, Statement stmt) {
        int lastCode = 200000;
        List<Integer> userIds = new ArrayList<Integer>();
        Map<Integer, String[]> userData = new HashMap<Integer, String[]>();

        String insertSql = "INSERT INTO Reservations (UserID, Phone, Email, ReservationStartTime, " +
                           "ReservationEndTime, ActualArrivalTime, ActualDepartureTime, " +
                           "NumberOfDiners, TableID, Status, ConfirmationCode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // Fetch existing subscribers to assign reservations to real users
            ResultSet rs = stmt.executeQuery("SELECT ID, Phone, Email FROM Users WHERE Identity = 'Subscriber'");
            while (rs.next()) {
                int id = rs.getInt("ID");
                userIds.add(id);
                userData.put(id, new String[] { rs.getString("Phone"), rs.getString("Email") });
            }

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                int userIdx = 0;

                // --- PART 1: Historical (Completed) ---
                // Generates past reservations for December 2025
                for (int day = 1; day <= 31; day++) {
                    LocalDate date = LocalDate.of(2025, 12, day);
                    int dailyOrders = 8 + (int)(Math.random() * 18); 
                    for (int i = 0; i < dailyOrders; i++) {
                        int uid = userIds.get(userIdx % userIds.size());
                        userIdx++;
                        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(10 + (i % 12), 0));
                        ps.setInt(1, uid);
                        ps.setString(2, userData.get(uid)[0]);
                        ps.setString(3, userData.get(uid)[1]);
                        ps.setTimestamp(4, Timestamp.valueOf(start));
                        ps.setTimestamp(5, Timestamp.valueOf(start.plusHours(2)));
                        ps.setTimestamp(6, Timestamp.valueOf(start.plusMinutes((int)(Math.random() * 61) - 30)));
                        ps.setTimestamp(7, Timestamp.valueOf(start.plusHours(2).plusMinutes((int)(Math.random() * 61) - 30)));
                        ps.setInt(8, 2 + (int)(Math.random() * 7));
                        ps.setInt(9, 1 + (int)(Math.random() * 6));
                        ps.setString(10, "Completed");
                        ps.setInt(11, lastCode++);
                        ps.addBatch();
                    }
                }

             // --- PART 2: Future (Pending - Long Term) ---
                LocalDate tomorrow = LocalDate.now().plusDays(1);
                
                // Generates reservations for the next 12 days
                for (int i = 0; i < 12; i++) {
                    
                    LocalDate futureDate = tomorrow.plusDays(i);
                    DayOfWeek dayOfWeek = futureDate.getDayOfWeek();

                    // 1. Skip Wednesday completely (No reservations created)
                    if (dayOfWeek == java.time.DayOfWeek.WEDNESDAY) { 
                        continue; 
                    }

                    // 2. Determine Opening Hour based on Day of Week
                    int baseHour;
                    
                    switch (dayOfWeek) {
                        case FRIDAY:
                            // Fridays open early for lunch (10:00, 12:00)
                            baseHour = 10; 
                            break;
                        case SATURDAY:
                            // Saturdays open late evening (20:00, 22:00)
                            baseHour = 20; 
                            break;
                        default:
                            // Regular weekdays (Sunday, Monday, Thursday) - Evening (18:00, 20:00)
                            baseHour = 18; 
                            break;
                    }

                    for (int k = 0; k < 2; k++) {
                        int uid = userIds.get(userIdx % userIds.size());
                        userIdx++;
                        
                        // Create reservation based on the calculated baseHour
                        LocalDateTime start = LocalDateTime.of(futureDate, LocalTime.of(baseHour + (k * 2), 0));
                        
                        ps.setInt(1, uid);
                        ps.setString(2, userData.get(uid)[0]);
                        ps.setString(3, userData.get(uid)[1]);
                        ps.setTimestamp(4, Timestamp.valueOf(start));
                        ps.setTimestamp(5, Timestamp.valueOf(start.plusHours(2)));
                        ps.setNull(6, java.sql.Types.TIMESTAMP);
                        ps.setNull(7, java.sql.Types.TIMESTAMP);
                        ps.setInt(8, 2 + k);
                        ps.setNull(9, java.sql.Types.INTEGER);
                        ps.setString(10, "Pending");
                        ps.setInt(11, lastCode++);
                        ps.addBatch();
                    }
                }

                // --- PART 3: Current Live (Active - Sitting Now) ---
                // Generates orders that started 90, 60, 30, and 10 minutes ago
                int[] minutesAgo = {90, 60, 30, 10}; 
                for (int i = 0; i < minutesAgo.length; i++) {
                    int uid = userIds.get(userIdx % userIds.size());
                    userIdx++;
                    LocalDateTime start = LocalDateTime.now().minusMinutes(minutesAgo[i]).withSecond(0).withNano(0);
                    ps.setInt(1, uid);
                    ps.setString(2, userData.get(uid)[0]);
                    ps.setString(3, userData.get(uid)[1]);
                    ps.setTimestamp(4, Timestamp.valueOf(start));
                    ps.setTimestamp(5, Timestamp.valueOf(start.plusHours(2)));
                    ps.setTimestamp(6, Timestamp.valueOf(start.plusMinutes(2))); 
                    ps.setNull(7, java.sql.Types.TIMESTAMP);
                    ps.setInt(8, 2 + i);
                    ps.setInt(9, i + 1); 
                    ps.setString(10, "Active"); 
                    ps.setInt(11, lastCode++);
                    ps.addBatch();
                }

                // --- PART 4: Waitlist Candidates (Pending for NOW/Soon, No Table) ---
                // These are customers who booked for NOW or very soon, but have no table assigned (making them Waitlist candidates).
                int[] minutesFromNow = {0, 15, 30}; // Booking for now, +15 mins, +30 mins
                for (int i = 0; i < minutesFromNow.length; i++) {
                    int uid = userIds.get(userIdx % userIds.size());
                    userIdx++;
                    
                    // Booking for a near time
                    LocalDateTime start = LocalDateTime.now().plusMinutes(minutesFromNow[i]).withSecond(0).withNano(0);
                    
                    ps.setInt(1, uid);
                    ps.setString(2, userData.get(uid)[0]);
                    ps.setString(3, userData.get(uid)[1]);
                    ps.setTimestamp(4, Timestamp.valueOf(start));
                    ps.setTimestamp(5, Timestamp.valueOf(start.plusHours(2)));
                    ps.setNull(6, java.sql.Types.TIMESTAMP); // Not arrived yet
                    ps.setNull(7, java.sql.Types.TIMESTAMP);
                    ps.setInt(8, 4); // Party of 4
                    ps.setNull(9, java.sql.Types.INTEGER);   // No table assigned (Crucial for Waitlist logic!)
                    ps.setString(10, "Pending");             // Status is Pending until a table frees up
                    ps.setInt(11, lastCode++);
                    ps.addBatch();
                }

                ps.executeBatch();
            }
            System.out.println("Reservations: Init complete (History, Future, Active, Waitlist-Candidates).");
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }

    /**
     * Initializes historical and active waitlist records.
     */
    private static void initWaitlists(Connection con, Statement stmt) {
        String selectHistory = "SELECT ID, ReservationStartTime FROM Reservations " +
                               "WHERE Status = 'Completed' AND MONTH(ReservationStartTime) = 12";
                               
        String insertSql = "INSERT INTO Waitlist (ReservationID, Status, creationTime, TableFreedTime) " +
                           "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            
            // --- Part A: Historical Data  ---
            try (ResultSet rs = stmt.executeQuery(selectHistory)) {
                while (rs.next()) {
                    LocalDateTime arrival = rs.getTimestamp("ReservationStartTime").toLocalDateTime();
                    int day = arrival.getDayOfMonth();
                    if (day % 5 == 0) continue; 

                    if (Math.random() > 0.3) {
                        int resId = rs.getInt("ID");
                        int waitMins = 5 + (int)(Math.random() * 30);
                        LocalDateTime created = arrival.minusMinutes(waitMins);

                        ps.setInt(1, resId);
                        ps.setString(2, "Completed"); 
                        ps.setTimestamp(3, Timestamp.valueOf(created));
                        ps.setTimestamp(4, Timestamp.valueOf(arrival)); 
                        ps.addBatch();
                    }
                }
            }

            // --- Part B: Active Waitlist  ---
            String selectActiveWaitlist = "SELECT ID FROM Reservations " +
                                          "WHERE Status = 'Pending' " +
                                          "AND TableID IS NULL " + 
                                          "AND DATE(ReservationStartTime) = CURDATE()"; 
            
            try (ResultSet rsWait = stmt.executeQuery(selectActiveWaitlist)) {
                while (rsWait.next()) {
                    int resId = rsWait.getInt("ID");
                    
                    LocalDateTime created = LocalDateTime.now().minusMinutes(5 + (int)(Math.random()*10));

                    ps.setInt(1, resId);
                    ps.setString(2, "Waiting"); 
                    ps.setTimestamp(3, Timestamp.valueOf(created));
                    ps.setNull(4, java.sql.Types.TIMESTAMP); 
                    ps.addBatch();
                }
            }

            ps.executeBatch();
            System.out.println("Waitlist: Initialized (Historical & Active Real-Time).");
        } catch (SQLException e) { e.printStackTrace(); }
    }
    /**
     * Pre-generates historical report data.
     * Updated to allow negative values for averages (Early arrival / Left early).
     */
    private static void initMonthlyReports(Connection con, Statement stmt) {
        try {
            int[] months = {10, 11}; 
            int year = 2025;

            for (int month : months) {
                String insertMgmt = String.format("INSERT INTO reports_management (report_month, report_year) VALUES (%d, %d)", month, year);
                stmt.executeUpdate(insertMgmt, Statement.RETURN_GENERATED_KEYS);
                
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int reportId = rs.getInt(1);
                    for (int day = 1; day <= 30; day++) {
                        
                        // UPDATED: Ranges now include negative numbers
                        // Avg Lateness: Range -10 to +20 (Negative means arrived early)
                        double avgLat = -10 + (Math.random() * 30);
                        
                        // Avg Overstay: Range -15 to +25 (Negative means left before 2 hours)
                        double avgOver = -15 + (Math.random() * 40);
                        
                        stmt.executeUpdate(String.format("INSERT INTO time_report_details (report_id, day_index, avg_lateness, avg_overstay) VALUES (%d, %d, %.2f, %.2f)", reportId, day, avgLat, avgOver));

                        int orders = 15 + (int)(Math.random() * 40);
                        int waitlist = (int)(Math.random() * 10);
                        stmt.executeUpdate(String.format("INSERT INTO subscriber_report_details (report_id, day_index, total_orders, waiting_list_count) VALUES (%d, %d, %d, %d)", reportId, day, orders, waitlist));
                    }
                }
            }
            System.out.println("Initialized reports for months 10 and 11 (With negative values).");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}