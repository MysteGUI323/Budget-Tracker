import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * BudgetPanel — Budget tab UI.
 *
 * Layout (top to bottom):
 *   - Budget Settings card: period selector + amount input
 *     + needs % input (Advanced only) + mode toggle buttons
 *   - Shared header: budget label + prorated info line
 *   - CardLayout view switcher:
 *       BASIC   — single progress bar + 4 stat cards
 *       ADVANCED — total progress bar + NEEDS card + WANTS card (each with 4 stat cards)
 *
 * The active mode is stored in DataStore and persisted between sessions.
 * Switching modes shows/hides the needs % input and swaps the CardLayout view.
 */
public class BudgetPanel extends JPanel {

    // ── Card keys ──────────────────────────────────────────────────────────────
    private static final String CARD_BASIC    = "BASIC";
    private static final String CARD_ADVANCED = "ADVANCED";

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();

    // ── Form controls ──────────────────────────────────────────────────────────
    private JTextField                        budgetField;
    private JTextField                        needsPercentField;
    private JComboBox<DataStore.BudgetPeriod> periodBox;
    private JLabel                            periodInfoLabel;
    private JLabel                            needsPercentLabel; // hidden in basic mode
    private JButton                           basicBtn;
    private JButton                           advancedBtn;

    // ── Shared header display ──────────────────────────────────────────────────
    private JLabel budgetDisplayLabel;
    private JLabel proratedInfoLabel;

    // ── CardLayout container ───────────────────────────────────────────────────
    private CardLayout cardLayout;
    private JPanel     cardPanel;

    // ── Basic mode components ──────────────────────────────────────────────────
    private JProgressBar basicProgressBar;
    private JLabel       basicStatusLabel;
    private JLabel       basicShouldveLabel;
    private JLabel       basicActuallyLabel;
    private JLabel       basicBufferLabel;
    private JLabel       basicSafeLabel;

    // ── Advanced mode — needs ──────────────────────────────────────────────────
    private JProgressBar needsProgressBar;
    private JLabel       needsStatusLabel;
    private JLabel       needsShouldveLabel;
    private JLabel       needsActuallyLabel;
    private JLabel       needsBufferLabel;
    private JLabel       needsSafeLabel;

    // ── Advanced mode — wants ──────────────────────────────────────────────────
    private JProgressBar wantsProgressBar;
    private JLabel       wantsStatusLabel;
    private JLabel       wantsShouldveLabel;
    private JLabel       wantsActuallyLabel;
    private JLabel       wantsBufferLabel;
    private JLabel       wantsSafeLabel;

    // ── Advanced total bar ─────────────────────────────────────────────────────
    private JProgressBar advTotalProgressBar;
    private JLabel       advTotalStatusLabel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public BudgetPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildSettingsCard(), BorderLayout.NORTH);
        add(buildMainArea(),     BorderLayout.CENTER);

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

        // Column layout (4 columns: 0=label, 1=input, 2=label, 3=input+button)
        // weightx: 0 for labels, 1.0 for inputs, 0 for buttons

        // Title row
        JLabel title = new JLabel("\uD83D\uDCB0  Budget Settings");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4; gbc.weightx = 0;
        panel.add(title, gbc);

        // Row 1 — period selector | budget amount field + Set Budget button
        gbc.gridwidth = 1; gbc.gridy = 1;

        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Budget Period:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        periodBox = new JComboBox<>(DataStore.BudgetPeriod.values());
        periodBox.setSelectedItem(store.getBudgetPeriod());
        UITheme.styleCombo(periodBox);
        panel.add(periodBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(UITheme.SymbolLabel("Budget Amount (\u20B1):"), gbc);

        // Budget field + Set Budget button in a sub-panel so they sit together
        JPanel budgetInputPanel = new JPanel(new BorderLayout(6, 0));
        budgetInputPanel.setBackground(UITheme.CARD);
        budgetField = UITheme.textField("0.00");
        JButton setBtn = UITheme.accentButton("Set Budget");
        setBtn.addActionListener(e -> applyBudget());
        budgetInputPanel.add(budgetField, BorderLayout.CENTER);
        budgetInputPanel.add(setBtn,      BorderLayout.EAST);

        gbc.gridx = 3; gbc.weightx = 1.0;
        panel.add(budgetInputPanel, gbc);

        // Row 2 — needs % (advanced only) | mode toggle buttons
        gbc.gridy = 2;

        gbc.gridx = 0; gbc.weightx = 0;
        needsPercentLabel = UITheme.label("Needs % (rest = Wants):");
        panel.add(needsPercentLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        needsPercentField = UITheme.textField("50");
        panel.add(needsPercentField, gbc);

        // Mode toggle buttons — span columns 2-3, GridLayout forces equal width
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        btnPanel.setBackground(UITheme.CARD);

        basicBtn = UITheme.accentButton("Basic");
        basicBtn.addActionListener(e -> switchMode(DataStore.TrackingMode.BASIC));
        btnPanel.add(basicBtn);

        advancedBtn = UITheme.accentButton("Advanced");
        advancedBtn.addActionListener(e -> switchMode(DataStore.TrackingMode.ADVANCED));
        btnPanel.add(advancedBtn);

        gbc.gridx = 2; gbc.weightx = 0; gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        // Row 3 — period hint
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 4; gbc.weightx = 1.0;
        periodInfoLabel = new JLabel(" ");
        periodInfoLabel.setFont(UITheme.SMALL_FONT);
        periodInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(periodInfoLabel, gbc);

        periodBox.addActionListener(e -> updatePeriodHint());
        updatePeriodHint();

        return panel;
    }

    // ── Main area: shared header + card switcher ───────────────────────────────

    private JPanel buildMainArea() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(UITheme.BG);

        // Shared header labels
        JPanel headerArea = new JPanel();
        headerArea.setLayout(new BoxLayout(headerArea, BoxLayout.Y_AXIS));
        headerArea.setBackground(UITheme.BG);
        headerArea.setBorder(new EmptyBorder(16, 0, 12, 0));

        budgetDisplayLabel = new JLabel(" ");
        budgetDisplayLabel.setFont(UITheme.PESO_FONT_BOLD);
        budgetDisplayLabel.setForeground(UITheme.TEXT_PRIMARY);
        budgetDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerArea.add(budgetDisplayLabel);

        headerArea.add(Box.createVerticalStrut(4));

        proratedInfoLabel = new JLabel(" ");
        proratedInfoLabel.setFont(UITheme.SMALL_FONT);
        proratedInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        proratedInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerArea.add(proratedInfoLabel);

        panel.add(headerArea, BorderLayout.NORTH);

        // CardLayout switcher — fills the remaining center space
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(UITheme.BG);

        cardPanel.add(buildBasicView(),    CARD_BASIC);
        cardPanel.add(buildAdvancedView(), CARD_ADVANCED);

        panel.add(cardPanel, BorderLayout.CENTER);
        return panel;
    }

    // ── Basic view ─────────────────────────────────────────────────────────────

    private JPanel buildBasicView() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(UITheme.BG);

        // Progress bar
        JPanel progressPanel = new JPanel(new BorderLayout(0, 4));
        progressPanel.setBackground(UITheme.BG);

        basicProgressBar = new JProgressBar(0, 100);
        basicProgressBar.setStringPainted(true);
        basicProgressBar.setFont(UITheme.BODY_FONT);
        basicProgressBar.setPreferredSize(new Dimension(400, 26));
        basicProgressBar.setBackground(UITheme.CARD);
        basicProgressBar.setForeground(UITheme.SUCCESS);
        basicProgressBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        progressPanel.add(basicProgressBar, BorderLayout.CENTER);

        basicStatusLabel = new JLabel(" ");
        basicStatusLabel.setFont(UITheme.BODY_FONT);
        basicStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        basicStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressPanel.add(basicStatusLabel, BorderLayout.SOUTH);

        // 4 stat cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(UITheme.BG);

        basicShouldveLabel = buildStatCard("Should've Spent", UITheme.ACCENT);
        basicActuallyLabel = buildStatCard("Actually Spent",  UITheme.DANGER);
        basicBufferLabel   = buildStatCard("Today's Buffer",  UITheme.SUCCESS);
        basicSafeLabel     = buildStatCard("Safe to Spend",   UITheme.WARNING);

        statsRow.add(buildStatWrapper(basicShouldveLabel));
        statsRow.add(buildStatWrapper(basicActuallyLabel));
        statsRow.add(buildStatWrapper(basicBufferLabel));
        statsRow.add(buildStatWrapper(basicSafeLabel));

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setBackground(UITheme.BG);
        north.add(progressPanel, BorderLayout.NORTH);
        north.add(statsRow,      BorderLayout.CENTER);

        panel.add(north, BorderLayout.NORTH);
        return panel;
    }

    // ── Advanced view ──────────────────────────────────────────────────────────

    private JPanel buildAdvancedView() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG);

        // Total progress bar
        JPanel totalProgressPanel = new JPanel(new BorderLayout(0, 4));
        totalProgressPanel.setBackground(UITheme.BG);

        advTotalProgressBar = new JProgressBar(0, 100);
        advTotalProgressBar.setStringPainted(true);
        advTotalProgressBar.setFont(UITheme.BODY_FONT);
        advTotalProgressBar.setPreferredSize(new Dimension(400, 26));
        advTotalProgressBar.setBackground(UITheme.CARD);
        advTotalProgressBar.setForeground(UITheme.SUCCESS);
        advTotalProgressBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        totalProgressPanel.add(advTotalProgressBar, BorderLayout.CENTER);

        advTotalStatusLabel = new JLabel(" ");
        advTotalStatusLabel.setFont(UITheme.SMALL_FONT);
        advTotalStatusLabel.setForeground(UITheme.TEXT_SECONDARY);
        advTotalStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalProgressPanel.add(advTotalStatusLabel, BorderLayout.SOUTH);

        panel.add(totalProgressPanel);
        panel.add(Box.createVerticalStrut(12));

        // Needs + wants pool cards side by side
        JPanel splitRow = new JPanel(new GridLayout(1, 2, 12, 0));
        splitRow.setBackground(UITheme.BG);

        splitRow.add(buildPoolCard(true));
        splitRow.add(buildPoolCard(false));

        panel.add(totalProgressPanel, BorderLayout.NORTH);
        panel.add(splitRow,           BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds a single needs or wants pool card containing:
     *   - Section label + category hint
     *   - Progress bar vs prorated ceiling
     *   - Status label
     *   - 4 stat cards: Should've Spent | Actually Spent | Today's Buffer | Safe to Spend
     */
    private JPanel buildPoolCard(boolean isNeeds) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(isNeeds ? UITheme.ACCENT : UITheme.WARNING, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        // Header area — section label + category hint
        JPanel headerArea = new JPanel(new BorderLayout(0, 2));
        headerArea.setBackground(UITheme.CARD);

        JLabel sectionLabel = new JLabel(isNeeds ? "\uD83C\uDFE0  NEEDS" : "\uD83C\uDF89  WANTS");
        sectionLabel.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        sectionLabel.setForeground(isNeeds ? UITheme.ACCENT : UITheme.WARNING);
        headerArea.add(sectionLabel, BorderLayout.NORTH);

        JLabel hint = new JLabel(isNeeds
                ? "Food, Transport, School, Health"
                : "Entertainment, Shopping, Other"
        );
        hint.setFont(UITheme.SMALL_FONT);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        headerArea.add(hint, BorderLayout.SOUTH);

        card.add(headerArea, BorderLayout.NORTH);

        // Center — progress bar + status label
        JPanel barArea = new JPanel(new BorderLayout(0, 4));
        barArea.setBackground(UITheme.CARD);
        barArea.setBorder(new EmptyBorder(8, 0, 8, 0));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setFont(UITheme.SMALL_FONT);
        bar.setPreferredSize(new Dimension(0, 22));
        bar.setBackground(UITheme.BG);
        bar.setForeground(UITheme.SUCCESS);
        bar.setBorder(new LineBorder(UITheme.BORDER, 1));
        barArea.add(bar, BorderLayout.CENTER);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(UITheme.SMALL_FONT);
        statusLbl.setForeground(UITheme.TEXT_SECONDARY);
        barArea.add(statusLbl, BorderLayout.SOUTH);

        card.add(barArea, BorderLayout.CENTER);

        // Bottom — 4 stat cards filling full width
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 6, 0));
        statsRow.setBackground(UITheme.CARD);

        JLabel shouldveLbl = buildStatCard("Should've Spent", UITheme.ACCENT);
        JLabel actuallyLbl = buildStatCard("Actually Spent",  UITheme.DANGER);
        JLabel bufferLbl   = buildStatCard("Today's Buffer",  UITheme.SUCCESS);
        JLabel safeLbl     = buildStatCard("Safe to Spend",   UITheme.WARNING);

        statsRow.add(buildStatWrapper(shouldveLbl));
        statsRow.add(buildStatWrapper(actuallyLbl));
        statsRow.add(buildStatWrapper(bufferLbl));
        statsRow.add(buildStatWrapper(safeLbl));

        card.add(statsRow, BorderLayout.SOUTH);

        // Store references for refresh()
        if (isNeeds) {
            needsProgressBar   = bar;
            needsStatusLabel   = statusLbl;
            needsShouldveLabel = shouldveLbl;
            needsActuallyLabel = actuallyLbl;
            needsBufferLabel   = bufferLbl;
            needsSafeLabel     = safeLbl;
        } else {
            wantsProgressBar   = bar;
            wantsStatusLabel   = statusLbl;
            wantsShouldveLabel = shouldveLbl;
            wantsActuallyLabel = actuallyLbl;
            wantsBufferLabel   = bufferLbl;
            wantsSafeLabel     = safeLbl;
        }

        return card;
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
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        wrapper.add(label, BorderLayout.CENTER);
        return wrapper;
    }

    /** Builds the two-line HTML used by every stat card label. */
    private String statHtml(String title, String value) {
        return String.format(
                "<html><center>"
                        + "<span style='font-family:Arial;font-size:10px;'>%s</span>"
                        + "<br><b style='font-family:Arial;font-size:14px;'>%s</b>"
                        + "</center></html>",
                title, value
        );
    }

    // ── Mode switch ────────────────────────────────────────────────────────────

    private void switchMode(DataStore.TrackingMode mode) {
        store.setTrackingMode(mode); // persists + triggers refresh()
    }

    /** Updates button appearance and needs % field visibility to match active mode. */
    private void applyModeUI(DataStore.TrackingMode mode) {
        boolean isAdvanced = mode == DataStore.TrackingMode.ADVANCED;

        // Show/hide needs % input
        needsPercentLabel.setVisible(isAdvanced);
        needsPercentField.setVisible(isAdvanced);

        // Highlight active button
        basicBtn.setBackground(isAdvanced ? UITheme.BORDER : UITheme.ACCENT);
        advancedBtn.setBackground(isAdvanced ? UITheme.ACCENT : UITheme.BORDER);

        // Swap card
        cardLayout.show(cardPanel, isAdvanced ? CARD_ADVANCED : CARD_BASIC);
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
                store.setBudgetPeriod(selected);
            }
            store.setMonthlyBudget(budget);

            // Only apply needs % in advanced mode
            if (store.getTrackingMode() == DataStore.TrackingMode.ADVANCED) {
                double needsPct = Double.parseDouble(needsPercentField.getText().trim());
                if (needsPct < 0 || needsPct > 100) throw new NumberFormatException();
                store.setNeedsPercent(needsPct);
            }

        } catch (NumberFormatException ex) {
            UITheme.showError(this, store.getTrackingMode() == DataStore.TrackingMode.ADVANCED
                    ? "Enter a valid budget amount and needs % (0-100)."
                    : "Enter a valid budget amount."
            );
        }
    }

    // ── Progress bar color + status helper ────────────────────────────────────

    private void updateBarColor(JProgressBar bar, JLabel statusLbl, int percent, String poolName) {
        if (percent >= 100) {
            bar.setForeground(UITheme.DANGER);
            statusLbl.setText("\u26A0 Exceeded prorated " + poolName + " budget.");
            statusLbl.setForeground(UITheme.DANGER);
        } else if (percent >= 80) {
            bar.setForeground(UITheme.WARNING);
            statusLbl.setText("\u26A1 Getting close to the " + poolName + " prorated limit.");
            statusLbl.setForeground(UITheme.WARNING);
        } else {
            bar.setForeground(UITheme.SUCCESS);
            statusLbl.setText("\u2705 On track with " + poolName + " budget.");
            statusLbl.setForeground(UITheme.SUCCESS);
        }
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        DataStore.TrackingMode mode   = store.getTrackingMode();
        DataStore.BudgetPeriod period = store.getBudgetPeriod();
        double budget   = store.getMonthlyBudget();
        double prorated = store.getProratedBudget();
        double spent    = store.getTotalExpensesForCurrentPeriod();
        int    elapsed  = period.daysElapsed();

        // Apply mode UI (card swap + button highlight + needs % visibility)
        applyModeUI(mode);

        // Sync form controls
        periodBox.setSelectedItem(period);
        budgetField.setText(String.format("%.2f", budget));
        needsPercentField.setText(String.format("%.0f", store.getNeedsPercent()));

        // Shared header label
        if (mode == DataStore.TrackingMode.ADVANCED) {
            budgetDisplayLabel.setText(String.format(
                    "%s Budget: \u20B1%.2f  (Day %d of %d)  \u2014  Needs: %.0f%%  /  Wants: %.0f%%",
                    period.displayName, budget, elapsed, period.days,
                    store.getNeedsPercent(), store.getWantsPercent()
            ));
        } else {
            budgetDisplayLabel.setText(String.format(
                    "%s Budget: \u20B1%.2f  (Day %d of %d)",
                    period.displayName, budget, elapsed, period.days
            ));
        }

        // Shared prorated info line
        double dailyRate = period.days > 0 ? budget / period.days : 0;
        proratedInfoLabel.setText(String.format(
                "Daily rate: \u20B1%.2f  \u2022  Prorated budget for today: \u20B1%.2f",
                dailyRate, prorated
        ));

        // ── Basic view ─────────────────────────────────────────────────────────
        int basicPct = prorated > 0 ? (int) Math.min((spent / prorated) * 100, 100) : 0;
        basicProgressBar.setValue(basicPct);
        basicProgressBar.setString(basicPct + "% of prorated budget used");
        updateBarColor(basicProgressBar, basicStatusLabel, basicPct, period.displayName.toLowerCase());

        basicShouldveLabel.setText(statHtml("Should've Spent", String.format("\u20B1%.2f", prorated)));
        basicActuallyLabel.setText(statHtml("Actually Spent",  String.format("\u20B1%.2f", spent)));
        basicBufferLabel.setText(statHtml("Today's Buffer",    String.format("\u20B1%.2f", Math.max(store.getRemainingProrated(), 0))));
        basicSafeLabel.setText(statHtml("Safe to Spend",       String.format("\u20B1%.2f", store.getDailyAllowance())));

        // ── Advanced view ──────────────────────────────────────────────────────
        int advTotalPct = prorated > 0 ? (int) Math.min((spent / prorated) * 100, 100) : 0;
        advTotalProgressBar.setValue(advTotalPct);
        advTotalProgressBar.setString(advTotalPct + "% of total prorated budget used");
        updateBarColor(advTotalProgressBar, advTotalStatusLabel, advTotalPct, "total " + period.displayName.toLowerCase());

        // Needs
        double needsProrated = store.getNeedsProratedBudget();
        double needsSpent    = store.getNeedsSpentThisPeriod();
        double needsRemain   = store.getNeedsRemainingProrated();
        int    needsPct      = needsProrated > 0 ? (int) Math.min((needsSpent / needsProrated) * 100, 100) : 0;

        needsProgressBar.setValue(needsPct);
        needsProgressBar.setString(needsPct + "% of needs prorated budget used");
        updateBarColor(needsProgressBar, needsStatusLabel, needsPct, "needs");

        needsShouldveLabel.setText(statHtml("Should've Spent", String.format("\u20B1%.2f", needsProrated)));
        needsActuallyLabel.setText(statHtml("Actually Spent",  String.format("\u20B1%.2f", needsSpent)));
        needsBufferLabel.setText(statHtml("Today's Buffer",    String.format("\u20B1%.2f", Math.max(needsRemain, 0))));
        needsSafeLabel.setText(statHtml("Safe to Spend",       String.format("\u20B1%.2f", store.getNeedsDailyAllowance())));

        // Wants
        double wantsProrated = store.getWantsProratedBudget();
        double wantsSpent    = store.getWantsSpentThisPeriod();
        double wantsRemain   = store.getWantsRemainingProrated();
        int    wantsPct      = wantsProrated > 0 ? (int) Math.min((wantsSpent / wantsProrated) * 100, 100) : 0;

        wantsProgressBar.setValue(wantsPct);
        wantsProgressBar.setString(wantsPct + "% of wants prorated budget used");
        updateBarColor(wantsProgressBar, wantsStatusLabel, wantsPct, "wants");

        wantsShouldveLabel.setText(statHtml("Should've Spent", String.format("\u20B1%.2f", wantsProrated)));
        wantsActuallyLabel.setText(statHtml("Actually Spent",  String.format("\u20B1%.2f", wantsSpent)));
        wantsBufferLabel.setText(statHtml("Today's Buffer",    String.format("\u20B1%.2f", Math.max(wantsRemain, 0))));
        wantsSafeLabel.setText(statHtml("Safe to Spend",       String.format("\u20B1%.2f", store.getWantsDailyAllowance())));

        updatePeriodHint();
    }
}