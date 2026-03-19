import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class HistoryPanel extends JPanel {
    private DataStore store = DataStore.getInstance();

    private JTable table;
    private DefaultTableModel tableModel;
    private JPanel summaryPanel;
    private JLabel grandTotalLabel;

    public HistoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeaderBar(), BorderLayout.NORTH);
        add(buildSplitPanel(), BorderLayout.CENTER);

        store.addListener(this::refresh);
        refresh();
    }

    private JPanel buildHeaderBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG);

        JLabel title = new JLabel("📊  Expense History & Summary");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(title, BorderLayout.WEST);

        JButton clearBtn = UITheme.dangerButton("🗑  Clear All History");
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Clear ALL expense history? This can't be undone.",
                    "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                List<Expense> expenses = store.getExpenses();
                for (int i = expenses.size() - 1; i >= 0; i--) store.removeExpense(i);
            }
        });

        panel.add(clearBtn, BorderLayout.EAST);
        return panel;
    }

    private JSplitPane buildSplitPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTablePanel(), buildSummaryPanel());
        split.setDividerLocation(520);
        split.setBackground(UITheme.BG);
        split.setBorder(null);
        split.setDividerSize(8);
        return split;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BG);

        String[] cols = {"Date", "Category", "Description", "Amount (₱)"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setFont(UITheme.BODY_FONT);
        table.setBackground(UITheme.CARD);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(UITheme.SYMBOL_BODY_FONT.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(UITheme.ACCENT);
        table.getTableHeader().setForeground(Color.BLACK);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);

        // Right-align the amount column with Arial for peso sign rendering
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(UITheme.PESO_FONT);
                if (!isSelected) {
                    setForeground(UITheme.ACCENT);
                    setBackground(row % 2 == 0 ? UITheme.CARD : new Color(33, 33, 46));
                }
                return this;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.CARD);
        panel.add(scroll, BorderLayout.CENTER);

        grandTotalLabel = new JLabel("Grand Total: ₱0.00");
        grandTotalLabel.setFont(UITheme.SYMBOL_HEADER_FONT);
        grandTotalLabel.setForeground(UITheme.ACCENT);
        grandTotalLabel.setHorizontalAlignment(SwingConstants.RIGHT);        panel.add(grandTotalLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG);

        JLabel title = new JLabel("  Category Breakdown");
        title.setFont(UITheme.HEADER_FONT);
        title.setForeground(UITheme.TEXT_PRIMARY);
        wrapper.add(title, BorderLayout.NORTH);

        summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(UITheme.BG);

        JScrollPane scroll = new JScrollPane(summaryPanel);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(UITheme.BG);
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    private void refresh() {
        // Refresh table
        tableModel.setRowCount(0);
        for (Expense e : store.getExpenses()) {
            tableModel.addRow(new Object[]{
                    e.getFormattedDate(),
                    e.getCategory(),
                    e.getDescription(),
                    String.format("₱%.2f", e.getAmount())
            });
        }
        grandTotalLabel.setText(String.format("Grand Total: ₱%.2f", store.getTotalExpenses()));

        // Refresh category summary        summaryPanel.removeAll();
        summaryPanel.add(Box.createVerticalStrut(8));
        double total = store.getTotalExpenses();

        for (String cat : Expense.CATEGORIES) {
            double catTotal = store.getTotalByCategory(cat);
            if (catTotal == 0) continue;

            int pct = total > 0 ? (int) ((catTotal / total) * 100) : 0;

            JPanel card = new JPanel(new BorderLayout(4, 2));
            card.setBackground(UITheme.CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UITheme.BORDER, 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

            JLabel catLabel = new JLabel(getCategoryIcon(cat) + " " + cat);
            catLabel.setFont(UITheme.BODY_FONT.deriveFont(Font.BOLD));
            catLabel.setForeground(UITheme.TEXT_PRIMARY);

            JLabel amtLabel = new JLabel(String.format("₱%.2f (%d%%)", catTotal, pct));
            amtLabel.setFont(UITheme.PESO_FONT);
            amtLabel.setForeground(UITheme.ACCENT);

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(pct);
            bar.setBackground(UITheme.BG);
            bar.setForeground(getCategoryColor(cat));
            bar.setBorder(null);
            bar.setPreferredSize(new Dimension(0, 6));

            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setBackground(UITheme.CARD);
            topRow.add(catLabel, BorderLayout.WEST);
            topRow.add(amtLabel, BorderLayout.EAST);

            card.add(topRow, BorderLayout.NORTH);
            card.add(bar, BorderLayout.SOUTH);

            summaryPanel.add(card);
            summaryPanel.add(Box.createVerticalStrut(6));
        }

        summaryPanel.revalidate();
        summaryPanel.repaint();
    }

    private String getCategoryIcon(String cat) {
        switch (cat) {
            case "Food": return "🍜";
            case "Transport": return "🚌";
            case "School": return "📚";
            case "Entertainment": return "🎮";
            case "Health": return "💊";
            case "Shopping": return "🛍";
            default: return "📁";
        }
    }

    private Color getCategoryColor(String cat) {
        switch (cat) {
            case "Food": return new Color(255, 149, 0);
            case "Transport": return new Color(0, 122, 255);
            case "School": return new Color(52, 199, 89);
            case "Entertainment": return new Color(175, 82, 222);
            case "Health": return new Color(255, 59, 48);
            case "Shopping": return new Color(255, 204, 0);
            default: return UITheme.ACCENT;
        }
    }
}