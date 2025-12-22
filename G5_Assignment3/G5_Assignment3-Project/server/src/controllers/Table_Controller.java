package controllers;

import messages.Message;
import messages.MessageType;
import entities.Reservation;
import entities.Restaurant;
import entities.Restaurant_Table;

import java.time.format.DateTimeFormatter;
import java.util.List;

import Data.Table_Repository;

public class Table_Controller {

	private static Table_Repository repo = Table_Repository.getInstance();

		public static Message handle(Message msg) {
	        switch (msg.getType()) {

	            case ADD_TABLE_REQUEST:
	                Restaurant_Table tAdd = (Restaurant_Table) msg.getContent();
	                if (repo.set(tAdd)) {
	                    repo.init(); // Refresh internal cache
	                    return new Message(MessageType.TABLE_OPERATION_SUCCESS, "Table added successfully.");
	                }
	                return new Message(MessageType.TABLE_OPERATION_FAILED, "Add table failed in database.");

	            case UPDATE_TABLE_REQUEST:
	                Restaurant_Table tUpd = (Restaurant_Table) msg.getContent();
	                
	                // Safety Check: Simulate if the updated table size/status causes conflicts
	                List<Reservation> updateConflicts = repo.findImpactedReservations(tUpd, false);
	                if (!updateConflicts.isEmpty()) {
	                    return new Message(MessageType.TABLE_OPERATION_FAILED, buildConflictString(updateConflicts));
	                }

	                if (repo.update(tUpd)) {
	                    repo.init(); // Refresh internal cache
	                    return new Message(MessageType.TABLE_OPERATION_SUCCESS, "Table updated successfully.");
	                }
	                return new Message(MessageType.TABLE_OPERATION_FAILED, "Update failed in database.");

	            case DELETE_TABLE_REQUEST:
	                int tableNumber = (int) msg.getContent();
	                
	                // Create a simulation object for the 'What-If' analysis
	                Restaurant_Table simTable = new Restaurant_Table(0); // Size is irrelevant for deletion);
	                simTable.setTableNumber(tableNumber);
	                
	                // Step 1: Pre-check for future reservation conflicts
	                List<Reservation> deleteConflicts = repo.findImpactedReservations(simTable, true);
	                
	                if (!deleteConflicts.isEmpty()) {
	                    // Conflict found: Return the list of reservations that would be impacted
	                    return new Message(MessageType.TABLE_OPERATION_FAILED, buildConflictString(deleteConflicts));
	                }

	                // Step 2: Proceed with deletion if safe
	                if (repo.deleteById(tableNumber)) {
	                    repo.init(); // Crucial: reload the tables into the Restaurant singleton cache
	                    return new Message(MessageType.TABLE_OPERATION_SUCCESS, "Table deleted successfully.");
	                }
	                return new Message(MessageType.TABLE_OPERATION_FAILED, "Delete failed: Table number not found.");

	            case GET_ALL_TABLES:
	                return new Message(MessageType.RETURN_ALL_TABLES, Restaurant.getInstance().getTables());

	            default:
	                return null;
	        }
	    }
    
		/**
	     * Formats the list of impacted reservations into a readable string for the Admin.
	     * Custom Format: at HH.mm dd/MM/yyyy
	     * * @param conflicts List of problematic reservations.
	     * @return Formatted error message.
	     */
	    private static String buildConflictString(List<Reservation> conflicts) {
	        StringBuilder sb = new StringBuilder();
	        sb.append("Cannot modify table. The following recent reservations would be left without a seat:\n\n");
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

	        int count = 0;
	        for (Reservation r : conflicts) {
	            String timeStr = (r.getOrderStartTime() != null) ? r.getOrderStartTime().format(formatter) : "Unknown Time";
	            sb.append("- Reservation #").append(r.getConfirmationCode())
	              .append(" (").append(r.getNumberOfDiners()).append(" people) at ")
	              .append(timeStr).append("\n");
	            
	            count++;
	            if (count >= 5) {
	                sb.append("...and ").append(conflicts.size() - 5).append(" more.");
	                break;
	            }
	        }
	        return sb.toString();
	    }
	}


