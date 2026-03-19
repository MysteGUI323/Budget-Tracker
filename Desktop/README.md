# 💸 Student Budget Tracker

A desktop budgeting app built for students to track expenses, manage budgets, and build saving habits — with a gamified XP and ranking system to keep you motivated.

Built with Java Swing. No internet connection required. All data saved locally on your machine.

---

## 📥 Download

Head to the [Releases](../../releases) page and download the latest `BudgetTracker.exe`.

> **Requirements:** Java 11 or higher must be installed on your machine.  
> Download Java at [adoptium.net](https://adoptium.net) if you don't have it.

---

## ✨ Features

### 💸 Expense Tracking
- Log expenses with a description, amount, and category
- Categories: Food, Transport, School, Entertainment, Health, Shopping, Other
- Remove individual entries from your expense list
- Running total displayed at the bottom

### 💰 Budget Management
- Set a **Weekly**, **Monthly**, or **Yearly** budget
- **Prorated tracking** — compares your spending against where you *should* be at this point in the period, not just the total
- Live progress bar with color-coded warnings (green → yellow → red)
- Three stat cards: Total Spent, Prorated Budget, Remaining Today

### 🎯 Savings Tracker
- Set a savings goal and log deposits toward it
- Progress bar toward your goal with motivational messages

### ⚡ XP & Ranking System
- Earn XP every time you save money (+1 XP per ₱10, minimum +5 XP per deposit)
- Level up through 10 ranks:

| Level | Title |
|-------|-------|
| 1 | Broke Boy |
| 2 | Budget Apprentice |
| 3 | Penny Pincher |
| 4 | Frugal Warrior |
| 5 | Savings Knight |
| 6 | Money Mage |
| 7 | Budget Royalty |
| 8 | Wealth Sage |
| 9 | Diamond Saver |
| 10 | Financial Legend |

- Level-up animation and toast notification on rank up
- XP bar resets and scales harder each level

### 📊 Expense History
- Full table of all logged expenses with date, category, description, and amount
- Category breakdown panel with per-category progress bars
- Clear all history option

### 🎨 Themes
Five built-in color themes, applied instantly and saved between sessions:
- Dark *(default)*
- Light
- Midnight Blue
- Forest Green
- Warm Sunset

### ⚙️ Settings
- Theme switcher
- Password-protected Admin Tools panel (for resetting progress during testing)(Password: Admin_Tools)

### 💾 Persistence
- All data auto-saves on close and on manual save
- Automatic backup file — if the save gets corrupted, it falls back to the last backup
- Save file location: `C:\Users\YourName\budget_tracker_save.dat`

---

## 🖼️ Screenshots

*Coming soon*

---

## 🛠️ Building from Source

If you want to compile and run it yourself:

**Requirements:** JDK 11 or higher

```bash
# Clone the repo
git clone https://github.com/YourUsername/StudentBudgetTracker.git
cd StudentBudgetTracker

# Compile
javac *.java

# Run
java Main

# Package as JAR
jar cfe BudgetTracker.jar Main *.class Savings_Tracker_Icon.png
```

To build the `.exe`, use [Launch4j](http://launch4j.sourceforge.net) pointing to the JAR with `Main` as the entry point.

---

## 📁 Project Structure

```
StudentBudgetTracker/
├── Main.java              # Entry point + splash screen hook
├── BudgetApp.java         # Main JFrame and tab layout
├── UITheme.java           # Colors, fonts, theme system, component factories
├── DataStore.java         # Singleton data layer, XP logic, budget period
├── SaveManager.java       # File persistence and backup rotation
├── SplashScreen.java      # Startup splash screen
├── ExpensePanel.java      # Expenses tab
├── BudgetPanel.java       # Budget tab with prorated tracking
├── SavingsPanel.java      # Savings + XP gamification tab
├── HistoryPanel.java      # Expense history and category breakdown
├── SettingsPanel.java     # Theme switcher + admin tools
├── Expense.java           # Expense data model
└── Savings_Tracker_Icon.png
```

---

## 📝 Notes

- Save data is stored as plain text in your home directory — you can open it in any text editor
- The `.bak` backup file is automatically created every time you save
- Admin password for the Settings panel is intentionally not listed here — check the source if you need it

---

## 👤 Author

Made by **MysteGUI**  
Built as a personal project for student budget management.

---

*Track it. Save it. Don't blow it.*
