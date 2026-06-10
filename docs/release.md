# Night Veil Release And Distribution Notes

## Scope

This document records the currently verified local build and distribution flow.
It excludes secrets and any release steps that were not confirmed from committed
repository code or command output.

## Verified Build Configuration

- `minSdk`: `21`
- `targetSdk`: `36`
- `compileSdk`: `36`
- Release build type exists
- Debug build type exists
- ProGuard configuration file is referenced for release builds

## Verified Local Build Commands

README currently documents these local commands:

```powershell
./gradlew.bat --offline assembleDebug
./gradlew.bat --offline testDebugUnitTest
```

Both commands were executed successfully during the current repository review.

## Security Boundaries

This document must not contain:

- keystore files
- passwords
- API keys
- token values
- unpublished artifact URLs that embed credentials

## What Was Not Verified In This Session

- release signing procedure
- artifact upload procedure
- CI-based release flow
- store submission flow

## Current Gaps

The repository does not currently provide a verified, long-form release procedure that covers:

- who prepares signing inputs
- how release signing is provisioned on a new machine
- how upload credentials are managed
- how release artifacts are approved
