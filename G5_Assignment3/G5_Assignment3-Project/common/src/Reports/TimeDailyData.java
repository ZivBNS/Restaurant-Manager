package Reports;

/**
 * Represents daily operational time metrics for the report.
 * Includes arrival lateness and departure overstay averages.
 */
public class TimeDailyData {
    private int dayIndex; // 1-31
    private double avgLateness;
    private double avgOverstay;

    public TimeDailyData() {}

    public TimeDailyData(int dayIndex, double avgLateness, double avgOverstay) {
        this.dayIndex = dayIndex;
        this.avgLateness = avgLateness;
        this.avgOverstay = avgOverstay;
    }

    public int getDayIndex() { return dayIndex; }
    public double getAvgLateness() { return avgLateness; }
    public double getAvgOverstay() { return avgOverstay; }
}
