package com.turbank.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.turbank.mobile.data.UserSettings
import com.turbank.mobile.R

private fun toolsText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

private data class BankTool(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun PremiumToolsScreen(settings: UserSettings, nav: NavHostController) {
    val language = settings.language
    var menuVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { menuVisible = true }
    val destinations = listOf(
        BankTool("qr-pay", toolsText(language, "QR ile ödeme", "QR payments", "QR-Zahlung"), toolsText(language, "QR okut veya talep oluştur", "Scan or create a request", "Scannen oder Anfrage erstellen"), Icons.Default.QrCode2),
        BankTool("transfer", toolsText(language, "Para gönder", "Send money", "Geld senden"), toolsText(language, "Alıcıya veya kendi hesabına", "To a recipient or own account", "An Empfänger oder eigenes Konto"), Icons.Default.Send),
        BankTool("cards", toolsText(language, "Kartlar", "Cards", "Karten"), toolsText(language, "Sanal kartlarını yönet", "Manage virtual cards", "Virtuelle Karten verwalten"), Icons.Default.CreditCard),
        BankTool("goals", toolsText(language, "Birikim hedefleri", "Savings goals", "Sparziele"), toolsText(language, "Hedef koy ve ilerlemeni izle", "Set and track targets", "Ziele setzen und Fortschritt sehen"), Icons.Default.Savings),
        BankTool("bills", toolsText(language, "Faturalar", "Bills", "Rechnungen"), toolsText(language, "Vade ve ödeme hatırlatmaları", "Due dates and reminders", "Fälligkeiten und Erinnerungen"), Icons.Default.ReceiptLong),
        BankTool("insights", toolsText(language, "Analizler", "Insights", "Analysen"), toolsText(language, "Harcama ve bütçe görünümü", "Spending and budget overview", "Ausgaben und Budgetüberblick"), Icons.Default.Analytics),
        BankTool("finance-hub", toolsText(language, "Finans merkezi", "Finance hub", "Finanz-Cockpit"), toolsText(language, "Bütçe, arama ve dışa aktarım", "Budgets, search and export", "Budgets, Suche und Export"), Icons.Default.AccountBalanceWallet),
        BankTool("profile", toolsText(language, "Kişisel profil", "Personal profile", "Persönliches Profil"), toolsText(language, "Bilgilerini ve hesabını yönet", "Manage your details and accounts", "Daten und Konten verwalten"), Icons.Default.Person),
        BankTool("notifications", toolsText(language, "Bildirimler", "Notifications", "Benachrichtigungen"), toolsText(language, "Hesap hareketi bildirimleri", "Account activity notifications", "Kontobenachrichtigungen"), Icons.Default.Notifications),
        BankTool("settings", toolsText(language, "Ayarlar", "Settings", "Einstellungen"), toolsText(language, "Görünüm, dil ve gizlilik", "Appearance, language and privacy", "Darstellung, Sprache und Datenschutz"), Icons.Default.Settings)
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                Modifier.background(Brush.linearGradient(listOf(Color(0xFF0B1A35), Color(0xFF1D4ED8), Color(0xFF0F766E))))
                    .fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(toolsText(language, "TUR BANK · KEŞFET", "TUR BANK · EXPLORE", "TUR BANK · ENTDECKEN"), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelLarge)
                Text(toolsText(language, "İhtiyacın olan her şey", "Everything in one place", "Alles an einem Ort"), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                Text(toolsText(language, "Ödemeler, kartlar, birikimler ve finansal araçlar.", "Payments, cards, savings and finance tools.", "Zahlungen, Karten, Sparziele und Finanztools."), color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        destinations.chunked(2).forEachIndexed { rowIndex, rowItems ->
            AnimatedVisibility(
                visible = menuVisible,
                enter = fadeIn(animationSpec = tween(240 + rowIndex * 65)) +
                    slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(280 + rowIndex * 65))
            ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Card(
                        modifier = Modifier.weight(1f).animateContentSize().clickable { nav.navigate(item.route) { launchSingleTop = true } },
                        shape = RoundedCornerShape(23.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                            }
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2)
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            }
        }
        Text(
            toolsText(language,
                "Hesaplar ve ödeme kayıtları bu cihazda saklanır. Gerçek banka bağlantısı kurulmuş değildir.",
                "Accounts and payment records are stored on this device. No bank connection is active.",
                "Konten und Zahlungseinträge werden lokal gespeichert. Es besteht keine Bankverbindung."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
