package Reports;
import java.util.List;

/**
 * Container for a full monthly report including both time and subscriber metrics.
 * This class serves as a Data Transfer Object (DTO) to aggregate daily performance 
 * statistics for a specific month and year.
 */
public class MonthlyFullReportData {
    /** The specific month this report covers (1-12) */
    private int month;
    /** The specific year this report covers */
    private int year;
    /** List containing daily timing and duration statistics */
    private List<TimeDailyData> timeDetails;
    /** List containing daily subscriber-related statistics */
    private List<SubscriberDailyData> subscriberDetails;

    /**
     * Default constructor for serialization and framework use.
     */
    public MonthlyFullReportData() {}

    /**
     * Constructs a full monthly report with all required data components.
     * * @param month The month of the report.
     * @param year The year of the report.
     * @param timeDetails A list of daily time-based metrics.
     * @param subscriberDetails A list of daily subscriber-based metrics.
     */
    public MonthlyFullReportData(int month, int year, List<TimeDailyData> timeDetails, List<SubscriberDailyData> subscriberDetails) {
        this.month = month;
        this.year = year;
        this.timeDetails = timeDetails;
        this.subscriberDetails = subscriberDetails;
    }

    /** @return The month represented in this report */
    public int getMonth() { return month; }
    
    /** @return The year represented in this report */
    public int getYear() { return year; }
    
    /** @return The list of daily timing data details */
    public List<TimeDailyData> getTimeDetails() { return timeDetails; }
    
    /** @return The list of daily subscriber activity details */
    public List<SubscriberDailyData> getSubscriberDetails() { return subscriberDetails; }
}