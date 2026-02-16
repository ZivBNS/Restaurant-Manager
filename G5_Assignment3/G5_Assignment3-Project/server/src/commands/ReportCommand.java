package commands;

import controllers.Report_Controller;
import messages.Message;
import ocsf.server.ConnectionToClient;

/**
 * Command implementation for handling monthly report requests.
 * Uses the Report_Controller to fetch data from the repository.
 */
public class ReportCommand implements Command {

	/**
	 * Executes the report command.
	 * Performs a direct call to the business logic controller to handle the report message.
	 * * @param msg    The message containing the report request.
	 * @param client The connection from the client.
	 * @return A Message object with the report data or response.
	 */
    @Override
    public Message execute(Message msg, ConnectionToClient client) {
        // Direct call to the business logic controller
        return Report_Controller.handleReportMessage(msg);
    }
}