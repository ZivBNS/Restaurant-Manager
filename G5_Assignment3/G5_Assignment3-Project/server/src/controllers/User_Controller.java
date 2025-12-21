//package controllers;
//
//import Data.User_Repository;
//import entities.Subscribed_Customer;
//import messages.Message;
//import messages.MessageType;
//
//import java.util.List;
//
//
//public class User_Controller {
//
//
//    private final User_Repository repo;
//
//    public User_Controller(User_Repository repo) {
//        this.repo = repo;
//    }
//
//    public Message handle(Object client, Message msg) {
//        try {
//            return switch (msg.getType()) {
//
//                case MessageType.GET_ALL_USERS_REQUEST -> handleGetAll();
//
//                case MessageType.ADD_USER -> handleAdd((Subscribed_Customer) msg.getObject());
//
//                case MessageType.UPDATE_USER -> handleUpdate((Subscribed_Customer) msg.getObject());
//
//                case MessageType.REMOVE_USER -> handleDelete((String) msg.getObject()); 
//                // e.g. delete by username (or id)
//
//                default -> new Message(MessageType.USERS_ERROR, "Unknown user action: " + msg.getType());
//            };
//        } catch (ClassCastException e) {
//            // happens if the client sent the wrong object type
//            return new Message(MessageType.USERS_ERROR, "Bad request (wrong data type).");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new Message(MessageType.USERS_ERROR, "Server error.");
//        }
//    }
//
//    private Message handleGetAll() {
//        List<Subscribed_Customer> users = repo.getAllSubscribedCustomers();
//        return new Message(MessageType.GET_ALL_USERS_RESPONSE, users);
//    }
//
//    private Message handleAdd(Subscribed_Customer u) {
//        String err = validateForAdd(u);
//        if (err != null) return new Message(MessageType.USERS_ERROR, err);
//
//        // uniqueness checks (example)
//        if (repo.existsByUsername(u.getUsername())) {
//            return new Message(MsgType.USERS_ERROR, "Username already exists.");
//        }
//        if (repo.existsByEmailOrPhone(u.getEmail(), u.getPhone())) {
//            return new Message(MsgType.USERS_ERROR, "Email or phone already exists.");
//        }
//
//        boolean ok = repo.insertSubscribedCustomer(u);
//        return ok
//            ? new Message(MessageType.USERS_OK, "User added.")
//            : new Message(MessageType.USERS_ERROR, "Failed to add user.");
//    }
//
//    private Message handleUpdate(Subscribed_Customer u) {
//        String err = validateForUpdate(u);
//        if (err != null) return new Message(MsgType.USERS_ERROR, err);
//
//        // If you allow changing username/email/phone, check collisions
//        // You likely need an ID or "originalUsername" to do this properly.
//        // For now, assume username identifies the row.
//        boolean ok = repo.updateSubscribedCustomer(u);
//        return ok
//            ? new Message(MessageType.USERS_OK, "User updated.")
//            : new Message(MessageType.USERS_ERROR, "Failed to update user.");
//    }
//
//    private Message handleDelete(String username) {
//        if (username == null || username.trim().isEmpty()) {
//            return new Message(MsgType.USERS_ERROR, "Username is required.");
//        }
//
//        boolean ok = repo.deleteByUsername(username.trim());
//        return ok
//            ? new Message(MsgType.USERS_OK, "User deleted.")
//            : new Message(MsgType.USERS_ERROR, "Failed to delete user.");
//    }
//
//    // ---------- Validation ----------
//
//    private String validateForAdd(Subscribed_Customer u) {
//        if (u == null) return "Missing user data.";
//
//        if (isBlank(u.getFirstName()) || isBlank(u.getLastName())) return "First/last name required.";
//        if (isBlank(u.getUsername())) return "Username required.";
//        if (isBlank(u.getPassword())) return "Password required.";
//        if (isBlank(u.getEmail()) && isBlank(u.getPhone())) return "Email or phone required.";
//
//        return null;
//    }
//
//    private String validateForUpdate(Subscribed_Customer u) {
//        if (u == null) return "Missing user data.";
//
//        if (isBlank(u.getUsername())) return "Username required.";
//        // password might be optional on update (only update if provided)
//        if (isBlank(u.getEmail()) && isBlank(u.getPhone())) return "Email or phone required.";
//
//        return null;
//    }
//
//    private boolean isBlank(String s) {
//        return s == null || s.trim().isEmpty();
//    }
//}