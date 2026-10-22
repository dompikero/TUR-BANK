package com.turbank.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.turbank.mobile.BankViewModel
import com.turbank.mobile.data.AccountEntity
import com.turbank.mobile.data.UserSettings
import com.turbank.mobile.domain.Money

private fun fundsText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

@Composable
fun AddFundsScreen(vm: BankViewModel, settings: UserSettings) {
    val accounts by vm.accounts.collectAsState()
    val working by vm.working.collectAsState()
    var accountId by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var chooseAccount by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accountId.isBlank() && accounts.isNotEmpty()) accountId = accounts.first().id
    }
    val account = accounts.firstOrNull { it.id == accountId }
    val amountMinor = Money.parseMinorUnits(amount)
    val canAdd = account != null && amountMinor != null && amountMinor > 0L && !working
    val quickAmounts = listOf("25", "50", "100", "250")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF08152C), Color(0xFF1D4ED8), Color(0xFF0F766E))))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(17.dp))
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(29.dp)) }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            fundsText(settings.language, "TUR BANK · BAKİYE", "TUR BANK · BALANCE", "TUR BANK · KONTOSTAND"),
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            fundsText(settings.language, "Hesaba para ekle", "Add funds", "Geld hinzufügen"),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Text(
                    fundsText(settings.language, "Hesabını seç, tutarı belirle ve bakiyene bir giriş kaydı ekle.", "Choose an account, enter an amount and add a balance entry.", "Wähle ein Konto, gib einen Betrag ein und füge einen Saldoeintrag hinzu."),
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    fundsText(settings.language, "Hedef hesap", "Destination account", "Zielkonto"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { chooseAccount = true },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(17.dp),
                        enabled = accounts.isNotEmpty()
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(account?.name ?: fundsText(settings.language, "Hesap oluştur", "Create an account", "Konto erstellen"))
                            Text(
                                account?.let { it.currencyCode + " · " + Money.format(it.balanceMinor, it.currencyCode, settings.language) }
                                    ?: fundsText(settings.language, "Önce bir hesap ekle", "Create an account first", "Erstelle zuerst ein Konto"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(expanded = chooseAccount, onDismissRequest = { chooseAccount = false }) {
                        accounts.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name + " · " + item.currencyCode) },
                                onClick = { accountId = item.id; chooseAccount = false }
                            )
                        }
                    }
                }

                Text(
                    fundsText(settings.language, "Eklenecek tutar", "Amount to add", "Betrag"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(15) },
                    label = { Text(fundsText(settings.language, "Tutar", "Amount", "Betrag")) },
                    placeholder = { Text("0,00") },
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    quickAmounts.forEach { quick ->
                        FilterChip(
                            selected = amount == quick,
                            onClick = { amount = quick },
                            label = { Text("+" + quick + " " + (account?.currencyCode ?: settings.displayCurrency)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    fundsText(settings.language, "Kayıt ayrıntıları", "Entry details", "Eintragsdetails"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it.take(80) },
                    label = { Text(fundsText(settings.language, "Kaynak / gönderen (isteğe bağlı)", "Source / sender (optional)", "Quelle / Absender (optional)")) },
                    placeholder = { Text(fundsText(settings.language, "Örn. nakit, maaş, dış transfer", "e.g. cash, salary, external transfer", "z. B. Bargeld, Gehalt, externe Überweisung")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(140) },
                    label = { Text(fundsText(settings.language, "Not (isteğe bağlı)", "Note (optional)", "Notiz (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = account != null && amountMinor != null && amountMinor > 0,
            enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 10 }, animationSpec = tween(260))
        ) {
            if (account != null && amountMinor != null && amountMinor > 0) {
            Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(fundsText(settings.language, "Yeni bakiye kaydı", "New balance entry", "Neuer Saldoeintrag"), fontWeight = FontWeight.Bold)
                        Text(Money.format(amountMinor, account.currencyCode, settings.language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            fundsText(settings.language, "Hesap: ", "Account: ", "Konto: ") + account.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            }
        }

        Button(
            onClick = {
                if (account != null && amountMinor != null && amountMinor > 0L) {
                    val sourceLabel = source.trim().ifBlank {
                        fundsText(settings.language, "Manuel giriş", "Manual entry", "Manuelle Eingabe")
                    }
                    val description = note.trim().ifBlank {
                        fundsText(settings.language, "Bakiye ekleme kaydı", "Balance entry", "Saldoeintrag")
                    }
                    vm.addFunds(account.id, amountMinor, sourceLabel, description)
                }
            },
            enabled = canAdd,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null)
            Spacer(Modifier.size(9.dp))
            Text(fundsText(settings.language, "Bakiyeye ekle", "Add to balance", "Zum Saldo hinzufügen"))
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    fundsText(
                        settings.language,
                        "Bu işlem uygulamadaki hesap bakiyesini ve hareket geçmişini günceller. Bankadan para çekmez ve gerçek para transferi başlatmaz.",
                        "This updates the in-app account balance and activity. It does not withdraw funds from a bank or initiate a real transfer.",
                        "Dies aktualisiert den Kontostand und die Aktivitäten in der App. Es zieht kein Geld von einer Bank ein und startet keine echte Überweisung."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
