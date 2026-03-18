import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class BudgetPanel extends JPanel {
    private DataStore store = DataStore.getInstance();

    private JTextField budgetField;
    private JComboBox<DataStore.BudgetPeriod> periodBox;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel spentLabel;
    private JLabel proratedLabel;
    private JLabel remainLabel;
    private JLabel budgetDisplayLabel;
    private JLabel periodInfoLabel;

    public BudgetPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildSetBudgetPanel(), BorderLayout.NORTH);
        add(buildStatusPanel(), BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    private JPanel buildSetBudgetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(UITheme.accentCardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("💰  Budget Settings");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        panel.add(title, gbc);

        // Row 1: Period selector
        gbc.gridwidth = 1; gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Budget Period:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.5;
        periodBox = new JComboBox<>(DataStore.BudgetPeriod.values());
        periodBox.setSelectedItem(store.getBudgetPeriod());
        UITheme.styleCombo(periodBox);
        panel.add(periodBox, gbc);

        // Row 1: Budget amount
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(UITheme.SymbolLabel("Budget Amount (₱):"), gbc);

        gbc.gridx = 3; gbc.weightx = 1.0;
        budgetField = UITheme.textField("0.00");
        panel.add(budgetField, gbc);

        // Row 2: Set button + hint
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        periodInfoLabel = new JLabel(" ");
        periodInfoLabel.setFont(UITheme.SMALL_FONT);
        periodInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(periodInfoLabel, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        JButton setBtn = UITheme.accentButton("Set Budget");
        setBtn.addActionListener(e -> setBudget());
        panel.add(setBtn, gbc);

        // Update hint when period changes
        periodBox.addActionListener(e -> updatePeriodHint());
        updatePeriodHint();

        return panel;
    }

    private void updatePeriodHint() {
        DataStore.BudgetPeriod p = (DataStore.BudgetPeriod) periodBox.getSelectedItem();
        if (p == null) return;
        int elapsed = p.daysElapsed();
        periodInfoLabel.setText(String.format(
                "Day %d of %d in current %s  —  budget resets to 0 on period change",
                elapsed, p.days, p.displayName.toLowerCase()
        ));
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG);

        panel.add(Box.createVerticalStrut(16));

        budgetDisplayLabel = new JLabel(" ");
        budgetDisplayLabel.setFont(UITheme.PESO_FONT_BOLD);
        budgetDisplayLabel.setForeground(UITheme.TEXT_PRIMARY);
        budgetDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(budgetDisplayLabel);

        panel.add(Box.createVerticalStrut(4));

        periodInfoLabel = new JLabel(" ");  // placeholder, real one is in form
        JLabel proratedInfo = new JLabel(" ");
        proratedInfo.setFont(UITheme.SMALL_FONT);
        proratedInfo.setForeground(UITheme.TEXT_SECONDARY);
        proratedInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        // store ref so refresh() can update it
        this.proratedLabel = proratedInfo;
        panel.add(proratedInfo);

        panel.add(Box.createVerticalStrut(16));

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

        // Three stat cards: Spent / Prorated Budget / Remaining
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setBackground(UITheme.BG);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        spentLabel    = buildStatCard("Total Spent",      "₱0.00", UITheme.DANGER);
        proratedLabel = buildStatCard("Prorated Budget",  "₱0.00", UITheme.ACCENT);
        remainLabel   = buildStatCard("Remaining Today",  "₱0.00", UITheme.SUCCESS);

        statsRow.add(buildStatWrapper(spentLabel));
        statsRow.add(buildStatWrapper(proratedLabel));
        statsRow.add(buildStatWrapper(remainLabel));

        panel.add(statsRow);

        return panel;
    }

    private JLabel buildStatCard(String title, String value, Color color) {
        JLabel lbl = new JLabel(String.format(
                "<html><center><span style='font-family:Arial;font-size:11px;'>%s</span>" +
                        "<br><b style='font-family:Arial;font-size:18px;'>%s</b></center></html>", title, value));
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

    private void setBudget() {
        try {
            double budget = Double.parseDouble(budgetField.getText().trim());
            if (budget <= 0) throw new NumberFormatException();

            DataStore.BudgetPeriod selectedPeriod =
                    (DataStore.BudgetPeriod) periodBox.getSelectedItem();

            // If period changed, reset budget first (DataStore handles zeroing)
            if (selectedPeriod != store.getBudgetPeriod()) {
                store.setBudgetPeriod(selectedPeriod);
            }
            store.setMonthlyBudget(budget);

        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid budget amount.");
        }
    }

    private void refresh() {
        DataStore.BudgetPeriod period = store.getBudgetPeriod();
        double budget    = store.getMonthlyBudget();
        double spent     = store.getTotalExpenses();
        double prorated  = store.getProratedBudget();
        double remaining = store.getRemainingProrated();
        int elapsed      = period.daysElapsed();

        // Sync period box to store without triggering action listener
        periodBox.setSelectedItem(period);
        budgetField.setText(String.format("%.2f", budget));

        // Header display
        budgetDisplayLabel.setText(String.format(
                "%s Budget: ₱%.2f  (Day %d of %d)",
                period.displayName, budget, elapsed, period.days
        ));

        // Prorated info line
        double dailyRate = period.days > 0 ? budget / period.days : 0;
        proratedLabel.setText(String.format(
                "Daily rate: ₱%.2f  •  Prorated budget for today: ₱%.2f",
                dailyRate, prorated
        ));

        // Progress against prorated amount
        int percent = prorated > 0 ? (int) Math.min((spent / prorated) * 100, 100) : 0;
        progressBar.setValue(percent);
        progressBar.setString(percent + "% of prorated budget used");

        if (percent >= 100) {
            progressBar.setForeground(UITheme.DANGER);
            statusLabel.setText("⚠ You've exceeded the prorated budget for today.");
            statusLabel.setForeground(UITheme.DANGER);
        } else if (percent >= 80) {
            progressBar.setForeground(UITheme.WARNING);
            statusLabel.setText("⚡ Getting close to today's prorated limit.");
            statusLabel.setForeground(UITheme.WARNING);
        } else {
            progressBar.setForeground(UITheme.SUCCESS);
            statusLabel.setText("✅ On track with your " + period.displayName.toLowerCase() + " budget.");
            statusLabel.setForeground(UITheme.SUCCESS);
        }

        // Stat cards
        spentLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:11px;'>Total Spent</span>" +
                        "<br><b style='font-family:Arial;font-size:18px;'>₱%.2f</b></center></html>", spent));

        proratedLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:11px;'>Prorated Budget</span>" +
                        "<br><b style='font-family:Arial;font-size:18px;'>₱%.2f</b></center></html>", prorated));

        remainLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:11px;'>Remaining Today</span>" +
                        "<br><b style='font-family:Arial;font-size:18px;'>₱%.2f</b></center></html>",
                Math.max(remaining, 0)));

        updatePeriodHint();
    }
}