package commands;
import ocsf.server.ConnectionToClient;
import messages.Message;

public interface Command {
    /**
     * Executes the business logic associated with a specific command.
     * @param msg The full message object received from the client.
     * @param client The connection object representing the client.
     * @return A Message object to be sent back as a response, or null if no response is needed.
     */
    Message execute(Message msg, ConnectionToClient client);
}