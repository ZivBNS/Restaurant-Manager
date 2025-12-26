package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import entities.Bill;
import entities.Reservation;

public class Bill_Repository {

    private static Bill_Repository instance;
    private final DB_Controller db = DB_Controller.getInstance();

    private Bill_Repository() {}

    public static Bill_Repository getInstance() {
        if (instance == null)
            instance = new Bill_Repository();
        return instance;
    }

    /**
     * Returns latest bill for a given id – according to the closest reservation in time
     */
	public Bill getBillByReservationId(Integer reservationId) {
		String sql =
			    "SELECT b.TotalAmount, b.BillDetails, b.Status, r.ID AS ReservationID " +
			    "FROM bills b " +
			    "JOIN reservations r ON b.ReservationID = r.ID " +
			    "WHERE r.ID = ? " +
			    "  AND b.Status IN ('OPEN', 'UNPAID', 'PAID') " +
			    "  AND r.Status = 'ACTIVE' " +
			    "ORDER BY ABS(TIMESTAMPDIFF(MINUTE, r.ReservationStartTime, NOW())) " +
			    "LIMIT 1";
		
		PooledConnection pConn = null;
		try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, String.valueOf(reservationId));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                double totalAmount = rs.getDouble("TotalAmount");
                String billDetails = rs.getString("BillDetails");
                int reservationID = rs.getInt("ReservationID");
                String status = rs.getString("Status");
                int id = rs.getInt("ID");

                // ⬅ כאן אנחנו משתמשים בקונסטרקטור הקיים של Bill(Reservation,...)
                Reservation reservation =
                        Reservation_Repository.getInstance().getReservationById(reservationID);

                if (reservation == null) return null;

                return new Bill(id,reservationID,billDetails,totalAmount,status);
            }

        } catch (Exception e) {
            System.out.println("getLatestBillByPhone ERROR: " + e.getMessage());
        } finally {
            if (pConn != null)
                db.releaseConnection(pConn);
        }
		
 
		return null;
	}

	public boolean markBillAsPaid(Integer billId) {
		// TODO Auto-generated method stub
		return false;
	}
}




