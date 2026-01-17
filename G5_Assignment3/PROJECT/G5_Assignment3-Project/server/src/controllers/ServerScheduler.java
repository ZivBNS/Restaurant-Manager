package controllers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import Data.Reports_Repository;

/**
 * Utility class to manage scheduled background tasks on the server.
 * This class handles periodic maintenance and automated operations, 
 * such as report generation.
 */
public class ServerScheduler {

    /** The executor service responsible for managing the background thread. */
    private static ScheduledExecutorService scheduler;

    /**
     * Starts a service that checks for the need to generate a monthly report.
     * The service runs once every 24 hours.
     * It triggers the repository logic to verify if the previous month's report exists 
     * and generates it if it is missing.
     */
    public static void startReportScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return; 
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if last month's report exists, generate if missing
                    Reports_Repository.checkAndGenerateReport();
                } catch (Exception e) {
                    System.err.println("[Server Scheduler] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 0, 24, TimeUnit.HOURS);
        
        System.out.println("[Server Scheduler] Monthly report service is active.");
    }

    /**
     * Shuts down the scheduling service gracefully.
     * Stops any further execution of scheduled tasks.
     */
    public static void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}