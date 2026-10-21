#!/usr/bin/env python3
"""Add card-management actions to TUR BANK Plus."""
from pathlib import Path
import sys

project = Path(sys.argv[1])
screen = project / "app/src/main/java/com/turbank/mobile/ui/BankScreens.kt"
source = screen.read_text(encoding="utf-8")

replacements = [
    (
        '    var deleteTarget by remember { mutableStateOf<VirtualCardEntity?>(null) }\n',
        '    var deleteTarget by remember { mutableStateOf<VirtualCardEntity?>(null) }\n'
        '    var limitTarget by remember { mutableStateOf<VirtualCardEntity?>(null) }\n',
        "limit dialog state",
    ),
    (
        '                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {\n'
        '                        OutlinedButton(onClick = { vm.updateCard(card.copy(frozen = !card.frozen)); }, modifier = Modifier.weight(1f)) {\n'
        '                            Text(stringResource(if (card.frozen) R.string.unfreeze_card else R.string.freeze_card))\n'
        '                        }\n'
        '                        Button(onClick = { paymentFor = card }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.simulate_purchase)) }\n'
        '                    }\n'
        '                    TextButton(onClick = { deleteTarget = card }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }\n',
        '                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {\n'
        '                        OutlinedButton(onClick = { vm.updateCard(card.copy(frozen = !card.frozen)); }, modifier = Modifier.weight(1f)) {\n'
        '                            Text(stringResource(if (card.frozen) R.string.unfreeze_card else R.string.freeze_card))\n'
        '                        }\n'
        '                        Button(onClick = { paymentFor = card }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.simulate_purchase)) }\n'
        '                    }\n'
        '                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {\n'
        '                        OutlinedButton(onClick = { limitTarget = card }, modifier = Modifier.weight(1f)) {\n'
        '                            Text(when (settings.language) { "tr" -> "Limiti düzenle"; "de" -> "Limit bearbeiten"; else -> "Edit limit" })\n'
        '                        }\n'
        '                        OutlinedButton(onClick = { vm.addCard(card.nickname + " Plus", card.limitMinor) }, modifier = Modifier.weight(1f)) {\n'
        '                            Text(when (settings.language) { "tr" -> "Kartı çoğalt"; "de" -> "Karte duplizieren"; else -> "Duplicate card" })\n'
        '                        }\n'
        '                    }\n'
        '                    TextButton(onClick = { deleteTarget = card }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }\n',
        "card limit edit and duplicate controls",
    ),
    (
        '    deleteTarget?.let { card ->\n'
        '        AlertDialog(\n'
        '            onDismissRequest = { deleteTarget = null }, title = { Text(stringResource(R.string.delete) + " · " + card.nickname) },\n'
        '            text = { Text(stringResource(R.string.delete_card_confirmation)) },\n'
        '            confirmButton = { TextButton(onClick = { vm.deleteCard(card.id); deleteTarget = null }) { Text(stringResource(R.string.delete)) } },\n'
        '            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } }\n'
        '        )\n'
        '    }\n'
        '}\n',
        '    deleteTarget?.let { card ->\n'
        '        AlertDialog(\n'
        '            onDismissRequest = { deleteTarget = null }, title = { Text(stringResource(R.string.delete) + " · " + card.nickname) },\n'
        '            text = { Text(stringResource(R.string.delete_card_confirmation)) },\n'
        '            confirmButton = { TextButton(onClick = { vm.deleteCard(card.id); deleteTarget = null }) { Text(stringResource(R.string.delete)) } },\n'
        '            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } }\n'
        '        )\n'
        '    }\n'
        '    limitTarget?.let { card ->\n'
        '        var limitText by remember(card.id) { mutableStateOf("%.2f".format(Locale.US, card.limitMinor / 100.0)) }\n'
        '        val parsedLimit = Money.parseMinorUnits(limitText)\n'
        '        val validLimit = parsedLimit != null && parsedLimit >= card.spentMinor\n'
        '        AlertDialog(\n'
        '            onDismissRequest = { limitTarget = null },\n'
        '            title = { Text(when (settings.language) { "tr" -> "Kart harcama limiti"; "de" -> "Kartenlimit"; else -> "Card spending limit" }) },\n'
        '            text = {\n'
        '                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n'
        '                    Text(card.nickname)\n'
        '                    OutlinedTextField(\n'
        '                        value = limitText,\n'
        '                        onValueChange = { limitText = it.filter { ch -> ch.isDigit() || ch == \'.\' || ch == \',\' } },\n'
        '                        label = { Text(stringResource(R.string.spending_limit)) },\n'
        '                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),\n'
        '                        singleLine = true\n'
        '                    )\n'
        '                    if (!validLimit) Text(when (settings.language) { "tr" -> "Limit, mevcut harcamadan düşük olamaz."; "de" -> "Das Limit darf nicht unter den bisherigen Ausgaben liegen."; else -> "Limit cannot be below already-spent amount." }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)\n'
        '                }\n'
        '            },\n'
        '            confirmButton = { Button(onClick = { parsedLimit?.let { vm.updateCard(card.copy(limitMinor = it)) }; limitTarget = null }, enabled = validLimit) { Text(stringResource(R.string.save)) } },\n'
        '            dismissButton = { TextButton(onClick = { limitTarget = null }) { Text(stringResource(R.string.cancel)) } }\n'
        '        )\n'
        '    }\n'
        '}\n',
        "edit card limit dialog",
    ),
]

for old, new, label in replacements:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match for {label}; found {count}")
    source = source.replace(old, new, 1)
screen.write_text(source, encoding="utf-8")
print("Added duplicate-card action and spending-limit editor.")
