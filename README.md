# FocusForest

FocusForest is an Android productivity application built with Jetpack Compose and Material Design 3 that rewards you with procedurally generated growing trees while you keep your phone stationary and stay focused.

## Features
- **Phone Placement Baseline Calibration**: Uses device gravity vectors, accelerometer, and orientation sensors to lock your phone's focus position.
- **Hysteresis & Motion Guard**: Distinguishes minor desk vibrations from actual pickups or movements.
- **Procedural Tree Growth Canvas**: Custom procedural rendering of saplings, foliage, blossoms, and fallen leaves based on focus duration.
- **Forest Analytics & History**: Track completed sessions, tree species, and focus streaks stored locally via Room.

## Exporting & Building the APK

### 1. Direct Download from AI Studio
You can download the APK or export the project directly from Google AI Studio:
1. Open the project in Google AI Studio.
2. Click on the project **Settings** menu (top-right gear or export icon).
3. Select **Download APK / Generate APK** or **Export as ZIP**.

### 2. GitHub Actions Automated Build
A GitHub Actions workflow is provided at `.github/workflows/build-apk.yml`.
When you push or sync this repository to GitHub:
1. Push to `main` or trigger manually via **Actions > Build Android APK > Run workflow**.
2. Once the workflow completes, download the `focusforest-debug-apk` artifact directly from the GitHub Actions run page.
