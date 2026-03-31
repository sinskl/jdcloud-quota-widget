# Repository Guidelines

## Project Structure & Module Organization
This repository is a single-module Android app. Core code lives under `app/src/main/java/com/yi/jdcloud/` and is split by responsibility: `data/` for API, cookies, and persistence; `domain/` for models; `di/` for Hilt wiring; `ui/login` and `ui/settings` for Compose screens; `worker/` for scheduled refresh; and `widget/` for the Glance widget. Android resources live in `app/src/main/res/`. Build and release configuration stays in `app/build.gradle.kts`, the root Gradle files, and `.github/workflows/`.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repo root:

- `./gradlew assembleDebug` builds the debug APK used by CI.
- `./gradlew build` runs the full local build, including checks.
- `./gradlew lint` runs Android lint for Kotlin, Compose, and manifest/resource issues.
- `./gradlew testDebugUnitTest` runs JVM unit tests when present.

CI currently builds `app/build/outputs/apk/debug/app-debug.apk` on pushes and PRs to `main`.

## Coding Style & Naming Conventions
Write Kotlin with 4-space indentation and standard Android/Kotlin naming: `PascalCase` for classes and composables, `camelCase` for methods and properties, `UPPER_SNAKE_CASE` for constants. Keep packages feature-oriented and consistent with the existing layout. Prefer small repository/service classes, explicit model names such as `QuotaModel`, and descriptive screen names such as `LoginScreen`. Follow existing Compose and Hilt patterns before introducing new abstractions.

## Testing Guidelines
There are no committed test source sets yet. Add JVM tests under `app/src/test/` and instrumentation or UI tests under `app/src/androidTest/`. Name test files after the target, for example `QuotaRepositoryTest.kt`. For changes touching login flow, background refresh, or widget rendering, include at least one automated test where practical and always run `./gradlew lint build` before opening a PR.

## Commit & Pull Request Guidelines
Recent history uses concise Conventional Commit-style subjects such as `fix: split login into separate Activity`. Follow the same pattern: `feat:`, `fix:`, `refactor:` plus a short imperative summary. PRs should explain the user-visible change, list validation commands, link any related issue, and include screenshots for Compose or widget UI changes.

## Security & Configuration Tips
Do not commit live JD Cloud cookies, tokens, or personal account data. Treat WebView login handling, stored preferences, and API headers as sensitive paths and review them carefully during changes.
