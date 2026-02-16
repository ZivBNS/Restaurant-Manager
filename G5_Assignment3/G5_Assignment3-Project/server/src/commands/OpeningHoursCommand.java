package commands;

import controllers.OpeningHours_Controller;
import controllers.Server_Controller;
import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for handling requests related to the restaurant's opening hours.
 * This class delegates the message processing to the OpeningHours_Controller.
 * <p>Updated to support Server-Side Broadcasting on successful updates.</p>
 */
public class OpeningHoursCommand implements Command {
    
    private Server_Controller server;

    /**
     * Constructor receiving the Server_Controller to enable broadcasting.
     * @param server The main server controller instance.
     */
    public OpeningHoursCommand(Server_Controller server) {
        this.server = server;
    }

    /**
     * Executes the opening hours command.
     * If a modification (Update/Add/Delete) is successful, it broadcasts the new
     * schedule to all connected clients to trigger a real-time refresh.
     * * @param msg    The message containing the opening hours request.
     * @param client The connection to the client that sent the request.
     * @return A response message for the specific client (GET/Error), or null if broadcasted.
     */
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // 1. Delegate logic to the static Controller to get the result/data
        Message response = OpeningHours_Controller.handle(msg);

        // 2. Check if we need to Broadcast:
        // Condition A: The request was NOT a simple "Get" (it was a change request)
        // Condition B: The operation succeeded (response type contains the new data)
        if (msg.getType() != MessageType.GET_OPENING_HOURS && 
            response.getType() == MessageType.RETURN_OPENING_HOURS) {
            
            // --- BROADCAST TO ALL ---
            // Send the updated Opening_Hours object to ALL clients (including the sender).
            // This ensures everyone sees the change instantly.
            server.broadcastToAllClients(response);
            
            // Return null because 'broadcastToAllClients' already handled the response.
            // Returning a message here would cause the sender to receive it twice.
            return null; 
        }

        // 3. For standard GET requests or Errors, just return the response to the sender
        return response;
    }
}