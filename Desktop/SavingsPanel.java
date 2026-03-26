import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * SavingsPanel — Savings tab UI.
 *
 * Layout (top to bottom):
 *   TOP    — two side-by-side cards: Set Savings Goal | Add to Savings
 *   CENTER — two side-by-side cards: Savings Progress | Level Badge (Saver Rank)
 *   GOALS  — savings goals section: add form + scrollable goal cards
 *   BOTTOM — XP progress bar panel
 *
 * Multi-goal behavior:
 *   - All goals share the same currentSavings pool
 *   - Each goal shows its own progress bar filled by currentSavings vs target
 *   - When affordable (100%), a Claim button appears
 *   - Claiming deducts the target from currentSavings and marks achieved
 *   - Goals can be removed at any time via Remove button
 */
public class SavingsPanel extends JPanel {

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();
    private int lastLevel = 1;

    // ── Form controls ──────────────────────────────────────────────────────────
    private JTextField goalField;
    private JTextField depositField;
    private JTextField withdrawField;
    private JTextField goalNameField;
    private JTextField goalTargetField;

    // ── Progress display ───────────────────────────────────────────────────────
    private JProgressBar savingsBar;
    private JLabel       goalDisplayLabel;
    private JLabel       motivationLabel;
    private JLabel       savedLabel;
    private JLabel       remainingLabel;

    // ── Level badge ────────────────────────────────────────────────────────────
    private JPanel levelBadge;
    private JLabel levelLabel;
    private JLabel titleLabel;
    private JLabel xpInfoLabel;

    // ── XP bar ─────────────────────────────────────────────────────────────────
    private JProgressBar xpBar;

    // ── Goals list panel (rebuilt on refresh) ─────────────────────────────────
    private JPanel goalsListPanel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public SavingsPanel() {
        setLayout(new BorderLayout(10, 12));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Wrap everything in a scroll pane so goals don't get clipped
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG);

        JPanel top    = buildTopPanel();
        JPanel center = buildCenterPanel();
        JPanel goals  = buildGoalsSection();
        JPanel xp     = buildXPPanel();

        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.setAlignmentX(Component.LEFT_ALIGNMENT);
        goals.setAlignmentX(Component.LEFT_ALIGNMENT);
        xp.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(top);
        content.add(Box.createVerticalStrut(12));
        content.add(center);
        content.add(Box.createVerticalStrut(12));
        content.add(goals);
        content.add(Box.createVerticalStrut(12));
        content.add(xp);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    // ── TOP: Set Goal + Deposit ────────────────────────────────────────────────

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setBackground(UITheme.BG);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        // Set Goal card
        JPanel goalCard = buildCard(UITheme.ACCENT);
        GridBagConstraints gbc = cardGbc();

        JLabel goalTitle = new JLabel("\uD83C\uDFAF  Set Savings Goal");
        goalTitle.setFont(UITheme.HEADER_FONT);
        goalTitle.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        goalCard.add(goalTitle, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0;
        goalCard.add(UITheme.SymbolLabel("Goal Amount (\u20B1):"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        goalField = UITheme.textField("1000.00");
        goalCard.add(goalField, gbc);

        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2;
        JButton setGoalBtn = UITheme.accentButton("Set Goal");
        setGoalBtn.addActionListener(e -> applyGoal());
        goalCard.add(setGoalBtn, gbc);

        // Add to Savings card
        JPanel depositCard = buildCard(UITheme.SUCCESS);
        GridBagConstraints gbc2 = cardGbc();

        JLabel depositTitle = new JLabel("\uD83D\uDCB5  Add / Withdraw Savings");
        depositTitle.setFont(UITheme.HEADER_FONT);
        depositTitle.setForeground(UITheme.SUCCESS);
        gbc2.gridx = 0; gbc2.gridy = 0; gbc2.gridwidth = 2;
        depositCard.add(depositTitle, gbc2);

        // Deposit row
        gbc2.gridwidth = 1; gbc2.gridy = 1;
        gbc2.gridx = 0; gbc2.weightx = 0;
        depositCard.add(UITheme.SymbolLabel("Amount to Save (\u20B1):"), gbc2);

        gbc2.gridx = 1; gbc2.weightx = 1.0;
        depositField = UITheme.textField("0.00");
        depositCard.add(depositField, gbc2);

        gbc2.gridy = 2; gbc2.gridx = 0; gbc2.gridwidth = 2;
        JButton depositBtn = UITheme.successButton("Add Savings  +XP");
        depositBtn.addActionListener(e -> deposit());
        depositCard.add(depositBtn, gbc2);

        // Divider
        gbc2.gridy = 3; gbc2.gridwidth = 2;
        JSeparator divider = new JSeparator();
        divider.setForeground(UITheme.BORDER);
        depositCard.add(divider, gbc2);

        // Withdraw row
        gbc2.gridwidth = 1; gbc2.gridy = 4;
        gbc2.gridx = 0; gbc2.weightx = 0;
        depositCard.add(UITheme.SymbolLabel("Amount to Withdraw (\u20B1):"), gbc2);

        gbc2.gridx = 1; gbc2.weightx = 1.0;
        withdrawField = UITheme.textField("0.00");
        depositCard.add(withdrawField, gbc2);

        gbc2.gridy = 5; gbc2.gridx = 0; gbc2.gridwidth = 2;
        JButton withdrawBtn = UITheme.dangerButton("Withdraw  \u22121 XP");
        withdrawBtn.addActionListener(e -> withdraw());
        depositCard.add(withdrawBtn, gbc2);

        panel.add(goalCard);
        panel.add(depositCard);
        return panel;
    }

    // ── CENTER: Savings Progress + Level Badge ─────────────────────────────────

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setBackground(UITheme.BG);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        // Savings progress card
        JPanel progressCard = buildCard(UITheme.BORDER);
        GridBagConstraints gbc = cardGbc();

        JLabel progTitle = new JLabel("\uD83D\uDCC8  Savings Progress");
        progTitle.setFont(UITheme.HEADER_FONT);
        progTitle.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        progressCard.add(progTitle, gbc);

        gbc.gridy = 1;
        goalDisplayLabel = new JLabel("Goal: \u20B11,000.00");
        goalDisplayLabel.setFont(UITheme.PESO_FONT);
        goalDisplayLabel.setForeground(UITheme.TEXT_SECONDARY);
        progressCard.add(goalDisplayLabel, gbc);

        gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        savingsBar = new JProgressBar(0, 100);
        savingsBar.setStringPainted(true);
        savingsBar.setFont(UITheme.BODY_FONT);
        savingsBar.setPreferredSize(new Dimension(300, 26));
        savingsBar.setBackground(UITheme.BG);
        savingsBar.setForeground(UITheme.SUCCESS);
        savingsBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        progressCard.add(savingsBar, gbc);

        gbc.gridy = 3;
        motivationLabel = new JLabel(" ");
        motivationLabel.setFont(UITheme.BODY_FONT);
        motivationLabel.setForeground(UITheme.TEXT_SECONDARY);
        progressCard.add(motivationLabel, gbc);

        gbc.gridy = 4;
        JPanel statsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        statsRow.setBackground(UITheme.CARD);
        savedLabel     = statLabel(UITheme.SUCCESS, "Saved");
        remainingLabel = statLabel(UITheme.WARNING, "Still Needed");
        statsRow.add(savedLabel);
        statsRow.add(remainingLabel);
        progressCard.add(statsRow, gbc);

        levelBadge = buildLevelBadge();

        panel.add(progressCard);
        panel.add(levelBadge);
        return panel;
    }

    private JPanel buildLevelBadge() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx  = 0;
        gbc.gridy  = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill   = GridBagConstraints.NONE;

        JLabel badgeTitle = new JLabel("\uD83C\uDFC6  Saver Rank");
        badgeTitle.setFont(UITheme.HEADER_FONT);
        badgeTitle.setForeground(UITheme.ACCENT);
        card.add(badgeTitle, gbc);

        levelLabel = new JLabel("LEVEL 1");
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        levelLabel.setForeground(Color.WHITE);
        card.add(levelLabel, gbc);

        titleLabel = new JLabel(DataStore.TITLES[1]);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UITheme.WARNING);
        card.add(titleLabel, gbc);

        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER);
        sep.setPreferredSize(new Dimension(200, 1));
        card.add(sep, gbc);

        xpInfoLabel = new JLabel("0 / 100 XP to next level");
        xpInfoLabel.setFont(UITheme.SMALL_FONT);
        xpInfoLabel.setForeground(UITheme.TEXT_SECONDARY);
        card.add(xpInfoLabel, gbc);

        int nextIdx = Math.min(2, DataStore.TITLES.length - 1);
        JLabel nextTitle = new JLabel("Next: " + DataStore.TITLES[nextIdx]);
        nextTitle.setFont(UITheme.SMALL_FONT);
        nextTitle.setForeground(UITheme.TEXT_SECONDARY);
        card.add(nextTitle, gbc);

        return card;
    }

    // ── GOALS: Savings Goals section ───────────────────────────────────────────

    private JPanel buildGoalsSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(UITheme.BG);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Section header
        JLabel sectionTitle = new JLabel("\uD83C\uDFAF  Savings Goals");
        sectionTitle.setFont(UITheme.HEADER_FONT);
        sectionTitle.setForeground(UITheme.TEXT_PRIMARY);
        section.add(sectionTitle, BorderLayout.NORTH);

        // Add goal form
        JPanel addForm = new JPanel(new GridBagLayout());
        addForm.setBackground(UITheme.CARD);
        addForm.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.weightx = 0;
        addForm.add(UITheme.label("Goal Name:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        goalNameField = UITheme.textField("e.g. New Phone");
        addForm.add(goalNameField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        addForm.add(UITheme.SymbolLabel("Target (\u20B1):"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        goalTargetField = UITheme.textField("0.00");
        addForm.add(goalTargetField, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        JButton addGoalBtn = UITheme.accentButton("Add Goal");
        addGoalBtn.addActionListener(e -> addGoal());
        addForm.add(addGoalBtn, gbc);

        // Goals list panel — rebuilt in refresh()
        goalsListPanel = new JPanel();
        goalsListPanel.setLayout(new BoxLayout(goalsListPanel, BoxLayout.Y_AXIS));
        goalsListPanel.setBackground(UITheme.BG);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(UITheme.BG);
        center.add(addForm,        BorderLayout.NORTH);
        center.add(goalsListPanel, BorderLayout.CENTER);

        section.add(center, BorderLayout.CENTER);
        return section;
    }

    // ── BOTTOM: XP bar ─────────────────────────────────────────────────────────

    private JPanel buildXPPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(UITheme.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel xpTitle = new JLabel("\u26A1  Experience Points");
        xpTitle.setFont(UITheme.HEADER_FONT);
        xpTitle.setForeground(UITheme.WARNING);
        panel.add(xpTitle, BorderLayout.NORTH);

        xpBar = new JProgressBar(0, 100);
        xpBar.setStringPainted(true);
        xpBar.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        xpBar.setBackground(UITheme.BG);
        xpBar.setForeground(UITheme.WARNING);
        xpBar.setBorder(new LineBorder(UITheme.BORDER, 1));
        panel.add(xpBar, BorderLayout.CENTER);

        JLabel hint = new JLabel("  +1 XP per \u20B110 saved  \u2022  Minimum +5 XP per deposit");
        hint.setFont(UITheme.SYMBOL_SMALL_FONT);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    // ── Goal card builder ──────────────────────────────────────────────────────

    /**
     * Builds a single goal card showing:
     *   - Goal name + target amount
     *   - Progress bar (currentSavings / target)
     *   - Claim button (only when affordable and not yet achieved)
     *   - Achieved label (when claimed)
     *   - Remove button (always)
     */
    private JPanel buildGoalCard(DataStore.SavingsGoal goal, int index) {
        double saved   = store.getCurrentSavings();
        double target  = goal.getTargetAmount();
        int    pct     = target > 0 ? (int) Math.min((saved / target) * 100, 100) : 0;
        boolean canAfford = saved >= target;
        boolean achieved  = goal.isAchieved();

        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(achieved ? UITheme.SUCCESS : (canAfford ? UITheme.WARNING : UITheme.BORDER), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Left: name + progress bar
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBackground(UITheme.CARD);

        // Name + target row
        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setBackground(UITheme.CARD);

        JLabel nameLbl = new JLabel(goal.getName());
        nameLbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        nameLbl.setForeground(achieved ? UITheme.SUCCESS : UITheme.TEXT_PRIMARY);
        nameRow.add(nameLbl, BorderLayout.WEST);

        JLabel targetLbl = new JLabel(String.format("Target: \u20B1%.2f", target));
        targetLbl.setFont(UITheme.PESO_FONT);
        targetLbl.setForeground(UITheme.TEXT_SECONDARY);
        nameRow.add(targetLbl, BorderLayout.EAST);

        leftPanel.add(nameRow, BorderLayout.NORTH);

        // Progress bar — fully green with achieved label when claimed
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(achieved ? 100 : pct);
        bar.setStringPainted(true);
        bar.setFont(UITheme.SMALL_FONT);
        bar.setBackground(UITheme.BG);
        bar.setForeground(achieved ? UITheme.SUCCESS : (canAfford ? UITheme.WARNING : UITheme.ACCENT));
        bar.setBorder(new LineBorder(UITheme.BORDER, 1));
        bar.setPreferredSize(new Dimension(0, 22));
        if (achieved) {
            bar.setString("\u2705 Goal Achieved!");
        } else {
            bar.setString(String.format("%d%%  —  \u20B1%.2f saved of \u20B1%.2f",
                    pct, Math.min(saved, target), target));
        }
        leftPanel.add(bar, BorderLayout.CENTER);

        card.add(leftPanel, BorderLayout.CENTER);

        // Right: action buttons
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(UITheme.CARD);

        if (achieved) {
            JLabel achievedLbl = new JLabel("\u2705 Achieved");
            achievedLbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
            achievedLbl.setForeground(UITheme.SUCCESS);
            achievedLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnPanel.add(achievedLbl);
            btnPanel.add(Box.createVerticalStrut(6));
        } else if (canAfford) {
            JButton claimBtn = UITheme.successButton("\u2714 Claim");
            claimBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            claimBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        String.format("Claim \"%s\"? This will deduct \u20B1%.2f from your savings.",
                                goal.getName(), target),
                        "Confirm Claim", JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) store.claimSavingsGoal(index);
            });
            btnPanel.add(claimBtn);
            btnPanel.add(Box.createVerticalStrut(6));
        }

        JButton removeBtn = UITheme.dangerButton("\uD83D\uDDD1 Remove");
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove \"" + goal.getName() + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) store.removeSavingsGoal(index);
        });
        btnPanel.add(removeBtn);

        card.add(btnPanel, BorderLayout.EAST);
        return card;
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    private void applyGoal() {
        try {
            double goal = Double.parseDouble(goalField.getText().trim());
            if (goal <= 0) throw new NumberFormatException();
            store.setSavingsGoal(goal);
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid savings goal.");
        }
    }

    private void deposit() {
        try {
            double amount = Double.parseDouble(depositField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();

            int prevLevel = store.getLevel();
            int xpEarned  = store.calcXPForAmount(amount);
            store.addSavings(amount);
            int newLevel = store.getLevel();

            depositField.setText("");
            showXPToast(xpEarned, newLevel > prevLevel, newLevel);

        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid amount to save.");
        }
    }

    private void withdraw() {
        try {
            double amount = Double.parseDouble(withdrawField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();

            if (amount > store.getCurrentSavings()) {
                UITheme.showError(this, String.format(
                        "You only have \u20B1%.2f saved. Can't withdraw more than that.",
                        store.getCurrentSavings()
                ));
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("Withdraw \u20B1%.2f from savings? This will deduct XP.", amount),
                    "Confirm Withdrawal", JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            int prevLevel = store.getLevel();
            store.withdrawSavings(amount);
            int newLevel = store.getLevel();

            withdrawField.setText("");

            // Show toast — warn if level dropped
            if (newLevel < prevLevel) {
                showWithdrawToast(true, prevLevel, newLevel);
            } else {
                showWithdrawToast(false, prevLevel, newLevel);
            }

        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid amount to withdraw.");
        }
    }

    private void addGoal() {
        String name = goalNameField.getText().trim();
        String targetText = goalTargetField.getText().trim();

        if (name.isEmpty()) {
            UITheme.showError(this, "Enter a name for the goal.");
            return;
        }
        try {
            double target = Double.parseDouble(targetText);
            if (target <= 0) throw new NumberFormatException();
            store.addSavingsGoal(name, target);
            goalNameField.setText("");
            goalTargetField.setText("");
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid target amount.");
        }
    }

    // ── XP toast popup ─────────────────────────────────────────────────────────

    private void showXPToast(int xpEarned, boolean leveledUp, int newLevel) {
        JWindow toast   = new JWindow(SwingUtilities.getWindowAncestor(this));
        JPanel  content = new JPanel(new java.awt.FlowLayout());
        content.setBackground(leveledUp ? UITheme.WARNING : new Color(40, 40, 55));
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(leveledUp ? UITheme.WARNING : UITheme.ACCENT, 2, true),
                new EmptyBorder(10, 18, 10, 18)
        ));

        String msg = leveledUp
                ? "\u2B06 LEVEL UP! Now Level " + newLevel + " \u2014 " + store.getCurrentTitle()
                : "\u26A1 +" + xpEarned + " XP earned!";

        JLabel lbl = new JLabel(msg);
        lbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        lbl.setForeground(leveledUp ? UITheme.BG : UITheme.TEXT_PRIMARY);
        content.add(lbl);

        showToast(toast, content);
    }

    private void showWithdrawToast(boolean leveledDown, int prevLevel, int newLevel) {
        JWindow toast   = new JWindow(SwingUtilities.getWindowAncestor(this));
        JPanel  content = new JPanel(new java.awt.FlowLayout());
        content.setBackground(leveledDown ? UITheme.DANGER : new Color(40, 40, 55));
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(leveledDown ? UITheme.DANGER : UITheme.BORDER, 2, true),
                new EmptyBorder(10, 18, 10, 18)
        ));

        String msg = leveledDown
                ? "\u2B07 LEVEL DOWN! Back to Level " + newLevel + " \u2014 " + store.getCurrentTitle()
                : "\uD83D\uDCE4 Withdrawal recorded. XP deducted.";

        JLabel lbl = new JLabel(msg);
        lbl.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        content.add(lbl);

        showToast(toast, content);
    }

    /** Shared toast display logic. */
    private void showToast(JWindow toast, JPanel content) {
        toast.add(content);
        toast.pack();

        try {
            Point loc = getLocationOnScreen();
            toast.setLocation(loc.x + (getWidth() - toast.getWidth()) / 2, loc.y + 10);
        } catch (Exception ex) {
            toast.setLocationRelativeTo(null);
        }

        toast.setVisible(true);
        Timer timer = new Timer(2500, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        double goal   = store.getSavingsGoal();
        double saved  = store.getCurrentSavings();
        double needed = Math.max(goal - saved, 0);
        int    pct    = goal > 0 ? (int) Math.min((saved / goal) * 100, 100) : 0;

        // Savings progress card
        goalDisplayLabel.setText(String.format("Goal: \u20B1%.2f", goal));
        goalField.setText(String.format("%.2f", goal));
        savingsBar.setValue(pct);
        savingsBar.setString(pct + "% of goal reached");

        if (pct >= 100) {
            motivationLabel.setText("\uD83C\uDF89 Goal reached! You actually did it. Nice.");
            motivationLabel.setForeground(UITheme.SUCCESS);
        } else if (pct >= 50) {
            motivationLabel.setText("\uD83D\uDCAA Halfway there! Keep going.");
            motivationLabel.setForeground(UITheme.ACCENT);
        } else if (pct > 0) {
            motivationLabel.setText("\uD83D\uDE80 Every peso counts. Don't stop.");
            motivationLabel.setForeground(UITheme.TEXT_SECONDARY);
        } else {
            motivationLabel.setText("Set a goal and start saving!");
            motivationLabel.setForeground(UITheme.TEXT_SECONDARY);
        }

        savedLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:10px;'>Saved</span>"
                        + "<br><b style='font-family:Arial;font-size:15px;'>\u20B1%.2f</b></center></html>", saved));

        remainingLabel.setText(String.format(
                "<html><center><span style='font-family:Arial;font-size:10px;'>Still Needed</span>"
                        + "<br><b style='font-family:Arial;font-size:15px;'>\u20B1%.2f</b></center></html>", needed));

        // XP / level badge
        int level     = store.getLevel();
        int currentXP = store.getCurrentLevelXP();
        int neededXP  = store.getXPForNextLevel();
        int xpPct     = neededXP > 0 ? (int)((currentXP / (double) neededXP) * 100) : 100;

        levelLabel.setText("LEVEL " + level);
        titleLabel.setText(store.getCurrentTitle());
        xpBar.setValue(Math.min(xpPct, 100));
        xpBar.setString(currentXP + " / " + neededXP + " XP");
        xpInfoLabel.setText(currentXP + " / " + neededXP + " XP  \u2022  Total: " + store.getTotalXP() + " XP");

        // Level-up flash animation
        if (level != lastLevel) {
            lastLevel = level;
            animateLevelUp();
        }

        // Rebuild goals list
        goalsListPanel.removeAll();
        List<DataStore.SavingsGoal> goals = store.getSavingsGoals();
        if (goals.isEmpty()) {
            JLabel emptyLbl = new JLabel("No savings goals yet. Add one above!");
            emptyLbl.setFont(UITheme.SMALL_FONT);
            emptyLbl.setForeground(UITheme.TEXT_SECONDARY);
            emptyLbl.setBorder(new EmptyBorder(8, 0, 0, 0));
            goalsListPanel.add(emptyLbl);
        } else {
            for (int i = 0; i < goals.size(); i++) {
                goalsListPanel.add(buildGoalCard(goals.get(i), i));
                goalsListPanel.add(Box.createVerticalStrut(8));
            }
        }
        goalsListPanel.revalidate();
        goalsListPanel.repaint();
    }

    // ── Level-up flash animation ───────────────────────────────────────────────

    private void animateLevelUp() {
        final int[] count = {0};
        Timer flashTimer = new Timer(120, null);
        flashTimer.addActionListener(e -> {
            boolean on = count[0] % 2 == 0;
            levelBadge.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(on ? UITheme.WARNING : UITheme.ACCENT, on ? 3 : 1, true),
                    new EmptyBorder(16, 16, 16, 16)
            ));
            levelLabel.setForeground(on ? UITheme.WARNING : Color.WHITE);
            count[0]++;
            if (count[0] >= 8) {
                flashTimer.stop();
                levelBadge.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(UITheme.ACCENT, 1, true),
                        new EmptyBorder(16, 16, 16, 16)
                ));
                levelLabel.setForeground(Color.WHITE);
            }
        });
        flashTimer.start();
    }

    // ── Card / layout helpers ──────────────────────────────────────────────────

    private JPanel buildCard(Color borderColor) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));
        return card;
    }

    private GridBagConstraints cardGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 5, 5, 5);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx   = 0;
        gbc.gridy   = GridBagConstraints.RELATIVE;
        return gbc;
    }

    private JLabel statLabel(Color color, String sub) {
        JLabel lbl = new JLabel(String.format(
                "<html><center><span style='font-family:Arial;font-size:10px;'>%s</span>"
                        + "<br><b style='font-family:Arial;font-size:15px;'>\u20B10.00</b></center></html>",
                sub
        ));
        lbl.setForeground(color);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        lbl.setOpaque(true);
        lbl.setBackground(UITheme.CARD);
        return lbl;
    }
}