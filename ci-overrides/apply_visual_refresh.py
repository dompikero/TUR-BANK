#!/usr/bin/env python3
"""Apply TUR BANK 1.3's animated premium dashboard and navigation transitions."""
from pathlib import Path
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_visual_refresh.py PROJECT_DIR")

project = Path(sys.argv[1])
screen = project / "app/src/main/java/com/turbank/mobile/ui/BankScreens.kt"
if not screen.is_file():
    raise SystemExit(f"BankScreens.kt not found: {screen}")
source = screen.read_text(encoding="utf-8")

imports = [
    "import androidx.compose.animation.AnimatedContent",
    "import androidx.compose.animation.AnimatedVisibility",
    "import androidx.compose.animation.fadeIn",
    "import androidx.compose.animation.slideInVertically",
    "import androidx.compose.animation.core.FastOutSlowInEasing",
    "import androidx.compose.animation.core.RepeatMode",
    "import androidx.compose.animation.core.animateFloatAsState",
    "import androidx.compose.animation.core.animateFloat",
    "import androidx.compose.animation.core.infiniteRepeatable",
    "import androidx.compose.animation.core.rememberInfiniteTransition",
    "import androidx.compose.animation.core.spring",
    "import androidx.compose.animation.core.tween",
    "import androidx.compose.material.icons.filled.AddCircle",
    "import androidx.compose.material.icons.filled.QrCode2",
    "import androidx.compose.foundation.interaction.MutableInteractionSource",
    "import androidx.compose.foundation.interaction.collectIsPressedAsState",
    "import androidx.compose.ui.draw.shadow",
    "import androidx.compose.ui.graphics.graphicsLayer",
    "import androidx.compose.ui.graphics.vector.ImageVector",
    "import androidx.compose.ui.text.style.TextAlign",
]
anchor = "import androidx.compose.animation.animateContentSize\n"
if source.count(anchor) != 1:
    raise SystemExit("Could not identify the animation import section.")
missing = [line for line in imports if line not in source]
source = source.replace(anchor, anchor + "".join(line + "\n" for line in missing), 1)

home_start_marker = "@Composable\nprivate fun HomeScreen(vm: BankViewModel, nav: NavHostController, settings: UserSettings) {"
home_end_marker = "@Composable\nprivate fun AccountsScreen(vm: BankViewModel, settings: UserSettings) {"
start = source.find(home_start_marker)
end = source.find(home_end_marker, start + 1)
if start < 0 or end < 0 or end <= start:
    raise SystemExit("Could not isolate HomeScreen for the premium dashboard update.")

premium_home = r'''@Composable
private fun HomeScreen(vm: BankViewModel, nav: NavHostController, settings: UserSettings) {
    val accounts by vm.accounts.collectAsState()
    val recent by vm.recentTransactions.collectAsState()
    val cards by vm.cards.collectAsState()
    val goals by vm.goals.collectAsState()
    val bills by vm.bills.collectAsState()
    val showBalances = settings.showBalances
    val currencyAccounts = accounts.filter { it.currencyCode == settings.displayCurrency }
    val total = currencyAccounts.sumOf { it.balanceMinor }
    val context = LocalContext.current
    val profileName = remember(context) {
        context.getSharedPreferences("tur_bank_personal_profile", Context.MODE_PRIVATE)
            .getString("profile_name", "").orEmpty()
    }
    val greeting = when (settings.language) {
        "tr" -> if (profileName.isBlank()) "Tekrar hoş geldin" else "Merhaba, " + profileName.trim().substringBefore(" ")
        "de" -> if (profileName.isBlank()) "Schön, dass du da bist" else "Hallo, " + profileName.trim().substringBefore(" ")
        else -> if (profileName.isBlank()) "Welcome back" else "Hello, " + profileName.trim().substringBefore(" ")
    }
    var dashboardReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dashboardReady = true }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DashboardEntrance(index = 0, visible = dashboardReady) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(17.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF079B63), Color(0xFF35C88B)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "TUR BANK",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(greeting, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        dashboardText(settings.language, "Finansların tek bakışta.", "Your money, at a glance.", "Deine Finanzen auf einen Blick."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {
                    Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.notifications))
                }
                IconButton(
                    onClick = { nav.navigate("profile") { launchSingleTop = true } },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Person, contentDescription = dashboardText(settings.language, "Profil", "Profile", "Profil"))
                }
            }
        }

        DashboardEntrance(index = 1, visible = dashboardReady) {
            DashboardBalanceCard(
                totalMinor = total,
                currency = settings.displayCurrency,
                language = settings.language,
                showBalances = showBalances,
                accountCount = accounts.size,
                cardCount = cards.size,
                onToggleBalances = { vm.setShowBalances(!showBalances) }
            )
        }

        DashboardEntrance(index = 2, visible = dashboardReady) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        dashboardText(settings.language, "Hızlı işlemler", "Quick actions", "Schnellzugriff"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dashboardText(settings.language, "Sık kullandıkların burada", "Everything you use most", "Deine wichtigsten Funktionen"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { nav.navigate("tools") }) { Text(stringResource(R.string.tools)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardAction(Icons.Default.SwapHoriz, stringResource(R.string.send_money), Modifier.weight(1f)) { nav.navigate("transfer") }
            DashboardAction(
                Icons.Default.QrCode2,
                dashboardText(settings.language, "QR ile ödeme", "QR payments", "QR-Zahlung"),
                Modifier.weight(1f)
            ) { nav.navigate("qr-pay") }
            DashboardAction(Icons.Default.CreditCard, stringResource(R.string.cards), Modifier.weight(1f)) { nav.navigate("cards") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardAction(Icons.Default.Analytics, stringResource(R.string.insights), Modifier.weight(1f)) { nav.navigate("insights") }
            DashboardAction(Icons.Default.Savings, stringResource(R.string.goals), Modifier.weight(1f)) { nav.navigate("goals") }
        }

        DashboardEntrance(index = 3, visible = dashboardReady) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.my_accounts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        accounts.size.toString() + " " + dashboardText(settings.language, "hesap", "accounts", "Konten"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { nav.navigate("accounts") }) { Text(stringResource(R.string.view_all)) }
            }
        }
        if (accounts.isEmpty()) {
            DashboardEntrance(index = 4, visible = dashboardReady) { EmptyState(stringResource(R.string.no_items)) }
        }
        accounts.filter { account -> dashboardSearch.isBlank() || listOf(account.name, account.currencyCode, account.id).any { it.contains(dashboardSearch, true) } }.take(3).forEachIndexed { index, account ->
            DashboardEntrance(index = 4 + index, visible = dashboardReady) {
                AccountRow(account, showBalances, settings.language, onClick = { nav.navigate("accounts") })
            }
        }

        DashboardEntrance(index = 7, visible = dashboardReady) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.recent_activity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        dashboardText(settings.language, "Son hareketlerin", "Latest activity", "Letzte Aktivitäten"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { nav.navigate("activity") }) { Text(stringResource(R.string.view_all)) }
            }
        }
        if (recent.isEmpty()) {
            DashboardEntrance(index = 8, visible = dashboardReady) { EmptyState(stringResource(R.string.no_transactions)) }
        }
        recent.take(4).forEachIndexed { index, transaction ->
            DashboardEntrance(index = 8 + index, visible = dashboardReady) {
                TransactionRow(transaction, accounts, showBalances, settings.language, onClick = { nav.navigate("activity") })
            }
        }

        DashboardEntrance(index = 12, visible = dashboardReady) {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(26.dp), clip = false)
                    .clickable { nav.navigate("finance-hub") },
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(17.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            dashboardText(settings.language, "Finans merkezi", "Finance hub", "Finanz-Cockpit"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            dashboardText(settings.language, "Bütçe, işlem arama ve CSV ekstre", "Budgets, transaction search and CSV statements", "Budgets, Suche und CSV-Auszüge"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        DashboardEntrance(index = 13, visible = dashboardReady) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardMiniStat(
                    label = dashboardText(settings.language, "Birikim hedefi", "Savings goals", "Sparziele"),
                    value = goals.size.toString(),
                    icon = Icons.Default.Savings,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniStat(
                    label = dashboardText(settings.language, "Fatura", "Bills", "Rechnungen"),
                    value = bills.count { !it.paid }.toString(),
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(stringResource(R.string.ledger_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardBalanceCard(
    totalMinor: Long,
    currency: String,
    language: String,
    showBalances: Boolean,
    accountCount: Int,
    cardCount: Int,
    onToggleBalances: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    val transition = rememberInfiniteTransition(label = "balance-card-glow")
    val glow by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.17f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "balance-card-glow-alpha"
    )
    val displayedBalance = if (showBalances) Money.format(totalMinor, currency, language) else "••••••"
    Box(
        Modifier.fillMaxWidth().shadow(15.dp, shape, clip = false).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFE8EEEE), Color(0xFFF5F7F7), Color(0xFFE5EFEB))))
            .padding(22.dp)
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 46.dp, y = (-45).dp).size(178.dp)
                .clip(CircleShape).background(Color(0xFF079B63).copy(alpha = glow))
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 17.dp, y = 47.dp).size(118.dp)
                .clip(CircleShape).background(Color(0xFF079B63).copy(alpha = 0.08f))
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        dashboardText(language, "TOPLAM BAKİYE", "TOTAL BALANCE", "GESAMTSALDO"),
                        color = Color(0xFF667573),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dashboardText(language, "Tüm hesapların · " + currency, "All accounts · " + currency, "Alle Konten · " + currency),
                        color = Color(0xFF6D7B79),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(
                    onClick = onToggleBalances,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.78f))
                ) {
                    Icon(
                        if (showBalances) Icons.Default.Shield else Icons.Default.Wallet,
                        contentDescription = dashboardText(language, "Bakiyeyi gizle/göster", "Hide/show balance", "Saldo ein-/ausblenden"),
                        tint = Color(0xFF168E68)
                    )
                }
            }
            AnimatedContent(targetState = displayedBalance, label = "balance-value") { value ->
                Text(
                    value,
                    color = Color(0xFF1D302C),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF5EEAD4)))
                Text(
                    dashboardText(language, "KİŞİSEL HESAP ALANI", "PERSONAL ACCOUNT", "PERSÖNLICHER KONTOBEREICH"),
                    color = Color(0xFF168E68),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(color = Color(0xFFD2DEDA))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardHeroMetric(
                    label = dashboardText(language, "HESAP", "ACCOUNTS", "KONTEN"),
                    value = accountCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                DashboardHeroMetric(
                    label = dashboardText(language, "SANAL KART", "VIRTUAL CARDS", "VIRTUELLE KARTEN"),
                    value = cardCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                DashboardHeroMetric(
                    label = dashboardText(language, "GİZLİLİK", "PRIVACY", "DATENSCHUTZ"),
                    value = dashboardText(language, "AÇIK", "ON", "AN"),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color(0xFF70807C), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(value, color = Color(0xFF1D302C), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun DashboardAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 650f),
        label = "dashboard-action-press"
    )
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (pressed) 2.dp else 5.dp, shape, clip = false)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF079B63), Color(0xFF35C88B)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2
            )
        }
    }
}

@Composable
private fun DashboardMiniStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DashboardEntrance(
    index: Int,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    AnimatedVisibility(
        visible = visible && entered,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 420, delayMillis = (index * 62).coerceAtMost(520), easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = tween(durationMillis = 420, delayMillis = (index * 62).coerceAtMost(520), easing = FastOutSlowInEasing)) { it / 14 }
    ) {
        content()
    }
}

private fun dashboardText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}
'''

source = source[:start] + premium_home + "\n\n" + source[end:]

# Wrap every top-level destination in a common short fade/slide transition.
nav_marker = 'NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(inner)) {'
nav_pos = source.find(nav_marker)
if nav_pos < 0:
    raise SystemExit("Could not locate main navigation host.")
nav_end = source.find("\n        }\n    }\n}", nav_pos)
if nav_end < 0:
    raise SystemExit("Could not locate the end of the main navigation routes.")
nav_block = source[nav_pos:nav_end]
pattern = re.compile(r'(?m)^(\s*)composable\("([^"]+)"\) \{ ([^\n{}]+) \}$')
routes = {"home", "accounts", "transfer", "activity", "insights", "goals", "bills", "cards", "tools", "receive", "notifications", "settings", "profile", "finance-hub"}
changed = []
def wrap_route(match):
    indent, route, body = match.groups()
    if route not in routes:
        return match.group(0)
    changed.append(route)
    return indent + 'composable("' + route + '") { RouteEntrance { ' + body + ' } }'
nav_block = pattern.sub(wrap_route, nav_block)
if set(changed) != routes:
    raise SystemExit("Navigation transition patch incomplete; wrapped=" + ", ".join(changed))
source = source[:nav_pos] + nav_block + source[nav_end:]

# Add a cascade animation to card creation and animate the spending-limit indicator.
cards_start = source.find("private fun CardsScreen(")
if cards_start < 0:
    raise SystemExit("Could not locate CardsScreen.")
cards_end = source.find("@Composable\nprivate fun ToolsScreen(", cards_start)
if cards_end < 0:
    raise SystemExit("Could not locate the end of CardsScreen.")
cards_block = source[cards_start:cards_end]
old_loop = "        cards.forEach { card ->\n"
if cards_block.count(old_loop) != 1:
    raise SystemExit("Could not uniquely locate virtual card list.")
cards_block = cards_block.replace(
    old_loop,
    "        cards.forEachIndexed { cardIndex, card ->\n            DashboardEntrance(index = cardIndex) {\n",
    1
)
old_progress = "            val limitProgress = if (card.limitMinor > 0L) (card.spentMinor.toFloat() / card.limitMinor.toFloat()).coerceIn(0f, 1f) else 0f\n"
new_progress = old_progress + """            val animatedLimitProgress by animateFloatAsState(
                targetValue = limitProgress,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                label = "card-limit-progress"
            )
"""
if cards_block.count(old_progress) != 1:
    raise SystemExit("Could not uniquely locate card-limit progress.")
cards_block = cards_block.replace(old_progress, new_progress, 1)
if cards_block.count("progress = limitProgress,") != 1:
    raise SystemExit("Could not uniquely locate progress bar.")
cards_block = cards_block.replace("progress = limitProgress,", "progress = animatedLimitProgress,", 1)
if cards_block.count("        }\n        Text(stringResource(R.string.card_security_note)") != 1:
    raise SystemExit("Could not uniquely locate end of animated card list.")
cards_block = cards_block.replace(
    "        }\n        Text(stringResource(R.string.card_security_note)",
    "            }\n        }\n        Text(stringResource(R.string.card_security_note)",
    1
)
source = source[:cards_start] + cards_block + source[cards_end:]

# Add destination transition composable only once.
helper = r'''@Composable
private fun RouteEntrance(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
            slideInVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) { it / 24 }
    ) {
        content()
    }
}

'''
if "private fun RouteEntrance(" in source:
    raise SystemExit("RouteEntrance already exists; refusing to apply the patch twice.")
anchor = "@Composable\nprivate fun localizeMessage(code: String): String"
pos = source.find(anchor)
if pos < 0:
    raise SystemExit("Could not find helper insertion point.")
source = source[:pos] + helper + source[pos:]

if "FinanceAcademyScreen" in source or 'composable("academy")' in source or "Icons.Default.School" in source:
    raise SystemExit("Removed Finance Academy code is still referenced from BankScreens.kt.")
if len(changed) != 14:
    raise SystemExit("Unexpected route animation count.")

screen.write_text(source, encoding="utf-8")
print("Applied premium animated dashboard, route transitions, press feedback, staged lists, and animated card limit bars.")
