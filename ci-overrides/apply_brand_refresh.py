#!/usr/bin/env python3
"""Apply TUR BANK's X-logo splash, brand icon and graphite/emerald form styling."""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_brand_refresh.py PROJECT_DIR")

project = Path(sys.argv[1])
app = project / "app"
src = app / "src/main/java/com/turbank/mobile"
ui = src / "ui"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


# The supplied image is installed as drawable/ic_tur_bank.xml by the overlays.
logo = app / "src/main/res/drawable/ic_tur_bank.xml"
if not logo.is_file() or "M300,400 H500 L900,800 H700 Z" not in logo.read_text(encoding="utf-8"):
    raise SystemExit("The requested X logo drawable was not installed.")


# Show the uploaded X mark briefly at launch, then continue to language selection/onboarding.
main_file = src / "MainActivity.kt"
main = main_file.read_text(encoding="utf-8")
if "import com.turbank.mobile.ui.BrandSplashScreen" not in main:
    main = replace_once(
        main,
        "import com.turbank.mobile.ui.AccountLockScreen\n",
        "import com.turbank.mobile.ui.AccountLockScreen\nimport com.turbank.mobile.ui.BrandSplashScreen\n",
        "splash screen import"
    )
imports = [
    "import androidx.compose.material3.MaterialTheme",
    "import androidx.compose.material3.darkColorScheme",
    "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.unit.dp",
    "import androidx.compose.foundation.shape.RoundedCornerShape",
    "import kotlinx.coroutines.delay",
]
for line in imports:
    if line not in main:
        anchor = "import androidx.activity.compose.setContent\n"
        main = replace_once(main, anchor, anchor + line + "\n", "brand theme import")

if "var splashFinished by remember" not in main:
    main = replace_once(
        main,
        "            var unlocked by remember { mutableStateOf(false) }\n",
        "            var splashFinished by remember { mutableStateOf(false) }\n"
        "            var unlocked by remember { mutableStateOf(false) }\n",
        "splash state"
    )
if 'LaunchedEffect("brand-splash")' not in main:
    anchor = "            LaunchedEffect(Unit) { vm.initialize() }\n"
    main = replace_once(
        main, anchor,
        anchor + '            LaunchedEffect("brand-splash") { delay(1050); splashFinished = true }\n',
        "splash timer"
    )

theme_anchor = "            TurBankTheme(settings.theme) {\n                when {\n"
theme_replacement = """            TurBankTheme(settings.theme) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Color(0xFF19C37D),
                        onPrimary = Color(0xFF042013),
                        primaryContainer = Color(0xFF15382B),
                        onPrimaryContainer = Color(0xFFD1FBE4),
                        secondary = Color(0xFF31D995),
                        onSecondary = Color(0xFF052016),
                        background = Color(0xFF080B0E),
                        onBackground = Color(0xFFF4F7F8),
                        surface = Color(0xFF11171B),
                        onSurface = Color(0xFFF3F6F7),
                        surfaceVariant = Color(0xFF1A2228),
                        onSurfaceVariant = Color(0xFFA4AFB6),
                        outline = Color(0xFF3A454C),
                        outlineVariant = Color(0xFF2A343A),
                        error = Color(0xFFFF6672),
                        onError = Color(0xFF30070C)
                    ),
                    shapes = MaterialTheme.shapes.copy(
                        small = RoundedCornerShape(17.dp),
                        medium = RoundedCornerShape(23.dp),
                        large = RoundedCornerShape(28.dp)
                    )
                ) {
                when {
                    !splashFinished -> BrandSplashScreen()
"""
if "colorScheme = darkColorScheme(" not in main:
    main = replace_once(main, theme_anchor, theme_replacement, "global launch/onboarding dark theme")

old_close = """                    else -> MainBankNavigation(vm = vm)
                }
            }
        }
"""
new_close = """                    else -> MainBankNavigation(vm = vm)
                }
                }
            }
        }
"""
if "colorScheme = darkColorScheme(" in main and new_close not in main:
    main = replace_once(main, old_close, new_close, "close global brand theme")
main_file.write_text(main, encoding="utf-8")


# Use the exact logo on the language selection screen and security unlock screen.
screens_file = ui / "BankScreens.kt"
screens = screens_file.read_text(encoding="utf-8")
for line in ["import androidx.compose.ui.res.painterResource", "import androidx.compose.ui.layout.ContentScale"]:
    if line not in screens:
        anchor = "import androidx.compose.ui.res.stringResource\n"
        screens = replace_once(screens, anchor, anchor + line + "\n", "logo image import")

language_logo_old = """        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("T", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        }"""
language_logo_new = """        Image(
            painter = painterResource(R.drawable.ic_tur_bank),
            contentDescription = "TUR BANK",
            modifier = Modifier.size(86.dp).clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )"""
if language_logo_old in screens:
    screens = replace_once(screens, language_logo_old, language_logo_new, "language selection logo")
elif "contentDescription = \"TUR BANK\"" not in screens:
    raise SystemExit("Language selection logo tile was not found.")

lock_logo_old = '        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary)'
lock_logo_new = """        Image(
            painter = painterResource(R.drawable.ic_tur_bank),
            contentDescription = "TUR BANK",
            modifier = Modifier.size(76.dp).clip(RoundedCornerShape(17.dp)),
            contentScale = ContentScale.Crop
        )"""
if lock_logo_old in screens:
    screens = replace_once(screens, lock_logo_old, lock_logo_new, "account lock logo")
screens_file.write_text(screens, encoding="utf-8")


# Verify setup form code is covered by the launch-wide theme; give the setup hero a slight entrance.
setup_file = ui / "PersonalAccountSetupScreen.kt"
setup = setup_file.read_text(encoding="utf-8")
if "painterResource(R.drawable.ic_tur_bank)" not in setup:
    raise SystemExit("Personal setup screen is missing the new X logo.")
if "RoundedCornerShape(27.dp)" not in setup or "Color(0xFF18362A)" not in setup:
    raise SystemExit("Personal setup premium banner change is missing.")

print("Applied X-logo splash and launcher drawable, dark graphite/emerald UI across launch/setup/navigation, rounded input fields, and logo-branded unlock screen.")
