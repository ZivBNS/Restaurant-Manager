package Reports;

/**
 * Represents daily operational time metrics for the report.
 * This class captures performance data regarding timing, specifically focusing on 
 * how closely customers adhere to their scheduled reservation times.
 */
public class TimeDailyData {
    /** The index of the day within the month (1-31) */
    private int dayIndex; 
    /** The average time (e.g., in minutes) customers arrived after their scheduled start time */
    private double avgLateness;
    /** The average time (e.g., in minutes) customers remained at the table after their scheduled end time */
    private double avgOverstay;

    /**
     * Default constructor for serialization and framework use.
     */
    public TimeDailyData() {}

    /**
     * Constructs a daily time metric record.
     * * @param dayIndex The day of the month (1-31).
     * @param avgLateness Calculated average lateness for the day.
     * @param avgOverstay Calculated average overstay for the day.
     */
    public TimeDailyData(int dayIndex, double avgLateness, double avgOverstay) {
        this.dayIndex = dayIndex;
        this.avgLateness = avgLateness;
        this.avgOverstay = avgOverstay;
    }

    /** @return The day index (1-31) */
    public int getDayIndex() { return dayIndex; }
    
    /** @return The average lateness recorded for this day */
    public double getAvgLateness() { return avgLateness; }
    
    /** @return The average overstay recorded for this day */
    public double getAvgOverstay() { return avgOverstay; }
}