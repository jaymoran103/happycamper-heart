# In-App Feedback Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let camp staff send a diagnostic feedback file from inside HappyCamper, without accounts, servers, or roster data ever leaving their machine.

**Architecture:** A new `com.echo.feedback` package holds four pure, headless-testable units — location resolution, warning summarization, the report value object, and delivery — plus one Swing dialog in the existing `com.echo.ui.dialog` package. Two entry points (a top-bar button and a link in the warning dialog) build a `FeedbackReport` from data the app already has, show the user exactly what will be written, save it to Downloads, and open a prefilled WhatsApp link in the user's own browser. Nothing transmits automatically.

**Tech Stack:** Java 22 source level, Swing, JUnit 5, Maven resource filtering. **No new dependencies.**

**Spec:** `docs/superpowers/specs/2026-08-09-feedback-reporting-design.md`

## Global Constraints

- **Java source level 22** (`pom.xml` `<release>22</release>`). Records, switch expressions, and `var` are available. Do not use preview features.
- **No new Maven dependencies.** The whole feature uses the JDK and Swing only.
- **The report never contains roster rows.** Not opt-in, not anonymized. Camper names must never reach the output. This is the one hard requirement — every other constraint is style.
- **This is a diagnostic summary, not anonymization.** Reproduction rate is zero by construction. Do not add roster export, and do not let the masking helper grow into an anonymizer.
- **User-facing naming is "feedback", never "report".** Input files are themselves reports exported from another service, and `ReportDialog` already means the activity report. Internal class names may use `Report` where the `Feedback` prefix disambiguates.
- **Tests outside `com.echo.ui.dialog` must be headless-safe.** Anything constructing a real dialog throws `HeadlessException` without a display. Keep logic in the pure units.
- **Do NOT commit.** Per `CLAUDE.md`, the maintainer commits and pushes; leave all changes in the working tree. Each task below ends with a verification step and a suggested commit message for the maintainer's use.
- **No `HappyCamper-pending-patches.md` entry needed.** That log is for direct fixes; this is a new feature, ported to the dev repo as a unit.
- **Never pass `File.separator` to a regex API.** It is `\` on Windows and an invalid regex. Use `File`/`Path` APIs.

## File Structure

| File | Responsibility |
|---|---|
| `src/main/java/com/echo/feedback/FeedbackLocation.java` | Resolve the Downloads directory across platforms. Pure. |
| `src/main/java/com/echo/feedback/WarningSummarizer.java` | Turn a `WarningManager` into safe summary lines. Pure. **Safety-critical.** |
| `src/main/java/com/echo/feedback/FeedbackReport.java` | Immutable payload + its single wire format. Pure. |
| `src/main/java/com/echo/feedback/FeedbackContact.java` | Read the build-injected WhatsApp number; build the `wa.me` URI. |
| `src/main/java/com/echo/feedback/FeedbackDelivery.java` | Write the file, copy to clipboard, open the browser. Injectable seams. |
| `src/main/java/com/echo/ui/dialog/FeedbackDialog.java` | Preview, note field, destination line, buttons. Thin. |
| `src/main/resources/contact.properties` | Filtered resource carrying the number. New. |
| `pom.xml` | One empty-by-default property. Modified. |
| `.github/workflows/release.yml` | Pass the secret; widen the Windows test filter. Modified. |
| `src/main/java/com/echo/ui/MainWindow.java` | One top-bar button. Modified. |
| `src/main/java/com/echo/ui/dialog/WarningDialog.java` | One accessor. Modified. |
| `src/main/java/com/echo/ui/dialog/WarningDialogPanel.java` | One link-styled button. Modified. |

Tasks 1–5 are independent of the UI and can each be verified on their own. Task 6 depends on 3 and 5; Task 7 depends on 2, 3, and 6.

---

### Task 1: `FeedbackLocation` — resolve the Downloads directory

Java has no API for the Downloads folder. This mirrors the shape of `ConfigLocation.resolveDir` (`src/main/java/com/echo/service/config/ConfigLocation.java:16-31`), which already solves the same problem for the config directory.

**Deviation from spec, applied deliberately:** the spec's signature includes an `osName` parameter for symmetry with `ConfigLocation`. It is not needed — `~/Downloads` is correct on macOS, Windows, and default Linux alike, so the only OS-specific branch is the Linux `XDG_DOWNLOAD_DIR` variable, which is simply absent elsewhere. Shipping an unused parameter would be worse than the asymmetry. The existence check is injected as a `Predicate<Path>` so the function stays pure and testable without touching a real filesystem.

**Files:**
- Create: `src/main/java/com/echo/feedback/FeedbackLocation.java`
- Create: `src/main/java/com/echo/feedback/package-info.java`
- Test: `src/test/java/com/echo/feedback/FeedbackLocationTest.java`
- Modify: `.github/workflows/release.yml:48`

**Interfaces:**
- Consumes: nothing.
- Produces: `FeedbackLocation.resolveDownloadsDir(Map<String,String> env, Path home, Predicate<Path> isUsableDir) → Path` and `FeedbackLocation.downloadsDir() → Path`. Task 5 calls `downloadsDir()`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/echo/feedback/FeedbackLocationTest.java`:

```java
package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class FeedbackLocationTest {

    private static final Path HOME = Path.of("/home/user");

    /** Stands in for "this directory exists and is writable" without touching disk. */
    private static Predicate<Path> usable(Path... paths) {
        Set<Path> set = Set.of(paths);
        return set::contains;
    }

    @Test void xdgDownloadDirWinsWhenSet() {
        Path dir = FeedbackLocation.resolveDownloadsDir(
                Map.of("XDG_DOWNLOAD_DIR", "/home/user/Hämtningar"),
                HOME,
                usable(HOME.resolve("Downloads")));
        assertEquals(Path.of("/home/user/Hämtningar"), dir);
    }

    @Test void blankXdgIsIgnored() {
        Path dir = FeedbackLocation.resolveDownloadsDir(
                Map.of("XDG_DOWNLOAD_DIR", "   "),
                HOME,
                usable(HOME.resolve("Downloads")));
        assertEquals(HOME.resolve("Downloads"), dir);
    }

    @Test void usesHomeDownloadsWhenPresent() {
        Path dir = FeedbackLocation.resolveDownloadsDir(
                Map.of(), HOME, usable(HOME.resolve("Downloads")));
        assertEquals(HOME.resolve("Downloads"), dir);
    }

    @Test void fallsBackToHomeWhenDownloadsMissing() {
        Path dir = FeedbackLocation.resolveDownloadsDir(Map.of(), HOME, usable());
        assertEquals(HOME, dir);
    }

    @Test void fallsBackToHomeWhenDownloadsNotWritable() {
        // usable() models "exists AND writable" as one predicate; an unwritable
        // Downloads folder is indistinguishable from a missing one here, by design.
        Path dir = FeedbackLocation.resolveDownloadsDir(Map.of(), HOME, p -> false);
        assertEquals(HOME, dir);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=FeedbackLocationTest`
Expected: FAIL — compilation error, `package com.echo.feedback does not exist`.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/echo/feedback/package-info.java`:

```java
/**
 * Local-only feedback reporting: builds a diagnostic summary the user can review and send.
 *
 * Nothing in this package transmits data. The only network actor is the user's own browser,
 * opened at a prefilled WhatsApp link they must still press send on. Roster rows never enter
 * a report — see WarningSummarizer for the rules that keep camper names out.
 */
package com.echo.feedback;
```

Create `src/main/java/com/echo/feedback/FeedbackLocation.java`:

```java
package com.echo.feedback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Resolves the OS-native Downloads directory, where feedback files are written by default.
 *
 * Mirrors {@link com.echo.service.config.ConfigLocation} in shape: a pure resolver taking its
 * inputs as parameters, plus a production accessor that supplies the real ones. The filesystem
 * check is injected so the resolver can be tested without a real home directory.
 */
public final class FeedbackLocation {

    private FeedbackLocation() {}

    /**
     * Pure: decide the downloads directory from inputs, with no filesystem access of its own.
     *
     * @param env environment variables, consulted for the Linux XDG_DOWNLOAD_DIR
     * @param home the user's home directory
     * @param isUsableDir tests whether a candidate directory exists and can be written to
     * @return the first usable candidate; never null
     */
    public static Path resolveDownloadsDir(Map<String, String> env, Path home,
                                           Predicate<Path> isUsableDir) {
        String xdg = env.get("XDG_DOWNLOAD_DIR");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg.trim());
        }
        Path downloads = home.resolve("Downloads");
        if (isUsableDir.test(downloads)) {
            return downloads;
        }
        return home;
    }

    /** Production accessor: resolves against the real environment and filesystem. */
    public static Path downloadsDir() {
        return resolveDownloadsDir(System.getenv(),
                                   Path.of(System.getProperty("user.home")),
                                   p -> Files.isDirectory(p) && Files.isWritable(p));
    }
}
```

Note there is deliberately no Windows registry lookup for a relocated Downloads folder. That needs JNI or a `reg query` subprocess; `%USERPROFILE%\Downloads` is correct for nearly every user and the home fallback catches the rest.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B test -Dtest=FeedbackLocationTest`
Expected: PASS, 5 tests.

- [ ] **Step 5: Widen the Windows CI test filter**

Path logic is exactly what the Windows CI job exists to catch, but the current filter would skip these tests. In `.github/workflows/release.yml`, change line 48 from:

```yaml
        run: mvn -B test "-Dtest=*SelectorTest,*FilterTest,*FeatureTest,*ServiceTest,*RosterTest"
```

to:

```yaml
        run: mvn -B test "-Dtest=*SelectorTest,*FilterTest,*FeatureTest,*ServiceTest,*RosterTest,Feedback*Test,WarningSummarizerTest"
```

Both new patterns are needed: `Feedback*Test` catches `FeedbackLocationTest`, `FeedbackReportTest`, and `FeedbackDeliveryTest`, but not `WarningSummarizerTest`, which is named for the class it tests.

- [ ] **Step 6: Verify the full suite still passes and leave for review**

Run: `mvn -B test`
Expected: PASS. On a machine without a display, `com.echo.ui.dialog.*` tests fail with `HeadlessException` — that is the pre-existing caveat in `CLAUDE.md`, not a regression. Confirm the count of *other* failures is zero.

Do not commit. Suggested message for the maintainer:

```
feat(feedback): add Downloads directory resolver
- pure resolver with injected existence check, XDG then ~/Downloads then home
- widen Windows CI test filter to cover feedback tests
```

---

### Task 2: `WarningSummarizer` — turn warnings into safe summary lines

**This is the safety-critical unit.** It decides what leaves the user's machine. Read `src/main/java/com/echo/logging/RosterWarning.java` before starting — particularly `buildNameString` (lines 286-308) and `create_badDataFormat` (lines 185-195).

Two facts drive the design:

1. Every camper-scoped factory writes `buildNameString(dataRow)` into `infoCells[0]`. That cell is always a camper name and is always dropped.
2. Dropping cell 0 is **not sufficient**. `create_badDataFormat` puts the offending field's *value* into cell 2. If the offending column is `First Name`, `Preferred Name`, or `Last Name`, that value is itself a camper name.

**Files:**
- Create: `src/main/java/com/echo/feedback/WarningSummarizer.java`
- Test: `src/test/java/com/echo/feedback/WarningSummarizerTest.java`

**Interfaces:**
- Consumes: `com.echo.logging.WarningManager`, `RosterWarning`, `RosterWarning.WarningType`, `com.echo.domain.RosterHeader`.
- Produces: `WarningSummarizer.summarize(WarningManager manager) → List<String>`. Tasks 6 and 7 call this.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/echo/feedback/WarningSummarizerTest.java`:

```java
package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;

class WarningSummarizerTest {

    /** A camper row whose name must never appear in any summary line. */
    private static Map<String, String> camperRow(String first, String last) {
        Map<String, String> row = new HashMap<>();
        row.put(RosterHeader.FIRST_NAME.camperRosterName, first);
        row.put(RosterHeader.LAST_NAME.camperRosterName, last);
        row.put(RosterHeader.GRADE.camperRosterName, "6th");
        return row;
    }

    private static String joined(WarningManager manager) {
        return String.join("\n", WarningSummarizer.summarize(manager));
    }

    @Test void dropsCamperNameFromBadDataFormat() {
        WarningManager manager = new WarningManager();
        Map<String, String> row = camperRow("Wednesday", "Addams");
        row.put(RosterHeader.GRADE.camperRosterName, "6th ");
        manager.logWarning(RosterWarning.create_badDataFormat(
                row, RosterHeader.GRADE.camperRosterName, "\\d+[a-z]{2}"));

        String out = joined(manager);
        assertFalse(out.contains("Wednesday"), "camper first name leaked: " + out);
        assertFalse(out.contains("Addams"), "camper last name leaked: " + out);
        assertTrue(out.contains("6th "), "the offending value should survive: " + out);
    }

    @Test void masksValueWhenOffendingColumnIsANameColumn() {
        WarningManager manager = new WarningManager();
        Map<String, String> row = camperRow("Mary-Kate", "O'Brien");
        manager.logWarning(RosterWarning.create_badDataFormat(
                row, RosterHeader.FIRST_NAME.camperRosterName, "[A-Za-z]+"));

        String out = joined(manager);
        assertFalse(out.contains("Mary-Kate"), "name leaked through the value cell: " + out);
        assertTrue(out.contains("Aaaa-Aaaa"), "expected the shape mask: " + out);
    }

    @Test void keepsAllCellsForActivityWarnings() {
        WarningManager manager = new WarningManager();
        // true => UNKNOWN_SWIM_ACTIVITY_FLAGGED; false => ..._IGNORED. Both are ALL_CELLS.
        manager.logWarning(RosterWarning.create_unknownSwimActivityWarning("Canoe Trip", true));

        String out = joined(manager);
        assertTrue(out.contains("Canoe Trip"), "activity name should survive: " + out);
    }

    @Test void collapsesIdenticalLinesWithACount() {
        WarningManager manager = new WarningManager();
        for (int i = 0; i < 4; i++) {
            Map<String, String> row = camperRow("Camper" + i, "Surname" + i);
            row.put(RosterHeader.GRADE.camperRosterName, "6th ");
            manager.logWarning(RosterWarning.create_badDataFormat(
                    row, RosterHeader.GRADE.camperRosterName, "\\d+[a-z]{2}"));
        }

        List<String> lines = WarningSummarizer.summarize(manager);
        assertEquals(1, lines.size(), "four identical warnings should collapse: " + lines);
        assertTrue(lines.get(0).endsWith("×4"), "expected a count suffix: " + lines.get(0));
    }

    @Test void truncatesLongCells() {
        WarningManager manager = new WarningManager();
        Map<String, String> row = camperRow("Long", "Value");
        row.put(RosterHeader.GRADE.camperRosterName, "x".repeat(200));
        manager.logWarning(RosterWarning.create_badDataFormat(
                row, RosterHeader.GRADE.camperRosterName, "\\d+"));

        String out = joined(manager);
        assertFalse(out.contains("x".repeat(100)), "cell was not truncated: " + out);
        assertTrue(out.contains("…"), "expected an ellipsis marker: " + out);
    }

    @Test void emptyManagerProducesNoLines() {
        assertTrue(WarningSummarizer.summarize(new WarningManager()).isEmpty());
    }
}
```

These factory signatures are verified against `RosterWarning.java` — `create_badDataFormat` at line 196 and `create_unknownSwimActivityWarning` at line 259. Do not add new factories.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=WarningSummarizerTest`
Expected: FAIL — `cannot find symbol: class WarningSummarizer`.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/echo/feedback/WarningSummarizer.java`:

```java
package com.echo.feedback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.RosterWarning.WarningType;
import com.echo.logging.WarningManager;

/**
 * Turns a {@link WarningManager} into summary lines safe to leave the user's machine.
 *
 * The rule is a whitelist keyed on {@link WarningType}: a cell appears in the output only if its
 * type's policy explicitly allows it. A type with no policy emits its name and nothing else, so a
 * warning type added without thought shows up as a gap in the report rather than as a leak.
 *
 * This is a summary, not anonymization — it does not and cannot reproduce a problem. Do not grow
 * it into an anonymizer; see the deferred Phase 2 section of the design spec.
 */
public final class WarningSummarizer {

    /** Longest a single cell may be before it is truncated. */
    private static final int MAX_CELL_LENGTH = 80;

    private enum Policy {
        /** Cell 0 is a camper name from buildNameString; drop it, keep the rest. */
        DROP_FIRST,
        /** As DROP_FIRST, but mask the value cell when the column cell names a name column. */
        DROP_FIRST_MASK_NAME_VALUES,
        /** No camper cell at all; keep everything. */
        ALL_CELLS
    }

    private static final Map<WarningType, Policy> POLICIES = new EnumMap<>(WarningType.class);
    static {
        POLICIES.put(WarningType.UNMATCHED_ACTIVITY_SKIPPED, Policy.DROP_FIRST);
        POLICIES.put(WarningType.UNMATCHED_ACTIVITY_ADDED, Policy.DROP_FIRST);
        POLICIES.put(WarningType.DUPLICATE_ACTIVITY, Policy.DROP_FIRST);
        POLICIES.put(WarningType.CAMPER_MISSING_FIELD, Policy.DROP_FIRST);
        POLICIES.put(WarningType.PROGRAM_PARSING_FAILURE, Policy.DROP_FIRST);
        POLICIES.put(WarningType.UNKNOWN_SWIM_LEVEL, Policy.DROP_FIRST);

        // The only type carrying both a column cell and a value cell.
        POLICIES.put(WarningType.BAD_DATA_FORMAT, Policy.DROP_FIRST_MASK_NAME_VALUES);

        POLICIES.put(WarningType.UNKNOWN_SWIM_ACTIVITY_FLAGGED, Policy.ALL_CELLS);
        POLICIES.put(WarningType.UNKNOWN_SWIM_ACTIVITY_IGNORED, Policy.ALL_CELLS);
        POLICIES.put(WarningType.MISSING_FEATURE_HEADER, Policy.ALL_CELLS);
        // WarningType.OTHER is deliberately absent: it falls through to the fail-closed default.
    }

    /** Column names, lowercased, whose values are camper names in either input roster. */
    private static final Set<String> NAME_COLUMNS =
            Stream.of(RosterHeader.FIRST_NAME, RosterHeader.PREFERRED_NAME, RosterHeader.LAST_NAME)
                  .flatMap(h -> Stream.of(h.camperRosterName, h.activityRosterName, h.standardName))
                  .filter(n -> n != null && !n.isBlank())
                  .map(n -> n.toLowerCase())
                  .collect(Collectors.toUnmodifiableSet());

    private WarningSummarizer() {}

    /**
     * Summarizes every warning in the log, one line per distinct line, with repeats collapsed.
     *
     * @param manager the log to summarize; may be empty
     * @return summary lines, never null, containing no camper names
     */
    public static List<String> summarize(WarningManager manager) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<WarningType, ? extends List<RosterWarning>> entry
                : manager.getWarningLog().entrySet()) {

            // LinkedHashMap keeps first-seen order while counting duplicates.
            LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
            for (RosterWarning warning : entry.getValue()) {
                counts.merge(formatLine(entry.getKey(), warning), 1, Integer::sum);
            }
            counts.forEach((line, count) -> lines.add(count == 1 ? line : line + "  ×" + count));
        }
        return lines;
    }

    private static String formatLine(WarningType type, RosterWarning warning) {
        Policy policy = POLICIES.get(type);
        if (policy == null) {
            return type.name();   // fail closed: no policy, no cells
        }
        List<String> cells = safeCells(warning.getDisplayData(), policy);
        if (cells.isEmpty()) {
            return type.name();
        }
        return type.name() + "  " + String.join(" | ", cells);
    }

    private static List<String> safeCells(String[] data, Policy policy) {
        if (data == null || data.length == 0) {
            return List.of();
        }
        List<String> cells = switch (policy) {
            case ALL_CELLS -> new ArrayList<>(Arrays.asList(data));
            case DROP_FIRST, DROP_FIRST_MASK_NAME_VALUES ->
                    new ArrayList<>(Arrays.asList(data).subList(1, data.length));
        };

        // BAD_DATA_FORMAT cells after the drop are [column, value]; the value is a camper
        // name whenever the column is a name column, so mask it to its character shape.
        if (policy == Policy.DROP_FIRST_MASK_NAME_VALUES
                && cells.size() >= 2
                && cells.get(0) != null
                && NAME_COLUMNS.contains(cells.get(0).toLowerCase())) {
            cells.set(1, maskShape(cells.get(1)));
        }

        return cells.stream().map(WarningSummarizer::truncate).toList();
    }

    /**
     * Replaces letters and digits with placeholders while preserving length, punctuation, and
     * spacing — enough to see why a value failed a format check, without revealing the value.
     */
    private static String maskShape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder shape = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c))      shape.append('A');
            else if (Character.isLowerCase(c)) shape.append('a');
            else if (Character.isDigit(c))     shape.append('9');
            else                               shape.append(c);
        }
        return shape.toString();
    }

    private static String truncate(String cell) {
        if (cell == null) {
            return "";
        }
        return cell.length() <= MAX_CELL_LENGTH
                ? cell
                : cell.substring(0, MAX_CELL_LENGTH - 1) + "…";
    }
}
```

If `WarningManager.getWarningLog()` returns `Map<WarningType, ArrayList<RosterWarning>>`, the `? extends List<RosterWarning>` wildcard in the for-loop will not compile against it directly. Change the loop variable to `Map.Entry<WarningType, ArrayList<RosterWarning>>` and import `java.util.ArrayList` accordingly — match the real signature at `WarningManager.java:73`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B test -Dtest=WarningSummarizerTest`
Expected: PASS, 6 tests.

If `masksValueWhenOffendingColumnIsANameColumn` fails, check that `create_badDataFormat` is being passed the *camper-roster* column name and that `NAME_COLUMNS` contains it lowercased. Do not "fix" the test by weakening the assertion — a failure here is a real leak.

- [ ] **Step 5: Verify the full suite and leave for review**

Run: `mvn -B test`
Expected: PASS apart from the known headless dialog caveat.

Suggested message:

```
feat(feedback): summarize warning log without camper names
- whitelist policy per WarningType, unknown types fail closed
- mask value cell when the offending column is a name column
```

---

### Task 3: `FeedbackReport` — the payload and its single wire format

**Files:**
- Create: `src/main/java/com/echo/feedback/FeedbackReport.java`
- Test: `src/test/java/com/echo/feedback/FeedbackReportTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces:
  - `FeedbackReport.Kind` — enum, values `DATA_QUIRK` and `GENERAL`
  - `new FeedbackReport(Kind kind, String environment, List<String> columnNames, List<String> quirkLines, String userNote)`
  - `report.toPlainText() → String`
  - `report.withNote(String note) → FeedbackReport`
  - `FeedbackReport.environmentString() → String` (static)

  Task 5 calls `toPlainText()`; Task 6 calls `withNote` and `toPlainText`; Task 7 calls the constructor and `environmentString()`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/echo/feedback/FeedbackReportTest.java`:

```java
package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.echo.feedback.FeedbackReport.Kind;

class FeedbackReportTest {

    private static FeedbackReport full() {
        return new FeedbackReport(
                Kind.DATA_QUIRK,
                "HappyCamper 2.5.0 · Windows 11 · Java 25",
                List.of("First Name", "Last Name", "Grade"),
                List.of("UNKNOWN_SWIM_LEVEL  Adv. Beginner", "BAD_DATA_FORMAT  Grade | 6th   ×4"),
                "Grade column is blank for the whole Wolf cabin");
    }

    @Test void includesEverySection() {
        String text = full().toPlainText();
        assertTrue(text.contains("HappyCamper 2.5.0"), text);
        assertTrue(text.contains("First Name, Last Name, Grade"), text);
        assertTrue(text.contains("UNKNOWN_SWIM_LEVEL"), text);
        assertTrue(text.contains("Wolf cabin"), text);
    }

    @Test void statesThatNamesWereRemoved() {
        assertTrue(full().toPlainText().contains("camper names removed"), "must disclose redaction");
    }

    @Test void omitsQuirkSectionWhenThereAreNone() {
        FeedbackReport report = new FeedbackReport(
                Kind.GENERAL, "env", List.of(), List.of(), "I wish it could sort by cabin");
        String text = report.toPlainText();
        assertFalse(text.contains("camper names removed"), text);
        assertTrue(text.contains("I wish it could sort by cabin"), text);
    }

    @Test void omitsNoteSectionWhenBlank() {
        FeedbackReport report = new FeedbackReport(
                Kind.GENERAL, "env", List.of(), List.of(), "   ");
        assertFalse(report.toPlainText().contains("Note from user"), report.toPlainText());
    }

    @Test void withNoteReplacesOnlyTheNote() {
        FeedbackReport updated = full().withNote("different note");
        assertEquals("different note", updated.userNote());
        assertEquals(full().quirkLines(), updated.quirkLines());
        assertTrue(updated.toPlainText().contains("different note"));
    }

    @Test void environmentStringNamesTheApp() {
        assertTrue(FeedbackReport.environmentString().contains("HappyCamper"),
                   FeedbackReport.environmentString());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=FeedbackReportTest`
Expected: FAIL — `cannot find symbol: class FeedbackReport`.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/echo/feedback/FeedbackReport.java`:

```java
package com.echo.feedback;

import java.util.List;

import com.echo.HappyCamper;

/**
 * The complete contents of one feedback submission, already in final form.
 *
 * {@link #toPlainText()} is the single definition of the wire format: the dialog preview renders
 * it verbatim and the saved file contains exactly it, so what the user reviews cannot drift from
 * what is written.
 *
 * @param kind which entry point produced this, used only to word the summary line
 * @param environment app version and platform, from {@link #environmentString()}
 * @param columnNames names of the loaded roster's columns, feature-generated ones included —
 *                    names of columns, never their contents
 * @param quirkLines summary lines from {@link WarningSummarizer}, already free of camper names
 * @param userNote whatever the user typed; may be blank
 */
public record FeedbackReport(Kind kind,
                             String environment,
                             List<String> columnNames,
                             List<String> quirkLines,
                             String userNote) {

    /** Which entry point produced the report. The entry point implies the kind; there is no picker. */
    public enum Kind { DATA_QUIRK, GENERAL }

    public FeedbackReport {
        columnNames = List.copyOf(columnNames);
        quirkLines = List.copyOf(quirkLines);
        userNote = userNote == null ? "" : userNote;
    }

    /** Returns a copy carrying a different note, for live preview as the user types. */
    public FeedbackReport withNote(String note) {
        return new FeedbackReport(kind, environment, columnNames, quirkLines, note);
    }

    /** App version and platform, e.g. {@code "HappyCamper 2.5.0 · Windows 11 · Java 25"}. */
    public static String environmentString() {
        return String.format("%s · %s %s · Java %s",
                HappyCamper.NAME_VERSION,
                System.getProperty("os.name", "unknown OS"),
                System.getProperty("os.version", ""),
                System.getProperty("java.version", "unknown"));
    }

    /** The exact text that gets previewed, copied, and written to disk. */
    public String toPlainText() {
        StringBuilder text = new StringBuilder();
        text.append(environment).append("\n");

        if (!columnNames.isEmpty()) {
            text.append("\nRoster columns: ").append(String.join(", ", columnNames)).append("\n");
        }

        if (!quirkLines.isEmpty()) {
            text.append("\nData quirks (").append(quirkLines.size())
                .append("), camper names removed:\n");
            for (String line : quirkLines) {
                text.append("  ").append(line).append("\n");
            }
        }

        if (!userNote.isBlank()) {
            text.append("\nNote from user:\n  ")
                .append(userNote.strip().replace("\n", "\n  "))
                .append("\n");
        }

        return text.toString();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B test -Dtest=FeedbackReportTest`
Expected: PASS, 6 tests.

- [ ] **Step 5: Verify the full suite and leave for review**

Run: `mvn -B test`

Suggested message:

```
feat(feedback): add FeedbackReport payload and wire format
- single toPlainText definition shared by preview, clipboard, and file
```

---

### Task 4: `FeedbackContact` — build-injected WhatsApp number

The number must not be committed to this repo, which is public and scrapeable. `src/main/resources` already has `<filtering>true</filtering>` (`pom.xml:79-84`) and `version.properties` uses it, so follow that pattern exactly. The reading code mirrors `HappyCamper.readVersion()` (`HappyCamper.java:21-33`).

**Files:**
- Create: `src/main/resources/contact.properties`
- Create: `src/main/java/com/echo/feedback/FeedbackContact.java`
- Modify: `pom.xml` (add one property)
- Modify: `.github/workflows/release.yml:110`
- Test: `src/test/java/com/echo/feedback/FeedbackContactTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `FeedbackContact.whatsappNumber() → String` (empty when unset)
  - `FeedbackContact.whatsappUri(String number, String message) → Optional<URI>` (static, pure)

  Task 5 calls both.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/echo/feedback/FeedbackContactTest.java`:

```java
package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class FeedbackContactTest {

    @Test void buildsAWaMeUriWithAnEncodedMessage() {
        Optional<URI> uri = FeedbackContact.whatsappUri("15551234567", "HappyCamper 2.5.0 feedback");
        assertTrue(uri.isPresent());
        assertEquals("wa.me", uri.get().getHost());
        assertTrue(uri.get().toString().contains("15551234567"), uri.get().toString());
        assertTrue(uri.get().toString().contains("HappyCamper%202.5.0"), uri.get().toString());
    }

    @Test void noUriWhenNumberIsBlank() {
        assertTrue(FeedbackContact.whatsappUri("", "msg").isEmpty());
        assertTrue(FeedbackContact.whatsappUri("   ", "msg").isEmpty());
        assertTrue(FeedbackContact.whatsappUri(null, "msg").isEmpty());
    }

    @Test void noUriWhenFilteringDidNotRun() {
        // An unfiltered resource leaves the literal Maven placeholder behind.
        assertTrue(FeedbackContact.whatsappUri("${happycamper.whatsapp}", "msg").isEmpty());
    }

    @Test void stripsFormattingFromTheNumber() {
        Optional<URI> uri = FeedbackContact.whatsappUri("+1 (555) 123-4567", "msg");
        assertTrue(uri.isPresent());
        assertTrue(uri.get().toString().contains("15551234567"), uri.get().toString());
    }

    @Test void numberIsBlankInLocalBuilds() {
        // The pom leaves the property empty; only CI injects a real number.
        assertEquals("", FeedbackContact.whatsappNumber());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=FeedbackContactTest`
Expected: FAIL — `cannot find symbol: class FeedbackContact`.

- [ ] **Step 3: Add the filtered resource and the pom property**

Create `src/main/resources/contact.properties`:

```properties
whatsapp=${happycamper.whatsapp}
```

In `pom.xml`, inside the existing `<properties>` block, add:

```xml
        <!-- Injected by CI from a repository secret so the number stays out of this public repo.
             Empty in local builds: the feedback dialog then offers "Just Save" only. -->
        <happycamper.whatsapp></happycamper.whatsapp>
```

In `.github/workflows/release.yml`, change line 110 from:

```yaml
        run: mvn -B package -DskipTests   # tests already ran in the test job
```

to:

```yaml
        run: mvn -B package -DskipTests "-Dhappycamper.whatsapp=${{ secrets.WHATSAPP_NUMBER }}"
```

If the `WHATSAPP_NUMBER` secret is unset, the expression expands to an empty string and the build still succeeds — the feature degrades to save-only rather than breaking.

- [ ] **Step 4: Write the implementation**

Create `src/main/java/com/echo/feedback/FeedbackContact.java`:

```java
package com.echo.feedback;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

/**
 * Supplies the maintainer's WhatsApp number, injected at build time.
 *
 * The number is deliberately absent from source: this repository is public. CI passes it via
 * -Dhappycamper.whatsapp from a secret, Maven resource filtering writes it into
 * contact.properties, and local builds simply get an empty value.
 */
public final class FeedbackContact {

    private static final String RESOURCE = "/contact.properties";

    private FeedbackContact() {}

    /** The configured number, or an empty string when this build has none. */
    public static String whatsappNumber() {
        try (InputStream in = FeedbackContact.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return "";
            }
            Properties props = new Properties();
            props.load(in);
            String number = props.getProperty("whatsapp", "");
            return number == null ? "" : number.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Builds a wa.me link carrying a short prefilled message.
     *
     * The message is deliberately short — the full text goes in the attached file, and URL length
     * limits vary by browser. This link does not carry the payload.
     *
     * @return the URI, or empty when no usable number is configured
     */
    public static Optional<URI> whatsappUri(String number, String message) {
        if (number == null || number.isBlank() || number.startsWith("${")) {
            return Optional.empty();
        }
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        String encoded = URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8)
                                   .replace("+", "%20");
        return Optional.of(URI.create("https://wa.me/" + digits + "?text=" + encoded));
    }
}
```

`URLEncoder` produces `+` for spaces, which is correct for form bodies but not for a query value WhatsApp will display literally — hence the replacement to `%20`, which the test asserts.

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -B test -Dtest=FeedbackContactTest`
Expected: PASS, 5 tests.

- [ ] **Step 6: Verify the full suite and leave for review**

Run: `mvn -B test`

Suggested message:

```
feat(feedback): inject WhatsApp contact at build time
- filtered contact.properties, empty by default, CI supplies the secret
- keeps the number out of this public repo
```

---

### Task 5: `FeedbackDelivery` — write the file, copy, and open the link

**Files:**
- Create: `src/main/java/com/echo/feedback/FeedbackDelivery.java`
- Test: `src/test/java/com/echo/feedback/FeedbackDeliveryTest.java`

**Interfaces:**
- Consumes: `FeedbackLocation.downloadsDir()` (Task 1), `FeedbackReport.toPlainText()` (Task 3), `FeedbackContact.whatsappNumber()` / `whatsappUri(...)` (Task 4).
- Produces:
  - `FeedbackDelivery.defaultFileName(LocalDateTime when) → String` (static)
  - `new FeedbackDelivery(Consumer<URI> browser, Consumer<String> clipboard)`
  - `FeedbackDelivery.production() → FeedbackDelivery` (static)
  - `delivery.save(FeedbackReport report, Path target) → Path`
  - `delivery.openWhatsApp(FeedbackReport report) → boolean`

  Task 6 calls `production()`, `defaultFileName`, `save`, and `openWhatsApp`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/echo/feedback/FeedbackDeliveryTest.java`:

```java
package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.feedback.FeedbackReport.Kind;

class FeedbackDeliveryTest {

    private final List<URI> opened = new ArrayList<>();
    private final List<String> copied = new ArrayList<>();

    private FeedbackDelivery delivery() {
        return new FeedbackDelivery(opened::add, copied::add);
    }

    private static FeedbackReport report() {
        return new FeedbackReport(Kind.DATA_QUIRK, "HappyCamper 2.5.0",
                                  List.of("Grade"), List.of("BAD_DATA_FORMAT  Grade | 6th "),
                                  "grade looks wrong");
    }

    @Test void fileNameIsFeedbackNotReport() {
        String name = FeedbackDelivery.defaultFileName(LocalDateTime.of(2026, 8, 9, 14, 32));
        assertEquals("HappyCamper-feedback-2026-08-09-1432.txt", name);
        assertFalse(name.contains("report"), "input files are already called reports: " + name);
    }

    @Test void savesTheExactPreviewText(@TempDir Path dir) throws IOException {
        Path written = delivery().save(report(), dir.resolve("out.txt"));
        assertEquals(report().toPlainText(), Files.readString(written));
    }

    @Test void savingAlsoCopiesToClipboard(@TempDir Path dir) throws IOException {
        delivery().save(report(), dir.resolve("out.txt"));
        assertEquals(List.of(report().toPlainText()), copied);
    }

    @Test void savingCreatesMissingParentDirectories(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("nested").resolve("deeper").resolve("out.txt");
        assertTrue(Files.exists(delivery().save(report(), target)));
    }

    @Test void noBrowserOpensWithoutAConfiguredNumber() {
        // Local builds leave the number empty, so there is nothing to open.
        assertFalse(delivery().openWhatsApp(report()));
        assertTrue(opened.isEmpty());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=FeedbackDeliveryTest`
Expected: FAIL — `cannot find symbol: class FeedbackDelivery`.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/echo/feedback/FeedbackDelivery.java`:

```java
package com.echo.feedback;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Saves a feedback file, copies its text to the clipboard, and opens a prefilled WhatsApp link.
 *
 * The three steps are ordered so each is useful if the next fails: the file exists even when the
 * clipboard is unavailable, and the text is on the clipboard even when no browser can be opened.
 * The user is never left with nothing.
 *
 * Nothing here transmits. Opening the link hands off to the user's browser; they still have to
 * attach the file and press send.
 */
public final class FeedbackDelivery {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private final Consumer<URI> browser;
    private final Consumer<String> clipboard;

    /** Seams exist so tests can assert what would be opened and copied without doing either. */
    public FeedbackDelivery(Consumer<URI> browser, Consumer<String> clipboard) {
        this.browser = browser;
        this.clipboard = clipboard;
    }

    /** Wires up the real browser and system clipboard, each degrading quietly if unavailable. */
    public static FeedbackDelivery production() {
        return new FeedbackDelivery(FeedbackDelivery::browseQuietly,
                                    FeedbackDelivery::copyQuietly);
    }

    /**
     * The suggested file name. Called "feedback" rather than "report" on purpose: the app's input
     * files are themselves reports exported from another service, and one of those may well be
     * sitting in the same folder.
     */
    public static String defaultFileName(LocalDateTime when) {
        return "HappyCamper-feedback-" + STAMP.format(when) + ".txt";
    }

    /** The default full path, in the user's Downloads folder. */
    public static Path defaultTarget(LocalDateTime when) {
        return FeedbackLocation.downloadsDir().resolve(defaultFileName(when));
    }

    /**
     * Writes the report and copies its text to the clipboard.
     *
     * @return the path actually written
     * @throws IOException if the file cannot be written; the caller shows this to the user
     */
    public Path save(FeedbackReport report, Path target) throws IOException {
        String text = report.toPlainText();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(target, text, StandardCharsets.UTF_8);
        clipboard.accept(text);
        return target;
    }

    /**
     * Opens a prefilled WhatsApp link, if this build has a number configured.
     *
     * @return true if a link was opened; false when no number is configured, in which case the
     *         caller should tell the user to send the saved file however they normally would
     */
    public boolean openWhatsApp(FeedbackReport report) {
        String summary = switch (report.kind()) {
            case DATA_QUIRK -> "HappyCamper feedback: something looks wrong with my roster data";
            case GENERAL -> "HappyCamper feedback";
        };
        Optional<URI> uri = FeedbackContact.whatsappUri(FeedbackContact.whatsappNumber(),
                                                        summary + " (" + report.environment() + ")");
        uri.ifPresent(browser);
        return uri.isPresent();
    }

    private static void browseQuietly(URI uri) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception ignored) {
            // Locked-down desktops throw here. The file is already saved and the text copied,
            // so the user still has everything they need; the dialog says where it went.
        }
    }

    private static void copyQuietly(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                   .setContents(new StringSelection(text), null);
        } catch (Exception ignored) {
            // Headless or clipboard-less environments. Saving already succeeded.
        }
    }
}
```

`openWhatsApp` returns `true` when a link was *dispatched*, not when the browser actually opened — `browseQuietly` swallows failures by design, because at that point the file is already saved and the caller has nothing better to offer the user.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B test -Dtest=FeedbackDeliveryTest`
Expected: PASS, 5 tests.

- [ ] **Step 5: Verify the full suite and leave for review**

Run: `mvn -B test`

Suggested message:

```
feat(feedback): save feedback file, copy text, open WhatsApp link
- three independent steps so a failure still leaves the user the file
- filename is feedback-, never report-
```

---

### Task 6: `FeedbackDialog` — review, note, destination, send

Thin by design: the logic is already tested in Tasks 1–5. This task wires it to Swing.

**Files:**
- Create: `src/main/java/com/echo/ui/dialog/FeedbackDialog.java`
- Test: `src/test/java/com/echo/ui/dialog/FeedbackDialogTest.java`

Read `DialogBase` (`src/main/java/com/echo/ui/dialog/DialogBase.java:29-70`) and `DialogConstants` before starting. `DialogBase(Window, boolean modal, int width)` builds a `BorderLayout` with a main panel in `CENTER` and a bottom panel in `PAGE_END`; get the former with `getMainPanel()`.

**Interfaces:**
- Consumes: `FeedbackReport` (Task 3), `FeedbackDelivery` (Task 5).
- Produces: `new FeedbackDialog(Window parent, FeedbackReport report)` and `dialog.showDialog()`. Task 7 calls both.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/echo/ui/dialog/FeedbackDialogTest.java`. This lives in `com.echo.ui.dialog` and therefore only runs under a display (xvfb on CI) — keep it minimal, since the logic worth testing lives in the pure units:

```java
package com.echo.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;

import com.echo.feedback.FeedbackReport;
import com.echo.feedback.FeedbackReport.Kind;

class FeedbackDialogTest {

    private static FeedbackReport report() {
        return new FeedbackReport(Kind.DATA_QUIRK, "HappyCamper test",
                                  List.of("Grade"), List.of("BAD_DATA_FORMAT  Grade | 6th "), "");
    }

    @Test void previewShowsExactlyWhatWouldBeWritten() {
        JFrame parent = new JFrame();
        FeedbackDialog dialog = new FeedbackDialog(parent, report());
        assertEquals(report().toPlainText(), dialog.previewText());
        dialog.dispose();
        parent.dispose();
    }

    @Test void previewFollowsTheNoteAsItChanges() {
        JFrame parent = new JFrame();
        FeedbackDialog dialog = new FeedbackDialog(parent, report());
        dialog.setNoteForTesting("cabin 4 grades are blank");
        assertTrue(dialog.previewText().contains("cabin 4 grades are blank"), dialog.previewText());
        dialog.dispose();
        parent.dispose();
    }

    @Test void staysAboveAnAlwaysOnTopParent() {
        // WarningDialog sets alwaysOnTop; a child that does not would open behind it and
        // look like a freeze.
        JFrame parent = new JFrame();
        parent.setAlwaysOnTop(true);
        FeedbackDialog dialog = new FeedbackDialog(parent, report());
        assertTrue(dialog.isAlwaysOnTop());
        dialog.dispose();
        parent.dispose();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=FeedbackDialogTest`
Expected: FAIL — `cannot find symbol: class FeedbackDialog`. On a headless machine it fails with `HeadlessException` instead, which is also acceptable at this step; the real verification is Step 4.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/echo/ui/dialog/FeedbackDialog.java`:

```java
package com.echo.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.echo.feedback.FeedbackDelivery;
import com.echo.feedback.FeedbackReport;
import com.echo.ui.elements.HoverButton;

/**
 * Shows the user exactly what a feedback submission contains, takes their note, and saves it.
 *
 * The preview is the backstop for WarningSummarizer: if a future warning type is summarized
 * wrongly, the user sees it here before anything is written or sent.
 */
public class FeedbackDialog extends DialogBase {

    private final FeedbackDelivery delivery = FeedbackDelivery.production();
    private final LocalDateTime openedAt = LocalDateTime.now();

    private FeedbackReport report;
    private Path target;

    private final JTextArea previewArea = new JTextArea();
    private final JTextArea noteArea = new JTextArea(3, 20);
    private final JLabel destinationLabel = new JLabel();

    public FeedbackDialog(Window parentWindow, FeedbackReport initialReport) {
        super(parentWindow, true, DialogConstants.DIALOG_WIDTH_STANDARD);
        this.report = initialReport;
        this.target = FeedbackDelivery.defaultTarget(openedAt);

        setTitle("Send feedback");
        buildContents();
        refreshPreview();
        refreshDestination();

        // WarningDialog sets alwaysOnTop; without this the dialog opens behind its own parent.
        setAlwaysOnTop(true);
        setLocationRelativeTo(parentWindow);
    }

    /** Test seam: the exact text that would be written. */
    String previewText() {
        return previewArea.getText();
    }

    /** Test seam: simulates the user typing a note. */
    void setNoteForTesting(String note) {
        noteArea.setText(note);
    }

    private void buildContents() {
        JPanel panel = getMainPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DialogConstants.DIALOG_COLOR_MAIN);

        panel.add(wrap(new JLabel(
                "<html>This saves a file describing the problem. Nothing is sent automatically — "
              + "you choose whether to send it.</html>")));

        previewArea.setEditable(false);
        previewArea.setLineWrap(false);
        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setPreferredSize(new Dimension(
                DialogConstants.COMPONENT_WIDTH_STANDARD, DialogConstants.COMPONENT_HEIGHT_LARGE));
        panel.add(wrap(previewScroll));

        panel.add(wrap(new JLabel("Anything you'd like to add?")));
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { refreshPreview(); }
            public void removeUpdate(DocumentEvent e)  { refreshPreview(); }
            public void changedUpdate(DocumentEvent e) { refreshPreview(); }
        });
        panel.add(wrap(new JScrollPane(noteArea)));

        JPanel destinationRow = new JPanel(new BorderLayout(
                DialogConstants.COMPONENT_SPACING, 0));
        destinationRow.setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        destinationRow.add(destinationLabel, BorderLayout.CENTER);
        JButton chooseButton = new HoverButton("Save elsewhere…");
        chooseButton.addActionListener(e -> chooseTarget());
        destinationRow.add(chooseButton, BorderLayout.EAST);
        panel.add(wrap(destinationRow));

        JButton sendButton = new HoverButton("Save & Open WhatsApp");
        sendButton.addActionListener(e -> saveThen(true));
        JButton saveButton = new HoverButton("Just Save");
        saveButton.addActionListener(e -> saveThen(false));
        JButton cancelButton = new HoverButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        buttons.add(sendButton);
        buttons.add(saveButton);
        buttons.add(cancelButton);
        panel.add(wrap(buttons));
    }

    /**
     * BoxLayout stretches children to the container width and lets them keep their own
     * alignment; wrapping in a BorderLayout panel keeps labels from being clipped, per the
     * fixed-size-container lesson in CLAUDE.md.
     */
    private JPanel wrap(java.awt.Component component) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        holder.setBorder(BorderFactory.createEmptyBorder(
                DialogConstants.COMPONENT_PADDING, DialogConstants.COMPONENT_PADDING,
                DialogConstants.COMPONENT_PADDING, DialogConstants.COMPONENT_PADDING));
        holder.add(component, BorderLayout.CENTER);
        return holder;
    }

    private void refreshPreview() {
        report = report.withNote(noteArea.getText());
        previewArea.setText(report.toPlainText());
        previewArea.setCaretPosition(0);
    }

    private void refreshDestination() {
        Path parent = target.getParent();
        String shown = parent == null
                ? target.getFileName().toString()
                : parent.getFileName() + File.separator + target.getFileName();
        destinationLabel.setText("Saves to: " + shown);
    }

    private void chooseTarget() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(target.toFile());
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            target = chooser.getSelectedFile().toPath();
            refreshDestination();
        }
    }

    private void saveThen(boolean openWhatsApp) {
        try {
            Path written = delivery.save(report, target);
            boolean opened = openWhatsApp && delivery.openWhatsApp(report);
            String message = opened
                    ? "Saved to:\n" + written + "\n\nWhatsApp is opening — attach this file and send it."
                    : "Saved to:\n" + written
                      + "\n\nThe text is also on your clipboard. Send the file to Jay however you normally would.";
            JOptionPane.showMessageDialog(this, message, "Feedback saved",
                                          JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save the file: " + ex.getMessage()
                  + "\n\nTry \"Save elsewhere…\" and pick a different folder.",
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showDialog() {
        setVisible(true);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B test -Dtest=FeedbackDialogTest`
Expected: PASS, 3 tests — **on a machine with a display.** Headless, these fail with `HeadlessException`, which is the documented caveat. If you are headless, confirm instead that `mvn -B test-compile` succeeds and let CI run them.

- [ ] **Step 5: Verify the full suite and leave for review**

Run: `mvn -B test`

Suggested message:

```
feat(feedback): add review-and-send dialog
- preview renders the exact text that gets written
- Downloads by default, chooser one click away
- always-on-top so it clears the modal warning dialog
```

---

### Task 7: Entry points — top-bar button and warning-dialog link

Two ways in, and the entry point implies the kind, so the dialog needs no type picker.

**Files:**
- Modify: `src/main/java/com/echo/ui/MainWindow.java` (button field ~line 89, creation ~line 227, panel add ~line 241, new handler near `handleTutorial` at line 702)
- Modify: `src/main/java/com/echo/ui/dialog/WarningDialog.java` (add accessor)
- Modify: `src/main/java/com/echo/ui/dialog/WarningDialogPanel.java` (add link in `buildPanel`, ~line 95)
- Test: `src/test/java/com/echo/feedback/FeedbackEntryPointTest.java`

**Interfaces:**
- Consumes: `WarningSummarizer.summarize` (Task 2), `FeedbackReport` + `environmentString` (Task 3), `FeedbackDialog` (Task 6).
- Produces: `WarningDialog.getWarningLog() → Map<WarningType, ArrayList<RosterWarning>>`.

- [ ] **Step 1: Write the failing test**

The button wiring itself needs a display, but the report-building logic does not — and that is the part with behavior worth pinning. Create `src/test/java/com/echo/feedback/FeedbackEntryPointTest.java`:

```java
package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.echo.domain.RosterHeader;
import com.echo.feedback.FeedbackReport.Kind;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;

class FeedbackEntryPointTest {

    @Test void quirkReportCarriesTheSummarizedWarnings() {
        WarningManager manager = new WarningManager();
        Map<String, String> row = new HashMap<>();
        row.put(RosterHeader.FIRST_NAME.camperRosterName, "Wednesday");
        row.put(RosterHeader.LAST_NAME.camperRosterName, "Addams");
        row.put(RosterHeader.GRADE.camperRosterName, "6th ");
        manager.logWarning(RosterWarning.create_badDataFormat(
                row, RosterHeader.GRADE.camperRosterName, "\\d+[a-z]{2}"));

        FeedbackReport report = new FeedbackReport(
                Kind.DATA_QUIRK,
                FeedbackReport.environmentString(),
                List.of("First Name", "Last Name", "Grade"),
                WarningSummarizer.summarize(manager),
                "");

        String text = report.toPlainText();
        assertFalse(text.contains("Wednesday"), "camper name reached the report: " + text);
        assertTrue(text.contains("BAD_DATA_FORMAT"), text);
    }

    @Test void generalReportWorksWithNoRosterLoaded() {
        FeedbackReport report = new FeedbackReport(
                Kind.GENERAL, FeedbackReport.environmentString(),
                List.of(), List.of(), "would love a cabin sort");

        String text = report.toPlainText();
        assertTrue(text.contains("would love a cabin sort"), text);
        assertTrue(text.contains("HappyCamper"), text);
        assertEquals(Kind.GENERAL, report.kind());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B test -Dtest=FeedbackEntryPointTest`
Expected: PASS already if Tasks 2 and 3 are done — this test pins the composition, not new code. If it fails, the failure is in Task 2 or 3 and belongs there. Proceed to Step 3 either way.

- [ ] **Step 3: Add the accessor to `WarningDialog`**

In `src/main/java/com/echo/ui/dialog/WarningDialog.java`, add below the constructor:

```java
    /**
     * Exposes the whole log so a feedback report raised from any card covers every warning,
     * not just the type currently displayed.
     */
    public Map<WarningType, ArrayList<RosterWarning>> getWarningLog() {
        return warningLog;
    }
```

The file already imports `java.util.ArrayList`, `java.util.Map`, `RosterWarning`, and `WarningType`.

- [ ] **Step 4: Add the link to `WarningDialogPanel`**

In `src/main/java/com/echo/ui/dialog/WarningDialogPanel.java`, in `buildPanel()`, insert immediately before the `add(createCheckboxPanel(borderWidth));` line:

```java
        //Contextual feedback entry point - catches the user at the moment they notice a quirk
        JButton feedbackButton = new HoverButton("Something look wrong? Tell Jay →");
        feedbackButton.setAlignmentX(LEFT_ALIGNMENT);
        feedbackButton.setBorderPainted(false);
        feedbackButton.setContentAreaFilled(false);
        feedbackButton.setForeground(DialogConstants.TEXT_COLOR_NORMAL);
        feedbackButton.addActionListener(a -> openFeedback());
        add(feedbackButton);
```

And add this method to the class:

```java
    /**
     * Opens the feedback dialog covering every warning in the log, not just this panel's type.
     */
    private void openFeedback() {
        WarningManager scratch = new WarningManager();
        parent.getWarningLog().values().forEach(list -> list.forEach(scratch::logWarning));

        FeedbackReport report = new FeedbackReport(
                FeedbackReport.Kind.DATA_QUIRK,
                FeedbackReport.environmentString(),
                List.of(),
                WarningSummarizer.summarize(scratch),
                "");
        new FeedbackDialog(parent, report).showDialog();
    }
```

Add imports: `javax.swing.JButton`, `java.util.List`, `com.echo.logging.WarningManager`, `com.echo.feedback.FeedbackReport`, `com.echo.feedback.WarningSummarizer`, `com.echo.ui.elements.HoverButton`.

Column names are empty here because `WarningDialogPanel` has no reference to the imported rosters; the warning lines already name the columns that mattered.

- [ ] **Step 5: Add the top-bar button to `MainWindow`**

In `src/main/java/com/echo/ui/MainWindow.java`, after the `tutorialButton` creation (line 227):

```java
        JButton feedbackButton = new HoverButton("Feedback");
        feedbackButton.addActionListener(this::handleFeedback);
```

and after `buttonPanel.add(tutorialButton);` (line 241):

```java
        buttonPanel.add(feedbackButton);
```

Do **not** add it to the roster-dependent disable list at lines 231-235. A user who cannot import a roster at all is precisely the user who most needs to report.

Add the handler next to `handleTutorial` (line 702):

```java
    /**
     * Opens the feedback dialog. Available at all times: a user whose roster will not import is
     * the user most in need of a way to say so.
     */
    private void handleFeedback(ActionEvent event) {
        WarningManager warnings = rosterService.getWarningManager();
        FeedbackReport report = new FeedbackReport(
                FeedbackReport.Kind.GENERAL,
                FeedbackReport.environmentString(),
                currentRoster == null ? List.of() : List.copyOf(currentRoster.getOrderedHeaders()),
                warnings == null ? List.of() : WarningSummarizer.summarize(warnings),
                "");
        new FeedbackDialog(this, report).showDialog();
    }
```

Add imports: `com.echo.feedback.FeedbackReport`, `com.echo.feedback.WarningSummarizer`, `com.echo.logging.WarningManager`, `com.echo.ui.dialog.FeedbackDialog`, `java.util.List`.

Both names are verified: the field is `private EnhancedRoster currentRoster` (`MainWindow.java:78`), and `getOrderedHeaders()` is declared at `Roster.java:230`. It is chosen over `getVisibleHeaders()` so hidden columns still appear — a column the user has hidden is still a column that might explain a quirk.

**One honest caveat to carry into the UI copy:** `currentRoster` is an `EnhancedRoster`, so these headers include feature-generated columns (`Preference Score`, `Aquatic Conflicts`, and so on), not only the columns that came from the CSV. That is still useful — a missing input column shows up by its absence — but the report must not claim these are the input file's columns. The label in `FeedbackReport.toPlainText()` is `"Roster columns:"`, which is accurate as written; do not change it to "input columns".

- [ ] **Step 6: Verify compilation and the full suite**

Run: `mvn -B test`
Expected: PASS apart from the known headless dialog caveat.

- [ ] **Step 7: Manual smoke test**

With a display available:

```bash
mvn -B package -DskipTests
java -cp "target/classes:target/lib/*" com.echo.HappyCamper
```

Confirm, in order:
1. **Feedback** appears in the top bar and is enabled before any import.
2. Clicking it opens the dialog; the preview names the app version; typing in the note updates the preview.
3. The destination line reads `Downloads/HappyCamper-feedback-<date>-<time>.txt`.
4. **Save elsewhere…** opens a chooser with that name pre-filled; cancelling leaves the destination unchanged.
5. **Just Save** writes the file and reports the path. Open it — confirm **no camper names appear anywhere**.
6. Import a roster that produces warnings, and from the warning dialog click **Something look wrong? Tell Jay →**. Confirm the feedback dialog appears *above* the warning dialog rather than behind it, and that its preview lists warnings from every card, not just the visible one.

Step 5 is the one that matters. If a camper name appears in that file, stop and fix `WarningSummarizer` before going further.

- [ ] **Step 8: Leave for review**

Suggested message:

```
feat(feedback): add top-bar and warning-dialog entry points
- Feedback button always enabled, even with no roster loaded
- warning-dialog link reports the whole log, not just the visible card
```

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
|---|---|
| §1 `FeedbackReport` | 3 |
| §2 `WarningSummarizer`, whitelist, masking, fail-closed, truncation, collapsing | 2 |
| §3 `FeedbackDelivery`, three-step cascade, filename, seams | 5 |
| §3a `FeedbackLocation` | 1 |
| §4 `FeedbackDialog`, preview, destination line, chooser, modality | 6 |
| §5 entry points, never-disabled button, whole-log reporting | 7 |
| §6 contact injection, pom property, CI secret, empty-number degradation | 4 |
| Testing section | tests in 1–7, Windows filter widened in 1 |
| Porting note | Global Constraints |

**Deviations from the spec, both deliberate:**
1. `resolveDownloadsDir` drops the `osName` parameter — the chain is OS-independent, and an unused parameter is worse than the asymmetry with `ConfigLocation`. Existence checking is injected as a `Predicate<Path>` so the function stays pure.
2. `FeedbackContact` is a fifth class, not folded into `FeedbackDelivery`. Resource reading and URI building are separately testable and have no reason to share a file.

Both are worth reflecting back into the spec.

**Symbols verified against the codebase.** Every type, method, and field the plan references was checked rather than inferred:

| Referenced | Verified at |
|---|---|
| `RosterWarning.create_badDataFormat(Map, String, String)` | `RosterWarning.java:196` |
| `RosterWarning.create_unknownSwimActivityWarning(String, boolean)` | `RosterWarning.java:259` |
| `WarningManager.getWarningLog()`, `logWarning` | `WarningManager.java:73`, `:32` |
| `RosterService.getWarningManager()` | `RosterService.java:245` |
| `MainWindow.currentRoster` (`EnhancedRoster`) | `MainWindow.java:78` |
| `Roster.getOrderedHeaders()` | `Roster.java:230` |
| `WarningDialogPanel.parent` (public final) | `WarningDialogPanel.java:30` |
| `DialogBase(Window, boolean, int)`, `getMainPanel()` | `DialogBase.java:48`, `:63` |
| `HoverButton(String)` | `HoverButton.java:22` |
| `HappyCamper.NAME_VERSION` | `HappyCamper.java:18` |
| `ReportDialog` JFileChooser idiom | `ReportDialog.java:154-159` |
| `ConfigLocation.resolveDir` pattern + its tests | `ConfigLocation.java:16`, `ConfigLocationTest.java` |

Two things the implementer should still expect to adjust rather than fight:

1. `WarningSummarizer`'s for-loop wildcard may not compile against `Map<WarningType, ArrayList<RosterWarning>>`; Task 2 Step 3 says to match the real signature.
2. Swing layout constants and padding in `FeedbackDialog` are a starting point, not a specification. Adjust spacing to taste — but keep the preview scrollable and keep `setAlwaysOnTop(true)`.
