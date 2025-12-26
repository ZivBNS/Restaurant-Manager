package Data;

import entities.Bill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Bill_Repository {

    private static Bill_Repository instance;

    private Bill_Repository() {}

    public static Bill_Repository getInstance() {
        if (instance == null)
            instance = new Bill_Repository();
        return instance;
    }

    public Bill getBillByReservationId(int reservationId) {

        Bill bill = null;
        String query =
            "SELECT id, reservationId, billDetails, totalAmount, status " +
            "FROM bills WHERE reservationId = " + reservationId +
            " ORDER BY id DESC LIMIT 1";

        PooledConnection pConn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            pConn = DB_Controller.getInstance().getConnection();
            stmt = pConn.getConnection().createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                bill = new Bill(
                        rs.getInt("id"),
                        rs.getInt("reservationId"),
                        rs.getString("billDetails"),
                        rs.getDouble("totalAmount"),
                        rs.getString("status")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            DB_Controller.getInstance().releaseConnection(pConn);
        }

        return bill;
    }

    public boolean markBillAsPaid(int billId) {

        String query =
            "UPDATE bills SET status = 'PAID' WHERE id = " + billId;

        PooledConnection pConn = null;
        Statement stmt = null;

        try {
            pConn = DB_Controller.getInstance().getConnection();
            stmt = pConn.getConnection().createStatement();
            int rows = stmt.executeUpdate(query);
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;

        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            DB_Controller.getInstance().releaseConnection(pConn);
        }
    }
    
    public int getReservationIdByBillId(int billId) {

        String sql = "SELECT reservationId FROM bills WHERE id = ?";

        PooledConnection pConn = null;

        try {
            pConn = DB_Controller.getInstance().getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);

            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return rs.getInt("reservationId");

        } catch (Exception e) {
            System.out.println("getReservationIdByBillId ERROR: " + e.getMessage());
        } finally {
            if (pConn != null)
            	DB_Controller.getInstance().releaseConnection(pConn);
        }

        return -1;
    }

}






