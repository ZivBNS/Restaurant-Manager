package Data;
import java.sql.Connection; 
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DB_Controller {
	private Connection con;
	private static DB_Controller instance;
	
	// db using single-tone pattern
	// connection started when first time get help from db and ends when server is disconnecting
	private DB_Controller() {
		try {
			//con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "zaqwsxcde321");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "212009666");
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
	Statement stmt;
	DB_Controller db=DB_Controller.getInstance();
	try {
		Connection con = db.getConnection();
		stmt = con.createStatement();
		stmt.executeUpdate("CREATE TABLE Users (ID INT PRIMARY KEY AUTO_INCREMENT, FirstName VARCHAR(25),LastName VARCHAR(25), Phone VARCHAR(14), Email VARCHAR(35), Username VARCHAR(20) UNIQUE , Password VARCHAR(20), Barcode VARCHAR(10), Identity ENUM('Subscriber', 'Manager', 'Employee') NOT NULL);");
		stmt.executeUpdate("CREATE TABLE Tables (ID INT PRIMARY KEY AUTO_INCREMENT, TableNumber INT , Size INT , IsActive BOOLEAN DEFAULT TRUE);");
		stmt.executeUpdate("CREATE TABLE OpeningHours (DayOfWeek ENUM('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday') NOT NULL, OpenTime TIME, CloseTime TIME, PRIMARY KEY (DayOfWeek, OpenTime));");
		stmt.executeUpdate("CREATE TABLE SpecialHours (Date DATE PRIMARY KEY, OpenTime TIME, CloseTime TIME, Description TEXT);");	
		stmt.executeUpdate("CREATE TABLE Orders (ID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, TableID INT, Phone VARCHAR(14), Email VARCHAR(35), OrderStartTime DATETIME, OrderEndTime DATETIME , ActualArrivalTime DATETIME, ActualDepartureTime DATETIME, NumberOfDiners INT, ConfirmationCode INT, Status VARCHAR(25), CreationTime DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (UserID) REFERENCES Users(ID), FOREIGN KEY (TableID) REFERENCES Tables(ID));");
		stmt.executeUpdate("CREATE TABLE Waitlist (ID INT PRIMARY KEY AUTO_INCREMENT, OrderID INT UNIQUE, Status VARCHAR(25),creationTime DATETIME, TableFreedTime DATETIME, FOREIGN KEY (OrderID) REFERENCES Orders(ID));");
		stmt.executeUpdate("CREATE TABLE Bills (ID INT PRIMARY KEY AUTO_INCREMENT, OrderID INT UNIQUE, TotalAmount DECIMAL(10, 2) NOT NULL, BillDetails TEXT, DiscountPercentage DECIMAL(5, 2) DEFAULT 0.00, Status VARCHAR(25), FOREIGN KEY (OrderID) REFERENCES Orders(ID));");
		System.out.println("Created successfully, all the cavod");
	}
	
	catch (SQLException e) {
		e.printStackTrace();
		System.out.println("ERROR - COULD NOT CREATE TABLES ISSUE");
		}
	}
}