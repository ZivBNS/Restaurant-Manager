package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import entities.Bill;


public class Bill_Repository {

    private static Bill_Repository instance;
    private DB_Controller db = DB_Controller.getInstance(); 

    private Bill_Repository() {}

    public static Bill_Repository getInstance() {
        if (instance == null)
            instance = new Bill_Repository();
        return instance;
    }

    
    
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
    
    
    public boolean update(Bill objToUpdate) {
        return false;
    }

   

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

    public boolean markBillAsPaid(int billId) {
        String query = "UPDATE bills SET status = 'PAID' WHERE id = " + billId;
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
    
    public boolean createBill(Bill bill) {
        return set(bill);
    }
    
    public boolean updateBillData(Bill bill) {
        // בניית השאילתה - וודא ששם העמודה DiscountPercentage מדויק
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






