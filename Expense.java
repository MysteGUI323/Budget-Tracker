import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Expense {
    private String description;
    private double amount;
    private String category;
    private LocalDate date;

    public static final String[] CATEGORIES = {
            "Food", "Transport", "School", "Entertainment", "Health", "Shopping", "Other"
    };

    public Expense(String description, double amount, String category) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = LocalDate.now();
    }

    // Used when loading from save file
    public Expense(String description, double amount, String category, LocalDate date) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public LocalDate getDate() { return date; }

    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: ₱%.2f", getFormattedDate(), category, description, amount);
    }
}