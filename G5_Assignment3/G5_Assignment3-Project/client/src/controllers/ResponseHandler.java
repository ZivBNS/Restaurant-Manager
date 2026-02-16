package controllers;

import messages.Message;

/**
 * Functional interface for handling server responses on the client side.
 * Implementations of this interface contain the specific logic to update the GUI
 * or process data based on the received MessageType.
 */
@FunctionalInterface
public interface ResponseHandler {
    /**
     * Processes the incoming message from the server.
     * @param msg The deserialized message object containing content and type.
     */
    void handle(Message msg);
}