package Reports;
import java.util.List;

/**
 * Container for a full monthly report including both time and subscriber metrics.
 */
public class MonthlyFullReportData {
    private int month;
    private int year;
    private List<TimeDailyData> timeDetails;
    private List<SubscriberDailyData> subscriberDetails;

    public MonthlyFullReportData() {}

    public MonthlyFullReportData(int month, int year, List<TimeDailyData> timeDetails, List<SubscriberDailyData> subscriberDetails) {
        this.month = month;
        this.year = year;
        this.timeDetails = timeDetails;
        this.subscriberDetails = subscriberDetails;
    }

    public int getMonth() { return month; }
    public int getYear() { return year; }
    public List<TimeDailyData> getTimeDetails() { return timeDetails; }
    public List<SubscriberDailyData> getSubscriberDetails() { return subscriberDetails; }
}
