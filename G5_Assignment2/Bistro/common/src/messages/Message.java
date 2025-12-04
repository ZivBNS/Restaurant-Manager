package messages;

import java.io.Serializable;


public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L; // Required for Serialization
    private MessageType type;   // The command being requested/sent
    private Object content;     // The data payload (Reservation object, User object, String)

    
    //Full Constructor: Used when a command includes data.
    public Message(MessageType type, Object content) {
        this.type = type;
        this.content = content;
    }

    
    //Command-Only Constructor: Used for simple requests
    public Message(MessageType type) {
        this.type = type;
        this.content = null;
    }

    //Getters & Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message [Type=" + type + ", Content=" + (content != null ? content.getClass().getSimpleName() : "null") + "]";
    }
}