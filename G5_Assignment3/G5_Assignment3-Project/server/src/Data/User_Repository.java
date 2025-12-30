package Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import entities.Subscribed_Customer;
import entities.UserRecord;

public class User_Repository {
	
    private DB_Controller db = DB_Controller.getInstance();
    private static User_Repository userRepositoryInstance = new User_Repository();
    private static int userCodeGenerator = 100;  //added this to create unique user code for each

    private User_Repository() {}

    public static User_Repository getInstance() { return userRepositoryInstance; }
    
    //added this to create unique user code for each
    public synchronized int getNextUserCode() { return userCodeGenerator++; }

    public void init() { /* Same logic using Pool and MAX(ConfirmationCode) */ 
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            String query = "SELECT MAX(subscriberCode) FROM Users";
            try (Statement stmt = pConn.getConnection().createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) userCodeGenerator = Math.max(100, rs.getInt(1) + 1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { if (pConn != null) db.releaseConnection(pConn); }
    }

	public UserRecord getByUsername(String username, String password) { /* Patterned pool fetch... */ 
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
	
	public UserRecord getByID(int id) {
        PooledConnection pConn = null;
        
        try {
            pConn = db.getConnection();
            String sql;

            sql = "SELECT ID, FirstName, LastName, Phone, Email,  Username, Password, subscriberCode, Identity"
            		+ " FROM users WHERE ID = ? LIMIT 1";
            
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToUser(rs);
                    }
                }
            }
            
        } catch (SQLException e) {
        	e.printStackTrace(); 
        } finally { 
        	if (pConn != null) db.releaseConnection(pConn); 
        }
        return null;
    }
	
	public List<UserRecord> getAllSubscribedCustomers() {
		
		List<UserRecord> results = new ArrayList<>();
		PooledConnection pConn = null;
		
		try {
            pConn = db.getConnection();
            String sql = "SELECT ID, FirstName, LastName, Phone, Email,  Username, Password, subscriberCode, Identity "
            		+ "FROM users "
            		+ "ORDER BY LastName, FirstName;";
            
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                    	int id = rs.getInt("ID");
                        String firstName = rs.getString("FirstName");
                        String lastName  = rs.getString("LastName");
                        String phone     = rs.getString("Phone");
                        String email     = rs.getString("Email");
                        String username  = rs.getString("Username");
                        String password  = rs.getString("Password");
                        int code = rs.getInt("subscriberCode");
                        String identity = rs.getString("Identity");
                        
                        results.add(new UserRecord(
                                id, firstName, lastName, phone, email, username, password, identity, code
                        ));
                    };
                }
            }
            
        } catch (SQLException e) {
        	e.printStackTrace(); 
        } finally { 
        	if (pConn != null) db.releaseConnection(pConn); 
        }
		System.out.println("returns user list of size : " + results.size());
		return results;
	}
	
	private UserRecord mapRowToUser(ResultSet rs) throws SQLException {
        return new UserRecord(

        	rs.getInt("ID"),
            rs.getString("firstName"),
            rs.getString("lastName"),
            rs.getString("Phone"),
            rs.getString("Email"),
            rs.getString("Username"),
            rs.getString("Password"),
            rs.getString("Identity"),
            rs.getInt("SubscriberCode")
        );
    }

	public boolean existsByUsername(String username) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean addNewUser(UserRecord user) {
		// TODO Auto-generated method stub
		
		String sql = """
		        INSERT INTO users
		        (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity)
		        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
		    """;
		
		PooledConnection pConn = null;
		
		try {
            pConn = db.getConnection();
            
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {

            	ps.setString(1, user.getFirstName());
                ps.setString(2, user.getLastName());
                ps.setString(3, user.getPhone());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getUsername());
                ps.setString(6, user.getPassword());

                // subscriberCode can be NULL
                if (user.getSubscriberCode() != null) {
                    ps.setInt(7, user.getSubscriberCode());
                } else {
                    ps.setNull(7, Types.INTEGER);
                }

                ps.setString(8, user.getIdentity());

                return ps.executeUpdate() == 1; // true if exactly 1 row inserted
                
            }
            
        } catch (SQLException e) {
        	e.printStackTrace(); 
        } finally { 
        	if (pConn != null) db.releaseConnection(pConn); 
        }
		
		
		return false;
	}

	
	public boolean updateUser(UserRecord u) {
	    PooledConnection pConn = null;

	    String sql = """
	        UPDATE users
	        SET FirstName = ?, LastName = ?, Phone = ?, Email = ?, Username = ?, Password = ?, subscriberCode = ?, Identity = ?
	        WHERE ID = ?
	    """;

	    try {
	        pConn = db.getConnection();

	        try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
	            ps.setString(1, u.getFirstName());
	            ps.setString(2, u.getLastName());
	            ps.setString(3, u.getPhone());
	            ps.setString(4, u.getEmail());
	            ps.setString(5, u.getUsername());
	            ps.setString(6, u.getPassword());          
	            ps.setInt(7, u.getSubscriberCode());
	            ps.setString(8, u.getIdentity());
	            ps.setInt(9, u.getId());

	            return ps.executeUpdate() == 1; // true if exactly 1 row updated
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    } finally {
	        if (pConn != null) db.releaseConnection(pConn);
	    }
	}
	
	public boolean deleteUserByID(UserRecord u) {
	    PooledConnection pConn = null;

	    String sql = "DELETE FROM users WHERE ID = ?";

	    try {
	        pConn = db.getConnection();

	        try (PreparedStatement ps =
	                 pConn.getConnection().prepareStatement(sql)) {

	            ps.setLong(1, u.getId());

	            return ps.executeUpdate() == 1;
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        if (pConn != null) db.releaseConnection(pConn);
	    }

	    return false;
	}
	
}
