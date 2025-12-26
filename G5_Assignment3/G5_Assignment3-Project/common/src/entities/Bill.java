package entities;


public class Bill {
	private final double UserDiscountRate=0.15;
    private double totalAmount=0.0;
    private double discountRate=0.0;    
    private String status="";
    private String billDetails="";
    private Reservation reservation;
    private int id;
    private int reservationId;
    
    public Bill(Reservation reservation,double totalAmount, String billDetails) {
    	this(reservation);
    	this.totalAmount = totalAmount;
    	this.billDetails=billDetails;

    }
    public Bill(Reservation reservation) {
        this.reservation = reservation;
        if(reservation.getUserId()!=null) discountRate=UserDiscountRate;    
    }    
    
    public Bill(int id, int reservationId, String billDetails, double totalAmount, String status) {
        this.id = id;
        this.reservationId = reservationId;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    
	public int getId() { return id; }
    public int getReservationId() { return reservationId; }

	//discount cal
    public double calculateFinalAmount() {
            return this.totalAmount*(1-discountRate);
    }

    // --- Getters and Setters ---
	public double getTotalAmount() {
		return totalAmount;
	}
	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}
	public double getDiscountRate() {
		return discountRate;
	}
	public void setDiscountRate(double discountRate) {
		this.discountRate = discountRate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getBillDetails() {
		return billDetails;
	}
	public void setBillDetails(String billDetails) {
		this.billDetails = billDetails;
	}
	public Reservation getReservation() {
		return reservation;
	}
	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

    
    
    @Override
    public String toString() {
        return "Bill [Amount: " + calculateFinalAmount() + ",Ordered: "+billDetails +", Status: " + status + "]";
    }
}