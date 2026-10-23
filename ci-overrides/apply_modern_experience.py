#!/usr/bin/env python3
"""TUR BANK 1.8: polished inputs, live camera QR scanning and hidden local balance tools."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_modern_experience.py PROJECT_DIR")

project = Path(sys.argv[1])
app = project / "app"
src = app / "src/main/java/com/turbank/mobile"
ui = src / "ui"
screens_file = ui / "BankScreens.kt"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


# Shared animated form fields across onboarding, profile, settings, funds, QR and account flows.
field_file = ui / "TURBankTextField.kt"
if not field_file.is_file() or "fun TURBankTextField(" not in field_file.read_text(encoding="utf-8"):
    raise SystemExit("Shared animated input component is missing.")
field_count = 0
for kotlin in src.rglob("*.kt"):
    if kotlin.name == "TURBankTextField.kt":
        continue
    text = kotlin.read_text(encoding="utf-8")
    updated, count = re.subn(r"\bOutlinedTextField\s*\(", "TURBankTextField(", text)
    if count:
        kotlin.write_text(updated, encoding="utf-8")
        field_count += count
if field_count < 8:
    raise SystemExit(f"Expected to modernize multiple input fields, only replaced {field_count}.")


# Drop the previous oversized receipt OCR/image-labeling dependencies and its screen.
gradle_file = app / "build.gradle.kts"
gradle = gradle_file.read_text(encoding="utf-8")
for dependency in [
    "com.google.mlkit:text-recognition:16.0.1",
    "com.google.mlkit:image-labeling:17.0.9",
]:
    gradle = re.sub(r"^[ \t]*implementation\(\"" + re.escape(dependency) + r"\"\)[ \t]*\n", "", gradle, flags=re.M)
gradle = re.sub(r"^[ \t]*implementation\(\"com.journeyapps:zxing-android-embedded:4.3.0\"\)[ \t]*\n", "", gradle, flags=re.M)
camera_dependencies = [
    '    implementation("androidx.camera:camera-camera2:1.4.2")',
    '    implementation("androidx.camera:camera-core:1.4.2")',
    '    implementation("androidx.camera:camera-lifecycle:1.4.2")',
    '    implementation("androidx.camera:camera-view:1.4.2")',
    '    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")',
    '    implementation("com.google.mlkit:barcode-scanning:17.3.0")',
]
for dependency in camera_dependencies:
    if dependency not in gradle:
        gradle = replace_once(gradle, "dependencies {\n", "dependencies {\n" + dependency + "\n", "CameraX and QR decoder dependency insertion")
version_pattern = re.compile(r"versionCode\s*=\s*\d+\s*\n\s*versionName\s*=\s*\"[^\"]+\"")
gradle, version_count = version_pattern.subn('versionCode = 9\n        versionName = "1.8.0"', gradle, count=1)
if version_count != 1:
    raise SystemExit(f"Could not safely bump build to 1.8.0 (matches={version_count}).")
gradle_file.write_text(gradle, encoding="utf-8")

receipt_screen = ui / "ReceiptScannerScreen.kt"
if receipt_screen.exists():
    receipt_screen.unlink()

# Keep all localized version labels current and add localized strings for the secret settings shortcut.
translations = {
    "values/strings.xml": {
        "secret_tools_title": "Hidden tools",
        "secret_tools_note": "Local-only utilities for managing this device's account ledger. No bank connection is made.",
        "secret_add_funds": "Add funds to account",
    },
    "values-tr/strings.xml": {
        "secret_tools_title": "Gizli araçlar",
        "secret_tools_note": "Bu cihazdaki hesap kayıtlarını yönetmek içindir. Banka bağlantısı kurulmaz.",
        "secret_add_funds": "Hesaba para ekle",
    },
    "values-de/strings.xml": {
        "secret_tools_title": "Versteckte Werkzeuge",
        "secret_tools_note": "Lokale Werkzeuge für das Kontobuch auf diesem Gerät. Es wird keine Bankverbindung hergestellt.",
        "secret_add_funds": "Geld zum Konto hinzufügen",
    },
}
for rel, mapping in translations.items():
    path = app / "src/main/res" / rel
    raw = path.read_text(encoding="utf-8").replace("1.6.0", "1.8.0").replace("1.7.0", "1.8.0")
    root = ET.fromstring(raw)
    names = {item.attrib.get("name") for item in root.findall("string")}
    insertions = []
    for name, value in mapping.items():
        if name not in names:
            safe = value.replace("&", "&amp;")
            insertions.append(f'    <string name="{name}">{safe}</string>')
    if insertions:
        raw = raw.replace("</resources>", "\n".join(insertions) + "\n</resources>", 1)
    ET.parse(path) if not insertions else ET.fromstring(raw)
    path.write_text(raw, encoding="utf-8")

# Add an easter-egg panel at the end of Settings: tap app version five times.
screens = screens_file.read_text(encoding="utf-8")
route_old = 'composable("settings") { RouteEntrance { SettingsScreen(vm, settings) } }'
route_new = 'composable("settings") { RouteEntrance { SettingsScreen(vm, settings, nav) } }'
if route_old in screens:
    screens = replace_once(screens, route_old, route_new, "pass navigation to Settings")
elif route_new not in screens:
    raise SystemExit("Could not locate Settings destination.")

signature_old = "private fun SettingsScreen(vm: BankViewModel, settings: UserSettings) {"
signature_new = "private fun SettingsScreen(vm: BankViewModel, settings: UserSettings, nav: NavHostController) {"
if signature_old in screens:
    screens = replace_once(screens, signature_old, signature_new, "Settings signature")
elif signature_new not in screens:
    raise SystemExit("Could not locate Settings screen declaration.")

settings_start = screens.find(signature_new)
setting_switch = screens.find("\n@Composable\nprivate fun SettingSwitch", settings_start)
if settings_start < 0 or setting_switch < 0:
    raise SystemExit("Could not isolate Settings screen.")

settings_block = screens[settings_start:setting_switch]
if "var showSecretTools by remember" not in settings_block:
    anchor = "    var exportError by remember { mutableStateOf(false) }\n"
    settings_block = replace_once(
        settings_block, anchor,
        anchor + "    var versionTapCount by remember { mutableIntStateOf(0) }\n"
        "    var showSecretTools by remember { mutableStateOf(false) }\n",
        "secret settings state"
    )
version_text = '        Text(stringResource(R.string.app_version), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)'
secret_version_text = '''        Text(
            stringResource(R.string.app_version),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                versionTapCount += 1
                if (versionTapCount >= 5) showSecretTools = true
            }.padding(vertical = 3.dp, horizontal = 5.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        AnimatedVisibility(
            visible = showSecretTools,
            enter = fadeIn(animationSpec = tween(230, easing = FastOutSlowInEasing)) +
                slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { it / 14 }
        ) {
            Card(shape = CardRadius, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.secret_tools_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.secret_tools_note), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { nav.navigate("add-funds") { launchSingleTop = true } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.secret_add_funds))
                    }
                }
            }
        }'''
if version_text in settings_block:
    settings_block = replace_once(settings_block, version_text, secret_version_text, "secret funds panel")
elif "stringResource(R.string.secret_add_funds)" not in settings_block:
    raise SystemExit("Could not locate the Settings version label for the hidden panel.")
screens = screens[:settings_start] + settings_block + screens[setting_switch:]


# Remove the old external scanning activity and make the QR feature use the live CameraX scanner.
qr_file = ui / "QrPaymentScreen.kt"
qr = qr_file.read_text(encoding="utf-8")
qr = qr.replace("import androidx.activity.compose.rememberLauncherForActivityResult\n", "")
qr = qr.replace("import com.journeyapps.barcodescanner.ScanContract\n", "")
qr = qr.replace("import com.journeyapps.barcodescanner.ScanOptions\n", "")
if "import androidx.compose.animation.AnimatedVisibility" not in qr:
    qr = replace_once(qr, "import androidx.compose.foundation.Image\n", "import androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.animation.fadeIn\nimport androidx.compose.animation.slideInVertically\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.Image\n", "QR scanner animation imports")
if "var showCameraScanner by rememberSaveable" not in qr:
    qr = replace_once(qr, '    var scanMessage by rememberSaveable { mutableStateOf("") }\n',
        '    var scanMessage by rememberSaveable { mutableStateOf("") }\n'
        '    var showCameraScanner by rememberSaveable { mutableStateOf(false) }\n',
        "QR camera overlay state")

old_scanner_start = '    val scanner = rememberLauncherForActivityResult(contract = ScanContract()) { result ->'
if old_scanner_start in qr:
    scanner_start = qr.find(old_scanner_start)
    scanner_end = qr.find('    val requestPayload =', scanner_start)
    if scanner_end < 0:
        raise SystemExit("Could not isolate external QR scan contract.")
    qr = qr[:scanner_start] + qr[scanner_end:]
elif "rememberLauncherForActivityResult(contract = ScanContract())" in qr:
    raise SystemExit("Unrecognized external scanner contract.")
button_old = '''            OutlinedButton(
                onClick = {
                    val options = ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt(qrText(settings.language, "TUR BANK ödeme QR kodunu okut", "Scan a TUR BANK payment QR", "TUR BANK Zahlungs-QR scannen"))
                        .setBeepEnabled(false)
                        .setOrientationLocked(false)
                    scanner.launch(options)
                },'''
button_new = '''            OutlinedButton(
                onClick = { showCameraScanner = true },'''
if button_old in qr:
    qr = replace_once(qr, button_old, button_new, "open live camera scanner")
elif 'onClick = { showCameraScanner = true }' not in qr:
    raise SystemExit("Could not locate old camera scan button.")
scanner_ui = '''    if (showCameraScanner) {
        CameraQrScannerScreen(
            language = settings.language,
            onClose = { showCameraScanner = false },
            onCodeRead = { contents ->
                showCameraScanner = false
                val parsed = parsePaymentQr(contents)
                if (parsed != null) {
                    recipient = parsed.payee
                    amount = "%.2f".format(Locale.US, parsed.amountMinor / 100.0)
                    recipientAccountId = parsed.accountId
                    reference = parsed.reference.ifBlank { UUID.randomUUID().toString().take(8).uppercase() }
                    scanMessage = qrText(settings.language, "QR okundu. Alıcıyı ve tutarı kontrol et.", "QR read. Check the recipient and amount.", "QR gelesen. Prüfe Empfänger und Betrag.")
                    mode = "pay"
                } else {
                    val preview = contents.replace("\\n", " ").take(180)
                    scanMessage = qrText(
                        settings.language,
                        "QR kodu okundu ama TUR BANK biçiminde değil. Bilgileri kontrol et: ",
                        "QR read, but it is not a TUR BANK payment code. Review the contents: ",
                        "QR gelesen, aber kein TUR BANK-Zahlungscode. Inhalt prüfen: "
                    ) + preview
                }
            }
        )
        return
    }

'''
qr_anchor = '    Column(\n        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),'
if "if (showCameraScanner)" not in qr:
    qr = replace_once(qr, qr_anchor, scanner_ui + qr_anchor, "camera scanner overlay")
qr_file.write_text(qr, encoding="utf-8")
screens_file.write_text(screens, encoding="utf-8")

# Camera permissions are runtime-requested in the scanner; mark the camera as optional for devices without one.
manifest_file = app / "src/main/AndroidManifest.xml"
manifest = manifest_file.read_text(encoding="utf-8")
if 'android.permission.CAMERA' not in manifest:
    manifest = replace_once(manifest, '<manifest xmlns:android="http://schemas.android.com/apk/res/android">', '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n    <uses-permission android:name="android.permission.CAMERA" />\n    <uses-feature android:name="android.hardware.camera.any" android:required="false" />', "camera permission")
manifest_file.write_text(manifest, encoding="utf-8")

# Validate actual features, and ensure the prior OCR/image-labeling size inflation is gone.
for dependency in ["com.google.mlkit:text-recognition", "com.google.mlkit:image-labeling"]:
    if dependency in gradle:
        raise SystemExit(f"Obsolete oversized OCR dependency remains: {dependency}")
if "com.google.mlkit:barcode-scanning:17.3.0" not in gradle or "androidx.camera:camera-view:1.4.2" not in gradle:
    raise SystemExit("Live QR scanning dependencies are incomplete.")
if "fun CameraQrScannerScreen(" not in (ui / "CameraQrScannerScreen.kt").read_text(encoding="utf-8"):
    raise SystemExit("Live QR scanner source missing.")
if "stringResource(R.string.secret_add_funds)" not in settings_block:
    raise SystemExit("Hidden funds action is missing from Settings.")
if "onClick = { showCameraScanner = true }" not in qr:
    raise SystemExit("QR screen is not wired to the live camera.")
if field_count < 8:
    raise SystemExit("Modern shared input styling was not applied broadly.")
print(f"Applied {field_count} animated form fields, live CameraX QR scanning, flash controls, hidden settings funds action, and version 1.8.0. Removed receipt OCR/image-labeling bloat.")
