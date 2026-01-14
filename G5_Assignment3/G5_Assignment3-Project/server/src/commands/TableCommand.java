package commands;

import controllers.Server_Controller;
import controllers.Table_Controller;
import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;
import utils.KryoUtil;

/**
 * Command implementation for managing restaurant table operations.
 * Delegates logic to Table_Controller and broadcasts updates to all clients.
 */
public class TableCommand implements Command {

    private Server_Controller server;

    /**
     * Constructor receiving the server instance to enable broadcasting.
     * @param server The main server controller.
     */
    public TableCommand(Server_Controller server) {
        this.server = server;
    }
    
    // Default constructor if needed elsewhere, though prefer using the one above
    public TableCommand() {}

    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // 1. Execute logic
        Message response = Table_Controller.handle(msg);

        // 2. Check for successful modification (Write Operation)
        if (server != null && response != null && response.getType() == MessageType.TABLE_OPERATION_SUCCESS) {
            
            // 3. Create broadcast message
            Message broadcastMsg = new Message(MessageType.TABLE_DATA_UPDATED_BROADCAST, "Refresh Tables");
            
            // 4. Send to ALL connected clients
            server.sendToAllClients(KryoUtil.serialize(broadcastMsg));
        }

        return response;
    }
}