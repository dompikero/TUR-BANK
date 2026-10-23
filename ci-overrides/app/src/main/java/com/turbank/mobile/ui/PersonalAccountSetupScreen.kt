package com.turbank.mobile.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.turbank.mobile.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private fun setupText(language: String, tr: String, en: String, de: String) = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

@Composable
fun PersonalAccountSetupScreen(
    language: String,
    onCreate: (String, String, String, String, String, String) -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var accountName by rememberSaveable { mutableStateOf("") }
    var accountType by rememberSaveable { mutableStateOf("EVERYDAY") }
    var currency by rememberSaveable { mutableStateOf("EUR") }
    var pin by rememberSaveable { mutableStateOf("") }
    var pinConfirm by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val emailInvalid = email.isNotBlank() && (!email.contains("@") || !email.substringAfter("@").contains("."))
    val pinInvalid = pin.isNotEmpty() && pin.length < 4
    val confirmInvalid = pinConfirm.isNotEmpty() && pin != pinConfirm
    val canCreate = fullName.trim().length >= 2 && accountName.trim().length >= 2 &&
        !emailInvalid && pin.length in 4..6 && pin == pinConfirm

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_tur_bank),
                contentDescription = "TUR BANK",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("TUR BANK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                Text(setupText(language, "Hesabını oluştur", "Set up your account", "Richte dein Konto ein"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Card(
            shape = RoundedCornerShape(27.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101D19))
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0D1714), Color(0xFF18362A), Color(0xFF10231D))))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(setupText(language, "Sana özel bir alan", "Your personal space", "Dein persönlicher Bereich"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    setupText(
                        language,
                        "Dil seçimin tamam. Şimdi profilini, ilk yerel hesabını ve uygulama PIN'ini ayarla.",
                        "Language selected. Create your profile, first account on this device, and a private app PIN.",
                        "Sprache ausgewählt. Erstelle dein Profil, dein erstes lokales Konto und eine persönliche App-PIN."
                    ),
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text(setupText(language, "1 · Profil bilgileri", "1 · Profile", "1 · Profil"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it.take(80) },
            label = { Text(setupText(language, "Ad soyad", "Full name", "Vor- und Nachname")) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it.trimStart().take(120) },
            label = { Text(setupText(language, "E-posta (isteğe bağlı)", "Email (optional)", "E-Mail (optional)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailInvalid, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (emailInvalid) Text(setupText(language, "E-posta biçimini kontrol et.", "Check the email format.", "Bitte prüfe das E-Mail-Format."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

        Text(setupText(language, "2 · İlk hesabın", "2 · First account", "2 · Erstes Konto"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = accountName, onValueChange = { accountName = it.take(60) },
            label = { Text(setupText(language, "Hesap adı (ör. Günlük hesabım)", "Account name (e.g. Everyday)", "Kontoname (z. B. Alltag)")) },
            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = accountType == "EVERYDAY",
                onClick = { accountType = "EVERYDAY" },
                label = { Text(setupText(language, "Günlük", "Everyday", "Alltag")) }
            )
            FilterChip(
                selected = accountType == "SAVINGS",
                onClick = { accountType = "SAVINGS" },
                label = { Text(setupText(language, "Birikim", "Savings", "Sparen")) }
            )
            FilterChip(
                selected = accountType == "SHARED",
                onClick = { accountType = "SHARED" },
                label = { Text(setupText(language, "Ortak", "Shared", "Gemeinsam")) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("EUR", "TRY", "USD", "GBP").forEach { code ->
                FilterChip(selected = currency == code, onClick = { currency = code }, label = { Text(code) })
            }
        }

        Text(setupText(language, "3 · Uygulama güvenliği", "3 · App security", "3 · App-Sicherheit"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(6); submitted = false },
            label = { Text(setupText(language, "4–6 haneli PIN oluştur", "Create a 4–6 digit PIN", "PIN mit 4–6 Ziffern erstellen")) },
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = pinInvalid, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pinConfirm, onValueChange = { pinConfirm = it.filter(Char::isDigit).take(6); submitted = false },
            label = { Text(setupText(language, "PIN'i tekrar yaz", "Confirm PIN", "PIN bestätigen")) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = confirmInvalid, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (confirmInvalid) Text(setupText(language, "PIN'ler eşleşmiyor.", "PINs do not match.", "Die PINs stimmen nicht überein."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                setupText(
                    language,
                    "Bu adım cihazında bir profil ve hesap defteri oluşturur. Gerçek bankada hesap açmaz ve banka hesabına bağlanmaz.",
                    "This creates a profile and account ledger on this device. It does not open or connect to a real bank account.",
                    "Dies erstellt ein Profil und ein Kontobuch auf diesem Gerät. Es eröffnet kein echtes Bankkonto und stellt keine Bankverbindung her."
                ),
                Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = {
                submitted = true
                if (canCreate) onCreate(fullName.trim(), email.trim(), accountName.trim(), accountType, currency, pin)
            },
            enabled = canCreate && !submitted,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(setupText(language, "Hesabımı oluştur", "Create my account", "Mein Konto erstellen"))
            Spacer(Modifier.size(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
        Spacer(Modifier.height(6.dp))
    }
}
