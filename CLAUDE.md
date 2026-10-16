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

Sprint 1 (`v0.2.0`) in progress. S1-H03 set up the project base: build types, networking with kotlinx.serialization, the `ui`/`domain`/`data`/`di` skeleton and CI/release workflows. The MVP code lives under `legacy/` and still drives the UI until S1-H04 (design system and 4-tab navigation) replaces it. When the code changes, update this file accordingly.

## Project

Single-module Android app (`:app`, package and `applicationId` `com.dev.julian09.financeapp`; **never change the `applicationId`**). Kotlin + Jetpack Compose + Material 3 + Navigation Compose + Hilt (KSP) + Room + Retrofit 3/OkHttp 4 with **kotlinx.serialization** (no Gson). minSdk 31, targetSdk 36 (compileSdk 36.1), no desugaring. AGP 9 with built-in Kotlin (no `kotlin-android` plugin). README, UI strings, log messages and test names are in Spanish; CHANGELOG follows Keep a Changelog in English. Commits use Conventional Commits.

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
./gradlew testDebugUnitTest                     # JVM tests (JUnit 4, MockK, Konsist)
./gradlew ktlintCheck                           # style (ktlintFormat fixes most issues)
./gradlew lint
./gradlew lint ktlintCheck testDebugUnitTest assembleStaging   # what CI runs
```

Unit tests run with `isReturnDefaultValues = true`, so `android.util.Log` calls are no-ops in JVM tests.

CI (`.github/workflows/ci.yml`) runs on every PR and push to `develop`/`main` and uploads the staging APK as the `coffer-staging-apk` artifact (that is what the PO installs to test a PR). `release.yml` runs on `v*` tags: signed `assembleRelease bundleRelease` and a GitHub Release with APK, AAB and `SHA256SUMS.txt`. Dependabot updates Gradle and Actions weekly.

## Architecture

Target structure: detailed-design §6.2.2. Today:

- `FinanceApp` (`@HiltAndroidApp`), `MainActivity`.
- `domain/port/Clock` (inject it; never read the system clock in domain code). Implemented by `data/clock/SystemClock`.
- `data/remote/HealthApi` (`GET actuator/health`, sends `Accept: application/json` because Actuator otherwise answers with its own vnd media type) + `dto/HealthDto`.
- `di/NetworkModule` (`Json { ignoreUnknownKeys }`, OkHttp with 60 s timeouts because Render's free plan sleeps, Retrofit on `BuildConfig.API_BASE_URL`), `di/DispatchersModule` (`@IoDispatcher`, `@DefaultDispatcher`), `di/RepositoryModule` (binds ports), `di/LegacyModule` (MVP Room and repository).
- `ui/theme` (still the MVP theme until S1-H04).
- `legacy/`: the MVP, described below.

Rules in `ArchitectureTest` (Konsist, runs with the unit tests): `domain` imports no `android.*`, `androidx.*` or `data`; no `Double`/`Float` properties, parameters or return types in `domain` (ADR-0004); `ui` does not import `data`. `legacy` is outside these rules.

## Legacy MVP (`legacy/`)

MVVM + Clean Architecture under `legacy/`, layered as `ui` → `viewmodel` → `domain/usecase` → `domain/repository` (interface) ← `data` (implementation). Everything revolves around a single entity, the transaction, represented three times with identical fields: `TransactionEntity` (Room), `TransactionDto` (API), `Transaction` (domain). Mapping between them is done inline in `TransactionRepositoryImpl` — there are no mapper classes, so a field change must be applied to all three plus the mapping code.

- **DI**: Room `finance_db`, DAO and repository in `di/LegacyModule.kt`; `ApiService` comes from the shared Retrofit in `di/NetworkModule.kt`, so it talks to the server of the current build type.
- **Backend contract** (`legacy/data/remote/ApiService.kt`): `GET actuator/health` (Spring Boot actuator), `GET /transactions`, `POST /transactions`.
- **Single ViewModel**: `TransactionViewModel` is shared by both screens (`MainScreen`, `RegisterTransaction`; routes in `ui/navigation/Screen.kt`, NavHost in `MainActivity`). It exposes one `StateFlow<UiState>`. Navigation back after saving is driven by `isTransactionAdded` + `resetNavigation()`.
- **Use cases** are thin `operator fun invoke` wrappers over the repository, injected with `@Inject constructor`.

### Sync flow & data conventions (non-obvious)

- `getTransactions()` first pulls all remote transactions **with `runBlocking`** and upserts them into Room (`REPLACE` on `localId`), then returns the Room `Flow`. Room is the single source of truth for the UI; the balance is recomputed on every emission.
- New transactions are only inserted locally with `synced = false`. `syncTransactions()` (triggered manually from the main screen) POSTs each unsynced row, then marks **all** unsynced rows as synced in one query. Errors are logged and swallowed.
- `localId` is a client-generated UUID string and is the primary key on both sides.
- `TransactionDto.value` is a `String`: since server v0.2.0 money travels as a JSON string; the repository converts it to and from the `Double` the MVP stores.
- **Sign encodes direction**: `value` is negative for expenses — the register screen prepends `-` to the amount for expense type. The balance is a plain sum of `value`. `type: Boolean` is derived from the sign in the ViewModel (`true` when negative).
- **Date format**: the UI passes `"<epochMillis> <H>:<m>"`; `TransactionRepositoryImpl.formatDate` converts it to an ISO-8601 offset string (UTC) before storing/sending, falling back to the raw string on parse failure.
- UI status colors: the per-item indicator reflects `synced`; the header indicator reflects `isApiHealthy` (health check runs once in the ViewModel `init`).
