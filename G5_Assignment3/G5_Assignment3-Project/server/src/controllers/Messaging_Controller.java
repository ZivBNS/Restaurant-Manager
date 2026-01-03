package controllers;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import Data.*;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Waitlist;
/*
 *	ניסיתי פה ליצור טיימר שיעבוד כל הזמן, כרגע הוא לא מופעל 
 *  יש פה פונקציה שאמורה לעזור ביצירת רשימת המתנה, אז לא למחוק בבקשה 
 * 
 * 
 * */


public class Messaging_Controller {
    
    // גישה לריפוזיטורים (Singleton)
    private static final Reservation_Repository reservationRepository = Reservation_Repository.getInstance();
    private static final Waitlist_Repository waitlistRepository = Waitlist_Repository.getInstance(); 

    // מנהל התהליכונים
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // קבועים
    private static final int MAX_DINING_HOURS = 2;       
    private static final int NO_SHOW_GRACE_MINUTES = 15; 

    public static void startBackgroundTasks() {
        System.out.println("[Messaging Service] Starting background tasks...");
        scheduler.scheduleAtFixedRate(Messaging_Controller::checkDiningLimit, 0, 30, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(Messaging_Controller::checkNoShows, 0, 15, TimeUnit.MINUTES);
        // scheduler.scheduleAtFixedRate(Messaging_Controller::sendPreArrivalReminders, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * משימה 1: בדיקת חריגה בזמן ישיבה
     */
    private static void checkDiningLimit() {
        try {
            List<Reservation> overstayers = reservationRepository.getOverstayingReservations(MAX_DINING_HOURS);
            for (Reservation res : overstayers) {
                System.out.println("-> [TIMER] ALERT: Table associated with ResID " + res.getId() + " has exceeded 2 hours.");
                System.out.println("->");
                //להוסיף קריאה להבאת נתוני הביל
                // כאן יש להוסיף לוגיקה לשליחת הודעה לצוות ולמזמין
            }
        } catch (Exception e) {
            System.err.println("[TIMER] Error in checkDiningLimit: " + e.getMessage());
        }
    }

    /**
     * משימה 2: בדיקת אי-הגעה (No-Show)
     * מטפל בשני מקרים: 
     * 1. מזמינים רגילים שלא הגיעו בזמן.
     * 2. אנשי רשימת המתנה שקיבלו הודעה (Notified) ועבר הזמן שלהם להגיע.
     */
    private static void checkNoShows() {
        try {
            // --- חלק א': הזמנות רגילות (Pending) ---
            List<Reservation> noShows = reservationRepository.getNoShowCandidates(NO_SHOW_GRACE_MINUTES);
            
            for (Reservation res : noShows) {
                System.out.println("-> [TIMER] Auto-Canceling Reservation " + res.getId() + " due to No-Show.");
                reservationRepository.updateStatusByID(res.getId(), ReservationStatus.CANCELED);
            }

            // --- חלק ב': אנשי ווייטליסט (Notified -> Expired) ---
            // נדרשת פונקציה ב-Waitlist_Repository: getExpiredNotifiedCustomers(int minutes)
            List<Waitlist> expiredWaitlist = waitlistRepository.getExpiredNotifiedCustomers(NO_SHOW_GRACE_MINUTES);
            
            for (Waitlist w : expiredWaitlist) {
                System.out.println("->[TIMER] Waitlist Expired for WaitlistID " + w.getId());
                
                // 1. עדכון סטטוס הווייטליסט ל-EXPIRED
                // נדרשת פונקציה: updateStatus(int waitlistId, String status)
                waitlistRepository.updateStatusByReservationId(w.getReservation(), "CANCELED");
                
                // 2. ביטול ההזמנה המקושרת ("הפלייס-הולדר")
                reservationRepository.updateStatusByID(w.getReservation(), ReservationStatus.CANCELED);
            }
            
        } catch (Exception e) {
            System.err.println("Error in checkNoShows: " + e.getMessage());
        }
    }

    // ====================================================================
    //              TRIGGER: Table Released (Payment)
    // ====================================================================

    
    public static void stopAllTasks() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}