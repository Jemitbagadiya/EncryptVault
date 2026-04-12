package settings;

import auth.EmailSender;
import db.DBConnection;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Base64;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

// Settings screen to update username, email, and password with OTP verification
public class SettingsFrame extends JFrame {

    // User data
    private String currentUsername;
    private String oldUsername, oldEmail, oldPassword;
    private int userId;

    // UI components
    JTextField usernameField, emailField, otpField;
    JPasswordField oldPass, newPass, confirmPass;
    JLabel timerLabel, resendLink;

    // OTP handling
    Timer timer;
    int timeLeft = 180;
    long otpTime;

    JButton saveBtn, sendOtpBtn;
    JFrame parent; 

    String generatedOTP = "";
    String otpEmail;
    boolean otpVerified = false;

    // UI colors
    Color BG = new Color(18,18,18);
    Color CARD = new Color(30,30,30);
    Color FIELD = new Color(45,45,45);

    public SettingsFrame(String username, JFrame parent) {

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.currentUsername = username;
        this.oldUsername = username;
        this.parent = parent;

        // Frame setup
        setTitle("Settings - EncryptVault");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // Load user data from database
        loadUserData();

        // Main card panel
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 600));
        card.setMaximumSize(new Dimension(420, 600));
        card.setBorder(BorderFactory.createEmptyBorder(30,30,30,30));

        JLabel title = new JLabel("Update Profile");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Input fields
        usernameField = createField("Username");
        usernameField.setText(oldUsername);

        emailField = createField("Email");
        emailField.setText(oldEmail);

        // Reset OTP when email changes
        emailField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { resetOTP(); }
            public void removeUpdate(DocumentEvent e) { resetOTP(); }
            public void changedUpdate(DocumentEvent e) { resetOTP(); }
        });

        // Password fields
        oldPass = createPassword("Old Password");
        newPass = createPassword("New Password");
        confirmPass = createPassword("Confirm Password");

        // OTP field disabled initially
        otpField = createField("OTP");
        otpField.setEnabled(false);

        // Timer label
        timerLabel = new JLabel("");
        timerLabel.setForeground(Color.ORANGE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Resend OTP link
        resendLink = new JLabel("Resend OTP");
        resendLink.setForeground(new Color(180,180,180));
        resendLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resendLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        resendLink.setVisible(false);

        // Hover and click behavior for resend link
        resendLink.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                resendLink.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                resendLink.setForeground(new Color(180,180,180));
            }
            public void mouseClicked(MouseEvent e) {
                sendOTP();
            }
        });

        // Buttons
        sendOtpBtn = createRoundedButton("Send OTP", new Color(255,140,0), 100, 35);
        saveBtn = createRoundedButton("Save Changes", new Color(40,167,69), 130, 40);

        saveBtn.setEnabled(false);
        saveBtn.setBackground(new Color(40,167,69));

        sendOtpBtn.addActionListener(e -> sendOTP());
        saveBtn.addActionListener(e -> updateProfile());

        // OTP validation listener
        otpField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { checkOTP(); }
            public void removeUpdate(DocumentEvent e) { checkOTP(); }
            public void changedUpdate(DocumentEvent e) { checkOTP(); }
        });

        // Build UI layout
        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(15));
        card.add(emailField);
        card.add(Box.createVerticalStrut(15));
        card.add(oldPass);
        card.add(Box.createVerticalStrut(15));
        card.add(newPass);
        card.add(Box.createVerticalStrut(15));
        card.add(confirmPass);
        card.add(Box.createVerticalStrut(15));
        card.add(sendOtpBtn);
        card.add(Box.createVerticalStrut(15));
        card.add(otpField);
        card.add(Box.createVerticalStrut(15));
        card.add(timerLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(resendLink);
        card.add(Box.createVerticalStrut(25));
        card.add(saveBtn);

        // Back button panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        topPanel.setBackground(BG);

        JButton backBtn = new JButton("← Back");
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(30,30,30));
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Restore old data and go back
        backBtn.addActionListener(e -> {
            usernameField.setText(oldUsername);
            emailField.setText(oldEmail);
            dispose();
            parent.setVisible(true);
        });

        topPanel.add(backBtn);

        // Center layout
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        centerPanel.add(card, gbc);

        // Absolute layout container
        JPanel mainPanel = new JPanel(null);
        mainPanel.setBackground(BG);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        int cardWidth = 420;
        int cardHeight = 600;

        int x = (screen.width - cardWidth) / 2;
        int y = (screen.height - cardHeight) / 2;

        card.setBounds(x, y, cardWidth, cardHeight);
        topPanel.setBounds(35, 25, 200, 50);

        mainPanel.add(card);
        mainPanel.add(topPanel);

        add(mainPanel);

        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
    }

    // Sends OTP to entered email
    void sendOTP(){

        otpField.setText("");
        generatedOTP = "";
        otpVerified = false;

        saveBtn.setEnabled(false);
        otpField.setEnabled(false);

        String email = emailField.getText().trim();

        if(email.isEmpty()){
            JOptionPane.showMessageDialog(this,"Enter email first");
            return;
        }

        Color originalColor = new Color(255,140,0);

        // Update button to sending state
        sendOtpBtn.setBackground(new Color(120,120,120));
        sendOtpBtn.setText("Sending...");
        sendOtpBtn.setEnabled(false);

        new Thread(() -> {
            try{
                generatedOTP = String.valueOf((int)(Math.random()*900000)+100000);
                otpEmail = emailField.getText().trim();

                EmailSender.sendOTP(email, generatedOTP);

                SwingUtilities.invokeLater(() -> {

                    otpField.setEnabled(true);

                    otpTime = System.currentTimeMillis();
                    startTimer();

                    // Show success state
                    sendOtpBtn.setBackground(new Color(40,167,69));
                    sendOtpBtn.setText("Sent");

                    // Restore button after delay
                    javax.swing.Timer t = new javax.swing.Timer(1500, e -> {
                        sendOtpBtn.setBackground(originalColor);
                        sendOtpBtn.setText("Send OTP");
                        sendOtpBtn.setEnabled(true);
                    });
                    t.setRepeats(false);
                    t.start();

                });

            }catch(Exception e){
                e.printStackTrace();

                SwingUtilities.invokeLater(() -> {
                    sendOtpBtn.setBackground(originalColor);
                    sendOtpBtn.setText("Send OTP");
                    sendOtpBtn.setEnabled(true);
                });
            }
        }).start();
    }

    // Starts OTP countdown timer
    void startTimer() {
        timeLeft = 180;
        resendLink.setVisible(false);

        if (timer != null) timer.stop();

        timer = new Timer(1000, e -> {
            timeLeft--;

            timerLabel.setText("OTP valid: " +
                    String.format("%02d:%02d", timeLeft/60, timeLeft%60));

            if (timeLeft <= 0) {
                timer.stop();
                timerLabel.setText("Expired!");
                resendLink.setVisible(true);

                otpField.setEnabled(false);
                saveBtn.setEnabled(false);
            }
        });

        timer.start();
    }

    // Validates entered OTP
    void checkOTP(){

        if(System.currentTimeMillis() - otpTime > 180000){
            saveBtn.setEnabled(false);
            return;
        }
    
        String entered = otpField.getText().trim();
        boolean valid = entered.equals(generatedOTP);
        otpVerified = valid;

        saveBtn.setEnabled(valid);
        saveBtn.setBackground(new Color(40,167,69));
    }

    // Updates user profile data
    void updateProfile(){
        try{
            Connection con = DBConnection.getConnection();

            String originalUsername = oldUsername;
            String originalEmail = oldEmail;

            String newUser = usernameField.getText().trim();
            String newEmail = emailField.getText().trim();

            String oldP = new String(oldPass.getPassword());
            String newP = new String(newPass.getPassword());
            String confirmP = new String(confirmPass.getPassword());

            boolean usernameChanged = !newUser.trim().equals(originalUsername.trim());
            boolean emailChanged = !newEmail.trim().equalsIgnoreCase(originalEmail.trim());
            boolean passwordChanged = !newP.isEmpty();

            // No changes detected
            if(!usernameChanged && !emailChanged && !passwordChanged){
                JOptionPane.showMessageDialog(this, "No changes detected!");
                return;
            }

            // Only username change without OTP
            if(usernameChanged && !emailChanged && !passwordChanged && otpEmail == null){
                JOptionPane.showMessageDialog(this,"Username Updated");

                currentUsername = newUser;
                dispose();
                parent.setVisible(true);
                return;
            }

            // Changes requiring OTP verification
            if(emailChanged || passwordChanged || otpEmail != null){

                String currentEmail = emailField.getText().trim();

                // Ensure email matches OTP email
                if(!currentEmail.equals(otpEmail)){
                    JOptionPane.showMessageDialog(this, "Email changed! Please send OTP.");
                    otpVerified = false;
                    return;
                }

                // Ensure OTP is verified
                if(!otpVerified){
                    JOptionPane.showMessageDialog(this,"Invalid or expired OTP");
                    return;
                }

                // Handle username change (update folder and logs)
                if(usernameChanged){

                    String basePath = System.getProperty("user.dir") + "/data/users/";

                    File oldFolder = new File(basePath + originalUsername);
                    File newFolder = new File(basePath + newUser);

                    if(oldFolder.exists()){
                        oldFolder.renameTo(newFolder);
                    }

                    PreparedStatement psLog = con.prepareStatement(
                        "UPDATE activity_log SET username=? WHERE username=?"
                    );
                    psLog.setString(1, newUser);
                    psLog.setString(2, originalUsername);
                    psLog.executeUpdate();
                }

                // Handle password change
                if(passwordChanged){
                    if(!hashPassword(oldP).equals(oldPassword)){
                        JOptionPane.showMessageDialog(this,"Wrong old password");
                        return;
                    }

                    if(newP.length() < 6){
                        JOptionPane.showMessageDialog(this, "Password must be at least 6 characters!");
                        return;
                    }

                    if(!newP.equals(confirmP)){
                        JOptionPane.showMessageDialog(this,"Password mismatch");
                        return;
                    }

                    oldPassword = hashPassword(newP);
                }

                // Update database
                PreparedStatement ps = con.prepareStatement(
                    "UPDATE users SET username=?, email=?, password_hash=? WHERE id=?"
                );

                ps.setString(1, newUser);
                ps.setString(2, newEmail);
                ps.setString(3, oldPassword);
                ps.setInt(4, userId);

                ps.executeUpdate();

                JOptionPane.showMessageDialog(this,"Profile Updated");

                currentUsername = newUser;
                dispose();
                parent.setVisible(true);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Loads user data from database
    void loadUserData(){
        try{
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM users WHERE username=?"
            );
            ps.setString(1, currentUsername);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                userId = rs.getInt("id");
                oldUsername = rs.getString("username");
                oldEmail = rs.getString("email");
                oldPassword = rs.getString("password_hash");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Hashes password using SHA-256
    String hashPassword(String pass){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(pass.getBytes()));
        }catch(Exception e){
            return null;
        }
    }

    // Resets OTP verification state
    void resetOTP(){
        otpVerified = false;
    }

    // Creates styled text field
    JTextField createField(String title){
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(350,45));
        field.setBackground(FIELD);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                title
        );
        border.setTitleColor(Color.LIGHT_GRAY);

        field.setBorder(border);
        return field;
    }

    // Creates styled password field
    JPasswordField createPassword(String title){
        JPasswordField field = new JPasswordField();
        field.setMaximumSize(new Dimension(350,45));
        field.setBackground(FIELD);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                title
        );
        border.setTitleColor(Color.LIGHT_GRAY);

        field.setBorder(border);
        return field;
    }

    // Creates custom rounded button
    JButton createRoundedButton(String text, Color color, int width, int height){
        JButton btn = new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0,0,getWidth(),getHeight(),height,height);

                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        Dimension size = new Dimension(width,height);
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);

        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);

        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return btn;
    }

    // Returns updated username after changes
    public String getUpdatedUsername(){
        return usernameField.getText().trim();
    }
}