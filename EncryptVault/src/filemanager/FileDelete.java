package filemanager;

import db.ActivityLogger;
import db.DBConnection;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.swing.*;

// Handles deletion of user files from system and database
public class FileDelete {

    // Deletes selected file for the given user
    public static void delete(String username) {

        try {

            // Step 1: Retrieve user ID from database
            Connection con = DBConnection.getConnection();

            PreparedStatement getUser = con.prepareStatement(
                    "SELECT id FROM users WHERE username=?"
            );
            getUser.setString(1, username);

            ResultSet userRs = getUser.executeQuery();

            int userId = 0;

            if (userRs.next()) {
                userId = userRs.getInt("id");
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "User not found!",
                        "EncryptVault",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Step 2: Fetch files belonging to this user
            PreparedStatement ps = con.prepareStatement(
                    "SELECT original_name, stored_name FROM user_files WHERE user_id=?"
            );
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            ArrayList<String> originalNames = new ArrayList<>();
            ArrayList<String> storedNames = new ArrayList<>();

            while (rs.next()) {
                originalNames.add(rs.getString("original_name"));
                storedNames.add(rs.getString("stored_name"));
            }

            // If no files exist
            if (originalNames.isEmpty()) {
                JOptionPane.showMessageDialog(
                        null,
                        "No encrypted files found!",
                        "EncryptVault",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            // Step 3: Show original file names to user for selection
            String selectedFile = (String) JOptionPane.showInputDialog(
                    null,
                    "Select file to delete:",
                    "EncryptVault – Delete File",
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    originalNames.toArray(),
                    originalNames.get(0)
            );

            if (selectedFile == null) return;

            // Step 4: Confirm deletion from user
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to delete this file?\n" + selectedFile,
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) return;

            // Step 5: Get stored (encrypted) file name
            int index = originalNames.indexOf(selectedFile);
            String storedName = storedNames.get(index);

            // Step 6: Delete file from storage
            File encryptedFile = new File("data/users/" + username + "/" + storedName);

            if (encryptedFile.exists()) {
                encryptedFile.delete();
            }

            // Step 7: Remove file record from database
            PreparedStatement deletePs = con.prepareStatement(
                    "DELETE FROM user_files WHERE user_id=? AND stored_name=?"
            );

            deletePs.setInt(1, userId);
            deletePs.setString(2, storedName);
            deletePs.executeUpdate();

            JOptionPane.showMessageDialog(
                    null,
                    "✔ File deleted successfully!",
                    "EncryptVault",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // Log delete activity
            ActivityLogger.log(username, "Delete", selectedFile);

            con.close();

        } catch (Exception e) {
            e.printStackTrace();

            // Show error message if deletion fails
            JOptionPane.showMessageDialog(
                    null,
                    "❌ Error while deleting file!",
                    "EncryptVault Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}