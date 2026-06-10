# Night Veil Architecture

## Scope

This document records the current, verified application structure of Night Veil.
It describes what can be confirmed from source code, manifest entries, Gradle files,
and local verification commands. It does not infer historical decision rationale.

## Project Shape

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

## Runtime Components

### MainActivity

`MainActivity` is the home screen entry point.

Responsibilities confirmed from code:

- Reads and writes the persisted opacity/running state
- Requests notification permission when needed
- Checks overlay permission state
- Starts and stops `FloatingService`
- Updates the home screen UI from current state
- Receives overlay state change broadcasts and refreshes the UI

### FloatingService

`FloatingService` is the foreground service that owns the overlay lifecycle.

Responsibilities confirmed from code:

- Creates and attaches the overlay view through `FloatingWindowHelper`
- Starts the foreground notification
- Applies opacity updates
- Hides and destroys the overlay when stopping
- Broadcasts overlay state changes back to the app process

### ShortcutProxyActivity

`ShortcutProxyActivity` handles launcher shortcut actions.

Responsibilities confirmed from code:

- Resolves start/stop shortcut intents
- Requests notification permission for shortcut-start flow when required
- Starts or stops `FloatingService`

## Package Structure

### `app/src/main/java/com/nightveil/app`

- Activity and service entry points
- UI/service orchestration

### `app/src/main/java/com/nightveil/app/model`

- Pure logic and state definitions
- Opacity rules
- Home screen state resolution
- Notification permission decision logic
- Overlay animation parameters
- Service and shortcut action identifiers
- Overlay state broadcast contract

### `app/src/main/java/com/nightveil/app/data`

- `MaskPreferences` wraps `SharedPreferences`
- Persisted keys confirmed from code:
  - last opacity
  - running state
  - permission-request marker

### `app/src/main/java/com/nightveil/app/overlay`

- Overlay rendering and animation control
- Circular reveal or fade-based show/hide behavior

### `app/src/main/java/com/nightveil/app/notification`

- Foreground notification creation
- Notification action wiring for opacity decrease, increase, and stop

### `app/src/main/java/com/nightveil/app/utils`

- Overlay window parameter creation
- Edge-to-edge inset merging helpers

## Permissions and Foreground Service

The manifest currently declares these permissions:

- `android.permission.SYSTEM_ALERT_WINDOW`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `android.permission.POST_NOTIFICATIONS`

The service is declared with foreground service type `specialUse`.

## Verified User Capabilities

Verified from README, manifest, resources, and source code:

- Full-screen dimming veil overlay
- Adjustable opacity
- Notification actions for increase, decrease, and close
- Persistence of last used opacity
- Launcher shortcut support
- Simplified Chinese resource localization

## Boundaries Of This Document

The following are intentionally not recorded here:

- Signing secrets or keystore contents
- Upload API keys
- Historical reasons for choosing the current architecture
- Unverified assumptions about future architecture direction
