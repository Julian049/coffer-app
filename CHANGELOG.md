# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Encrypted local database `finance.db` (Room + SQLCipher, ADR-0008) with settings, accounts, categories, tags, transactions and sync metadata; schema exported to `app/schemas`
- `DatabaseKeyProvider`: random 256-bit passphrase stored wrapped by its own Android Keystore key
- `LegacyDatabaseMigrator`: one-time move of the MVP `finance_db` into `finance.db` with the server's V6 rules and fixed UUIDs; every row starts pending upload
- Ledger domain models (`Account`, `Category`, `Tag`, `Transaction`, `Settings`) and read ports
- Tests: MVP migration, DAOs and an instrumented encryption test (run by hand)
- Background sync (ADR-0006): `SyncApi`, `SyncTables` (entity ↔ change codec), `ChangeApplier` (client last-write-wins, cursor saved per page), `SyncEngine` (push batches of 200, pull pages of 500, refuses another account's database), `SyncWorker` (retries on network, 5xx and 429) and `SyncSchedulerImpl` (on change, every 15 min, initial download)
- `BaseCatalogSeeder`: base categories (detailed-design §6.9.10) and default settings on a new account
- Tests: `SyncEngineTest` (MockWebServer) and `ChangeApplierTest`
- "Descargando tus datos" screen: an active session whose first download has not finished waits there, with progress per data type, a waiting-for-network notice and a retry after a permanent failure
- Sync badge in Home and Settings ("Sincronizado · hace N min", "N pendientes", "Sincronizando…", "Sin conexión") from pending rows, running work, connectivity and the last sync time
- Read-only History (newest movements grouped by day) and Accounts (name, type, initial balance) to see the synced data until Sprints 4 and 5
- Every start with a session and a finished download requests a sync and keeps the 15-minute periodic work
- Tests: `InitialDownloadViewModelTest`, `SessionViewModelTest`, `HistoryViewModelTest`, `SyncedLabelTest` and new screenshots

### Changed

- Android backup and device-to-device transfer are disabled (ADR-0008: the cloud is the backup)
- Signing out also cancels the background sync
- Sync rounds never overlap: the one-off, periodic and initial works share a lock

### Removed

- The MVP Room schema (`legacy`)

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
