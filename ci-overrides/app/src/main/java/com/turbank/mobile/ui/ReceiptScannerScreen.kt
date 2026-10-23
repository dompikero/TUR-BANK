package com.turbank.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.turbank.mobile.data.UserSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun receiptText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

private fun receiptTotalCandidate(text: String): String? {
    val currency = Regex("""(?i)(?:€|EUR|USD|TRY|GBP)\s*\d{1,5}(?:[ .]\d{3})*[,.]\d{2}|\d{1,5}(?:[ .]\d{3})*[,.]\d{2}\s*(?:€|EUR|USD|TRY|GBP)?""")
    return currency.findAll(text).map { it.value.trim() }.lastOrNull()
}

@Composable
fun ReceiptScannerScreen(settings: UserSettings) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tur_bank_receipts", Context.MODE_PRIVATE) }
    var extractedText by rememberSaveable { mutableStateOf(prefs.getString("last_text", "").orEmpty()) }
    var suggestedTotal by rememberSaveable { mutableStateOf(prefs.getString("last_total", "").orEmpty()) }
    var labels by rememberSaveable { mutableStateOf(prefs.getString("last_labels", "").orEmpty()) }
    var savedAt by rememberSaveable { mutableStateOf(prefs.getString("saved_at", "").orEmpty()) }
    var scanning by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanning = true
        message = ""
        val input = runCatching { InputImage.fromFilePath(context, uri) }.getOrElse { error ->
            scanning = false
            message = error.localizedMessage ?: receiptText(settings.language, "Görsel açılamadı.", "Could not open image.", "Bild konnte nicht geöffnet werden.")
            return@rememberLauncherForActivityResult
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                extractedText = result.text.trim()
                suggestedTotal = receiptTotalCandidate(result.text).orEmpty()
                if (extractedText.isBlank()) {
                    message = receiptText(settings.language, "Metin bulunamadı. Daha net bir fotoğraf dene.", "No text found. Try a clearer photo.", "Kein Text gefunden. Bitte ein schärferes Foto versuchen.")
                } else {
                    message = receiptText(settings.language, "Metin okundu. Tutar önerisini kontrol et ve kaydet.", "Text recognized. Check the suggested amount and save.", "Text erkannt. Prüfe den Betrag und speichere.")
                }
            }
            .addOnFailureListener { error ->
                message = error.localizedMessage ?: receiptText(settings.language, "Metin okunamadı.", "Could not recognize text.", "Text konnte nicht erkannt werden.")
            }
            .addOnCompleteListener { recognizer.close(); scanning = false }

        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(input)
            .addOnSuccessListener { result ->
                labels = result.sortedByDescending { it.confidence }.take(3)
                    .joinToString(" · ") { it.text }
            }
            .addOnCompleteListener { labeler.close() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A17))
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0B1114), Color(0xFF17372A), Color(0xFF0F201B))))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF38E29A), modifier = Modifier.size(28.dp))
                }
                Text(receiptText(settings.language, "Akıllı fiş tarayıcı", "Smart receipt scanner", "Intelligenter Belegscanner"), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    receiptText(settings.language, "Fiş fotoğrafından metni ve olası toplam tutarı cihazında çıkar. Görsel bir sunucuya yüklenmez.", "Extract receipt text and a likely total on-device. The image is not uploaded to a server.", "Belegtext und einen möglichen Gesamtbetrag direkt auf dem Gerät erkennen. Das Bild wird nicht hochgeladen."),
                    color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(receiptText(settings.language, "Fiş görseli", "Receipt image", "Belegbild"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(receiptText(settings.language, "JPG veya PNG seç", "Choose a JPG or PNG", "JPG oder PNG auswählen"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !scanning,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(Modifier.size(9.dp))
                    Text(receiptText(settings.language, "Fotoğraf seç ve tara", "Choose photo & scan", "Foto auswählen & scannen"))
                }
                AnimatedContent(targetState = scanning, label = "receipt-scan-state") { busy ->
                    if (busy) {
                        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Text(receiptText(settings.language, "Cihazda analiz ediliyor…", "Analyzing on device…", "Wird lokal analysiert…"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                            Text(receiptText(settings.language, "Çevrimdışı model · Yerel işlem", "Bundled model · Local processing", "Gebündeltes Modell · Lokale Verarbeitung"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }

        AnimatedVisibility(
            visible = extractedText.isNotBlank(),
            enter = fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 12 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(receiptText(settings.language, "Bulunan toplam (öneri)", "Detected total (suggestion)", "Erkannter Gesamtbetrag (Vorschlag)"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            suggestedTotal.ifBlank { receiptText(settings.language, "Tutar seçilemedi", "No likely total found", "Kein Gesamtbetrag erkannt") },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(receiptText(settings.language, "Bu yalnızca metinden çıkarılan bir öneridir; tutarı kendin doğrula.", "This is a text-based suggestion; verify the amount yourself.", "Dies ist nur ein Textvorschlag; bitte überprüfe den Betrag."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
                    }
                }

                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(receiptText(settings.language, "Okunan metin", "Recognized text", "Erkannter Text"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("TUR BANK receipt", extractedText))
                                message = receiptText(settings.language, "Metin kopyalandı.", "Text copied.", "Text kopiert.")
                            }) { Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.size(4.dp)); Text(receiptText(settings.language, "Kopyala", "Copy", "Kopieren")) }
                        }
                        SelectionContainer {
                            Text(extractedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (labels.isNotBlank()) {
                            Text(receiptText(settings.language, "Görsel etiketleri: ", "Image labels: ", "Bildlabels: ") + labels, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                    prefs.edit().putString("last_text", extractedText).putString("last_total", suggestedTotal)
                                        .putString("last_labels", labels).putString("saved_at", stamp).apply()
                                    savedAt = stamp
                                    message = receiptText(settings.language, "Fiş metni bu cihazda saklandı.", "Receipt text saved on this device.", "Belegtext auf diesem Gerät gespeichert.")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) { Icon(Icons.Default.CheckCircle, contentDescription = null); Spacer(Modifier.size(6.dp)); Text(receiptText(settings.language, "Yerel kaydet", "Save locally", "Lokal speichern")) }
                            OutlinedButton(
                                onClick = { extractedText = ""; suggestedTotal = ""; labels = ""; message = "" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(receiptText(settings.language, "Temizle", "Clear", "Leeren")) }
                        }
                        if (savedAt.isNotBlank()) Text(receiptText(settings.language, "Son kayıt: ", "Last saved: ", "Zuletzt gespeichert: ") + savedAt, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Text(
            receiptText(settings.language, "Fiş verisi gerçek bir banka işlemi oluşturmaz; taranan metin ve önerilen tutar yalnızca cihazında saklanır.", "Receipt data does not create a bank transaction; the text and suggested total are stored only on this device.", "Belegdaten erstellen keine Banktransaktion; Text und Betrag werden nur auf diesem Gerät gespeichert."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
