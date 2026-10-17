# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Product and documentation

This repository is the Android client of **Coffer** (formerly "Finance App"): an offline-first personal finance app. GitHub: `Julian049/coffer-app`. Its sibling repositories are `coffer-server` (Spring Boot backend) and `coffer-docs` (all project documentation). Locally they live side by side in the same parent folder (`finance-app/`, `finance-server/`, `docs/`), so the docs are at `../docs/`.

**The documentation is the source of truth — read it before changing anything structural:**

| What | Where |
| --- | --- |
| Requirements (SRS v1.0) | `../docs/01-requirements/` |
| Architecture (arc42) and ADRs | `../docs/02-architecture/architecture.md`, `../docs/02-architecture/adr/` |
| Detailed design: packages, classes, algorithms | `../docs/03-design/detailed-design.md` |
| Data model, Room mapping and MVP migration | `../docs/03-design/data-model.md` (§5) |
| Sequence diagrams per use case | `../docs/03-design/sequences/` |
| API contract | `../docs/04-api/openapi.yml` |
| Test plan and acceptance cases | `../docs/05-test/test-plan.md` |
| Branching, versioning, releases | `../docs/06-settings/manage-config.md` |
| Scrum rules and **sprint plan** | `../docs/07-management/scrum.md`, `../docs/07-management/sprint-plan.md` |
| UI: Figma prototype and design system | `../docs/08-ui/README.md`, `../docs/08-ui/design-system.md` |

## How work is done here

- Work proceeds **sprint by sprint** following `sprint-plan.md`; only implement the current sprint's stories (no scope creep). Each story = one issue, one branch `feature/<issue>-<slug>` from `develop`, one PR to `develop`, with a "Cómo probar" section for manual testing by the product owner (@Julian).
- Never push to `main` or `develop` directly, never force-push shared branches, never commit secrets (keystores, `.env`, keys). Release signing uses GitHub secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- **Do not change the `applicationId`** (`com.dev.julian09.financeapp`): changing it would make Android treat the app as a different one and the MVP local database could not be migrated. Only the visible app name becomes "Coffer".
- Ask the product owner when the SRS, design or prototype are ambiguous; structural decisions get an ADR.
- Conventional Commits; CHANGELOG (Keep a Changelog) updated in every PR under `[Unreleased]`. Commits produced with AI assistance carry a `Co-Authored-By` trailer.

## Current state

Sprint 1 (`v0.2.0`): project base (S1-H03) and design system + 4-tab navigation (S1-H04). The tabs are empty containers until their sprints fill them (Home and Accounts in S4, History in S5, Reports in S9); Settings shows the server status and the version. The MVP code is gone; its data moves to the encrypted database on first start (S3-H02). When the code changes, update this file accordingly.

## Project

Single-module Android app (`:app`, package and `applicationId` `com.dev.julian09.financeapp`; **never change the `applicationId`**). Kotlin + Jetpack Compose + Material 3 + Navigation Compose + Hilt (KSP) + Room + Retrofit 3/OkHttp 4 with **kotlinx.serialization** (no Gson). minSdk 31, targetSdk 36, compileSdk 37 (current AndroidX and OkHttp 5 require it), no desugaring. AGP 9 with built-in Kotlin (no `kotlin-android` plugin). README, UI strings, log messages and test names are in Spanish; CHANGELOG follows Keep a Changelog in English. Commits use Conventional Commits.

Version: `versionName` = SemVer, `versionCode` = `MAJOR×10000 + MINOR×100 + PATCH` (`0.2.0` → 200), both in `app/build.gradle.kts`.

### Build types (manage-config §5.3)

| Build type | `applicationId` | Name on the phone | `BuildConfig.API_BASE_URL` | Signing |
| --- | --- | --- | --- | --- |
| `debug` | `….financeapp.debug` | Coffer debug | `http://10.0.2.2:8080/` (local server from the emulator) | debug |
| `staging` | `….financeapp.staging` | Coffer staging | `https://coffer-server-staging.onrender.com/` | debug key |
| `release` | `….financeapp` | Coffer | `https://coffer-server.onrender.com/` | release keystore, only in `release.yml` |

`app_name` comes from `resValue` per build type (not `strings.xml`). Cleartext HTTP is allowed **only** in `debug` and only to `10.0.2.2` (`src/debug/res/xml/network_security_config.xml`); `main` forbids it (RNF-10). The release `signingConfig` exists only when `RELEASE_KEYSTORE_PATH` is set (the release workflow decodes `RELEASE_KEYSTORE_BASE64` into it), so local `assembleRelease` builds unsigned.

## Commands

On this machine the Android SDK is installed on Windows, so from WSL run Gradle through Windows: `cmd.exe /c "gradlew.bat <tasks>"` (it also avoids WSL's DNS hiccups). If the Kotlin daemon crashes for lack of memory, add `-Pkotlin.compiler.execution.strategy=in-process`.

```bash
./gradlew assembleDebug assembleStaging         # APKs
./gradlew installStaging                        # install on a connected device
./gradlew testDebugUnitTest                     # JVM tests (JUnit 4, MockK, Konsist, Robolectric)
./gradlew recordRoborazziDebug                  # re-record reference screenshots after an intended visual change
./gradlew ktlintCheck                           # style (ktlintFormat fixes most issues)
./gradlew lint
./gradlew lint ktlintCheck testDebugUnitTest assembleStaging -Proborazzi.test.verify=true   # what CI runs
```

Network tests use MockWebServer (`mockwebserver3`) with the same interceptor/authenticator wiring (`TestNetwork`) and `FakeSessionStore`. Instrumented tests (`src/androidTest`, e.g. `SessionStoreTest` with the real Keystore) are **run by hand** (`./gradlew connectedDebugAndroidTest` on a device or an emulator with API 31–36), not in CI. On this machine, if Gradle fails with "paging file is too small", add `--max-workers=1`.

Robolectric runs with a plain `android.app.Application` (`src/test/resources/robolectric.properties`) because `FinanceApp` would open the SQLCipher database, whose native library only exists on devices; JVM database tests use `inMemoryDatabase()` (same schema, no SQLCipher) and encryption is covered by `EncryptionInstrumentedTest`.

Unit tests run with `isReturnDefaultValues = true`, so `android.util.Log` calls are no-ops in JVM tests.

CI (`.github/workflows/ci.yml`) runs on every PR and push to `develop`/`main` and uploads the staging APK as the `coffer-staging-apk` artifact (that is what the PO installs to test a PR). `release.yml` runs on `v*` tags: signed `assembleRelease bundleRelease` and a GitHub Release with APK, AAB and `SHA256SUMS.txt`. Dependabot updates Gradle and Actions weekly.

## Architecture

Target structure: detailed-design §6.2.2. Today:

- `FinanceApp` (`@HiltAndroidApp`), `MainActivity` (edge-to-edge, `CofferTheme { AppNavHost() }`).
- **Theme** (`ui/theme`): the 16 tokens of `design-system.md` as `CofferColors` (light and dark), read with `CofferTheme.colors.<token>`; `CofferTypography` (weights 400/500 only), `CofferShapes` (12/16/14/20 dp radii, pill). The Material `ColorScheme` is derived from the tokens with every surface slot flat (no tonal tint). No dynamic color. Do not hardcode colors in screens.
- **Components** (`ui/components`): `MoneyText` + `formatMoney` (COP without decimals, `$1.250.000`; sign and color by `MoneyKind`: income `+` green, expense/debt `−` red, transfer neutral), `AppButton` (`ButtonKind` Primary/Secondary/Ghost/Danger), `AppCard` (1 dp border, `highlight` for the featured card), `AppChip`, `SegmentedSelector`, `AppBottomSheet`, `ConfirmDialog`, `EmptyState` (resting fox), `SyncBadge` (`domain/model/SyncStatus`), `AppHeader` + `Avatar`, `AppTextField` (label above, 48 dp `surface` field, 1.5 dp `primary` border on focus, `expense` on error, password toggle, autofill `ContentType`). Sizes and paddings follow the prototype (Figma plugin `docs/08-ui/figma-plugins/srs-prototype/code.js` is the most precise spec of each piece).
- **Root** (`ui/root/AppRoot`, S2-UI02): `SessionViewModel` observes `ObserveSession`; `Loading` → blank background, `SignedOut`/`Expired` → `AuthNavHost` (Login ⇄ Register; expired shows a notice and pre-fills the username), `Active` → `AppNavHost(user)`. Screens never navigate after sign-in or sign-out: the session change swaps the whole tree.
- **Navigation** (`ui/navigation`): type-safe routes (`@Serializable` objects in `Routes.kt`), `AppNavHost` (Scaffold; the custom `BottomBar` only on the 4 tabs; standard tab behavior with saved state), avatar → `SettingsRoute`.
- **Features** (`ui/feature/*`): `auth` (`LoginScreen`/`RegisterScreen` + view models; failures map to field errors or a form message via `AuthFailure.formMessage()`, never technical text), `home`, `history`, `reports`, `accounts` (empty containers) and `settings` (profile from the session + `RefreshProfile`, server status via `CheckServerStatus` → `ServerStatusRepository` → `HealthApi`, sign-out with `ConfirmDialog`). Each screen has a stateless `*Content` composable used by tests and screenshots.
- **Domain**: `model/Money` (Long ×10⁴, ADR-0004), `model/SyncStatus`, `model/Session` (`SessionState` Loading/SignedOut/Active/Expired, `UserProfile`), `model/AuthFailure` (what auth screens show) and `AuthValidation` (same rules as the contract, checked before any network call), `port/Clock`, `port/ServerStatusRepository`, `port/AuthRepository`, use cases `CheckServerStatus`, `RegisterUser`, `Login`, `Logout`, `ObserveSession`.
- **Session** (`data/session`, SEQ-01/03): `SessionStore` interface, implemented by `EncryptedSessionStore` (typed DataStore `session.bin`, bytes AES-256-GCM encrypted with the Keystore key `coffer_session_key` via `KeystoreCipher`; an undecryptable file resets to signed out). `DeviceIdProvider` keeps a per-installation UUID in SharedPreferences (survives logout). `AuthInterceptor` adds the bearer token except on `AuthApi.PUBLIC_PATHS`; `TokenAuthenticator` refreshes once under a lock on 401 (`AuthApi.refresh` is a blocking `Call` because it runs inside OkHttp) and retries, marks the session expired when the refresh is rejected, and does nothing on network errors. Both read the store with `runBlocking` on OkHttp's threads. `AuthRepositoryImpl` maps RFC 9457 problems to `AuthFailure` (409 tells username from email by the server's wording).
- **Data**: `clock/SystemClock`, `remote/HealthApi` (sends `Accept: application/json` because Actuator otherwise answers with its own vnd media type), `repository/ServerStatusRepositoryImpl`.
- **DI**: `NetworkModule` (`Json { ignoreUnknownKeys }`, OkHttp with 60 s timeouts because Render's free plan sleeps, Retrofit on `BuildConfig.API_BASE_URL`), `DispatchersModule` (`@IoDispatcher`, `@DefaultDispatcher`), `RepositoryModule` (binds ports).
- **Local database** (`data/local`, data-model §5): `FinanceDatabase` (`finance.db`, Room version 1, schemas exported to `app/schemas/` — commit them; later changes need a tested `Migration`, never a destructive fallback) opened with SQLCipher (`SupportOpenHelperFactory`) using `DatabaseKeyProvider` (random 256-bit passphrase wrapped with the Keystore key `coffer_database_key`, file in `noBackupFilesDir`; if it cannot be unwrapped the database is deleted and rebuilt from the cloud). Entities mirror the server tables without `user_id`/`version`, plus `syncState` (`PENDING`/`SYNCED`/`FAILED`, the outbox) and `syncError`; amounts are `Long` ×10⁴, instants epoch ms, UUIDs text. `sync_meta` holds `pull_cursor`, `last_sync_at`, `legacy_migrated`. `LegacyDatabaseMigrator` runs once at app start (`FinanceApp`, on `@ApplicationScope`): reads the MVP `finance_db` with plain SQLite, writes it with the V6 rules and the server's fixed UUIDs (account `…a1`, categories `…b1`/`…b2`), all rows `PENDING`, then deletes `finance_db`. Read ports `AccountRepository`, `CategoryRepository`, `TransactionRepository` → `*RepositoryImpl`.
- **Sync** (`data/sync`, ADR-0006, SEQ-02/08/09): `SyncEngine.sync()` = push the outbox (rows with `syncState = PENDING`, in `SyncTables.all` dependency order, batches of 200; `APPLIED`/`STALE` → `SYNCED` and `REJECTED` → `FAILED` only if `updatedAt` did not change while sending) → pull from `sync_meta.pull_cursor` (pages of 500; `ChangeApplier` keeps a local unsent edit that is as new or newer, otherwise writes the server row as `SYNCED`, and saves the cursor in the same transaction) → on the first round, `BaseCatalogSeeder` and `initial_sync_done`. `sync_meta.owner_user_id` pins the database to its first account: another account's session throws `ForeignDataException` and never syncs. `SyncWorker` (`@HiltWorker`; `FinanceApp` provides the `HiltWorkerFactory`, the default WorkManager initializer is removed in the manifest) maps errors: network, 5xx, 429 → retry with backoff, anything else → failure. `SyncScheduler` (`SyncSchedulerImpl`): `requestSync` (unique, KEEP, 5 s, needs network), `requestInitialSync` (expedited, returns the work id), `ensurePeriodic` (15 min), `cancelAll` (called by `Logout`). To sync a new table: entity + DAO sync queries (`pending`, `syncInfo`, `markSynced`, `markFailed`) + a `SyncTable` in `SyncTables.all`.
- `KeystoreCipher` takes an alias: `SESSION_KEY` for tokens, `DATABASE_KEY` for the database passphrase (ADR-0008: one key per purpose). Android backup and device transfer are off (`allowBackup=false`, `data_extraction_rules.xml` excludes everything).

Rules in `ArchitectureTest` (Konsist, runs with the unit tests): `domain` imports no `android.*`, `androidx.*` or `data`; no `Double`/`Float` properties, parameters or return types in `domain` (ADR-0004); `ui` does not import `data`.

### Brand assets and screenshots

- Brand and mascot images come from `../docs/08-ui/icons/{brand,mascot}` (source of truth, fixed names). `scripts/sync-brand-assets.sh` (wrapper of `sync_brand_assets.py`, needs Pillow with WebP) writes them to `res/drawable-nodpi/` as 512 px WebP with underscores (`mascot-resting.png` → `R.drawable.mascot_resting`). On this machine Pillow lives on Windows: run `python.exe 'C:\...\finance-app\scripts\sync_brand_assets.py'` or `PYTHON=python.exe scripts/sync-brand-assets.sh`. The launcher icon is adaptive: `coffer_mark` inset to the safe zone on `#FAFAF9`, `coffer_mark_mono` as the themed icon.
- Reference screenshots (Roborazzi + Robolectric, light and dark) live in `app/src/test/screenshots/` and CI verifies them. After an intended visual change, run `recordRoborazziDebug`, look at the PNGs and commit them.
