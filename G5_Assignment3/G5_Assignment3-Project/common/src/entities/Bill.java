package entities;

/**
 * Represents a bill associated with a restaurant reservation.
 * This class handles the pricing, discounts, and payment status of an order.
 */
public class Bill {
    /** The default discount rate for registered users (10%) */
	private final double UserDiscountRate=0.1;
    
    /** The total amount of the bill before discounts */
	private double totalAmount=0.0;
    /** The discount percentage to be applied to the bill */
    private double discountRate=0.0;    
    /** The current status of the bill (e.g., Paid, Pending) */
    private String status="";
    /** Detailed description of the items ordered */
    private String billDetails;
    /** The reservation object associated with this bill */
    private Reservation reservation;
    /** Unique identifier for the bill in the database */
    private int id;
    /** The ID of the associated reservation */
    private int reservationId;
    
    /**
     * Constructs a Bill with reservation details and initial total amount.
     * * @param reservation The reservation associated with the bill.
     * @param totalAmount The initial total price.
     * @param billDetails Detailed description of the order.
     */
    public Bill(Reservation reservation,double totalAmount, String billDetails) {
    	this(reservation);
    	this.totalAmount = totalAmount;
    	this.billDetails=billDetails;
    }
    
    /**
     * Constructs a Bill based on a reservation and determines if a user discount applies.
     * * @param reservation The reservation associated with the bill.
     */
    public Bill(Reservation reservation) {
        this.reservation = reservation;
        // Apply discount if the reservation is linked to a registered user
        if(reservation.getUserId()!=null) discountRate=UserDiscountRate;    
    }

    /**
     * Constructor used to set a new bill related to a new reservation in the database.
     * * @param reservationId The unique ID of the reservation.
     * @param isSubscriber Whether the customer is a subscriber eligible for a discount.
     */
    public Bill(int reservationId , boolean isSubscriber) {
        this.reservationId = reservationId;
        if (isSubscriber) discountRate=UserDiscountRate;    
    }    
    
    /**
     * Full constructor for retrieving an existing bill record from the database.
     * * @param id The bill ID.
     * @param reservationId The associated reservation ID.
     * @param billDetails The description of the order.
     * @param totalAmount The total price.
     * @param status The payment status.
     * @param discountRate The discount rate applied.
     */
    public Bill(int id, int reservationId, String billDetails, double totalAmount, String status, double discountRate) {
        this.id = id;
        this.reservationId = reservationId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.billDetails = billDetails;
        this.discountRate = discountRate; 
    }
    
    /** @return The bill's unique ID */
	public int getId() { return id; }
	/** @return The ID of the reservation linked to this bill */
    public int getReservationId() { return reservationId; }

	/**
     * Calculates the final amount to be paid after applying the discount rate.
     * * @return The final total after discount.
     */
    public double calculateFinalAmount() {
        return this.totalAmount * (1.0 - (discountRate));
    }

    // --- Getters and Setters ---
    
    /** @return The raw total amount */
	public double getTotalAmount() {
		return totalAmount;
	}
	/** @param totalAmount Set the raw total amount */
	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}
	/** @return The current discount rate */
	public double getDiscountRate() {
		return discountRate;
	}
	/** @param discountRate Set the discount rate */
	public void setDiscountRate(double discountRate) {
		this.discountRate = discountRate;
	}
	/** @return The current status of the bill */
	public String getStatus() {
		return status;
	}
	/** @param status Set the status of the bill */
	public void setStatus(String status) {
		this.status = status;
	}
	/** @return The string details of the order */
	public String getBillDetails() {
		return billDetails;
	}
	/** @param billDetails Set the details of the order */
	public void setBillDetails(String billDetails) {
		this.billDetails = billDetails;
	}
	/** @return The Reservation object associated with this bill */
	public Reservation getReservation() {
		return reservation;
	}
	/** @param reservation Set the Reservation object */
	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

    /**
     * Returns a string representation of the bill, including the final amount and status.
     */
    @Override
    public String toString() {
        return "Bill [Amount: " + calculateFinalAmount() + ",Ordered: "+billDetails +", Status: " + status + "]";
    }
}