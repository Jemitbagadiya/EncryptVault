package auth;

import dashboard.DashboardFrame;
import db.DBConnection;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Base64;
import javax.swing.*;
import javax.swing.border.TitledBorder;

// Main login frame handling authentication, OTP verification, and password reset
public class LoginFrame extends JFrame {

    // Input fields
    JTextField user, otpField;
    JPasswordField pass, newPass, confirmPass;

    // Buttons
    JButton loginBtn, sendOtpBtn, changePassBtn;

    // Labels and navigation links
    JLabel forgotBtn, signupBtn, resendLink, timerLabel, backToLogin, title;

    // OTP and user-related data
    String generatedOTP, userEmail, userAESKey;
    long otpTime;

    // Timer for OTP expiration
    Timer timer;
    int timeLeft = 180;

    // Mode flag to switch between login and forgot password
    boolean isForgotMode = false;

    // UI color configuration
    Color BG = new Color(18,18,18);
    Color CARD = new Color(35,35,35);
    Color FIELD = new Color(50,50,50);

    public LoginFrame() {

        // Set cross-platform look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Customize UI behavior
        UIManager.put("Button.focus", new Color(0,0,0,0));
        UIManager.put("OptionPane.isYesLast", false);
        UIManager.put("OptionPane.defaultButton", "OK");

        // Configure main frame
        setTitle("EncryptVault");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new GridBagLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create main card panel
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40,40,40,40));
        card.setPreferredSize(new Dimension(420, 600));

        // Title label
        title = new JLabel("Login");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Input fields
        user = createField("Username");
        pass = createPassword("Password");

        // OTP field initially disabled
        otpField = createField("OTP");
        otpField.setEnabled(false);

        // Password reset fields
        newPass = createPassword("New Password");
        confirmPass = createPassword("Confirm Password");

        // Hide and disable reset fields initially
        newPass.setVisible(false);
        confirmPass.setVisible(false);
        newPass.setEnabled(false);
        confirmPass.setEnabled(false);

        // Buttons
        sendOtpBtn = createButton("Send OTP", new Color(255,140,0));
        loginBtn = createButton("Login", new Color(40,167,69));
        changePassBtn = createButton("Reset Password", new Color(0,123,255));

        // Set button sizes
        sendOtpBtn.setPreferredSize(new Dimension(100, 35));
        sendOtpBtn.setMaximumSize(new Dimension(100, 35));

        loginBtn.setPreferredSize(new Dimension(130, 40));
        loginBtn.setMaximumSize(new Dimension(130, 40));

        changePassBtn.setPreferredSize(new Dimension(136, 38));
        changePassBtn.setMaximumSize(new Dimension(136, 38));

        // Disable buttons initially
        loginBtn.setEnabled(false);
        changePassBtn.setEnabled(false);
        changePassBtn.setVisible(false);

        // Timer label for OTP countdown
        timerLabel = new JLabel("");
        timerLabel.setForeground(Color.ORANGE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Resend OTP link (hidden initially)
        resendLink = createLink("Resend OTP");
        resendLink.setVisible(false);

        // Navigation links
        forgotBtn = createLink("Forgot Password?");
        signupBtn = createLink("Don't have account? Sign Up");

        backToLogin = createLink("← Back to Login");
        backToLogin.setVisible(false);

        // Attach button actions
        sendOtpBtn.addActionListener(e -> sendOTP());
        loginBtn.addActionListener(e -> verifyLoginOTP());
        changePassBtn.addActionListener(e -> resetPassword());

        // Auto-verify OTP when full OTP is entered
        otpField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (generatedOTP != null &&
                        otpField.getText().length() == generatedOTP.length()) {
                    verifyOTPOnly();
                }
            }
        });

        // Resend OTP action
        resendLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                sendOTP();
            }
        });

        // Enable forgot password mode
        forgotBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                enableForgotMode();
            }
        });

        // Return to login mode
        backToLogin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                disableForgotMode();
            }
        });

        // Navigate to registration screen
        signupBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                dispose();
                new RegisterFrame();
            }
        });

        // Build UI layout
        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(user);
        card.add(Box.createVerticalStrut(15));
        card.add(pass);
        card.add(Box.createVerticalStrut(15));
        card.add(sendOtpBtn);
        card.add(Box.createVerticalStrut(15));
        card.add(otpField);
        card.add(Box.createVerticalStrut(10));
        card.add(timerLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(resendLink);

        card.add(Box.createVerticalStrut(15));
        card.add(newPass);
        card.add(Box.createVerticalStrut(10));
        card.add(confirmPass);

        card.add(Box.createVerticalStrut(15));
        card.add(loginBtn);
        card.add(changePassBtn);

        card.add(Box.createVerticalStrut(10));
        card.add(forgotBtn);
        card.add(backToLogin);

        card.add(Box.createVerticalStrut(10));
        card.add(signupBtn);

        // Add panel to frame
        add(card);
        getContentPane().setBackground(BG);

        // Apply UI updates
        SwingUtilities.updateComponentTreeUI(this); 

        setVisible(true);
    }

    // Creates a styled text field with border and colors
    JTextField createField(String title){
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
        f.setBackground(FIELD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120,120,120)),
                title
        );
        border.setTitleColor(Color.LIGHT_GRAY);

        f.setBorder(border);
        return f;
    }

    // Creates a styled password field
    JPasswordField createPassword(String title){
        JPasswordField f = new JPasswordField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
        f.setBackground(FIELD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120,120,120)),
                title
        );
        border.setTitleColor(Color.LIGHT_GRAY);

        f.setBorder(border);
        return f;
    }

    // Creates a custom rounded button with styling
    JButton createButton(String text, Color color){
        JButton b = new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return b;
    }

    // Creates a clickable label used as a link
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

    // Verifies OTP and enables next step based on current mode
    void verifyOTPOnly() {
        if (System.currentTimeMillis() - otpTime > 180000) return;

        if (!otpField.getText().equals(generatedOTP)) return;

        if (!isForgotMode) {
                loginBtn.setEnabled(true);
                JOptionPane.showMessageDialog(this, "OTP Verified!");
        }

        if (isForgotMode) {
            newPass.setEnabled(true);
            confirmPass.setEnabled(true);
            changePassBtn.setEnabled(true);
            JOptionPane.showMessageDialog(this, "OTP Verified!");
        }
    }

    // Validates login using OTP and credentials
    void verifyLoginOTP() {

        String username = user.getText().trim();
        String password = new String(pass.getPassword()).trim();

        if (!isForgotMode) {

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username!");
                return;
            }

            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter password!");
                return;
            }
        }

        if (System.currentTimeMillis() - otpTime > 180000) {
            JOptionPane.showMessageDialog(this, "OTP Expired!");
            return;
        }

        if (!otpField.getText().equals(generatedOTP)) {
            JOptionPane.showMessageDialog(this, "Invalid OTP");
            return;
        }

        if (isForgotMode) return;

        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM users WHERE username=? AND password_hash=?"
            );

            ps.setString(1, username);
            ps.setString(2, hashPassword(password));

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Invalid credentials!");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Login error!");
            return;
        }

        dispose();
        new DashboardFrame(username, userAESKey);
    }

    // Enables forgot password mode and resets UI state
    void enableForgotMode() {
        isForgotMode = true;

        title.setText("Forgot Password");

        pass.setVisible(false);
        loginBtn.setVisible(false);

        newPass.setVisible(true);
        confirmPass.setVisible(true);
        changePassBtn.setVisible(true);

        newPass.setEnabled(false);
        confirmPass.setEnabled(false);
        changePassBtn.setEnabled(false);

        forgotBtn.setVisible(false);
        backToLogin.setVisible(true);

        user.setText("");
        pass.setText("");
        otpField.setText("");
        newPass.setText("");
        confirmPass.setText("");

        otpField.setEnabled(false);
        timerLabel.setText("");
        resendLink.setVisible(false);

        generatedOTP = null;
        otpTime = 0;

        if (timer != null) timer.stop();

        sendOtpBtn.setText("Send OTP");
        sendOtpBtn.setBackground(new Color(255,140,0));
        sendOtpBtn.setEnabled(true);
    }

    // Restores login mode UI and resets state
    void disableForgotMode() {
        isForgotMode = false;

        title.setText("Login");

        pass.setVisible(true);
        loginBtn.setVisible(true);

        newPass.setVisible(false);
        confirmPass.setVisible(false);
        changePassBtn.setVisible(false);

        newPass.setEnabled(false);
        confirmPass.setEnabled(false);
        changePassBtn.setEnabled(false);

        forgotBtn.setVisible(true);
        backToLogin.setVisible(false);

        user.setText("");
        pass.setText("");
        otpField.setText("");
        newPass.setText("");
        confirmPass.setText("");

        otpField.setEnabled(false);
        timerLabel.setText("");
        resendLink.setVisible(false);

        generatedOTP = null;
        otpTime = 0;

        if (timer != null) timer.stop();

        sendOtpBtn.setText("Send OTP");
        sendOtpBtn.setBackground(new Color(255,140,0));
        sendOtpBtn.setEnabled(true);
    }

    // Sends OTP after validating user input and database records
    void sendOTP() {

        String username = user.getText().trim();
        String password = new String(pass.getPassword()).trim();

        if (!isForgotMode) {

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username!");
                return;
            }

            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter password!");
                return;
            }

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters!");
                return;
            }
        }

        otpField.setText("");
        generatedOTP = null;

        loginBtn.setEnabled(false);
        changePassBtn.setEnabled(false);

        sendOtpBtn.setBackground(new Color(100,100,100));
        sendOtpBtn.setText("Sending...");
        sendOtpBtn.setEnabled(false);

        new Thread(() -> {
            try {
                Connection con = DBConnection.getConnection();

                PreparedStatement ps;

                if (!isForgotMode) {
                    ps = con.prepareStatement(
                        "SELECT email, aes_key FROM users WHERE username=? AND password_hash=?"
                    );
                    ps.setString(1, username);
                    ps.setString(2, hashPassword(password));

                } else {
                    ps = con.prepareStatement(
                        "SELECT email, aes_key FROM users WHERE username=?"
                    );
                    ps.setString(1, user.getText().trim());
                }

                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "User not found");

                        sendOtpBtn.setBackground(new Color(255,140,0));
                        sendOtpBtn.setText("Send OTP");
                        sendOtpBtn.setEnabled(true);
                    });
                    return;
                }

                userEmail = rs.getString("email");
                userAESKey = rs.getString("aes_key");

                if(userAESKey == null || userAESKey.isEmpty()){
                    JOptionPane.showMessageDialog(this, "Encryption key missing!");
                    return;
                }

                generatedOTP = OTPGenerator.generateOTP();
                otpTime = System.currentTimeMillis();

                EmailSender.sendOTP(userEmail, generatedOTP);

                SwingUtilities.invokeLater(() -> {

                    otpField.setEnabled(true);
                    startTimer();

                    sendOtpBtn.setBackground(new Color(40,167,69));
                    sendOtpBtn.setText("Sent");

                    Timer t = new Timer(1500, e -> {
                        sendOtpBtn.setBackground(new Color(255,140,0));
                        sendOtpBtn.setText("Send OTP");
                        sendOtpBtn.setEnabled(true);
                    });
                    t.setRepeats(false);
                    t.start();

                });

            } catch (Exception e) {
                e.printStackTrace();

                SwingUtilities.invokeLater(() -> {
                    sendOtpBtn.setBackground(new Color(255,140,0));
                    sendOtpBtn.setText("Send OTP");
                    sendOtpBtn.setEnabled(true);
                });
            }
        }).start();
    }

    // Starts OTP timer and updates UI every second
    void startTimer() {
        timeLeft = 180;
        resendLink.setVisible(false);

        if (timer != null) timer.stop();

        timer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("OTP valid: " + String.format("%02d:%02d", timeLeft/60, timeLeft%60));

            if (timeLeft <= 0) {
                timer.stop();
                timerLabel.setText("Expired!");
                resendLink.setVisible(true);
                otpField.setEnabled(false);

                loginBtn.setEnabled(false);

                sendOtpBtn.setEnabled(true);
            }
        });

        timer.start();
    }

    // Resets user password after validating inputs
    void resetPassword() {
        try {
            String p1 = new String(newPass.getPassword());
            String p2 = new String(confirmPass.getPassword());

            if (p1.isEmpty() || p2.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter password");
                return;
            }

            if (p1.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters!");
                return;
            }

            if (!p1.equals(p2)) {
                JOptionPane.showMessageDialog(this, "Password mismatch");
                return;
            }

            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE users SET password_hash=? WHERE username=?"
            );

            ps.setString(1, hashPassword(p1));
            ps.setString(2, user.getText());

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Password Changed!");

            disableForgotMode();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hashes password using SHA-256 algorithm and encodes it in Base64
    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.trim().getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}