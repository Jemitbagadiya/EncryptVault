package db;

import java.sql.Connection;
import java.sql.PreparedStatement;

// Utility class to log user activities into the database
public class ActivityLogger {

    // Inserts a new activity record for a user
    public static void log(String username, String action, String filename){
        try{
            // Get database connection
            Connection con = DBConnection.getConnection();

            // Prepare SQL statement to insert activity log
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO activity_log(username, action, filename) VALUES(?,?,?)"
            );

            // Set values for username, action, and filename
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, filename);

            // Execute insert operation
            ps.executeUpdate();

        }catch(Exception e){
            // Handle database errors
            e.printStackTrace();
        }
    }
}