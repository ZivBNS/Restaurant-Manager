package Data;
import java.sql.Connection; 
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import entities.Restaurant_Table;

public class DB_Controller {
	private Connection con;
	private static DB_Controller instance;
	
	// db using single-tone pattern
	// connection started when first time get help from db and ends when server is disconnecting
	private DB_Controller() {
		try {
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "zaqwsxcde321");
			//con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "212009666");
			System.out.println("Connection Succeed");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Connection FAILED");
		}
	}

	public static DB_Controller getInstance() {
        if (instance == null)
            instance = new DB_Controller();
        return instance;
    }

    public Connection getConnection() {
        return con;
    }
    
    // server use this method to close connection when exit.
    public boolean closeConnection() {
    	if(con == null)
    		return true;
    	try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    	
    	return true;
    }




//if we will need to make more tables

public static void main(String[] args) {
	int i;
	Statement stmt;
	DB_Controller db=DB_Controller.getInstance();
	try {
		Connection con = db.getConnection();
		stmt = con.createStatement();
		//create all tables
		stmt.executeUpdate("CREATE TABLE Users (ID INT PRIMARY KEY AUTO_INCREMENT, FirstName VARCHAR(25),LastName VARCHAR(25), Phone VARCHAR(14), Email VARCHAR(35), Username VARCHAR(20) UNIQUE , Password VARCHAR(20), subscriberCode INT, Identity ENUM('Subscriber', 'Manager', 'Employee') NOT NULL);");
		stmt.executeUpdate("CREATE TABLE Tables (ID INT PRIMARY KEY AUTO_INCREMENT, TableNumber INT , Size INT , IsActive BOOLEAN DEFAULT TRUE);");
		stmt.executeUpdate("CREATE TABLE OpeningHours (DayOfWeek ENUM('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday') NOT NULL, OpenTime TIME, CloseTime TIME, PRIMARY KEY (DayOfWeek, OpenTime));");
		stmt.executeUpdate("CREATE TABLE SpecialHours (Date DATE PRIMARY KEY, OpenTime TIME, CloseTime TIME, Description TEXT);");	
		stmt.executeUpdate("CREATE TABLE Reservations (ID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, TableID INT, Phone VARCHAR(14), Email VARCHAR(35), ReservationStartTime DATETIME, ReservationEndTime DATETIME , ActualArrivalTime DATETIME, ActualDepartureTime DATETIME, NumberOfDiners INT, ConfirmationCode INT, Status VARCHAR(25), CreationTime DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (UserID) REFERENCES Users(ID), FOREIGN KEY (TableID) REFERENCES Tables(ID));");
		stmt.executeUpdate("CREATE TABLE Waitlist (ID INT PRIMARY KEY AUTO_INCREMENT, ReservationID INT UNIQUE, Status VARCHAR(25),creationTime DATETIME, TableFreedTime DATETIME, FOREIGN KEY (ReservationID) REFERENCES Reservations(ID));");
		stmt.executeUpdate("CREATE TABLE Bills (ID INT PRIMARY KEY AUTO_INCREMENT, ReservationID INT UNIQUE, TotalAmount DECIMAL(10, 2) NOT NULL, BillDetails TEXT, DiscountPercentage DECIMAL(5, 2) DEFAULT 0.00, Status VARCHAR(25), FOREIGN KEY (ReservationID) REFERENCES Reservations(ID));");
		
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
	}
	
	catch (SQLException e) {
		e.printStackTrace();
		System.out.println("ERROR - COULD NOT CREATE TABLES ISSUE");
		}
	}
}