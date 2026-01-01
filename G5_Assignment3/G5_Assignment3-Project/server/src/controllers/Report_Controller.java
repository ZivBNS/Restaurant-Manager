package controllers;

import Data.Reports_Repository;
import Reports.MonthlyFullReportData;
import messages.Message;
import messages.MessageType;

/**
 * Server-side handler for all report-related operations.
 * This class routes different report requests to their respective logic.
 */
public class Report_Controller {

    /**
     * Central message handler for report operations.
     * @param msg The incoming message from the client.
     * @return A response message to be sent back to the client.
     */
    public static Message handleReportMessage(Message msg) {
        switch (msg.getType()) {
            
            case GET_MONTHLY_REPORT:
                return getMonthlyReport(msg.getContent());
                
            /* כאן תוכל להוסיף מקרים נוספים בעתיד, למשל:
               case GENERATE_NEW_REPORT:
                   return generateReport(msg.getContent());
               case DELETE_REPORT:
                   return deleteReport(msg.getContent());
            */

            default:
                return new Message(MessageType.REPORT_ERROR, "Unknown report operation.");
        }
    }

    /**
     * Internal logic for fetching a specific monthly report.
     */
    private static Message getMonthlyReport(Object content) {
        try {
            int[] params = (int[]) content;
            int month = params[0];
            int year = params[1];

            // Fetch compiled data from the repository
            MonthlyFullReportData reportData = Reports_Repository.getMonthlyReport(month, year);

            if (reportData != null) {
                return new Message(MessageType.REPORT_DATA_SUCCESS, reportData);
            } else {
                return new Message(MessageType.REPORT_ERROR, "No report generated for " + month + "/" + year);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Message(MessageType.REPORT_ERROR, "Server Error: Failed to retrieve report data.");
        }
    }
}