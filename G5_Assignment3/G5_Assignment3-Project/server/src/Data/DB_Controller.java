package Data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Controller for managing a custom Database Connection Pool.
 * Implements the Singleton pattern to provide a centralized access point for 
 * database connections while optimizing performance through reuse.
 */
public class DB_Controller {
    
    /** The single instance of this controller. */
    private static DB_Controller instance;
    
    /** Database connection details. */
    private final String DB_URL = "jdbc:mysql://localhost:3306/bistro?serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
    private final String USER = "root";
    //private final String PASS = "zaqwsxcde321";
    //private final String PASS = "212009666";
    //private final String PASS = "8630547";
    private final String[] PASS = {"8630547","zaqwsxcde321","212009666"};
    private final int MAX_POOL_SIZE = 10;

    /** Thread-safe queue to store available pooled connections. */
    private BlockingQueue<PooledConnection> pool;

    /**
     * Private constructor to initialize the connection pool.
     * Uses a LinkedBlockingQueue to store pooled connections up to a defined limit.
     */
    private DB_Controller() {
        pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        System.out.println("[Pool] Initialized. Max Size: " + MAX_POOL_SIZE);
    }

    /**
     * Retrieves the singleton instance of the DB_Controller.
     * * @return The active DB_Controller instance.
     */
    public static synchronized DB_Controller getInstance() {
        if (instance == null)
            instance = new DB_Controller();
        return instance;
    }

    /**
     * Retrieves a connection from the pool. 
     * If the pool is empty, a new physical connection is created. 
     * If a connection is retrieved from the pool, its usage timestamp is updated.
     * * @return A PooledConnection object ready for database operations.
     * @throws SQLException If a database access error occurs during physical connection creation.
     */
    public PooledConnection getConnection() throws SQLException {
        // Attempt to retrieve a connection from the queue
        PooledConnection pConn = pool.poll();
        SQLException lastException = null;
        Connection conn=null;
        if (pConn == null) {
            // No available connections in pool, create a new physical one
            System.out.println("[Pool] Queue empty. Creating NEW physical connection!!!");
            //PASS are = 8630547, zaqwsxcde321, 212009666
            for (String password:PASS) {
            	try {
                    conn = DriverManager.getConnection(DB_URL, USER, password);
                    break;
				} catch (SQLException e) {
					if (e.getErrorCode() == 1045) { //if access denied- password not match issue
	                    lastException = e;
	                    continue;		//check the next password
					}
					else throw e;
				}
            }
            if (conn == null) {
                throw new SQLException("Failed to connect: All passwords rejected.", lastException);
            }
            pConn = new PooledConnection(conn);
        } else {
            // Connection found, update its 'last used' status before returning
            System.out.println("[Pool] Reusing existing connection.");
            pConn.touch();
        }
        return pConn;
    }

    /**
     * Returns a connection to the pool for future reuse.
     * If the pool has reached its maximum capacity, the physical connection is closed.
     * * @param pConn The PooledConnection object to be returned or closed.
     */
    public void releaseConnection(PooledConnection pConn) {
        if (pConn != null) {
            // Attempt to put the connection back into the queue
            boolean added = pool.offer(pConn);
            
            if (added) {
                System.out.println("[Pool] Connection returned. Current Pool Size: " + pool.size());
            } else {
                // If the pool is full, close the underlying physical connection to free resources
                try {
                    System.out.println("[Pool] Pool full. Closing physical connection.");
                    pConn.closePhysicalConnection();
                } catch (SQLException e) {
                    System.err.println("[Pool] Error while closing physical connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * Closes all physical connections currently stored in the pool.
     * This should be called when the server is shutting down to ensure
     * no orphan connections remain open in the database.
     */
    public void closePool() {
        System.out.println("[Pool] Closing connection pool...");
        
        // While there are connections in the queue, take them and close them physically
        while (!pool.isEmpty()) {
            PooledConnection pConn = pool.poll();
            if (pConn != null) {
                try {
                    pConn.closePhysicalConnection();
                } catch (SQLException e) {
                    System.err.println("[Pool] Error closing physical connection during shutdown: " + e.getMessage());
                }
            }
        }
        System.out.println("[Pool] All connections closed successfully.");
    }
    
    

}