package Data;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;

import entities.Reservation;
//this class uses single-tone
public class Reservation_Repository implements Repository_Interface<Reservation>{
	private DB_Controller db = DB_Controller.getInstance();
    private static Reservation_Repository ReservationRepositoryInstance = new Reservation_Repository();

	private Reservation_Repository(){
	}

	public static Reservation_Repository getInstance() {
		return ReservationRepositoryInstance;
	}
	
	
	@Override    //search id in table order, returns reservation from db if exist, otherwise null
	public Reservation getById(int id) {
		String sqlGet = "SELECT * FROM `Order` WHERE order_number = " + id;
		return this.getOneOrderFromDb(sqlGet, id, "Id");
	}
	
	
				//search confirmation code in table order, returns reservation from db if exist, otherwise null
	public Reservation getByCode(int confimrationCode) {
		String sqlGet = "SELECT * FROM `Order` WHERE confirmation_code = " + confimrationCode;
		return this.getOneOrderFromDb(sqlGet, confimrationCode, "Confirmation code");
	}

	
				//search user id in table order, returns all reservations from db if exist as list, otherwise null
	public List<Reservation> getByUserId(int userId) {
		String sqlGet = "SELECT * FROM `Order` WHERE subscriber_id = " + userId;
		List<Reservation> lst = getListOfOrdersFromDb(sqlGet, userId, "Subscriber ID");	
		if (lst.isEmpty()) {
			System.out.println("the Reservations with subscriber_id: "+ userId + " is not found!!");
			return null;
		}
		return lst;
	}

				//these 3 functions are helping get orders from db.
				//1st get sql statement and returns one reservation
	private Reservation getOneOrderFromDb(String sqlGet,int key,String typeOfKey) {
		List<Reservation> reservations = getListOfOrdersFromDb(sqlGet, key, typeOfKey);
		if (reservations.isEmpty()) {
			System.out.println("the Reservation with " + typeOfKey +": " + key + " is not found!!");
			return null;
		}
		return reservations.get(0);
	}
	//2st get sql statement and returns list of reservations
	private List<Reservation> getListOfOrdersFromDb(String sqlGet,int key,String typeOfKey) {
		Statement stmt;
		ResultSet rs;
		try {
			stmt = db.getConnection().createStatement();
	        rs = stmt.executeQuery(sqlGet);
	        return orderTranslator(rs);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("has problem getting data from DB");
			return null;
		}
	}
	//3rd
	private List<Reservation> orderTranslator(ResultSet rs) throws SQLException {
		ArrayList<Reservation> reservations = new ArrayList<Reservation>();
		while (rs.next()) {
			int orderNumber = rs.getInt("order_number");
        	Date orderDate = rs.getDate("order_date");
        	int numOfDiners = rs.getInt("number_of_guests");
        	int confCode = rs.getInt("confirmation_code");
        	int subscriberId = rs.getInt("subscriber_id");
        	Date dateOfPlacingOrder = rs.getDate("date_of_placing_order");
        	reservations.add(new Reservation(orderNumber,orderDate.toLocalDate().atStartOfDay() ,
        		numOfDiners,confCode,subscriberId,dateOfPlacingOrder.toLocalDate().atStartOfDay()));
		}
		return reservations;
	}
	
	
	
	
	    /*set new reservation in db, if there is order with
		same id returns false, else set the order and return true*/
	@Override     
	public boolean set(Reservation objToSet) {
		String sqlSet = "INSERT INTO `order` (order_number, order_date, number_of_guests,"
				+ " confirmation_code, subscriber_id, date_of_placing_order) VALUES (?, ?, ?, ?, ?, ?)";
        
        try {
    		PreparedStatement pstmt = db.getConnection().prepareStatement(sqlSet);			
        	pstmt.setInt(1, objToSet.getId());
            pstmt.setDate(2, Date.valueOf(objToSet.getReservationTime().toLocalDate()));
            pstmt.setInt(3, objToSet.getNumDiners());
            pstmt.setInt(4, objToSet.getConfirmationCode());
            pstmt.setInt(5, objToSet.getSubscriberId());
            pstmt.setDate(6,Date.valueOf(objToSet.getDateOfPlacingOrder().toLocalDate()));
            pstmt.executeUpdate();
            return true;
		} catch (SQLException e) {
			System.out.println("failed to set reservation in db!");
			e.printStackTrace();
		}	
		return false;
	}

	
				//search by confirmation code, delete order from db if found, return true if succeed else false
	public boolean deleteByCode(int confirmationCode) {	
		String sqlDelete = "DELETE FROM `Order` WHERE confirmation_code = " + confirmationCode;
		return this.deleteFromDb(sqlDelete, confirmationCode, "Confirmation code");
	}
	
		//search by order id, delete order from db if found, return true if succeed else false
	public boolean deleteById(int id) {	
		String sqlDelete = "DELETE FROM `Order` WHERE order_number = " + id;
		return this.deleteFromDb(sqlDelete, id, "ID");
	}
		
				//function to help delete order from db
	private boolean deleteFromDb(String sqlDelete,int key,String typeOfKey) {
		try {
			Statement st=db.getConnection().createStatement();
			
	        if (st.executeUpdate(sqlDelete)>0) { //track program by prints
	            System.out.println("Order with "+typeOfKey+": "+key +" was deleted successfully!");
	            return true;
	        }	
	        System.out.println("No order found with "+ typeOfKey+": " + key);
		}
	    catch (SQLException e) {
	    	e.printStackTrace();
	    }
	    return false;
	}
	
	//function to update exsisting order. gets reservation and return true if updated successfully,
	//if not exsist ((or nothing updated)) returns false 
	public boolean update(Reservation objToUpdate) {
	    String sql = "UPDATE `Order` SET number_of_guests = ?, order_date = ? WHERE order_number = ?";

	    try {
	    	PreparedStatement stmt = db.getConnection().prepareStatement(sql);
	        stmt.setInt(1, objToUpdate.getNumDiners());
	        stmt.setDate(2, Date.valueOf(objToUpdate.getReservationTime().toLocalDate()));
	        stmt.setInt(3, objToUpdate.getId());

	        int changed = stmt.executeUpdate();
	        System.out.println("Rows updated: " + changed);

	        if (changed>0) return true; 
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to update order!!");
		}
	
		return false;
		
	}
	
	
	

	
	
	
	
	
	
	
	
/*	
	//testing
	public static void main(String[] args) {		
		Reservation_Repository rr = new Reservation_Repository();
		Reservation res0=new Reservation(null, LocalDateTime.of(2025, 12, 6, 8, 40) , 4);
		Reservation res1=new Reservation(null, LocalDateTime.now(), 7);
		Reservation res2=new Reservation(null, LocalDateTime.now(), 8);
		Reservation res3=new Reservation(null, LocalDateTime.now(), 10);
		Reservation res4=new Reservation(null, LocalDateTime.now(), 12);
		Reservation res5=new Reservation(null, LocalDateTime.now(), 12);
		Reservation res6=new Reservation(null, LocalDateTime.now(), 12);
		res6.setReservationTime(LocalDateTime.of(2025, 12, 12, 8, 40));
		res6.setNumDiners(101);
		if (rr.update(res6)==false) System.out.println("FAILED");;
		rr.update(res1);
		rr.update(res1);
		rr.update(res1);
				rr.set(res0);
		rr.set(res1);
		rr.set(res2);
		rr.set(res3);
		rr.set(res4);
		Reservation r1 = rr.getById(0);
		Reservation s1 = rr.getById(1);
		Reservation t1 = rr.getByCode(100002);
		Reservation d1 = rr.getByCode(100001);

		System.out.println("TRY1: "+ r1);
		System.out.println("TRY2: "+ s1);
		System.out.println("TRY3: "+ t1);
		System.out.println("TRY4: "+ d1);
		
		t1.setSubscriberId(9);
		t1.setNumDiners(1);
		rr.update(t1);
		System.out.println("TRY5: "+ t1);
	}*/
}






