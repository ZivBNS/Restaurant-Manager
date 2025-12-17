package Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import entities.Waitlist;

public class Waitlist_Repository implements Repository_Interface<Waitlist> {
	private DB_Controller db = DB_Controller.getInstance();
    private static Waitlist_Repository waitlistRepositoryInstance = new Waitlist_Repository();
	private List<Waitlist> activeWaitlist = new ArrayList<>();

	private Waitlist_Repository(){
	}

	public static Waitlist_Repository getInstance() {
		return waitlistRepositoryInstance;
	}

	
	@Override
	public void init() {
	    String sql = "SELECT ID, ReservationID, Status, creationTime, TableFreedTime " +
	                 "FROM Waitlist " +
	                 "WHERE TableFreedTime IS NULL AND Status = 'WAITING' " +
	                 "ORDER BY ID ASC";

	    try (Statement stmt = db.getConnection().createStatement();
	         ResultSet rs = stmt.executeQuery(sql)) {

	        while (rs.next()) {
	            int id = rs.getInt("ID");
	            int resId = rs.getInt("ReservationID");
	            String status = rs.getString("Status");
	            
	            LocalDateTime creationTime = rs.getTimestamp("creationTime").toLocalDateTime();
	            
	            LocalDateTime freedTime = null;
	            Timestamp freedTimestamp = rs.getTimestamp("TableFreedTime");
	            if (freedTimestamp != null) {
	                freedTime = freedTimestamp.toLocalDateTime();
	            }

	            Waitlist waitEntry = new Waitlist(id, resId, status, creationTime, freedTime);
	            activeWaitlist.add(waitEntry);

	        }
	        System.out.println("Successfully loaded WAITLIST FOR TODAY into Waitlist_Repository");

	        
	    } catch (SQLException e) {
	        System.err.println("Error fetching active waitlist: " + e.getMessage());
	        e.printStackTrace();
	    }	    
	}
	
	@Override
	public boolean set(Waitlist objToSet) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Waitlist objToUpdate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteById(int confimrationCode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Waitlist getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Waitlist> getWaitlistToday() {
		return activeWaitlist;
	}

	public void setWaitlistToday(List<Waitlist> waitlistToday) {
		this.activeWaitlist = waitlistToday;
	}
	
	
}
