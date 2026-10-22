#!/usr/bin/env python3
"""TUR BANK 1.4 personal onboarding, seed cleanup, QR payment and navigation refresh."""
from pathlib import Path
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_personal_upgrade.py PROJECT_DIR")

project = Path(sys.argv[1])
app = project / "app"
src = app / "src/main/java/com/turbank/mobile"
ui = src / "ui"
db_file = src / "data/BankDatabase.kt"
repo_file = src / "data/BankRepository.kt"
entities_file = src / "data/Entities.kt"
vm_file = src / "BankViewModel.kt"
main_file = src / "MainActivity.kt"
screens_file = ui / "BankScreens.kt"
gradle_file = app / "build.gradle.kts"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


# Clean only known seed rows from older installations; do not seed sample accounts on new installs.
db = db_file.read_text(encoding="utf-8")
dao_additions = [
    ('@Query("DELETE FROM accounts") suspend fun clear()',
     '@Query("DELETE FROM accounts") suspend fun clear()\n    @Query("DELETE FROM accounts WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM transactions") suspend fun clear()',
     '@Query("DELETE FROM transactions") suspend fun clear()\n    @Query("DELETE FROM transactions WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM recipients") suspend fun clear()',
     '@Query("DELETE FROM recipients") suspend fun clear()\n    @Query("DELETE FROM recipients WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM savings_goals") suspend fun clear()',
     '@Query("DELETE FROM savings_goals") suspend fun clear()\n    @Query("DELETE FROM savings_goals WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM budgets") suspend fun clear()',
     '@Query("DELETE FROM budgets") suspend fun clear()\n    @Query("DELETE FROM budgets WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM bills") suspend fun clear()',
     '@Query("DELETE FROM bills") suspend fun clear()\n    @Query("DELETE FROM bills WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM virtual_cards") suspend fun clear()',
     '@Query("DELETE FROM virtual_cards") suspend fun clear()\n    @Query("DELETE FROM virtual_cards WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
    ('@Query("DELETE FROM app_notifications") suspend fun clear()',
     '@Query("DELETE FROM app_notifications") suspend fun clear()\n    @Query("DELETE FROM app_notifications WHERE id LIKE \'seed-%\'") suspend fun clearSeeded()'),
]
for old, new in dao_additions:
    if new not in db:
        db = replace_once(db, old, new, "legacy seed cleanup DAO")
db_file.write_text(db, encoding="utf-8")

repo = repo_file.read_text(encoding="utf-8")
seed_start = repo.find("    suspend fun seedIfNeeded()")
create_start = repo.find("    suspend fun createAccount(", seed_start)
if seed_start < 0 or create_start < 0:
    raise SystemExit("Could not locate repository seed section.")
repo_code = '''    suspend fun seedIfNeeded() = db.withTransaction {
        // Upgrade cleanup: remove rows inserted by the old sample-data pack only.
        if (db.accounts().getAll().any { it.id.startsWith("seed-") }) {
            db.transactions().clearSeeded()
            db.accounts().clearSeeded()
            db.recipients().clearSeeded()
            db.goals().clearSeeded()
            db.budgets().clearSeeded()
            db.bills().clearSeeded()
            db.cards().clearSeeded()
            db.notifications().clearSeeded()
        }
    }

    suspend fun resetDemoData() = db.withTransaction {
        db.transactions().clear()
        db.goals().clear()
        db.budgets().clear()
        db.bills().clear()
        db.cards().clear()
        db.recipients().clear()
        db.notifications().clear()
        db.accounts().clear()
    }

'''
repo = repo[:seed_start] + repo_code + repo[create_start:]
repo = repo.replace('description.ifBlank { "Demo transfer" }', 'description.ifBlank { "Transfer" }')
repo = repo.replace('title = "Demo transfer complete"', 'title = "Transfer recorded"')
repo = repo.replace('body = "A demo payment to $label was recorded. No real money moved."', 'body = "Activity was recorded locally for $label."')
funds_start = repo.find("    suspend fun receiveDemoMoney(")
funds_end = repo.find("    suspend fun createGoal(", funds_start)
if funds_start < 0 or funds_end < 0 or funds_end <= funds_start:
    raise SystemExit("Could not isolate incoming-funds repository operation.")
funds_method = '''    suspend fun addFunds(
        accountId: String,
        amountMinor: Long,
        sender: String,
        description: String
    ): TransferResult = db.withTransaction {
        if (amountMinor <= 0) return@withTransaction TransferResult.Failure("invalid_amount")
        val account = db.accounts().getById(accountId)
            ?: return@withTransaction TransferResult.Failure("source_missing")
        if (account.balanceMinor > Long.MAX_VALUE - amountMinor) {
            return@withTransaction TransferResult.Failure("invalid_amount")
        }
        val id = UUID.randomUUID().toString()
        db.accounts().setBalance(account.id, account.balanceMinor + amountMinor)
        db.transactions().insert(TransactionEntity(
            id = id,
            accountId = account.id,
            recipient = sender.ifBlank { "Manual entry" },
            amountMinor = amountMinor,
            direction = "IN",
            category = "Deposit",
            description = description.ifBlank { "Balance entry" },
            operationId = id
        ))
        db.notifications().insert(AppNotificationEntity(
            title = "Balance updated",
            body = "A balance entry was recorded locally for " + account.name + "."
        ))
        TransferResult.Success(id)
    }

'''
repo = repo[:funds_start] + funds_method + repo[funds_end:]
repo_file.write_text(repo, encoding="utf-8")

entities = entities_file.read_text(encoding="utf-8")
entities = entities.replace('val reference: String = "DEMO-" + UUID.randomUUID().toString().take(6).uppercase()', 'val reference: String = "TUR-" + UUID.randomUUID().toString().take(6).uppercase()')
entities_file.write_text(entities, encoding="utf-8")

# Create a named personal account before switching into the application.
vm = vm_file.read_text(encoding="utf-8")
vm_anchor = '    fun createAccount(name: String, type: String, currency: String = "EUR") = perform {'
vm_method = '''    fun completePersonalSetup(accountName: String, type: String, currency: String) = perform {
        if (accountName.isBlank() || type.isBlank() || currency.length != 3) {
            "invalid_input"
        } else if (!repository.createAccount(accountName.trim(), type, currency.uppercase())) {
            "invalid_input"
        } else {
            preferences.setOnboardingComplete(true)
            "setup_complete"
        }
    }

'''
if "fun completePersonalSetup(" not in vm:
    vm = replace_once(vm, vm_anchor, vm_method + vm_anchor, "personal account setup method")
funds_start = vm.find("    fun receiveDemoMoney(")
funds_end = vm.find("    fun createGoal(", funds_start)
if funds_start < 0 or funds_end < 0 or funds_end <= funds_start:
    raise SystemExit("Could not isolate incoming-funds view model operation.")
vm_funds = '''    fun addFunds(accountId: String, amountMinor: Long?, sender: String, description: String) {
        val amount = amountMinor ?: run { _message.value = "invalid_amount"; return }
        perform {
            when (val result = repository.addFunds(accountId, amount, sender, description)) {
                is TransferResult.Success -> "receive_success"
                is TransferResult.Failure -> result.reason
            }
        }
    }

'''
vm = vm[:funds_start] + vm_funds + vm[funds_end:]
vm_file.write_text(vm, encoding="utf-8")

# Language selection is followed by the new local profile + account setup, including a unique PIN hash.
main = main_file.read_text(encoding="utf-8")
main = main.replace("com.turbank.mobile.ui.DemoLockScreen", "com.turbank.mobile.ui.AccountLockScreen")
main = main.replace("DemoLockScreen(", "AccountLockScreen(")
main = main.replace("the demo remains locked.", "the app remains locked.")
if "import com.turbank.mobile.ui.PersonalAccountSetupScreen" not in main:
    main = replace_once(
        main,
        "import com.turbank.mobile.ui.LanguageSelectionScreen",
        "import com.turbank.mobile.ui.LanguageSelectionScreen\nimport com.turbank.mobile.ui.PersonalAccountSetupScreen\nimport com.turbank.mobile.ui.LocalPinSecurity",
        "setup screen imports"
    )
if "var hasPersonalSetup by remember" not in main:
    main = replace_once(
        main,
        "            var unlocked by remember { mutableStateOf(false) }\n",
        '''            var unlocked by remember { mutableStateOf(false) }
            var hasPersonalSetup by remember {
                mutableStateOf(
                    getSharedPreferences("tur_bank_personal_profile", MODE_PRIVATE)
                        .getBoolean("setup_complete", false)
                )
            }
''',
        "setup persistence state"
    )
if "PersonalAccountSetupScreen(" not in main:
    old_flow = '                    !settings.onboardingComplete -> com.turbank.mobile.ui.WelcomeScreen(onContinue = { vm.completeOnboarding() })\n'
    new_flow = '''                    !settings.onboardingComplete || !hasPersonalSetup -> PersonalAccountSetupScreen(
                        language = settings.language,
                        onCreate = { name, email, accountName, accountType, currency, pin ->
                            val profilePrefs = getSharedPreferences("tur_bank_personal_profile", MODE_PRIVATE)
                            val salt = LocalPinSecurity.createSalt()
                            profilePrefs.edit()
                                .putString("profile_name", name)
                                .putString("profile_email", email)
                                .putString("profile_phone", "")
                                .putString("profile_pin_salt", salt)
                                .putString("profile_pin_hash", LocalPinSecurity.hashPin(pin, salt))
                                .putBoolean("setup_complete", true)
                                .apply()
                            hasPersonalSetup = true
                            vm.completePersonalSetup(accountName, accountType, currency)
                        }
                    )
'''
    main = replace_once(main, old_flow, new_flow, "welcome replacement")
if "language = settings.language,\n                        showBiometric = settings.biometricPreference," not in main:
    main = replace_once(
        main,
        'settings.lockOnRestart && !unlocked -> DemoLockScreen(\n                        showBiometric = settings.biometricPreference,',
        'settings.lockOnRestart && !unlocked -> DemoLockScreen(\n                        language = settings.language,\n                        showBiometric = settings.biometricPreference,',
        "pass selected language to PIN lock"
    )
main_file.write_text(main, encoding="utf-8")

# QR camera scanner, and bumped version.
gradle = gradle_file.read_text(encoding="utf-8")
if 'implementation("com.journeyapps:zxing-android-embedded:4.3.0")' not in gradle:
    gradle = replace_once(gradle, "    implementation(libs.zxing.core)\n",
        '    implementation(libs.zxing.core)\n    implementation("com.journeyapps:zxing-android-embedded:4.3.0")\n',
        "QR scanner library")
old_version = '        versionCode = 5\n        versionName = "1.4.0"'
new_version = '        versionCode = 6\n        versionName = "1.5.0"'
if old_version in gradle:
    gradle = gradle.replace(old_version, new_version, 1)
elif new_version not in gradle:
    raise SystemExit("Could not safely bump app version to TUR BANK 1.5.0.")
gradle_file.write_text(gradle, encoding="utf-8")

screens = screens_file.read_text(encoding="utf-8")
screens = screens.replace("DemoLockScreen(", "AccountLockScreen(")
screens = screens.replace("fun DemoLockScreen(", "fun AccountLockScreen(")
screens = screens.replace("DemoNotice()", "LocalRecordNotice()")
screens = screens.replace("fun DemoNotice()", "fun LocalRecordNotice()")
anchor = "import androidx.compose.material.icons.filled.Home\n"
new_imports = [
    "import androidx.compose.material.icons.filled.GridView",
    "import androidx.compose.material.icons.filled.Notifications",
    "import androidx.compose.material.icons.filled.Person",
    "import androidx.compose.material.icons.filled.QrCode2",
]
if any(line not in screens for line in new_imports):
    screens = replace_once(screens, anchor, anchor + "".join(line + "\n" for line in new_imports if line not in screens), "new nav icons")

# Replace the universal test PIN with the salted PIN selected by the user during onboarding.
lock_start = screens.find("@Composable\nfun DemoLockScreen(")
nav_start = screens.find("@Composable\nfun MainBankNavigation(vm: BankViewModel)", lock_start)
if lock_start < 0 or nav_start < 0 or nav_start <= lock_start:
    raise SystemExit("Could not isolate access-lock composable.")
lock_screen = '''@Composable
fun DemoLockScreen(language: String, showBiometric: Boolean, onUnlock: () -> Unit, onBiometricUnlock: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tur_bank_personal_profile", Context.MODE_PRIVATE) }
    val salt = remember { prefs.getString("profile_pin_salt", "").orEmpty() }
    val expectedHash = remember { prefs.getString("profile_pin_hash", "").orEmpty() }
    var pin by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("TUR BANK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Text(appText(language, "Hesabının kilidini aç", "Unlock your account", "Konto entsperren"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(appText(language, "Kurulum sırasında oluşturduğun PIN'i gir.", "Enter the PIN you created during setup.", "Gib die bei der Einrichtung erstellte PIN ein."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6); wrong = false },
            label = { Text(appText(language, "Uygulama PIN'i", "App PIN", "App-PIN")) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (wrong) Text(appText(language, "PIN yanlış. Tekrar dene.", "Incorrect PIN. Try again.", "Falsche PIN. Bitte erneut versuchen."), color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (LocalPinSecurity.matches(pin, salt, expectedHash)) onUnlock() else wrong = true },
            enabled = pin.length in 4..6,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(appText(language, "Kilidi aç", "Unlock", "Entsperren")) }
        if (showBiometric) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBiometricUnlock, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Shield, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(appText(language, "Cihaz güvenliğiyle aç", "Unlock with device security", "Mit Gerätesicherheit entsperren"))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(appText(language, "Kayıtların bu cihazda saklanır.", "Your records are stored on this device.", "Deine Daten werden auf diesem Gerät gespeichert."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

private fun appText(language: String, tr: String, en: String, de: String): String = when (language) {
    "tr" -> tr
    "de" -> de
    else -> en
}

private data class BottomItem(val route: String, val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

'''
screens = screens[:lock_start] + lock_screen + screens[nav_start:]
nav_signature = "@Composable" + chr(10) + "fun MainBankNavigation(vm: BankViewModel)"
nav_annotated = "@OptIn(ExperimentalMaterial3Api::class)" + chr(10) + nav_signature
if nav_annotated not in screens:
    screens = replace_once(screens, nav_signature, nav_annotated, "restore Material 3 API opt-in")

# Update title for QR and replace the old five bottom shortcuts with a coherent five-destination bar.
old_receive_title = '        "receive" -> stringResource(R.string.receive_demo)\n'
if old_receive_title in screens:
    screens = screens.replace(old_receive_title, '        "receive", "qr-pay" -> stringResource(R.string.receive_demo)\n', 1)
elif '"qr-pay" -> stringResource(R.string.receive_demo)' not in screens:
    notif_anchor = '        "notifications" -> stringResource(R.string.notifications)\n'
    screens = replace_once(screens, notif_anchor, '        "qr-pay" -> stringResource(R.string.receive_demo)\n' + notif_anchor, "QR toolbar title")
bottom_start = screens.find("    val bottomItems = listOf(")
bottom_end = screens.find("\n\n    Scaffold(", bottom_start)
if bottom_start < 0 or bottom_end < 0:
    raise SystemExit("Could not locate the bottom navigation list.")
bottom = '''    val bottomItems = listOf(
        BottomItem("home", R.string.home, Icons.Default.Home),
        BottomItem("accounts", R.string.accounts, Icons.Default.AccountBalanceWallet),
        BottomItem("qr-pay", R.string.receive_demo, Icons.Default.QrCode2),
        BottomItem("activity", R.string.transactions, Icons.Default.ReceiptLong),
        BottomItem("tools", R.string.explore, Icons.Default.GridView)
    )'''
screens = screens[:bottom_start] + bottom + screens[bottom_end:]

# Replace title bar banner and its actions, preserving the rest of the Scaffold.
screens = replace_once(
    screens,
    'Text(stringResource(R.string.demo_warning), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)',
    'Text(appText(settings.language, "Kişisel finans alanın", "Your personal finance space", "Dein persönlicher Finanzbereich"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)',
    "remove test banner"
)
actions = '''                actions = {
                    IconButton(onClick = { nav.navigate("qr-pay") { launchSingleTop = true } }) {
                        Icon(Icons.Default.QrCode2, contentDescription = appText(settings.language, "QR ile ödeme", "QR payments", "QR-Zahlung"))
                    }
                    IconButton(onClick = { nav.navigate("profile") { launchSingleTop = true } }) {
                        Icon(Icons.Default.Person, contentDescription = appText(settings.language, "Profil", "Profile", "Profil"))
                    }
                    IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.notifications))
                    }
                }'''
screens, action_count = re.subn(r'(?s)                actions = \{.*?\n                \}', actions, screens, count=1)
if action_count != 1:
    raise SystemExit(f"Could not replace top-bar actions using balanced indentation (count={action_count}).")


# The feature directory should be a polished grid, not the old long list.
tools_route_old = 'composable("tools") { RouteEntrance { ToolsScreen(settings) } }'
if tools_route_old in screens:
    screens = screens.replace(tools_route_old, 'composable("tools") { RouteEntrance { PremiumToolsScreen(settings, nav) } }', 1)
elif 'PremiumToolsScreen(settings, nav)' not in screens:
    raise SystemExit("Could not locate tools route.")
receive_route_old = 'composable("receive") { RouteEntrance { ReceiveScreen(vm, settings) } }'
if receive_route_old in screens:
    screens = screens.replace(receive_route_old, 'composable("receive") { RouteEntrance { QrPaymentScreen(vm, settings) } }\n            composable("qr-pay") { RouteEntrance { QrPaymentScreen(vm, settings) } }', 1)
elif 'composable("qr-pay")' not in screens:
    raise SystemExit("Could not locate QR destination insertion point.")
screens = screens.replace("DEMO MODU · GÜVENLİ DENEME", "KİŞİSEL HESAP · YEREL KAYIT")
screens = screens.replace("DEMO MODE · SAFE SANDBOX", "PERSONAL ACCOUNT · LOCAL LEDGER")
screens = screens.replace("DEMO-MODUS · SICHERE TESTUMGEBUNG", "PERSÖNLICHES KONTO · LOKALES KONTOBUCH")

section_pattern = re.compile(r'@Composable\nprivate fun SectionHeading\(title: String, modifier: Modifier = Modifier\) \{\n.*?\n\}', re.S)
section_replacement = '''@Composable
private fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.width(4.dp).height(23.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}'''
screens, changed_heading = section_pattern.subn(section_replacement, screens, count=1)
if changed_heading != 1:
    raise SystemExit(f"Could not update common section heading, count={changed_heading}")
screens = screens.replace("private val CardRadius = RoundedCornerShape(22.dp)", "private val CardRadius = RoundedCornerShape(25.dp)")
screens_file.write_text(screens, encoding="utf-8")

# Localize all formerly demo-centric labels without pretending this app is connected to a real bank.
translations = {
    "values/strings.xml": {
        "demo_warning": "Local records only · not connected to a bank",
        "demo_login": "Create my account",
        "reset_demo": "Clear local data",
        "reset_confirm": "This removes accounts, cards, goals and activity stored on this device. Continue?",
        "demo_user": "Personal account",
        "demo_pin": "Enter the PIN you created during setup",
        "wrong_pin": "Incorrect PIN. Try again.",
        "fictional_profile": "Personal profile · saved on this device",
        "about_body": "TUR BANK stores the profile and account records you create on this device. It is not connected to a financial institution, and QR or transfer actions do not move real money.",
        "profile": "Personal profile",
        "session_lock": "Lock the app when reopened",
        "export_csv": "Export activity as CSV",
        "app_version": "Version 1.4.0 · TUR BANK",
        "receive_demo": "QR payments",
        "qr_demo_info": "Scan a payment QR or create your own payment request.",
        "copy_demo_data": "Copy QR details",
        "share_demo_data": "Share payment request",
        "credit_demo_money": "Record incoming payment",
        "demo_sender": "Payment sender",
        "qr_alt": "Payment QR code",
        "manual_demo_note": "QR details help prepare a local payment record; they do not send money to a bank.",
        "receive_success": "Payment record added.",
        "transfer_success": "Payment record saved on this device. No real money was transferred.",
        "regenerate_reference": "New reference",
        "receive_demo_note": "A payment entry is recorded locally and does not move money outside this device.",
        "local_profile_note": "Your profile is stored on this device; no bank login is connected.",
        "ledger_notice": "Accounts, balances and activity you add are stored locally on this device.",
        "demo_fee": "Estimated fee (0.50%)",
        "demo_fee_label": "Estimated fee",
        "insights_demo_note": "Insights use activity saved on this device.",
        "bills_demo_note": "Bills are reminders in your local ledger; they are not bank payment instructions.",
        "biometric_fallback": "Use the PIN you created during setup if device biometrics are unavailable.",
        "biometric_prompt_title": "TUR BANK",
        "biometric_prompt_subtitle": "Unlock your local account",
        "msg_reset_done": "Local data cleared.",
        "msg_duplicate": "This payment reference has already been processed.",
        "msg_invalid_request": "The payment request is invalid.",
        "card_security_note": "These are local virtual-card controls. Real payment network access or card credentials are not created.",
        "no_bills": "No upcoming bills.",
        "sample_data": "Activity",
        "budget_currency_note": "Budgets use local transactions in the selected currency.",
    },
    "values-tr/strings.xml": {
        "demo_warning": "Yerel kayıtlar · banka bağlantısı yok",
        "demo_login": "Hesabımı oluştur",
        "reset_demo": "Yerel verileri sil",
        "reset_confirm": "Bu işlem cihazında saklanan hesapları, kartları, hedefleri ve hareketleri siler. Devam edilsin mi?",
        "demo_user": "Kişisel hesap",
        "demo_pin": "Kurulumda oluşturduğun PIN'i gir",
        "wrong_pin": "PIN yanlış. Tekrar dene.",
        "fictional_profile": "Kişisel profil · bu cihazda saklanır",
        "about_body": "TUR BANK, bu cihazda oluşturduğun profil ve hesap kayıtlarını saklar. Bir finans kuruluşuna bağlı değildir; QR veya transfer işlemleri gerçek para taşımaz.",
        "profile": "Kişisel profil",
        "session_lock": "Uygulama yeniden açılınca kilitle",
        "export_csv": "Hareketleri CSV olarak dışa aktar",
        "app_version": "Sürüm 1.4.0 · TUR BANK",
        "receive_demo": "QR ile ödeme",
        "qr_demo_info": "Ödeme QR'ını okut veya kendi ödeme talebini oluştur.",
        "copy_demo_data": "QR bilgilerini kopyala",
        "share_demo_data": "Ödeme talebini paylaş",
        "credit_demo_money": "Gelen ödemeyi kaydet",
        "demo_sender": "Ödeme göndereni",
        "qr_alt": "Ödeme QR kodu",
        "manual_demo_note": "QR bilgileri yerel ödeme kaydı hazırlamak içindir; bankaya para göndermez.",
        "receive_success": "Ödeme kaydı eklendi.",
        "transfer_success": "Ödeme bu cihaza kaydedildi. Gerçek para aktarılmadı.",
        "regenerate_reference": "Yeni referans oluştur",
        "receive_demo_note": "Ödeme kaydı cihazda saklanır; cihaz dışına para aktarılmaz.",
        "local_profile_note": "Profilin bu cihazda saklanır; banka girişi bağlı değildir.",
        "ledger_notice": "Eklediğin hesaplar, bakiyeler ve hareketler bu cihazda yerel olarak saklanır.",
        "demo_fee": "Tahmini ücret (%0,50)",
        "demo_fee_label": "Tahmini ücret",
        "insights_demo_note": "Analizler bu cihazda kayıtlı hareketleri kullanır.",
        "bills_demo_note": "Faturalar yerel defterindeki hatırlatıcılardır; bankaya ödeme talimatı değildir.",
        "biometric_fallback": "Cihaz biyometrisi kullanılamıyorsa kurulumda oluşturduğun PIN'i kullan.",
        "biometric_prompt_title": "TUR BANK",
        "biometric_prompt_subtitle": "Yerel hesabının kilidini aç",
        "msg_reset_done": "Yerel veriler silindi.",
        "msg_duplicate": "Bu ödeme referansı daha önce işlendi.",
        "msg_invalid_request": "Ödeme talebi geçersiz.",
        "card_security_note": "Bunlar yerel sanal kart kontrolleridir. Gerçek ödeme ağı bağlantısı veya kart bilgisi oluşturulmaz.",
        "no_bills": "Yaklaşan fatura yok.",
        "sample_data": "Hareketler",
        "budget_currency_note": "Bütçeler seçilen para birimindeki yerel hareketleri kullanır.",
    },
    "values-de/strings.xml": {
        "demo_warning": "Lokale Einträge · keine Bankverbindung",
        "demo_login": "Mein Konto erstellen",
        "reset_demo": "Lokale Daten löschen",
        "reset_confirm": "Dadurch werden Konten, Karten, Sparziele und Aktivitäten auf diesem Gerät gelöscht. Fortfahren?",
        "demo_user": "Persönliches Konto",
        "demo_pin": "Gib die bei der Einrichtung erstellte PIN ein",
        "wrong_pin": "Falsche PIN. Bitte erneut versuchen.",
        "fictional_profile": "Persönliches Profil · auf diesem Gerät gespeichert",
        "about_body": "TUR BANK speichert das Profil und die Kontoeinträge, die du auf diesem Gerät erstellst. Es besteht keine Verbindung zu einem Finanzinstitut; QR- und Überweisungsaktionen bewegen kein echtes Geld.",
        "profile": "Persönliches Profil",
        "session_lock": "App beim erneuten Öffnen sperren",
        "export_csv": "Aktivitäten als CSV exportieren",
        "app_version": "Version 1.4.0 · TUR BANK",
        "receive_demo": "QR-Zahlung",
        "qr_demo_info": "Zahlungs-QR scannen oder eine eigene Zahlungsanfrage erstellen.",
        "copy_demo_data": "QR-Daten kopieren",
        "share_demo_data": "Zahlungsanfrage teilen",
        "credit_demo_money": "Eingehende Zahlung erfassen",
        "demo_sender": "Zahlender",
        "qr_alt": "Zahlungs-QR-Code",
        "manual_demo_note": "QR-Daten bereiten einen lokalen Zahlungseintrag vor; sie überweisen kein Geld an eine Bank.",
        "receive_success": "Zahlungseintrag hinzugefügt.",
        "transfer_success": "Zahlung auf diesem Gerät gespeichert. Es wurde kein echtes Geld überwiesen.",
        "regenerate_reference": "Neue Referenz",
        "receive_demo_note": "Der Zahlungseintrag wird lokal gespeichert; Geld verlässt dieses Gerät nicht.",
        "local_profile_note": "Dein Profil wird auf diesem Gerät gespeichert; es ist keine Bankanmeldung verbunden.",
        "ledger_notice": "Konten, Salden und Aktivitäten, die du hinzufügst, werden lokal auf diesem Gerät gespeichert.",
        "demo_fee": "Geschätzte Gebühr (0,50 %)",
        "demo_fee_label": "Geschätzte Gebühr",
        "insights_demo_note": "Auswertungen verwenden lokal gespeicherte Aktivitäten.",
        "bills_demo_note": "Rechnungen sind Erinnerungen im lokalen Kontobuch, keine Zahlungsaufträge an eine Bank.",
        "biometric_fallback": "Falls Gerätebiometrie nicht verfügbar ist, verwende die bei der Einrichtung erstellte PIN.",
        "biometric_prompt_title": "TUR BANK",
        "biometric_prompt_subtitle": "Lokales Konto entsperren",
        "msg_reset_done": "Lokale Daten gelöscht.",
        "msg_duplicate": "Diese Zahlungsreferenz wurde bereits verarbeitet.",
        "msg_invalid_request": "Die Zahlungsanfrage ist ungültig.",
        "card_security_note": "Dies sind lokale virtuelle Kartensteuerungen. Es werden keine echte Zahlungsnetzverbindung oder Kartendaten erstellt.",
        "no_bills": "Keine anstehenden Rechnungen.",
        "sample_data": "Aktivitäten",
        "budget_currency_note": "Budgets verwenden lokal gespeicherte Buchungen in der ausgewählten Währung.",
    }
}
string_re = re.compile(r'(<string\s+name="([^"]+)"[^>]*>)(.*?)(</string>)', re.S)
for rel, mapping in translations.items():
    path = app / "src/main/res" / rel
    if not path.is_file():
        raise SystemExit(f"Missing string resource file: {path}")
    content = path.read_text(encoding="utf-8")
    seen = set()
    def change_string(match):
        name = match.group(2)
        if name not in mapping:
            return match.group(0)
        seen.add(name)
        return match.group(1) + mapping[name].replace("'", "\\'") + match.group(4)
    content = string_re.sub(change_string, content)
    required = {"demo_warning", "reset_demo", "demo_user", "about_body", "receive_demo", "profile", "ledger_notice", "app_version"}
    missing = required - seen
    if missing:
        raise SystemExit(f"Required strings missing in {rel}: {sorted(missing)}")
    path.write_text(content, encoding="utf-8")

print("Applied personalized onboarding, seed cleanup, PIN-based local lock, QR payment scanner and redesigned secondary navigation.")
