package controllers;

import java.util.List;
import Data.Reservation_Repository;
import entities.Reservation;
import integration.EmailService;

/**
 * Background thread monitoring reservations.
 * Handles:
 * 1. Pre-arrival reminders (2 hours before).
 * 2. Departure reminders (Time expired).
 */
public class ServerWatchdog implements Runnable {

    private boolean isRunning;
    private final int CHECK_INTERVAL_MS = 60 * 1000; // 1 Minute
    private final Reservation_Repository reservationRepo;

    public ServerWatchdog() {
        this.reservationRepo = Reservation_Repository.getInstance();
        this.isRunning = false;
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            new Thread(this, "ServerWatchdog").start();
            System.out.println("[Watchdog] Service Started.");
        }
    }

    public void stop() {
        isRunning = false;
        System.out.println("[Watchdog] Service Stopped.");
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                // --- TASK 1: Check for Pre-Arrival Reminders (2 Hours Before) ---
                List<Reservation> upcoming = reservationRepo.getUpcomingReservationsForReminder();
                for (Reservation res : upcoming) {
                    System.out.println("[Watchdog] Sending Pre-Arrival for ID: " + res.getId());
                    EmailService.sendPreArrivalReminder(res);
                    // Mark as reminded immediately to prevent duplicates
                    reservationRepo.markAsReminded(res.getId(), "PRE");
                }

                // --- TASK 2: Check for Departure Reminders (Time Expired) ---
                List<Reservation> expired = reservationRepo.getExpiredActiveReservations();
                for (Reservation res : expired) {
                    System.out.println("[Watchdog] Sending Departure for ID: " + res.getId());
                    EmailService.sendDepartureReminder(res);
                    // Mark as reminded immediately to prevent duplicates
                    reservationRepo.markAsReminded(res.getId(), "DEP");
                }

                Thread.sleep(CHECK_INTERVAL_MS);

            } catch (InterruptedException e) {
                isRunning = false;
            } catch (Exception e) {
                System.err.println("[Watchdog] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}