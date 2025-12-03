package entities;

import java.time.LocalDateTime;

public class Reservation {
	private static int idCreator=0;
	private static int confCodeCreator=100000;

	
	private final int id;
	private Casual_Customer customer;
	private Restaurant_Table assignedTable;
	private LocalDateTime reservationTime;
	private int numDiners;
	private final int confirmationCode;
	private Bill bill;
	private String status;
	private int subscriberId;
	private LocalDateTime dateOfPlacingOrder;

	// new reservation
	public Reservation(Casual_Customer customer, LocalDateTime reservationTime, int numDiners) {
		this.id = idCreator++;
		this.customer = customer;
		this.reservationTime = reservationTime;
		this.numDiners = numDiners;
		this.assignedTable = null;
		this.status = "PENDING";
		this.bill = null;
		this.confirmationCode = confCodeCreator++;
		dateOfPlacingOrder=dateOfPlacingOrder.now();
	}

	// exist reservation
	public Reservation(Casual_Customer customer, Restaurant_Table table, LocalDateTime time, int diners,
			String code ,Bill bill, String status) {
		this.id = idCreator++;
		this.customer = customer;
		this.assignedTable = table;
		this.reservationTime = time;
		this.numDiners = diners;
		this.confirmationCode = confCodeCreator++;
		this.bill= bill;
		this.status = status;
		dateOfPlacingOrder=dateOfPlacingOrder.now();
	}
	//get from db
	public Reservation(int orderId, LocalDateTime orderDate, int diners,
		int confCode , int subId, LocalDateTime dateOfPlacingOrder) {
		this.id = orderId;
		this.reservationTime = orderDate;
		this.numDiners = diners;
		this.confirmationCode = confCode;
		this.subscriberId=subId;
		this.dateOfPlacingOrder=dateOfPlacingOrder;
	}
	
	public Reservation() {
		this.id = idCreator++;
		this.confirmationCode = confCodeCreator++;
	}

	// --- Getters and Setters ---

	public int getId() {
		return id;
	}

	public Casual_Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Casual_Customer customer) {
		this.customer = customer;
	}

	public Restaurant_Table getAssignedTable() {
		return assignedTable;
	}

	public void setAssignedTable(Restaurant_Table assignedTable) {
		this.assignedTable = assignedTable;
	}

	public LocalDateTime getReservationTime() {
		return reservationTime;
	}

	public void setReservationTime(LocalDateTime reservationTime) {
		this.reservationTime = reservationTime;
	}

	public int getNumDiners() {
		return numDiners;
	}

	public void setNumDiners(int numDiners) {
		this.numDiners = numDiners;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Bill getBill() {
		return bill;
	}

	public void setBill(Bill bill) {
		this.bill = bill;
	}
	
	public LocalDateTime getDateOfPlacingOrder() {
		return dateOfPlacingOrder;
	}
	
	public void setSubscriberId(int id) {
		subscriberId=id;
	}

	@Override
	public String toString() {
		return "Reservation [Code=" + confirmationCode + ", Time=" + reservationTime + ", Diners=" + numDiners
				+ ", Status=" + status + "]";
	}

	public int getSubscriberId() {
		return subscriberId;
	}
}