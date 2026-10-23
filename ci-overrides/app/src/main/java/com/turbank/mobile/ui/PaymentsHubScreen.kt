package com.turbank.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.turbank.mobile.BankViewModel
import com.turbank.mobile.data.UserSettings
import com.turbank.mobile.domain.Money

private fun paymentsText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

private data class PaymentHubAction(val title: String, val route: String, val icon: ImageVector)

@Composable
fun PaymentsHubScreen(vm: BankViewModel, nav: NavHostController, settings: UserSettings) {
    val recent by vm.recentTransactions.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var query by remember { mutableStateOf("") }

    val favorites = listOf(
        PaymentHubAction(paymentsText(settings.language, "Para ekle", "Add money", "Geld hinzufügen"), "add-funds", Icons.Default.AddCircle),
        PaymentHubAction(paymentsText(settings.language, "Kartlar", "Cards", "Karten"), "cards", Icons.Default.CreditCard),
        PaymentHubAction(paymentsText(settings.language, "QR öde", "QR pay", "QR-Zahlung"), "qr-pay", Icons.Default.QrCode2),
        PaymentHubAction(paymentsText(settings.language, "Transfer", "Transfer", "Überweisen"), "transfer", Icons.Default.Send),
        PaymentHubAction(paymentsText(settings.language, "Faturalar", "Bills", "Rechnungen"), "bills", Icons.Default.ReceiptLong)
    )
    val transferOptions = listOf(
        PaymentHubAction(paymentsText(settings.language, "Karttan karta", "Card to card", "Karte zu Karte"), "transfer", Icons.Default.CreditCard),
        PaymentHubAction(paymentsText(settings.language, "Hesaba", "To account", "Auf Konto"), "transfer", Icons.Default.AccountBalanceWallet),
        PaymentHubAction(paymentsText(settings.language, "QR ile", "With QR", "Per QR"), "qr-pay", Icons.Default.QrCode2)
    )
    val paymentOptions = listOf(
        PaymentHubAction(paymentsText(settings.language, "Ulaşım", "Transport", "Mobilität"), "bills", Icons.Default.GridView),
        PaymentHubAction(paymentsText(settings.language, "İnternet ve TV", "Internet & TV", "Internet & TV"), "bills", Icons.Default.Analytics),
        PaymentHubAction(paymentsText(settings.language, "Telefon", "Phone", "Telefon"), "add-funds", Icons.Default.AccountBalanceWallet),
        PaymentHubAction(paymentsText(settings.language, "Birikim", "Savings", "Sparen"), "goals", Icons.Default.Savings),
        PaymentHubAction(paymentsText(settings.language, "Ekstre", "Statements", "Auszüge"), "activity", Icons.Default.ReceiptLong),
        PaymentHubAction(paymentsText(settings.language, "Diğer", "More", "Mehr"), "tools", Icons.Default.GridView)
    )

    val filteredTransactions = recent.filter { transaction ->
        query.isBlank() || listOf(transaction.recipient, transaction.description, transaction.category, transaction.note, transaction.amountMinor.toString())
            .any { it.contains(query, ignoreCase = true) }
    }
    val matchingAccounts = accounts.filter { account ->
        query.isNotBlank() && listOf(account.name, account.currencyCode, account.id)
            .any { it.contains(query, ignoreCase = true) }
    }
    val recentPeople = recent.map { it.recipient.trim() }.filter { it.isNotBlank() }.distinct().take(6)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(paymentsText(settings.language, "İşlem, ödeme veya alıcı ara", "Search transactions, payments or recipients", "Transaktionen, Zahlungen oder Empfänger suchen")) }
        )

        if (query.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    paymentsText(settings.language, "Hesap eşleşmeleri", "Matching accounts", "Passende Konten"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (matchingAccounts.isEmpty() && filteredTransactions.isEmpty()) {
                    Text(
                        paymentsText(settings.language, "Sonuç bulunamadı. Başka bir kelime dene.", "No results found. Try another search.", "Keine Ergebnisse. Versuche einen anderen Suchbegriff."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                matchingAccounts.take(4).forEach { account ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { nav.navigate("accounts") { launchSingleTop = true } },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(account.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(account.currencyCode, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                Money.format(account.balanceMinor, account.currencyCode, settings.language),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                paymentsText(settings.language, "Favoriler", "Favorites", "Favoriten"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 11.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    favorites.forEach { action ->
                        Column(
                            modifier = Modifier.width(58.dp).clip(RoundedCornerShape(15.dp))
                                .clickable { nav.navigate(action.route) { launchSingleTop = true } }
                                .padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                Modifier.size(43.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(action.icon, contentDescription = action.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                            }
                            Text(action.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                paymentsText(settings.language, "Transfer seçenekleri", "Transfer options", "Überweisungsoptionen"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                transferOptions.forEachIndexed { index, action ->
                    Card(
                        modifier = Modifier.width(156.dp).height(94.dp)
                            .clickable { nav.navigate(action.route) { launchSingleTop = true } },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    when (index) {
                                        0 -> listOf(Color(0xFFF3F6F5), Color(0xFFE3EBE7))
                                        1 -> listOf(Color(0xFFF1F4F3), Color(0xFFE4EAE8))
                                        else -> listOf(Color(0xFFF1F5F2), Color(0xFFE2EEE7))
                                    }
                                )
                            ).padding(14.dp)
                        ) {
                            Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopStart).size(23.dp))
                            Text(action.title, modifier = Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(paymentsText(settings.language, "Son Kişiler", "Recent people", "Letzte Personen"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    paymentsText(settings.language, "Tümünü Gör", "View all", "Alle ansehen"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { nav.navigate("activity") }.padding(5.dp)
                )
            }
            if (recentPeople.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        paymentsText(settings.language, "İlk transferinden sonra kişiler burada görünecek.", "Recipients will appear here after your first transfer.", "Empfänger erscheinen nach deiner ersten Überweisung hier."),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    recentPeople.forEach { person ->
                        Column(Modifier.width(58.dp).clickable { nav.navigate("transfer") { launchSingleTop = true } }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(47.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text(person.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text(person.take(9), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                paymentsText(settings.language, "Ödeme seçenekleri", "Payment options", "Zahlungsoptionen"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            paymentOptions.chunked(3).forEach { rowActions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rowActions.forEach { action ->
                        Card(
                            modifier = Modifier.weight(1f).height(94.dp).clickable { nav.navigate(action.route) { launchSingleTop = true } },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                }
                                Text(action.title, style = MaterialTheme.typography.labelSmall, maxLines = 2, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (rowActions.size < 3) Spacer(Modifier.weight((3 - rowActions.size).toFloat()))
                }
            }
        }

        Text(
            paymentsText(
                settings.language,
                "Hesap hareketleri bu cihazda kaydedilir; gerçek banka veya kart ağı bağlantısı yoktur.",
                "Activity is recorded on this device; no real bank or card network is connected.",
                "Aktivitäten werden lokal gespeichert; es besteht keine Verbindung zu einer Bank oder einem Kartennetz."
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
