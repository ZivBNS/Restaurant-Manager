package Data;

import java.util.List;

import entities.Reservation;

public class Reservation_Repository implements Repository_Interface<Reservation>{
	 private Object today;
	private List<Reservation> reservationsForToday;

	public Reservation_Repository(){
		//get data from db for todays reservations and initialize;
	}		
	
	public Reservation getReservationByTable(int tableNumber) {
		for (Reservation r:reservationsForToday)
			if (r.getAssignedTable().getTableNumber()==tableNumber) return r;
		return null;
	}	

		
	public void setReservation(Reservation reservation) {

	}
	
	public void updateReservation(Reservation reservation) {
		//sql to get reservation by code
		//sql to set the new one
		Reservation oldReservation; 
		if (reservation.getReservationTime().equals(today)) {
			reservationsForToday.remove(oldReservation)
			reservationsForToday.add(reservation);
			
		}
	}
    
    List<Reservation> findAllByDate(Date date){
    	
    }

	@Override
	public Reservation getByCode(String confimrationCode) {
		for (Reservation r:reservationsForToday)
			if (r.getConfirmationCode().equals(confimrationCode)) return r;
		//should ask with sql to get reservation start with this confi code(and prepare statment)
		//should take the data and make it new reservation and return it.
		//otherwise:
		return null;
	}


	@Override
	public boolean deleteByCode(String confimrationCode) {
		
		
		return true;
	}

	@Override
	public boolean set(Reservation objToSet) {
		//sql to set new reservation
		if (objToSet.getReservationTime().equals(today)) reservationsForToday.add(objToSet);
		
		return false;
	}

	@Override
	public boolean updateByCode(Reservation objToUpdate) {
		return false;
	}
	
	public List<Reservation> getReservationsForToday() {
		return reservationsForToday;
	}

	public void setReservationsForToday(List<Reservation> reservationsForToday) {
		this.reservationsForToday = reservationsForToday;
	}

	public static boolean createReservation(Reservation reservation) {
		// TODO Auto-generated method stub
		return false;
	}

	public static Object getReservationsByUser(int userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean cancelReservation(int reservationId) {
		// TODO Auto-generated method stub
		return false;
	}
}
