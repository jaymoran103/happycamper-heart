# CLAUDE.md

HappyCamper — Java Swing desktop app for camp roster validation. This repo is the **public distribution point**: primary development happens in a separate dev repo; changes here are either cherry-picks or small direct fixes.

## Working in this repo

- Direct code fixes must be logged in `~/Desktop/HappyCamper-pending-patches.md` (symptoms, root cause, diff, porting notes) so they can be ported to the dev repo. Docs-only changes (README, demo-data, docs/) don't need logging.
- The maintainer commits and pushes; leave changes in the working tree for review.
- Releases: see `RELEASING.md`. Key rule: **bump pom `<version>` and commit BEFORE tagging** — the tag snapshot is what builds, and the MSI won't upgrade unless the version increased.

## Build & test

- `mvn -B test` — full suite (~373 tests, a few seconds)
- `mvn -B package -DskipTests` — jar + runtime deps in `target/lib`
- Installers are built ONLY by CI (`.github/workflows/release.yml`); jpackage can't cross-compile
- Java: source level 22 (pom), CI builds/bundles Temurin 25

### Test caveats
- `com.echo.ui.dialog.*` tests open real dialogs → `HeadlessException` without a display. CI runs the full suite under xvfb (Linux) and a headless-safe subset (`*SelectorTest,*FilterTest,*FeatureTest,*ServiceTest,*RosterTest`) on Windows to catch platform bugs like path-separator issues.
- Test fixtures live in `src/test/resources/testRosters/`; `automation/TestFiles.java` + `TestPreset.java` enumerate them.

## Architecture pointers

- `com.echo.domain` — `Roster`, `Camper`, `EnhancedRoster`, `RosterHeader` (enum; **its order controls column order**, and a column only appears if a feature registers the header via `getAddedHeaders()` → `roster.addHeader()`)
- `com.echo.feature` — pluggable features registered in `RosterService` constructor
- `com.echo.filter` — sidebar filters, instantiated in `FilterManager.loadFilters` keyed by `roster.hasFeature("<id>")`
- `com.echo.ui.selector` — input widgets (note: their tests are under `com.echo.ui.dialog.selector`)
- `com.echo.ui.help` — help window content assembled in `PageContentBuilder`

## UI conventions / hard-won lessons

- `InputSelector` panels are hard-fixed to `STANDARD_COMPONENT_WIDTH × componentHeight` via `DialogUtils.fixSize`. Inside fixed-size containers, FlowLayout wraps overflowing content into clipped (invisible) rows — use a constraining layout (e.g. BorderLayout CENTER) so labels ellipsize instead.
- Never pass `File.separator` to a regex API (`String.split` etc.) — it's `\` on Windows, an invalid regex. Use `File`/`Path` APIs.
- The medical feature was fully removed in 2026-06 (Patch 3); don't reintroduce references when cherry-picking older dev-repo commits.
- The June 2026 experimental sprint deliberately bends a few conventions (ordinal display, one-feature-two-columns, a filter that drives a sort, non-gated search). They're catalogued in `docs/sprint-conventions.md` — read it before "fixing" something there that looks irregular.

## Distribution surface (what users touch)

- GitHub Release assets are **versionless** (`HappyCamper-windows.msi` etc.) so `releases/latest/download/...` links in README/landing page never change
- Landing page: `docs/index.html` (GitHub Pages from `/docs`); shows version via live API fetch — no redeploy needed per release
- Demo data: `demo-data/` zipped by CI into `HappyCamper-demo-data.zip` release asset
