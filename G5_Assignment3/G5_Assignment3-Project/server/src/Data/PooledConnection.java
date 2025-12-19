package Data;

public class PooledConnection {
    private java.sql.Connection connection;
    private long lastUsed;

    public PooledConnection(java.sql.Connection connection) {
        this.connection = connection;
        this.lastUsed = System.currentTimeMillis();
    }

    public java.sql.Connection getConnection() {
        return connection;
    }

    public void touch() {
        this.lastUsed = System.currentTimeMillis();
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void closePhysicalConnection() throws java.sql.SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
