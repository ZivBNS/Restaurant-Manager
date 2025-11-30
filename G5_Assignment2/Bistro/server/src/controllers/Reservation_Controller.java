package controllers;

import entities.Reservation;  
import entities.Bill;    
import entities.Table; 
import Data.Bill_Repository;           
import Data.Waitlist_Repository;     
import Data.Table_Repository; 
import java.time.LocalDateTime;  
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID; 

public class Reservation_Controller {

    private final Reservation_Repository reservationRepository;
    private final Bill_Repository billRepository;
    private final Waitlist_Repository waitlistRepository; 
    private final Messaging_Controller messagingController;
    private final Table_Repository tableRepository; 

    public Reservation_Controller(
            Reservation_Repository reservationRepository,
            Bill_Repository billRepository,
            Waitlist_Repository waitlistRepository,
            Messaging_Controller messagingController,
            Table_Repository tableRepository) {
        
        this.reservationRepository = reservationRepository;
        this.billRepository = billRepository;
        this.waitlistRepository = waitlistRepository;
        this.messagingController = messagingController;
        this.tableRepository = tableRepository;
    }


    public Reservation createReservation(
            LocalDateTime dateTime, 
            int numberOfDiners, 
            String customerPhone, 
            String customerEmail,
            String subscriberCode) {


        Optional<Table> availableTable = tableRepository.findAvailableTable(dateTime, numberOfDiners);
        
        if (availableTable.isPresent()) {
        	
            Reservation newReservation = new Reservation();
            newReservation.setDateTime(dateTime);
            newReservation.setNumberOfDiners(numberOfDiners);
            newReservation.setCustomerPhone(customerPhone);
            newReservation.setCustomerEmail(customerEmail);
            newReservation.setSubscriberCode(subscriberCode);
            newReservation.setConfirmationCode(UUID.randomUUID().toString().substring(0, 8)); 
            newReservation.setStatus("CONFIRMED");


            Reservation savedReservation = reservationRepository.save(newReservation);
            
            messagingController.sendConfirmation(savedReservation.getCustomerPhone(), savedReservation.getConfirmationCode());

            return savedReservation;
        } else {

            tryMoveToWaitlist(dateTime, numberOfDiners, customerPhone);
            return null; 
        }
    }

    public Optional<Reservation> getReservationByCode(String code) {

        return reservationRepository.findByCode(code);
    }
    
    public List<Reservation> getReservationByDate(LocalDate date) {
        return reservationRepository.findByDate(date);
    }


    public boolean updateReservationDetails(String confirmationCode, LocalDateTime newDateTime, int newNumberOfDiners) {
        Optional<Reservation> reservationOpt = reservationRepository.findByCode(confirmationCode);
        
        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            
            reservation.setDateTime(newDateTime);
            reservation.setNumberOfDiners(newNumberOfDiners);
            reservationRepository.update(reservation);
            
            messagingController.sendUpdateNotification(reservation.getCustomerPhone());
            return true;
        }
        return false;
    }

    public boolean cancelReservation(String code) {
        Optional<Reservation> reservationOpt = reservationRepository.findByCode(code);
        
        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            reservation.setStatus("CANCELED");
            reservationRepository.update(reservation);
            
            messagingController.sendCancellation(reservation.getCustomerPhone());
            return true;
        }
        return false;
    }

    public Bill processCheckIn(String confirmationCode, int tableId) {
        Optional<Reservation> reservationOpt = reservationRepository.findByCode(confirmationCode);
        
        if (reservationOpt.isPresent() && reservationOpt.get().getStatus().equals("CONFIRMED")) {
            Reservation reservation = reservationOpt.get();
            reservation.setStatus("CHECKED_IN");
            reservationRepository.update(reservation);
            
            Bill newBill = new Bill(reservation, tableId);
            billRepository.save(newBill);
            
            return newBill;
        }
        return null;
    }

    public boolean tryMoveToWaitlist(LocalDateTime dateTime, int numberOfDiners, String customerPhone) {
    	
        waitlistRepository.addToWaitlist(dateTime, numberOfDiners, customerPhone);
        messagingController.sendWaitlistConfirmation(customerPhone);
        return true;
    }
    
    public boolean handleNoShow(String code) {
        Optional<Reservation> reservationOpt = reservationRepository.findByCode(code);
        
        if (reservationOpt.isPresent() && !reservationOpt.get().getStatus().equals("CHECKED_IN")) {
            Reservation reservation = reservationOpt.get();
            reservation.setStatus("NO_SHOW");
            reservationRepository.update(reservation);
            return true;
        }
        return false;
    }
    
    public boolean checkAvailability(LocalDateTime time, int diners) {
        return tableRepository.findAvailableTable(time, diners).isPresent();
    }
}
