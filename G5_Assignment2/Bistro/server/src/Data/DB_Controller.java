package Data;

public class DB_Controller {
	import java.sql.Connection;
	import java.sql.DriverManager;
	import java.sql.PreparedStatement;
	import java.sql.ResultSet;
	import java.sql.SQLException;
	import java.sql.Statement;
	import java.sql.Time;
	import java.util.ArrayList;
	import java.util.List;

	public static void main(String[] args)
		{
        try 
        {
			Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/sys?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false", "root", "212009666");
            System.out.println("SQL connection succeed");
            createReservationTable(conn);
            changeData(conn);
            printFlights(conn);
     	} catch (SQLException ex) 
     	    {          //handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            }
   	}
	
	public static void changeData(Connection con) {
		List<Flight> flights = new ArrayList<>();
		try 
		{
			Statement stmt= con.createStatement();
			PreparedStatement ps = con.prepareStatement("UPDATE flights SET scheduled = ? WHERE flight = ?");			
			//stmt.executeUpdate("UPDATE flights SET scheduled= '14:00' WHERE flight='KU101'");
			//stmt.executeUpdate("UPDATE flights SET scheduled= '16:33' WHERE origin='paris' AND scheduled < '15:00:00'");
			Time a= Time.valueOf("15:03:03");
			String b="LX354";
			ps.setTime(1,a);
			ps.setString(2,b);
			ps.executeUpdate();
			
		} catch (SQLException e) {e.printStackTrace();}
		
	}

	
	
	//public static void printCourses(Connection con)
	//{
		/*Statement stmt;
		try 
		{
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM courses;");
	 		while(rs.next())
	 		{
				 // Print out the values
				 System.out.println(rs.getString(1)+"  " +rs.getString(2));
			} 
			rs.close();
			//stmt.executeUpdate("UPDATE course SET semestr=\"W08\" WHERE num=61309");
		} catch (SQLException e) {e.printStackTrace();}*/
	//}
	
	public static void printFlights(Connection con)
	{
		Statement stmt;
		try 
		{
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM flights;");
	 		while(rs.next())
	 		{
				 // Print out the values
				 System.out.println(rs.getString(1)+"  " +rs.getString(2)+"  " +rs.getString(3)+"  " +rs.getString(4)+"  " +rs.getString(5));
			} 
			rs.close();

			stmt.executeUpdate("UPDATE flights SET scheduled= '16:33:00' WHERE origin='paris' AND scheduled < '15:00:00'");
		//	stmt.executeUpdate("UPDATE flights SET scheduled= '14:00' WHERE flight='KU101'");
		} catch (SQLException e) {e.printStackTrace();}
	}

	
	public static void createReservationTable(Connection con1){
		Statement stmt;
		try {
			stmt = con1.createStatement();
			stmt.executeUpdate("create table Order(order_number INT PRIMARY KEY, order_date DATE, number_of_guests INT NOT NULL, confirmation_code INT UNIQUE, subscriber_id INT, date_of_placing_order DATE NOT NULL, FOREIGN KEY (subscriber_id) REFERENCES Subscribers(subscriber_id));");
			System.out.println("Created successfuly");
			//stmt.executeUpdate("create table flights(scheduled TIME, flight VARCHAR(8), origin VARCHAR(20), delay VARCHAR(30), terminal INT);");
		} catch (SQLException e) {	e.printStackTrace();
		System.out.println("failed to create tables");
		}
	}	 		
}
