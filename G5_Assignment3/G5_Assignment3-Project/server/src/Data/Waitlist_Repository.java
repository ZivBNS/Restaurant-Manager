package Data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import entities.Waitlist;

public class Waitlist_Repository implements Repository_Interface<Waitlist> {
    
    private DB_Controller db = DB_Controller.getInstance();
    private static Waitlist_Repository waitlistRepositoryInstance = new Waitlist_Repository();
    private List<Waitlist> activeWaitlist = new ArrayList<>();

    private Waitlist_Repository(){
    }

    public static Waitlist_Repository getInstance() {
        return waitlistRepositoryInstance;
    }

    @Override
    public void init() {
        String sql = "SELECT ID, ReservationID, Status, creationTime, TableFreedTime " +
                     "FROM Waitlist " +
                     "WHERE TableFreedTime IS NULL AND Status = 'WAITING' " +
                     "ORDER BY ID ASC";

        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            Connection conn = pConn.getConnection();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                activeWaitlist.clear();
                while (rs.next()) {
                    int id = rs.getInt("ID");
                    int resId = rs.getInt("ReservationID");
                    String status = rs.getString("Status");
                    LocalDateTime creationTime = rs.getTimestamp("creationTime").toLocalDateTime();
                    
                    LocalDateTime freedTime = null;
                    Timestamp freedTimestamp = rs.getTimestamp("TableFreedTime");
                    if (freedTimestamp != null) {
                        freedTime = freedTimestamp.toLocalDateTime();
                    }

                    Waitlist waitEntry = new Waitlist(id, resId, status, creationTime, freedTime);
                    activeWaitlist.add(waitEntry);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }
    
    @Override
    public boolean set(Waitlist objToSet) {
        String creationTimeStr = Timestamp.valueOf(LocalDateTime.now()).toString();
        String sql = "INSERT INTO Waitlist (ReservationID, Status, creationTime, TableFreedTime) VALUES (" +
                     objToSet.getReservation() + ", '" + 
                     objToSet.getStatus() + "', '" + 
                     creationTimeStr + "', NULL)";
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            pConn.getConnection().setAutoCommit(true);
            try (Statement stmt = pConn.getConnection().createStatement()) {
                return stmt.executeUpdate(sql) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    @Override
    public boolean update(Waitlist objToUpdate) {
        String freedTimeStr = (objToUpdate.getTableFreedTime() != null) ? 
                              "'" + Timestamp.valueOf(objToUpdate.getTableFreedTime()).toString() + "'" : "NULL";
        
        String sql = "UPDATE Waitlist SET Status = '" + objToUpdate.getStatus() + "', " +
                     "TableFreedTime = " + freedTimeStr + " " +
                     "WHERE ID = " + objToUpdate.getId();
        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            pConn.getConnection().setAutoCommit(true);
            try (Statement stmt = pConn.getConnection().createStatement()) {
                return stmt.executeUpdate(sql) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM Waitlist WHERE ID = " + id;        
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            pConn.getConnection().setAutoCommit(true);
            try (Statement stmt = pConn.getConnection().createStatement()) {
                return stmt.executeUpdate(sql) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
    }

    @Override
    public Waitlist getById(int id) {
        String sql = "SELECT * FROM Waitlist WHERE ID = " + id;
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    LocalDateTime creation = rs.getTimestamp("creationTime").toLocalDateTime();
                    LocalDateTime freed = (rs.getTimestamp("TableFreedTime") != null) ? 
                                           rs.getTimestamp("TableFreedTime").toLocalDateTime() : null;
                    return new Waitlist(rs.getInt("ID"), rs.getInt("ReservationID"), rs.getString("Status"), creation, freed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }

    public List<Waitlist> getWaitlistToday() {
        return activeWaitlist;
    }

    public void setWaitlistToday(List<Waitlist> waitlistToday) {
        this.activeWaitlist = waitlistToday;
    }

    public Waitlist getByReservationId(int rid) {
        String sql = "SELECT * FROM Waitlist WHERE ReservationID = " + rid;
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (Statement stmt = pConn.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    LocalDateTime creation = rs.getTimestamp("creationTime").toLocalDateTime();
                    LocalDateTime freed = (rs.getTimestamp("TableFreedTime") != null) ? 
                                           rs.getTimestamp("TableFreedTime").toLocalDateTime() : null;
                    return new Waitlist(rs.getInt("ID"), rs.getInt("ReservationID"), rs.getString("Status"), creation, freed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }
        return null;
    }
}