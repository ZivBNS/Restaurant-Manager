package Data;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.Restaurant_Table;

/**
 * Database Initialization class.
 * This class drops existing tables, creates the schema, and populates the 
 * database with default and sample data for testing purposes.
 */
public class Init_All {

    /**
     * Entry point for database initialization.
     * This will wipe the existing database schema and recreate it from scratch.
     */
    public static void main(String[] args) {
        Statement stmt;
        DB_Controller db = DB_Controller.getInstance();
        try {
            PooledConnection pcon = db.getConnection();
            Connection con = pcon.getConnection();

            stmt = con.createStatement();
            
            System.out.println("Starting database initialization...");
            
            // Step 0: Clear existing data
            dropExistingTables(con, stmt);
            
            // Step 1: Create Schema
            createTables(con, stmt);
            
            // Step 2: Populate Data
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
        }
    }

    /**
     * Drops all existing tables in the database to ensure a clean state.
     * Foreign key checks are disabled during this process to avoid dependency errors.
     */
    private static void dropExistingTables(Connection con, Statement stmt) {
        try {
            System.out.println("Dropping existing tables...");
            
            // Disable foreign key checks to allow dropping tables with relationships
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

            // Re-enable foreign key checks
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
            stmt.executeUpdate("CREATE TABLE Users (ID INT PRIMARY KEY AUTO_INCREMENT, FirstName VARCHAR(25),LastName VARCHAR(25), Phone VARCHAR(14), Email VARCHAR(35), Username VARCHAR(20) UNIQUE , Password VARCHAR(20), subscriberCode INT, Identity ENUM('Subscriber', 'Manager', 'Employee', 'DELETED') NOT NULL);");
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
     * Initializes default opening hours for a standard week.
     */
    private static void initOpeningHours(Connection con, Statement stmt) {
        try {
            String[] weekDays = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday" };
            for (String day : weekDays) {
                stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('" + day + "', '08:00:00', '23:00:00')");
            }
            stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Friday', '08:00:00', '14:00:00')");
            stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Saturday', '20:00:00', '23:00:00')");
            System.out.println("Inserted default opening hours.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes default restaurant tables with varying capacities.
     */
    private static void initTables(Connection con, Statement stmt) {
        try {
            List<Restaurant_Table> rTables = new ArrayList<Restaurant_Table>();
            for (int i = 0; i < 4; i++) {
                rTables.add(new Restaurant_Table(2));
                rTables.add(new Restaurant_Table(4));
                if (i < 2) rTables.add(new Restaurant_Table(8));
            }
            int tableNum = 1;
            for (Restaurant_Table rt : rTables) {
                stmt.executeUpdate("INSERT INTO Tables (TableNumber, Size, IsActive) VALUES (" + (tableNum++) + ", " + rt.getSize() + ", true)");
            }
            System.out.println("Inserted default tables.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Generates bills for all 'Completed' reservations.
     * Applies a 15% discount if the customer is a Subscriber.
     */
    private static void initBills(Connection con, Statement stmt) {
        // Query to join Reservations with Users to identify subscribers for discount logic
        String selectSql = "SELECT r.ID, u.Identity " +
                           "FROM Reservations r " +
                           "LEFT JOIN Users u ON r.UserID = u.ID " +
                           "WHERE r.Status = 'Completed'";

        String insertSql = "INSERT INTO Bills (ReservationID, TotalAmount, BillDetails, DiscountPercentage, Status) " +
                           "VALUES (?, ?, ?, ?, ?)";

        try (ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = con.prepareStatement(insertSql)) {

            int count = 0;
            while (rs.next()) {
                int resId = rs.getInt("ID");
                String identity = rs.getString("Identity"); // 'Subscriber', 'Employee', etc.

                // 1. Generate Random Total Amount (50.00 to 450.00)
                double rawAmount = 50.0 + (Math.random() * 400.0);
                double totalAmount = Math.round(rawAmount * 100.0) / 100.0;

                // 2. Determine Discount (15% for Subscribers)
                double discount = 0.0;
                if ("Subscriber".equalsIgnoreCase(identity)) {
                    discount = 15.0;
                }

                // 3. Generate Details based on price
                String details;
                if (totalAmount < 100) details = "Light Lunch Special + Drinks";
                else if (totalAmount < 250) details = "Standard Dinner Service (2 Guests)";
                else details = "Premium Chef's Special + Wine Bottle";

                // 4. Set Values
                ps.setInt(1, resId);
                ps.setDouble(2, totalAmount);
                ps.setString(3, details);
                ps.setDouble(4, discount);
                ps.setString(5, "Paid"); // Assuming completed reservations are paid

                ps.addBatch();
                count++;
            }

            ps.executeBatch();
            System.out.println("Bills: Generated " + count + " bills for completed reservations.");

        } catch (SQLException e) {
            System.err.println("Error initializing bills: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Initializes a set of dummy subscribers for the system.
     */
    private static void initUsers(Connection con, Statement stmt) {
        String[] firstNames = { "Oshri", "Dor", "Daniel", "Ziv", "John", "Jennifer", "Michael", "Linda", "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Charles", "Karen" };
        String[] lastNames = { "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin" };
        String[] EmployeeFirstNames = { "workerName1" };
        String[] EmployeeLastNames = { "lastName1" };
        
        int subCode = 100000;
        char userChar = 'a';
        try {
            for (int i = 0; i < 20; i++) {
                String sql = String.format("INSERT INTO Users (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity) VALUES ('%s', '%s', '050%d', '%s', '%s', '1', %d, 'Subscriber')",
                        firstNames[i], lastNames[i], (1000000 + i), firstNames[i].toLowerCase() + "@mail.com", String.valueOf(userChar++), subCode++);
                stmt.executeUpdate(sql);
            }
            System.out.println("Inserted 20 subscribers.");
            for (int i = 0; i < EmployeeFirstNames.length; i++) {
                String sql = String.format("INSERT INTO Users (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity) VALUES ('%s', '%s', '050%d', '%s', '%s', '1', %d, 'Employee')",
                		EmployeeFirstNames[i], EmployeeLastNames[i], (1000000 + i), EmployeeFirstNames[i].toLowerCase() + "@mail.com", "1", subCode++);
                stmt.executeUpdate(sql);
            }
            System.out.println("Inserted Employees.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void initReservations(Connection con, Statement stmt) {
        int lastCode = 200000;
        List<Integer> userIds = new ArrayList<Integer>();
        Map<Integer, String[]> userData = new HashMap<Integer, String[]>();

        String insertSql = "INSERT INTO Reservations (UserID, Phone, Email, ReservationStartTime, " +
                           "ReservationEndTime, ActualArrivalTime, ActualDepartureTime, " +
                           "NumberOfDiners, TableID, Status, ConfirmationCode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // Fetch users
            ResultSet rs = stmt.executeQuery("SELECT ID, Phone, Email FROM Users WHERE Identity = 'Subscriber'");
            while (rs.next()) {
                int id = rs.getInt("ID");
                userIds.add(id);
                userData.put(id, new String[] { rs.getString("Phone"), rs.getString("Email") });
            }

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                int userIdx = 0;

                // --- PART 1: Historical (Completed) ---
                for (int day = 1; day <= 31; day++) {
                    LocalDate date = LocalDate.of(2025, 12, day);
                    int dailyOrders = 8 + (int)(Math.random() * 18); 
                    for (int i = 0; i < dailyOrders; i++) {
                        // ... (אותו קוד קיים לחלק ההיסטורי) ...
                        int uid = userIds.get(userIdx % userIds.size());
                        userIdx++;
                        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(10 + (i % 12), 0));
                        LocalDateTime end = start.plusHours(2);
                        LocalDateTime actArr = start.plusMinutes((int)(Math.random() * 61) - 30); 
                        LocalDateTime actDep = end.plusMinutes((int)(Math.random() * 61) - 30);
                        int numDiners = 2 + (int)(Math.random() * 7);
                        int tableId = 1 + (int)(Math.random() * 10);

                        ps.setInt(1, uid);
                        ps.setString(2, userData.get(uid)[0]);
                        ps.setString(3, userData.get(uid)[1]);
                        ps.setTimestamp(4, Timestamp.valueOf(start));
                        ps.setTimestamp(5, Timestamp.valueOf(end));
                        ps.setTimestamp(6, Timestamp.valueOf(actArr));
                        ps.setTimestamp(7, Timestamp.valueOf(actDep));
                        ps.setInt(8, numDiners);
                        ps.setInt(9, tableId);
                        ps.setString(10, "Completed");
                        ps.setInt(11, lastCode++);
                        ps.addBatch();
                    }
                }

                // --- PART 2: Future (Pending) ---
                for (int day = 15; day <= 21; day++) {
                    // ... (אותו קוד קיים לחלק העתידי) ...
                    LocalDate date = LocalDate.of(2026, 1, day);
                    int dailyOrders = 5;
                    for (int i = 0; i < dailyOrders; i++) {
                        int uid = userIds.get(userIdx % userIds.size());
                        userIdx++;
                        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(12 + (int)(Math.random() * 10), 0));
                        ps.setInt(1, uid);
                        ps.setString(2, userData.get(uid)[0]);
                        ps.setString(3, userData.get(uid)[1]);
                        ps.setTimestamp(4, Timestamp.valueOf(start));
                        ps.setTimestamp(5, Timestamp.valueOf(start.plusHours(2)));
                        ps.setNull(6, java.sql.Types.TIMESTAMP);
                        ps.setNull(7, java.sql.Types.TIMESTAMP);
                        ps.setInt(8, 2 + (int)(Math.random() * 5));
                        ps.setNull(9, java.sql.Types.INTEGER);
                        ps.setString(10, "Pending");
                        ps.setInt(11, lastCode++);
                        ps.addBatch();
                    }
                }

                // --- PART 3: Current Live (Active) - NEW ---
                // אנשים שיושבים כרגע במסעדה (נניח התאריך הוא 10.1.2026)
                LocalDate today = LocalDate.of(2026, 1, 10); 
                int activeTables = 4; // 4 שולחנות פעילים כרגע

                for (int i = 0; i < activeTables; i++) {
                    int uid = userIds.get(userIdx % userIds.size());
                    userIdx++;

                    // הם הגיעו לפני שעה בערך
                    LocalDateTime start = LocalDateTime.of(today, LocalTime.now().minusMinutes(45 + (i * 10)));
                    LocalDateTime end = start.plusHours(2);
                    
                    // הם הגיעו בפועל (Actual Arrival)
                    LocalDateTime actArr = start.plusMinutes(2); 

                    int numDiners = 2 + (int)(Math.random() * 4);
                    int tableId = 1 + (int)(Math.random() * 10);

                    ps.setInt(1, uid);
                    ps.setString(2, userData.get(uid)[0]);
                    ps.setString(3, userData.get(uid)[1]);
                    ps.setTimestamp(4, Timestamp.valueOf(start));
                    ps.setTimestamp(5, Timestamp.valueOf(end));
                    ps.setTimestamp(6, Timestamp.valueOf(actArr)); // הגיעו
                    ps.setNull(7, java.sql.Types.TIMESTAMP);       // עדיין לא עזבו (NULL)
                    ps.setInt(8, numDiners);
                    ps.setInt(9, tableId); // יש להם שולחן
                    ps.setString(10, "Active"); // סטטוס פעיל
                    ps.setInt(11, lastCode++);
                    
                    ps.addBatch();
                }

                ps.executeBatch();
            }
            System.out.println("Reservations: Init complete (History, Future-Pending, Live-Active).");
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }

    private static void initWaitlists(Connection con, Statement stmt) {
        // 1. Insert Completed Waitlist Records (Historical)
        String selectSql = "SELECT ID, ReservationStartTime FROM Reservations " +
                           "WHERE Status = 'Completed' AND MONTH(ReservationStartTime) = 12";
        String insertSql = "INSERT INTO Waitlist (ReservationID, Status, creationTime, TableFreedTime) " +
                           "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            
            // --- Part A: Historical Data ---
            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                while (rs.next()) {
                    LocalDateTime arrival = rs.getTimestamp("ReservationStartTime").toLocalDateTime();
                    int day = arrival.getDayOfMonth();
                    if (day % 5 == 0) continue; 

                    if (Math.random() > 0.3) {
                        int resId = rs.getInt("ID");
                        int waitMins = 5 + (int)(Math.random() * 30);
                        LocalDateTime created = arrival.minusMinutes(waitMins);

                        ps.setInt(1, resId);
                        ps.setString(2, "Completed"); // Status
                        ps.setTimestamp(3, Timestamp.valueOf(created));
                        ps.setTimestamp(4, Timestamp.valueOf(arrival)); // Freed when they sat down
                        ps.addBatch();
                    }
                }
            }

            // --- Part B: Active Waitlist (People currently waiting) ---
            // אנחנו שולפים הזמנות עתידיות/ממתינות (Pending) ומדמים שהן ברשימת המתנה כרגע
            String selectPending = "SELECT ID, ReservationStartTime FROM Reservations " +
                                   "WHERE Status = 'Pending' LIMIT 3"; // ניקח 3 אנשים שמחכים
            
            try (ResultSet rsPending = stmt.executeQuery(selectPending)) {
                while (rsPending.next()) {
                    int resId = rsPending.getInt("ID");
                    LocalDateTime resTime = rsPending.getTimestamp("ReservationStartTime").toLocalDateTime();
                    
                    // הם נכנסו לרשימה לפני 10 דקות
                    LocalDateTime created = LocalDateTime.now().minusMinutes(10 + (int)(Math.random()*15));

                    ps.setInt(1, resId);
                    ps.setString(2, "Waiting"); // הם עדיין ברשימה
                    ps.setTimestamp(3, Timestamp.valueOf(created));
                    ps.setNull(4, java.sql.Types.TIMESTAMP); // עדיין לא התפנה שולחן (NULL)
                    ps.addBatch();
                }
            }

            ps.executeBatch();
            System.out.println("Waitlist: Initialized (Historical & Active).");
        } catch (SQLException e) { e.printStackTrace(); }
    }
    /**
     * Pre-generates historical report data for October and November.
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
                        double avgLat = 5 + (Math.random() * 15);
                        double avgOver = 2 + (Math.random() * 20);
                        stmt.executeUpdate(String.format("INSERT INTO time_report_details (report_id, day_index, avg_lateness, avg_overstay) VALUES (%d, %d, %.2f, %.2f)", reportId, day, avgLat, avgOver));

                        int orders = 15 + (int)(Math.random() * 40);
                        int waitlist = (int)(Math.random() * 10);
                        stmt.executeUpdate(String.format("INSERT INTO subscriber_report_details (report_id, day_index, total_orders, waiting_list_count) VALUES (%d, %d, %d, %d)", reportId, day, orders, waitlist));
                    }
                }
            }
            System.out.println("Initialized reports for months 10 and 11.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}