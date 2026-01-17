package commands;

import controllers.Server_Controller;
import controllers.User_Controller;
import messages.Message;
import messages.MessageType;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for user-related operations.
 * Updated to support Server-Side Broadcasting.
 */
public class UserCommand implements Command {
    
    private final User_Controller userController;
    private final Server_Controller server; // Reference for broadcasting

    /**
     * Constructor accepting both controllers.
     */
    public UserCommand(User_Controller userController, Server_Controller server) {
        this.userController = userController;
        this.server = server;
    }

    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // 1. Perform the operation (Add/Edit/Delete/Get/Update)
        Message response = userController.handle(msg);

        // 2. Check if a modification occurred successfully
        MessageType type = response.getType();
        boolean isModification = (
            type == MessageType.ADD_USER_RESPONSE_OK ||
            type == MessageType.EDIT_USER_RESPONSE_OK ||
            type == MessageType.DELETE_USER_RESPONSE_OK ||
            type == MessageType.UPDATE_USER_DETAILS_RESPONSE_OK
        );

        if (isModification) {
            // --- BROADCAST ---
            // Fetch the updated list of all users to refresh Admin screens
            Message allUsersMsg = userController.handle(new Message(MessageType.GET_ALL_USERS_REQUEST, null));
            
            // Broadcast the list to ALL clients
            server.broadcastToAllClients(allUsersMsg);
            
            // Note: We still return the original 'response' here (e.g., "User Updated"),
            // unlike OpeningHours where we returned null. 
            // Why? Because the specific user/admin performing the action expects a specific confirmation/feedback 
            // (like "User Added Successfully") separate from the background table refresh.
        }

        return response;
    }
}