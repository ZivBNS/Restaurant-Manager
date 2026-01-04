package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import entities.Reservation;
import entities.ReservationStatus;

/**
 * Repository for reservation data. Updated to support Logical Seating
 * simulation by storing TableID as NULL.
 */
public class Reservation_Repository {

	private DB_Controller db = DB_Controller.getInstance();
	private static Reservation_Repository reservationRepositoryInstance = new Reservation_Repository();
	private static int confirmationCodeGenerator = 100000;

	private Reservation_Repository() {
	}

	public static Reservation_Repository getInstance() {
		return reservationRepositoryInstance;
	}

	/**
	 * Fetches a reservation by its unique confirmation code.
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

	public synchronized int getNextConfirmationCode() {
		return confirmationCodeGenerator++;
	}

	/**
	 * Saves a new reservation to the database. TableID is explicitly set to NULL on
	 * creation. Reminder flags are initialized to 0 (false).
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
	String sql = "UPDATE reservations SET Status = 'Completed', ActualDepartureTime = NOW() WHERE ID = ?";

	public boolean updateReservationForCheckIn(int confCode, int TableId, ReservationStatus rs) {
		// UserID INT, TableID INT, Phone VARCHAR(14), Email VARCHAR(35),
		// ReservationStartTime DATETIME, ReservationEndTime DATETIME ,
		// ActualArrivalTime DATETIME, ActualDepartureTime DATETIME, NumberOfDiners INT,
		// ConfirmationCode INT, Status CreationTime DATETIME);
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

	public boolean updateReservationForCheckOut(int confCode, LocalDateTime actualFinishTime) {
		String sql = "UPDATE reservations SET Status = 'Completed', ActualDepartureTime = ? WHERE ConfirmationCode = ?";
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
	 * currently 'Pending' or 'Active'. * @param userId The unique subscriber code.
	 * 
	 * @return A list of filtered reservations.
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
	 * currently 'Pending' or 'Active'. * @param contact The phone number or email
	 * string.
	 * 
	 * @return A list of filtered reservations.
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

	public List<Reservation> getAllPendingReservations() { /* Patterned pool fetch... */
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
	 * Hard deletes a reservation from the database. RESERVED FOR ADMIN USE ONLY.
	 * * @param id The reservation ID to delete.
	 * 
	 * @return true if deleted.
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
	 * Helper method to map a SQL ResultSet row to a Reservation object. Updated to
	 * include the reminder flags.
	 * 
	 * @param rs The result set cursor.
	 * @return A populated Reservation object.
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
	 * Updates only the status of a specific reservation. Used for cancellations,
	 * check-ins, and completions. * @param reservationId The ID of the reservation.
	 * 
	 * @param newStatus The new status enum value.
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

	public boolean updateStatusByConfirmationCode(int confCode, ReservationStatus newStatus) {
		String sql = "UPDATE Reservations SET Status = '" + newStatus.toString() + "' WHERE ConfirmationCode = "
				+ confCode;
		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			pConn.getConnection().setAutoCommit(true);
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
	 * Retrieves a reservation by its unique internal ID.
	 * 
	 * @param id The reservation ID.
	 * @return The Reservation object or null if not found.
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
	 * 
	 * @param confCode    The unique confirmation code of the reservation.
	 * @param arrivalTime The new actual arrival time to set.
	 * @return true if the update was successful, false otherwise.
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
	 * Fetches ACTIVE reservations that have exceeded their time limit AND have not
	 * been reminded yet.
	 * 
	 * @return List of overdue reservations.
	 */
	public List<Reservation> getExpiredActiveReservations() {
		List<Reservation> expiredList = new ArrayList<>();
		// Query: Status=ACTIVE, Time Passed, Not Reminded Yet
		String sql = "SELECT * FROM reservations WHERE Status = 'ACTIVE' "
				+ "AND ReservationEndTime < ? AND RemindedDeparture = 0";

		PooledConnection pConn = null;
		try {
			pConn = db.getConnection();
			try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
				pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
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
	 * Fetches reservations scheduled to start within the next 2 hours (approx) that
	 * have NOT received a reminder yet.
	 * 
	 * @return List of upcoming reservations.
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
	 * Updates the database to indicate a reminder email has been sent.
	 * 
	 * @param reservationId The ID to update.
	 * @param type          "PRE" for Pre-Arrival, "DEP" for Departure.
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

	// for waitlist controller:
	public Reservation getLastReservationByContact(String phone, String email) {
		String query = "SELECT * FROM Reservations WHERE (Phone = ? OR Email = ?) ORDER BY ID DESC LIMIT 1";

		PooledConnection pConn = null;
		PreparedStatement pstmt = null;
		Reservation reservation = null;

		try {
			pConn = db.getConnection();
			pstmt = pConn.getConnection().prepareStatement(query);

			pstmt.setString(1, phone);
			pstmt.setString(2, email);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {

					int id = rs.getInt("ID");

					int uId = rs.getInt("UserID");
					Integer userId = rs.wasNull() ? null : uId;

					int tId = rs.getInt("TableID");
					Integer tableId = rs.wasNull() ? null : tId;

					String resPhone = rs.getString("Phone");
					String resEmail = rs.getString("Email");

					java.sql.Timestamp startTs = rs.getTimestamp("ReservationStartTime");
					LocalDateTime orderStartTime = (startTs != null) ? startTs.toLocalDateTime() : null;

					java.sql.Timestamp endTs = rs.getTimestamp("ReservationEndTime");
					LocalDateTime orderEndTime = (endTs != null) ? endTs.toLocalDateTime() : null;

					java.sql.Timestamp arrTs = rs.getTimestamp("ActualArrivalTime");
					LocalDateTime actualArrivalTime = (arrTs != null) ? arrTs.toLocalDateTime() : null;

					java.sql.Timestamp depTs = rs.getTimestamp("ActualDepartureTime");
					LocalDateTime actualDepartureTime = (depTs != null) ? depTs.toLocalDateTime() : null;

					int diners = rs.getInt("NumberOfDiners");
					int code = rs.getInt("ConfirmationCode");
					String status = rs.getString("Status");

					java.sql.Timestamp createTs = rs.getTimestamp("CreationTime");
					LocalDateTime creationTime = (createTs != null) ? createTs.toLocalDateTime() : null;

					boolean remindedPre = false;
					try {
						remindedPre = rs.getBoolean("RemindedPreArrival");
					} catch (SQLException e) {
					}

					boolean remindedDep = false;
					try {
						remindedDep = rs.getBoolean("RemindedDeparture");
					} catch (SQLException e) {
					}

					reservation = new Reservation(id, userId, tableId, resPhone, resEmail, orderStartTime, orderEndTime,
							actualArrivalTime, actualDepartureTime, diners, code, status, creationTime, remindedPre,
							remindedDep);
				}
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
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

		return reservation;
	}

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

	// FOR TIMER
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

}