# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.0] - 2026-10-16

### Added

- Sign-in and registration screens (Figma "Iniciar Sesión" and "Registro") with errors next to each field; Google sign-in visible but disabled until Sprint 11
- The app starts according to the session: sign-in when signed out or expired (keeping local data), Home when signed in
- Settings: profile card (`GET /me`) and sign-out with a confirmation dialog (REQ-AUT-06); the header avatar shows the user's initial
- `AppTextField` component (label, 48 dp field, focus and error states, password toggle, autofill hints)
- Tests: sign-in/registration/settings view models, UI validation, and light/dark screenshots of the new screens
- Session layer: `SessionStore` (typed DataStore encrypted with an AES-256-GCM key in Android Keystore) and `DeviceIdProvider`
- `AuthApi`, `AuthInterceptor` (bearer token) and `TokenAuthenticator` (one shared refresh on 401, then retry; a rejected refresh marks the session expired without deleting local data)
- `AuthRepositoryImpl` mapping server problems (RFC 9457) to `AuthFailure`; use cases `RegisterUser`, `Login`, `Logout`, `ObserveSession` with form validation
- Tests: `TokenAuthenticatorTest` and `AuthRepositoryImplTest` (MockWebServer), `AuthUseCasesTest`, instrumented `SessionStoreTest` (run by hand)

### Changed

- Dependabot groups minor/patch Gradle bumps and all Actions bumps
- compileSdk 36.1 -> 37 (needed by current AndroidX and OkHttp 5); targetSdk stays 36
- Kotlin 2.2.10 -> 2.4.21 together with Hilt 2.57.1 -> 2.60.1 and KSP 2.3.4 -> 2.3.12, all in the version catalog

## [0.2.0] - 2026-10-16

### Added

- Build types `debug` (`.debug`, local server), `staging` (`.staging`, staging server) and `release`, with `BuildConfig.API_BASE_URL` and a visible name per build type ("Coffer", "Coffer staging", "Coffer debug")
- Network security config: cleartext only to `10.0.2.2` in `debug`; HTTPS everywhere else
- `NetworkModule` (Retrofit 3 + OkHttp + kotlinx.serialization), `HealthApi`, `DispatchersModule` and the `Clock` port with `SystemClock`
- Konsist architecture rules (`domain` free of Android, `data` and floating point; `ui` does not import `data`)
- GitHub Actions: CI (`lint`, `ktlintCheck`, `testDebugUnitTest`, `assembleStaging` with the APK as artifact) and signed release on `v*` tags with checksums; Dependabot for Gradle and Actions
- ktlint
- Design system theme from `design-system.md` v2.1: light and dark tokens, typography (400/500), shapes, no shadows
- Base components: `MoneyText` (COP format), buttons, `AppCard`, chips, `SegmentedSelector`, `AppBottomSheet`, `ConfirmDialog`, `EmptyState`, `SyncBadge`, header with avatar
- 4-tab navigation (Inicio, Historial, Reportes, Cuentas) with type-safe routes; avatar opens Settings
- Settings: app version and server status with retry
- Coffer launcher icon (adaptive, with themed icon) and mascot images synced from coffer-docs (`scripts/sync-brand-assets.sh`)
- Robolectric component tests and Roborazzi screenshots in light and dark, verified in CI

### Changed

- minSdk 31 (Android 12) and no core library desugaring
- Version `0.2.0` (`versionCode` 200)
- Gson replaced by kotlinx.serialization; MVP amounts are read as strings, as the server now sends them
- MVP code moved to the `legacy` package; application class renamed to `FinanceApp`
- The server URL is no longer hardcoded: each build type points to its environment
- `CLAUDE.md`: sibling repositories referenced by relative path (`../docs/`) instead of an absolute WSL path

### Removed

- Template example tests
- MVP screens, view model, use cases and network code; only its Room schema remains for the Sprint 3 migration

## [0.1.0] - 2026-05-08
### Added

- Initial implementation of the MVVM (Model-View-ViewModel) and Clean Architecture
- Home screen displaying the calculated current balance
- List of transactions sorted in descending order by date
- Screen for recording new transactions (income and expenses)
- Screen navigation using Navigation Compose
- Local transaction persistence using Room
- Synchronization of pending local transactions with REST API
- Visual indicator of server connection status (green/yellow)
- Automatic health check upon app launch
- Calculation of total balance by adding income and subtracting expenses
- Dependency injection with Hilt
- MVVM + Clean Architecture with separate use cases
- Unit test for `getTotalAmount` with MockK
