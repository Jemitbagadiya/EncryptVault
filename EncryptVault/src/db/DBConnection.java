package db;

import java.sql.Connection;
import java.sql.DriverManager;

// Utility class for establishing database connection
public class DBConnection {

    // Returns a connection object to the MySQL database
    public static Connection getConnection() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection using database URL, username, and password
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/encryptvault",
                "root",
                "" // Mysql password
            );
        } catch (Exception e) {
            // Handle connection errors
            e.printStackTrace();
        }

        // Return null if connection fails
        return null;
    }
}