package Data;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import entities.Reservation;
import entities.ReservationStatus;

/**
 * Repository class for managing Reservation entities in the database.
 * Supports "Logical Seating" simulation where TableID remains NULL until check-in.
 * Handles confirmation code generation, lifecycle status updates, and reminder tracking.
 */
public class Reservation_Repository {

	private DB_Controller db = DB_Controller.getInstance();
	private static Reservation_Repository reservationRepositoryInstance = new Reservation_Repository();
	private static int confirmationCodeGenerator = 100000;

	private Reservation_Repository() {
	}

	/**
	 * Retrieves the singleton instance of the Reservation_Repository.
	 * @return The active Reservation_Repository instance.
	 */
	public static Reservation_Repository getInstance() {
		return reservationRepositoryInstance;
	}

	/**
	 * Fetches a reservation by its unique confirmation code.
	 * @param code The unique confirmation code assigned to the reservation.
	 * @return A populated Reservation object, or null if no match is found.
	 */
	public Reservation getByConfirmationCode(int code) {
		String sql = "SELECT * FROM reservations WHERE ConfirmationCode = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, code);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next())
						return extractReservationFromResultSet(rs);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return null;
	}

	/**
	 * Synchronizes the internal confirmation code generator with the database.
	 * Finds the current maximum confirmation code and sets the generator to the next value.
	 */
	public void init() { /* Same logic using Pool and MAX(ConfirmationCode) */
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			String query = "SELECT MAX(ConfirmationCode) FROM reservations";
			try (Statement stmt = pConn.getConnection().createStatement(); ResultSet rs = stmt.executeQuery(query)) {
				if (rs.next())
					confirmationCodeGenerator = Math.max(100000, rs.getInt(1) + 1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Generates the next unique confirmation code in a thread-safe manner.
	 * @return A unique integer confirmation code.
	 */
	public synchronized int getNextConfirmationCode() {
		return confirmationCodeGenerator++;
	}

	/**
	 * Saves a new reservation to the database. 
	 * Implements "Logical Seating" by explicitly setting TableID to NULL upon creation.
	 * Initializes reminder flags (Pre-Arrival and Departure) to false.
	 * @param res The Reservation object to persist.
	 * @return true if the insertion was successful, false otherwise.
	 */
	public boolean set(Reservation res) {
		String sql = "INSERT INTO reservations (UserID, TableID, Phone, Email, ReservationStartTime, "
				+ "ReservationEndTime, NumberOfDiners, ConfirmationCode, Status, CreationTime, "
				+ "RemindedPreArrival, RemindedDeparture) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				if (res.getUserId() != null)
					pstmt.setInt(1, res.getUserId());
				else
					pstmt.setNull(1, java.sql.Types.INTEGER);

				pstmt.setNull(2, java.sql.Types.INTEGER);

				pstmt.setString(3, res.getPhone());
				pstmt.setString(4, res.getEmail());
				pstmt.setTimestamp(5, Timestamp.valueOf(res.getOrderStartTime()));
				pstmt.setTimestamp(6, Timestamp.valueOf(res.getOrderEndTime()));
				pstmt.setInt(7, res.getNumberOfDiners());
				pstmt.setInt(8, res.getConfirmationCode());
				pstmt.setString(9, res.getStatus());
				pstmt.setTimestamp(10, Timestamp.valueOf(res.getCreationTime()));
				pstmt.setBoolean(11, false);
				pstmt.setBoolean(12, false);

				return pstmt.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			System.err.println("[Reservation_Repository] Insert Error: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Updates the basic details of an existing reservation.
	 * @param res The Reservation object containing updated information.
	 * @return true if the update affected at least one row, false otherwise.
	 */
	public boolean update(Reservation res) {
		String sql = "UPDATE reservations SET NumberOfDiners = ?, ReservationStartTime = ?, "
				+ "ReservationEndTime = ?, Status = ? WHERE ID = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, res.getNumberOfDiners());
				pstmt.setTimestamp(2, Timestamp.valueOf(res.getOrderStartTime()));
				pstmt.setTimestamp(3, Timestamp.valueOf(res.getOrderEndTime()));
				pstmt.setString(4, res.getStatus());
				pstmt.setInt(5, res.getId());
				return pstmt.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			return false;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}
	
	/**
	 * Updates a reservation record during the check-in process.
	 * Assigns a specific TableID and records the actual arrival time.
	 * @param confCode The confirmation code for the reservation.
	 * @param TableId  The ID of the table assigned to the guest.
	 * @param rs       The new reservation status (e.g., Active).
	 * @return true if all updates were successful.
	 */
	public boolean updateReservationForCheckIn(int confCode, int TableId, ReservationStatus rs) {

		if(!updateStatusByConfirmationCode(confCode, rs)) {
			System.out.println("R_R------>>>>>> cannot change status during check in");
			return false;
		} 
		String sql = "UPDATE reservations SET TableID = ?, ActualArrivalTime = ? WHERE ConfirmationCode = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, TableId);
				pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
				pstmt.setInt(3, confCode);

				int affectedRows = pstmt.executeUpdate();
				return affectedRows > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null) {
				db.releaseConnection(pConn);
			}
		}
	}

	/**
	 * Marks a reservation as completed and records the actual departure time.
	 * @param confCode          The unique confirmation code.
	 * @param actualFinishTime  The timestamp when the guest left.
	 * @return true if the record was updated.
	 */
	public boolean updateReservationForCheckOut(int confCode, LocalDateTime actualFinishTime) {
		String sql = "UPDATE reservations SET Status = '"+ ReservationStatus.COMPLETED.toString()+"', ActualDepartureTime = ? WHERE ConfirmationCode = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(actualFinishTime));
				pstmt.setInt(2, confCode);

				return pstmt.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null) {
				db.releaseConnection(pConn);
			}
		}
	}

	/**
	 * Performs a comprehensive update of a reservation by an employee.
	 * Allows manual assignment of tables and contact information changes.
	 * @param res The Reservation object with updated staff-managed fields.
	 * @return true if the update was successful.
	 */
	public boolean updateByEmployee(Reservation res) {
		String sql = "UPDATE reservations SET NumberOfDiners = ?, ReservationStartTime = ?, "
				+ "ReservationEndTime = ?, Status = ?, TableID = ?, Phone = ?, Email = ? WHERE ID = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, res.getNumberOfDiners());
				pstmt.setTimestamp(2, Timestamp.valueOf(res.getOrderStartTime()));
				pstmt.setTimestamp(3, Timestamp.valueOf(res.getOrderEndTime()));
				pstmt.setString(4, res.getStatus());
				if (res.getTableId() != null)
					pstmt.setInt(5, res.getTableId());
				else
					pstmt.setNull(5, java.sql.Types.INTEGER);
				pstmt.setString(6, res.getPhone());
				pstmt.setString(7, res.getEmail());
				pstmt.setInt(8, res.getId());
				return pstmt.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			return false;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Retrieves all reservations associated with a specific subscriber ID that are
	 * currently 'Pending' or 'Active'.
	 * @param userId The unique subscriber ID.
	 * @return A list of filtered reservations for the customer.
	 */
	public List<Reservation> getByUserId(int userId) {
		List<Reservation> results = new ArrayList<Reservation>();
		// Updated SQL to filter only relevant statuses for the customer view
		String sql = "SELECT * FROM reservations WHERE UserID = ? AND Status IN ('Pending', 'Active')";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, userId);
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						results.add(extractReservationFromResultSet(rs));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return results;
	}

	/**
	 * Retrieves all reservations for a casual customer by phone or email that are
	 * currently 'Pending' or 'Active'.
	 * @param contact The phone number or email string used for the search.
	 * @return A list of filtered reservations for the casual customer.
	 */
	public List<Reservation> getByContactInfo(String contact) {
		List<Reservation> results = new ArrayList<Reservation>();
		// Updated SQL to filter only relevant statuses for the customer view
		String sql = "SELECT * FROM reservations WHERE (Phone = ? OR Email = ?) AND Status IN ('Pending', 'Active')";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setString(1, contact);
				pstmt.setString(2, contact);
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						results.add(extractReservationFromResultSet(rs));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return results;
	}

	/**
	 * Retrieves all reservations currently in 'Pending' status.
	 * Used primarily by admin staff to monitor upcoming arrivals.
	 * @return A list of pending reservations.
	 */
	public List<Reservation> getAllPendingReservations() {
		List<Reservation> results = new ArrayList<>();
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (Statement stmt = pConn.getConnection().createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM reservations WHERE Status = 'Pending'")) {
				while (rs.next())
					results.add(extractReservationFromResultSet(rs));
			}
		} catch (SQLException e) {
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return results;
	}

	/**
	 * Retrieves all reservations that are either in 'Pending' or 'Active' status.
	 * This is used by the server to monitor current and upcoming restaurant activity.
	 * @return A list of Reservation objects with Pending or Active status.
	 */
	public List<Reservation> getPendingAndActiveReservations() {
	    List<Reservation> results = new ArrayList<>();
	    PooledConnection pConn = null;
	    
	    // SQL query using IN operator to fetch both statuses efficiently
	    String sql = "SELECT * FROM reservations WHERE Status IN ('Pending', 'Active')";
	    
	    try {
	        pConn = db.getConnection();
	        // Using try-with-resources for Statement and ResultSet to ensure they are closed
	        try (Statement stmt = pConn.getConnection().createStatement();
	             ResultSet rs = stmt.executeQuery(sql)) {
	            
	            while (rs.next()) {
	                // Map the current row to a Reservation entity
	                results.add(extractReservationFromResultSet(rs));
	            }
	        }
	    } catch (SQLException e) {
	        // Standard error logging for the repository layer
	        System.err.println("[Reservation Repository] Error fetching pending/active reservations: " + e.getMessage());
	        e.printStackTrace();
	    } finally {
	        // Ensure the connection is returned to the pool regardless of success or failure
	        if (pConn != null) {
	            db.releaseConnection(pConn);
	        }
	    }
	    return results;
	}

	/**
	 * Hard deletes a reservation from the database. 
	 * RESERVED FOR ADMIN USE ONLY.
	 * @param id The internal database ID of the reservation to delete.
	 * @return true if the deletion affected a row.
	 */
	public boolean deleteById(int id) {
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection()
					.prepareStatement("DELETE FROM reservations WHERE ID = ?")) {
				pstmt.setInt(1, id);
				return pstmt.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			return false;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Helper method to map a SQL ResultSet row to a Reservation object.
	 * Includes support for reminder flags and arrival/departure timestamps.
	 * @param rs The result set cursor pointing to a valid row.
	 * @return A fully populated Reservation object.
	 * @throws SQLException If a database access error occurs.
	 */
	private Reservation extractReservationFromResultSet(ResultSet rs) throws SQLException {
		return new Reservation(rs.getInt("ID"), (Integer) rs.getObject("UserID"), (Integer) rs.getObject("TableID"),
				rs.getString("Phone"), rs.getString("Email"), rs.getTimestamp("ReservationStartTime").toLocalDateTime(),
				rs.getTimestamp("ReservationEndTime").toLocalDateTime(),
				rs.getTimestamp("ActualArrivalTime") != null ? rs.getTimestamp("ActualArrivalTime").toLocalDateTime()
						: null,
				rs.getTimestamp("ActualDepartureTime") != null
						? rs.getTimestamp("ActualDepartureTime").toLocalDateTime()
						: null,
				rs.getInt("NumberOfDiners"), rs.getInt("ConfirmationCode"), rs.getString("Status"),
				rs.getTimestamp("CreationTime").toLocalDateTime(), rs.getBoolean("RemindedPreArrival"),
				rs.getBoolean("RemindedDeparture"));
	}

	/**
	 * Updates the status of a specific reservation identified by its internal ID.
	 * @param reservationId The database ID of the reservation.
	 * @param newStatus     The new status enum value.
	 * @return true if the update was successful.
	 */
	public boolean updateStatusByID(int reservationId, ReservationStatus newStatus) {
		String sql = "UPDATE reservations SET Status = ? WHERE ID = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setString(1, newStatus.toString()); // Converts enum to "Pending"/"Canceled" etc.
				pstmt.setInt(2, reservationId);
				return pstmt.executeUpdate() > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Updates the status of a specific reservation identified by its confirmation code.
	 * @param confCode  The unique confirmation code.
	 * @param newStatus The new status enum value.
	 * @return true if the update was successful.
	 */
	public boolean updateStatusByConfirmationCode(int confCode, ReservationStatus newStatus) {
		String sql = "UPDATE Reservations SET Status = '" + newStatus.toString() + "' WHERE ConfirmationCode = "
				+ confCode;
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			Statement stmt = pConn.getConnection().createStatement();
			int x = stmt.executeUpdate(sql);
			if (x == 0)
				return false;
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Retrieves the current status string of a reservation by its confirmation code.
	 * @param confCode The unique confirmation code.
	 * @return The status string (e.g., "Active"), or null if not found.
	 */
	public String getStatusByConfirmationCode(int confCode) {
		String sql = "SELECT Status FROM Reservations WHERE ConfirmationCode = " + confCode;
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			Statement stmt = pConn.getConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				String status = rs.getString("Status");
				System.out.println("Found status: " + status + " for code: " + confCode);
				return status;
			} else
				return null;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Retrieves the latest active reservation for a given phone number.
	 * Used during check-in or terminal interactions.
	 * @param phone The phone number to search for.
	 * @return The latest active Reservation or null if none found.
	 */
	public Reservation getLatestReservationByPhone(String phone) {

		String sql = "SELECT ID, Phone, ReservationStartTime, NumberOfDiners, TableID, Status " + "FROM reservations "
				+ "WHERE Phone = ? " + "AND status = 'ACTIVE'" + "ORDER BY ReservationStartTime ASC " + "LIMIT 1";

		PooledConnection pConn = null;

		try {
			pConn = db.getConnection();
			Connection conn = pConn.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, phone);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				return new Reservation(rs.getInt("ID"), rs.getString("Phone"),
						rs.getTimestamp("ReservationStartTime").toLocalDateTime(), rs.getInt("NumberOfDiners"),
						(Integer) rs.getObject("TableID"), rs.getString("Status"));
			}

		} catch (Exception e) {
			System.out.println("getLatestReservationByPhone ERROR: " + e.getMessage());
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}

		return null;
	}
	
	/**
	 * Retrieves the latest active reservation associated with a specific email address.
	 * @param email The email address to search for.
	 * @return The latest active Reservation or null if none found.
	 */
	public Reservation getLatestReservationByEmail(String email) {

	    // SQL targeted at the Email column instead of Phone
	    String sql = "SELECT ID, Phone, Email, ReservationStartTime, NumberOfDiners, TableID, Status " + 
	                 "FROM reservations " + 
	                 "WHERE Email = ? " + 
	                 "AND status = 'ACTIVE' " + 
	                 "ORDER BY ReservationStartTime ASC " + 
	                 "LIMIT 1";

	    PooledConnection pConn = null;

	    try {
	        pConn = db.getConnection();
	        Connection conn = pConn.getConnection();
	        PreparedStatement ps = conn.prepareStatement(sql);
	        ps.setString(1, email); // Setting the email string as the parameter

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {
	            // Mapping the database result to the Reservation entity
	            return new Reservation(
	                rs.getInt("ID"), 
	                rs.getString("Phone"),
	                rs.getTimestamp("ReservationStartTime").toLocalDateTime(), 
	                rs.getInt("NumberOfDiners"),
	                (Integer) rs.getObject("TableID"), 
	                rs.getString("Status")
	            );
	        }

	    } catch (Exception e) {
	        System.out.println("getLatestReservationByEmail ERROR: " + e.getMessage());
	    } finally {
	        if (pConn != null)
	            db.releaseConnection(pConn);
	    }

	    return null;
	}

	/**
	 * Finalizes a reservation session. 
	 * Sets status to 'Completed' and records the current timestamp as ActualDepartureTime.
	 * @param reservationId The internal database ID of the reservation.
	 */
	public void markReservationAsCompleted(int reservationId) {
		String sql = "UPDATE reservations SET Status = 'Completed', ActualDepartureTime = NOW() WHERE ID = ?";

		PooledConnection pConn = null;

		try {
			pConn = db.getConnection();
			Connection conn = pConn.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql);

			ps.setInt(1, reservationId);
			ps.executeUpdate();

			ps.close();

		} catch (Exception e) {
			System.out.println("markReservationAsCompleted ERROR: " + e.getMessage());
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Retrieves a single reservation by its unique internal database ID.
	 * @param id The primary key ID.
	 * @return The populated Reservation object or null if not found.
	 */
	public Reservation getById(int id) {
		String sql = "SELECT * FROM reservations WHERE ID = ?";
		PooledConnection pConn = null;

		try {
			pConn = db.getConnection();
			try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
				ps.setInt(1, id);

				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						// Use the centralized extraction logic to avoid duplication
						return extractReservationFromResultSet(rs);
					}
				}
			}
		} catch (SQLException e) {
			System.err.println("[Reservation_Repository] Error in getById: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return null; // Not found
	}

	/**
	 * Assigns a specific table to a reservation identified by its confirmation code.
	 * @param confCode The reservation confirmation code.
	 * @param tableId  The internal ID of the table to assign.
	 * @return true if the update affected a row.
	 */
	public boolean updateTableByConfirmationCode(int confCode, int tableId) {
		String sql = "UPDATE reservations SET TableID = ? WHERE ConfirmationCode = ?";

		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, tableId);
				pstmt.setInt(2, confCode);
				int affectedRows = pstmt.executeUpdate();

				return affectedRows > 0;
			}
		} catch (SQLException e) {
			System.err.println("[Database] Error updating table ID for code: " + confCode);
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null) {
				db.releaseConnection(pConn);
			}
		}
	}

	/**
	 * Updates only the Actual Arrival Time of a reservation identified by its
	 * confirmation code.
	 * @param confCode    The unique confirmation code.
	 * @param arrivalTime The timestamp to record as the actual arrival time.
	 * @return true if the update was successful.
	 */
	public boolean updateActualArrivalTimeOnly(int confCode, LocalDateTime arrivalTime) {
		String sql = "UPDATE Reservations SET ActualArrivalTime = ? WHERE ConfirmationCode = ?";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(arrivalTime));
				pstmt.setInt(2, confCode);

				int rowsAffected = pstmt.executeUpdate();
				return rowsAffected > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (pConn != null) {
				db.releaseConnection(pConn);
			}
		}
	}

	/**
	 * Fetches ACTIVE reservations that have exceeded their time limit.
	 * Used by the Watchdog service to send departure reminders or auto-complete sessions.
	 * @param isNotified If true, fetches reservations that have already received a reminder 15 minutes ago.
	 * @return A list of overdue reservations.
	 */
	public List<Reservation> getExpiredActiveReservations(boolean isNotified) { //is notified halpes to find what reservations to mark as completed in watchdog
		List<Reservation> expiredList = new ArrayList<>();
		// Query: Status=ACTIVE, Time Passed, Not Reminded Yet
		String sql = "SELECT * FROM reservations WHERE Status = '"+ReservationStatus.ACTIVE.toString()+"' "
				+ "AND ReservationEndTime < ? AND RemindedDeparture = 0";
		if (isNotified) sql = "SELECT * FROM reservations WHERE Status = '"+ReservationStatus.ACTIVE.toString()+"' "
				+ "AND ReservationEndTime < ? AND RemindedDeparture = 1";
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				if (!isNotified) pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
				else pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)));
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next())
						expiredList.add(extractReservationFromResultSet(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return expiredList;
	}

	/**
	 * Fetches reservations scheduled to start within the next 2 hours that
	 * have not yet received a pre-arrival reminder.
	 * @return A list of upcoming reservations for the reminder service.
	 */
	public List<Reservation> getUpcomingReservationsForReminder() {
		List<Reservation> upcomingList = new ArrayList<>();
		// Logic: StartTime is between NOW and NOW+2.5 Hours, Status is
		// Approved/Pending, Flag is 0
		String sql = "SELECT * FROM reservations WHERE Status IN ('Pending', 'Approved') "
				+ "AND ReservationStartTime BETWEEN ? AND ? " + "AND RemindedPreArrival = 0";

		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				LocalDateTime now = LocalDateTime.now();
				pstmt.setTimestamp(1, Timestamp.valueOf(now));
				pstmt.setTimestamp(2, Timestamp.valueOf(now.plusMinutes(120))); // Look ahead 2 hours

				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next())
						upcomingList.add(extractReservationFromResultSet(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
		return upcomingList;
	}

	/**
	 * Updates the database to indicate that a reminder notification has been successfully sent.
	 * @param reservationId The internal ID to update.
	 * @param type          "PRE" for Pre-Arrival reminder, "DEP" for Departure reminder.
	 */
	public void markAsReminded(int reservationId, String type) {
		String column = type.equals("PRE") ? "RemindedPreArrival" : "RemindedDeparture";
		String sql = "UPDATE reservations SET " + column + " = 1 WHERE ID = ?";

		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setInt(1, reservationId);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (pConn != null)
				db.releaseConnection(pConn);
		}
	}

	/**
	 * Retrieves reservations where guests have stayed longer than the permitted time.
	 * @param hours The number of hours threshold to consider as overstaying.
	 * @return A list of active reservations that exceeded the threshold.
	 */
	// FOR TIMER
	public List<Reservation> getOverstayingReservations(int hours) {
		List<Reservation> lateReservations = new ArrayList<>();

		// השאילתה:
		// 1. Status = 'Active' -> הלקוח עדיין יושב
		// 2. ActualArrivalTime < (עכשיו פחות X שעות) -> הוא הגיע מזמן
		String query = "SELECT * FROM reservations WHERE Status = 'Active' AND ActualArrivalTime < DATE_SUB(NOW(), INTERVAL ? HOUR)";

		PooledConnection pConn = null;
		PreparedStatement pstmt = null;

		try {
			pConn = db.getConnection();
			pstmt = pConn.getConnection().prepareStatement(query);

			pstmt.setInt(1, hours); // הצבת מספר השעות (למשל 2)

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Reservation r = extractReservationFromResultSet(rs);
					lateReservations.add(r);
				}
			}
		} catch (SQLException e) {
			System.err.println("Error fetching overstaying reservations: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (pstmt != null)
					pstmt.close();
			} catch (SQLException e) {
			}
			if (pConn != null)
				db.releaseConnection(pConn);
		}

		return lateReservations;
	}

	/**
	 * Retrieves reservations where guests failed to show up after their scheduled time.
	 * @param minutes The grace period in minutes before a reservation is considered a 'no-show'.
	 * @return A list of pending reservations that exceeded the grace period.
	 */
	// FOR TIMER-WATCHDOG uses it
	public List<Reservation> getNoShowCandidates(int minutes) {
		List<Reservation> noShowReservations = new ArrayList<>();

		// השאילתה:
		// 1. Status = 'Pending' -> הלקוח טרם הגיע
		// 2. ReservationStartTime < (עכשיו פחות X דקות) -> זמן ההגעה עבר מזמן
		String query = "SELECT * FROM reservations WHERE Status = 'Pending' AND ReservationStartTime < DATE_SUB(NOW(), INTERVAL ? MINUTE)";

		PooledConnection pConn = null;
		PreparedStatement pstmt = null;

		try {
			pConn = db.getConnection();
			pstmt = pConn.getConnection().prepareStatement(query);

			pstmt.setInt(1, minutes);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Reservation r = extractReservationFromResultSet(rs);
					noShowReservations.add(r);
				}
			}
		} catch (SQLException e) {
			System.err.println("Error fetching no-show candidates: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (pstmt != null)
					pstmt.close();
			} catch (SQLException e) {
			}
			if (pConn != null)
				db.releaseConnection(pConn);
		}

		return noShowReservations;
	}
	
	/**
	 * Fetches all reservations that overlap with a specific time range.
	 * Used for capacity validation and conflict detection during booking.
	 * @param start     The start of the time range.
	 * @param end       The end of the time range.
	 * @param excludeId An optional ID to exclude from the check (e.g., when updating an existing reservation).
	 * @return A list of overlapping reservations.
	 */
    public List<Reservation> getOverlappingReservationsList(LocalDateTime start, LocalDateTime end, Integer excludeId) {
        List<Reservation> conflicts = new ArrayList<Reservation>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * ");
        sql.append("FROM reservations ");
        sql.append("WHERE Status IN ('Pending', 'Active') ");
        sql.append("AND (ReservationStartTime < ? AND ReservationEndTime > ?) ");
        
        if (excludeId != null) {
            sql.append("AND ID != ?");
        }

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql.toString())) {
                pstmt.setTimestamp(1, Timestamp.valueOf(end));
                pstmt.setTimestamp(2, Timestamp.valueOf(start));
                
                if (excludeId != null) {
                    pstmt.setInt(3, excludeId);
                }

                ResultSet rs = pstmt.executeQuery();
                while (rs.next())
                	conflicts.add(extractReservationFromResultSet(rs)); 	
            }
            
        } catch (SQLException e) {
            System.err.println("DB Error while fetching overlapping reservations: " + e.getMessage());
        } finally {
            if (pConn != null) {
                db.releaseConnection(pConn);
            }
        }
        return conflicts;
    }
    
    /**
     * Finds the closest pending or active reservation for a contact to facilitate "Forgot Code" recovery.
     * Prioritizes 'Active' reservations and sorts by temporal proximity to the current time.
     * @param phone The contact phone number.
     * @param email The contact email address.
     * @return The most relevant Reservation object or null if none found.
     */
    //for forgot the code logic(found in user controller- used by terminal)
    public Reservation getClosestReservationByContact(String phone, String email) {

        String sql = "SELECT * FROM reservations "
                   + "WHERE (Phone = ? OR Email = ?) " 
                   + "AND Status IN ('ACTIVE', 'PENDING') "
                   + "ORDER BY "
                   + "  CASE WHEN Status = 'ACTIVE' THEN 1 ELSE 2 END ASC, " 
                   + "  ABS(TIMESTAMPDIFF(SECOND, ReservationStartTime, NOW())) ASC "
                   + "LIMIT 1";

        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, phone);
            ps.setString(2, email);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return extractReservationFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.out.println("getClosestReservationByContact ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pConn != null)
                db.releaseConnection(pConn);
        }

        return null;
    }

    /**
     * Retrieves the history of completed dining visits for a specific user.
     * <p>
     * This method fetches reservations that strictly meet two conditions:
     * 1. The reservation status is 'Completed'.
     * 2. There is an associated Bill record in the database.
     * </p>
     * * @param userId The unique ID of the subscriber.
     * @return A List of Reservation objects populated with their associated Bill details.
     */
    public List<Reservation> getCompletedVisitsByUserId(int userId) {
        List<Reservation> results = new ArrayList<>();
        
        // SQL Query construction:
        // We use 'JOIN' (which is an INNER JOIN) instead of 'LEFT JOIN'.
        // This acts as a filter: if a reservation does not have a matching row in the Bills table,
        // it will be excluded from the results entirely.
        String sql = "SELECT r.*, " +
                     "b.ID AS BillID, b.TotalAmount, b.BillDetails, b.DiscountPercentage, b.Status AS BillStatus " +
                     "FROM Reservations r " +
                     "JOIN Bills b ON r.ID = b.ReservationID " + 
                     "WHERE r.UserID = ? AND r.Status = 'Completed' " +
                     "ORDER BY r.ReservationStartTime DESC";
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        // 1. Extract the base Reservation data
                        Reservation res = extractReservationFromResultSet(rs);
                        
                        // 2. Extract the Bill data
                        // Since we used INNER JOIN, we are guaranteed that BillID is valid and exists.
                        int billId = rs.getInt("BillID");
                        
                        entities.Bill bill = new entities.Bill(
                            billId,
                            res.getId(),
                            rs.getString("BillDetails"),
                            rs.getDouble("TotalAmount"),
                            rs.getString("BillStatus"),
                            rs.getDouble("DiscountPercentage")
                        );
                        
                        // 3. Attach the Bill object to the Reservation
                        res.setBill(bill);
                        
                        results.add(res);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return results;
    }

    /**
     * Retrieves ALL reservation history (Completed, Canceled, No-show).
     * This corresponds to the "Order History" tab.
     * @param userId The subscriber ID.
     * @return List of all past reservations.
     */
    public List<Reservation> getAllReservationHistory(int userId) {
        List<Reservation> results = new ArrayList<>();
        // Fetch everything that is NOT pending/active (i.e., historical)
        String sql = "SELECT * FROM Reservations WHERE UserID = ? " +
                     "AND Status IN ('Completed', 'Canceled', 'No-show') " +
                     "ORDER BY ReservationStartTime DESC";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(extractReservationFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return results;
    }

    /**
     * Checks for scheduling conflicts between proposed operating hours and existing reservations.
     * This method handles both standard shifts and overnight shifts (crossing midnight).
     * * @param targetDay The day of the week to check (e.g., WEDNESDAY).
     * @param newOpen   The new opening time.
     * @param newClose  The new closing time.
     * @param isActive  True if the day is set to be open, False if the day is being closed.
     * @return The {@link LocalDate} of the furthest future reservation that conflicts with the new hours, 
     * or {@code null} if no conflicts are found.
     */
    public LocalDate findConflictForRegularUpdate(DayOfWeek targetDay, LocalTime newOpen, LocalTime newClose, boolean isActive) {
        
        LocalDate furthestConflictDate = null;
        
        // Check if the shift spans across midnight (e.g., Open 12:00 PM, Close 03:00 AM next day)
        boolean crossesMidnight = newClose.isBefore(newOpen);

        String sql = "SELECT ID, ReservationStartTime, ReservationEndTime FROM reservations " +
                     "WHERE Status = 'Pending' " +
                     "AND ReservationStartTime > NOW()";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    LocalDateTime resStart = rs.getTimestamp("ReservationStartTime").toLocalDateTime();
                    LocalDateTime resEnd = rs.getTimestamp("ReservationEndTime").toLocalDateTime();

                    // Filter: Ensure we only check reservations that fall on the specific target day
                    if (resStart.getDayOfWeek() != targetDay) {
                        continue; 
                    }

                    boolean isConflict = false;
                    // Scenario A: The user wants to CLOSE the restaurant on this day.
                    if (!isActive) {
                        isConflict = true;
                    } else {
                        LocalTime resStartTime = resStart.toLocalTime();
                        LocalTime resEndTime = resEnd.toLocalTime();

                        if (crossesMidnight) {
                            // --- Logic for Overnight Shifts (e.g., 12:00 to 03:00) ---
                            // In this scenario, a time is "forbidden" only if it falls in the "closed gap" 
                            // between the closing time (morning) and opening time (noon).
                            // i.e., Conflict if time is AFTER 03:00 AND BEFORE 12:00.

                            // Check start time against the closed gap
                            if (resStartTime.isAfter(newClose) && resStartTime.isBefore(newOpen)) {
                                isConflict = true;
                            }
                            // Check end time against the closed gap
                            else if (resEndTime.isAfter(newClose) && resEndTime.isBefore(newOpen)) {
                                isConflict = true;
                            }

                        } else {
                            // --- Logic for Standard Shifts (e.g., 08:00 to 22:00) ---
                            // Standard bounds check.
                            if (resStartTime.isBefore(newOpen)) {
                                isConflict = true;
                            } else if (resEndTime.isAfter(newClose)) {
                                 if (!resEndTime.equals(newClose)) {
                                    isConflict = true;
                                 }
                            }
                        }
                    }

                    // If a conflict was found, update the furthest date found so far
                    if (isConflict) {
                        LocalDate currentResDate = resStart.toLocalDate();
                        // We want the MAX date (the furthest one in the future)
                        if (furthestConflictDate == null || currentResDate.isAfter(furthestConflictDate)) {
                            furthestConflictDate = currentResDate;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        
        return furthestConflictDate;
    }

    /**
     * Checks if a new special schedule (exception date) conflicts with reservations on that date.
     * @param date     The specific date to check.
     * @param newOpen  The new opening time for that date (null if the restaurant is to be closed).
     * @param newClose The new closing time for that date.
     * @return true if at least one reservation conflicts with the new hours, false otherwise.
     */
    public boolean hasConflictForSpecialDate(LocalDate date, LocalTime newOpen, LocalTime newClose) {
        String sql = "SELECT ReservationStartTime, ReservationEndTime FROM reservations " +
                     "WHERE Status IN ('Pending', 'Active') " +
                     "AND DATE(ReservationStartTime) = ?";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Case 1: Closed completely
                        if (newOpen == null || newClose == null) return true; // Found a reservation on a day you want to close

                        // Case 2: Partial Hours
                        LocalDateTime resStart = rs.getTimestamp("ReservationStartTime").toLocalDateTime();
                        LocalDateTime resEnd = rs.getTimestamp("ReservationEndTime").toLocalDateTime();
                        
                        if (resStart.toLocalTime().isBefore(newOpen) || resEnd.toLocalTime().isAfter(newClose)) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return false;
    }
}