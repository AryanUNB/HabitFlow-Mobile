# HabitFlow

**Small habits. Lasting growth.**

HabitFlow is a native Android application that helps users build consistent routines, set weekly goals, and track their progress. The prototype stores habits and completion history locally, so its core functionality works offline.

Developed for **CS2063 — Introduction to Mobile Application Development** at the **University of New Brunswick, Fredericton**, Fall 2026.

## Team 11

- **Hadi Akbar**
- **Aryan Bansal**
- **Daniel Bradbury**

**Professor:** Howard Powell

## Features

- Animated four-second splash screen with the HabitFlow logo and floating habit emojis.
- Minimal welcome screen with guest access.
- Home dashboard showing the current date, habits, and daily completion progress.
- Custom habits with a name, weekly target of 1–7 days, and an optional category.
- Daily completion tracking with undo and one completion per habit per date.
- Habit deletion with confirmation before removing the habit and its history.
- Local storage for habits and completion history.
- Weekly statistics with completion counts, a bar chart, and a circular goal indicator.

**Google sign-in is planned.** The welcome screen displays the option as unavailable; authentication is not integrated.

## Technology

| Component | Implementation |
| --- | --- |
| Language | Kotlin |
| Interface | XML layouts and native Android Views |
| Storage | Private SharedPreferences with JSON |
| Charts | Native Canvas drawing |
| Build system | Gradle Kotlin DSL with the included wrapper |
| Minimum Android version | Android 10 (API 29) |
| Compile and target SDK | API 37 |

The project retains Compose dependencies and theme files from the original Android Studio template. The prototype screens use XML and native Views.

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/AryanUNB/HabitFlow-Mobile.git
   cd HabitFlow-Mobile
   ```

2. Open the project directory in Android Studio.
3. Install Android SDK 37 through SDK Manager if needed.
4. Sync Gradle using the included wrapper and the configured JDK 25 daemon toolchain.
5. Connect a physical Android device running Android 10 or newer. Enable USB debugging and authorize the computer.
6. Select the device in Android Studio and run the `app` configuration.

SDK paths belong in your local `local.properties` file, which is excluded from version control. Build outputs and IDE caches are also excluded.

### Build and validation

On macOS or Linux:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Application flow

```text
Animated splash → Welcome → Continue as guest → Dashboard
                                                  ├── Add habit
                                                  ├── Complete or undo a habit
                                                  └── Weekly statistics
```

Guest habits remain on the device between app launches. Clearing application data or uninstalling the app removes its local storage; there is no cloud synchronization.

### Progress calculations

Weeks run from **Monday through Sunday**. Each habit contributes up to its own weekly target toward the overall goal percentage. Exceeding one habit’s target does not compensate for an unfinished goal on another habit.

The dashboard tracks daily completion separately from weekly goals. Statistics reflect recorded completions; the app does not generate sample history.

## Code organization

Source files are in `app/src/main/java/ca/unb/mobiledev/habitflow/`.

| File or directory | Responsibility |
| --- | --- |
| `MainActivity.kt` | Splash and welcome flow, guest navigation, dashboard, completion and undo |
| `AddHabitActivity.kt` | Habit creation and input validation |
| `Habit.kt` | Habit model and weekly goal calculations |
| `HabitRepository.kt` | Reading and saving local habit data |
| `StatsActivity.kt` | Weekly statistics screen |
| `WeeklyChartView.kt` | Bar chart and circular progress indicator |
| `FloatingHabitsView.kt` | Decorative splash animations |
| `WindowInsetsHelper.kt` | System bar and keyboard spacing |
| `app/src/main/res/layout/` | XML screen layouts and habit rows |

SharedPreferences provides a simple persistence approach for the small prototype dataset. A larger application would benefit from a database such as Room and background storage operations.

## Testing and demonstration

The debug build, local unit tests, and Android lint have passed. Unit tests cover week boundaries, goal percentage limits, and unique completion dates with undo. Physical-device interactions, visual layout, and persistence after restarting still require manual verification.

### Physical-device checklist

1. Launch HabitFlow and confirm a single animated splash followed by the welcome screen.
2. Select **Continue as guest** and check the dashboard and current date.
3. Select **Add habit** and verify that a blank name is rejected.
4. Create **Read Books** with a weekly target of **5** and category **Learning**.
5. Complete the habit and confirm that today’s progress updates.
6. Open weekly statistics. With only this habit and one completion, expect **20%** goal progress.
7. Undo the completion and confirm that the dashboard and statistics update.
8. Complete the habit again, force-stop the app, reopen it, and confirm that the saved habit and completion remain.
9. Rotate the device and check screen state, navigation, keyboard spacing, and system bars.
10. Enable airplane mode and confirm that creating and completing habits still works.

The preliminary course demonstration must use a **physical Android device**.

## Planned improvements

- Google sign-in
- Predefined habits
- Reminders and notifications
- Step tracking
- Focus timer
- Gamification and expanded analytics

These features are outside the current prototype implementation.
