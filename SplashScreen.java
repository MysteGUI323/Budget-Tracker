import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class SplashScreen extends JWindow {

    private static final int WIDTH  = 480;
    private static final int HEIGHT = 280;

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0)); // transparent frame edge

        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background rounded rect
                g2.setColor(UITheme.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                // Accent top bar
                g2.setColor(UITheme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), 6, 6, 6);
                g2.fillRect(0, 3, getWidth(), 3);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(36, 40, 30, 40));

        // ── Center content ─────────────────────────────────────────────────────

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        // App icon / emoji
        JLabel icon = new JLabel("💸");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(icon);

        center.add(Box.createVerticalStrut(12));

        // App title
        JLabel title = new JLabel("Student Budget Tracker");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        title.setForeground(UITheme.ACCENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(title);

        center.add(Box.createVerticalStrut(6));

        // Tagline
        JLabel tagline = new JLabel("Track it. Save it. Don't blow it.");
        tagline.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        tagline.setForeground(UITheme.TEXT_SECONDARY);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(tagline);

        center.add(Box.createVerticalStrut(28));

        // Loading bar
        JProgressBar loadBar = new JProgressBar(0, 100);
        loadBar.setIndeterminate(true);
        loadBar.setPreferredSize(new Dimension(320, 6));
        loadBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        loadBar.setBackground(UITheme.BORDER);
        loadBar.setForeground(UITheme.ACCENT);
        loadBar.setBorderPainted(false);
        loadBar.setStringPainted(false);
        loadBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(loadBar);

        center.add(Box.createVerticalStrut(10));

        // Loading label
        JLabel loadLabel = new JLabel("Loading...");
        loadLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        loadLabel.setForeground(UITheme.TEXT_SECONDARY);
        loadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(loadLabel);

        content.add(center, BorderLayout.CENTER);

        // ── Bottom version tag ─────────────────────────────────────────────────

        JLabel version = new JLabel("v1.0");
        version.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
        version.setForeground(UITheme.BORDER);
        version.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(version, BorderLayout.SOUTH);

        // Subtle drop shadow effect via border
        content.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER, 1, true),
            new EmptyBorder(36, 40, 30, 40)
        ));

        setContentPane(content);
    }

    // Call this to show splash, do work, then transition to main app
    public static void show(Runnable onComplete) {
        SplashScreen splash = new SplashScreen();
        splash.setVisible(true);

        // Run loading work on background thread so UI stays responsive
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(2000); // minimum display time
                return null;
            }

            @Override
            protected void done() {
                splash.dispose();
                onComplete.run();
            }
        };
        worker.execute();
    }
}
