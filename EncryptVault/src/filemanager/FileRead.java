package filemanager;

import db.ActivityLogger;
import db.DBConnection;
import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.swing.*;
import security.AESUtil;

// Handles reading (opening) user files after decrypting them
public class FileRead {

    // Opens selected file securely for the given user
    public static void read(String username, String aesKey) {

        try {

            // Get database connection
            Connection con = DBConnection.getConnection();

            // Retrieve user ID from database
            PreparedStatement getUser = con.prepareStatement(
                    "SELECT id FROM users WHERE username = ?"
            );
            getUser.setString(1, username);
            ResultSet userRs = getUser.executeQuery();

            int userId = 0;

            if (userRs.next()) {
                userId = userRs.getInt("id");
            } else {
                JOptionPane.showMessageDialog(null, "User not found!");
                return;
            }

            // Fetch files belonging to the user
            PreparedStatement ps = con.prepareStatement(
                    "SELECT original_name, stored_name FROM user_files WHERE user_id = ?"
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

            // Show file selection dialog
            String selectedFile = (String) JOptionPane.showInputDialog(
                    null,
                    "Select file to open:",
                    "EncryptVault - Your Files",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    originalNames.toArray(),
                    originalNames.get(0)
            );

            if (selectedFile == null) return;

            // Get stored file name corresponding to selected file
            int index = originalNames.indexOf(selectedFile);
            String storedName = storedNames.get(index);

            // Locate encrypted file in storage
            File encryptedFile = new File("data/users/" + username + "/" + storedName);

            if (!encryptedFile.exists()) {
                JOptionPane.showMessageDialog(null, "Encrypted file missing!");
                return;
            }

            // Create temporary file for decrypted content
            File tempFile = File.createTempFile("decrypt_", "_" + selectedFile);

            // Decrypt encrypted file into temporary file
            AESUtil.decryptFile(encryptedFile, tempFile, aesKey);

            // Open decrypted file using system default application
            Desktop.getDesktop().open(tempFile);

            JOptionPane.showMessageDialog(
                    null,
                    "✔ File opened securely.",
                    "EncryptVault",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // Ensure temporary file is deleted when application exits
            tempFile.deleteOnExit();

            // Log read activity
            ActivityLogger.log(username, "Read", selectedFile);
            
            con.close();

        } catch (Exception e) {
            e.printStackTrace();

            // Show error message if decryption or opening fails
            JOptionPane.showMessageDialog(
                    null,
                    "❌ File decryption failed!\n" + e.getMessage(),
                    "EncryptVault Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}