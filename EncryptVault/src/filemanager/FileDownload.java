package filemanager;

import db.ActivityLogger;
import db.DBConnection;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.swing.*;
import security.AESUtil;

// Handles downloading and decrypting user files
public class FileDownload {

    // Downloads selected file for the given user and decrypts it
    public static void download(String username, String aesKey) {

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

            // If no files found
            if (originalNames.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No files found!");
                return;
            }

            // Show file selection dialog
            String selectedFile = (String) JOptionPane.showInputDialog(
                    null,
                    "Select file to download:",
                    "EncryptVault - Download",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    originalNames.toArray(),
                    originalNames.get(0)
            );

            if (selectedFile == null) return;

            // Get stored file name corresponding to selected file
            int index = originalNames.indexOf(selectedFile);
            String storedName = storedNames.get(index);

            // Locate encrypted file
            File encryptedFile = new File("data/users/" + username + "/" + storedName);

            if (!encryptedFile.exists()) {
                JOptionPane.showMessageDialog(null, "Encrypted file missing!");
                return;
            }

            // Open file chooser for save location
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setSelectedFile(new File(selectedFile));

            int saveResult = saveChooser.showSaveDialog(null);

            if (saveResult == JFileChooser.APPROVE_OPTION) {

                File saveLocation = saveChooser.getSelectedFile();

                // Decrypt file and save to selected location
                AESUtil.decryptFile(encryptedFile, saveLocation, aesKey);

                JOptionPane.showMessageDialog(
                        null,
                        "✔ File downloaded successfully!",
                        "EncryptVault",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            // Log download activity
            ActivityLogger.log(username, "Download", selectedFile);

            con.close();

        } catch (Exception e) {
            e.printStackTrace();

            // Show error message if download fails
            JOptionPane.showMessageDialog(
                    null,
                    "❌ Download failed!\n" + e.getMessage(),
                    "EncryptVault Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}