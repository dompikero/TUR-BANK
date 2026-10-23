package com.turbank.mobile.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.turbank.mobile.BankViewModel
import com.turbank.mobile.data.UserSettings
import com.turbank.mobile.domain.Money

private fun referenceHomeText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

@Composable
fun ReferenceHomeScreen(vm: BankViewModel, nav: NavHostController, settings: UserSettings) {
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.recentTransactions.collectAsState()
    val cards by vm.cards.collectAsState()
    val goals by vm.goals.collectAsState()
    val bills by vm.bills.collectAsState()
    val showBalances = settings.showBalances
    val currencyAccounts = accounts.filter { it.currencyCode == settings.displayCurrency }
    val total = currencyAccounts.sumOf { it.balanceMinor }
    val context = LocalContext.current
    val profileName = remember(context) { context.getSharedPreferences("tur_bank_personal_profile", Context.MODE_PRIVATE).getString("profile_name", "").orEmpty() }
    var query by remember { mutableStateOf("") }
    val matchingAccounts = accounts.filter { query.isNotBlank() && listOf(it.name, it.currencyCode, it.id).any { field -> field.contains(query, true) } }
    val matchingTransactions = transactions.filter { query.isBlank() || listOf(it.recipient, it.description, it.category, it.note, it.amountMinor.toString()).any { field -> field.contains(query, true) } }
    val people = transactions.map { it.recipient.trim() }.filter { it.isNotBlank() }.distinct().take(6)
    val incoming = transactions.filter { it.direction == "IN" }.sumOf { it.amountMinor }
    val outgoing = transactions.filter { it.direction != "IN" }.sumOf { it.amountMinor }
    val selectedAccount = if (query.isBlank()) currencyAccounts.firstOrNull() ?: accounts.firstOrNull() else matchingAccounts.firstOrNull()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(profileName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(profileName.trim().ifBlank { "TUR BANK" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(referenceHomeText(settings.language, "Kişisel hesabın", "Personal account", "Persönliches Konto"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {
                Icon(Icons.Default.Notifications, contentDescription = referenceHomeText(settings.language, "Bildirimler", "Notifications", "Benachrichtigungen"))
            }
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(25.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(referenceHomeText(settings.language, "Hesap, ödeme ve işlem ara", "Search accounts, payments and activity", "Konten, Zahlungen und Aktivitäten suchen")) }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(referenceHomeText(settings.language, "Hesaplar", "Accounts", "Konten"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { nav.navigate("accounts") { launchSingleTop = true } }) { Text(referenceHomeText(settings.language, "Tümünü Gör  ›", "View all  ›", "Alle ansehen  ›")) }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(37.dp).clip(CircleShape).background(Color(0xFFE0E7E5)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF178D65)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(selectedAccount?.name ?: if (query.isNotBlank()) referenceHomeText(settings.language, "Eşleşen hesap yok", "No matching account", "Kein passendes Konto") else referenceHomeText(settings.language, "Hesap oluştur", "Create an account", "Konto erstellen"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(selectedAccount?.let { it.currencyCode + " · " + it.id.takeLast(8) } ?: referenceHomeText(settings.language, "Kişisel hesap", "Personal account", "Persönliches Konto"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.setShowBalances(!showBalances) }) { Icon(if (showBalances) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = referenceHomeText(settings.language, "Bakiyeyi gizle/göster", "Hide/show balance", "Saldo ein-/ausblenden"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                val shownBalance = if (query.isBlank()) total else matchingAccounts.filter { it.currencyCode == settings.displayCurrency }.sumOf { it.balanceMinor }
                Text(if (showBalances) Money.format(shownBalance, settings.displayCurrency, settings.language) else "••••••", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(referenceHomeText(settings.language, "Kullanılabilir bakiye", "Available balance", "Verfügbarer Saldo"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.take(2).forEachIndexed { index, card ->
                val greenCard = index == 0
                Card(Modifier.width(137.dp).height(82.dp).clickable { nav.navigate("cards") { launchSingleTop = true } }, shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = if (greenCard) Color(0xFF087B58) else Color(0xFFE1E6E6))) {
                    Column(Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("TUR-BANK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (greenCard) Color.White else Color(0xFF63716E))
                        Text(card.nickname, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (greenCard) Color.White else Color(0xFF263733), maxLines = 1)
                        Text(referenceHomeText(settings.language, "SANAL KART", "VIRTUAL CARD", "VIRTUELLE KARTE"), style = MaterialTheme.typography.labelSmall, color = if (greenCard) Color.White.copy(alpha = 0.8f) else Color(0xFF63716E))
                    }
                }
            }
            Card(Modifier.width(76.dp).height(82.dp).clickable { nav.navigate("cards") { launchSingleTop = true } }, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(referenceHomeText(settings.language, "Kart ekle", "Add card", "Karte hinzufügen"), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { nav.navigate("transfer") { launchSingleTop = true } }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null); Spacer(Modifier.width(7.dp)); Text(referenceHomeText(settings.language, "Transfer", "Transfer", "Überweisen"))
            }
            OutlinedButton(onClick = { nav.navigate("qr-pay") { launchSingleTop = true } }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Default.QrCode2, contentDescription = null); Spacer(Modifier.width(7.dp)); Text(referenceHomeText(settings.language, "QR ile ödeme", "QR payment", "QR-Zahlung"))
            }
        }
        TextButton(onClick = { nav.navigate("accounts") { launchSingleTop = true } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp))
            Text(referenceHomeText(settings.language, "Yeni hesap veya ürün aç", "Open a new account or product", "Neues Konto oder Produkt eröffnen"))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(referenceHomeText(settings.language, "Genel Bakış", "Overview", "Überblick"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(referenceHomeText(settings.language, "Bu dönem", "This period", "Dieser Zeitraum"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(referenceHomeText(settings.language, "Harcamalar", "Spendings", "Ausgaben"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(Money.format(outgoing, settings.displayCurrency, settings.language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) { listOf(Color(0xFF0D9D68), Color(0xFF60BD93), Color(0xFFE9C84A), Color(0xFFE88B45), Color(0xFFD7DEDC)).forEach { shade -> Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(shade)) } }
                    Text(referenceHomeText(settings.language, "Giden hareketler", "Outgoing activity", "Ausgehende Buchungen"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(Modifier.weight(1f), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(referenceHomeText(settings.language, "Gelen", "Incoming", "Eingehend"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(Money.format(incoming, settings.displayCurrency, settings.language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(goals.size.toString() + referenceHomeText(settings.language, " birikim hedefi", " savings goals", " Sparziele"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(bills.count { !it.paid }.toString() + referenceHomeText(settings.language, " bekleyen fatura", " pending bills", " offene Rechnungen"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(referenceHomeText(settings.language, "Hızlı transfer", "Quick transfer", "Schnellüberweisung"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { nav.navigate("transfer") { launchSingleTop = true } }) { Text(referenceHomeText(settings.language, "Tümünü Gör", "View all", "Alle ansehen")) }
        }
        if (people.isEmpty()) {
            Text(referenceHomeText(settings.language, "İlk transferinden sonra kişiler burada görünecek.", "Recipients will appear here after your first transfer.", "Empfänger erscheinen nach deiner ersten Überweisung hier."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                people.forEach { person ->
                    Column(Modifier.width(58.dp).clickable { nav.navigate("transfer") { launchSingleTop = true } }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(person.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        Text(person.take(9), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(referenceHomeText(settings.language, "Son İşlemler", "Recent activity", "Letzte Buchungen"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { nav.navigate("activity") { launchSingleTop = true } }) { Text(referenceHomeText(settings.language, "Tümünü Gör  ›", "View all  ›", "Alle ansehen  ›")) }
        }
        if (matchingTransactions.isEmpty()) {
            Text(if (query.isBlank()) referenceHomeText(settings.language, "Henüz işlem yok.", "No activity yet.", "Noch keine Buchungen.") else referenceHomeText(settings.language, "Sonuç bulunamadı.", "No results found.", "Keine Ergebnisse gefunden."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    matchingTransactions.take(3).forEachIndexed { index, tx ->
                        val currency = accounts.firstOrNull { it.id == tx.accountId }?.currencyCode ?: settings.displayCurrency
                        Row(Modifier.fillMaxWidth().clickable { nav.navigate("activity") { launchSingleTop = true } }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(if (tx.direction == "IN") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (tx.direction == "IN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(tx.recipient.ifBlank { tx.description.ifBlank { tx.category } }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(tx.description.ifBlank { tx.category }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            Text((if (tx.direction == "IN") "+" else "−") + Money.format(tx.amountMinor, currency, settings.language), color = if (tx.direction == "IN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        if (index < matchingTransactions.take(3).lastIndex) Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    }
                }
            }
        }
    }
}
