import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * BudgetPanel — Budget tab UI.
 *
 * Layout (top to bottom):
 *   - Budget Settings card: period selector + amount input
 *   - Header label: current budget + day counter
 *   - Info line: daily rate + prorated budget
 *   - Progress bar: period spending vs prorated ceiling
 *   - Status label: on-track / warning / over-budget message
 *   - Stat cards row: This Period Spent | Prorated Budget | Remaining Today | Spend Today
 *
 * All display values update via refresh(), which is wired to DataStore's
 * listener system and fires on every data change.
 */
public class BudgetPanel extends JPanel {

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();

    // ── Form controls ──────────────────────────────────────────────────────────
    private JTextField                  budgetField;
    private JComboBox<DataStore.BudgetPeriod> periodBox;
    private JLabel                      periodInfoLabel;

    // ── Status display ─────────────────────────────────────────────────────────
    private JLabel      budgetDisplayLabel;
    private JLabel      proratedInfoLabel;
    private JProgressBar progressBar;
    private JLabel      statusLabel;

    // ── Stat cards ─────────────────────────────────────────────────────────────
    private JLabel spentLabel;
    private JLabel proratedLabel;
    private JLabel remainLabel;
    private JLabel dailyAllowanceLabel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public BudgetPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildSettingsCard(), BorderLayout.NORTH);
        add(buildStatusPanel(),  BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    // ── Budget settings card ───────────────────────────────────────────────────

    private JPanel buildSettingsCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(UITheme.accentCardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("💰  Budget Settings");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        panel.add(title, gbc);

        // Row 1 — period selector
        gbc.gridwidth = 1; gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Budget Period:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        periodBox = new JComboBox<>(DataStore.BudgetPeriod.values());
        periodBox.setSelectedItem(store.getBudgetPeriod());
        UITheme.styleCombo(periodBox);
        panel.add(periodBox, gbc);

        // Row 1 — budget amount
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(UITheme.SymbolLabel("Budget Amount (\u20B1):"), gbc);

        gbc.gridx = 3; gbc.weightx = 1.0;
        budgetField = UITheme.textField("0.00");
        panel.add(budgetField, gbc);

        // Row 2 — period hint + set button
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        periodInfoLabel = new JLabel(" ");
        periodInfoLabel.setFont(UITheme.SMALL_FONT);
        periodInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(periodInfoLabel, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        JButton setBtn = UITheme.accentButton("Set Budget");
        setBtn.addActionListener(__ -> applyBudget());
        panel.add(setBtn, gbc);

        // Refresh hint whenever period selection changes
        periodBox.addActionListener(__ -> updatePeriodHint());
        updatePeriodHint();

        return panel;
    }

    // ── Status panel (progress + stat cards) ──────────────────────────────────

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG);

        panel.add(Box.createVerticalStrut(16));

        // Budget header label
        budgetDisplayLabel = new JLabel(" ");
        budgetDisplayLabel.setFont(UITheme.PESO_FONT_BOLD);
        budgetDisplayLabel.setForeground(UITheme.TEXT_PRIMARY);
        budgetDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(budgetDisplayLabel);

        panel.add(Box.createVerticalStrut(4));

        // Daily rate + prorated info line
        proratedInfoLabel = new JLabel(" ");
        proratedInfoLabel.setFont(UITheme.SMALL_FONT);
        proratedInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        proratedInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(proratedInfoLabel);

        panel.add(Box.createVerticalStrut(16));

        // Progress bar panel
        JPanel progressPanel = new JPanel(new BorderLayout(0, 6));
        progressPanel.setBackground(UITheme.BG);
        progressPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(UITheme.BODY_FONT);
        progressBar.setPreferredSize(new Dimension(400, 30));
        progressBar.setBackground(UITheme.CARD);
        progressBar.setForeground(UITheme.SUCCESS);
        progressBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        progressPanel.add(progressBar, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.BODY_FONT);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);

        panel.add(progressPanel);
        panel.add(Box.createVerticalStrut(16));

        // Stat cards: This Period Spent | Prorated Budget | Remaining Today | Spend Today
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(UITheme.BG);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        spentLabel          = buildStatCard("This Period Spent", UITheme.DANGER);
        proratedLabel       = buildStatCard("Prorated Budget",   UITheme.ACCENT);
        remainLabel         = buildStatCard("Remaining Today",   UITheme.SUCCESS);
        dailyAllowanceLabel = buildStatCard("Spend Today",       UITheme.WARNING);

        statsRow.add(buildStatWrapper(spentLabel));
        statsRow.add(buildStatWrapper(proratedLabel));
        statsRow.add(buildStatWrapper(remainLabel));
        statsRow.add(buildStatWrapper(dailyAllowanceLabel));

        panel.add(statsRow);
        return panel;
    }

    // ── Stat card helpers ──────────────────────────────────────────────────────

    private JLabel buildStatCard(String title, Color color) {
        JLabel lbl = new JLabel(statHtml(title, "\u20B10.00"));
        lbl.setForeground(color);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JPanel buildStatWrapper(JLabel label) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.CARD);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        wrapper.add(label, BorderLayout.CENTER);
        return wrapper;
    }

    /** Builds the two-line HTML used by every stat card label. */
    private String statHtml(String title, String value) {
        return String.format(
                "<html><center>"
                        + "<span style='font-family:Arial;font-size:11px;'>%s</span>"
                        + "<br><b style='font-family:Arial;font-size:18px;'>%s</b>"
                        + "</center></html>",
                title, value
        );
    }

    // ── Period hint ────────────────────────────────────────────────────────────

    private void updatePeriodHint() {
        DataStore.BudgetPeriod p = (DataStore.BudgetPeriod) periodBox.getSelectedItem();
        if (p == null) return;
        periodInfoLabel.setText(String.format(
                "Day %d of %d in current %s  \u2014  budget resets to 0 on period change",
                p.daysElapsed(), p.days, p.displayName.toLowerCase()
        ));
    }

    // ── Budget apply action ────────────────────────────────────────────────────

    private void applyBudget() {
        try {
            double budget = Double.parseDouble(budgetField.getText().trim());
            if (budget <= 0) throw new NumberFormatException();

            DataStore.BudgetPeriod selected = (DataStore.BudgetPeriod) periodBox.getSelectedItem();
            if (selected != store.getBudgetPeriod()) {
                store.setBudgetPeriod(selected); // also zeroes budget
            }
            store.setMonthlyBudget(budget);

        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid budget amount.");
        }
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        DataStore.BudgetPeriod period    = store.getBudgetPeriod();
        double budget    = store.getMonthlyBudget();
        double spent     = store.getTotalExpensesForCurrentPeriod();
        double prorated  = store.getProratedBudget();
        double remaining = store.getRemainingProrated();
        int    elapsed   = period.daysElapsed();

        // Sync form controls to current store state
        periodBox.setSelectedItem(period);
        budgetField.setText(String.format("%.2f", budget));

        // Header label
        budgetDisplayLabel.setText(String.format(
                "%s Budget: \u20B1%.2f  (Day %d of %d)",
                period.displayName, budget, elapsed, period.days
        ));

        // Daily rate + prorated info line
        double dailyRate = period.days > 0 ? budget / period.days : 0;
        proratedInfoLabel.setText(String.format(
                "Daily rate: \u20B1%.2f  \u2022  Prorated budget for today: \u20B1%.2f",
                dailyRate, prorated
        ));

        // Progress bar — spending vs prorated ceiling
        int percent = prorated > 0 ? (int) Math.min((spent / prorated) * 100, 100) : 0;
        progressBar.setValue(percent);
        progressBar.setString(percent + "% of prorated budget used");

        if (percent >= 100) {
            progressBar.setForeground(UITheme.DANGER);
            statusLabel.setText("\u26A0 You've exceeded the prorated budget for today.");
            statusLabel.setForeground(UITheme.DANGER);
        } else if (percent >= 80) {
            progressBar.setForeground(UITheme.WARNING);
            statusLabel.setText("\u26A1 Getting close to today's prorated limit.");
            statusLabel.setForeground(UITheme.WARNING);
        } else {
            progressBar.setForeground(UITheme.SUCCESS);
            statusLabel.setText("\u2705 On track with your " + period.displayName.toLowerCase() + " budget.");
            statusLabel.setForeground(UITheme.SUCCESS);
        }

        // Stat cards
        spentLabel.setText(statHtml("This Period Spent",
                String.format("\u20B1%.2f", spent)));

        proratedLabel.setText(statHtml("Prorated Budget",
                String.format("\u20B1%.2f", prorated)));

        remainLabel.setText(statHtml("Remaining Today",
                String.format("\u20B1%.2f", Math.max(remaining, 0))));

        dailyAllowanceLabel.setText(statHtml("Spend Today",
                String.format("\u20B1%.2f", store.getDailyAllowance())));

        updatePeriodHint();
    }
}