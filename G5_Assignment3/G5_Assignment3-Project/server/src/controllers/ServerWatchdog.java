package controllers;

import java.util.List;

import Data.Bill_Repository;
import Data.Reservation_Repository;
import Data.Waitlist_Repository;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Waitlist;
import integration.EmailService;
import messages.Message;

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
    private final Waitlist_Repository waitlistRepo;
    private final Bill_Repository billRepo;

    public ServerWatchdog() {
        this.reservationRepo = Reservation_Repository.getInstance();
        this.waitlistRepo = Waitlist_Repository.getInstance();
        this.billRepo=Bill_Repository.getInstance();
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
                boolean notified=false;//notified=false catch reservations who not reminded yet
                List<Reservation> expired = reservationRepo.getExpiredActiveReservations(notified);
                for (Reservation res : expired) {
                    System.out.println("[Watchdog] Sending Departure for ID: " + res.getId());
                    EmailService.sendDepartureReminder(res);
                    // Mark as reminded immediately to prevent duplicates
                    reservationRepo.markAsReminded(res.getId(), "DEP");
                }
                
                // --- TASK 3: if anyone is in the restaurant for more then 2:15 hours we assume they paid and gone(Max time at restaurant)
                notified=true;//notified=true catch reservations who reminded before 15 minutes
                int billId=-1;
                List<Reservation> expiredMoreThanRevaShaa = reservationRepo.getExpiredActiveReservations(notified);
                for (Reservation res : expiredMoreThanRevaShaa) {
                    System.out.println("[Watchdog] Marking reservation: " + res.getId()+" as completed (15 minutes after notified)");
                    // Mark as reminded immediately to prevent duplicates
                    try { billId=billRepo.getBillByReservationId(res.getId()).getId();
                    } catch (Exception e) {}
                    if (Payment_Controller.handleMessage(new Message(messages.MessageType.BILL_PAYMENT_REQUEST, billId)).getType().equals(messages.MessageType.BILL_PAYMENT_FAILED))
                    	System.err.println("CANNOT MARK THIS RESERVATION AS PAID, CHECK DB FOR MORE INFO");
                }
                
                // --- TASK 4: Check for Notified people in waitlist (Time Expired-more than 15 minutes) ---
                List<Waitlist> notifiedWaitlists = waitlistRepo.getExpiredNotifiedWaitlists(15);
                for (Waitlist w : notifiedWaitlists) {
                    System.out.println("[Watchdog] Waitlist notified for more than 15 minutes, canceling waitlist: " + w.getId());
                    waitlistRepo.cancelWaitlistById(w.getId());
                    int diners = reservationRepo.getById(w.getReservation()).getNumberOfDiners();
                    reservationRepo.deleteById(w.getReservation());
                	Waitlist_Controller.onTableReleased(diners); 
                }
                
                // --- TASK 5: Check for reservations when the people didnt show(Time Expired-more than 15 minutes) ---
                List<Reservation> rToCancelNotArrivedMoreThanRevaShaa = reservationRepo.getNoShowCandidates(15);
                for (Reservation res : rToCancelNotArrivedMoreThanRevaShaa) {
                    System.out.println("[Watchdog] Marking reservation: " + res.getId()+" as canceled (15 minutes late for restaurant)");
                    // Mark as reminded immediately to prevent duplicates
                	res.setStatus(ReservationStatus.CANCELED.toString());
                	reservationRepo.update(res);
                	Waitlist_Controller.onTableReleased(res.getNumberOfDiners()); 

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