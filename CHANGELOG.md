# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Several tags per movement (S18, 2.0, ADR-0015): Room 9 stores them in `transactions.tagIds` (`,id1,id2,`), the old single tag migrates into the list, a movement counts whole in each of its tags, deleting a tag keeps the others, and the native CSV joins them with `|`
- A color per tag from a palette of 6 (REQ-ETQ-05), picked in Nueva and Editar etiqueta (Room 9 `tags.color`)
- Recuperar contraseña (REQ-AUT-09): «¿Olvidaste tu contraseña?» in Iniciar sesión asks for the email, then the 6-digit code and the new password; afterwards every device signs in again

- Transactions to review from bank emails (S19, S20, ADR-0014): Room 10 `transaction_drafts` synced as `transactionDraft`; «Por revisar» in Historial applies one in a tap when its last 4 digits and its merchant are known, otherwise opens Revisar transacción (the form filled from the email, with Aplicar and Descartar); Inicio shows how many are waiting. Drafts count in no balance, total or report until applied
- Ajustes → Conectar correo: the forwarding address with a copy button, the Gmail steps, Gmail's confirmation code and the switch for the notice (REQ-COR-01, 10)
- «Últimos 4 dígitos» in accounts and in the card edit (REQ-COR-06)
- The notice of transactions to review through Firebase Cloud Messaging (ADR-0016), set up from `coffer.firebase.*` Gradle properties instead of `google-services.json`; off while they are blank
- Reportes detallados (S21, changes-ui-2.0 §3): «Resumen» and «Calendario» accesses, Gastos por categoría (pie and every category with its exact value), Detalle de categoría (vs. the month before to date, average, six months, subcategories, last movements and «Ver en Historial»), Resumen general (global filters sheet, a bar pair for every month of the period, tap a month to focus it, «En qué se fue») and Calendario (the balance at the end of each day, the day's movements and Hoja · Cuentas del calendario, saved on the phone). Fixed colors for the six base categories; any other goes into «Otras»
- The animated fox (S22, FEATURE-09): `FoxMascot` assembles the 29 layers of the PO's handoff (`FOX_LAYERS`, generated from `manifest.json`) and gives each state its loop — breathing, waving and blinking, the coffee toast, floating Z, twinkles, the drip and the calendar sweep — with a 520 ms bounce on entry and a cross-fade between states; still when the system has animations off. Inicio and Bienvenida use it
- A colored dot per tag after the title of every movement, and beside the tag's name in Etiquetas, pinned tags and Detalle de etiqueta (changes-ui-2.0 §2)

### Changed

- «Por revisar» takes Figma's finish: a dashed card, gray amounts without time, the ✓ that fills on tap; Revisar transacción shows «Descartar» as red text. Inicio's notice sits under the balance even when the quick actions are hidden (S21)
- The calendar starts with the accounts in Inicio's total only (no cards or loans), and a category's change is measured against the month before up to the same day

### Removed

- «Omitir esta vez» in recurring expenses (FEATURE-08): a pending payment is paid or the recurrence ends; the reminder no longer offers it either

## [1.6.1] - 2026-10-24

### Added

- Inicio: an eye on the balance card hides and shows the balance on the screen (Figma `1:997`; UI-01, #171)
- Categorías: «Usar para cuotas de préstamos» in the options of any expense category, main or sub, sets where the principal of loan installments counts without deleting anything; deleting that category now also offers subcategories as the new place (S17-H13, #170)

### Fixed

- Inicio: «Transferencia» is no longer cut in the quick actions, the strip takes a neutral tone and the fox stands on the balance card (#178, #179)
- Gestión de cuentas: the «Ver movimientos» row measures 44 dp (#180)
- Categorías: «Añadir subcategoría» is the link under the tiles, as in Figma
- Nueva transacción: «Deslizar» and the dot of the chosen account (#182)
- Nueva transacción: «Transferencia» fits on one line in the type tabs, the amount shows no «0» next to the cursor while it has the focus, and the title suggestion scrolls into view above the keyboard (S17-H12, #169)

## [1.6.0] - 2026-10-23

### Added

- A registered payment (loan installment, extra payment, card payment) can change its date from its detail, and the installments it settled follow; paying an installment or a card asks for the date and time (S15-H05, #162)
- Gastos recurrentes: tapping a row opens Hoja · Pendiente, and Editar gasto recurrente has «Finalizar recurrencia»
- Detalle de etiqueta: a trash beside the pencil deletes the tag after telling how many movements lose it
- Historial: the search also finds the movements of an exact amount typed as `45000`, `45.000`, `$45.000` or `45.000,50` (S15-H03, #160)
- Bienvenida is the first screen without a session (brand, mascot, «Comenzar», Google, «Inicia sesión»); an expired session still goes to Iniciar sesión (S15-H04, #161)
- Hoja · Detalle del saldo: «Elegir deudas» marks the cards and loans the net balance subtracts (Room 8 `accounts.includeInNet`, server V14); the net balance mode `TOTAL_DEBT` is now `CUSTOM` (S15-H07, #164)
- «Deudas», an expense category the system creates, receives the principal of loan installments and extra payments, which count as expenses in Reportes, Inicio, the fox and Historial; deleting it asks for another category that takes over (Room 8 `settings.loanPaymentCategoryId`, server V14; S15-H06, #163)

### Changed

- Inicio: the fox is 92 dp and stands 12 dp over the balance card, and the quick actions are a 48 dp strip (S16-H08, #165)
- Nuevo gasto recurrente: the category is the same row of four categories and «Más» as Nueva transacción (S16-H09, #166)
- Editar transacción: «Eliminar» is a red trash icon (S16-H10, #167)
- A movement saved without a title takes its category's name as the title (S15-H02)
- Loans are only seen in Gestión de cuentas: the movement, transfer, recurring and pending-payment pickers, the Home carousel and Personalizar inicio no longer offer them, and the default account is never a loan (S15-H02, #159)
- Nueva transacción: the amount takes the focus with the keyboard up, the tabs carry the Home quick-action icons, and the category row is five equal cells with «Más» in the fifth (also in Filtros and Nueva categoría)
- Links to `coffer-docs` removed from the README and the issue template, because it is now private; a Gitleaks workflow scans every PR and push for secrets
- The privacy policy, terms and third-party notices are published from `docs/` of this repository with GitHub Pages (`https://julian049.github.io/coffer-app/`), because `coffer-docs` goes private; `LegalDocs` points there
- Categorías: the main icon is round and «Añadir» is the last tile of the subcategories (S15-H04)
- Hoja · Detalle del saldo: the cards are tapped to choose what Home shows, with no «Mostrar en inicio» and no «Listo» (S15-H07)

### Fixed

- Editar préstamo: a payment day earlier than today no longer leaves the next installment overdue
- The History summary leaves out categories excluded from reports and the opening balance, like Home (S15-H01, #158)
- Reportes: in «Ahorro por mes» each month sits under its bar
- The title suggestion shows the best match first (exact, prefix, start of a word, contains), also when it is the exact one, and merges notes that differ only in case

## [1.5.0] - 2026-10-22

### Added

- Ajustes → Eliminar mi cuenta (S14-H01, #146): asks for the password again (or a Google confirmation) and, once the server (`DELETE /api/v1/me`) has erased the account, wipes the local encrypted database, stops the sync and the reminders and returns to the start. Needs a connection; a wrong proof keeps the session (`FIN-CONFIRMATION`, 403)

## [1.4.0] - 2026-10-22

### Added

- Reportes as in Figma (S13-H01, #139): fixed filters with Mes ▾, the month's saving with its share of the income and the average, Ritmo de gasto (cumulative spending against the month before, in days), Cambios por categoría (to the same day, by how much they changed, with diverging bars), Ahorro por mes (values above the bars, the running month dashed), Por etiqueta with Ver todas and one tap to each tag, Intereses y cargos and the activity line
- Categorías as a mosaic (S13-H02, #140): the month's total, each main category in a card with its share and its subcategories as tiles, Añadir subcategoría; Cambiar ícono in a category's options; Hoja · Nueva categoría with Ubicación and Ícono as icon rows and the button fixed; a name is unique across the whole tree («Ya existe una categoría llamada…»)
- One picker for categories everywhere: the four most used and Más (Hoja · Elegir categoría, a flat grid with search); Hoja · Filtros del historial picks several on the same row and Más opens Elegir categorías (varias). Accounts and categories pick their icon from five and Más (Hoja · Elegir ícono by group)
- Gastos recurrentes counts card statements and loan installments in the month's commitments and lists the installments with Registrar pago (S13-H03, #131)
- Título and, in a transfer, a separate Nota o concepto (S12-H01, #132; Room 7 `transactions.concept`, server V13). The form follows Figma: amount, title with earlier titles (category · account), the four most used categories and Más (Hoja · Elegir categoría, with search, most used first), date, the account without its balance, tags
- Gestión de cuentas (S12-H02, #136): Lo que tienes (bank accounts and cash) and Lo que debes (cards with use of the limit and next payment, loans with paid installments and next installment), with Pagar and Ver movimientos; Préstamos · Agregar creates a loan, which Nueva cuenta no longer offers; Editar cuenta changes the icon and color and no longer the opening balance; Editar préstamo changes name, rate, payment day, insurance and costs and recalculates the installments left; long names take two lines at most
- Net balance by total debt (S12-H03, #137): Hoja · Detalle del saldo chooses Cuotas del periodo or Deuda total (Room 7 `settings.netBalanceMode`, server V13)

### Changed

- Categories are flat outside Categorías and Reportes: rows, filters, suggestions and sheets show «Restaurantes», never «Comida > Restaurantes»; new users get Taller and Reparaciones instead of two Mantenimiento, and every base subcategory has its own icon (S13-H04, #141)
- Nueva transferencia as in Figma: full-width Desde and Hacia with a direction icon and the swap centred between them; Nota o concepto · opcional. The title suggestion is one row, the most recent, with what was typed in bold, and fills the account too
- Nueva cuenta shows the three types as equal boxes; Editar tarjeta shows the card and its debt above the editable fields; loan insurance and other costs are switches that ask for the monthly value; Gestión de cuentas adds Consolidado and quiet rows when there are no cards or loans; Descargando tus datos shows the fox
- An asset account's opening balance is a «Saldo inicial» movement, excluded from reports, and the field goes to 0; existing accounts move on the next start (S12-H03, #137)
- Forms and sheets with text keep their buttons fixed above the keyboard (S12-H04, #138); predictive back is off, so going back plays the screens' own transition; Descargando tus datos shows a short message and groups recurring expenses and tags; Home drops «Bienvenido de nuevo» and Gasto · Ingreso · Transferencia get the same padding; deleting a tag asks first and says its movements keep their data

## [1.3.1] - 2026-10-21

### Changed

- Personalizar inicio has no Guardar button: each change to Home's cards or the order of the accounts is saved as it happens, and leaving the screen keeps the last one

### Fixed

- Historial de cambios opens fast with thousands of entries (an import logs one per movement), shows a loading indicator meanwhile and no longer blocks Back (#127)
- Descargando tus datos no longer counts deleted movements, categories or accounts as downloaded data (#128)
- Pressing a movement highlights the whole row up to the card's edges (#129)
- The bottom bar shows the press on the tab's pill, the same area as the active tab (#130)

## [1.3.0] - 2026-10-21

### Added

- Historial de cambios (Ajustes): the last 90 days of creations, edits, deletions and restores grouped by day; Restaurar brings back a deleted movement and Revertir undoes an edit, only on the latest change of a movement (S11-H01, #118). Entries older than 90 days leave the phone on every start
- Net balance (REQ-CTA-08): the available total minus each card's current statement and each loan's next installment. A tap on Home's balance opens Hoja · Detalle del saldo with both views; Ajustes → Inicio and the sheet choose which one Home shows (S11-H02, #119)
- Personalizar inicio (from Home and Ajustes → Inicio): show, hide and reorder Home's cards by holding the handle (TalkBack gets Subir and Bajar), and reorder the accounts of the carousel (M9, #92). Ajustes → Montos shows every amount with 0 or 2 decimals (RNF-13) (S11-H04, #120)
- Historial shows the mascot of the month in its summary: a past month the state it closed with, by Home's rules on its last day; the current month Home's (M14, #97)
- Importar warns, before importing, when an account or a card already has an opening balance or debt that the imported history would add again (#67)

### Changed

- Home: the mascot stands on the background over the balance card instead of inside a card (M13, #96), and Gasto · Ingreso · Transferencia are a thin bar of three segments (M12, #95)
- Nueva transacción: Nota o concepto is the first field and suggests earlier notes of the same type; picking one fills the note and its latest category (M5, #89)

## [1.2.2] - 2026-10-21

### Fixed

- Associating an imported category with an existing one no longer closes the app (#114).

## [1.2.1] - 2026-10-20

### Added

- Loans (S10-H01 and H02, REQ-PRE-01 to 10, AT-HU14-1 and 2, #107, #108): Nueva cuenta → Préstamo opens Nuevo préstamo, detailed or simple. A detailed loan asks for the amount, the destination account, installments, payment day, rate (M.V. or E.A.), **fixed payment or fixed principal** (PO, 2026-10-20), first due date, life insurance and other costs, and shows the first installment; the money arrives as a transfer from the loan, not as income. "Ya lo estoy pagando" registers a loan in progress with today's balance, the installments left and the next due date, without a disbursement. A simple loan asks for the balance, the monthly payment and the payment day, and can become detailed later. Detalle de préstamo shows the debt, the terms and the plan; Pagar cuota splits principal (lowers the debt) from interest, insurance and costs (to "Intereses y cargos financieros", also in Reportes), and Abonar a capital simulates reducing the term or the payment before recalculating the plan. Each next installment is a pending payment in Home with the reminders of recurring expenses. Room 6 adds `loan_details`, synced as `loanDetails` (server V12)

### Changed

- Screens change with a short fade and a slight slide; the system's "remove animations" setting turns it off (M10, #93)

### Fixed

- Bottom sheets scroll when their content is taller than the screen: with many categories, Nueva categoría hid its "Crear categoría" button (#110)

## [1.1.0] - 2026-10-20

### Added

- Tags (S9-H02, REQ-ETQ-01 to 04, AT-HU10-1 and 2, #103): Ajustes → Etiquetas (also from Reportes, M6 #90) lists every tag with its month total and a pin; create, rename and delete (its movements stay, without a tag); the detail shows this month, last month or a range, the total, the expenses by category and the movements; pinned tags show their month total on Home
- Reportes (S9-H01, REQ-REP-01 to 05, #102): week (Monday to Sunday), month or range; income, expenses and balance; the evolution of the last 6 periods with the change against the previous one; expenses and income by category (tap to see the subcategories); interest and fees paid per card; activity (movements and average per day); transfers and excluded categories do not count, a deferred purchase counts installment by installment on each due date and a refund on a card is income (PO, 2026-10-20). `ReportAggregator` is pure domain code
- Exclude a category from reports (S9-H03, REQ-CAT-04, #85): Categorías → a main category's options → "Excluir de reportes", with a warning before turning it on; its movements and its subcategories' stop counting in Home's monthly totals and the fox (and in Reportes), the balances do not change; Room 5 adds `categories.excludeFromReports`, synced as `excludeFromReports` (server V11)

### Changed

- One tap on a movement opens its edit, with "Eliminar" on top; purchases in installments and card payments, which are not edited, still open their detail (M3, #87)
- The save button of the movement, account, card and recurring forms stays at the bottom, visible while the form scrolls (M4, #88)
- More icons to choose from: 27 for categories (transport, fuel, flights, coffee, games, gym, clothes, beauty, kids, phone, internet, power, water, repairs, savings…) and 11 for accounts; the pickers wrap into rows (M11, #94)
- Nueva tarjeta asks for the current debt (0 by default) instead of the current available credit, and shows the available credit it gives (M2, #86)

## [1.0.2] - 2026-10-20

### Changed

- The debit account type is gone: it was the same as savings. Nueva cuenta no longer offers it and the Cashew import creates savings accounts (#84)

### Fixed

- Typing in an amount or in the history search no longer jumps the cursor back or reorders letters: amount fields keep only the digits and draw `$1.250.000` on top (#75)
- The history search looks in the whole history, not just the selected month, unless Filtros set a date range (#79)
- Filtros shows subcategories as "Padre > Sub", so two subcategories with the same name can be told apart (#77)
- The name typed in Nueva cuenta is kept after choosing Tarjeta de crédito (#78)
- Bottom sheets no longer draw a stray line across the screen (#80)
- The last row of Home and History is no longer hidden behind the + button (#81)
- The import and export icons were swapped (#82)
- "Continuar con Google" shows the Google logo (#83)

## [1.0.1] - 2026-10-20

### Fixed

- A credit card's (or loan's) balance ignored its income: a refund, or a payment recorded as income, did not lower the debt (#72)
- Importing from Cashew brought in what Cashew does not count yet (pending loans, future subscriptions with an "amount unpaid"); now only the paid part is imported and the rest is reported as pending (#72)
- The two halves of a Cashew transfer are joined even when other rows sit between them, so they no longer count as an expense and an income (#72)
- Rows of the same file are no longer flagged as duplicates of each other (two bus fares in the same minute are two movements); duplicates are looked for only among the movements already in Coffer (#72)

## [1.0.0] - 2026-10-19

### Added

- "Continuar con Google" in Iniciar sesión and Registro (REQ-AUT-03, moved from S11 into 1.0.0 by the PO): Credential Manager gives a Google ID token for the web client id (`coffer.googleWebClientId` in `gradle.properties`; empty keeps the button disabled) and `POST /api/v1/auth/google` signs in, links by email or signs up
- Import from Cashew (S8-H01): Ajustes → Importar y exportar asks where the file comes from (only Cashew for now), reads the CSV in streaming (separator and encoding detected), suggests the column mapping (editable), and before importing shows the rows ready, the rows with errors, the possible duplicates (skipped by default) and the unknown accounts and categories, each created or associated with an existing one ("Crear todas"); Cashew's two-row transfers become one transfer and its budget becomes the tag; `ImportWorker` writes batches of 500 in the background, all pending sync, and the screen keeps the last summary
- Export to CSV (S8-H02) with an optional range (all, this month, last month, custom) in Coffer's native format (`date, type, amount, account, destination_account, category, subcategory, tag, note`)
- Release pipeline (S8-OPS02): the instrumented tests run on an API 31 emulator before signing (`instrumented.yml`, also on PRs that touch them)
- Tests: `CsvReaderTest`, `CashewFormatTest` (anonymized Cashew export), `ImportTest` (AT-HU11-1 to 3, 5.000 rows, export), `PerformanceTest` (Home with 10.000 movements) and screenshots of Importar y exportar

## [0.8.0] - 2026-10-19

### Added

- Credit cards (S7-H01): Nueva tarjeta de crédito (name, credit limit, current available credit — the difference is the initial debt, due as a purchase in one installment of the period —, cut and payment days, rate in M.V. or E.A. with the conversion visible, first installment interest-free), reached by choosing Tarjeta de crédito in Nueva cuenta, and its edit; Detalle de tarjeta with the debt and credit used, what is due this period, the terms and the active deferred purchases; synced as `creditCardDetails`, `deferredPurchase` and `installment` (Room version 4)
- Card engine: `RateConverter` (2 % M.V. ↔ 26,8242 % E.A.), `InstallmentCalculator` (fixed principal, interest on the balance with a 30-day month, the last installment absorbs the rounding) and `CardPeriodCalculator` (cuts on the 28th to 31st follow short months; payment on the first payment day after the cut; purchases between the cut and the payment carry no extra interest, PO 2026-10-19)
- Purchases in installments (S7-H02): choosing a card in Nueva transacción shows Condiciones del crédito with 1 installment by default; from 2 on, the card's rate, editable (0 % agreements); the plan is written with the expense in one database transaction, and deleting the expense deletes its plan
- Card payment (S7-H03): Hoja · Pagar tarjeta with pago mínimo (what is due) or pago total (the whole debt plus the period's interest, no future interest), source account and an editable amount split into fees, interest and principal; only the principal lowers the debt; the minimum settles the period's installments and the total cancels the future ones; the card's payment is a pending payment in Home and Gastos recurrentes with the same window, urgency and notices as recurring expenses; the detail of a payment shows its split
- Tests: `RateConverterTest`, `InstallmentCalculatorTest`, `CardPeriodCalculatorTest`, `CreditCardTest`, `CardCodecTest`, the 3 → 4 migration, a card notice in `ReminderWorkerTest` and screenshots of the card screens

### Changed

- A deferred purchase or a card payment is not edited from its detail: it is deleted and registered again
- Editing a movement keeps what the form does not show, such as the recurring expense a payment came from

## [0.7.0] - 2026-10-18

### Added

- Recurring expenses (S6-H01): Gastos recurrentes with the month's commitments (paid plus pending), filters Todos, Próximos, Vencidos and Pagados, and the form with weekly, biweekly, monthly, quarterly, yearly or custom frequencies; months are added to the start date, so the 31st falls on the last day of short months; a start in the past begins at the next date from today; reached from Settings; synced as `recurringExpense` (Room version 3)
- Pending payments (S6-H02): Home's Próximos pagos lists the recurring expenses inside the reminder window and the overdue ones, with their urgency, and never changes a balance; the sheet pays (amount of the last payment, today and the account, all editable; creates the expense and moves to the next date), skips this time or finalizes; Gastos recurrentes gets Omitir and Registrar pago; the fox gets attentive when a payment is due in 2 days or less
- Reminders (S6-H03): a daily check near 08:00 without network sends one notice when a payment's window opens (or on the next run inside it) and one on the due date if still unpaid, in the "Pagos pendientes" channel; tapping it opens the pending sheet in Home; Settings → Avisos turns them off and sets the lead days (0 to 30, 5 by default); the notification permission is asked when saving a new recurring expense or turning notices on; sign-out cancels them

## [0.6.0] - 2026-10-18

### Added

- Record expenses, income and transfers (S5-H01): one form with tabs, amount, category (main ones at hand, all in "Ver todas"), date and time now but editable, the last account used, one tag with "Nueva etiqueta" inline, and note; Home's quick actions and the + button of Home and History open it
- `change_log` (Room 2, auto-migration tested with `MigrationTestHelper`): every create, edit and delete of a movement writes its before and after in the same database transaction, and syncs as `changeLog`; the pending badge still counts one per movement
- Full History (S5-H02): month chips, live search in note, category, account and tag, type chips, a filter sheet (type, category with its subcategories, account as origin or destination, tag, date range), the month summary and rows loaded 150 at a time
- Movement detail, edit and delete with confirmation (S5-H03); rows of Home, History and account detail open it
- Tests: `RegisterTransactionTest` (including a save under 300 ms), `RegisterTransferTest`, `EditDeleteTransactionTest`, `TransactionDaoTest`, `FinanceDatabaseMigrationTest`, a new `HistoryViewModelTest` and screenshots of the form, the transfer and the detail

### Changed

- Within a day the newest movement goes first (PO, 2026-10-18)

## [0.5.0] - 2026-10-18

### Added

- Accounts (S4-H01): Gestión de cuentas with balances, Nueva cuenta (cash, debit, savings; cards and loans show "Disponible pronto"), Detalle de cuenta with "Cómo se calcula" and its movements, Editar cuenta (name, initial balance, include in total; the type never changes), Cuentas desactivadas with Reactivar
- Removing an account (SEQ-12): deactivate (recommended), move its movements to another active account (transfers between both disappear) or delete it with its movements after typing its name, with the impact shown first (REQ-CTA-05)
- Categories (S4-H02): expense and income tabs with this month's totals, new category or subcategory (one level, same kind), rename, and delete with its subcategories moving every movement to the chosen category; "Intereses y cargos financieros" cannot be deleted while a card or loan exists
- Home (S4-H03): total available balance with the change against last month's close, the fox with its mood (rules approved by the PO), quick actions (disabled until S5), accounts carousel and the last 5 days grouped by Hoy, Ayer, Antier and date
- Settings links to Categorías and Cuentas desactivadas
- `BalanceCalculator`, `FoxRules` and the account and category use cases; every write is atomic, stays PENDING and requests a sync
- Base categories get icons on new accounts
- Unit test coverage with AGP's JaCoCo (`createDebugUnitTestCoverageReport`)
- Tests: `BalanceCalculatorTest`, `FoxRulesTest`, `RemoveAccountTest`, `DeleteCategoryTest`, `CategoryRepositoryTest`, `HomeViewModelTest`, `ConfirmDeleteAccountTest` and new screenshots

### Changed

- History uses the names of every account (deactivated ones too) and shares the day groups of Home and account detail

## [0.4.0] - 2026-10-17

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
