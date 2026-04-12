package dashboard;

import auth.LoginFrame;
import db.DBConnection;
import filemanager.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import javax.swing.*;
import settings.SettingsFrame;

// Dashboard screen shown after successful login
// Handles file operations, recent activity, settings, and session management
public class DashboardFrame extends JFrame {

    // Current logged-in user details
    private String currentUsername;
    private String aesKey;

    // UI components
    JLabel welcomeLabel;
    JPanel recentPanel;

    // Color configuration
    Color BG = new Color(18,18,18);
    Color SIDEBAR = new Color(28,28,28);
    Color HOVER = new Color(60,60,60);
    Color ACTIVE = new Color(45,110,220);

    // Tracks currently active sidebar button
    JButton activeButton = null;

    // Idle session tracking timers
    Timer idleTimer;
    Timer warningTimer;

    // Warning popup dialog
    JDialog warningDialog;

    // Flags for session state
    boolean isWarningShown = false;
    boolean listenerAdded = false;

    // Global activity listener
    AWTEventListener activityListener;

    // Idle and warning durations
    int IDLE_TIME = 5 * 60 * 1000;
    int WARNING_TIME = 15 * 1000;
    int countdown = 15;

    public DashboardFrame(String username, String aesKey) {

        this.currentUsername = username;
        this.aesKey = aesKey;

        // Validate AES key before loading dashboard
        if(aesKey == null || aesKey.isEmpty()){
            JOptionPane.showMessageDialog(this, "AES Key missing in Dashboard!");
            return;
        }

        // Frame configuration
        setTitle("EncryptVault - Dashboard");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create sidebar panel
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Application logo
        JLabel logo = new JLabel("EncryptVault", SwingConstants.CENTER);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));

        sidebar.add(logo);

        // Sidebar menu options
        sidebar.add(createMenu("Upload", "upload.png", () -> FileUpload.upload(currentUsername, aesKey)));
        sidebar.add(createMenu("Read", "read.png", () -> FileRead.read(currentUsername, aesKey)));
        sidebar.add(createMenu("Download", "download.png", () -> FileDownload.download(currentUsername, aesKey)));
        sidebar.add(createMenu("Delete", "delete.png", () -> FileDelete.delete(currentUsername)));

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(createMenu("Settings", "settings.png", () -> openSettings()));
        sidebar.add(createMenu("Logout", "logout.png", () -> logout()));

        // Main content panel
        JPanel main = new JPanel();
        main.setBackground(BG);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(40,60,40,60));

        // Welcome label
        welcomeLabel = new JLabel("Welcome, " + currentUsername);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));

        main.add(welcomeLabel);

        // Recent activity panel
        recentPanel = createRecentActivityPanel();
        main.add(recentPanel);

        // Auto refresh recent activity every 3 seconds
        new javax.swing.Timer(3000, e -> refreshRecentActivity()).start();

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        setVisible(true);

        // Start tracking user inactivity
        startIdleTracking();
    }

    // Refreshes recent activity panel
    void refreshRecentActivity(){

        recentPanel.removeAll();

        JPanel newPanel = createRecentActivityPanel();

        for(Component c : newPanel.getComponents()){
            recentPanel.add(c);
        }

        recentPanel.revalidate();
        recentPanel.repaint();
    }

    // Creates panel showing last 5 user activities
    JPanel createRecentActivityPanel(){

        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Recent Activity");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        panel.add(Box.createVerticalStrut(50));
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        try{
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                "SELECT action, filename, time FROM activity_log WHERE username=? ORDER BY id DESC LIMIT 5"
            );

            // Set username parameter
            ps.setString(1, currentUsername.trim());

            ResultSet rs = ps.executeQuery();

            boolean hasData = false;

            while(rs.next()){
                hasData = true;

                String action = rs.getString("action");
                String file = rs.getString("filename");
                String time = rs.getString("time");

                JLabel item = new JLabel("• " + action + " --- " + file + " --- (" + time + ")");
                item.setForeground(Color.LIGHT_GRAY);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 14));

                panel.add(item);
                panel.add(Box.createVerticalStrut(5));
            }

            // Show message if no activity found
            if(!hasData){
                JLabel empty = new JLabel("No recent activity");
                empty.setForeground(Color.GRAY);
                panel.add(empty);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return panel;
    }

    // Opens settings screen and updates username if changed
    void openSettings(){
        SettingsFrame s = new SettingsFrame(currentUsername, this);
        this.setVisible(false);

        s.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {

                DashboardFrame.this.setVisible(true);

                String newUsername = s.getUpdatedUsername();

                // Update username and UI if changed
                if(newUsername != null && !newUsername.trim().isEmpty()){
                    currentUsername = newUsername.trim();
                    welcomeLabel.setText("Welcome, " + currentUsername);
                    refreshRecentActivity();
                }
            }
        });
    }

    // Loads and recolors icon images
    ImageIcon loadIcon(String name){
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/" + name));
            Image img = icon.getImage();

            BufferedImage buffered = new BufferedImage(
                    img.getWidth(null),
                    img.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2 = buffered.createGraphics();
            g2.drawImage(img, 0, 0, null);

            // Convert icon color to white
            for(int y = 0; y < buffered.getHeight(); y++){
                for(int x = 0; x < buffered.getWidth(); x++){
                    int rgba = buffered.getRGB(x, y);
                    int alpha = (rgba >> 24) & 0xff;

                    if(alpha != 0){
                        buffered.setRGB(x, y, (alpha << 24) | 0xffffff);
                    }
                }
            }

            g2.dispose();

            Image scaled = buffered.getScaledInstance(18,18, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Creates sidebar menu button
    JButton createMenu(String text, String iconName, Runnable action){

        JButton btn = new JButton(text, loadIcon(iconName));

        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.setBackground(SIDEBAR);
        btn.setForeground(Color.WHITE);

        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        btn.setFocusPainted(false);
        btn.setBorderPainted(false);

        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(10);

        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Handle menu click
        btn.addActionListener(e -> {
            setActive(btn);
            action.run();
        });

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn != activeButton)
                    btn.setBackground(HOVER);
            }
            public void mouseExited(MouseEvent e) {
                if (btn != activeButton)
                    btn.setBackground(SIDEBAR);
            }
        });

        return btn;
    }

    // Highlights active menu button
    void setActive(JButton btn){
        if(activeButton != null){
            activeButton.setBackground(SIDEBAR);
        }
        activeButton = btn;
        btn.setBackground(ACTIVE);
    }

    // Logs out user and returns to login screen
    void logout(){

        if(idleTimer != null) idleTimer.stop();
        if(warningTimer != null) warningTimer.stop();

        if(activityListener != null){
            Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        }

        this.setVisible(false);
        this.dispose();

        new LoginFrame();
    }

    // Starts tracking user inactivity
    void startIdleTracking(){

        idleTimer = new Timer(IDLE_TIME, e -> showWarningPopup());
        idleTimer.setRepeats(false);
        idleTimer.start();

        if(!listenerAdded){

            activityListener = event -> {

                if(isWarningShown) return;

                if(event instanceof MouseEvent || event instanceof KeyEvent){
                    resetIdleTimer();
                }
            };

            Toolkit.getDefaultToolkit().addAWTEventListener(
                activityListener,
                AWTEvent.MOUSE_EVENT_MASK |
                AWTEvent.MOUSE_MOTION_EVENT_MASK |
                AWTEvent.KEY_EVENT_MASK
            );

            listenerAdded = true;
        }
    }

    // Resets idle timer when user performs activity
    void resetIdleTimer(){
        if(idleTimer != null){
            idleTimer.restart();
        }
    }

    // Shows warning popup before auto logout
    void showWarningPopup(){
        isWarningShown = true;

        warningDialog = new JDialog(this, "Session Expiring", true);
        warningDialog.setSize(350,150);
        warningDialog.setLocationRelativeTo(this);
        warningDialog.setLayout(new BorderLayout());

        JLabel msg = new JLabel("", SwingConstants.CENTER);
        final Timer[] countdownTimer = new Timer[1];

        JButton continueBtn = new JButton("Continue");

        // Continue session and reset timers
        continueBtn.addActionListener(e -> {
            if(countdownTimer[0] != null) countdownTimer[0].stop();
            warningTimer.stop();
            warningDialog.dispose();
            isWarningShown = false;
            resetIdleTimer();
        });

        warningDialog.add(msg, BorderLayout.CENTER);

        countdown = 15;
        msg.setText("Session expiring in " + countdown + " seconds");

        // Countdown timer display
        countdownTimer[0] = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                msg.setText("Session expiring in " + countdown + " seconds");

                countdown--;

                if(countdown < 0){
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        countdownTimer[0].start();

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(18,18,18));
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        continueBtn.setPreferredSize(new Dimension(120, 35));

        btnPanel.add(continueBtn);

        warningDialog.add(btnPanel, BorderLayout.SOUTH);

        // Auto logout after warning timeout
        warningTimer = new Timer(WARNING_TIME, e -> {
            if(countdownTimer[0] != null) countdownTimer[0].stop();
            warningDialog.dispose();
            logout();
        });

        warningTimer.setRepeats(false);
        warningTimer.start();

        warningDialog.setVisible(true);
    }
}