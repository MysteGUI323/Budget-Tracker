import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ExpensePanel — Expenses tab UI.
 *
 * Layout (top to bottom):
 *   - Add Expense form: description | amount | category | Add button
 *   - Recent Expenses list: scrollable, one entry per expense
 *   - Bottom bar: running total label + Remove Selected button
 *
 * The list displays all expenses from DataStore in insertion order.
 * Removal is index-based, confirmed via dialog before executing.
 */
public class ExpensePanel extends JPanel {

    // ── State ──────────────────────────────────────────────────────────────────
    private final DataStore store = DataStore.getInstance();

    // ── Form controls ──────────────────────────────────────────────────────────
    private JTextField       descField;
    private JTextField       amountField;
    private JComboBox<String> categoryBox;

    // ── List display ───────────────────────────────────────────────────────────
    private JList<String>        expenseList;
    private DefaultListModel<String> listModel;

    // ── Summary ────────────────────────────────────────────────────────────────
    private JLabel totalLabel;

    // ── Constructor ────────────────────────────────────────────────────────────

    public ExpensePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildFormPanel(),   BorderLayout.NORTH);
        add(buildListPanel(),   BorderLayout.CENTER);
        add(buildBottomBar(),   BorderLayout.SOUTH);

        store.addListener(this::refresh);
        refresh();
    }

    // ── Add Expense form ───────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.ACCENT, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("\u2795  Add New Expense");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.ACCENT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        panel.add(title, gbc);

        // Row 1 — description + amount
        gbc.gridwidth = 1; gbc.gridy = 1;

        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Description:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        descField = UITheme.textField("e.g. Lunch at canteen");
        panel.add(descField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(UITheme.SymbolLabel("Amount (\u20B1):"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        amountField = UITheme.textField("0.00");
        panel.add(amountField, gbc);

        // Row 2 — category + add button
        gbc.gridy = 2;

        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(UITheme.label("Category:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        categoryBox = new JComboBox<>(Expense.CATEGORIES);
        UITheme.styleCombo(categoryBox);
        panel.add(categoryBox, gbc);

        gbc.gridx = 3; gbc.weightx = 0;
        JButton addBtn = UITheme.accentButton("Add Expense");
        addBtn.addActionListener(e -> addExpense());
        panel.add(addBtn, gbc);

        return panel;
    }

    // ── Expense list ───────────────────────────────────────────────────────────

    private JPanel buildListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BG);

        JLabel lbl = new JLabel("\uD83D\uDCCB  Recent Expenses");
        lbl.setFont(UITheme.HEADER_FONT);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        listModel   = new DefaultListModel<>();
        expenseList = new JList<>(listModel);
        expenseList.setFont(UITheme.PESO_FONT);
        expenseList.setBackground(UITheme.CARD);
        expenseList.setForeground(UITheme.TEXT_PRIMARY);
        expenseList.setSelectionBackground(UITheme.ACCENT);
        expenseList.setSelectionForeground(Color.WHITE);
        expenseList.setFixedCellHeight(36);
        expenseList.setBorder(new EmptyBorder(4, 8, 4, 8));

        JScrollPane scroll = new JScrollPane(expenseList);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.CARD);
        panel.add(scroll, BorderLayout.CENTER);

        JButton removeBtn = UITheme.dangerButton("\uD83D\uDDD1  Remove Selected");
        removeBtn.addActionListener(e -> removeSelected());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setBackground(UITheme.BG);
        btnRow.add(removeBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    // ── Bottom bar ─────────────────────────────────────────────────────────────

    private JPanel buildBottomBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(UITheme.BG);

        totalLabel = new JLabel();
        totalLabel.setFont(UITheme.PESO_FONT_BOLD);
        totalLabel.setForeground(UITheme.ACCENT);
        panel.add(totalLabel);

        return panel;
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    private void addExpense() {
        String desc     = descField.getText().trim();
        String amtText  = amountField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        if (desc.isEmpty()) {
            UITheme.showError(this, "Description can't be empty, genius.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            UITheme.showError(this, "Enter a valid amount greater than 0.");
            return;
        }

        store.addExpense(new Expense(desc, amount, category));
        descField.setText("");
        amountField.setText("");
    }

    private void removeSelected() {
        int index = expenseList.getSelectedIndex();
        if (index == -1) {
            UITheme.showError(this, "Select an expense to remove first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this, "Remove this expense?", "Confirm", JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) store.removeExpense(index);
    }

    // ── Refresh ────────────────────────────────────────────────────────────────

    private void refresh() {
        listModel.clear();
        for (Expense e : store.getExpenses()) {
            listModel.addElement("  " + e.toString());
        }
        totalLabel.setText(String.format("Total Spent: \u20B1%.2f", store.getTotalExpenses()));
    }
}