# Column Visibility Presets — Design Spec

- **Date:** 2026-06-28
- **Target repo:** `happycamper-heart` (`~/Desktop/happycamper-public`) — the public-distribution / rapid-prototype repo
- **Spec location:** tracked in-repo at `happycamper-public/docs/design/column-visibility-presets-design.md`
- **Status:** Approved design, ready for implementation planning
- **Author context:** Brainstormed interactively; this document carries all context needed to execute in a fresh session.

---

## 1. Context & motivation

HappyCamper is a Java 22 Swing desktop app (Maven) for camp-roster validation. Users open a roster (CSV), and a configurable set of columns is shown. Column visibility today lives **only in memory** for the session — there is **no cross-run persistence anywhere in the app**. The only disk I/O today is CSV import/export.

This feature adds **named, persisted presets for column visibility**: a user can save the current column layout under a name ("Swim view", "Medical view", …), re-apply it later, and optionally mark one to auto-apply.

### Why this matters beyond the feature itself (the "canary")
This is the **first permanence-between-runs mechanism in the codebase**. It is deliberately built in `happycamper-heart` as a quick ship *and* as an exploratory canary for the eventual clean implementation in `happycamper-v3`. Three patterns this prototype is meant to prove out and hand to v3:
1. **Cross-platform config-location resolution** (per-OS app-data dir).
2. **Forgiving apply** (a saved view gracefully meets any roster, including rosters whose columns differ).
3. **Versioned, migrate-on-load persistence** with a JSON format that ports verbatim to web `localStorage` (v3 is web-bound; a TeaVM spike already proved core transpiles to JS).

> **v3 relationship (do not couple):** v3 already has a domain `CampConfig` *and* a separate `ExportConfig`. This view-config is a **sibling to `ExportConfig`** (display/UI concern), explicitly **not** part of `CampConfig` (domain constants like rounds/swim levels). v3 may later reuse the *plumbing* (a generic config store), but never share a schema. We are not touching the v3 codebase in this work.

---

## 2. Goals / Non-goals

### Goals
- Persist a small library of **named column-visibility presets** across app runs.
- Persist a single, optional **default preset** that auto-applies when a roster is loaded.
- Apply/save/delete/set-default from the **existing Column Settings dialog** — **no new window**.
- Survive heart re-releases (frequent) with **zero user friction** — presets must not be lost on upgrade.
- Be platform-agnostic (Windows / macOS / Linux).
- Produce a JSON format and code patterns that inform v3 without coupling.

### Non-goals (this prototype)
- No sizing/sort/filter state in a preset — **visibility only**.
- No explicit Export/Import-preset UI (the plain-JSON file is a manual escape hatch; sharing is "send them the file").
- No multi-default / per-roster default sets.
- No per-checkbox visual "diff highlighting" (deferred — see §12).
- No "Explore / show-all" built-in starter preset (deferred — see §12).
- No changes to `happycamper-v3`.

---

## 3. Key decisions (locked, with one-line rationale)

| Decision | Choice | Why |
|---|---|---|
| Preset scope | **Visibility only** | Cleanest "view = column set" story; sizing is a live tweak, not a saved view. |
| Encoding | **Per-column overrides on a curated baseline** (not a flat hide/show list) | Preserves the `RosterHeader` "displayed vs serves-a-purpose-but-hidden" hierarchy; compact; lets presets both hide noise and reveal hidden columns. |
| Unmentioned columns | **Inherit `RosterHeader.defaultVisibility`** (custom CSV columns → visible) | Forward-compatible: new feature columns appear iff their author set the default; old presets need no migration. |
| Storage format | **Hand-rolled JSON** (zero new deps), self-contained | Honors heart's dependency-free ethos + v3/TeaVM-friendliness; JSON ports straight to web `localStorage` (the canary's point). |
| Storage location | **OS-native app-data dir**, outside the install dir | Survives upgrades for free; good OS citizenship; the cross-platform path logic is exactly what the canary should prove. |
| Startup behavior | **Opt-in default preset**, applied on roster load via a **forgiving filter** | Smooth "I choose what loads" UX at a tiny failure-surface increment over a pure manual library. |
| "Reset to Default" | **Unchanged**; remains the privileged factory base layer | It is the referent for overrides, the recovery floor, and the only layer that auto-covers future columns. |
| UI | **Purpose-built `PresetSelector`** (rows, not dropdown+buttons) inside the existing dialog | Presets are stateful objects (active/default/dirty); rows *show* state, buttons can't. Fits the existing modular selector system. |

---

## 4. Data model & JSON schema

### Two-layer visibility model
- **Layer 0 — factory baseline (privileged, code-owned):** `RosterHeader.defaultVisibility` booleans. **Unchanged.** This is what **"Reset to Default"** applies. Not stored in the config file, not deletable, auto-covers new columns.
- **Layer 1 — user presets (config file):** each preset is a set of **per-column overrides** (desired visibility, either direction) against Layer 0, plus a nullable **`default`** pointer (the preset name to auto-apply, or absent/null for none).

### Resolved visibility (the apply computation)
For a given roster:
```
resolved(column) =
    preset.overrides[column]              if the preset names this column
    else factoryDefault(column)           // RosterHeader.determineHeaderType(column)?.defaultVisibility, else true
```
Then **drop any override naming a column not present in the current roster** (ignore, never throw). `factoryDefault` reuses the exact logic already in `Roster.resetHeaderVisibility()` / `ColumnVisibilityDialog.visibility_resetToDefault()` — extract a shared helper rather than duplicate a third copy.

### JSON schema (`view-presets.json`)
```json
{
  "version": 1,
  "default": "Swim view",
  "presets": {
    "Swim view":    { "overrides": { "Cabin": false, "Age": false } },
    "Medical view": { "overrides": { "Activity 1": false, "Activity 2": false } }
  }
}
```
- `version` (int): schema version for migrate-on-load. Current = `1`.
- `default` (string | null): name of the preset to auto-apply on roster load; `null`/absent = none.
- `presets` (object): name → `{ "overrides": { columnName: boolean } }`. Override value = desired visibility (`false` = hide, `true` = reveal).
- **Keys are the same column-name strings** that `Roster.headerVisibility` uses (so arbitrary CSV columns work with no special-casing).
- Preset insertion order should be preserved (`LinkedHashMap`) for stable UI ordering.

---

## 5. Persistence architecture

Three small, independently-testable pieces (this separation is part of the v3 hand-off). Suggested package: `com.echo.service.config` (or alongside existing services — match the team's preference; `com.echo.service` is where `ColumnSettings`/`FileHandler` live).

### 5.1 `ConfigLocation` — cross-platform path resolver
Resolves and lazily creates the app-data directory, returns the `view-presets.json` path.

| OS | Directory |
|---|---|
| macOS | `~/Library/Application Support/HappyCamper/` |
| Windows | `%APPDATA%\HappyCamper\` (via `System.getenv("APPDATA")`; fall back to `user.home` if unset) |
| Linux/other | `$XDG_CONFIG_HOME/happycamper/` if set, else `~/.config/happycamper/` |

- Detect via `System.getProperty("os.name")` (lower-cased contains "win" / "mac").
- **Use `Path`/`Files` APIs only** — never pass `File.separator` to a regex (`String.split`), per a documented Windows bug in this repo (CLAUDE.md "UI conventions / hard-won lessons").
- Create the directory if missing (`Files.createDirectories`).
- **Split pure decision from I/O (key for QA):** expose path *resolution* as a pure function — `resolve(osName, env, homeDir) → Path`, with **no** filesystem calls — and keep directory creation/writing in a separate thin method. This is what makes every per-OS branch unit-testable on any host (see §5.4).

### 5.2 `ViewPresetJson` — hand-rolled read/write
**Why hand-rolled — on its own legs, not by precedent:** (1) the pom carries **no** JSON dependency and we deliberately won't add one; (2) reflection-based JSON libraries are a liability for the v3 **TeaVM** transpile; (3) the document must drop **verbatim into web `localStorage`**. This is heart's **first** on-disk JSON — a deliberate, self-standing choice. The only real code-surface is the *reader* (the writer is trivial); that risk is owned by the malformed/escape/round-trip tests in the plan plus the corrupt-file quarantine.

- **Writer:** trivial — a small `StringBuilder` with proper string escaping. No new dependency.
- **Reader:** a small parser scoped to *exactly* this schema (an object with `version`, nullable string `default`, and a `presets` object of `{overrides: {string: bool}}`). It must be **forgiving** — see §6 error handling. Keep the schema deliberately shallow so the parser stays small.
- (Implementation note: if hand-rolling a robust reader proves disproportionately risky during execution, that is the one place to *pause and reconsider* — but the default decision is dependency-free. Do not silently add a JSON dependency without flagging it; v3/TeaVM-friendliness depends on it.)

### 5.3 `ViewPresetService` — load / mutate / persist
- Loads the store on first use (cache in memory).
- API roughly: `listPresets()`, `getDefaultName()`, `setDefault(name|null)`, `savePreset(name, overrides)`, `deletePreset(name)`, `resolveFor(roster, presetName) -> Map<String,Boolean>`.
- **Library mutations persist immediately** (save/delete/set-default write the file now — see §6 atomic write). Applying a preset to the table does **not** persist anything (it's a view action, committed via the dialog's existing Apply).
- **Production wiring is a singleton (interim, not the clean solution).** The service's *core* is fully unit-tested via a constructor-injected `Path`; production access goes through a `getInstance()` singleton purely because heart's dialog/selector code is already static-heavy (static `createSelectors`, static `cachedSettings`). This is a pragmatic match to the existing code, **not** the target design — v3 should wire the service via proper dependency injection (constructor-passed), not global state. Flagged here and in the patch record so the v3 port doesn't inherit the shortcut by default.

### 5.4 QA strategy for the cross-platform surface (the riskiest part)
Path resolution is the highest-risk failure surface here: per-OS, env-dependent, and run on machines we can't all hand-test. Maximize pre-ship confidence *and* guard future edits from becoming a liability:
- **Pure resolver + table-driven tests.** Because `resolve(...)` is pure (§5.1), unit-test every branch on any host: macOS; Windows with `APPDATA` set *and* unset (→ `user.home` fallback); Linux with `XDG_CONFIG_HOME` set *and* unset (→ `~/.config`); unusual `os.name` strings; and paths containing spaces. Treat the §5.1 path table as the test's golden expectations, so any future change that moves a path **breaks a test loudly** rather than silently shipping.
- **Lean on the existing CI matrix.** CI already runs tests on Linux (xvfb) and a headless subset on Windows. Name these tests to fall in that subset (e.g. `…ServiceTest`) so the branches **actually execute on two real OSes**, not just simulated ones. The dev machine covers macOS locally; CI's jpackage step also yields real per-OS installers for a manual smoke test before any release.
- **Never throw into startup.** If the resolved dir can't be created or written (permissions/sandbox), degrade to a safe fallback (`user.home`, or in-memory-only for the session) and log. Presets are a convenience — never a reason to fail launch.
- **Discoverability affordance (also serves QA + §6 recovery).** Surface the resolved location to the user: minimally log it once at startup; ideally a small menu action *"Open presets folder…"* (`java.awt.Desktop#open`). This lets QA verify *where* it wrote, lets users find/recover files (including a quarantined `.bak`), and turns otherwise-invisible config into something inspectable.
- **Single source of truth.** Define the app folder name (`HappyCamper` / `happycamper`) and the file name **once**, so future edits can't drift the platforms apart.

---

## 6. Apply semantics & error handling (failure-surface design)

This is where robustness lives. HappyCamper loads arbitrary CSV rosters, so **any saved view can name columns absent from the current roster.** Every path below degrades gracefully and never throws into the UI.

- **Forgiving apply:** resolve per §4, then ignore overrides for columns not in `roster.getAllHeaders()`. A preset can never put the table in a broken state. (Helpfully, both `Roster.setHeaderVisibility` and `CheckBoxSelector.setValue` already only act on keys that exist — built-in forgiveness to lean on.)
- **Default points to a deleted/absent preset:** ignore; fall back to factory defaults (i.e., behave as today).
- **Corrupt / unparseable file:** treat as "no presets" and **quarantine the bad file to `view-presets.bak`** (atomic move) before writing fresh. This self-heals (the app keeps working) while preserving the original — a conventional "move aside corrupt state" pattern (browsers/editors do the same with unreadable profiles). **But quarantine only helps if the file is findable**, so it depends on the §5.4 discoverability affordance: the existing log is **not** a user-facing channel here (it surfaces only during the import run), so don't rely on it to inform the user. For the prototype, quarantine + "Open presets folder…" is sufficient; an *active* one-time non-blocking notice naming the `.bak` is optional polish, warranted only if corruption proves common. Presets are recreatable, so silent-reset-with-recovery is an acceptable failure mode.
- **Missing file / first run:** empty store, no error.
- **Schema `version` mismatch:** run a migrate-on-load hook. Today it is a no-op (only v1 exists); the *pattern* (read old shape → upgrade in memory → write back) is a deliverable for v3. Unknown/newer `version` than supported → treat as corrupt (back up to `.bak`, start fresh) to avoid lossy misreads.
- **Atomic writes:** write to a temp file in the same directory, then `Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)` so a crash mid-save can't corrupt the live file.

---

## 7. UI design

**No new window.** Extend the existing `ColumnVisibilityDialog` (`com.echo.ui.dialog.ColumnVisibilityDialog`), which is composed from a modular **selector** system (`InputSelector<T>` subtypes assembled into a `selectors[]` array). Add a new selector, `PresetSelector`, as a first-class citizen of that system.

### 7.1 Why a purpose-built selector (not dropdown + buttons)
Presets have three live states a combobox/button-bar cannot express: which is the **launch default**, which is **currently active**, and whether the working checkboxes have **drifted** from the active preset. Rows that *show* state communicate this; an action bar does not. The repo's existing `CheckBoxSelector` already renders a labeled list of rows, so this widget shape has precedent.

### 7.2 Target layout (conceptual — refine against POC, see §7.5)
```
┌ Presets ─────────────────────────────────┐
│  default                                 │
│   ●   Aquatics view • (modified)   [×]   │  ← row highlight = active; • = drifted from active
│   ○   Activity swap view           [×]   │  ← ● = launch default (single-select radio column)
│   ○   Preference review            [×]   │
│  ──────────────────────────────────────  │
│   [ Update "Aquatics view" ] [ Revert ]  │  ← always shown; grayed/disabled until modified
│   [ + Save current columns as preset… ]  │
└──────────────────────────────────────────┘
```
- **Set as default → the radio column.** One radio per row (plus implicit "none") natively enforces "exactly one default," which *is* the `default` pointer's semantics. Reuses the `RadioButtonSelector` idiom already present in this dialog.
- **Delete → per-row `[×]`.** Unambiguous ("delete *this* one") vs a global Delete button. (Confirm-on-delete optional.)
- **Apply a preset → select/click its row.** Loads the preset's resolved map into the visibility `CheckBoxSelector` (live, like the existing Show All / Reset buttons do); the table itself changes on the dialog's existing **Apply** button — unchanged.
- **Save → `+ Save current columns as preset…`** prompts for a name (reuse an existing input selector/prompt pattern), computes overrides from the current working checkboxes vs. factory defaults, persists immediately.
- **Dirty state → marker + always-present actions.** When the working checkboxes diverge from the active preset's resolved map, mark the active row `• (modified)` and **enable** `[Update "<name>"]` / `[Revert]`.
- **No conditionally-hidden controls (locked UX rule).** Every control is always *visible*; availability is communicated by **enabled vs. grayed/disabled** state, never by appearing/disappearing. A disabled `[Update]`/`[Revert]` advertises that the action exists and signals "make a change first," giving a stable, learnable layout. (`[+ Save current columns as preset…]`, the per-row default radio, and `[×]` delete are always enabled; `[Update]`/`[Revert]` are enabled only while the active preset is modified.)

### 7.3 Collaboration / wiring
`PresetSelector` is more controller than value-holder. It collaborates with:
- the visibility **`CheckBoxSelector`** (read working state via `getValue()`; apply via `setValue(map)`),
- the **roster** (for `getAllHeaders()` + factory defaults), and
- **`ViewPresetService`** (persistence).

It may extend `InputSelector<…>` for visual framing (`createPanel()` gives the titled etched border + fixed sizing). Its "value" semantics are loose; `getValue()` can return the active preset name (or null). Set its `componentHeight` from the preset count, following `ActionButtonSelector` (`(35 * rows) + 25`).

**Dirty recomputation must be triggered on every working-state change:** manual checkbox toggles, the Show All / Hide All / Reset buttons, and preset application. Use `InputSelector.setUpdateCallback` / `notifyUpdateCallback()` to chain: when the visibility selector changes, recompute dirty and refresh the marker. (This callback chaining is the one fiddly bit — verify the `CheckBoxSelector` notifies on both user toggles and programmatic `setValue`; add a listener where it doesn't.)

### 7.4 Persistence vs. Apply (locked)
- **Library mutations** (save / delete / set-default) → **persist to the file immediately** (don't lose a saved preset by hitting Cancel).
- **View application** (applying a preset, toggling checkboxes) → affects the table only via the dialog's existing **Apply** flow (`updateSelections()`); persists nothing to the config file.

### 7.5 Lock-now vs. defer-to-POC
- **Locked now** (shapes the selector API + persistence wiring): presets are first-class stateful rows · set-default is a single-select radio column · dirty = marker + **always-present** Update/Revert (grayed until modified, never hidden) · **no conditionally-hidden controls** — availability shown via enabled/disabled state · library CRUD persists immediately while view application stays on Apply.
- **Defer to POC iteration** (cheap to tune): exact row layout, glyphs/icons, hover affordances, confirm-on-delete, and whether per-checkbox diff highlighting earns its keep.

---

## 8. Integration points (verified 2026-06-28; path:line)

> Line numbers are accurate as of writing; confirm before editing.

**Apply default preset on roster load — THE hook:**
- `com.echo.ui.MainWindow#setRoster(EnhancedRoster)` — `src/main/java/com/echo/ui/MainWindow.java:521`
  - At **line 546**, after `roster.resetHeaderVisibility()` (factory defaults applied) and **before line 552** `rosterTable.setRoster(...)`, apply the default preset's overrides (forgiving) if one is set.

**Dialog to extend:**
- `com.echo.ui.dialog.ColumnVisibilityDialog` — `src/main/java/com/echo/ui/dialog/ColumnVisibilityDialog.java`
  - `createSelectors(...)` builds `selectors[]` (currently `[ActionButtonSelector, CheckBoxSelector, RadioButtonSelector, NumberInputSelector]`) — add `PresetSelector`.
  - `visibility_resetToDefault(...)` (line 154) duplicates factory-default logic — extract a shared `factoryDefaultVisibility` helper and reuse it for preset resolution.
  - `updateSelections()` (line 231) is the existing Apply path — unchanged.

**Selector framework (for `PresetSelector`):**
- `com.echo.ui.selector.InputSelector<T>` — `src/main/java/com/echo/ui/selector/InputSelector.java`
  - `InputSelector(String title)` (l.57); abstract `T getValue()` (l.147), `void setValue(T)` (l.154), `boolean hasSelection()` (l.161), `protected void buildSelectorPanel(JPanel)` (l.169).
  - `void setUpdateCallback(Runnable)` (l.135) → calls `initializeSelector()`; `protected void notifyUpdateCallback()` (l.183).
  - `final JPanel createPanel()` (l.88) builds titled etched border + fixed size; content panel is `BoxLayout Y_AXIS`. Width fixed to `STANDARD_COMPONENT_WIDTH`; height via `componentHeight`.
- `com.echo.ui.selector.ActionButtonSelector(String title, String[] labels, Runnable[] actions)` — vertical buttons; height `(35*n)+25` (l.61). Reference for row/button rendering.
- `com.echo.ui.selector.CheckBoxSelector(String title, Map<String,Boolean> options, boolean requireSelection[, boolean includeSelectAll])` — keys are labels, values initial state, `LinkedHashMap` order preserved. `Map<String,Boolean> getValue()` returns a copy (l.111); `setValue(Map)` updates only existing keys + syncs UI (l.116).
- `com.echo.ui.selector.RadioButtonSelector<T>` — idiom to mirror for the single-select default column.

**Domain methods:**
- `com.echo.domain.RosterHeader.determineHeaderType(String)` (`RosterHeader.java:111`) → enum or `null`; `public final boolean defaultVisibility` (l.51).
- `com.echo.domain.Roster.setHeaderVisibility(String,boolean)` (l.170, only acts on existing keys), `isHeaderVisible(String)` (l.124), `getAllHeaders()` (l.221), `resetHeaderVisibility()` (l.191, the canonical factory-default applier).

**Startup (context; not the apply hook):**
- `com.echo.HappyCamper#setupApp(boolean)` (`HappyCamper.java:49`) → `createSingleWindow` (l.73) → `new MainWindow(rosterService)`. `EXIT_ON_CLOSE`, no custom `WindowListener`. (We persist eagerly on mutation, so no shutdown hook is required.)

---

## 9. Testing strategy

Per `CLAUDE.md`: `com.echo.ui.dialog.*` tests open real dialogs → `HeadlessException` without a display; CI runs them under xvfb (Linux) plus a headless-safe subset on Windows. The pure-logic pieces here are headless-safe and should carry the weight of the testing:

- **`ConfigLocation`** — unit-test OS branch selection by injecting/forcing `os.name` and env (or factor the OS string into a parameter). Assert the resolved relative path per platform. *(This is the canary's riskiest cross-platform surface — test it well.)*
- **`ViewPresetJson`** — round-trip write→read; malformed input → forgiving empty result + `.bak` behavior; string escaping (names/columns with quotes, unicode, commas); version handling.
- **`ViewPresetService`** — save/delete/set-default persistence; `resolveFor(roster, name)` correctness including overrides, factory-default inheritance for unmentioned columns, and dropping columns absent from the roster; default-points-to-deleted fallback.
- **`PresetSelector`** — follow the existing headless-safe selector pattern (`src/test/java/com/echo/ui/dialog/selector/`): instantiate, call `createPanel()`, locate components, simulate clicks, fire listeners explicitly, assert on state/callbacks (see `CheckBoxSelectorTest`, `ActionButtonSelectorTest`).
- Place new logic tests so they fall in CI's headless subset (`*SelectorTest, *FilterTest, *FeatureTest, *ServiceTest, *RosterTest`) where appropriate (e.g., name the service test `…ServiceTest`).
- Full suite: `mvn -B test` (~373 tests, seconds).

---

## 10. Distribution / versioning notes

- Because `view-presets.json` lives **outside the install dir**, re-releasing heart (frequent this month) **does not disturb existing presets** — directly addresses the "don't disincentivize upgrades" concern. No migration utility needed for version upgrades; the only cross-version concern is *schema* change, handled by §6's `version` + migrate-on-load.
- Release mechanics unchanged (`RELEASING.md`): bump pom `<version>` and commit **before** tagging; installers built only by CI (`.github/workflows/release.yml`). This feature touches no release tooling.
- **Version bumps and releases are human-initiated only.** Implementing this feature must **not** bump the pom `<version>`, tag, or cut a release. Those are a separate, later, explicitly-instructed step — an agent performs them only on direct instruction, never as an implied part of "shipping" the feature.
- **Patch record:** this is a meaningful heart code change (not docs-only), so when implemented, log it in `~/Desktop/HappyCamper-pending-patches.md` (symptoms/root-cause/diff/porting notes) per `CLAUDE.md`, to facilitate the clean v3 port. Include the v3 boundary note (§1) and the deferred follow-ups (§12).

---

## 11. v3 canary hand-off (what this teaches the clean implementation)

Three reusable, framework-agnostic patterns to graduate to v3 **without** coupling view-state to domain config:
1. `ConfigLocation` cross-platform resolver.
2. Forgiving-apply (resolve against a curated baseline; ignore unknowns; never throw).
3. Versioned, migrate-on-load JSON persistence whose document ports verbatim to web `localStorage`.

In v3 this lands as a **sibling to `ExportConfig`**, separate from `CampConfig`. Shared *plumbing* (a generic config store) is acceptable later; a shared *schema* is not.

> **Worth an ADR — in v3.** The *persistence approach itself* (the app's first cross-run persistence: hand-rolled JSON in an OS app-data dir, versioned + migrate-on-load, web-portable) is a precedent-setting, costly-to-reverse architectural choice — exactly ADR-class, and v3 already uses that convention (`docs/decisions/`, ADR-004/005). The feature-level choices here (override model, `PresetSelector`, opt-in default) are **spec-class, not ADR-class** — reversible and localized, already captured above. This session does not touch v3, so capture the ADR when the work graduates (or draft it as a separate loose artifact now to file later).

---

## 12. Deferred follow-ups (noted so they aren't lost)

- **"Explore / show-all" built-in starter preset** — an app-defined preset revealing every column in one click; high-value for discoverability (surfaces breadth without making "show everything" the default), but out of scope for the prototype.
- **Per-checkbox diff highlighting** — visually mark, in the visibility list, which columns differ from the active preset. Informative but fights the fixed-size ellipsizing label rendering; revisit only if the simple `• (modified)` marker proves insufficient.
- **Confirm-on-delete**, glyph/icon polish, hover affordances — POC-time tuning.

---

## 13. Open assumptions (flag if any are wrong)

- **Spec location:** tracked in the repo under a new `docs/design/` directory, so the design is version-controlled alongside the code it describes. `docs/` changes are low-friction per `CLAUDE.md` (docs-only changes need no patch-record logging).
- **New code package:** recommend a small dedicated package, **`com.echo.service.config`**, holding `ConfigLocation` / `ViewPresetJson` / `ViewPresetService` as one cohesive, liftable unit — the v3 canary wants to graduate the persistence bundle whole, not as classes scattered through `com.echo.service`. Stakes are low and fully reversible: Java packaging affects organization/portability, not behavior. So this is a recommendation, not a constraint; the only pull the other way is heart's currently flat `com.echo.service` (home of `ColumnSettings`/`FileHandler`). Default to the dedicated package unless you'd rather match the flat style.
- **"Default applies on roster load"** (via `MainWindow.setRoster`), not at bare app startup, since visibility is roster-scoped. If a roster is already open when a default is set, applying takes effect on the next load (acceptable for the prototype).
