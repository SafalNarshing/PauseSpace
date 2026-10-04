# PauseSpace
A breath of room between you and the feed.

PauseSpace puts a short breathing pause in front of the apps you choose. When you open one, you breathe first, then decide: close it, open it with a timer, or open it with no limit.

The design lives in `PauseSpace — mindful app opener.html` (8 screens, light and dark). The Android app is in `android/`.

## Android app

Native Kotlin + Jetpack Compose, Android 8.0+ (API 26).

| Screen | File |
|---|---|
| Setup & permissions | `ui/screens/SetupScreen.kt` |
| Today dashboard | `ui/screens/TodayScreen.kt` |
| Choose apps | `ui/screens/AppsScreen.kt` |
| Per-app rules | `ui/screens/RulesScreen.kt` |
| Ritual (breathing time) | `ui/screens/RitualScreen.kt` |
| Words on screen | `ui/screens/WordsScreen.kt` |
| Breathing overlay → reflect → done | `overlay/InterventionUi.kt` |

How it works:

- **`service/PauseAccessibilityService`** notices when a paused app's activity comes to the front (package and class name only, never screen content) and shows the overlay.
- **`overlay/OverlayController`** hosts the Compose overlay in its own window above the app.
- **`data/`** stores settings (`Store`, SharedPreferences JSON), a log of every pause and its outcome (`InterventionLog`, SQLite), and reads screen time from `UsageStatsManager` (`Usage`). Nothing leaves the phone.
- **`data/Rules`** holds the pure decision logic (schedules, growing rounds, skips, opens), covered by `RulesTest`.

### Build & run

```sh
cd android
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew installDebug           # install on a connected device/emulator
```

Or open `android/` in Android Studio. On first launch, allow the three permissions (Usage access, Display over apps, Accessibility).

For quick testing on an emulator, the permissions can be granted from adb:

```sh
adb shell appops set com.pausespace.app GET_USAGE_STATS allow
adb shell appops set com.pausespace.app SYSTEM_ALERT_WINDOW allow
adb shell settings put secure enabled_accessibility_services com.pausespace.app/com.pausespace.app.service.PauseAccessibilityService
```
