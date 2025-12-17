package entities;

import java.time.LocalDateTime;

public class Reservation {
    private static int confirmationCodeGenerator=100001; 

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

    //constructor for db to use
    public Reservation(int id, Integer userId, Integer tableId, String phone, String email, 
                       LocalDateTime orderStartTime, LocalDateTime orderEndTime, 
                       LocalDateTime actualArrivalTime, LocalDateTime actualDepartureTime, 
                       int numberOfDiners, int confirmationCode, String status, LocalDateTime creationTime) {
        
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
    }

    //constructor to insert data to db
    public Reservation(Integer userId, String phone, String email, 
                       LocalDateTime orderStartTime, LocalDateTime orderEndTime, 
                       int numberOfDiners) {
        
        this.userId = userId;
        this.phone = phone;
        this.email = email;
        this.orderStartTime = orderStartTime;
        this.orderEndTime = orderEndTime;
        this.numberOfDiners = numberOfDiners;
        this.confirmationCode = getConfirmationCodeGenerator();
        
        id = 0;
        status = "Pending";
        tableId = null;
        actualArrivalTime = null;
        actualDepartureTime = null;
        creationTime = LocalDateTime.now(); 
    }

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
    
    @Override
    public String toString() {
    	return "Reservation [Confirmation code: "+confirmationCode+", Order date: "+orderStartTime+", number of diners: "+numberOfDiners+"]";
    }

	public static int getConfirmationCodeGenerator() {
		return confirmationCodeGenerator++;
	}

	public static void setConfirmationCodeGenerator(int confirmationCodeGenerator) {
		Reservation.confirmationCodeGenerator = confirmationCodeGenerator;
	}
}