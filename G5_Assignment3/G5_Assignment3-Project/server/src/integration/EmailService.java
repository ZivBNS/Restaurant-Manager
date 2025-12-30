package integration;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import entities.Reservation;

/**
 * Service class for all email communications.
 * Centralizes messaging logic for confirmations and departure reminders.
 */

//username: bistro5dodz@gmail.com
//password: Aa123456789!

public class EmailService {

    private static final String HOST = "smtp.gmail.com";
    private static final String PORT = "587";
    private static final String FROM_EMAIL = "bistro5dodz@gmail.com"; 
    private static final String APP_PASSWORD = "hmry uukw yrqw fxbu";

    /**
     * Sends a polite departure reminder when the dining time slot has ended.
     * Centralizes the professional phrasing within the service.
     * @param res The Reservation entity for the customer being notified.
     */
    public static void sendDepartureReminder(final Reservation res) {
        if (res == null || res.getEmail() == null || res.getEmail().isEmpty()) return;

        Thread emailThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(EmailService.class.getClassLoader());

                try {
                    Session session = createSession();
                    MimeMessage mimeMessage = new MimeMessage(session);
                    
                    mimeMessage.setFrom(new InternetAddress(FROM_EMAIL, "Bistro Restaurant"));
                    mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(res.getEmail()));
                    
                    mimeMessage.setSubject("We Hope You Enjoyed Your Visit at Bistro!");

                    String content = "Hello,\n\n" +
                                     "We truly hope you enjoyed your dining experience with us today.\n\n" +
                                     "We would like to kindly remind you that your table's reserved time window " +
                                     "has now concluded. To help us welcome the next guests waiting to dine, " +
                                     "we would appreciate it if you could kindly settle your bill at your convenience.\n\n" +
                                     "Thank you for your understanding and for choosing Bistro. We look forward to seeing you again soon!\n\n" +
                                     "Warm regards,\n" +
                                     "The Bistro Team";
                                     
                    mimeMessage.setText(content);
                    Transport.send(mimeMessage);
                    
                    System.out.println("EmailService: Departure reminder sent to " + res.getEmail());

                } catch (Exception e) {
                    System.err.println("EmailService Error (Reminder): " + e.getMessage());
                }
            }
        });
        emailThread.start();
    }

	/**
	 * Sends a reservation confirmation email to the customer.
	 * 
	 * @param res The Reservation entity containing details for the confirmation.
	 */
    public static void sendConfirmationEmail(final Reservation res) {
        if (res == null || res.getEmail() == null || res.getEmail().isEmpty()) return;

        Thread emailThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(EmailService.class.getClassLoader());
                try {
                    Session session = createSession();
                    MimeMessage mimeMessage = new MimeMessage(session);
                    mimeMessage.setFrom(new InternetAddress(FROM_EMAIL, "Bistro Restaurant"));
                    mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(res.getEmail()));
                    mimeMessage.setSubject("Reservation Confirmed! Code: " + res.getConfirmationCode());

                    String content = "Hello,\n\n" +
                                     "Your reservation is confirmed!\n\n" +
                                     "Details:\n" +
                                     "- Date: " + res.getFormattedDate() + "\n" +
                                     "- Time: " + res.getFormattedTime() + "\n" +
                                     "- Guests: " + res.getNumberOfDiners() + "\n" +
                                     "- Code: " + res.getConfirmationCode() + "\n\n" +
                                     "See you soon!";
                                     
                    mimeMessage.setText(content);
                    Transport.send(mimeMessage);
                } catch (Exception e) {
                    System.err.println("EmailService Error (Confirmation): " + e.getMessage());
                }
            }
        });
        emailThread.start();
    }
    /**
     * Sends a reminder email 2 hours before the reservation starts.
     * @param res The reservation details.
     */
    public static void sendPreArrivalReminder(final Reservation res) {
        if (res == null || res.getEmail() == null || res.getEmail().isEmpty()) return;

        Thread emailThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(EmailService.class.getClassLoader());
                try {
                    Session session = createSession(); // Use the helper method we made earlier
                    MimeMessage message = new MimeMessage(session);
                    
                    message.setFrom(new InternetAddress(FROM_EMAIL, "Bistro Restaurant"));
                    message.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(res.getEmail()));
                    message.setSubject("Upcoming Reservation Reminder - See you soon!");

                    String content = "Hello,\n\n" +
                                     "Just a friendly reminder that your table at Bistro is reserved for today at " + 
                                     res.getFormattedTime() + ".\n\n" +
                                     "We are getting everything ready for your arrival.\n" +
                                     "If you need to make any changes, please let us know.\n\n" +
                                     "See you in 2 hours!\n" +
                                     "The Bistro Team";
                                     
                    message.setText(content);
                    Transport.send(message);
                    System.out.println("EmailService: Pre-Arrival reminder sent to " + res.getEmail());

                } catch (Exception e) {
                    System.err.println("EmailService Error: " + e.getMessage());
                }
            }
        });
        emailThread.start();
    }
    /** Helper to centralize SMTP configuration. */
    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);
        props.put("mail.smtp.ssl.trust", HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }
}