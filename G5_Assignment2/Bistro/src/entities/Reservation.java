package entities;

import java.time.LocalDateTime;

public class Reservation {

	private int id;
	private Casual_Customer customer;
	private Restaurant_Table assignedTable;
	private LocalDateTime reservationTime;
	private int numDiners;
	private String confirmationCode;
	private Bill bill;
	private String status;

	// new reservation
	public Reservation(int id, Casual_Customer customer, LocalDateTime reservationTime, int numDiners) {
		this.id = id;
		this.customer = customer;
		this.reservationTime = reservationTime;
		this.numDiners = numDiners;
		this.assignedTable = null;
		this.status = "PENDING";
		this.bill = null;
		this.confirmationCode = generateConfirmationCode();
	}

	// exist reservation
	public Reservation(int id, Casual_Customer customer, Restaurant_Table table, LocalDateTime time, int diners,
			String code ,Bill bill, String status) {
		this.id = id;
		this.customer = customer;
		this.assignedTable = table;
		this.reservationTime = time;
		this.numDiners = diners;
		this.confirmationCode = code;
		this.bill= bill;
		this.status = status;
	}

	public Reservation() {
	}

	// --- לוגיקה עסקית ---

	// מייצר קוד אישור פשוט (למשל: RES-105)
	private String generateConfirmationCode() {
		return "RES-" + this.id + "-" + (int) (Math.random() * 1000);
	}

	// --- Getters and Setters ---

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
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

	@Override
	public String toString() {
		return "Reservation [Code=" + confirmationCode + ", Time=" + reservationTime + ", Diners=" + numDiners
				+ ", Status=" + status + "]";
	}
}