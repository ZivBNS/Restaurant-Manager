package entities;

public class Bill {

    private int id;
    private double totalAmount;
    private double finalAmount;    
    private boolean isPaid;          
    
    private Reservation reservation; //optional

    public Bill(int id, double totalAmount, Reservation reservation) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.reservation = reservation;
        this.isPaid = false;
        
        calculateFinalAmount();
    }
    
    public Bill() {}

    //discount cal
    public void calculateFinalAmount() {
        if (reservation != null && reservation.getCustomer() instanceof Subscribed_Customer) {
            this.finalAmount = this.totalAmount * 0.9;
        } else {
            this.finalAmount = this.totalAmount;
        }
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { 
        this.totalAmount = totalAmount; 
        calculateFinalAmount();
    }

    public double getFinalAmount() { return finalAmount; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    @Override
    public String toString() {
        return "Bill [Amount=" + finalAmount + ", Paid=" + isPaid + "]";
    }
}