package Reports;

/**
 * Represents daily subscriber activity and order volume for the report.
 * This class tracks metrics such as the total number of orders and the number of 
 * entries in the waiting list for a specific day of the month.
 */
public class SubscriberDailyData {
    /** The index of the day within the month (ranging from 1 to 31) */
    private int dayIndex; 
    /** The total number of orders completed or placed on this specific day */
    private int totalOrders;
    /** The count of customers who were added to the waiting list on this day */
    private int waitingListCount;

    /**
     * Default constructor for serialization and framework use.
     */
    public SubscriberDailyData() {}

    /**
     * Constructs a daily data record with specific activity metrics.
     * * @param dayIndex The day of the month (1-31).
     * @param totalOrders Total count of orders.
     * @param waitingListCount Total count of waitlist entries.
     */
    public SubscriberDailyData(int dayIndex, int totalOrders, int waitingListCount) {
        this.dayIndex = dayIndex;
        this.totalOrders = totalOrders;
        this.waitingListCount = waitingListCount;
    }

    /** @return The day index (1-31) */
    public int getDayIndex() { return dayIndex; }
    
    /** @return The total number of orders for the day */
    public int getTotalOrders() { return totalOrders; }
    
    /** @return The number of people who joined the waitlist */
    public int getWaitingListCount() { return waitingListCount; }
}