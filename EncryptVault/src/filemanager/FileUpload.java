package filemanager;

import db.ActivityLogger;
import db.DBConnection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import security.AESUtil;

// Handles file upload, encryption, and storage
public class FileUpload {

    // Encrypts selected file and stores it for the given user
    public static void upload(String username, String aesKey) {

        try {

            // Set Nimbus look and feel if available
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }

            // File chooser initialized to Desktop directory
            JFileChooser chooser = new JFileChooser(
                    new File(System.getProperty("user.home"), "Desktop")
            );

            chooser.setDialogTitle("EncryptVault – Select File to Encrypt");
            chooser.setPreferredSize(new java.awt.Dimension(900, 550));

            // File filters for allowed types
            FileNameExtensionFilter docs = new FileNameExtensionFilter(
                    "Documents (PDF, DOCX, TXT)", "pdf", "docx", "txt"
            );

            FileNameExtensionFilter images = new FileNameExtensionFilter(
                    "Images (JPG, PNG)", "jpg", "jpeg", "png"
            );

            chooser.addChoosableFileFilter(docs);
            chooser.addChoosableFileFilter(images);
            chooser.setAcceptAllFileFilterUsed(true);

            int result = chooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {

                // Selected file
                File original = chooser.getSelectedFile();
                long fileSize = original.length();

                // Generate random encrypted file name
                String storedName = UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8) + ".enc";

                // Create user directory if not exists
                File userDir = new File("data/users/" + username);
                if (!userDir.exists()) {
                    userDir.mkdirs();
                }

                // Destination encrypted file
                File encrypted = new File(userDir, storedName);

                // Encrypt original file
                AESUtil.encryptFile(original, encrypted, aesKey);

                // Attempt to delete original file after encryption
                System.out.println("Trying to delete: " + original.getAbsolutePath());

                try {
                    Path path = original.toPath();
                    Files.delete(path);

                    JOptionPane.showMessageDialog(
                            null,
                            "✔ File encrypted successfully!",
                            "EncryptVault",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (Exception ex) {

                    JOptionPane.showMessageDialog(
                            null,
                            "⚠ File encrypted BUT original file NOT deleted!\n\nReason:\n" + ex.getMessage(),
                            "Warning",
                            JOptionPane.WARNING_MESSAGE
                    );

                    // Schedule deletion on exit if immediate delete fails
                    original.deleteOnExit();
                }

                // Store file metadata in database
                Connection con = DBConnection.getConnection();

                PreparedStatement getUser = con.prepareStatement(
                        "SELECT id FROM users WHERE username = ?"
                );
                getUser.setString(1, username);

                ResultSet rs = getUser.executeQuery();
                int userId = 0;

                if (rs.next()) {
                    userId = rs.getInt("id");
                }

                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO user_files (user_id, original_name, stored_name, file_size) VALUES (?, ?, ?, ?)"
                );

                ps.setInt(1, userId);
                ps.setString(2, original.getName());
                ps.setString(3, storedName);
                ps.setLong(4, fileSize);

                ps.executeUpdate();

                // Log upload activity
                ActivityLogger.log(username, "Upload", original.getName());

                con.close();
            }

        } catch (Exception e) {
            e.printStackTrace();

            // Show error if encryption fails
            JOptionPane.showMessageDialog(
                    null,
                    "❌ File encryption failed!\n" + e.getMessage(),
                    "EncryptVault Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}