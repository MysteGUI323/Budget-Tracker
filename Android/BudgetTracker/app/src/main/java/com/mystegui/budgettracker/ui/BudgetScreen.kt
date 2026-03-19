package com.mystegui.budgettracker.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystegui.budgettracker.AppState
import com.mystegui.budgettracker.BudgetPeriod
import com.mystegui.budgettracker.BudgetViewModel
import com.mystegui.budgettracker.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(state: AppState, viewModel: BudgetViewModel) {
    val colors = LocalAppColors.current

    var budgetInput by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(state.budgetPeriod) }
    var periodExpanded by remember { mutableStateOf(false) }

    val pct = state.spentPct
    val barColor = when {
        pct >= 100f -> colors.danger
        pct >= 80f  -> colors.warning
        else        -> colors.success
    }
    val statusText = when {
        pct >= 100f -> "Budget exceeded for this period!"
        pct >= 80f  -> "Getting close to your prorated limit."
        else        -> "On track — ${"%.0f".format(pct)}% of prorated budget used"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Budget Settings", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Period dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Period", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = periodExpanded,
                                onExpandedChange = { periodExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedPeriod.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded)
                                    },
                                    colors = outlinedTextFieldColors(colors),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = periodExpanded,
                                    onDismissRequest = { periodExpanded = false },
                                    modifier = Modifier.background(colors.card)
                                ) {
                                    BudgetPeriod.entries.forEach { period ->
                                        DropdownMenuItem(
                                            text = { Text(period.displayName, color = colors.textPrimary) },
                                            onClick = {
                                                selectedPeriod = period
                                                periodExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Amount field
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amount (₱)", fontSize = 12.sp, color = colors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = budgetInput,
                                onValueChange = { budgetInput = it },
                                placeholder = {
                                    Text("${"%.2f".format(state.budget)}", color = colors.textMuted)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = outlinedTextFieldColors(colors),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Period info
                    val elapsed = selectedPeriod.daysElapsed()
                    val totalDays = selectedPeriod.days
                    Text(
                        "Day $elapsed of $totalDays  •  Daily rate: ₱${"%.2f".format(state.dailyRate)}",
                        fontSize = 12.sp, color = colors.textMuted
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val amt = budgetInput.toDoubleOrNull() ?: return@Button
                            if (amt <= 0) return@Button
                            viewModel.setBudget(amt, selectedPeriod)
                            budgetInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Set Budget", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${state.budgetPeriod.displayName} Budget: ₱${"%.2f".format(state.budget)}",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = barColor,
                        trackColor = colors.border
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(statusText, fontSize = 12.sp, color = barColor)
                    Spacer(Modifier.height(16.dp))

                    // Stat cards
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "Total Spent",
                            value = "₱${"%.2f".format(state.totalSpent)}",
                            valueColor = colors.danger,
                            cardColor = colors.card,
                            borderColor = colors.border,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Prorated Budget",
                            value = "₱${"%.2f".format(state.prorated)}",
                            valueColor = colors.accent,
                            cardColor = colors.card,
                            borderColor = colors.border,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    StatCard(
                        label = "Remaining Today",
                        value = "₱${"%.2f".format(state.remaining)}",
                        valueColor = colors.success,
                        cardColor = colors.card,
                        borderColor = colors.border,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    cardColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = valueColor)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp,
                color = cardColor.copy(alpha = 0f),
                modifier = Modifier.background(Color.Transparent))
            Text(label, fontSize = 11.sp, color = borderColor)
        }
    }
}