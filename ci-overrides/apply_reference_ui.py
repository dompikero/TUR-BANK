#!/usr/bin/env python3
"""Apply the TUR BANK light/emerald reference UI to all main navigation surfaces."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_reference_ui.py PROJECT_DIR")

project = Path(sys.argv[1])
app = project / "app"
src = app / "src/main/java/com/turbank/mobile"
ui = src / "ui"
screens_file = ui / "BankScreens.kt"
screens = screens_file.read_text(encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


# Use white surfaces, soft grey cards and emerald actions, matching the supplied reference.
if "import androidx.compose.material3.lightColorScheme" not in screens and "import androidx.compose.material3.*" not in screens:
    anchor = "import androidx.compose.material3.MaterialTheme\n"
    screens = replace_once(screens, anchor, anchor + "import androidx.compose.material3.lightColorScheme\n", "dark palette import")
if "import androidx.compose.material.icons.filled.Search" not in screens:
    anchor = "import androidx.compose.material.icons.filled.Send\n"
    if anchor not in screens:
        anchor = next((line + "\n" for line in screens.splitlines() if line.startswith("import androidx.compose.material.icons.filled.")), "")
        if not anchor:
            raise SystemExit("Could not identify icon imports for home search.")
    screens = replace_once(screens, anchor, anchor + "import androidx.compose.material.icons.filled.Search\n", "home search icon import")

# Install the palette around the Scaffold, so inputs, menus, bottom navigation and dialogs
# all inherit the light surfaces instead of only recoloring the hero cards.
scaffold_anchor = "    Scaffold(\n        topBar = {"
scaffold_replacement = """    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF079B63),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE0F4EA),
            onPrimaryContainer = Color(0xFF14382C),
            secondary = Color(0xFF168E68),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFF5F7F7),
            onBackground = Color(0xFF192725),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D2A28),
            surfaceVariant = Color(0xFFEEF2F2),
            onSurfaceVariant = Color(0xFF6D7A79),
            outline = Color(0xFFD5DEDC),
            outlineVariant = Color(0xFFE2E9E7),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF)
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
    Scaffold(
        topBar = {"""
screens = replace_once(screens, scaffold_anchor, scaffold_replacement, "global light palette scaffold")
close_anchor = "    }\n}\n\n@Composable\nprivate fun RouteEntrance"
close_replacement = "    }\n    }\n}\n\n@Composable\nprivate fun RouteEntrance"
screens = replace_once(screens, close_anchor, close_replacement, "close light palette wrapper")

# The reference uses four/five stable destinations with payments as a first-class section.
screens = replace_once(
    screens,
    '        "tools" -> stringResource(R.string.tools)\n        "receive", "qr-pay" -> stringResource(R.string.receive_demo)\n',
    '        "tools" -> stringResource(R.string.tools)\n'
    '        "payments" -> stringResource(R.string.payments_home)\n'
    '        "receive", "qr-pay" -> stringResource(R.string.receive_demo)\n',
    "payments title"
)
screens = replace_once(
    screens,
    '    val bottomItems = listOf(\n'
    '        BottomItem("home", R.string.home, Icons.Default.Home),\n'
    '        BottomItem("accounts", R.string.accounts, Icons.Default.AccountBalanceWallet),\n'
    '        BottomItem("qr-pay", R.string.receive_demo, Icons.Default.QrCode2),\n'
    '        BottomItem("activity", R.string.transactions, Icons.Default.ReceiptLong),\n'
    '        BottomItem("tools", R.string.explore, Icons.Default.GridView)\n'
    '    )',
    '    val bottomItems = listOf(\n'
    '        BottomItem("home", R.string.home, Icons.Default.Home),\n'
    '        BottomItem("transfer", R.string.transfer, Icons.Default.Send),\n'
    '        BottomItem("payments", R.string.payments_home, Icons.Default.QrCode2),\n'
    '        BottomItem("cards", R.string.cards, Icons.Default.CreditCard),\n'
    '        BottomItem("tools", R.string.explore, Icons.Default.GridView)\n'
    '    )',
    "reference bottom navigation"
)
route_anchor = '            composable("tools") { RouteEntrance { PremiumToolsScreen(settings, nav) } }\n'
screens = replace_once(
    screens,
    route_anchor,
    route_anchor + '            composable("payments") { RouteEntrance { PaymentsHubScreen(vm, nav, settings) } }\n',
    "payments hub route"
)

# Home search field sits between the personal header and balance card and filters local activity.
screens = replace_once(
    screens,
    '    var dashboardReady by remember { mutableStateOf(false) }\n',
    '    var dashboardReady by remember { mutableStateOf(false) }\n'
    '    var dashboardSearch by remember { mutableStateOf("") }\n',
    "home search state"
)
screens = replace_once(
    screens,
    '        DashboardEntrance(index = 1, visible = dashboardReady) {\n'
    '            DashboardBalanceCard(\n',
    '        DashboardEntrance(index = 1, visible = dashboardReady) {\n'
    '            OutlinedTextField(\n'
    '                value = dashboardSearch,\n'
    '                onValueChange = { dashboardSearch = it },\n'
    '                modifier = Modifier.fillMaxWidth(),\n'
    '                singleLine = true,\n'
    '                shape = RoundedCornerShape(19.dp),\n'
    '                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },\n'
    '                placeholder = { Text(dashboardText(settings.language, "Hesap, ödeme ve işlem ara", "Search accounts, payments and activity", "Konten, Zahlungen und Aktivitäten suchen")) }\n'
    '            )\n'
    '        }\n'
    '        DashboardEntrance(index = 2, visible = dashboardReady) {\n'
    '            DashboardBalanceCard(\n',
    "home search field"
)
screens = replace_once(
    screens,
    '        recent.take(4).forEachIndexed { index, transaction ->',
    '        recent.filter { transaction -> dashboardSearch.isBlank() || listOf(transaction.recipient, transaction.description, transaction.category, transaction.note).any { it.contains(dashboardSearch, true) } }.take(4).forEachIndexed { index, transaction ->',
    "searchable home activity"
)

# Bring the hero and action accents away from blue into the graphite/emerald reference palette.
palette_replacements = [
    ("listOf(Color(0xFF07172F), Color(0xFF123F84), Color(0xFF4F46E5))",
     "listOf(Color(0xFF0B1114), Color(0xFF17282A), Color(0xFF10231B))", "home balance gradient"),
    ("listOf(Color(0xFF2563EB), Color(0xFF14B8A6))",
     "listOf(Color(0xFF00A86B), Color(0xFF35D994))", "home action gradient"),
    ("listOf(Color(0xFF2563EB), Color(0xFF06B6D4))",
     "listOf(Color(0xFF068B59), Color(0xFF35D994))", "finance hub gradient"),
]
for old, new, label in palette_replacements:
    if old in screens:
        screens = screens.replace(old, new)
screens_file.write_text(screens, encoding="utf-8")

# The new bottom-navigation label exists in all locales.
translations = {
    "values/strings.xml": "Payments",
    "values-tr/strings.xml": "Ödemeler",
    "values-de/strings.xml": "Zahlungen",
}
for rel, value in translations.items():
    path = app / "src/main/res" / rel
    root = ET.parse(path).getroot()
    found = any(item.attrib.get("name") == "payments_home" for item in root.findall("string"))
    if not found:
        xml_value = value.replace("&", "&amp;")
        insertion = f'    <string name="payments_home">{xml_value}</string>\n'
        raw = path.read_text(encoding="utf-8")
        raw = raw.replace("</resources>", insertion + "</resources>", 1)
        path.write_text(raw, encoding="utf-8")
    # Re-parse after the insertion so malformed XML fails this step before compilation.
    ET.parse(path)

# Match the core feature banners to the quieter black/green brand while preserving their behavior.
gradient_updates = {
    "AddFundsScreen.kt": [
        ("listOf(Color(0xFF08152C), Color(0xFF1D4ED8), Color(0xFF0F766E))",
         "listOf(Color(0xFF0B1114), Color(0xFF17282A), Color(0xFF0D3A2A))")
    ],
    "PremiumToolsScreen.kt": [
        ("listOf(Color(0xFF0B1A35), Color(0xFF1D4ED8), Color(0xFF0F766E))",
         "listOf(Color(0xFF0B1114), Color(0xFF17282A), Color(0xFF0D3A2A))")
    ],
}
for filename, replacements in gradient_updates.items():
    path = ui / filename
    data = path.read_text(encoding="utf-8")
    for old, new in replacements:
        data = data.replace(old, new)
    path.write_text(data, encoding="utf-8")

payment_file = ui / "PaymentsHubScreen.kt"
if not payment_file.is_file():
    raise SystemExit(f"PaymentsHubScreen.kt overlay missing: {payment_file}")
if 'composable("payments")' not in screens or 'BottomItem("payments"' not in screens:
    raise SystemExit("Reference payments destination was not wired into navigation.")
if "lightColorScheme(" not in screens or "dashboardSearch" not in screens or "Search accounts, payments and activity" not in screens:
    raise SystemExit("Reference dark palette or home search is missing.")
print("Applied clean light/emerald UI, rounded shared search/input fields, home search, payments hub and compact bottom navigation.")
