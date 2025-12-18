package Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import entities.Restaurant;
import entities.Restaurant_Table;

/**
 * Repository class for managing Table data and availability. Implements the
 * Singleton pattern to provide a single point of database access for tables.
 */
public class Table_Repository implements Repository_Interface<Restaurant_Table> {
	private DB_Controller db = DB_Controller.getInstance();
	private static Table_Repository TableRepositoryInstance = new Table_Repository();

	private Table_Repository() {
	}

	/**
	 * @return The single instance of Table_Repository.
	 */
	public static Table_Repository getInstance() {
		return TableRepositoryInstance;
	}

	/**
	 * Initializes the restaurant by loading all tables from the database and
	 * calculating the largest available table size.
	 */
	@Override
	public void init() {
		int maxTableSize = 0;
		List<Restaurant_Table> tablesList = new ArrayList<>();
		String sql = "SELECT ID, TableNumber, Size, IsActive FROM Tables";

		try (Statement stmt = db.getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

			while (rs.next()) {
				int id = rs.getInt("ID");
				int tableNumber = rs.getInt("TableNumber");
				int size = rs.getInt("Size");
				boolean isActive = rs.getBoolean("IsActive");

				Restaurant_Table table = new Restaurant_Table(id, tableNumber, size, isActive);
				if (table.getSize() > maxTableSize)
					maxTableSize = table.getSize();
				tablesList.add(table);
			}

			Restaurant.getInstance().setTables(tablesList);
			Restaurant.setBiggestTableSize(maxTableSize);
			System.out.println("Table Repository: Loaded " + tablesList.size() + " tables.");

		} catch (SQLException e) {
			System.err.println("Init Error: Failed to load tables: " + e.getMessage());
		}
	}

	/**
	 * Finds the most suitable available table ID for a given time slot and guest
	 * count. Logic: Smallest active table that fits the guests and has no
	 * overlapping reservations. * @param start The requested start time of the
	 * reservation.
	 * 
	 * @param end    The calculated end time of the reservation (e.g., start + 2
	 *               hours).
	 * @param guests The number of diners to seat.
	 * @return The Table ID if an available table is found, null otherwise.
	 */
	public Integer findBestAvailableTable(LocalDateTime start, LocalDateTime end, int guests) {
		// Query checks for capacity, active status, and non-overlapping time slots
		String sql = "SELECT ID FROM Tables " + "WHERE Size >= ? AND IsActive = 1 " + "AND ID NOT IN ("
				+ "    SELECT TableID FROM reservations " + "    WHERE TableID IS NOT NULL "
				+ "    AND Status != 'Canceled' " + "    AND (ReservationStartTime < ? AND ReservationEndTime > ?)"
				+ ") " + "ORDER BY Size ASC " + // Best Fit: prioritize smaller tables first
				"LIMIT 1";

		try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
			pstmt.setInt(1, guests);
			pstmt.setTimestamp(2, Timestamp.valueOf(end));
			pstmt.setTimestamp(3, Timestamp.valueOf(start));

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("ID");
				}
			}
		} catch (SQLException e) {
			System.err.println("Database Error: Table search failed: " + e.getMessage());
		}
		return null;
	}

	@Override
	public boolean set(Restaurant_Table obj) {
		return false;
	}

	@Override
	public boolean update(Restaurant_Table obj) {
		return false;
	}

	@Override
	public boolean deleteById(int id) {
		return false;
	}

	@Override
	public Restaurant_Table getById(int id) {
		return null;
	}
}