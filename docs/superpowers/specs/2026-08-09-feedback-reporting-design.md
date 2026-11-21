# In-App Feedback Reporting — Phase 1 Design

**Date:** 2026-08-09
**Status:** Proposed.
**Scope:** A local-only feedback channel producing a diagnostic summary. No network client, no
server, no roster data. Phase 2 (attaching a reproducible roster file) is deferred and depends on an
anonymizer that does not yet exist — its requirements are recorded at the end so they aren't lost.

## Problem

Two gaps, established with the maintainer:

1. **Staff don't report at all.** Problems are noticed and not mentioned. There is no in-app prompt,
   so reporting means remembering to raise it on WhatsApp later, unprompted.
2. **Data quirks go uncollected.** A new export format produces values the app doesn't recognize —
   a swim level of `"Adv. Beginner"`, an activity named `"Canoe Trip"` that isn't in the requirements
   list. These are often harmless and visible to the user, but each one is a rule the maintainer
   could encode. Today they are seen and forgotten.

The second gap is already 80% instrumented. `WarningManager` collects typed, structured records of
exactly these quirks — `BAD_DATA_FORMAT`, `UNKNOWN_SWIM_LEVEL`, `UNKNOWN_SWIM_ACTIVITY_FLAGGED`,
`UNKNOWN_SWIM_ACTIVITY_IGNORED`, `PROGRAM_PARSING_FAILURE`, `MISSING_FEATURE_HEADER`,
`UNMATCHED_ACTIVITY_*`, `DUPLICATE_ACTIVITY`, `CAMPER_MISSING_FIELD`. Each is a `WarningType` plus a
`String[]` of display cells (`RosterWarning.java:100-101`), and they are already shown to the user in
`WarningDialog` at the end of import (`ImportDialog.java:206-207`).

The signal exists and is displayed. It just has no way to leave the machine. This is an export
problem, not a detection problem.

## Decisions

Settled during design; recorded so they aren't relitigated.

- **No server, no token, no background traffic.** A GitHub token cannot ship in a distributed jar —
  it is extractable in seconds and GitHub's secret scanning revokes it on sight. Auto-filing issues
  would require a hosted relay. Rejected: the maintainer can reach every user on WhatsApp already.
  The app's "no internet connection" promise (`README.md`) survives intact — the only network actor
  is the user's own browser.
- **Delivery is file + WhatsApp.** Camp staff do not have GitHub accounts. A prefilled-issue URL asks
  them to sign up first, which is *more* friction aimed at exactly the users who already aren't
  reporting. The maintainer transcribes into a GitHub issue at their end.
- **Two entry points, no type selector.** The entry point implies the report kind: the warning dialog
  means "this data quirk looks wrong" and prefills the summary; the top-bar button means general
  feedback or a feature wish.
- **The report never contains roster rows.** Not opt-in, not anonymized, not at all. See Phase 2.
- **This is a diagnostic summary, not anonymization.** It has a reproduction rate of zero by
  construction, and it is described to the user in those terms. It is a stack trace, not a test case.
  The name-masking rule in §2 is hygiene on a summary — it must not be mistaken for, or grow into, an
  anonymizer.

## Architecture

One new package, `com.echo.feedback`, plus two small edits to existing UI classes. The two pure units
(`FeedbackReport`, `WarningSummarizer`) carry the logic and are headless-testable; the dialog is thin.

### 1. `FeedbackReport` — new record, `com.echo.feedback`

Immutable value object. Holds everything that will be written, already in final form:

```java
public record FeedbackReport(Kind kind,
                             String environment,
                             List<String> columnNames,
                             List<String> quirkLines,
                             String userNote) {

    public enum Kind { DATA_QUIRK, GENERAL }

    public String toPlainText() { ... }
}
```

`environment` is `HappyCamper.NAME_VERSION` plus `os.name`, `os.version`, and `java.version`.
`toPlainText()` is the single definition of the payload's wire format, so the dialog preview and the
written file cannot diverge — the dialog renders `toPlainText()` verbatim rather than re-deriving it.

A `GENERAL` report from the top bar has empty `quirkLines` and, if no roster is loaded, empty
`columnNames`. It is otherwise the same object and the same code path.

### 2. `WarningSummarizer` — new class, `com.echo.feedback`

A pure function `WarningManager → List<String>`, with no Swing and no filesystem access. This is the
unit that decides what is safe to include, so it is the one that gets thorough tests.

The rule is a **whitelist keyed on `WarningType`**: a cell appears in the output only if that type's
policy explicitly names it. Three policies cover every existing type:

| Policy | Applies to | Emits |
|---|---|---|
| `DROP_FIRST` | `UNMATCHED_ACTIVITY_SKIPPED`, `UNMATCHED_ACTIVITY_ADDED`, `DUPLICATE_ACTIVITY`, `CAMPER_MISSING_FIELD`, `PROGRAM_PARSING_FAILURE`, `UNKNOWN_SWIM_LEVEL` | cells 1..n |
| `DROP_FIRST_MASK_NAME_VALUES` | `BAD_DATA_FORMAT` | cells 1..n, with cell 2 masked when cell 1 names a name header |
| `ALL_CELLS` | `UNKNOWN_SWIM_ACTIVITY_FLAGGED`, `UNKNOWN_SWIM_ACTIVITY_IGNORED`, `MISSING_FEATURE_HEADER` | every cell |

`BAD_DATA_FORMAT` is the only type carrying both a column cell and a value cell
(`{Camper, Column, Value}`), so it is the only one the masking policy applies to. `OTHER` — the
generic demo type — is deliberately absent from the table and falls through to the fail-closed
default below.

Three details make this correct:

**Cell 0 is always dropped for camper-scoped types.** Every camper-scoped factory writes
`buildNameString(dataRow)` into index 0 (`RosterWarning.java:286-308`).

**Dropping cell 0 is not sufficient.** `create_badDataFormat` puts `dataRow.getOrDefault(column, ...)`
into cell 2 (`RosterWarning.java:191-193`). If the offending column is `First Name`, `Preferred Name`,
or `Last Name`, that *value* cell is a camper's name. `DROP_FIRST_MASK_NAME_VALUES` compares the
column cell against `RosterHeader.FIRST_NAME`, `PREFERRED_NAME`, and `LAST_NAME` — both their
`camperRosterName` and `activityRosterName` — and on a match replaces the value with its character
shape (`"Mary-Kate"` → `"Aaaa-Aaaa"`). The shape is what makes the report useful: it preserves the
punctuation and length that caused the format mismatch, without the name.

**Unknown types fail closed.** A `WarningType` with no policy entry emits its type name and zero
cells. Adding a warning type without thinking about this is then a visible gap in the report, not a
silent leak.

Output lines are truncated to 80 characters, and identical lines collapse to a single line with a
`×N` count. On a bad roster `BAD_DATA_FORMAT` can fire hundreds of times over the same malformed
column; collapsing is what keeps the report to a readable page and is the reason the maintainer sees
"this column is wrong" rather than 400 near-identical rows.

### 3. `FeedbackDelivery` — new class, `com.echo.feedback`

Three steps, each independently useful if the next fails:

1. Write `HappyCamper-feedback-YYYY-MM-DD-HHmm.txt` to the user's Downloads folder (see
   `FeedbackLocation` below), or to a path the user chose via **Save elsewhere…**.
2. Copy `toPlainText()` to the system clipboard.
3. Open `https://wa.me/<number>?text=<short prefilled message>` via `Desktop.browse`.

The file is named **feedback**, not report, deliberately: the app's *input* files are themselves
reports exported from another service, and `ReportDialog` already means the activity report. A file
called `HappyCamper-report-…` sitting in Downloads next to a CampMinder export invites exactly the
confusion this avoids.

The prefilled message is deliberately short — a sentence naming the app version and the feedback kind
— because the full text goes in the attached file, and URL length limits are unreliable across
browsers. Step 3 does not carry the payload.

Failure handling is the point of the ordering. If `Desktop.isDesktopSupported()` is false or `browse`
throws — plausible on a locked-down Linux box, and the reason this isn't a one-liner — the dialog
reports the saved file path and notes the text is already on the clipboard. The user is never left
with nothing and is never shown a stack trace.

Testing seam: the class takes a `java.util.function.Consumer<URI>` browser and a `Path` output
directory, both defaulted in production. Tests assert *what would be written and opened* without
touching the real Downloads folder or launching a browser.

**No step is automatic and no step is silent.** The report leaves the machine only when the user
presses send in WhatsApp.

### 3a. `FeedbackLocation` — new class, `com.echo.feedback`

Java has no API for the Downloads folder, so resolution is a pure function mirroring the shape of
`ConfigLocation.resolveDir` (`ConfigLocation.java:16-31`), which already solves the same OS split for
the config directory:

```java
public static Path resolveDownloadsDir(Map<String,String> env, Path home,
                                       Predicate<Path> isUsableDir)
```

Unlike `ConfigLocation.resolveDir` this takes no `osName`: the chain below is OS-independent, since
`~/Downloads` is correct on macOS, Windows, and default Linux alike, and the one Linux-specific input
is simply absent elsewhere. The existence check is injected rather than called directly so the
resolver stays pure and testable against a fictional home directory.

Chain, first hit wins:

1. `XDG_DOWNLOAD_DIR` from the environment, if set and non-blank (Linux).
2. `home/Downloads`, if it exists and is writable — correct on macOS, Windows, and default Linux.
3. `home` itself.

Deliberately **not** reading the Windows registry `KNOWNFOLDERID` for a relocated Downloads folder.
That needs JNI or a `reg query` subprocess, the default is correct for nearly every user, and step 3
catches the rest. Revisit only if a real user hits it.

Pure and headless like its `ConfigLocation` sibling, so all three branches get unit tests without
touching the filesystem.

### 4. `FeedbackDialog` — new class, `com.echo.ui.dialog`

Extends `DialogBase(parentWindow, true, width)`. Contents top to bottom:

- One line stating what this does and that nothing sends automatically.
- A read-only, scrollable `JTextArea` showing `report.toPlainText()` verbatim — the exact bytes that
  will be written. This preview is the backstop for §2: a future warning type whose policy is wrong
  is visible to the user before anything leaves.
- A `JTextArea` for the user's note, which is the only editable field.
- A destination line showing the resolved path — `Downloads/HappyCamper-feedback-2026-08-09-1432.txt`
  — abbreviated to the parent folder plus filename, following the convention `FileSelector` already
  uses for input paths (`FileSelector.java:542`).
- A link-styled **Save elsewhere…** button beside it, opening a `JFileChooser` with the filename
  pre-filled, exactly as `ReportDialog.exportCsv` does (`ReportDialog.java:154-159`). Choosing a path
  updates the destination line; cancelling leaves the Downloads default intact.
- Buttons: **Save & Open WhatsApp** / **Just Save** / **Cancel**.

The default path costs the user no clicks, which matters because this flow exists for people who
currently don't report at all — every added decision is attrition. The chooser is there for the user
who wants it and reuses the app's existing export idiom when invoked.

The preview updates when the note changes, so the shown text stays identical to the written text.

**Modality gotcha:** `WarningDialog` is application-modal *and* calls `setAlwaysOnTop(true)`
(`WarningDialog.java:38`). A child dialog opened from it must also set always-on-top, or it opens
behind its own parent and the app appears frozen. This is the one thing most likely to be missed.

### 5. Entry points — edits to `MainWindow` and `WarningDialogPanel`

**Top bar** (`MainWindow.java:220-241`): a seventh `HoverButton("Feedback")`, added after the tutorial
button. Unlike the roster-dependent controls it is **never disabled** — a user who can't get a roster
to import at all is precisely the user who most needs to report. It opens a `GENERAL` report, with
column names and quirk lines populated from `rosterService.getWarningManager()` if a roster is loaded
and empty otherwise. `MainWindow` already holds `rosterService`, so no new plumbing is required.

**Warning dialog** (`WarningDialogPanel.buildPanel`): a left-aligned link-styled button reading
`Something look wrong? Tell Jay →`, appended below the secondary explanation label.

It reports the **entire** warning log, not just the type on the current card — the user clicked
because something looked wrong, and the neighbouring cards are context, not noise. `WarningDialogPanel`
already holds `public final WarningDialog parent` (`WarningDialogPanel.java:30`); `WarningDialog`
gains a `getWarningLog()` accessor, mirroring the one already on `WarningManager`.

### 6. Contact number injection

The WhatsApp number must not be committed to this repo — it is public and scrapeable. `src/main/resources`
already has `<filtering>true</filtering>` (`pom.xml:79-84`), and `version.properties` uses it
(`version=${project.version}`). Follow that pattern:

- `src/main/resources/contact.properties` containing `whatsapp=${happycamper.whatsapp}`
- a `<happycamper.whatsapp></happycamper.whatsapp>` property in `pom.xml`, empty by default
- `.github/workflows/release.yml` passing `-Dhappycamper.whatsapp=${{ secrets.WHATSAPP_NUMBER }}`

Reading it is a separate small class, `FeedbackContact`, rather than part of `FeedbackDelivery` —
resource loading and URI construction are independently testable and share nothing. It mirrors
`HappyCamper.readVersion()` (`HappyCamper.java:21-33`), which already reads a filtered resource with
a graceful fallback.

The number is then present in release builds and absent from source and from local dev builds. When
the property resolves empty — every local build, and any release where the secret is unset —
`FeedbackDelivery` skips step 3 and the dialog offers **Just Save** only, telling the user to send the
saved file however they normally reach Jay. A missing secret degrades the feature; it does not break
the build or produce a broken link.

## Out of scope

- Any hosted service, relay, or API token.
- Any automatic, background, or silent transmission.
- Roster rows, in any form, redacted or otherwise.
- Porting NameSwap to Java. It is a Python CLI; jpackage bundles a JRE, not a Python runtime. It
  belongs on the maintainer's machine, run against files received out-of-band — which is what it was
  built for.
- Crash reporting. An uncaught exception that kills the app leaves no opportunity to open a dialog;
  that needs a handler and a spool file, and is a separate piece of work.
- Any change to what `WarningManager` collects. This spec exports the existing signal and adds no new
  warning types.

## Phase 2 — split into an unblocked half and a gated one

Planning after Phase 1 shipped established that most of the "can't reproduce" value does **not**
depend on the anonymizer. Phase 2 therefore splits:

**Phase 2a — unblocked.** Diagnose without the file, roughly 6–8 hours:

1. **An `ErrorDialog` entry point.** A Phase 1 omission. `ImportDialog` shows `ErrorDialog` when the
   roster is unusable and then returns, so the user never reaches the main window — the moment staff
   are likeliest to give up silently, and the one moment with no contextual link.
2. **An error summarizer.** The error log is a second structured signal already collected and
   currently unexportable. `RosterException.malformedRow_stringReport` alone carries file name,
   expected vs. actual cell count, and row number: a complete bug report containing no camper data.
   Two hazards specific to this channel — `DetailedRosterException.getTableData()` is a `String[][]`
   of raw CSV rows and must be excluded outright, and `create_normalWrapper` embeds an arbitrary
   exception's message, which must be treated as untrusted rather than passed through.
3. **A structural fingerprint** — information *about* the input files rather than *from* them: the
   raw header row verbatim (catching a BOM in the first cell, trailing spaces in header names,
   renamed or reordered columns), detected charset/delimiter/line-endings, row count, per-column
   fill rate and distinct-value count, and — only for low-cardinality, non-name columns — the actual
   distinct values, which is where "a quirk I could encode as a rule" lives.

That last item needs its thresholds settled before implementation: the cardinality ceiling, and a
minimum roster size below which distinct values are suppressed entirely, since on a very small
roster a low-cardinality column is a large fraction of the population.

Together these resolve encoding, BOM, delimiter, and header-shape problems with no anonymizer at all.

**Phase 2b — gated.** Attaching a roster file that reproduces the problem when run, roughly a day
once the tool exists. Two requirements, recorded here because the second is easy to miss and
expensive to retrofit:

**1. Consistent mapping across both rosters.** A camper appears in both the camper roster and the
activity roster, and the pseudonym must be identical in both or the join breaks and the app produces
entirely different warnings than the original files did.

The good news is that this is a small, fixed column swap rather than a fuzzy match. The join key is
built by `Roster.generateCamperId` (`Roster.java:382-391`):

```java
return String.format("%s_%s_%s", firstName, lastName, grade)
        .toLowerCase()
        .replace(" ", "_");
```

So the key is `FIRST_NAME` + `LAST_NAME` + `GRADE`, lowercased with spaces replaced by underscores.
`ActivityFeature.java:237-239` reads `FIRST_NAME`/`PREFERRED_NAME`/`LAST_NAME` from the activity
roster, and `RosterHeader` gives all three name columns *identical names in both files* —
`"First Name"`, `"Preferred Name"`, `"Last Name"`. One mapping applied to those columns in both files
preserves every join. Grade participates in the key but is not a name column and should be left
untouched.

**2. Defect-preserving pseudonyms.** This is the harder requirement and the one a privacy-oriented
tool will not satisfy by default.

The bugs most needing a file are join failures — `UNMATCHED_ACTIVITY_*`, the top "can't reproduce"
class — and those are almost always caused by the name string itself: a trailing space in one file,
`O'Brien` vs `OBrien`, a preferred name populated in one roster and blank in the other. An anonymizer
that maps `"Mary-Kate O'Brien "` to `"Jane Smith"` launders the defect out of existence and produces a
file that runs clean.

`generateCamperId` already normalizes two of these away: `.toLowerCase()` means casing mismatches
never break a join, and `.replace(" ", "_")` collapses internal spacing. What survives normalization —
and therefore what actually causes the join failures worth reproducing — is **punctuation and
trailing whitespace**. `"mary_o'brien_6th"` and `"mary_obrien_6th"` are different keys; so are
`"mary_smith_6th"` and `"mary_smith_6th_"`.

So the mapping must preserve length, internal punctuation, and leading and trailing whitespace.
`"Mary-Kate O'Brien "` must become something like `"Fnvz-Rylk B'Qsvra "` — same length, same hyphen,
same apostrophe, same trailing space. Capitalization matters less than the other properties given the
`toLowerCase()`, but preserving it too is free and avoids depending on a downstream normalization
detail that could change. Privacy alone is not the requirement; shape-preserving privacy is.

**No attachment seam is built in Phase 1.** Designing the interface before the anonymizer that must
satisfy it exists would be guessing. Phase 2 will know what it needs, and a small refactor then beats
a wrong abstraction now.

## Testing

Headless-safe, so they run in CI on both platforms (`*ServiceTest`/`*FeatureTest` naming per
`CLAUDE.md`):

- `WarningSummarizerTest` — one case per `WarningType`, each asserting the camper name is **absent**
  from the output rather than asserting the output equals a fixed string. A test that pins exact
  formatting passes while leaking; a test that searches for the name does not.
- The name-column masking case explicitly: a `BAD_DATA_FORMAT` warning whose column is `First Name`,
  asserting the camper's name does not appear and the shape does.
- An unknown-type case, via a warning whose type has no policy entry, asserting cells are dropped.
- Truncation and `×N` collapsing.
- `FeedbackReportTest` — `toPlainText()` round-trip, empty-quirks case, empty-note case.
- `FeedbackLocationTest` — all three branches of `resolveDownloadsDir`: `XDG_DOWNLOAD_DIR` set, a
  present `~/Downloads`, and the bare-home fallback, across macOS/Windows/Linux `os.name` values.
  Pure inputs, no filesystem, mirroring the existing `ConfigLocation` tests.
- `FeedbackDeliveryTest` — with an injected temp directory and a recording `Consumer<URI>`: asserts
  file contents, the generated filename, and that an empty contact number produces no URI.

`FeedbackDialogTest` lives in `com.echo.ui.dialog` and is therefore xvfb-only, per the existing
caveat. Keep it minimal — that the preview text equals `toPlainText()`, and that the dialog opens
above an always-on-top parent. The logic worth testing is already in the pure units.

## Porting note

This is a new feature rather than a fix, so it does not need a `HappyCamper-pending-patches.md` entry
under the usual rule. It should still be ported to the dev repo as a unit. Touch points in existing
files are small and worth listing for the port:

- `MainWindow.java` — one button in the control bar, never disabled
- `WarningDialogPanel.java` — one link-styled button in `buildPanel`
- `WarningDialog.java` — one `getWarningLog()` accessor
- `pom.xml` — one property
- `.github/workflows/release.yml` — one `-D` flag
- `src/main/resources/contact.properties` — new, filtered

Estimate: 5–7 hours, the bulk of it in `WarningSummarizer` tests and dialog layout.
