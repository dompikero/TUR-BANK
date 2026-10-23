package com.turbank.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.turbank.mobile.BankViewModel
import com.turbank.mobile.data.UserSettings
import com.turbank.mobile.domain.Money

private fun historyText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

@Composable
fun TransactionHistoryScreen(vm: BankViewModel, nav: NavHostController, settings: UserSettings) {
    val recent by vm.recentTransactions.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var showSummary by remember { mutableStateOf(true) }
    val outgoing = recent.filter { it.direction != "IN" }.sumOf { it.amountMinor }
    val incoming = recent.filter { it.direction == "IN" }.sumOf { it.amountMinor }
    val total = (outgoing + incoming).coerceAtLeast(1L)
    val visible = recent.filter { tx ->
        val directionMatch = filter == "all" || (filter == "in" && tx.direction == "IN") || (filter == "out" && tx.direction != "IN")
        val queryMatch = query.isBlank() || listOf(tx.recipient, tx.description, tx.category, tx.note, tx.amountMinor.toString()).any { it.contains(query, true) }
        directionMatch && queryMatch
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(25.dp), leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { IconButton(onClick = { showSummary = !showSummary }) { Icon(Icons.Default.Tune, contentDescription = historyText(settings.language, "Özeti aç/kapat", "Toggle summary", "Zusammenfassung umschalten")) } },
            placeholder = { Text(historyText(settings.language, "Hesap, ödeme ve işlem ara", "Search accounts, payments and activity", "Konten, Zahlungen und Aktivitäten suchen")) }
        )
        if (showSummary) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(historyText(settings.language, "Harcama özeti", "Spending summary", "Ausgabenübersicht"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(recent.size.toString() + " " + historyText(settings.language, "işlem", "items", "Buchungen"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(Money.format(outgoing, settings.displayCurrency, settings.language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.weight((outgoing.toDouble() / total.toDouble()).toFloat().coerceAtLeast(0.15f)).fillMaxHeight().clip(CircleShape).background(Color(0xFF0AA76B)))
                        Box(Modifier.weight((incoming.toDouble() / total.toDouble()).toFloat().coerceAtLeast(0.15f)).fillMaxHeight().clip(CircleShape).background(Color(0xFFE8C448)))
                        Box(Modifier.weight(0.22f).fillMaxHeight().clip(CircleShape).background(Color(0xFFE9A05A)))
                        Box(Modifier.weight(0.35f).fillMaxHeight().clip(CircleShape).background(Color(0xFFD8DEDC)))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(historyText(settings.language, "Giden", "Outgoing", "Ausgehend"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(outgoing, settings.displayCurrency, settings.language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(historyText(settings.language, "Gelen", "Incoming", "Eingehend"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(incoming, settings.displayCurrency, settings.language), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text(historyText(settings.language, "Tümü", "All", "Alle")) })
            FilterChip(selected = filter == "in", onClick = { filter = "in" }, label = { Text(historyText(settings.language, "Gelen", "Incoming", "Eingehend")) })
            FilterChip(selected = filter == "out", onClick = { filter = "out" }, label = { Text(historyText(settings.language, "Giden", "Outgoing", "Ausgehend")) })
        }
        if (visible.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(if (query.isNotBlank()) historyText(settings.language, "Sonuç bulunamadı. Başka bir kelime dene.", "No results found. Try another search.", "Keine Ergebnisse. Versuche einen anderen Suchbegriff.") else historyText(settings.language, "Henüz işlem yok.", "No activity yet.", "Noch keine Buchungen."), modifier = Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            visible.forEachIndexed { index, tx ->
                if (index == 0 || index == 3 || index == 6) {
                    Text(when (index) { 0 -> historyText(settings.language, "Bugün", "Today", "Heute"); 3 -> historyText(settings.language, "Dün", "Yesterday", "Gestern"); else -> historyText(settings.language, "Önceki işlemler", "Earlier activity", "Frühere Buchungen") }, modifier = Modifier.padding(top = if (index == 0) 2.dp else 5.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                val currency = accounts.firstOrNull { it.id == tx.accountId }?.currencyCode ?: settings.displayCurrency
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(if (tx.direction == "IN") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (tx.direction == "IN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(tx.recipient.ifBlank { tx.description.ifBlank { tx.category } }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(tx.description.ifBlank { tx.category }.ifBlank { tx.status }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text((if (tx.direction == "IN") "+" else "−") + Money.format(tx.amountMinor, currency, settings.language), color = if (tx.direction == "IN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(tx.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                if (index < visible.lastIndex) Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
            }
        }
    }
}
