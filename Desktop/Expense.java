import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Expense — immutable value object representing a single spending entry.
 *
 * Fields:
 *   description  — free-text label entered by the user
 *   amount       — peso value (must be > 0)
 *   category     — one of the CATEGORIES constants
 *   date         — date the expense was created (defaults to today)
 *
 * The four-arg constructor is used exclusively by SaveManager when
 * restoring saved expenses with their original dates.
 */
public class Expense {

    // ── Category list (shared with UI dropdowns) ───────────────────────────────
    public static final String[] CATEGORIES = {
            "Food", "Transport", "School", "Entertainment", "Health", "Shopping", "Other"
    };

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final String    description;
    private final double    amount;
    private final String    category;
    private final LocalDate date;

    // ── Constructors ───────────────────────────────────────────────────────────

    /** Normal constructor — date defaults to today. */
    public Expense(String description, double amount, String category) {
        this(description, amount, category, LocalDate.now());
    }

    /** Restore constructor — used by SaveManager to preserve original date. */
    public Expense(String description, double amount, String category, LocalDate date) {
        this.description = description;
        this.amount      = amount;
        this.category    = category;
        this.date        = date;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String    getDescription()  { return description; }
    public double    getAmount()       { return amount; }
    public String    getCategory()     { return category; }
    public LocalDate getDate()         { return date; }
    public String    getFormattedDate(){ return date.format(DATE_FMT); }

    // ── Display ────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: \u20B1%.2f",
                getFormattedDate(), category, description, amount);
    }
}