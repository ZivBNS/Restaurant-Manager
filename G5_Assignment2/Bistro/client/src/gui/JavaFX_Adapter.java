package gui;

import controllers.Client_Controller;
import javafx.application.Platform;

/**
 * Adapter between Customer GUI and Client Controller logic.
 * This class does NOT launch JavaFX. `Client_GUI` does that.
 */
public class JavaFX_Adapter {

    private Customer_GUI customerGUI;
    private Client_Controller controller;

    public JavaFX_Adapter(Client_Controller controller) {
        this.controller = controller;
    }

    /**
     * Attach GUI so adapter can update it later.
     */
    public void attachCustomerGUI(Customer_GUI gui) {
        this.customerGUI = gui;
    }

    /**
     * Called from GUI when Edit Reservation button is clicked.
     * Just forwards to controller logic.
     */
    
    public void requestShowReservations() {
        if (controller != null) {
            controller.onShowReservationsRequest(); // your app logic handles it
        }
    }
    
    public void requestEditReservation() {
        if (controller != null) {
            controller.onEditReservationRequest("test"); // your app logic handles it
        }
    }

    /**
     * Called BY controller to show feedback safely on the UI thread.
     */
    //public void onServerMessage(String message) {
    //    if (customerGUI == null) return;
    //    Platform.runLater(() -> customerGUI.showAdapterMessage(message));
    //}
}