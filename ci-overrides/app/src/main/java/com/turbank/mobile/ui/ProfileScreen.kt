package com.turbank.mobile.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.turbank.mobile.BankViewModel

private fun profileText(language: String, tr: String, en: String, de: String): String =
    when (language) {
        "tr" -> tr
        "de" -> de
        else -> en
    }

@Composable
fun ProfileScreen(vm: BankViewModel, nav: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tur_bank_personal_profile", Context.MODE_PRIVATE) }
    val settings by vm.settings.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val cards by vm.cards.collectAsState()
    val goals by vm.goals.collectAsState()

    var name by remember { mutableStateOf(prefs.getString("profile_name", "") ?: "") }
    var email by remember { mutableStateOf(prefs.getString("profile_email", "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString("profile_phone", "") ?: "") }
    var selectedAccountId by remember { mutableStateOf(prefs.getString("primary_account_id", "") ?: "") }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
    val language = settings.language
    val title = profileText(language, "Kişisel profil", "Personal profile", "Persönliches Profil")
    val emailInvalid = email.isNotBlank() && !email.contains("@")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Color(0xFF10213F), Color(0xFF356AE6), Color(0xFF16A085))))
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(66.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = name.ifBlank { profileText(language, "Adını ekle", "Add your name", "Namen hinzufügen") },
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = email.ifBlank { profileText(language, "Kişisel alanın", "Your personal space", "Dein persönlicher Bereich") },
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        profileText(language, "TUR BANK • KİŞİSEL PANEL", "TUR BANK • PERSONAL HUB", "TUR BANK • PERSÖNLICHER BEREICH"),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        profileText(
                            language,
                            "Profil bilgileri yalnızca bu cihazda saklanır; gerçek banka girişi veya banka bağlantısı değildir.",
                            "Profile details stay on this device; this is not a real bank login or bank connection.",
                            "Profildaten bleiben auf diesem Gerät; dies ist keine echte Bankanmeldung oder Bankverbindung."
                        ),
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileMetricCard(
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                label = profileText(language, "Hesap", "Accounts", "Konten"),
                value = accounts.size.toString(),
                modifier = Modifier.weight(1f)
            )
            ProfileMetricCard(
                icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                label = profileText(language, "Kart", "Cards", "Karten"),
                value = cards.size.toString(),
                modifier = Modifier.weight(1f)
            )
            ProfileMetricCard(
                icon = { Icon(Icons.Default.Savings, contentDescription = null) },
                label = profileText(language, "Hedef", "Goals", "Ziele"),
                value = goals.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            profileText(language, "Profil bilgileri", "Profile details", "Profildaten"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(profileText(language, "Ad soyad", "Full name", "Vor- und Nachname")) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trimStart() },
            label = { Text(profileText(language, "E-posta (isteğe bağlı)", "Email (optional)", "E-Mail (optional)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailInvalid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (emailInvalid) {
            Text(
                profileText(language, "E-posta adresini kontrol et.", "Check the email address.", "Bitte prüfe die E-Mail-Adresse."),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(profileText(language, "Telefon (isteğe bağlı)", "Phone (optional)", "Telefon (optional)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    profileText(language, "Varsayılan hesabım", "Default account", "Standardkonto"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    profileText(language, "Profilde hızlı erişim için bir hesap seç.", "Choose an account for quick access.", "Wähle ein Konto für den Schnellzugriff."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Box {
                    OutlinedButton(onClick = { accountMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedAccount?.name ?: profileText(language, "Henüz hesap yok", "No account yet", "Noch kein Konto"))
                    }
                    DropdownMenu(expanded = accountMenuExpanded, onDismissRequest = { accountMenuExpanded = false }) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name + " · " + account.currencyCode) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                if (accounts.isEmpty()) {
                    Text(
                        profileText(language, "Önce bir hesap oluştur.", "Create an account first.", "Erstelle zuerst ein Konto."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Button(
            onClick = {
                prefs.edit()
                    .putString("profile_name", name.trim())
                    .putString("profile_email", email.trim())
                    .putString("profile_phone", phone.trim())
                    .putString("primary_account_id", selectedAccount?.id ?: selectedAccountId)
                    .apply()
                Toast.makeText(
                    context,
                    profileText(language, "Profil bu cihaza kaydedildi.", "Profile saved on this device.", "Profil auf diesem Gerät gespeichert."),
                    Toast.LENGTH_SHORT
                ).show()
            },
            enabled = name.isNotBlank() && !emailInvalid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(profileText(language, "Profili kaydet", "Save profile", "Profil speichern"))
        }

        Text(
            profileText(language, "Hızlı erişim", "Quick access", "Schnellzugriff"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.navigate("accounts") }, modifier = Modifier.weight(1f)) {
                Text(profileText(language, "Hesaplar", "Accounts", "Konten"))
            }
            OutlinedButton(onClick = { nav.navigate("cards") }, modifier = Modifier.weight(1f)) {
                Text(profileText(language, "Kartlar", "Cards", "Karten"))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.navigate("goals") }, modifier = Modifier.weight(1f)) {
                Text(profileText(language, "Birikim hedefleri", "Savings goals", "Sparziele"))
            }
            OutlinedButton(onClick = { nav.navigate("insights") }, modifier = Modifier.weight(1f)) {
                Text(profileText(language, "Analizler", "Insights", "Einblicke"))
            }
        }

        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(
                    profileText(language, "Gizlilik ve güvenlik", "Privacy and security", "Datenschutz und Sicherheit"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                ProfileToggleRow(
                    label = profileText(language, "Bakiyeleri göster", "Show balances", "Kontostände anzeigen"),
                    checked = settings.showBalances,
                    onCheckedChange = vm::setShowBalances
                )
                HorizontalDivider()
                ProfileToggleRow(
                    label = profileText(language, "Bildirimleri etkinleştir", "Enable notifications", "Benachrichtigungen aktivieren"),
                    checked = settings.notificationsEnabled,
                    onCheckedChange = vm::setNotifications
                )
                HorizontalDivider()
                ProfileToggleRow(
                    label = profileText(language, "Biyometrik kilit tercihi", "Prefer biometric unlock", "Biometrische Entsperrung bevorzugen"),
                    checked = settings.biometricPreference,
                    onCheckedChange = vm::setBiometrics
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileMetricCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            icon()
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

