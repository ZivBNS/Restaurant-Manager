package controllers;

import messages.Message;
import messages.MessageType;
import entities.Restaurant;
import entities.Restaurant_Table;
import Data.Table_Repository;

public class Table_Controller {

    private static Table_Repository repo =
        Table_Repository.getInstance();

    public static Message handle(Message msg) {

        switch (msg.getType()) {

        case ADD_TABLE_REQUEST:
            Restaurant_Table tAdd =
                (Restaurant_Table) msg.getContent();
            return repo.set(tAdd)
                ? new Message(MessageType.TABLE_OPERATION_SUCCESS, null)
                : new Message(MessageType.TABLE_OPERATION_FAILED, "Add table failed");

        case UPDATE_TABLE_REQUEST:
            Restaurant_Table tUpd =
                (Restaurant_Table) msg.getContent();
            return repo.update(tUpd)
                ? new Message(MessageType.TABLE_OPERATION_SUCCESS, null)
                : new Message(MessageType.TABLE_OPERATION_FAILED, "Update table failed");

        case GET_ALL_TABLES:
            return new Message(
                MessageType.RETURN_ALL_TABLES,
                Restaurant.getInstance().getTables()
            );

        case DELETE_TABLE_REQUEST:
            int id = (int) msg.getContent();
            return repo.deleteById(id)
                ? new Message(MessageType.TABLE_OPERATION_SUCCESS, null)
                : new Message(MessageType.TABLE_OPERATION_FAILED, "Delete table failed");

        default:
            return null;
        }
    }
}
