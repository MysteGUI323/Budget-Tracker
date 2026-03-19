# 💸 Student Budget Tracker

A budgeting app built for students to track expenses, manage budgets, and build saving habits — with a gamified XP and ranking system to keep you motivated.

Available on **Windows desktop** and **Android**. No internet connection required. All data saved locally on your device.

---

## 📥 Download

Head to the [Releases](../../releases) page and download the latest version for your platform.

| Platform | File | Requirements |
|---|---|---|
| Windows | `BudgetTracker.exe` | Java 11 or higher |
| Android | `BudgetTracker.apk` | Android 8.0 or higher |

> **Windows:** Download Java at [adoptium.net](https://adoptium.net) if you don't have it.  
> **Android:** Enable **Install from unknown sources** in Settings → Security before installing the APK.

---

## ✨ Features

### 💸 Expense Tracking
- Log expenses with a description, amount, and category
- Categories: Food, Transport, School, Entertainment, Health, Shopping, Other
- Remove individual entries from your expense list
- Running total displayed at all times

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
- Password-protected Admin Tools panel (for resetting progress during testing)

### 💾 Persistence & Backup
- All data saves automatically
- Desktop: auto-saves on close with a manual save button and automatic `.bak` backup file
- Android: saves instantly to device storage on every change

### 🔔 Daily Reminder *(Android only)*
- Optional daily notification to remind you to log your expenses
- Customizable reminder time with a built-in time picker
- Defaults to 8:00 PM — persists through phone restarts

---

## 🖼️ Screenshots

<img width="1268" height="825" alt="Screenshot 2026-03-18 200012" src="https://github.com/user-attachments/assets/44303730-1d46-44bb-b82d-f90675b24f72" />
<img width="1272" height="830" alt="Screenshot 2026-03-18 200025" src="https://github.com/user-attachments/assets/8def6f48-b083-43a0-ab09-45b0d2ef2d5b" />
<img width="1373" height="828" alt="Screenshot 2026-03-18 200039" src="https://github.com/user-attachments/assets/d85703fc-5bd1-4da4-a4fb-32d4521dde71" />
<img width="1277" height="836" alt="Screenshot 2026-03-18 195950" src="https://github.com/user-attachments/assets/ddd56a03-07d8-49ea-b034-66dabd5f0408" />

---

## 🛠️ Building from Source

### Desktop (Windows)

**Requirements:** JDK 11 or higher

```bash
# Clone the repo
git clone https://github.com/YourUsername/StudentBudgetTracker.git
cd StudentBudgetTracker/desktop

# Compile
javac *.java

# Run
java Main

# Package as JAR
jar cfe BudgetTracker.jar Main *.class Savings_Tracker_Icon.png
```

To build the `.exe`, use [Launch4j](http://launch4j.sourceforge.net) pointing to the JAR with `Main` as the entry point.

### Android

**Requirements:** Android Studio, JDK 11 or higher

```bash
# Clone the repo
git clone https://github.com/YourUsername/StudentBudgetTracker.git
cd StudentBudgetTracker/android

# Open in Android Studio and run, or build APK via:
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

---

## 📁 Project Structure

```
StudentBudgetTracker/
├── desktop/                        # Windows Java Swing app
│   ├── Main.java                   # Entry point + splash screen hook
│   ├── BudgetApp.java              # Main JFrame and tab layout
│   ├── UITheme.java                # Colors, fonts, theme system
│   ├── DataStore.java              # Singleton data layer, XP logic
│   ├── SaveManager.java            # File persistence and backup
│   ├── SplashScreen.java           # Startup splash screen
│   ├── ExpensePanel.java           # Expenses tab
│   ├── BudgetPanel.java            # Budget tab with prorated tracking
│   ├── SavingsPanel.java           # Savings + XP gamification tab
│   ├── HistoryPanel.java           # Expense history and breakdown
│   ├── SettingsPanel.java          # Theme switcher + admin tools
│   ├── Expense.java                # Expense data model
│   └── Savings_Tracker_Icon.png
│
├── android/                        # Android Jetpack Compose app
│   ├── app/src/main/java/com/mystegui/budgettracker/
│   │   ├── MainActivity.kt         # Entry point + navigation
│   │   ├── AppData.kt              # State, ViewModel, SaveManager, XP logic
│   │   ├── Expense.kt              # Expense data model
│   │   ├── ReminderManager.kt      # Daily notification scheduler
│   │   ├── ReminderReceiver.kt     # Broadcast receiver for notifications
│   │   └── ui/
│   │       ├── ExpenseScreen.kt    # Expenses tab
│   │       ├── BudgetScreen.kt     # Budget tab
│   │       ├── SavingsScreen.kt    # Savings + XP tab
│   │       ├── HistoryScreen.kt    # Expense history tab
│   │       ├── SettingsScreen.kt   # Theme + admin + reminder settings
│   │       └── theme/
│   │           └── Theme.kt        # Color themes
│
└── README.md
```

---

## 📝 Notes

### Desktop
- Save data is stored as plain text in your home directory — you can open it in any text editor
- The `.bak` backup file is automatically created every time you save
- Save file location: `C:\Users\YourName\budget_tracker_save.dat`

### Android
- Data is stored locally on your device — uninstalling the app will erase your data
- The APK is not from the Google Play Store so you'll need to allow unknown sources to install it
- For the daily reminder to persist after a phone restart, the app needs to have been opened at least once after boot

---

## 👤 Author

Made by **MysteGUI**  
Built as a personal project for student budget management.

---

*Track it. Save it. Don't blow it.*
