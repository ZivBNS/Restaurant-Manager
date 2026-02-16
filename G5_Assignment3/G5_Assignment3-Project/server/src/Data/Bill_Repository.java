package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import entities.Bill;

/**
 * Repository class for managing Bill entities in the database.
 * Implements the Singleton pattern to provide a centralized access point for bill-related data operations.
 */
public class Bill_Repository {

    private static Bill_Repository instance;
    private DB_Controller db = DB_Controller.getInstance(); 

    private Bill_Repository() {}

    /**
     * Retrieves the singleton instance of the Bill_Repository.
     * * @return The active Bill_Repository instance.
     */
    public static Bill_Repository getInstance() {
        if (instance == null)
            instance = new Bill_Repository();
        return instance;
    }

    /**
     * Persists a new bill in the database.
     * Sets the default status to "Unpaid" upon creation.
     * * @param bill The Bill object containing the data to be saved.
     * @return true if the bill was successfully inserted, false otherwise.
     */
    public boolean set(Bill bill) {
    	String query = "INSERT INTO bills (ReservationID, TotalAmount, BillDetails, Status, DiscountPercentage) VALUES (?, ?, ?, ?, ?)";        
        PooledConnection pConn = null;
        PreparedStatement ps = null;

        try {
            pConn = db.getConnection();
            ps = pConn.getConnection().prepareStatement(query);

            ps.setInt(1, bill.getReservationId());
            ps.setDouble(2, bill.getTotalAmount());
            ps.setString(3, bill.getBillDetails());
            ps.setString(4, "Unpaid"); 
            ps.setDouble(5, bill.getDiscountRate());
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error creating bill: " + e.getMessage());
            return false;
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Deletes a bill from the database based on its ID.
     * * @param id The primary key ID of the bill to delete.
     * @return true if the bill was deleted, false otherwise.
     */
    public boolean deleteById(int id) {
        String query = "DELETE FROM bills WHERE ID = ?";
        
        PooledConnection pConn = null;
        PreparedStatement ps = null;

        try {
            pConn = db.getConnection();
            ps = pConn.getConnection().prepareStatement(query);
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    /**
     * Retrieves a bill from the database by its ID.
     * * @param id The primary key ID of the bill.
     * @return The Bill object if found, or null if no bill exists with that ID.
     */
    public Bill getById(int id) {
        String query = "SELECT * FROM bills WHERE id = ?";
        PooledConnection pConn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Bill bill = null;

        try {
            pConn = db.getConnection();
            ps = pConn.getConnection().prepareStatement(query);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
            	bill = new Bill(
                        rs.getInt("ID"),
                        rs.getInt("ReservationID"),
                        rs.getString("BillDetails"),
                        rs.getDouble("TotalAmount"),
                        rs.getString("Status"),
                        rs.getDouble("DiscountPercentage") 
                    );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (ps != null) ps.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
        return bill;
    }
    
    /**
     * Stub for updating a bill object.
     * * @param objToUpdate The Bill to update.
     * @return false (not yet implemented).
     */
    public boolean update(Bill objToUpdate) {
        return false;
    }

    /**
     * Fetches a list of all bills stored in the database, ordered by ID descending.
     * * @return A list of Bill objects.
     */
    public List<Bill> getAllBills() {
        List<Bill> list = new ArrayList<>();
        String query = "SELECT * FROM bills ORDER BY ID DESC"; 

        PooledConnection pConn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            pConn = db.getConnection();
            stmt = pConn.getConnection().createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
            	Bill b = new Bill(
                        rs.getInt("ID"),
                        rs.getInt("ReservationID"),
                        rs.getString("BillDetails"),
                        rs.getDouble("TotalAmount"),
                        rs.getString("Status"),
                        rs.getDouble("DiscountPercentage") 
                );
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
        return list;
    }

    /**
     * Retrieves the most recent bill associated with a given reservation ID.
     * * @param reservationId The ID of the reservation.
     * @return The Bill object, or null if no bill is associated with the reservation.
     */
    public Bill getBillByReservationId(int reservationId) {
        Bill bill = null;
        String query = "SELECT * FROM bills WHERE reservationId = " + reservationId + " ORDER BY id DESC LIMIT 1";

        PooledConnection pConn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            pConn = db.getConnection();
            stmt = pConn.getConnection().createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                bill = new Bill(
                    rs.getInt("ID"),
                    rs.getInt("ReservationID"),
                    rs.getString("BillDetails"),
                    rs.getDouble("TotalAmount"),
                    rs.getString("Status"),
                    rs.getDouble("DiscountPercentage") 
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
        return bill;
    }
    
    /**
     * Retrieves the reservation ID associated with a specific bill ID.
     * * @param billId The ID of the bill.
     * @return The reservation ID, or -1 if the lookup fails.
     */
    public int getReservationIdByBillId(int billId) {
        String sql = "SELECT reservationId FROM bills WHERE id = ?";
        PooledConnection pConn = null;

        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);

            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return rs.getInt("reservationId");

        } catch (Exception e) {
            System.out.println("getReservationIdByBillId ERROR: " + e.getMessage());
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return -1;
    }

    /**
     * Updates the status of a specific bill to "Paid".
     * * @param billId The ID of the bill to be updated.
     * @return true if the status was successfully updated, false otherwise.
     */
    public boolean markBillAsPaid(int billId) {
        String query = "UPDATE bills SET status = 'Paid' WHERE id = " + billId;
        PooledConnection pConn = null;
        Statement stmt = null;

        try {
            pConn = db.getConnection();
            stmt = pConn.getConnection().createStatement();
            int rows = stmt.executeUpdate(query);
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    
    /**
     * Alias for the set method to create a new bill record.
     * * @param bill The Bill object to be created.
     * @return true if successful.
     */
    public boolean createBill(Bill bill) {
        return set(bill);
    }
    
    /**
     * Updates the existing data of a bill, including total amount, details, discount, and status.
     * Prints the executing SQL for debugging purposes.
     * * @param bill The Bill object containing updated information.
     * @return true if the record was updated, false otherwise.
     */
    public boolean updateBillData(Bill bill) {
        String sql = "UPDATE bills SET " +
                     "TotalAmount = " + bill.getTotalAmount() + ", " +
                     "BillDetails = '" + bill.getBillDetails() + "', " +
                     "DiscountPercentage = " + bill.getDiscountRate() + ", " +
                     "Status = '" + bill.getStatus() + "' " +
                     "WHERE ID = " + bill.getId() + ";";
        
        System.out.println("[SQL DEBUG] Executing Query: " + sql);
        
        PooledConnection pConn = null;
        Statement stmt = null;
        try {
            pConn = db.getConnection();
            stmt = pConn.getConnection().createStatement();
            int affectedRows = stmt.executeUpdate(sql);
            return affectedRows > 0; 
        } catch (SQLException e) {
            System.out.println("[SQL ERROR] " + e.getMessage());
            return false;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
}