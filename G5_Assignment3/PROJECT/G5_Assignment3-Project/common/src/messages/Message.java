package messages;

/**
 * Represents a standardized message object for communication between the Client and Server.
 * This class acts as a container that wraps a specific command (MessageType) 
 * and an optional data payload (content).
 */
public class Message {
    
    /** The specific command or action type being requested or sent */
    private MessageType type;
    
    /** The data payload associated with the message (e.g., a Reservation object, a List, or a String) */
    private Object content;

    
    /**
     * Full Constructor: Used when a command requires a data payload.
     * * @param type The MessageType indicating the purpose of the message.
     * @param content The object containing the data to be processed.
     */
    public Message(MessageType type, Object content) {
        this.type = type;
        this.content = content;
    }

    
    /**
     * Command-Only Constructor: Used for simple requests that do not require additional data.
     * * @param type The MessageType indicating the request or notification.
     */
    public Message(MessageType type) {
        this.type = type;
        this.content = null;
    }

    // --- Getters & Setters ---

    /** @return The type/command of this message */
    public MessageType getType() {
        return type;
    }

    /** @param type Set the command type for this message */
    public void setType(MessageType type) {
        this.type = type;
    }

    /** @return The data payload of the message, or null if empty */
    public Object getContent() {
        return content;
    }

    /** @param content Set the data payload for this message */
    public void setContent(Object content) {
        this.content = content;
    }

    /**
     * Returns a string representation of the message, showing the command type 
     * and the simple name of the class contained in the content.
     */
    @Override
    public String toString() {
        return "Message [Type=" + type + ", Content=" + (content != null ? content.getClass().getSimpleName() : "null") + "]";
    }
}