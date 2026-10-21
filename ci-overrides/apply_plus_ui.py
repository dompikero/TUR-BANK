#!/usr/bin/env python3
"""Apply additive TUR BANK Plus UI changes to the extracted Android source."""
from pathlib import Path
import base64
import gzip
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_plus_ui.py PROJECT_DIR")

project = Path(sys.argv[1])
encoded_hub = project / "app/src/main/java/com/turbank/mobile/ui/FinanceHubScreen.kt.gz.b64"
if encoded_hub.is_file():
    hub_source = project / "app/src/main/java/com/turbank/mobile/ui/FinanceHubScreen.kt"
    hub_source.parent.mkdir(parents=True, exist_ok=True)
    hub_source.write_bytes(gzip.decompress(base64.b64decode(encoded_hub.read_text(encoding="ascii").strip())))
    encoded_hub.unlink()

build_file = project / "app/build.gradle.kts"
build_text = build_file.read_text(encoding="utf-8")
old_version = '        versionCode = 1\n        versionName = "1.0.0"'
new_version = '        versionCode = 2\n        versionName = "1.1.0"'
if old_version in build_text:
    build_file.write_text(build_text.replace(old_version, new_version, 1), encoding="utf-8")
elif new_version not in build_text:
    raise SystemExit("Could not safely bump app version for TUR BANK Plus.")

screen = project / "app/src/main/java/com/turbank/mobile/ui/BankScreens.kt"
if not screen.is_file():
    raise SystemExit(f"BankScreens.kt not found: {screen}")
source = screen.read_text(encoding="utf-8")

replacements = [
    (
        'import androidx.compose.material.icons.filled.Tune\n',
        'import androidx.compose.material.icons.filled.Tune\nimport androidx.compose.material.icons.filled.Person\n',
        "profile icon import",
    ),
    (
        '        "notifications" -> stringResource(R.string.notifications)\n        else -> stringResource(R.string.settings)\n',
        '        "notifications" -> stringResource(R.string.notifications)\n'
        '        "profile" -> when (settings.language) {\n'
        '            "tr", "de" -> "Profil"\n'
        '            else -> "Profile"\n'
        '        }\n'
        '        "finance-hub" -> when (settings.language) {\n'
        '            "tr" -> "Finansal Merkez"\n'
        '            "de" -> "Finanz-Cockpit"\n'
        '            else -> "Finance Hub"\n'
        '        }\n'
        '        else -> stringResource(R.string.settings)\n',
        "profile and finance hub titles",
    ),
    (
        '                actions = {\n'
        '                    IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {\n',
        '                actions = {\n'
        '                    IconButton(onClick = { nav.navigate("finance-hub") { launchSingleTop = true } }) {\n'
        '                        Icon(Icons.Default.Analytics, contentDescription = "Finance Hub")\n'
        '                    }\n'
        '                    IconButton(onClick = { nav.navigate("profile") { launchSingleTop = true } }) {\n'
        '                        Icon(Icons.Default.Person, contentDescription = "Profile")\n'
        '                    }\n'
        '                    IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {\n',
        "profile and finance hub toolbar shortcuts",
    ),
    (
        '            composable("notifications") { NotificationsScreen(vm, settings) }\n'
        '            composable("settings") { SettingsScreen(vm, settings) }\n',
        '            composable("notifications") { NotificationsScreen(vm, settings) }\n'
        '            composable("settings") { SettingsScreen(vm, settings) }\n'
        '            composable("profile") { ProfileScreen(vm = vm, nav = nav) }\n'
        '            composable("finance-hub") { FinanceHubScreen(vm = vm, nav = nav, settings = settings) }\n',
        "profile and finance hub navigation routes",
    ),
    (
        '        Text(stringResource(R.string.display_currency_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n',
        '        Card(\n'
        '            modifier = Modifier.fillMaxWidth().clickable { nav.navigate("finance-hub") },\n'
        '            shape = RoundedCornerShape(22.dp),\n'
        '            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)\n'
        '        ) {\n'
        '            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {\n'
        '                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))\n'
        '                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {\n'
        '                    Text("TUR BANK PLUS", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)\n'
        '                    Text(when (settings.language) { "tr" -> "Yeni finansal kontrol merkezi · bütçe · işlem arama · CSV ekstre"; "de" -> "Finanz-Cockpit · Budget · Umsatzsuche · CSV-Auszug"; else -> "Finance hub · budget · transaction search · CSV statement" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)\n'
        '                }\n'
        '                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)\n'
        '            }\n'
        '        }\n'
        '        Text(stringResource(R.string.display_currency_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n',
        "home screen plus hub card",
    ),
    (
        '        cards.forEach { card ->\n'
        '            Card(\n'
        '                shape = RoundedCornerShape(24.dp),\n'
        '                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),\n',
        '        cards.forEach { card ->\n'
        '            val cardGradient = when (Math.floorMod(card.nickname.hashCode(), 4)) {\n'
        '                0 -> listOf(Color(0xFF10213F), Color(0xFF0077B6))\n'
        '                1 -> listOf(Color(0xFF3A0CA3), Color(0xFF7209B7))\n'
        '                2 -> listOf(Color(0xFF004D40), Color(0xFF16A085))\n'
        '                else -> listOf(Color(0xFF7F1D1D), Color(0xFFEA580C))\n'
        '            }\n'
        '            val limitProgress = if (card.limitMinor > 0L) (card.spentMinor.toFloat() / card.limitMinor.toFloat()).coerceIn(0f, 1f) else 0f\n'
        '            Card(\n'
        '                shape = RoundedCornerShape(24.dp),\n'
        '                colors = CardDefaults.cardColors(containerColor = cardGradient.first()),\n',
        "dynamic virtual card styles",
    ),
    (
        '                Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF2E416D)))).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {\n',
        '                Column(Modifier.background(Brush.linearGradient(cardGradient)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {\n',
        "card gradient",
    ),
    (
        '                        if (card.frozen) Text(stringResource(R.string.frozen_badge), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)\n'
        '                    }\n'
        '                }\n'
        '            }\n'
        '            Card(shape = CardRadius) {\n',
        '                        if (card.frozen) Text(stringResource(R.string.frozen_badge), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)\n'
        '                    }\n'
        '                    LinearProgressIndicator(\n'
        '                        progress = limitProgress,\n'
        '                        modifier = Modifier.fillMaxWidth().height(5.dp),\n'
        '                        color = MaterialTheme.colorScheme.secondary,\n'
        '                        trackColor = Color.White.copy(alpha = 0.22f)\n'
        '                    )\n'
        '                }\n'
        '            }\n'
        '            Card(shape = CardRadius) {\n',
        "card limit progress",
    ),
]

for old, new, label in replacements:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match for {label}; found {count}")
    source = source.replace(old, new, 1)

screen.write_text(source, encoding="utf-8")
print("Applied profile route, finance hub, custom card themes, spending progress, CSV export and local personal finance tools.")
