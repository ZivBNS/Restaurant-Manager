package Data;
import java.sql.Connection; 
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB_Controller {
	private Connection con;
	private static DB_Controller instance;
	
	// db using single-tone pattern
	// connection started when first time get help from db and ends when server is disconnecting
	private DB_Controller() {
		try {
		//	con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bistro?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "zaqwsxcde321");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/sys?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "212009666");
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
    	try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    	
    	return true;
    }




//if we will need to make more tables
/*
void createTables() {
	Statement stmt;
	try {
		stmt = con.createStatement();
		stmt.executeUpdate("create table `Order`(order_number INT PRIMARY KEY, order_date DATE, number_of_guests INT NOT NULL, confirmation_code INT UNIQUE, subscriber_id INT, date_of_placing_order DATE NOT NULL);");
	}
	catch (SQLException e) {
		e.printStackTrace();
		}
	}
*/
}