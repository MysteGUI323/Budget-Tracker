import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SaveManager — handles persistence for the Student Budget Tracker.
 *
 * Save file : ~/budget_tracker_save.dat
 * Backup    : ~/budget_tracker_save.bak  (rotated before every save)
 *
 * Format — plain key=value text, one entry per line:
 *
 *   BUDGET=5000.0
 *   BUDGET_PERIOD=Weekly
 *   SAVINGS_GOAL=2000.0
 *   CURRENT_SAVINGS=450.0
 *   TOTAL_XP=120
 *   LEVEL=2
 *   THEME=Dark
 *   EXPENSE_COUNT=3
 *   EXPENSE_0=2024-03-01|Food|Lunch at canteen|85.0
 *   EXPENSE_1=2024-03-02|Transport|Jeepney fare|15.0
 *   EXPENSE_2=2024-03-03|School|Printing|50.0
 *
 * Pipe characters inside expense descriptions are escaped as \| on save
 * and unescaped on load.
 */
public class SaveManager {

    // ── File paths ─────────────────────────────────────────────────────────────

    private static final String SAVE_FILE   = System.getProperty("user.home")
            + File.separator + "budget_tracker_save.dat";

    private static final String BACKUP_FILE = System.getProperty("user.home")
            + File.separator + "budget_tracker_save.bak";

    // ── Save ───────────────────────────────────────────────────────────────────

    public static void save() {
        DataStore      store    = DataStore.getInstance();
        List<Expense>  expenses = store.getExpenses();

        rotateBackup(); // move current save → backup before overwriting

        try (BufferedWriter w = new BufferedWriter(new FileWriter(SAVE_FILE))) {
            w.write("BUDGET="          + store.getMonthlyBudget());            w.newLine();
            w.write("BUDGET_PERIOD="   + store.getBudgetPeriod().displayName); w.newLine();
            w.write("SAVINGS_GOAL="    + store.getSavingsGoal());              w.newLine();
            w.write("CURRENT_SAVINGS=" + store.getCurrentSavings());           w.newLine();
            w.write("TOTAL_XP="        + store.getTotalXP());                  w.newLine();
            w.write("LEVEL="           + store.getLevel());                    w.newLine();
            w.write("THEME="           + UITheme.getCurrentTheme().displayName); w.newLine();
            w.write("EXPENSE_COUNT="   + expenses.size());                     w.newLine();

            for (int i = 0; i < expenses.size(); i++) {
                Expense e        = expenses.get(i);
                String  safeDesc = e.getDescription().replace("|", "\\|"); // escape pipes
                w.write("EXPENSE_" + i + "="
                        + e.getDate()     + "|"
                        + e.getCategory() + "|"
                        + safeDesc        + "|"
                        + e.getAmount()
                );
                w.newLine();
            }

        } catch (IOException ex) {
            System.err.println("[SaveManager] Failed to save: " + ex.getMessage());
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    public static void load() {
        File saveFile = new File(SAVE_FILE);
        if (!saveFile.exists()) return; // first launch — nothing to load

        try {
            loadFromFile(saveFile);
        } catch (Exception ex) {
            System.err.println("[SaveManager] Save file corrupted, trying backup...");
            File backup = new File(BACKUP_FILE);
            if (backup.exists()) {
                try {
                    loadFromFile(backup);
                    System.out.println("[SaveManager] Restored from backup.");
                } catch (Exception bex) {
                    System.err.println("[SaveManager] Backup also failed. Starting fresh.");
                }
            }
        }
    }

    private static void loadFromFile(File file) throws IOException {
        DataStore     store         = DataStore.getInstance();
        double        budget        = 5000.0;
        double        savingsGoal   = 1000.0;
        double        currentSavings= 0.0;
        int           totalXP       = 0;
        int           level         = 1;
        List<Expense> expenses      = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int eq = line.indexOf('=');
                if (eq == -1) continue;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                switch (key) {
                    case "BUDGET":          budget         = Double.parseDouble(value);             break;
                    case "BUDGET_PERIOD":   store.restoreBudgetPeriod(DataStore.BudgetPeriod.fromString(value)); break;
                    case "SAVINGS_GOAL":    savingsGoal    = Double.parseDouble(value);             break;
                    case "CURRENT_SAVINGS": currentSavings = Double.parseDouble(value);             break;
                    case "TOTAL_XP":        totalXP        = Integer.parseInt(value);               break;
                    case "LEVEL":           level          = Integer.parseInt(value);               break;
                    case "THEME":           UITheme.applyTheme(UITheme.Theme.fromString(value));    break;
                    case "EXPENSE_COUNT":   /* informational only — list built from EXPENSE_N keys */ break;
                    default:
                        if (key.startsWith("EXPENSE_")) {
                            Expense e = parseExpense(value);
                            if (e != null) expenses.add(e);
                        }
                        break;
                }
            }
        }

        // Restore all state into DataStore
        store.setMonthlyBudget(budget);
        store.setSavingsGoal(savingsGoal);
        store.restoreSavings(currentSavings);
        store.restoreXP(totalXP, level);
        store.restoreExpenses(expenses);
    }

    // ── Expense line parser ────────────────────────────────────────────────────

    /**
     * Parses a single expense line.
     * Format: date|category|description|amount
     * Pipe characters inside description are escaped as \|.
     */
    private static Expense parseExpense(String value) {
        String[] parts = value.split("(?<!\\\\)\\|", 4); // split on unescaped pipes
        if (parts.length != 4) return null;

        try {
            LocalDate date        = LocalDate.parse(parts[0].trim());
            String    category    = parts[1].trim();
            String    description = parts[2].trim().replace("\\|", "|"); // unescape
            double    amount      = Double.parseDouble(parts[3].trim());
            return new Expense(description, amount, category, date);
        } catch (Exception ex) {
            System.err.println("[SaveManager] Skipping bad expense entry: " + value);
            return null;
        }
    }

    // ── Backup rotation ────────────────────────────────────────────────────────

    /** Moves the current save file to .bak before overwriting. */
    private static void rotateBackup() {
        File current = new File(SAVE_FILE);
        File backup  = new File(BACKUP_FILE);
        if (current.exists()) {
            if (backup.exists() && !backup.delete()) {
                System.err.println("[SaveManager] Warning: could not delete old backup file.");
            }
            if (!current.renameTo(backup)) {
                System.err.println("[SaveManager] Warning: could not rotate save to backup — proceeding without backup.");
            }
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    public static String getSaveFilePath() { return SAVE_FILE; }
}