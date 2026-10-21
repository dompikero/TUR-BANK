package com.turbank.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.turbank.mobile.BankViewModel
import com.turbank.mobile.data.AccountEntity
import com.turbank.mobile.data.UserSettings
import com.turbank.mobile.domain.Money
import java.util.Locale
import java.util.UUID

private fun qrText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

private data class ParsedPaymentQr(
    val payee: String,
    val accountId: String,
    val currency: String,
    val amountMinor: Long,
    val reference: String
)

private fun parsePaymentQr(contents: String): ParsedPaymentQr? {
    if (!contents.startsWith("TURBANK_PAY_V1|")) return null
    val fields = contents.split("|").drop(1).mapNotNull { part ->
        val splitAt = part.indexOf('=')
        if (splitAt <= 0) null else part.substring(0, splitAt) to part.substring(splitAt + 1)
    }.toMap()
    val payee = fields["PAYEE"]?.trim().orEmpty()
    val accountId = fields["ACCOUNT"]?.trim().orEmpty()
    val currency = fields["CURRENCY"]?.trim().orEmpty()
    val amount = fields["AMOUNT_MINOR"]?.toLongOrNull() ?: return null
    if (payee.isBlank() || amount <= 0 || currency.length != 3) return null
    return ParsedPaymentQr(payee, accountId, currency, amount, fields["REFERENCE"].orEmpty())
}

private fun paymentQrBitmap(contents: String): Bitmap? = runCatching {
    val size = 420
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, size, size, hints)
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) for (y in 0 until size) {
            setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
}.getOrNull()

@Composable
fun QrPaymentScreen(vm: BankViewModel, nav: NavHostController?, settings: UserSettings) {
    val accounts by vm.accounts.collectAsState()
    val working by vm.working.collectAsState()
    val context = LocalContext.current
    val profilePrefs = remember { context.getSharedPreferences("tur_bank_personal_profile", Context.MODE_PRIVATE) }
    val profileName = remember { profilePrefs.getString("profile_name", "").orEmpty() }
    var mode by rememberSaveable { mutableStateOf("pay") }
    var sourceAccountId by rememberSaveable { mutableStateOf("") }
    var requestAccountId by rememberSaveable { mutableStateOf("") }
    var recipient by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var requestAmount by rememberSaveable { mutableStateOf("") }
    var recipientAccountId by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf(UUID.randomUUID().toString().take(8).uppercase()) }
    var scanMessage by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(accounts) {
        if (sourceAccountId.isBlank() && accounts.isNotEmpty()) sourceAccountId = accounts.first().id
        if (requestAccountId.isBlank() && accounts.isNotEmpty()) requestAccountId = accounts.first().id
    }
    val source = accounts.firstOrNull { it.id == sourceAccountId }
    val requestAccount = accounts.firstOrNull { it.id == requestAccountId }
    val paymentMinor = Money.parseMinorUnits(amount)
    val requestMinor = Money.parseMinorUnits(requestAmount)
    val shareTitle = qrText(settings.language, "TUR BANK ödeme isteği", "TUR BANK payment request", "TUR BANK Zahlungsanfrage")
    val scanner = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        val parsed = result.contents?.let(::parsePaymentQr)
        if (parsed == null) {
            scanMessage = qrText(settings.language, "Geçerli bir TUR BANK ödeme QR'ı okunamadı.", "A valid TUR BANK payment QR was not found.", "Kein gültiger TUR BANK Zahlungs-QR gefunden.")
        } else {
            recipient = parsed.payee
            amount = "%.2f".format(Locale.US, parsed.amountMinor / 100.0)
            recipientAccountId = parsed.accountId
            reference = parsed.reference.ifBlank { UUID.randomUUID().toString().take(8).uppercase() }
            scanMessage = qrText(settings.language, "QR bilgileri dolduruldu. Onaylamadan önce alıcı ve tutarı kontrol et.", "QR details loaded. Verify the recipient and amount before confirming.", "QR-Daten geladen. Prüfe Empfänger und Betrag vor der Bestätigung.")
            mode = "pay"
        }
    }
    val requestPayload = if (requestAccount != null && requestMinor != null && requestMinor > 0) {
        val safeName = (profileName.ifBlank { requestAccount.name }).replace("|", " ").replace("=", " ")
        "TURBANK_PAY_V1|PAYEE=" + safeName +
            "|ACCOUNT=" + requestAccount.id +
            "|CURRENCY=" + requestAccount.currencyCode +
            "|AMOUNT_MINOR=" + requestMinor +
            "|REFERENCE=" + reference
    } else ""
    val requestQr = remember(requestPayload) { if (requestPayload.isNotBlank()) paymentQrBitmap(requestPayload) else null }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0B1A35), Color(0xFF1D4ED8), Color(0xFF0F766E)))).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                Text(qrText(settings.language, "QR ile ödeme", "QR payments", "QR-Zahlung"), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(qrText(settings.language, "Kod okut, bilgileri kontrol et ve yerel hareket kaydı oluştur.", "Scan a code, verify its details and record the payment locally.", "Code scannen, Angaben prüfen und die Zahlung lokal erfassen."), color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = mode == "pay", onClick = { mode = "pay" },
                label = { Text(qrText(settings.language, "QR ile öde", "Pay with QR", "Per QR bezahlen")) },
                leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) }
            )
            FilterChip(
                selected = mode == "request", onClick = { mode = "request" },
                label = { Text(qrText(settings.language, "QR talebi oluştur", "Create request QR", "QR-Anfrage erstellen")) },
                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
            )
        }

        if (mode == "pay") {
            SectionHeading(qrText(settings.language, "Ödeme bilgileri", "Payment details", "Zahlungsdetails"))
            ChoiceField(qrText(settings.language, "Gönderen hesabın", "Pay from account", "Konto für Zahlung"), accounts.map { it.id to (it.name + " · " + it.currencyCode) }, sourceAccountId, { sourceAccountId = it })
            OutlinedTextField(value = recipient, onValueChange = { recipient = it.take(100) }, label = { Text(qrText(settings.language, "Alıcı adı", "Recipient", "Empfänger")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(15) },
                label = { Text(qrText(settings.language, "Tutar", "Amount", "Betrag")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            if (scanMessage.isNotBlank()) {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(scanMessage, Modifier.padding(13.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(
                onClick = {
                    val options = ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt(qrText(settings.language, "TUR BANK ödeme QR kodunu okut", "Scan a TUR BANK payment QR", "TUR BANK Zahlungs-QR scannen"))
                        .setBeepEnabled(false)
                        .setOrientationLocked(false)
                    scanner.launch(options)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(qrText(settings.language, "Kamerayla QR okut", "Scan QR with camera", "QR mit Kamera scannen"))
            }
            Button(
                onClick = {
                    if (source != null && paymentMinor != null && paymentMinor > 0 && recipient.isNotBlank()) {
                        val localTarget = accounts.firstOrNull { it.id == recipientAccountId && it.id != source.id }?.id
                        vm.transfer(source.id, localTarget, recipient.trim(),
                            paymentMinor, qrText(settings.language, "QR ödemesi", "QR payment", "QR-Zahlung") + " · " + reference,
                            UUID.randomUUID().toString())
                    }
                },
                enabled = !working && source != null && paymentMinor != null && paymentMinor > 0 && recipient.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)
            ) { Text(qrText(settings.language, "Ödemeyi kaydet", "Record payment", "Zahlung erfassen")) }
        } else {
            SectionHeading(qrText(settings.language, "Ödeme isteğin", "Your payment request", "Deine Zahlungsanfrage"))
            ChoiceField(qrText(settings.language, "Ödeme alacağın hesap", "Receive to account", "Empfangskonto"), accounts.map { it.id to (it.name + " · " + it.currencyCode) }, requestAccountId, { requestAccountId = it })
            OutlinedTextField(
                value = requestAmount, onValueChange = { requestAmount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(15) },
                label = { Text(qrText(settings.language, "İstenen tutar", "Requested amount", "Angeforderter Betrag")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            if (requestQr != null) {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(requestQr.asImageBitmap(), contentDescription = qrText(settings.language, "Ödeme isteği QR kodu", "Payment request QR", "QR-Zahlungsanfrage"), modifier = Modifier.size(232.dp).clip(RoundedCornerShape(18.dp)))
                        Text(profileName.ifBlank { requestAccount?.name.orEmpty() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (requestMinor != null && requestAccount != null) Money.format(requestMinor, requestAccount.currencyCode, settings.language) else "", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Text(qrText(settings.language, "Referans: ", "Reference: ", "Referenz: ") + reference, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(shareTitle, requestPayload))
                        }, modifier = Modifier.weight(1f)
                    ) { Text(qrText(settings.language, "Kodu kopyala", "Copy code", "Code kopieren")) }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, requestPayload)
                            }
                            context.startActivity(Intent.createChooser(intent, shareTitle))
                        }, modifier = Modifier.weight(1f)
                    ) { Text(qrText(settings.language, "Paylaş", "Share", "Teilen")) }
                }
                OutlinedButton(onClick = { reference = UUID.randomUUID().toString().take(8).uppercase() }, modifier = Modifier.fillMaxWidth()) {
                    Text(qrText(settings.language, "Yeni referans üret", "Generate new reference", "Neue Referenz erzeugen"))
                }
            } else {
                EmptyState(qrText(settings.language, "QR oluşturmak için geçerli bir tutar gir.", "Enter a valid amount to create a QR.", "Gib einen gültigen Betrag ein, um einen QR-Code zu erstellen."))
            }
        }
        Text(
            qrText(
                settings.language,
                "Bilgi: Bu sürüm ödeme talebini ve hareket kaydını cihazında hazırlar. Gerçek banka transferi için ödeme sağlayıcısı bağlantısı gerekir.",
                "Info: This version prepares a payment request and local ledger entry. Real bank transfers require a payment provider integration.",
                "Hinweis: Diese Version erstellt eine Zahlungsanfrage und einen lokalen Eintrag. Für echte Banküberweisungen ist eine Zahlungsanbieter-Integration erforderlich."
            ),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
