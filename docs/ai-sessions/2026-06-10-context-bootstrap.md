# 2026-06-10 Context Bootstrap

## Purpose

This note captures the verified project context established during the 2026-06-10
AI coding bootstrap session.

## Verified Project Context

- Project type: single-module Android application
- Root project name: `Night Veil`
- Main module: `app`
- Package name: `com.nightveil.app`
- UI stack: Android Views/XML with ViewBinding
- Build stack:
  - Gradle Groovy DSL
  - Android Gradle Plugin `8.13.2`
  - Kotlin Android plugin `2.3.21`
  - Gradle Wrapper `8.13`

## Verified Main Structure

- `MainActivity`: home screen state and permission/service orchestration
- `FloatingService`: foreground overlay service
- `ShortcutProxyActivity`: shortcut intent handling
- `data`: local `SharedPreferences` wrapper
- `model`: pure logic/state helpers
- `notification`: foreground notification factory
- `overlay`: overlay animation/rendering control
- `utils`: overlay and edge-to-edge helpers

## Verified Build And Test Commands

The following commands were executed successfully during the session:

```powershell
./gradlew.bat --offline testDebugUnitTest
./gradlew.bat --offline assembleDebug
```

## Verified Capabilities

- screen dimming overlay
- adjustable opacity
- foreground notification controls
- launcher shortcuts
- persisted last opacity
- Simplified Chinese resources

## Missing Long-Term Docs Identified In Session

- `docs/architecture.md`
- `docs/release.md`
- `docs/testing.md`
- CI documentation or repository CI configuration
- compatibility / device behavior notes

## Explicit Non-Goals

This note does not record:

- signing secrets
- keystore contents
- API keys
- temporary git state
- one-off build artifacts
- unverified historical rationale
