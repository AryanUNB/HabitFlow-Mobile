# HabitFlow

CS2063, Team 11 — Kotlin/XML Android habit tracker. Minimum Android 10 (API 29).

## Build and run

Open this directory in Android Studio and sync Gradle. Use the existing Gradle daemon JVM configuration (JDK 25); the wrapper and Android Gradle plugin are already configured. Install SDK 37 when prompted. `local.properties` contains your own SDK location and is ignored by Git.

Run `./gradlew assembleDebug testDebugUnitTest lintDebug` to compile, test goal calculations, and check Android lint. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Select your physical Android phone in Android Studio and press Run. Enable USB debugging and authorize the computer on the phone first.

## Code guide

- `MainActivity`: four-second animated logo splash, welcome screen, guest navigation, dashboard, completion and undo. Google sign-in is marked coming soon and is not integrated.
- `FloatingHabitsView`: floating habit emojis on the splash screen.
- `AddHabitActivity`: validates the name and saves a target of 1–7 days.
- `StatsActivity` and `WeeklyChartView`: Monday–Sunday statistics and native Canvas charts.
- `Habit`: data and pure weekly goal calculations.
- `HabitRepository`: local JSON in private SharedPreferences. A set of ISO dates prevents duplicate daily completions. Saves are committed before reporting success. Suitable for the small prototype dataset; larger datasets should use Room and background I/O.
- `WindowInsetsHelper`: keeps content clear of system bars and the keyboard.
- `res/layout`: XML screens and habit rows.

Existing Compose starter theme files and dependencies are retained but the prototype screens use native Views and XML. Main features need no internet permission, account, or server.

Weekly goal percentage caps each habit at its target, so exceeding one goal does not compensate for another unfinished goal. The dashboard shows today's completion independently of weekly goals. New habits have no seeded history; statistics initially reflect only actual completions.

## Physical-device demo checklist (not yet verified)

1. Open the app; check the single animated splash, then tap Continue as guest on the welcome screen. Check the empty dashboard and current date.
2. Tap Add habit, try a blank name, then save Read Books with target 5 and category Learning.
3. Confirm the habit appears; mark it completed and check today's count.
4. Open statistics: expect one completion and 20% weekly goal progress.
5. Return; undo completion and confirm both screens update.
6. Complete it again, force-stop the app, reopen, and confirm the habit and completion persist.
7. Rotate the phone on each screen and check navigation, keyboard, and system-bar spacing.
8. Turn on airplane mode and repeat creating and completing a second habit.

The prototype has been compiled and unit tested, but no connected physical phone was available for interaction, visual, or restart-persistence verification. Future features (reminders, steps, gamification, focus timer) are outside this implementation.
