package Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entities.Reservation;
import entities.Subscribed_Customer;

public class User_Repository {
	
    private DB_Controller db = DB_Controller.getInstance();
    private static User_Repository userRepositoryInstance = new User_Repository();
    
    private User_Repository() {}

    public static User_Repository getInstance() { return userRepositoryInstance; }

	public Subscribed_Customer getByUsername(String username, String password) { /* Patterned pool fetch... */ 
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("SELECT * FROM users WHERE Username = ? AND Password = ?")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToUser(rs);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); } finally { if (pConn != null) db.releaseConnection(pConn); }
        return null;
    }
	
	public boolean getByEmailOrPhone(String email, int phone) {
        PooledConnection pConn = null;
        
        boolean hasEmail = (email != null && !email.trim().isEmpty());
        boolean hasPhone = (phone != 0);
        try {
            pConn = db.getConnection();
            String sql;
            
            if (hasEmail && hasPhone) { //TODO: remove unneeded check
                sql = "SELECT 1 FROM users WHERE Email = ? OR Phone = ? LIMIT 1";
            } else if (hasEmail) {
                sql = "SELECT 1 FROM users WHERE Email = ? LIMIT 1";
            } else {
                sql = "SELECT 1 FROM users WHERE Phone = ? LIMIT 1";
            }
            
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                int idx = 1;
                if (hasEmail) pstmt.setString(idx++, email.trim());
                if (hasPhone) pstmt.setString(idx++, String.valueOf(phone));

                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
            
        } catch (SQLException e) {
        	e.printStackTrace(); 
        } finally { 
        	if (pConn != null) db.releaseConnection(pConn); 
        }
        return false;
    }
	
	public List<Subscribed_Customer> getAllSubscribedCustomers() {
		
		List<Subscribed_Customer> results = new ArrayList<>();
		PooledConnection pConn = null;
		
		try {
            pConn = db.getConnection();
            String sql = "SELECT * FROM users";
            
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String firstName = rs.getString("FirstName");
                        String lastName  = rs.getString("LastName");
                        String phone     = rs.getString("Phone");
                        String email     = rs.getString("Email");
                        String username  = rs.getString("Username");
                        String password  = rs.getString("Password");
                    };
                }
            }
            
        } catch (SQLException e) {
        	e.printStackTrace(); 
        } finally { 
        	if (pConn != null) db.releaseConnection(pConn); 
        }
		
		return null;
	}
	
	private Subscribed_Customer mapRowToUser(ResultSet rs) throws SQLException {
        return new Subscribed_Customer(

            rs.getString("firstName"),
            rs.getString("lastName"),
            rs.getString("Phone"),
            rs.getString("Email"),
            rs.getString("Username"),
            rs.getString("Password")
        );
    }

	
}
