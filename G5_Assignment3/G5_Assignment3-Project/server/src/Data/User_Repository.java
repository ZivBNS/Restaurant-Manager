package Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import entities.UserRecord;

/**
 * Repository class for managing User entities in the database.
 * Implements the Singleton pattern and provides methods for authentication, 
 * user registration, and profile management for both customers and employees.
 */
public class User_Repository {
	
    private DB_Controller db = DB_Controller.getInstance();
    private static User_Repository userRepositoryInstance = new User_Repository();
    /** Generator for unique user/subscriber codes. */
    private static int userCodeGenerator = 100;  //added this to create unique user code for each

    private User_Repository() {}

    /**
     * Retrieves the singleton instance of the User_Repository.
     * @return The active User_Repository instance.
     */
    public static User_Repository getInstance() { return userRepositoryInstance; }
    
    /**
     * Generates the next unique user code in a thread-safe manner.
     * @return A unique integer user code.
     */
    //added this to create unique user code for each
    public synchronized int getNextUserCode() { return userCodeGenerator++; }

    /**
     * Initializes the repository by synchronizing the user code generator 
     * with the highest existing subscriber code in the database.
     */
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

    /**
     * Authenticates a user by username and password.
     * Filters out users with the 'Deleted' identity.
     * @param username The login username.
     * @param password The login password.
     * @return A populated UserRecord if credentials are valid, or null otherwise.
     */
	public UserRecord getByUsername(String username, String password) { 
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("SELECT * FROM users WHERE Username = ? AND Password = ?"
            		+ "AND identity <> 'Deleted'")) {
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
	
    /**
     * Authenticates an employee or manager by username and password.
     * @param username The login username.
     * @param password The login password.
     * @return A UserRecord if the user exists and has an 'Employee' or 'Manager' identity.
     */
	public UserRecord getEmpByUsername(String username, String password) { 
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement("SELECT * FROM users WHERE Username = ? AND Password = ?"
            		+ " AND (Identity = ? OR Identity = ?)")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, "Employee");
                pstmt.setString(4, "Manager");
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToUser(rs);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); } finally { if (pConn != null) db.releaseConnection(pConn); }
        return null;
    } 
	
    /**
     * Checks if a user exists with the specified email or phone number.
     * Used to prevent duplicate registrations.
     * @param email The email address to check.
     * @param phone The phone number to check.
     * @return true if a non-deleted user matches either contact detail.
     */
	public boolean getByEmailOrPhone(String email, String phone) {
        PooledConnection pConn = null;
        
        boolean hasEmail = (email != null && !email.trim().isEmpty());
        boolean hasPhone = (phone != null && !phone.isEmpty());

        try {
            pConn = db.getConnection();
            String sql;
            
            if (hasEmail) {
                sql = "SELECT 1 FROM users WHERE Email = ? AND identity <> 'Deleted' LIMIT 1";
            } else {
                sql = "SELECT 1 FROM users WHERE Phone = ? AND identity <> 'Deleted' LIMIT 1";
            }
            
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                int idx = 1;
                if (hasEmail) pstmt.setString(idx, email.trim());
                if (hasPhone) pstmt.setString(idx, String.valueOf(phone));

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
	
    /**
     * Retrieves a user record based on their internal database ID.
     * @param id The primary key ID of the user.
     * @return The UserRecord object or null if not found or deleted.
     */
	public UserRecord getByID(int id) {
        PooledConnection pConn = null;
        
        try {
            pConn = db.getConnection();
            String sql;

            sql = "SELECT ID, FirstName, LastName, Phone, Email,  Username, Password, subscriberCode, Identity"
            		+ " FROM users WHERE ID = ? AND identity <> 'Deleted' LIMIT 1";
            
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
	
    /**
     * Fetches all active (non-deleted) users from the database.
     * @return A list of all UserRecord objects, sorted by name.
     */
	public List<UserRecord> getAllSubscribedCustomers() {
		
		List<UserRecord> results = new ArrayList<>();
		PooledConnection pConn = null;
		
		try {
            pConn = db.getConnection();
            String sql = "SELECT ID, FirstName, LastName, Phone, Email,  Username, Password, subscriberCode, Identity "
            		+ "FROM users "
            		+ "WHERE identity <> 'Deleted'"
            		+ "ORDER BY LastName, FirstName";
            		
            
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
	
    /**
     * Maps a single row from a ResultSet into a UserRecord object.
     * @param rs The ResultSet cursor.
     * @return A populated UserRecord.
     * @throws SQLException If a database error occurs.
     */
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

    /**
     * Checks if a username is already taken.
     * @param username The username string to verify.
     * @return true if the username exists in the database.
     */
	public boolean existsByUsername(String username) {
        PooledConnection pConn = null;

        String sql =
            "SELECT 1 FROM users WHERE Username = ? LIMIT 1";

        try {
            pConn = db.getConnection();

            try (PreparedStatement ps =
                     pConn.getConnection().prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next(); // true if a row exists
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) db.releaseConnection(pConn);
        }

        return false;
    }

    /**
     * Checks whether an email exists on any other user than the provided ID.
     * Used to validate updates so a user can keep their own email without failing the uniqueness check.
     */
    public boolean existsByEmailExcludingId(String email, int excludeId) {
        if (email == null || email.trim().isEmpty()) return false;
        PooledConnection pConn = null;
        String sql = "SELECT 1 FROM users WHERE Email = ? AND identity <> 'Deleted' AND ID <> ? LIMIT 1";
        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setString(1, email.trim());
                ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
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

    /**
     * Checks whether a phone exists on any other user than the provided ID.
     * Used to validate updates so a user can keep their own phone without failing the uniqueness check.
     */
    public boolean existsByPhoneExcludingId(String phone, int excludeId) {
        if (phone == null || phone.isEmpty()) return false;
        PooledConnection pConn = null;
        String sql = "SELECT 1 FROM users WHERE Phone = ? AND identity <> 'Deleted' AND ID <> ? LIMIT 1";
        try {
            pConn = db.getConnection();
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql)) {
                ps.setString(1, phone);
                ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
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

    /**
     * Inserts a new user into the database and generates their unique subscriber code.
     * The process involves an initial insert followed by an update to set the subscriberCode.
     * @param user The UserRecord containing new account details.
     * @return true if the user was successfully registered.
     */
	public boolean addNewUser(UserRecord user) {
		
		String sql = """
		        INSERT INTO users
		        (FirstName, LastName, Phone, Email, Username, Password, subscriberCode, Identity)
		        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
		    """;
		
		String updateSql = "UPDATE users SET subscriberCode = ? WHERE id = ?";
		
		PooledConnection pConn = null;
		
		try {
            pConn = db.getConnection();
            
            try (PreparedStatement ps = pConn.getConnection().prepareStatement(sql,
            		Statement.RETURN_GENERATED_KEYS)) { //we get the generated ID here 

            	ps.setString(1, user.getFirstName());
                ps.setString(2, user.getLastName());
                ps.setString(3, user.getPhone());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getUsername());
                ps.setString(6, user.getPassword());
                ps.setNull(7, Types.INTEGER);
                ps.setString(8, user.getIdentity());
                
                int affectedRows = ps.executeUpdate();
                if (affectedRows != 1) {
                    return false;
                }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        int subscriberCode = (int) (100000 + generatedId);
                        
                        try (PreparedStatement ps2 = pConn.getConnection().prepareStatement(updateSql)) {
                        	ps2.setInt(1, subscriberCode);
                            ps2.setInt(2, generatedId);

                            int updated = ps2.executeUpdate();
                            if (updated != 1) {
                                return false;
                            }
                            return true;
                        }
                        
                    } else {
                        throw new SQLException("User inserted but no ID returned.");
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

	/**
     * Updates an existing user record in the database.
     * @param u The UserRecord containing updated information.
     * @return true if the update affected exactly one row.
     */
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
	
    /**
     * Performs a soft delete by changing the user's identity to 'Deleted'.
     * @param u The user record to deactivate.
     * @return true if the operation was successful.
     */
	public boolean deleteUserByID(UserRecord u) {
	    PooledConnection pConn = null;

	    String sql = "UPDATE users SET identity = 'Deleted' WHERE ID = ?";

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
    
	/**
     * Retrieves a user by their unique Subscriber Code (e.g., 100001).
     * Useful for manual entry forms.
     * @param code The numeric subscriber code.
     * @return The UserRecord if found and not deleted, or null otherwise.
     */
    public UserRecord getBySubscriberCode(int code) {
        PooledConnection pConn = null;
        try {
            pConn = db.getConnection();
            String sql = "SELECT * FROM users WHERE subscriberCode = ? AND identity <> 'Deleted' LIMIT 1";
            
            try (PreparedStatement pstmt = pConn.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, code);
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
	
}