import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * DataStore — singleton model layer for the Budget Tracker.
 *
 * Owns all runtime state:
 *   - Expense list
 *   - Budget amount + period
 *   - Savings goal + current savings
 *   - XP / level / gamification
 *
 * Panels register Runnable listeners via addListener(); every mutating
 * method calls notifyListeners() so the UI stays in sync automatically.
 *
 * Budget math overview:
 *   getProratedBudget()               → how much you should have spent by today
 *   getRemainingProrated()            → prorated budget minus period expenses
 *   getDailyAllowance()               → remaining budget divided by days left
 *   getTotalExpensesForCurrentPeriod()→ expenses filtered to the active period only
 */
public class DataStore {

    // ── Singleton ──────────────────────────────────────────────────────────────

    private static DataStore instance;

    private DataStore() {}

    public static DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    // ── Budget Period enum ─────────────────────────────────────────────────────

    public enum BudgetPeriod {
        WEEKLY ("Weekly",  7),
        MONTHLY("Monthly", 30),
        YEARLY ("Yearly",  365);

        public final String displayName;
        public final int    days;

        BudgetPeriod(String displayName, int days) {
            this.displayName = displayName;
            this.days        = days;
        }

        @Override
        public String toString() { return displayName; }

        public static BudgetPeriod fromString(String s) {
            for (BudgetPeriod p : values())
                if (p.displayName.equals(s)) return p;
            return MONTHLY;
        }

        /**
         * Returns how many days have elapsed so far in the current period.
         * WEEKLY : Mon=1 … Sun=7
         * MONTHLY: day-of-month (1-31)
         * YEARLY : day-of-year  (1-365/366)
         */
        public int daysElapsed() {
            LocalDate now = LocalDate.now();
            switch (this) {
                case WEEKLY:  return now.getDayOfWeek().getValue();
                case YEARLY:  return now.getDayOfYear();
                default:      return now.getDayOfMonth(); // MONTHLY
            }
        }
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private BudgetPeriod   budgetPeriod   = BudgetPeriod.MONTHLY;
    private double         monthlyBudget  = 5000.0;
    private List<Expense>  expenses       = new ArrayList<>();
    private double         savingsGoal    = 1000.0;
    private double         currentSavings = 0.0;
    private final List<Runnable> listeners = new ArrayList<>();

    // XP / gamification
    private int totalXP = 0;
    private int level   = 1;

    // ── Listener system ────────────────────────────────────────────────────────

    public void addListener(Runnable listener) { listeners.add(listener); }

    private void notifyListeners() { listeners.forEach(Runnable::run); }

    /** Forces all panels to re-render — used after a theme change. */
    public void forceRefresh() { notifyListeners(); }

    // ── Budget period ──────────────────────────────────────────────────────────

    public BudgetPeriod getBudgetPeriod() { return budgetPeriod; }

    public void setBudgetPeriod(BudgetPeriod period) {
        this.budgetPeriod  = period;
        this.monthlyBudget = 0.0; // user must set a new amount for the new period
        notifyListeners();
    }

    /**
     * Returns the calendar date on which the current budget period started.
     * Used to filter expenses to the active period only.
     */
    public LocalDate getPeriodStart() {
        LocalDate now = LocalDate.now();
        switch (budgetPeriod) {
            case WEEKLY:  return now.minusDays(now.getDayOfWeek().getValue() - 1); // Monday
            case YEARLY:  return LocalDate.of(now.getYear(), 1, 1);
            default:      return LocalDate.of(now.getYear(), now.getMonth(), 1);   // MONTHLY
        }
    }

    // ── Budget calculations ────────────────────────────────────────────────────

    /**
     * Prorated budget = (total budget / period days) x days elapsed.
     * Represents the maximum you should have spent in total by today.
     */
    public double getProratedBudget() {
        if (monthlyBudget <= 0) return 0;
        int elapsed = Math.max(1, budgetPeriod.daysElapsed());
        return (monthlyBudget / budgetPeriod.days) * elapsed;
    }

    /**
     * How much headroom is left relative to the prorated ceiling.
     * Negative means you have already overspent for this point in the period.
     */
    public double getRemainingProrated() {
        return getProratedBudget() - getTotalExpensesForCurrentPeriod();
    }

    /**
     * Forward-looking daily allowance: remaining budget divided by days left.
     * Returns 0 on the last day of a period (no days left to divide across).
     */
    public double getDailyAllowance() {
        int daysLeft = budgetPeriod.days - budgetPeriod.daysElapsed();
        if (daysLeft <= 0) return 0;
        double remaining = monthlyBudget - getTotalExpensesForCurrentPeriod();
        return Math.max(remaining / daysLeft, 0);
    }

    // ── Expense accessors ──────────────────────────────────────────────────────

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

    /** Returns a defensive copy of the full expense list. */
    public List<Expense> getExpenses() { return new ArrayList<>(expenses); }

    /** All-time total across every logged expense. */
    public double getTotalExpenses() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    /** Period-filtered total — only counts expenses from the current budget period. */
    public double getTotalExpensesForCurrentPeriod() {
        LocalDate periodStart = getPeriodStart();
        return expenses.stream()
                .filter(e -> !e.getDate().isBefore(periodStart))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    /** All-time total for a specific category. */
    public double getTotalByCategory(String category) {
        return expenses.stream()
                .filter(e -> e.getCategory().equals(category))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    // ── Budget setters ─────────────────────────────────────────────────────────

    public double getMonthlyBudget() { return monthlyBudget; }

    public void setMonthlyBudget(double budget) {
        this.monthlyBudget = budget;
        notifyListeners();
    }

    // ── Savings ────────────────────────────────────────────────────────────────

    public double getSavingsGoal() { return savingsGoal; }

    public void setSavingsGoal(double goal) {
        this.savingsGoal = goal;
        notifyListeners();
    }

    public double getCurrentSavings() { return currentSavings; }

    public void addSavings(double amount) {
        this.currentSavings += amount;
        awardXP(calcXPForAmount(amount));
        notifyListeners();
    }

    // ── XP / Gamification ─────────────────────────────────────────────────────

    public int getTotalXP()         { return totalXP; }
    public int getLevel()           { return level; }
    public int getXPForNextLevel()  { return 100 * level; }
    public int getCurrentLevelXP() { return totalXP - xpAtLevelStart(); }

    /** Cumulative XP threshold at the start of the current level. */
    private int xpAtLevelStart() {
        int sum = 0;
        for (int i = 1; i < level; i++) sum += 100 * i;
        return sum;
    }

    public void awardXP(int xp) {
        totalXP += xp;
        while (getCurrentLevelXP() >= getXPForNextLevel()) level++;
        notifyListeners();
    }

    /** XP earned for a savings deposit: 1 XP per P10, minimum 5 XP. */
    public int calcXPForAmount(double amount) {
        return Math.max(5, (int)(amount / 10));
    }

    public static final String[] TITLES = {
            "",                  // index 0 — unused placeholder
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
        return TITLES[Math.min(level, TITLES.length - 1)];
    }

    // ── Save/Load restore methods ──────────────────────────────────────────────
    // Bypass listeners and XP logic — called only by SaveManager on startup.

    public void restoreExpenses(List<Expense> saved)      { this.expenses = new ArrayList<>(saved); }
    public void restoreSavings(double savings)            { this.currentSavings = savings; }
    public void restoreXP(int xp, int lvl)               { this.totalXP = xp; this.level = lvl; }
    public void restoreBudgetPeriod(BudgetPeriod period)  { this.budgetPeriod = period; }

    // ── Admin reset methods ────────────────────────────────────────────────────

    public void adminResetSavings() {
        this.currentSavings = 0.0;
        this.savingsGoal    = 1000.0;
        notifyListeners();
    }

    public void adminResetXP() {
        this.totalXP = 0;
        this.level   = 1;
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