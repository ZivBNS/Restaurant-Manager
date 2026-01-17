package commands;

import controllers.Reservation_Controller;
import controllers.Server_Controller;
import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;
import utils.KryoUtil;

/**
 * Command implementation for handling restaurant reservation requests.
 * Executes logic and triggers a global broadcast if data is modified.
 */
public class ReservationCommand implements Command {

    private Server_Controller server;

    /**
     * Constructor receiving the server instance to enable broadcasting.
     * @param server The main server controller.
     */
    public ReservationCommand(Server_Controller server) {
        this.server = server;
    }

    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // 1. Execute the logic in the controller
        Message response = Reservation_Controller.handleMessage(msg);

        // 2. Check if the operation was a "Write" operation (Change Data)
        if (response != null && isDataChangeSuccess(response.getType())) {
            
            // 3. Create the broadcast signal
            Message broadcastMsg = new Message(MessageType.RESERVATION_DATA_UPDATED_BROADCAST, "Refresh");
            
            // 4. Send to ALL clients (Broadcasting)
            server.sendToAllClients(KryoUtil.serialize(broadcastMsg));
        }

        return response;
    }

    /**
     * Helper to determine if a message type implies a successful database change.
     */
    private boolean isDataChangeSuccess(MessageType type) {
        return type == MessageType.RESERVATION_CONFIRMED ||
               type == MessageType.INSTANT_RESERVATION_SUCCESS ||
               type == MessageType.RESERVATION_UPDATE_SUCCESS ||
               type == MessageType.RESERVATION_CANCELED ||
               type == MessageType.ADMIN_UPDATE_SUCCESS ||
               type == MessageType.CHECK_IN_COMPLETED ||  // Status change
               type == MessageType.BILL_PAYMENT_SUCCESS;  // Status change
    }
}