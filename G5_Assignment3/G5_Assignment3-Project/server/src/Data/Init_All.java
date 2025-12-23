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
import java.util.List;

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
			stmt.executeUpdate("CREATE TABLE OpeningHours (DayOfWeek ENUM('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday') NOT NULL, OpenTime TIME, CloseTime TIME, PRIMARY KEY (DayOfWeek, OpenTime));");
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

    
    
    
    
    private static void initReservations(Connection con,Statement stmt) {
    	int lastCode=100000;    	
    	for (int dayOffset = 1; dayOffset <= 30; dayOffset++) {
    	    LocalDate date = LocalDate.now().plusDays(dayOffset);
    	    DayOfWeek day = date.getDayOfWeek();
    	    
    	    String[] hours = {};
    	    int reservationsPerSlot = 0;
    	    
    	    // הגדרת חוקי הימים והשעות
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

    	            // חלוקת שולחנות (4 של 2, 4 של 4, 2 של 8)
    	            if (j < 4) { 
    	                tableId = j + 1; // שולחנות 1-4 (גודל 2)
    	                numDiners = 2; 
    	            } else if (j < 8) { 
    	                tableId = j + 1; // שולחנות 5-8 (גודל 4)
    	                numDiners = 4; 
    	            } else { 
    	                tableId = (j % 2) + 9; // שולחנות 9-10 (גודל 8)
    	                numDiners = 8; 
    	            }

    	            // יצירת השאילתה עם הקוד הרץ
    	            String sql = String.format(
    	                "INSERT INTO Reservations (Phone, Email, ReservationStartTime, ReservationEndTime, NumberOfDiners, TableID, Status, ConfirmationCode) " +
    	                "VALUES ('0501112233', 'customer@mail.com', '%s', '%s', %d, %d, 'Pending', %d)",
    	                Timestamp.valueOf(resStart), Timestamp.valueOf(resEnd), numDiners, tableId, lastCode++
    	            );
    	            
    	            try {
						stmt.executeUpdate(sql);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.out.println("failed to insert reservetion: "+sql);
					}
    	        }
    	    }
    	}
    	System.out.println("Finished inserting 30 days of reservations starting from code: 100000");
    }
    
    	//creates 10 waitlists with status COMPLITED, also the revesvation linked to it.
	private static void initWaitlists(Connection con, Statement stmt) {

		int pastConfirmationCode = 1000; // קודים מתחת ל-100,000
		LocalDateTime now = LocalDateTime.now();

		try {
		    for (int i = 1; i <= 10; i++) {
		        // 2. יצירת זמן רנדומלי בשבוע האחרון (בין לפני שבוע לאתמול)
		        int daysBack = (int) (Math.random() * 7) + 1; // 1 עד 7 ימים אחורה
		        int randomHour = 8 + (int) (Math.random() * 12); // שעות 8:00 עד 20:00
		        LocalDateTime pastTime = now.minusDays(daysBack).withHour(randomHour).withMinute(0);
		        
		        // 3. הכנסת הזמנה לטבלת Reservations
		        // סטטוס 'Completed' כי אלו הזמנות מהעבר
		        String insertRes = String.format(
		            "INSERT INTO Reservations (Phone, Email, ReservationStartTime, ReservationEndTime, NumberOfDiners, TableID, Status, ConfirmationCode) " +
		            "VALUES ('0509998877', 'past_customer@test.com', '%s', '%s', 4, %d, 'Completed', %d)",
		            Timestamp.valueOf(pastTime), 
		            Timestamp.valueOf(pastTime.plusHours(2)), 
		            (i % 10) + 1, // חלוקה בין השולחנות 1-10
		            pastConfirmationCode++
		        );
		        
		        // הרצת השאילתה וקבלת ה-ID שנוצר אוטומטית (כדי להשתמש בו בווייטליסט)
		        stmt.executeUpdate(insertRes, Statement.RETURN_GENERATED_KEYS);
		        ResultSet rs = stmt.getGeneratedKeys();
		        
		        if (rs.next()) {
		            int newResID = rs.getInt(1);
		            
		            // 4. הכנסת רשומה תואמת לטבלת Waitlist
		            // זמן יצירה: שעה לפני ההזמנה, זמן התפנות: זמן תחילת ההזמנה
		            int timeOffset = 7;
		            LocalDateTime creationTime = pastTime.minusMinutes(timeOffset%29);
		            timeOffset+=7;
		            LocalDateTime tableFreedTime = pastTime;

		            String insertWait = String.format(
		                "INSERT INTO Waitlist (ReservationID, Status, creationTime, TableFreedTime) " +"VALUES (%d, 'COMPLETED', '%s', '%s')",
		                newResID, 
		                Timestamp.valueOf(creationTime), 
		                Timestamp.valueOf(tableFreedTime)
		            );
		            
		            stmt.executeUpdate(insertWait);
		        }
		    }
		    System.out.println("Successfully inserted 10 past Reservations and 10 COMPLETED Waitlist entries.");

		} catch (SQLException e) {
		    System.err.println("Error populating past data: " + e.getMessage());
		    e.printStackTrace();
		}
	}
}
