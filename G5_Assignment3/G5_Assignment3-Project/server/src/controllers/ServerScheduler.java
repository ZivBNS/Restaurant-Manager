package controllers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import Data.Reports_Repository;

/**
 * Utility class to manage scheduled background tasks on the server.
 */
public class ServerScheduler {

    private static ScheduledExecutorService scheduler;

    /**
     * Starts a service that checks for the need to generate a monthly report.
     * Runs once every 24 hours.
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
     * Shuts down the scheduling service.
     */
    public static void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}