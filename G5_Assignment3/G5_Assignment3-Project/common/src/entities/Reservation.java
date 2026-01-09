package entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Reservation {

	private int id;
	private Integer userId;
	private Integer tableId;
	private String phone;
	private String email;
	private LocalDateTime orderStartTime;
	private LocalDateTime orderEndTime;
	private LocalDateTime actualArrivalTime;
	private LocalDateTime actualDepartureTime;
	private int numberOfDiners;
	private int confirmationCode;
	private String status;
	private LocalDateTime creationTime;
	private boolean remindedPreArrival;
	private boolean remindedDeparture;
	private Bill bill;
	/**
	 * Default constructor for serialization frameworks.
	 */
	public Reservation() {
	}

	/**
	 * Constructor for retrieving existing reservations from the database.
	 */
	public Reservation(int id, Integer userId, Integer tableId, String phone, String email,
			LocalDateTime orderStartTime, LocalDateTime orderEndTime, LocalDateTime actualArrivalTime,
			LocalDateTime actualDepartureTime, int numberOfDiners, int confirmationCode, String status,
			LocalDateTime creationTime, boolean remindedPreArrival, boolean remindedDeparture) {

		this.id = id;
		this.userId = userId;
		this.tableId = tableId;
		this.phone = phone;
		this.email = email;
		this.orderStartTime = orderStartTime;
		this.orderEndTime = orderEndTime;
		this.actualArrivalTime = actualArrivalTime;
		this.actualDepartureTime = actualDepartureTime;
		this.numberOfDiners = numberOfDiners;
		this.confirmationCode = confirmationCode;
		this.status = status;
		this.creationTime = creationTime;
		this.remindedPreArrival = remindedPreArrival;
		this.remindedDeparture = remindedDeparture;
	}

	/**
	 * Constructor used by the Client to create a new reservation request. Note: The
	 * confirmationCode is initialized to 0 and will be assigned by the Server.
	 */
	public Reservation(Integer userId, String phone, String email, LocalDateTime orderStartTime,
			LocalDateTime orderEndTime, int numberOfDiners) {

		this.userId = userId;
		this.phone = phone;
		this.email = email;
		this.orderStartTime = orderStartTime;
		this.orderEndTime = orderEndTime;
		this.numberOfDiners = numberOfDiners;

		// Initial defaults before server processing
		this.id = 0;
		this.status = "Pending";
		this.confirmationCode = 0;
		this.tableId = null;
		this.actualArrivalTime = null;
		this.actualDepartureTime = null;
		this.creationTime = LocalDateTime.now();
	}

	public Reservation(int id, String phone, LocalDateTime startTime, int numberOfDiners, Integer tableId,
			String status) {
		this.id = id;
		this.phone = phone;
		this.orderStartTime = startTime;
		this.numberOfDiners = numberOfDiners;
		this.tableId = tableId;
		this.status = status;
	}

	// --- Getters and Setters ---

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getTableId() {
		return tableId;
	}

	public void setTableId(Integer tableId) {
		this.tableId = tableId;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getOrderStartTime() {
		return orderStartTime;
	}

	public void setOrderStartTime(LocalDateTime orderStartTime) {
		this.orderStartTime = orderStartTime;
	}

	public LocalDateTime getOrderEndTime() {
		return orderEndTime;
	}

	public void setOrderEndTime(LocalDateTime orderEndTime) {
		this.orderEndTime = orderEndTime;
	}

	public LocalDateTime getActualArrivalTime() {
		return actualArrivalTime;
	}

	public void setActualArrivalTime(LocalDateTime actualArrivalTime) {
		this.actualArrivalTime = actualArrivalTime;
	}

	public LocalDateTime getActualDepartureTime() {
		return actualDepartureTime;
	}

	public void setActualDepartureTime(LocalDateTime actualDepartureTime) {
		this.actualDepartureTime = actualDepartureTime;
	}

	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(LocalDateTime creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * Formats the reservation date to dd.MM.yyyy string format. Uses the
	 * ReservationStartTime column data.
	 * 
	 * @return A string representing the date in dd.MM.yyyy format.
	 */
	public String getFormattedDate() {
		if (orderStartTime == null)
			return "";

		// Define the desired pattern: dd.MM.yyyy
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		// Format the LocalDateTime object
		return orderStartTime.format(dateFormatter);
	}

	
	public String getFormattedTime() {
		if (orderStartTime == null)
			return "";
		// Formats to HH:mm
		int hour = orderStartTime.getHour();
		int minute = orderStartTime.getMinute();
		return String.format("%02d:%02d", hour, minute);
	}

	public boolean isRemindedPreArrival() {
		return remindedPreArrival;
	}

	public void setRemindedPreArrival(boolean remindedPreArrival) {
		this.remindedPreArrival = remindedPreArrival;
	}

	public boolean isRemindedDeparture() {
		return remindedDeparture;
	}

	public void setRemindedDeparture(boolean remindedDeparture) {
		this.remindedDeparture = remindedDeparture;
	}
	public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }
	@Override
	public String toString() {
		return "Reservation [id=" + id + ", userId=" + userId + ", tableId=" + tableId + ", phone=" + phone + ", email="
				+ email + ", orderStartTime=" + orderStartTime + ", orderEndTime=" + orderEndTime
				+ ", actualArrivalTime=" + actualArrivalTime + ", actualDepartureTime=" + actualDepartureTime
				+ ", numberOfDiners=" + numberOfDiners + ", confirmationCode=" + confirmationCode + ", status=" + status
				+ ", creationTime=" + creationTime + "]";
	}

}