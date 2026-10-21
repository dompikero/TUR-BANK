#!/usr/bin/env python3
"""Apply additive TUR BANK Plus UI changes to the extracted Android source."""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_plus_ui.py PROJECT_DIR")

project = Path(sys.argv[1])
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
        '        else -> stringResource(R.string.settings)\n',
        "profile screen title",
    ),
    (
        '        actions = {\n'
        '            IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {\n',
        '        actions = {\n'
        '            IconButton(onClick = { nav.navigate("profile") { launchSingleTop = true } }) {\n'
        '                Icon(Icons.Default.Person, contentDescription = "Profile")\n'
        '            }\n'
        '            IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {\n',
        "profile toolbar shortcut",
    ),
    (
        '            composable("notifications") { NotificationsScreen(vm, settings) }\n'
        '            composable("settings") { SettingsScreen(vm, settings) }\n',
        '            composable("notifications") { NotificationsScreen(vm, settings) }\n'
        '            composable("settings") { SettingsScreen(vm, settings) }\n'
        '            composable("profile") { ProfileScreen(vm = vm, nav = nav) }\n',
        "profile navigation route",
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
print("Applied profile route, local personal profile, distinct card themes and card-limit progress.")
