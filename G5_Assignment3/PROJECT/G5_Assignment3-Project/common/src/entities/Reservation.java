package entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a table reservation in the restaurant system.
 * This class stores all data related to a booking, including customer contact info,
 * scheduled times, actual arrival/departure tracking, and status.
 */
public class Reservation {

    /** Unique identifier for the reservation */
	private int id;
    /** The ID of the user who made the reservation (can be null for guests) */
	private Integer userId;
    /** The ID of the table assigned to this reservation */
	private Integer tableId;
    /** Customer's contact phone number */
	private String phone;
    /** Customer's email address for notifications and billing */
	private String email;
    /** Scheduled start time for the reservation */
	private LocalDateTime orderStartTime;
    /** Scheduled end time for the reservation */
	private LocalDateTime orderEndTime;
    /** The actual time the customer arrived at the restaurant */
	private LocalDateTime actualArrivalTime;
    /** The actual time the customer left the restaurant */
	private LocalDateTime actualDepartureTime;
    /** Number of people attending the reservation */
	private int numberOfDiners;
    /** Security/Verification code for the reservation */
	private int confirmationCode;
    /** Current status (e.g., Pending, Confirmed, Cancelled, Completed) */
	private String status;
    /** The timestamp when the reservation was first created */
	private LocalDateTime creationTime;
    /** Flag indicating if a pre-arrival reminder was sent */
	private boolean remindedPreArrival;
    /** Flag indicating if a departure/thank-you reminder was sent */
	private boolean remindedDeparture;
    /** The billing information associated with this reservation */
	private Bill bill;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public Reservation() {
	}

	/**
	 * Constructor for retrieving existing reservations from the database.
     * * @param id The reservation ID.
     * @param userId The ID of the user.
     * @param tableId The assigned table ID.
     * @param phone Contact phone.
     * @param email Contact email.
     * @param orderStartTime Scheduled start.
     * @param orderEndTime Scheduled end.
     * @param actualArrivalTime Arrival timestamp.
     * @param actualDepartureTime Departure timestamp.
     * @param numberOfDiners Guest count.
     * @param confirmationCode Verification code.
     * @param status Current status string.
     * @param creationTime When the record was created.
     * @param remindedPreArrival Reminder status.
     * @param remindedDeparture Departure reminder status.
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
	 * Constructor used by the Client to create a new reservation request. 
     * Note: The confirmationCode is initialized to 0 and will be assigned by the Server.
     * * @param userId User identifier.
     * @param phone Phone number.
     * @param email Email address.
     * @param orderStartTime Requested start time.
     * @param orderEndTime Requested end time.
     * @param numberOfDiners Number of guests.
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

    /**
     * Simplified constructor for basic reservation tracking or list displays.
     */
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

    /** @return The unique reservation ID */
	public int getId() {
		return id;
	}

    /** @param id Set the unique reservation ID */
	public void setId(int id) {
		this.id = id;
	}

    /** @return The ID of the user */
	public Integer getUserId() {
		return userId;
	}

    /** @param userId Set the user ID */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

    /** @return The assigned table ID */
	public Integer getTableId() {
		return tableId;
	}

    /** @param tableId Set the assigned table ID */
	public void setTableId(Integer tableId) {
		this.tableId = tableId;
	}

    /** @return Contact phone number */
	public String getPhone() {
		return phone;
	}

    /** @param phone Set the contact phone number */
	public void setPhone(String phone) {
		this.phone = phone;
	}

    /** @return Contact email address */
	public String getEmail() {
		return email;
	}

    /** @param email Set the contact email address */
	public void setEmail(String email) {
		this.email = email;
	}

    /** @return The scheduled start time */
	public LocalDateTime getOrderStartTime() {
		return orderStartTime;
	}

    /** @param orderStartTime Set the scheduled start time */
	public void setOrderStartTime(LocalDateTime orderStartTime) {
		this.orderStartTime = orderStartTime;
	}

    /** @return The scheduled end time */
	public LocalDateTime getOrderEndTime() {
		return orderEndTime;
	}

    /** @param orderEndTime Set the scheduled end time */
	public void setOrderEndTime(LocalDateTime orderEndTime) {
		this.orderEndTime = orderEndTime;
	}

    /** @return The timestamp of actual arrival */
	public LocalDateTime getActualArrivalTime() {
		return actualArrivalTime;
	}

    /** @param actualArrivalTime Set the timestamp of actual arrival */
	public void setActualArrivalTime(LocalDateTime actualArrivalTime) {
		this.actualArrivalTime = actualArrivalTime;
	}

    /** @return The timestamp of actual departure */
	public LocalDateTime getActualDepartureTime() {
		return actualDepartureTime;
	}

    /** @param actualDepartureTime Set the timestamp of actual departure */
	public void setActualDepartureTime(LocalDateTime actualDepartureTime) {
		this.actualDepartureTime = actualDepartureTime;
	}

    /** @return The number of diners */
	public int getNumberOfDiners() {
		return numberOfDiners;
	}

    /** @param numberOfDiners Set the number of diners */
	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

    /** @return The reservation confirmation code */
	public int getConfirmationCode() {
		return confirmationCode;
	}

    /** @param confirmationCode Set the reservation confirmation code */
	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

    /** @return The current status of the reservation */
	public String getStatus() {
		return status;
	}

    /** @param status Set the status of the reservation */
	public void setStatus(String status) {
		this.status = status;
	}

    /** @return The timestamp of when the reservation was created */
	public LocalDateTime getCreationTime() {
		return creationTime;
	}

    /** @param creationTime Set the creation timestamp */
	public void setCreationTime(LocalDateTime creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * Formats the reservation date to dd.MM.yyyy string format. 
	 * * @return A string representing the date in dd.MM.yyyy format.
	 */
	public String getFormattedDate() {
		if (orderStartTime == null)
			return "";

		// Define the desired pattern: dd.MM.yyyy
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		// Format the LocalDateTime object
		return orderStartTime.format(dateFormatter);
	}

	/**
     * Extracts the time from the scheduled start time.
     * * @return A string formatted as HH:mm.
     */
	public String getFormattedTime() {
		if (orderStartTime == null)
			return "";
		// Formats to HH:mm
		int hour = orderStartTime.getHour();
		int minute = orderStartTime.getMinute();
		return String.format("%02d:%02d", hour, minute);
	}

    /** @return True if pre-arrival reminder was sent */
	public boolean isRemindedPreArrival() {
		return remindedPreArrival;
	}

    /** @param remindedPreArrival Set pre-arrival reminder status */
	public void setRemindedPreArrival(boolean remindedPreArrival) {
		this.remindedPreArrival = remindedPreArrival;
	}

    /** @return True if departure reminder was sent */
	public boolean isRemindedDeparture() {
		return remindedDeparture;
	}

    /** @param remindedDeparture Set departure reminder status */
	public void setRemindedDeparture(boolean remindedDeparture) {
		this.remindedDeparture = remindedDeparture;
	}

    /** @return The Bill associated with this reservation */
	public Bill getBill() {
        return bill;
    }

    /** @param bill Set the Bill for this reservation */
    public void setBill(Bill bill) {
        this.bill = bill;
    }

    /**
     * Returns a detailed string representation of the reservation object.
     */
	@Override
	public String toString() {
		return "Reservation [id=" + id + ", userId=" + userId + ", tableId=" + tableId + ", phone=" + phone + ", email="
				+ email + ", orderStartTime=" + orderStartTime + ", orderEndTime=" + orderEndTime
				+ ", actualArrivalTime=" + actualArrivalTime + ", actualDepartureTime=" + actualDepartureTime
				+ ", numberOfDiners=" + numberOfDiners + ", confirmationCode=" + confirmationCode + ", status=" + status
				+ ", creationTime=" + creationTime + "]";
	}

}