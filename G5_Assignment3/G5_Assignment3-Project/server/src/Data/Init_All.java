package Data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.Restaurant_Table;

public class Init_All {
    //READ: how to use:
	//1. make sure that the tables are not exist in the db(mysql)
	//2. change the password in db_controller to the pass that matches yours
	//3. then you can run this main
	
	
    public static void main(String[] args) {
    	Statement stmt;
    	DB_Controller db=DB_Controller.getInstance();
    	try {
    		PooledConnection pcon=db.getConnection();
    		Connection con = pcon.getConnection();
    		
    		stmt = con.createStatement();
    		createTables(con, stmt);
    		initTables(con, stmt);
    		initOpeningHours(con, stmt);
    		initUsers(con,stmt);
    		initReservations(con, stmt);
    		initWaitlists(con, stmt);
    	}
    	
    	catch (SQLException e) {
    		e.printStackTrace();
    		System.out.println("ERROR - COULD NOT CREATE TABLES ISSUE");
    		}
    	}
    

	private static void createTables(Connection con,Statement stmt) {
		//create all tables
		try {
	    	stmt.executeUpdate("CREATE TABLE Users (ID INT PRIMARY KEY AUTO_INCREMENT, FirstName VARCHAR(25),LastName VARCHAR(25), Phone VARCHAR(14), Email VARCHAR(35), Username VARCHAR(20) UNIQUE , Password VARCHAR(20), subscriberCode INT, Identity ENUM('Subscriber', 'Manager', 'Employee') NOT NULL);");
			stmt.executeUpdate("CREATE TABLE Tables (ID INT PRIMARY KEY AUTO_INCREMENT, TableNumber INT , Size INT , IsActive BOOLEAN DEFAULT TRUE);");
			stmt.executeUpdate("CREATE TABLE OpeningHours (DayOfWeek ENUM('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday') NOT NULL, OpenTime TIME, CloseTime TIME,IsActive BOOLEAN DEFAULT TRUE, PRIMARY KEY (DayOfWeek, OpenTime));");
			stmt.executeUpdate("CREATE TABLE SpecialHours (Date DATE PRIMARY KEY, OpenTime TIME, CloseTime TIME, Description TEXT);");	
			stmt.executeUpdate("CREATE TABLE Reservations (ID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, TableID INT, Phone VARCHAR(14), Email VARCHAR(35), ReservationStartTime DATETIME, ReservationEndTime DATETIME , ActualArrivalTime DATETIME, ActualDepartureTime DATETIME, NumberOfDiners INT, ConfirmationCode INT, Status VARCHAR(25), CreationTime DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (UserID) REFERENCES Users(ID), FOREIGN KEY (TableID) REFERENCES Tables(ID));");
			stmt.executeUpdate("CREATE TABLE Waitlist (ID INT PRIMARY KEY AUTO_INCREMENT, ReservationID INT UNIQUE, Status VARCHAR(25),creationTime DATETIME, TableFreedTime DATETIME, FOREIGN KEY (ReservationID) REFERENCES Reservations(ID));");
			stmt.executeUpdate("CREATE TABLE Bills (ID INT PRIMARY KEY AUTO_INCREMENT, ReservationID INT UNIQUE, TotalAmount DECIMAL(10, 2) NOT NULL, BillDetails TEXT, DiscountPercentage DECIMAL(5, 2) DEFAULT 0.00, Status VARCHAR(25), FOREIGN KEY (ReservationID) REFERENCES Reservations(ID));");
		}catch (SQLException e) {
			e.printStackTrace();
    		System.out.println("ERROR - COULD NOT CREATE TABLES ISSUE");
		}
    }

    private static void initOpeningHours(Connection con, Statement stmt) {
    	try {
    		//create default oppening time for the first time
    		//ראשון-חמישי מ8 בבוקר עד 11 בלילה
    		//שישי מהבוקר עד הצהריים, מוצש מ8 בלילה עד 11
    		String[] weekDays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
    		for (String day : weekDays) {
    		    stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('" + day + "', '08:00:00', '23:00:00')");
    		}
    		stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Friday', '08:00:00', '14:00:00')");
    		stmt.executeUpdate("INSERT INTO OpeningHours (DayOfWeek, OpenTime, CloseTime) VALUES ('Saturday', '20:00:00', '23:00:00')");
    		System.out.println("inserted DEFAULT data to OPENING HOURS");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("failed to insert open hours for some day");
		}	
	}
    
    private static void initTables(Connection con,Statement stmt) {
		int i=0;
    	try {
    		//create default tables for the first time
    		//4 tables for 2,4 tables for 4, 2 tables for 8
    		List<Restaurant_Table> rTables= new ArrayList<Restaurant_Table>();
    		for (i = 0; i < 4; i++) {
    			rTables.add(new Restaurant_Table(2));
    			rTables.add(new Restaurant_Table(4));
    			if (i<2) rTables.add(new Restaurant_Table(8));
    		}
    		i=1;
    		for (Restaurant_Table rt:rTables) 
    			stmt.executeUpdate("INSERT INTO Tables (TableNumber, Size, IsActive) VALUES ("+ (i++) +", "+ rt.getSize() +", true)");
    		System.out.println("inserted DEFAULT data to TABLES");
    		System.out.println("Created successfully, all the cavod");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("failed to insert table: "+ i);
			}
    }

    private static void initUsers(Connection con, Statement stmt) {
        String[] firstNames = {"Oshri", "Dor", "Daniel", "Ziv", "John", "Jennifer", "Michael", "Linda", "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Charles", "Karen"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin"};

        int subscriberCode = 100;
        char usernameChar = 'a';

        try {
            for (int i = 0; i < 20; i++) {
                String firstName = firstNames[i];
                String lastName = lastNames[i];
                String phone = "050" + (1000000 + i); // מייצר מספר טלפון ייחודי
                String email = firstName.toLowerCase() + i + "@bistro.com";
                String username = String.valueOf(usernameChar++); // a, b, c...
                String password = "1";
                
                // יצירת השאילתה
                String sql = String.format(
                    "INSERT INTO Users (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity) " +
                    "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %d, 'Subscriber')",
                    firstName, lastName, phone, email, username, password, subscriberCode++
                );

                stmt.executeUpdate(sql);
            }
            System.out.println("Successfully initialized 20 Subscribers (Users 'a' through 't').");

        } catch (SQLException e) {
            System.err.println("Error initializing users: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    private static void initReservations(Connection con, Statement stmt) {
        
    	int lastCode = 100000;
        List<Integer> userIds = new ArrayList<>();
        Map<Integer, String[]> userData = new HashMap<>(); // לשמירת טלפון ואימייל לפי ID

        // 1. שליפת ה-IDs והנתונים של המשתמשים הקיימים
        try (ResultSet rs = stmt.executeQuery("SELECT ID, Phone, Email FROM Users WHERE Identity = 'Subscriber'")) {
            while (rs.next()) {
                int id = rs.getInt("ID");
                userIds.add(id);
                userData.put(id, new String[]{rs.getString("Phone"), rs.getString("Email")});
            }
        } catch (SQLException e) {
            System.out.println("Could not fetch users for reservations linking.");
        }

        int userIndex = 0;
     // --- חלק חדש: יצירת 30 הזמנות עבר (לפני 1, 2, 3 ימים) ---
        for (int daysAgo = 1; daysAgo <= 3; daysAgo++) {
            LocalDate pastDate = LocalDate.now().minusDays(daysAgo);
            for (int i = 0; i < 10; i++) {
                int currentUserId = userIds.get(userIndex % userIds.size());
                userIndex++;
                
                LocalDateTime start = pastTimeForHistory(pastDate, i);
                LocalDateTime end = start.plusHours(2);

                String sql = String.format(
                    "INSERT INTO Reservations (UserID, Phone, Email, ReservationStartTime, ReservationEndTime, NumberOfDiners, TableID, Status, ConfirmationCode) " +
                    "VALUES (%d, '%s', '%s', '%s', '%s', 4, %d, 'Completed', %d)",
                    currentUserId, userData.get(currentUserId)[0], userData.get(currentUserId)[1],
                    Timestamp.valueOf(start), Timestamp.valueOf(end), (i % 10) + 1, lastCode++
                );
                try { stmt.executeUpdate(sql); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        for (int dayOffset = 1; dayOffset <= 30; dayOffset++) {
            LocalDate date = LocalDate.now().plusDays(dayOffset);
            DayOfWeek day = date.getDayOfWeek();

            String[] hours = {};
            int reservationsPerSlot = 0;

            if (day == DayOfWeek.SUNDAY || day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY) {
                hours = new String[]{"08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:30"};
                reservationsPerSlot = 8;
            } else if (day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY) {
                hours = new String[]{"09:00", "11:00", "13:00", "15:00", "19:00", "20:00"};
                reservationsPerSlot = 2;
            } else if (day == DayOfWeek.FRIDAY) {
                hours = new String[]{"09:00", "11:00", "12:00"};
                reservationsPerSlot = 2;
            }

            for (String hour : hours) {
                LocalTime startTime = LocalTime.parse(hour);
                LocalDateTime resStart = LocalDateTime.of(date, startTime);
                LocalDateTime resEnd = resStart.plusHours(2);

                for (int j = 0; j < reservationsPerSlot; j++) {
                    int tableId;
                    int numDiners;

                    if (j < 4) { tableId = j + 1; numDiners = 2; } 
                    else if (j < 8) { tableId = j + 1; numDiners = 4; } 
                    else { tableId = (j % 2) + 9; numDiners = 8; }

                    String userIdVal = "NULL";
                    String phone = "0501112233";
                    String email = "guest@mail.com";

                    if (j % 2 == 0 && !userIds.isEmpty()) {
                        int currentUserId = userIds.get(userIndex % userIds.size());
                        userIdVal = String.valueOf(currentUserId);
                        phone = userData.get(currentUserId)[0];
                        email = userData.get(currentUserId)[1];
                        userIndex++;
                    }

                    String sql = String.format(
                        "INSERT INTO Reservations (UserID, Phone, Email, ReservationStartTime, ReservationEndTime, NumberOfDiners, TableID, Status, ConfirmationCode) " +
                        "VALUES (%s, '%s', '%s', '%s', '%s', %d, %d, 'Pending', %d)",
                        userIdVal, phone, email, Timestamp.valueOf(resStart), Timestamp.valueOf(resEnd), numDiners, tableId, lastCode++
                    );

                    try {
                        stmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        System.out.println("failed to insert reservation: " + sql);
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("Finished inserting 30 days. Half of reservations are linked to subscribers.");
    }
    
    private static LocalDateTime pastTimeForHistory(LocalDate date, int index) {
        int hour = 10 + (index % 10); // מפזר את 10 ההזמנות על פני היום
        return LocalDateTime.of(date, LocalTime.of(hour, 0));
    }
    
    	//creates 10 waitlists with status COMPLITED, also the revesvation linked to it.
    private static void initWaitlists(Connection con, Statement stmt) {
        // שליפת הזמנות שהושלמו מהימים האחרונים (כדי לקשר לווייטליסט)
        String selectSql = "SELECT ID, ReservationStartTime FROM Reservations WHERE Status = 'Completed' AND DATE(ReservationStartTime) < CURDATE()";
        
        try (ResultSet rs = stmt.executeQuery(selectSql)) {
            List<Integer> resIds = new ArrayList<>();
            List<LocalDateTime> startTimes = new ArrayList<>();
            
            while (rs.next()) {
                resIds.add(rs.getInt("ID"));
                startTimes.add(rs.getTimestamp("ReservationStartTime").toLocalDateTime());
            }

            for (int i = 0; i < resIds.size(); i++) {
                int resId = resIds.get(i);
                LocalDateTime actualArrival = startTimes.get(i);
                
                // הגרלת זמן המתנה ממוצע של 10 דקות (בין 5 ל-15 דקות)
                int waitMinutes = 5 + (int)(Math.random() * 11); 
                LocalDateTime creationTime = actualArrival.minusMinutes(waitMinutes);
                
                // בווייטליסט שלנו, TableFreedTime הוא הרגע שבו הלקוח באמת קיבל שולחן (actualArrival)
                String insertWait = String.format(
                    "INSERT INTO Waitlist (ReservationID, Status, creationTime, TableFreedTime) " +
                    "VALUES (%d, 'COMPLETED', '%s', '%s')",
                    resId, Timestamp.valueOf(creationTime), Timestamp.valueOf(actualArrival)
                );
                
                stmt.executeUpdate(insertWait);
            }
            System.out.println("Successfully initialized Waitlist for " + resIds.size() + " past reservations.");

        } catch (SQLException e) {
            System.err.println("Error initializing waitlists: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
