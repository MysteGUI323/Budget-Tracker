import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * BudgetApp — root application window (JFrame).
 *
 * Builds the top-level layout:
 *   - Header bar: app title + save-file path
 *   - Tabbed pane: Expenses | Budget | Savings | History | Settings
 *
 * Save-on-close is handled by the WindowListener so no manual save
 * button is needed — data is written to disk whenever the user exits.
 */
public class BudgetApp extends JFrame {

    // ── Constructor ────────────────────────────────────────────────────────────

    public BudgetApp() {
        setTitle("Budget Tracker");
        setMinimumSize(new Dimension(900, 650));
        setPreferredSize(new Dimension(1050, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);

        // Save automatically when the window is closed
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SaveManager.save();
                dispose();
                System.exit(0);
            }
        });

        pack();
    }

    // ── Header bar ─────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.HEADER_BG);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        // App title
        JLabel title = new JLabel("📚Budget Tracker");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
        title.setForeground(UITheme.ACCENT);

        // Save-file path hint
        JLabel subtitle = new JLabel(
                "Track it. Save it. Don't blow it.  —  Save file: " + SaveManager.getSaveFilePath()
        );
        subtitle.setFont(UITheme.SMALL_FONT);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setBackground(UITheme.HEADER_BG);
        titleGroup.add(title);
        titleGroup.add(subtitle);

        header.add(titleGroup, BorderLayout.WEST);
        return header;
    }

    // ── Tab pane ───────────────────────────────────────────────────────────────

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UITheme.BG);
        tabs.setForeground(Color.BLACK);
        tabs.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        tabs.setBorder(null);

        // Force tab text to black regardless of system L&F overrides
        UIManager.put("TabbedPane.foreground",           Color.BLACK);
        UIManager.put("TabbedPane.selectedForeground",   Color.BLACK);
        UIManager.put("TabbedPane.unselectedForeground", Color.BLACK);
        tabs.updateUI();

        tabs.addTab("  💸Expenses  ", new ExpensePanel());
        tabs.addTab("  💰Budget  ",   new BudgetPanel());
        tabs.addTab("  🎯Savings  ",  new SavingsPanel());
        tabs.addTab("  📊History  ",  new HistoryPanel());
        tabs.addTab("  ⚙️Settings  ", new SettingsPanel());

        return tabs;
    }
}