package Data;

import java.util.List;

import entities.Reservation;

public class Reservation_Repository {
	 private Object today;
	private List<Reservation> reservationsForToday;

	public Reservation_Repository(){
		//get data from db for todays reservations and initialize;
	}		
	
	public Reservation getReservationByConfirmationCode(String confCode) {
		for (Reservation r:reservationsForToday)
			if (r.getConfirmationCode().equals(confCode)) return r;
		//should ask with sql to get reservation start with this confi code(and prepare statment)
		//should take the data and make it new reservation and return it.
		//otherwise:
		return null;
	}
	
	public Reservation getReservationByTable(int tableNumber) {
		for (Reservation r:reservationsForToday)
			if (r.getAssignedTable().getTableNumber()==tableNumber) return r;
		return null;
	}	

		
	public void setReservation(Reservation reservation) {
		//sql to set new reservation
		if (reservation.getReservationTime().equals(today)) reservationsForToday.add(reservation);
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

    void deleteById(String id);

	public List<Reservation> getReservationsForToday() {
		return reservationsForToday;
	}

	public void setReservationsForToday(List<Reservation> reservationsForToday) {
		this.reservationsForToday = reservationsForToday;
	}
}
