package auth;

import db.DBConnection;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.*;

// Registration frame for creating new user accounts with OTP verification
public class RegisterFrame extends JFrame {

    // Input fields
    JTextField user, email, otpField;
    JPasswordField pass, confirmPass;

    // Buttons and labels
    JButton sendOtpBtn, registerBtn;
    JLabel resendLink, timerLabel;

    // OTP and verification data
    String generatedOTP;
    String verifiedEmail;
    long otpTime;

    // Timer for OTP validity
    Timer timer;
    int timeLeft = 180;

    // UI colors
    Color BG = new Color(18,18,18);
    Color CARD = new Color(35,35,35);
    Color FIELD = new Color(50,50,50);

    public RegisterFrame() {

        // Set consistent look and feel for UI components
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {}

        // Configure main frame
        setTitle("Register - EncryptVault");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new GridBagLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create main container panel
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40,40,40,40));
        card.setPreferredSize(new Dimension(450, 600));

        // Title label
        JLabel title = new JLabel("Create Account");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Input fields
        user = createField("Username");
        email = createField("Email");

        // OTP field initially disabled
        otpField = createField("OTP");
        otpField.setEnabled(false);

        // Password fields initially disabled until OTP verification
        pass = createPassword("Password");
        confirmPass = createPassword("Confirm Password");
        pass.setEnabled(false);
        confirmPass.setEnabled(false);

        // Buttons
        sendOtpBtn = createButton("Send OTP", new Color(255,140,0), 100, 35);
        registerBtn = createButton("Register", new Color(40,167,69), 135, 40);
        registerBtn.setEnabled(false);

        // Timer label for OTP countdown
        timerLabel = new JLabel("");
        timerLabel.setForeground(Color.ORANGE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Resend OTP link (hidden initially)
        resendLink = createLink("Resend OTP");
        resendLink.setVisible(false);

        // Navigation link to login screen
        JLabel login = createLink("Already have account? Login");

        // Attach actions
        sendOtpBtn.addActionListener(e -> sendOTP());

        resendLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                sendOTP();
            }
        });

        // Automatically verify OTP when full length is entered
        otpField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (generatedOTP != null &&
                        otpField.getText().length() == generatedOTP.length()) {
                    verifyOTP();
                }
            }
        });

        registerBtn.addActionListener(e -> registerUser());

        // Navigate to login screen
        login.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                dispose();
                new LoginFrame();
            }
        });

        // Build UI layout
        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(user);
        card.add(Box.createVerticalStrut(15));
        card.add(email);
        card.add(Box.createVerticalStrut(15));
        card.add(sendOtpBtn);
        card.add(Box.createVerticalStrut(15));
        card.add(otpField);
        card.add(Box.createVerticalStrut(10));
        card.add(timerLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(resendLink);
        card.add(Box.createVerticalStrut(15));
        card.add(pass);
        card.add(Box.createVerticalStrut(10));
        card.add(confirmPass);
        card.add(Box.createVerticalStrut(20));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(15));
        card.add(login);

        add(card);
        getContentPane().setBackground(BG);

        setVisible(true);
    }

    // Sends OTP after validating username and email
    void sendOTP() {

        // Validate username
        if (user.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username!");
            return;
        }

        // Validate email
        if (email.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter email!");
            return;
        }

        // Check if username already exists
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement checkUser = con.prepareStatement(
                    "SELECT * FROM users WHERE username=?"
            );
            checkUser.setString(1, user.getText().trim());

            ResultSet rsUser = checkUser.executeQuery();

            if (rsUser.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists!");
                return;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Check if email already registered
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement check = con.prepareStatement(
                    "SELECT * FROM users WHERE email=?"
            );
            check.setString(1, email.getText().trim());

            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Email already registered!");
                return;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Store original button color
        Color originalColor = new Color(255,140,0);

        // Update UI to show sending state
        sendOtpBtn.setText("Sending...");
        sendOtpBtn.setEnabled(false);
        sendOtpBtn.setBackground(new Color(120,120,120));

        // Send OTP in background thread
        new Thread(() -> {
            try {
                generatedOTP = OTPGenerator.generateOTP();
                otpTime = System.currentTimeMillis();

                EmailSender.sendOTP(email.getText(), generatedOTP);

                verifiedEmail = email.getText().trim();

                SwingUtilities.invokeLater(() -> {

                    otpField.setEnabled(true);
                    startTimer();

                    // Update UI to success state
                    sendOtpBtn.setText("Sent");
                    sendOtpBtn.setBackground(new Color(40,167,69));

                    // Restore button state after delay
                    new Timer(1500, e -> {
                        sendOtpBtn.setText("Send OTP");
                        sendOtpBtn.setEnabled(true);
                        sendOtpBtn.setBackground(originalColor);
                    }).start();
                });

            } catch (Exception e) {
                e.printStackTrace();

                // Restore UI on failure
                SwingUtilities.invokeLater(() -> {
                    sendOtpBtn.setText("Send OTP");
                    sendOtpBtn.setEnabled(true);
                    sendOtpBtn.setBackground(originalColor);
                });
            }
        }).start();
    }

    // Verifies OTP and enables password fields
    void verifyOTP() {
        if (!otpField.getText().equals(generatedOTP)) return;

        pass.setEnabled(true);
        confirmPass.setEnabled(true);
        registerBtn.setEnabled(true);

        JOptionPane.showMessageDialog(this, "OTP Verified!");
    }

    // Starts countdown timer for OTP validity
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
            }
        });

        timer.start();
    }

    // Registers user after validating passwords and OTP
    void registerUser() {
        try {
            String p1 = new String(pass.getPassword());
            String p2 = new String(confirmPass.getPassword());

            if (p1.trim().isEmpty() || p2.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter password!");
                return;
            }

            if (p1.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters!");
                return;
            }

            if (!p1.equals(p2)) {
                JOptionPane.showMessageDialog(this, "Password mismatch!");
                return;
            }

            // Hash password and generate AES key
            String passwordHash = hashPassword(p1);
            String aesKey = generateAESKey();

            Connection con = DBConnection.getConnection();
            
            // Ensure email has not been changed after OTP verification
            if (!email.getText().trim().equals(verifiedEmail)) {
                JOptionPane.showMessageDialog(this, "Email changed! Please verify again.");
                return;
            }

            // Check if email already exists
            PreparedStatement check = con.prepareStatement(
                    "SELECT * FROM users WHERE email=?"
            );
            check.setString(1, email.getText().trim());

            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Email already exists!");
                return;
            }

            // Insert new user into database
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users(username,email,password_hash,aes_key) VALUES (?,?,?,?)"
            );

            ps.setString(1, user.getText().trim());
            ps.setString(2, email.getText().trim());
            ps.setString(3, passwordHash);
            ps.setString(4, aesKey);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Registered Successfully!");
            dispose();
            new LoginFrame();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Creates styled text field
    JTextField createField(String title){
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));

        f.setBackground(FIELD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        f.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120,120,120)),
                title,
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                Color.LIGHT_GRAY
        ));
        return f;
    }

    // Creates styled password field
    JPasswordField createPassword(String title){
        JPasswordField f = new JPasswordField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));

        f.setBackground(FIELD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        f.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120,120,120)),
                title,
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                Color.LIGHT_GRAY
        ));
        return f;
    }

    // Creates custom styled button
    JButton createButton(String text, Color color, int width, int height){
        JButton b = new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        height, height);

                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        b.setPreferredSize(new Dimension(width, height));
        b.setMaximumSize(new Dimension(width, height));

        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));

        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return b;
    }

    // Creates clickable link label
    JLabel createLink(String text){
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);

        l.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { l.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { l.setForeground(Color.LIGHT_GRAY); }
        });

        return l;
    }

    // Hashes password using SHA-256
    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(
            md.digest(password.trim().getBytes("UTF-8"))
        );
    }

    // Generates AES encryption key for user
    private String generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}