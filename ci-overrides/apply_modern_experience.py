#!/usr/bin/env python3
"""Apply the 1.7 modern forms, entry/route motion and offline receipt OCR update."""
from pathlib import Path
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_modern_experience.py PROJECT_DIR")

project = Path(sys.argv[1])
app = project / "app"
src = app / "src/main/java/com/turbank/mobile"
ui = src / "ui"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


# The shared field owns all text entry styling throughout screens and decoded source.
field = ui / "TURBankTextField.kt"
if not field.is_file() or "fun TURBankTextField(" not in field.read_text(encoding="utf-8"):
    raise SystemExit("Shared animated input component is missing.")
field_count = 0
for kotlin in src.rglob("*.kt"):
    if kotlin.name == "TURBankTextField.kt":
        continue
    text = kotlin.read_text(encoding="utf-8")
    updated, n = re.subn(r"\bOutlinedTextField\s*\(", "TURBankTextField(", text)
    if n:
        kotlin.write_text(updated, encoding="utf-8")
        field_count += n
if field_count < 8:
    raise SystemExit(f"Expected to modernize multiple input fields, only replaced {field_count}.")


# Add a real bundled on-device receipt OCR model and on-device image labeling.
gradle_file = app / "build.gradle.kts"
gradle = gradle_file.read_text(encoding="utf-8")
version_pattern = re.compile(r"versionCode\s*=\s*\d+\s*\n\s*versionName\s*=\s*\"[^\"]+\"")
gradle, version_count = version_pattern.subn('versionCode = 8\n        versionName = "1.7.0"', gradle, count=1)
if version_count != 1:
    raise SystemExit(f"Could not safely bump build to 1.7.0 (matches={version_count}).")
deps = [
    '    implementation("com.google.mlkit:text-recognition:16.0.1")',
    '    implementation("com.google.mlkit:image-labeling:17.0.9")',
]
for dep in deps:
    if dep not in gradle:
        anchor = "dependencies {\n"
        gradle = replace_once(gradle, anchor, anchor + dep + "\n", "bundled ML dependency insertion")
gradle_file.write_text(gradle, encoding="utf-8")


# Add the receipt scanner as a real route and a discoverable tools destination.
screens_file = ui / "BankScreens.kt"
screens = screens_file.read_text(encoding="utf-8")
if 'composable("receipt-scan")' not in screens:
    route_anchor = '            composable("payments") { RouteEntrance { PaymentsHubScreen(vm, nav, settings) } }\n'
    screens = replace_once(
        screens, route_anchor,
        route_anchor + '            composable("receipt-scan") { RouteEntrance { ReceiptScannerScreen(settings) } }\n',
        "receipt scanner route"
    )
screens_file.write_text(screens, encoding="utf-8")

tools_file = ui / "PremiumToolsScreen.kt"
tools = tools_file.read_text(encoding="utf-8")
receipt_action = '        BankTool("receipt-scan", toolsText(language, "Akıllı fiş tarayıcı", "Smart receipt scanner", "Intelligenter Belegscanner"), toolsText(language, "Fiş metnini ve toplamı cihazda çıkar", "Read receipt text and total on-device", "Belegtext und Betrag lokal erkennen"), Icons.Default.ReceiptLong),\n'
if 'BankTool("receipt-scan"' not in tools:
    anchor = '        BankTool("finance-hub", toolsText(language, "Finans merkezi", "Finance hub", "Finanz-Cockpit"), toolsText(language, "Bütçe, arama ve dışa aktarım", "Budgets, search and export", "Budgets, Suche und Export"), Icons.Default.AccountBalanceWallet),\n'
    tools = replace_once(tools, anchor, anchor + receipt_action, "receipt scanner tool card")
tools_file.write_text(tools, encoding="utf-8")


# Make launch mark enter with a short spring/fade instead of appearing abruptly.
splash_file = ui / "BrandSplashScreen.kt"
splash = splash_file.read_text(encoding="utf-8")
imports = [
    "import androidx.compose.animation.core.animateFloatAsState",
    "import androidx.compose.animation.core.spring",
    "import androidx.compose.animation.core.tween",
    "import androidx.compose.runtime.LaunchedEffect",
    "import androidx.compose.runtime.getValue",
    "import androidx.compose.runtime.mutableStateOf",
    "import androidx.compose.runtime.remember",
    "import androidx.compose.runtime.setValue",
    "import androidx.compose.ui.graphics.graphicsLayer",
]
for line in imports:
    if line not in splash:
        anchor = "import androidx.compose.runtime.Composable\n"
        splash = replace_once(splash, anchor, anchor + line + "\n", "splash animation import")
if "var entered by remember" not in splash:
    anchor = "@Composable\nfun BrandSplashScreen() {\n"
    splash = replace_once(
        splash, anchor,
        anchor + "    var entered by remember { mutableStateOf(false) }\n"
        "    LaunchedEffect(Unit) { entered = true }\n"
        "    val scale by animateFloatAsState(if (entered) 1f else 0.86f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 220f), label = \"logo-scale\")\n"
        "    val alpha by animateFloatAsState(if (entered) 1f else 0f, animationSpec = tween(420), label = \"logo-alpha\")\n",
        "animated brand splash"
    )
if ".graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }" not in splash:
    splash = replace_once(
        splash,
        "            modifier = Modifier.fillMaxWidth().aspectRatio(1f),\n",
        "            modifier = Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },\n",
        "animated logo graphics"
    )
splash_file.write_text(splash, encoding="utf-8")


# Verify route, bundled models, and form usage; size is checked by CI separately.
receipt_file = ui / "ReceiptScannerScreen.kt"
receipt_source = receipt_file.read_text(encoding="utf-8")
for marker in ["TextRecognition.getClient", "ImageLabeling.getClient", "putString(\"last_text\", extractedText)", "InputImage.fromFilePath"]:
    if marker not in receipt_source:
        raise SystemExit(f"Receipt scanner missing required feature marker: {marker}")
if 'composable("receipt-scan")' not in screens:
    raise SystemExit("Receipt scanner route is not wired.")
if "implementation(\"com.google.mlkit:text-recognition:16.0.1\")" not in gradle or "implementation(\"com.google.mlkit:image-labeling:17.0.9\")" not in gradle:
    raise SystemExit("Bundled on-device ML dependencies are missing.")
print(f"Modernized {field_count} text fields with animated focus styling; added offline receipt OCR/image labeling and animated logo. Target version 1.7.0.")
