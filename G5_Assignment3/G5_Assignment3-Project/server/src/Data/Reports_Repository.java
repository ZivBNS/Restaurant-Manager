package Data;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import Reports.MonthlyFullReportData;
import Reports.SubscriberDailyData;
import Reports.TimeDailyData;

/**
 * Data Access Object (DAO) for handling Report-related database operations.
 * This class manages the retrieval, existence checks, and automated generation 
 * of monthly reports. It has been updated to calculate net average values, 
 * which include negative values for early arrivals and early departures.
 */
public class Reports_Repository {

    /**
     * Retrieves a complete monthly report from the database by aggregating 
     * data from management, time details, and subscriber details tables.
     * * @param month The month to retrieve (1-12).
     * @param year  The year to retrieve (e.g., 2026).
     * @return A MonthlyFullReportData object containing all daily stats, or null if the report does not exist.
     */
    public static MonthlyFullReportData getMonthlyReport(int month, int year) {
        int reportId = -1;
        List<TimeDailyData> timeList = new ArrayList<>();
        List<SubscriberDailyData> subList = new ArrayList<>();

        try (Connection conn = DB_Controller.getInstance().getConnection().getConnection()) {
            
            String idQuery = "SELECT report_id FROM reports_management WHERE report_month = ? AND report_year = ?";
            try (PreparedStatement psId = conn.prepareStatement(idQuery)) {
                psId.setInt(1, month);
                psId.setInt(2, year);
                try (ResultSet rsId = psId.executeQuery()) {
                    if (rsId.next()) {
                        reportId = rsId.getInt("report_id");
                    } else {
                        return null;
                    }
                }
            }

            String timeQuery = "SELECT day_index, avg_lateness, avg_overstay FROM time_report_details " +
                               "WHERE report_id = ? ORDER BY day_index ASC";
            try (PreparedStatement psTime = conn.prepareStatement(timeQuery)) {
                psTime.setInt(1, reportId);
                try (ResultSet rsTime = psTime.executeQuery()) {
                    while (rsTime.next()) {
                        timeList.add(new TimeDailyData(
                            rsTime.getInt("day_index"),
                            rsTime.getDouble("avg_lateness"),
                            rsTime.getDouble("avg_overstay")
                        ));
                    }
                }
            }

            String subQuery = "SELECT day_index, total_orders, waiting_list_count FROM subscriber_report_details " +
                              "WHERE report_id = ? ORDER BY day_index ASC";
            try (PreparedStatement psSub = conn.prepareStatement(subQuery)) {
                psSub.setInt(1, reportId);
                try (ResultSet rsSub = psSub.executeQuery()) {
                    while (rsSub.next()) {
                        subList.add(new SubscriberDailyData(
                            rsSub.getInt("day_index"),
                            rsSub.getInt("total_orders"),
                            rsSub.getInt("waiting_list_count")
                        ));
                    }
                }
            }

            return new MonthlyFullReportData(month, year, timeList, subList);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks whether a report for the specified month and year already exists in the system.
     * * @param month The month to check.
     * @param year  The year to check.
     * @return true if the report entry exists, false otherwise.
     */
    public static boolean isReportExists(int month, int year) {
        String sql = "SELECT report_id FROM reports_management WHERE report_month = ? AND report_year = ?";
        try (Connection con = DB_Controller.getInstance().getConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            return ps.executeQuery().next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Periodically checks if the report for the previous month has been generated.
     * If the report is missing, it triggers the generation process.
     */
    public static void checkAndGenerateReport() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonthDate = today.minusMonths(1);
        int targetMonth = lastMonthDate.getMonthValue();
        int targetYear = lastMonthDate.getYear();

        if (isReportExists(targetMonth, targetYear)) {
            System.out.println("[Reports Repository] Report for " + targetMonth + "/" + targetYear + " already exists.");
            return;
        }

        System.out.println("[Reports Repository] Generating report for " + targetMonth + "/" + targetYear + "...");
        generateReport(targetMonth, targetYear);
    }

    /**
     * Generates the report by aggregating raw reservation and waitlist data.
     * The calculation includes negative values for lateness and overstaying 
     * (representing early arrivals or early departures) to provide a net average.
     * This method uses a transaction to ensure data integrity across multiple tables.
     * * @param month The month for which to generate the report.
     * @param year  The year for which to generate the report.
     */
    private static void generateReport(int month, int year) {
        Connection con = null;
        try {
            con = DB_Controller.getInstance().getConnection().getConnection();
            con.setAutoCommit(false); 

            String sqlMgmt = "INSERT INTO reports_management (report_month, report_year) VALUES (?, ?)";
            PreparedStatement psMgmt = con.prepareStatement(sqlMgmt, Statement.RETURN_GENERATED_KEYS);
            psMgmt.setInt(1, month);
            psMgmt.setInt(2, year);
            psMgmt.executeUpdate();

            ResultSet rsKeys = psMgmt.getGeneratedKeys();
            if (!rsKeys.next()) {
                con.rollback();
                return;
            }
            int reportId = rsKeys.getInt(1);

            YearMonth ym = YearMonth.of(year, month);
            int daysInMonth = ym.lengthOfMonth();

            String sqlTime = "INSERT INTO time_report_details (report_id, day_index, avg_lateness, avg_overstay) VALUES (?, ?, ?, ?)";
            String sqlSub = "INSERT INTO subscriber_report_details (report_id, day_index, total_orders, waiting_list_count) VALUES (?, ?, ?, ?)";
            
            PreparedStatement psTime = con.prepareStatement(sqlTime);
            PreparedStatement psSub = con.prepareStatement(sqlSub);

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = LocalDate.of(year, month, day);
                
                // UPDATED QUERY: Removed CASE WHEN to allow negative values in AVG calculation
                String statsSql = 
                    "SELECT " +
                    "AVG(TIMESTAMPDIFF(MINUTE, ReservationStartTime, ActualArrivalTime)) as avg_late, " +
                    "AVG(TIMESTAMPDIFF(MINUTE, ReservationEndTime, ActualDepartureTime)) as avg_over, " +
                    "COUNT(*) as total_orders " +
                    "FROM reservations " +
                    "WHERE DATE(ReservationStartTime) = ? AND Status = 'Completed'";
                
                PreparedStatement psDailyStats = con.prepareStatement(statsSql);
                psDailyStats.setDate(1, java.sql.Date.valueOf(currentDate));
                ResultSet rsStats = psDailyStats.executeQuery();
                
                double avgLate = 0;
                double avgOver = 0;
                int totalOrders = 0;
                
                if (rsStats.next()) {
                    avgLate = rsStats.getDouble("avg_late");
                    avgOver = rsStats.getDouble("avg_over");
                    totalOrders = rsStats.getInt("total_orders");
                }
                rsStats.close();
                psDailyStats.close();

                String waitSql = "SELECT COUNT(*) FROM waitlist WHERE DATE(creationTime) = ?";
                PreparedStatement psWait = con.prepareStatement(waitSql);
                psWait.setDate(1, java.sql.Date.valueOf(currentDate));
                ResultSet rsWait = psWait.executeQuery();
                int waitCount = 0;
                if (rsWait.next()) waitCount = rsWait.getInt(1);
                
                rsWait.close();
                psWait.close();

                psTime.setInt(1, reportId);
                psTime.setInt(2, day);
                psTime.setDouble(3, avgLate);
                psTime.setDouble(4, avgOver);
                psTime.addBatch();

                psSub.setInt(1, reportId);
                psSub.setInt(2, day);
                psSub.setInt(3, totalOrders);
                psSub.setInt(4, waitCount);
                psSub.addBatch();
            }

            psTime.executeBatch();
            psSub.executeBatch();

            con.commit(); 
            System.out.println("[Reports Repository] Report generated successfully (Net Average) for " + month + "/" + year);

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (Exception ex) {}
        }
    }
}