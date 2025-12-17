package Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import entities.Opening_Hours;
import entities.Restaurant;

public class OpeningHours_Repository implements Repository_Interface<Opening_Hours> {
	private DB_Controller db = DB_Controller.getInstance();
    private static OpeningHours_Repository OpeningHoursInstance = new OpeningHours_Repository();

	private OpeningHours_Repository(){
	}

	public static OpeningHours_Repository getInstance() {
		return OpeningHoursInstance;
	}
	
	
	@Override
	public void init() {
		Opening_Hours oh = new Opening_Hours();
		String sqlRegular = "SELECT DayOfWeek, OpenTime, CloseTime FROM OpeningHours";
	    try (Statement stmt = db.getConnection().createStatement();
	         ResultSet rs = stmt.executeQuery(sqlRegular)) {
	        
	        while (rs.next()) {
	            DayOfWeek day = DayOfWeek.valueOf(rs.getString("DayOfWeek").toUpperCase());
	            LocalTime open = rs.getTime("OpenTime").toLocalTime();
	            LocalTime close = rs.getTime("CloseTime").toLocalTime();
	            oh.setRegularHour(day, open, close);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    String sqlSpecial = "SELECT Date, OpenTime, CloseTime FROM SpecialHours";
	    try (Statement stmt = db.getConnection().createStatement();
	         ResultSet rs = stmt.executeQuery(sqlSpecial)) {
	        
	        while (rs.next()) {
	            LocalDate date = rs.getDate("Date").toLocalDate();
	            if (rs.getTime("OpenTime") != null) {
	                LocalTime open = rs.getTime("OpenTime").toLocalTime();
	                LocalTime close = rs.getTime("CloseTime").toLocalTime();
	                oh.setException(date, open, close);
	            } else {
	                oh.setException(date, null, null); // סגור
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    Restaurant.getInstance().setOpeningHours(oh);
        System.out.println("Successfully loaded OPENING HOURS into RESTAURANT-Opening hours");
        System.out.println(oh.toString());
	}
	
	@Override
	public boolean set(Opening_Hours objToSet) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Opening_Hours objToUpdate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteById(int confimrationCode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Opening_Hours getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
