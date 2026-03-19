package com.mystegui.budgettracker

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// ── XP / Ranking ──────────────────────────────────────────────────────────────

val TITLES = listOf(
    "", "Broke Boy", "Budget Apprentice", "Penny Pincher",
    "Frugal Warrior", "Savings Knight", "Money Mage",
    "Budget Royalty", "Wealth Sage", "Diamond Saver", "Financial Legend"
)

fun xpAtLevelStart(level: Int): Int = (1 until level).sumOf { it * 100 }

fun computeLevel(totalXP: Int): Int {
    var level = 1
    while (totalXP - xpAtLevelStart(level) >= level * 100) level++
    return level
}

fun xpForNext(level: Int) = level * 100
fun currentLevelXP(totalXP: Int, level: Int) = totalXP - xpAtLevelStart(level)
fun calcXP(amount: Double) = maxOf(5, (amount / 10).toInt())
fun getTitle(level: Int) = TITLES[minOf(level, TITLES.size - 1)]

// ── Budget Period ─────────────────────────────────────────────────────────────

enum class BudgetPeriod(val displayName: String, val days: Int) {
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    YEARLY("Yearly", 365);

    fun daysElapsed(): Int {
        val now = LocalDate.now()
        return when (this) {
            WEEKLY  -> now.dayOfWeek.value
            YEARLY  -> now.dayOfYear
            MONTHLY -> now.dayOfMonth
        }
    }

    companion object {
        fun fromString(s: String) =
            entries.firstOrNull { it.displayName == s } ?: MONTHLY
    }
}

fun proratedBudget(budget: Double, period: BudgetPeriod): Double {
    if (budget <= 0) return 0.0
    val elapsed = maxOf(1, period.daysElapsed())
    return (budget / period.days) * elapsed
}

// ── App State ─────────────────────────────────────────────────────────────────

data class AppState(
    val expenses: List<Expense> = emptyList(),
    val budget: Double = 0.0,
    val budgetPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val savingsGoal: Double = 1000.0,
    val currentSavings: Double = 0.0,
    val totalXP: Int = 0,
    val level: Int = 1,
    val theme: String = "Dark"
) {
    val totalSpent    get() = expenses.sumOf { it.amount }
    val prorated      get() = proratedBudget(budget, budgetPeriod)
    val remaining     get() = maxOf(prorated - totalSpent, 0.0)
    val savingsNeeded get() = maxOf(savingsGoal - currentSavings, 0.0)
    val savingsPct    get() = if (savingsGoal > 0) (currentSavings / savingsGoal * 100).toFloat() else 0f
    val spentPct      get() = if (prorated > 0) (totalSpent / prorated * 100).toFloat() else 0f
    val lvlXP         get() = currentLevelXP(totalXP, level)
    val nextXP        get() = xpForNext(level)
    val xpPct         get() = if (nextXP > 0) (lvlXP.toFloat() / nextXP * 100) else 0f
    val dailyRate     get() = if (budgetPeriod.days > 0) budget / budgetPeriod.days else 0.0
}

// ── SaveManager ───────────────────────────────────────────────────────────────

class SaveManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("budget_tracker", Context.MODE_PRIVATE)

    fun save(state: AppState) {
        val arr = JSONArray()
        state.expenses.forEach { e ->
            arr.put(JSONObject().apply {
                put("id",       e.id)
                put("desc",     e.description)
                put("amount",   e.amount)
                put("category", e.category)
                put("date",     e.date)
            })
        }
        prefs.edit()
            .putString("expenses",       arr.toString())
            .putFloat("budget",          state.budget.toFloat())
            .putString("budgetPeriod",   state.budgetPeriod.displayName)
            .putFloat("savingsGoal",     state.savingsGoal.toFloat())
            .putFloat("currentSavings",  state.currentSavings.toFloat())
            .putInt("totalXP",           state.totalXP)
            .putInt("level",             state.level)
            .putString("theme",          state.theme)
            .apply()
    }

    fun load(): AppState {
        val expensesJson = prefs.getString("expenses", "[]") ?: "[]"
        val arr = JSONArray(expensesJson)
        val expenses = (0 until arr.length()).map { i ->
            arr.getJSONObject(i).let { o ->
                Expense(
                    id          = o.getLong("id"),
                    description = o.getString("desc"),
                    amount      = o.getDouble("amount"),
                    category    = o.getString("category"),
                    date        = o.getString("date")
                )
            }
        }
        return AppState(
            expenses       = expenses,
            budget         = prefs.getFloat("budget", 0f).toDouble(),
            budgetPeriod   = BudgetPeriod.fromString(
                prefs.getString("budgetPeriod", "Monthly") ?: "Monthly"
            ),
            savingsGoal    = prefs.getFloat("savingsGoal", 1000f).toDouble(),
            currentSavings = prefs.getFloat("currentSavings", 0f).toDouble(),
            totalXP        = prefs.getInt("totalXP", 0),
            level          = prefs.getInt("level", 1),
            theme          = prefs.getString("theme", "Dark") ?: "Dark"
        )
    }
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class BudgetViewModel(private val saveManager: SaveManager) : ViewModel() {
    private val _state = MutableStateFlow(saveManager.load())
    val state: StateFlow<AppState> = _state

    private fun update(block: AppState.() -> AppState) {
        _state.value = _state.value.block()
        saveManager.save(_state.value)
    }

    fun addExpense(desc: String, amount: Double, category: String) {
        val expense = Expense(
            description = desc,
            amount      = amount,
            category    = category,
            date        = LocalDate.now().toString()
        )
        update { copy(expenses = expenses + expense) }
    }

    fun removeExpense(id: Long) =
        update { copy(expenses = expenses.filter { it.id != id }) }

    fun setBudget(amount: Double, period: BudgetPeriod) {
        if (period != _state.value.budgetPeriod) {
            update { copy(budget = amount, budgetPeriod = period) }
        } else {
            update { copy(budget = amount) }
        }
    }

    fun setSavingsGoal(goal: Double) =
        update { copy(savingsGoal = goal) }

    fun addSavings(amount: Double): Int {
        val xpEarned   = calcXP(amount)
        val newTotalXP = _state.value.totalXP + xpEarned
        val newLevel   = computeLevel(newTotalXP)
        update { copy(currentSavings = currentSavings + amount, totalXP = newTotalXP, level = newLevel) }
        return xpEarned
    }

    fun setTheme(theme: String) =
        update { copy(theme = theme) }

    fun adminResetSavings() =
        update { copy(currentSavings = 0.0, savingsGoal = 1000.0) }

    fun adminResetXP() =
        update { copy(totalXP = 0, level = 1) }

    fun adminResetExpenses() =
        update { copy(expenses = emptyList()) }

    fun adminResetBudget() =
        update { copy(budget = 0.0, budgetPeriod = BudgetPeriod.MONTHLY) }

    fun adminFullReset() =
        update {
            copy(
                expenses       = emptyList(),
                budget         = 0.0,
                budgetPeriod   = BudgetPeriod.MONTHLY,
                savingsGoal    = 1000.0,
                currentSavings = 0.0,
                totalXP        = 0,
                level          = 1
            )
        }
}

// ── ViewModel Factory ─────────────────────────────────────────────────────────

class BudgetViewModelFactory(private val saveManager: SaveManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BudgetViewModel(saveManager) as T
    }
}