# Discipline 30 — Android habit tracker

A dark, gold-accented 30-day habit tracker based on the supplied chart.

## Features
- 30 daily checkboxes for every habit, saved locally on the device.
- Edit, add, and delete habits.
- Overall completion percentage and completed check-in count.
- Daily reminder time per habit, delivered as Android notifications.
- Reminder alarms are restored after device restart.
- Editable reflection / notes area.
- Horizontal scrolling for the 30-day grid on smaller phones.

## Build the APK

### Android Studio
1. Install Android Studio (with Android SDK Platform 35) and JDK 17.
2. Open this `Discipline30` folder as an existing project.
3. Let Gradle sync, then choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Build automatically with GitHub Actions
1. Create a GitHub repository and upload the contents of this folder.
2. Open **Actions → Build Android APK → Run workflow** (or push to `main`).
3. Download the `Discipline30-debug-apk` artifact from the completed workflow.

## Reminder notes
- Allow notifications when Android asks.
- On Android 12+, allow exact alarms if the app opens the exact-alarm settings page. Some manufacturers also require allowing background activity / auto-start for reliable reminders.
- Reminders are daily local-time notifications. They are not cloud-synced.

## App identity
- Package: `com.example.discipline30`
- Version: `1.0`
- Minimum Android: 6.0 (API 23)
