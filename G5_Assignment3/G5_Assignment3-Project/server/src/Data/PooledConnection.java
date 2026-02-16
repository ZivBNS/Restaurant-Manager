package Data;

import java.sql.Connection;

/**
 * A wrapper class for a physical database connection within a connection pool.
 * It tracks the activity of the connection using a timestamp to determine
 * its idle time and usage.
 */
public class PooledConnection {
    private java.sql.Connection connection;
    private long lastUsed;

    /**
     * Constructs a PooledConnection wrapper around a given JDBC connection.
     * Sets the lastUsed timestamp to the current system time.
     * * @param connection the physical java.sql.Connection to wrap.
     */
    public PooledConnection(Connection connection) {
        this.connection = connection;
        this.lastUsed = System.currentTimeMillis();
    }

    /**
     * Returns the underlying physical database connection.
     * * @return the wrapped java.sql.Connection object.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Updates the lastUsed timestamp to the current system time.
     * This should be called whenever the connection is retrieved or reused from the pool.
     */
    public void touch() {
        this.lastUsed = System.currentTimeMillis();
    }

    /**
     * Retrieves the timestamp of when this connection was last accessed.
     * * @return the lastUsed time in milliseconds.
     */
    public long getLastUsed() {
        return lastUsed;
    }

    /**
     * Closes the underlying physical database connection if it is not already closed.
     * * @throws java.sql.SQLException if a database access error occurs.
     */
    public void closePhysicalConnection() throws java.sql.SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}