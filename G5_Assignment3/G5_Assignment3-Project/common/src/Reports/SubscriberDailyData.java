package Reports;
/**
 * Represents daily subscriber activity and order volume for the report.
 * Tracks total orders and waiting list entries.
 */
public class SubscriberDailyData {
    private int dayIndex; // 1-31
    private int totalOrders;
    private int waitingListCount;

    public SubscriberDailyData() {}

    public SubscriberDailyData(int dayIndex, int totalOrders, int waitingListCount) {
        this.dayIndex = dayIndex;
        this.totalOrders = totalOrders;
        this.waitingListCount = waitingListCount;
    }

    public int getDayIndex() { return dayIndex; }
    public int getTotalOrders() { return totalOrders; }
    public int getWaitingListCount() { return waitingListCount; }
}