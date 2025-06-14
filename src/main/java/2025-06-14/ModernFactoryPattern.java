package patterns.creational;

import java.util.*;
import java.util.function.*;

/**
 * Modern Factory Pattern Implementation
 * Demonstrates type-safe factory using generics and functional programming
 * 
 * Benefits:
 * - Type safety at compile time
 * - Extensible without modifying existing code
 * - Functional programming approach
 * - Thread-safe implementation
 */
public class ModernFactoryPattern {
    
    // Abstract product interface
    public interface DatabaseConnection {
        void connect();
        void disconnect();
        String getConnectionString();
        DatabaseType getType();
    }
    
    // Database types enum
    public enum DatabaseType {
        MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
        POSTGRESQL("PostgreSQL", "org.postgresql.Driver"),
        MONGODB("MongoDB", "mongodb://"),
        REDIS("Redis", "redis://");
        
        private final String displayName;
        private final String driver;
        
        DatabaseType(String displayName, String driver) {
            this.displayName = displayName;
            this.driver = driver;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDriver() { return driver; }
    }
    
    // Concrete implementations
    public static class MySQLConnection implements DatabaseConnection {
        private final String host;
        private final int port;
        private final String database;
        private boolean connected = false;
        
        public MySQLConnection(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
        
        @Override
        public void connect() {
            System.out.println("Connecting to MySQL: " + getConnectionString());
            connected = true;
        }
        
        @Override
        public void disconnect() {
            System.out.println("Disconnecting from MySQL");
            connected = false;
        }
        
        @Override
        public String getConnectionString() {
            return String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        }
        
        @Override
        public DatabaseType getType() {
            return DatabaseType.MYSQL;
        }
    }
    
    public static class PostgreSQLConnection implements DatabaseConnection {
        private final String host;
        private final int port;
        private final String database;
        private boolean connected = false;
        
        public PostgreSQLConnection(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
        
        @Override
        public void connect() {
            System.out.println("Connecting to PostgreSQL: " + getConnectionString());
            connected = true;
        }
        
        @Override
        public void disconnect() {
            System.out.println("Disconnecting from PostgreSQL");
            connected = false;
        }
        
        @Override
        public String getConnectionString() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        }
        
        @Override
        public DatabaseType getType() {
            return DatabaseType.POSTGRESQL;
        }
    }
    
    public static class MongoDBConnection implements DatabaseConnection {
        private final String host;
        private final int port;
        private final String database;
        private boolean connected = false;
        
        public MongoDBConnection(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
        
        @Override
        public void connect() {
            System.out.println("Connecting to MongoDB: " + getConnectionString());
            connected = true;
        }
        
        @Override
        public void disconnect() {
            System.out.println("Disconnecting from MongoDB");
            connected = false;
        }
        
        @Override
        public String getConnectionString() {
            return String.format("mongodb://%s:%d/%s", host, port, database);
        }
        
        @Override
        public DatabaseType getType() {
            return DatabaseType.MONGODB;
        }
    }
    
    // Configuration class for database parameters
    public static class DatabaseConfig {
        private final String host;
        private final int port;
        private final String database;
        private final Map<String, String> properties;
        
        private DatabaseConfig(Builder builder) {
            this.host = builder.host;
            this.port = builder.port;
            this.database = builder.database;
            this.properties = new HashMap<>(builder.properties);
        }
        
        // Builder pattern for configuration
        public static class Builder {
            private String host = "localhost";
            private int port;
            private String database;
            private Map<String, String> properties = new HashMap<>();
            
            public Builder host(String host) {
                this.host = host;
                return this;
            }
            
            public Builder port(int port) {
                this.port = port;
                return this;
            }
            
            public Builder database(String database) {
                this.database = database;
                return this;
            }
            
            public Builder property(String key, String value) {
                this.properties.put(key, value);
                return this;
            }
            
            public DatabaseConfig build() {
                return new DatabaseConfig(this);
            }
        }
        
        // Getters
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
        public Map<String, String> getProperties() { return properties; }
    }
    
    // Modern factory using functional interfaces
    public static class DatabaseConnectionFactory {
        
        // Factory registry using functional interfaces
        private static final Map<DatabaseType, Function<DatabaseConfig, DatabaseConnection>> FACTORY_MAP = 
            new EnumMap<>(DatabaseType.class);
        
        // Static initialization of factory methods
        static {
            FACTORY_MAP.put(DatabaseType.MYSQL, config -> 
                new MySQLConnection(config.getHost(), config.getPort(), config.getDatabase()));
                
            FACTORY_MAP.put(DatabaseType.POSTGRESQL, config -> 
                new PostgreSQLConnection(config.getHost(), config.getPort(), config.getDatabase()));
                
            FACTORY_MAP.put(DatabaseType.MONGODB, config -> 
                new MongoDBConnection(config.getHost(), config.getPort(), config.getDatabase()));
        }
        
        // Factory method with type safety
        public static <T extends DatabaseConnection> T createConnection(
                DatabaseType type, DatabaseConfig config, Class<T> expectedType) {
            
            Function<DatabaseConfig, DatabaseConnection> factory = FACTORY_MAP.get(type);
            if (factory == null) {
                throw new IllegalArgumentException("Unsupported database type: " + type);
            }
            
            DatabaseConnection connection = factory.apply(config);
            
            // Type safety check
            if (!expectedType.isInstance(connection)) {
                throw new ClassCastException(
                    String.format("Expected %s but got %s", 
                                expectedType.getSimpleName(), 
                                connection.getClass().getSimpleName()));
            }
            
            return expectedType.cast(connection);
        }
        
        // Convenient overloaded method
        public static DatabaseConnection createConnection(DatabaseType type, DatabaseConfig config) {
            return createConnection(type, config, DatabaseConnection.class);
        }
        
        // Register new factory (extensibility)
        public static void registerFactory(DatabaseType type, 
                Function<DatabaseConfig, DatabaseConnection> factory) {
            FACTORY_MAP.put(type, factory);
        }
        
        // Get all supported types
        public static Set<DatabaseType> getSupportedTypes() {
            return Collections.unmodifiableSet(FACTORY_MAP.keySet());
        }
    }
    
    // Connection pool using factory
    public static class DatabaseConnectionPool {
        private final DatabaseType type;
        private final DatabaseConfig config;
        private final Queue<DatabaseConnection> availableConnections;
        private final Set<DatabaseConnection> usedConnections;
        private final int maxPoolSize;
        
        public DatabaseConnectionPool(DatabaseType type, DatabaseConfig config, int maxPoolSize) {
            this.type = type;
            this.config = config;
            this.maxPoolSize = maxPoolSize;
            this.availableConnections = new ArrayDeque<>();
            this.usedConnections = new HashSet<>();
            
            // Pre-populate pool
            for (int i = 0; i < Math.min(3, maxPoolSize); i++) {
                DatabaseConnection connection = DatabaseConnectionFactory.createConnection(type, config);
                availableConnections.offer(connection);
            }
        }
        
        public synchronized DatabaseConnection getConnection() {
            if (availableConnections.isEmpty() && usedConnections.size() < maxPoolSize) {
                DatabaseConnection newConnection = DatabaseConnectionFactory.createConnection(type, config);
                availableConnections.offer(newConnection);
            }
            
            DatabaseConnection connection = availableConnections.poll();
            if (connection != null) {
                usedConnections.add(connection);
                connection.connect();
            }
            
            return connection;
        }
        
        public synchronized void releaseConnection(DatabaseConnection connection) {
            if (usedConnections.remove(connection)) {
                connection.disconnect();
                availableConnections.offer(connection);
            }
        }
        
        public synchronized void closeAll() {
            availableConnections.forEach(DatabaseConnection::disconnect);
            usedConnections.forEach(DatabaseConnection::disconnect);
            availableConnections.clear();
            usedConnections.clear();
        }
        
        public synchronized int getAvailableCount() {
            return availableConnections.size();
        }
        
        public synchronized int getUsedCount() {
            return usedConnections.size();
        }
    }
    
    // Demonstration and testing
    public static void main(String[] args) {
        System.out.println("=== Modern Factory Pattern Demo ===\n");
        
        // 1. Basic factory usage
        System.out.println("1. Basic Factory Usage:");
        DatabaseConfig mysqlConfig = new DatabaseConfig.Builder()
            .host("localhost")
            .port(3306)
            .database("myapp")
            .property("useSSL", "true")
            .build();
            
        DatabaseConnection mysqlConn = DatabaseConnectionFactory.createConnection(
            DatabaseType.MYSQL, mysqlConfig);
        mysqlConn.connect();
        System.out.println("Created: " + mysqlConn.getConnectionString());
        mysqlConn.disconnect();
        
        // 2. Type-safe factory usage
        System.out.println("\n2. Type-Safe Factory Usage:");
        MySQLConnection typedMysqlConn = DatabaseConnectionFactory.createConnection(
            DatabaseType.MYSQL, mysqlConfig, MySQLConnection.class);
        typedMysqlConn.connect();
        
        // 3. Connection pool demonstration
        System.out.println("\n3. Connection Pool Demo:");
        DatabaseConnectionPool pool = new DatabaseConnectionPool(
            DatabaseType.POSTGRESQL, 
            new DatabaseConfig.Builder()
                .host("localhost")
                .port(5432)
                .database("testdb")
                .build(),
            5
        );
        
        System.out.println("Initial pool - Available: " + pool.getAvailableCount() + 
                          ", Used: " + pool.getUsedCount());
        
        // Get connections
        DatabaseConnection conn1 = pool.getConnection();
        DatabaseConnection conn2 = pool.getConnection();
        
        System.out.println("After getting 2 connections - Available: " + pool.getAvailableCount() + 
                          ", Used: " + pool.getUsedCount());
        
        // Release connections
        pool.releaseConnection(conn1);
        pool.releaseConnection(conn2);
        
        System.out.println("After releasing connections - Available: " + pool.getAvailableCount() + 
                          ", Used: " + pool.getUsedCount());
        
        // 4. Extensibility demonstration
        System.out.println("\n4. Factory Extensibility:");
        System.out.println("Supported types: " + DatabaseConnectionFactory.getSupportedTypes());
        
        pool.closeAll();
        System.out.println("\nDemo completed successfully!");
    }
}