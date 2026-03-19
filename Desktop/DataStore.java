import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class DataStore {
    private static DataStore instance;

    // ── Budget Period ──────────────────────────────────────────────────────────

    public enum BudgetPeriod {
        WEEKLY("Weekly", 7),
        MONTHLY("Monthly", 30),
        YEARLY("Yearly", 365);

        public final String displayName;
        public final int days;

        BudgetPeriod(String displayName, int days) {
            this.displayName = displayName;
            this.days = days;
        }

        @Override public String toString() { return displayName; }

        public static BudgetPeriod fromString(String s) {
            for (BudgetPeriod p : values()) if (p.displayName.equals(s)) return p;
            return MONTHLY;
        }

        // How many days have elapsed so far in the current period
        public int daysElapsed() {
            LocalDate now = LocalDate.now();
            switch (this) {
                case WEEKLY:
                    // Days since Monday of current week
                    return now.getDayOfWeek().getValue(); // Mon=1 … Sun=7
                case YEARLY:
                    return now.getDayOfYear();
                default: // MONTHLY
                    return now.getDayOfMonth();
            }
        }
    }

    private BudgetPeriod budgetPeriod = BudgetPeriod.MONTHLY;

    public BudgetPeriod getBudgetPeriod() { return budgetPeriod; }

    public void setBudgetPeriod(BudgetPeriod period) {
        this.budgetPeriod = period;
        this.monthlyBudget = 0.0; // reset — user sets a new one for the new period
        notifyListeners();
    }

    // Prorated budget = (budget / total days in period) * days elapsed so far
    public double getProratedBudget() {
        if (monthlyBudget <= 0) return 0;
        int elapsed = Math.max(1, budgetPeriod.daysElapsed());
        return (monthlyBudget / budgetPeriod.days) * elapsed;
    }

    public double getRemainingProrated() {
        return getProratedBudget() - getTotalExpenses();
    }

    private List<Expense> expenses = new ArrayList<>();
    private double monthlyBudget = 5000.0;
    private double savingsGoal = 1000.0;
    private double currentSavings = 0.0;
    private List<Runnable> listeners = new ArrayList<>();

    // XP / Gamification
    private int totalXP = 0;
    private int level = 1;

    // XP needed to level up increases each level: base 100 * level
    public int getXPForNextLevel() { return 100 * level; }
    public int getCurrentLevelXP() { return totalXP - xpAtLevelStart(); }
    public int getTotalXP() { return totalXP; }
    public int getLevel() { return level; }

    private int xpAtLevelStart() {
        // Sum of all XP thresholds before current level
        int sum = 0;
        for (int i = 1; i < level; i++) sum += 100 * i;
        return sum;
    }

    public void awardXP(int xp) {
        totalXP += xp;
        // Check for level up(s)
        while (getCurrentLevelXP() >= getXPForNextLevel()) {
            level++;
        }
        notifyListeners();
    }

    public static final String[] TITLES = {
            "",                  // placeholder for index 0
            "Broke Boy",
            "Budget Apprentice",
            "Penny Pincher",
            "Frugal Warrior",
            "Savings Knight",
            "Money Mage",
            "Budget Royalty",
            "Wealth Sage",
            "Diamond Saver",
            "Financial Legend"
    };

    public String getCurrentTitle() {
        int idx = Math.min(level, TITLES.length - 1);
        return TITLES[idx];
    }

    private DataStore() {}

    public static DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) r.run();
    }

    // Forces all registered panels to re-run their refresh() — used after theme change
    public void forceRefresh() {
        notifyListeners();
    }

    public void addExpense(Expense e) {
        expenses.add(e);
        notifyListeners();
    }

    public void removeExpense(int index) {
        if (index >= 0 && index < expenses.size()) {
            expenses.remove(index);
            notifyListeners();
        }
    }

    public List<Expense> getExpenses() {
        return new ArrayList<>(expenses);
    }

    public double getTotalExpenses() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    public double getTotalByCategory(String category) {
        return expenses.stream()
                .filter(e -> e.getCategory().equals(category))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(double budget) {
        this.monthlyBudget = budget;
        notifyListeners();
    }

    public double getSavingsGoal() { return savingsGoal; }
    public void setSavingsGoal(double goal) {
        this.savingsGoal = goal;
        notifyListeners();
    }

    public double getCurrentSavings() { return currentSavings; }
    public void addSavings(double amount) {
        this.currentSavings += amount;
        int xpEarned = Math.max(5, (int)(amount / 10));
        awardXP(xpEarned);
        notifyListeners();
    }

    public int calcXPForAmount(double amount) {
        return Math.max(5, (int)(amount / 10));
    }

    public double getRemainingBudget() {
        return monthlyBudget - getTotalExpenses();
    }

    // --- Save/Load restoration methods ---
    // These bypass listeners and XP logic — used only by SaveManager on startup

    public void restoreExpenses(List<Expense> saved) {
        this.expenses = new ArrayList<>(saved);
    }

    public void restoreSavings(double savings) {
        this.currentSavings = savings;
    }

    public void restoreXP(int xp, int lvl) {
        this.totalXP = xp;
        this.level = lvl;
    }

    public void restoreBudgetPeriod(BudgetPeriod period) {
        this.budgetPeriod = period;
    }

    // --- Admin reset methods ---

    public void adminResetSavings() {
        this.currentSavings = 0.0;
        this.savingsGoal = 1000.0;
        notifyListeners();
    }

    public void adminResetXP() {
        this.totalXP = 0;
        this.level = 1;
        notifyListeners();
    }

    public void adminResetExpenses() {
        this.expenses.clear();
        notifyListeners();
    }

    public void adminResetBudget() {
        this.monthlyBudget = 5000.0;
        notifyListeners();
    }
}