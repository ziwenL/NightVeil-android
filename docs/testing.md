# Night Veil Testing Guide

## Scope

This document records the current, verified testing entry points and coverage shape.
It only includes commands and structure confirmed during the current repository review.

## Verified Commands

The following commands were executed successfully in the local repository during the
context bootstrap session:

```powershell
./gradlew.bat --offline testDebugUnitTest
./gradlew.bat --offline assembleDebug
```

## Current Test Layout

### Unit tests

Current unit tests live under:

```text
app/src/test/java/
```

Verified areas covered by unit tests:

- `model`
  - home UI state
  - notification permission logic
  - foreground service constants
  - overlay opacity rules
  - overlay animation math
  - overlay state signal
  - shortcut actions
- `notification`
  - notification action constants and ordering
- `utils`
  - floating window layout config
  - edge-to-edge inset helper
- `data`
  - shared preference key constants

### Instrumentation tests

Current instrumentation tests live under:

```text
app/src/androidTest/java/
```

Only a basic package-name instrumentation test was confirmed during this review.

## Current Testing Style

The codebase currently favors extracting small pieces of logic into `model` and `utils`
classes so that behavior can be covered with fast unit tests.

Examples confirmed from source:

- home screen state resolution is kept in `MaskHomeUiModel`
- opacity range and stepping are kept in `MaskOverlayModel`
- overlay window configuration is validated through helper tests

## What Was Not Verified In This Session

- Instrumentation test execution on device or emulator
- UI interaction tests beyond the sample instrumentation test
- Release build test flow
- Automated CI test execution

## Recommended Usage

- Run `./gradlew.bat --offline testDebugUnitTest` before committing logic changes
- Run `./gradlew.bat --offline assembleDebug` before submitting changes that affect build,
  resources, manifest declarations, or Gradle configuration

## Exclusions

This document does not record temporary logs, flaky local environment details,
or unverified assumptions about device coverage.
