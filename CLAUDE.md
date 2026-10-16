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

The code below is still the **MVP (`v0.1.0`)**. Sprint 1 (`v0.2.0`) restructures it according to the detailed design (packages `ui`/`domain`/`data`/`di`, minSdk 31, build types `debug`/`staging`/`release`, kotlinx.serialization, Material 3 theme from the design system). When the code changes, update this file accordingly.

## Project (MVP)

Single-module Android app (`:app`, package `com.dev.julian09.financeapp`) for personal finance tracking: offline-first with Room, synced to a REST backend via Retrofit. Kotlin + Jetpack Compose + Navigation Compose + Hilt (KSP). minSdk 24, compile/targetSdk 36, Java 11 with core library desugaring (needed for `java.time` on old APIs). README, UI strings, log messages and test names are in Spanish; CHANGELOG follows Keep a Changelog in English. Commits use Conventional Commits (`feat:`, `fix(ui):`, `docs:`).

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # JVM unit tests (JUnit 4 + MockK)
./gradlew testDebugUnitTest --tests "com.dev.julian09.financeapp.data.TransactionRepositoryImplTest"   # single test class
./gradlew connectedAndroidTest          # instrumented tests (needs device)
./gradlew lint
```

Unit tests run with `isReturnDefaultValues = true`, so `android.util.Log` calls are no-ops in JVM tests.

Note: `TransactionRepositoryImplTest` builds `Transaction` with an `Int` `localId`, but the model uses `String` (UUID) — the test source set won't compile until that is fixed.

## Architecture (MVP)

MVVM + Clean Architecture, layered as `ui` → `viewmodel` → `domain/usecase` → `domain/repository` (interface) ← `data` (implementation). Everything revolves around a single entity, the transaction, represented three times with identical fields: `TransactionEntity` (Room), `TransactionDto` (API), `Transaction` (domain). Mapping between them is done inline in `TransactionRepositoryImpl` — there are no mapper classes, so a field change must be applied to all three plus the mapping code.

- **DI**: everything is wired in `di/AppModule.kt` (Retrofit, `ApiService`, Room `finance_db`, DAO, repository binding). The Retrofit **base URL is hardcoded** there — currently a zrok public share; the commented-out `http://10.0.2.2:8080/` is the local backend from the emulator. Cleartext traffic is enabled in the manifest for that local case.
- **Backend contract** (`data/remote/ApiService.kt`): `GET actuator/health` (Spring Boot actuator), `GET /transactions`, `POST /transactions`.
- **Single ViewModel**: `TransactionViewModel` is shared by both screens (`MainScreen`, `RegisterTransaction`; routes in `ui/navigation/Screen.kt`, NavHost in `MainActivity`). It exposes one `StateFlow<UiState>`. Navigation back after saving is driven by `isTransactionAdded` + `resetNavigation()`.
- **Use cases** are thin `operator fun invoke` wrappers over the repository, injected with `@Inject constructor`.

### Sync flow & data conventions (non-obvious)

- `getTransactions()` first pulls all remote transactions **with `runBlocking`** and upserts them into Room (`REPLACE` on `localId`), then returns the Room `Flow`. Room is the single source of truth for the UI; the balance is recomputed on every emission.
- New transactions are only inserted locally with `synced = false`. `syncTransactions()` (triggered manually from the main screen) POSTs each unsynced row, then marks **all** unsynced rows as synced in one query. Errors are logged and swallowed.
- `localId` is a client-generated UUID string and is the primary key on both sides.
- **Sign encodes direction**: `value` is negative for expenses — the register screen prepends `-` to the amount for expense type. The balance is a plain sum of `value`. `type: Boolean` is derived from the sign in the ViewModel (`true` when negative).
- **Date format**: the UI passes `"<epochMillis> <H>:<m>"`; `TransactionRepositoryImpl.formatDate` converts it to an ISO-8601 offset string (UTC) before storing/sending, falling back to the raw string on parse failure.
- UI status colors: the per-item indicator reflects `synced`; the header indicator reflects `isApiHealthy` (health check runs once in the ViewModel `init`).
