# Night Veil

Night Veil is an Android screen-dimming overlay app built for night eye comfort.

## What It Does

- Adds a full-screen dimming veil above the system UI
- Lets you adjust veil intensity from the home screen
- Keeps quick controls in the notification shade
- Remembers the last used intensity
- Uses a center-out reveal animation when the veil starts

## Language Support

- English is the default language
- Simplified Chinese is included through Android resource localization

## Tech Notes

- Minimum SDK: 21
- Target SDK: 36
- Foreground service uses Android 16 compatible `specialUse` configuration
- Main package name: `com.nightveil.app`

## Build

```powershell
.\gradlew.bat --offline assembleDebug
.\gradlew.bat --offline testDebugUnitTest
```
