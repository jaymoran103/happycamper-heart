# Filters as Assertion Indicators — Prototype Design

**Date:** 2026-07-31
**Status:** Implemented, on branch `assertion-indicators` (13 local commits, unpushed)
**Scope:** Demo prototype. Communicates the objective; not the eventual refactor.

## Objective

Every view filter in the sidebar doubles as an assertion indicator. Each filter header shows a
green checkmark when all campers satisfy that filter's assertion and a red failure count
when some do not. The point of the demo is that a reader can glance at the sidebar and see, without
clicking anything, which properties of the roster hold and which are violated.

## Eventual direction (context, not scope)

Later, assertions replace the visual and functional roles of filters:

- the sidebar becomes a visualization of the assertion sequence
- assertions are the collapsible/expandable rows
- a filter is redefined as the view/control component of its assertion — the checkboxes inside the
  expanded region isolate the rows relevant to that assertion
- names gain subheadings: `Programs Filter → Rounds assigned are consistent for each program`,
  `Aquatic Conflicts → Campers are eligible for their water activities`

**This prototype does not fully realize that.** The sidebar is still a list of filters, not a
visualization of an assertion sequence, and filters are not renamed or given subheadings. But it is
no longer true that status is the only thing made visible: the claim now leads each filter's
expandable region, above the checkboxes — a genuine partial step toward the third bullet above,
where a filter acts as the view/control component of its own assertion. The claim's tooltip still
exists and still matters (see §5c) — it is what carries meaning when a panel is collapsed and the
claim block is hidden — but it is a supporting detail now, not the sole vehicle for the claim.

## Decisions

| Question | Decision | Reason |
|---|---|---|
| Filters with no pass/fail meaning (Assignment, Activity Selector) | No indicator at all — headers unchanged | Honest: reads as "not an assertion". Keeps the green/red signal meaningful, and visibly shows the sidebar is mixed today. Search (`TextSearchFilter`) is not part of this decision: `createFilterPanel()` returns `null`, so it has no sidebar header to put an indicator on in the first place. |
| Evaluation scope | Always the whole roster, regardless of checkbox state | Stable and honest. Under a visible-rows scope, unchecking "show incompatible" would turn Aquatic Conflicts green while conflicts still exist — confusing on stage. |
| Visual treatment | Right-aligned fixed-size tinted badge: green `✓` when satisfied, red count when not. The claim itself leads the expandable region. | Leaves the existing left side (toggle + bold title) untouched and the header at 30px. The tinted badge scans as a column of green/red down the sidebar without the reader parsing digits; the count conveys severity; the in-panel claim carries meaning without hiding it behind hover. See §5 for the four rejected placements. |
| Assignment Filter | **No assertion.** | A round count on its own is neither valid nor invalid — round counts legitimately vary by program, and there is no configured expectation to check a camper against. Any round-count *inconsistency* is already reported by the Programs assertion. Configurable expected round counts are future work; when they exist, Assignment gains a real per-camper assertion. |

## Architecture

Chosen approach: **assertion as a defaulted method on `RosterFilter`.** The assertion lives next to
the predicate it belongs with, and it is the shape the eventual refactor wants — the filter already
owns its assertion rather than a parallel registry needing to be kept in sync. Rejected
alternatives: an `AssertionRegistry` keyed by filter ID (creates a second registration site that the
refactor would delete), and computing inline in `FilterSidebar` (buries domain logic in a UI class).

Every filter in the current sidebar falls into exactly one of three categories, and each maps onto
one piece of the implementation:

| Category | Filters | Implementation |
|---|---|---|
| 1. Column-based assertion | Preference, Aquatic Conflicts, Swim Lessons, Duplicate Activities | shared `AssertionResult.forFlagColumn` helper |
| 2. Custom check | Programs | hand-written `checkAssertion` override |
| 3. No assertion | Assignment, Search, Activity Selector | inherits the defaulted no-op; not modified |

Category 1 is genuinely uniform — all four `apply()` methods have the identical shape (`null` → pass,
`DataConstants.DISPLAY_EMPTY` or `isEmpty` → valid, any other value → flagged), so the helper needs
no per-filter special-casing. Category 2 is the escape hatch: any future assertion that isn't a
single flag column lands there. Category 3 needs no code at all.

Eight filters exist, but only seven have a sidebar panel at all — `TextSearchFilter.createFilterPanel()`
returns `null` because universal search renders in the top bar, not the sidebar, so Search has no
sidebar row to show an indicator on. Five of those seven filters therefore show an indicator.

Note: `CamperRoundsFilter` exists in the package but `FilterManager` leaves it commented out
("Disabling, redundant now"), so it never reaches the sidebar. It hardcodes `roundCount == 3` as
"complete" — the assumption this design explicitly rejects. Leave it untouched.

### 1. `AssertionResult` — new record, `com.echo.filter`

```java
public record AssertionResult(boolean applicable, int failureCount, String unit, String claim) {
    public static AssertionResult none();
    public static AssertionResult of(int failureCount, String unit, String claim);
    public boolean satisfied();   // applicable && failureCount == 0
    public String statusText();   // "satisfied" / "1 camper fails" / "3 campers fail" / "" when not applicable
}
```

- `applicable` false means "this filter is not an assertion" — the header renders unchanged.
- `unit` is the noun being counted (`"camper"` / `"program"`), used for tooltip pluralization. The
  unit is deliberately not uniform across filters: the Programs assertion counts programs, the rest
  count campers.
- `claim` is the plain-English assertion, e.g. `"Rounds assigned are consistent for each program"`.

### 2. `AssertionResult.forFlagColumn` — new static factory, `com.echo.filter`

```java
static AssertionResult forFlagColumn(EnhancedRoster roster, RosterHeader header,
                                     String unit, String claim);
```

This is a static factory on `AssertionResult` itself, not a separate helper class. A standalone
`com.echo.filter.Assertions` class was rejected: that name would shadow JUnit's
`org.junit.jupiter.api.Assertions` in same-package tests, forcing an import alias or fully-qualified
references everywhere the test package asserts. Counts campers whose `header` value is non-empty,
treating `null`, `DataConstants.DISPLAY_EMPTY`, and `DataConstants.isEmpty(...)` as empty — matching
the existing `apply()` convention in the flag-column filters. Four of the five assertions are exactly
this shape (category 1 above).

### 3. `RosterFilter` gains one defaulted method

```java
default AssertionResult checkAssertion(EnhancedRoster roster) {
    return AssertionResult.none();
}
```

`AssignmentFilter`, `TextSearchFilter`, and `ActivityFilter` inherit the default and are not
modified — category 3.

### 4. Five overrides

| Filter | Assertion | Column / source | Unit |
|---|---|---|---|
| `PreferenceFilter` | No camper is assigned an activity they didn't request | `UNREQUESTED_ACTIVITIES` empty | camper |
| `SwimLevelFilter` | Campers are eligible for their water activities | `SWIMCONFLICTS` empty | camper |
| `SwimLessonFilter` | Swim lesson assignments match swim level | `SWIMLESSON` empty | camper |
| `DuplicateActivityFilter` | No camper is assigned the same activity twice | `DUPLICATE_ACTIVITY` empty | camper |
| `SortedProgramFilter` | Rounds assigned are consistent for each program | `ProgramFeature.getProgramsByRoundCount(roster).get(-1)` is empty | program |

The first four delegate to `AssertionResult.forFlagColumn` (category 1).

**`SortedProgramFilter`** (category 2): `ProgramFeature.getProgramsByRoundCount` already buckets
programs by round count with `-1` meaning Mixed. The assertion is that the Mixed bucket is empty;
the failure count is its size. No new logic — this is the only assertion needing a hand-written
override, and it is the only place round-count inconsistency is reported.

### 5. Presentation: the badge and the in-panel claim

Status and meaning are shown in two places, chosen so the header never grows:

- **The header carries status** — a fixed-size badge, right-aligned, always the same box.
- **The expandable region carries meaning** — the assertion's claim leads the content panel, above
  the checkboxes that filter the rows it concerns.

This split is deliberate. It reads top-to-bottom as *the assertion, then its controls*, which is the
structure the eventual refactor is heading for (a filter becoming the view/control component of its
assertion) — reached here without restructuring the sidebar. It also means a collapsed panel costs
30px and still reports status, so collapsing everything yields a compact stack of title-plus-status
rows: a preview of the eventual assertion-sequence sidebar, for free.

#### 5a. `AssertionBadge` — new class, `com.echo.ui.filter`

A small `JLabel` subclass rather than more logic inside `CollapsibleFilterPanel`, which is focused
today and would otherwise be doing three jobs.

- **Fixed 38×22**, enforced via `setPreferredSize`, `setMinimumSize`, *and* `setMaximumSize`. The box
  is identical for `✓`, `1`, `10`, and `12`: size never varies with status or digit count. This is a
  hard requirement, not a nicety.

  All three setters are load-bearing, and the reason for the third is counter-intuitive enough to be
  worth stating: `ComponentUI.getMaximumSize()` returns the UI's *computed* preferred size and
  ignores `setPreferredSize`, so without an explicit maximum `BoxLayout` **shrinks** the badge to the
  glyph's natural height — measured at 38×19, sitting 2px lower. It does not stretch it. Dropping
  `setMaximumSize` leaves a preferred-size assertion green while the render is wrong, so the badge's
  maximum size is asserted directly.
- `setOpaque(true)`, background `ASSERTION_PASS_BG` / `ASSERTION_FAIL_BG`.
- `BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)` — a recessed indicator well, matching the
  content panels' treatment.
- Centred bold 12pt: `✓` (U+2713) when satisfied, otherwise the failure count.
- Foreground `ASSERTION_PASS_COLOR` / `ASSERTION_FAIL_COLOR`.
- Swing component name `"assertionIndicator"`, so tests locate it by name rather than position.

Appended to `headerPanel` after the existing `Box.createHorizontalGlue()` with an 8px right gap, so
it right-aligns with no layout change. The header stays 30px. A non-applicable assertion hides the
badge entirely, leaving the header exactly as it renders without this feature.

#### 5b. The claim block

`setAssertion` inserts, at index 0 of the content panel, a claim label followed by a horizontal
separator — above the checkboxes. Ordering already works: `FilterSidebar.addFilterPanel` calls
`createFilterPanel(roster)` (which populates the content) and only then `setAssertion`, so index 0
lands above existing content.

- Claim text at `(45, 45, 45)`, plain weight, 11pt — near the weight of the surrounding labels, with
  the separator and the checkboxes' bold weight carrying the hierarchy instead of low contrast.
- ~4px padding above and below, so it crowds neither the panel border nor the separator.
- A non-applicable assertion inserts nothing — no claim, no separator.
- `setAssertion` must be idempotent: remove any previously inserted claim block before inserting, so
  a second call cannot stack duplicates.

**Wrapping gotcha, do not rediscover this:** `<html><body style='width:245px'>` is *silently ignored*
by Swing's HTML renderer — the label renders one long line and clips mid-word. Only
`<html><table width=245><tr><td>…</td></tr></table></html>` actually wraps. The width must be passed
to the `table` element.

#### 5c. Tooltip

Unchanged from the first iteration, and still load-bearing: on an applicable panel the header
tooltip is `claim + " — " + status` (`"satisfied"` / `"N campers fail"` / `"N programs fail"`,
pluralized from `unit`). Non-applicable panels keep `title + " - Click to expand/collapse"`.

It matters more now, not less: a **collapsed** panel shows the badge but hides the claim, so the
tooltip is what carries meaning in that state. Without it, collapsing would leave bare numbers.

#### 5d. Rejected alternatives

Four other placements were prototyped and rendered before this one was chosen:

- **claim as a header subtitle** — rejected: it grows every header, giving ragged heights and
  costing a visible panel of sidebar density;
- **reserved info strip at the sidebar bottom** / **at the sidebar top** — rejected: both hide
  meaning behind hover, and force the eye between the badge and a strip elsewhere in the column;
- **hover writes into the window's existing status bar** — rejected for the same reason, plus it
  shares one line with the camper count, so resting and hover text look alike.

The common failure of all four is that meaning appears only on hover. Putting the claim in the
expandable region makes it simply present.

### 6. Colors

Declared in `FilterSidebar` next to the existing sidebar palette, which is where the
`FUTURE — centralize all colors somewhere?` note already lives.

Glyph colors — saturated enough to read against the badge fill:

- `ASSERTION_PASS_COLOR = new Color(46, 125, 50)` — muted green
- `ASSERTION_FAIL_COLOR = new Color(178, 34, 34)` — firebrick

Badge fills — pale tints of the same hue families:

- `ASSERTION_PASS_BG = new Color(198, 214, 198)`
- `ASSERTION_FAIL_BG = new Color(228, 198, 198)`

The tinted fill is what makes status *scannable*: colour covers the whole 38×22 badge, so a reader
takes in the column of green and red without parsing digits. Two neutral-fill alternatives were
rendered and rejected — a raised gray chip and a recessed darker gray well — because confining
colour to the glyph slows that scan, and because at 38×22 an etched border is only ~2px, so
raised-versus-recessed barely registers and cannot carry the distinction on its own.

The tint is *continuity, not novelty*: `TableColors.FLAGGED_EVEN` (240,185,185) is already the app's
pale-red "something is wrong" row tint, so a pale-red badge speaks the vocabulary the table
established. (The first iteration's note that this hue "has too little contrast to read as a 9px
dot" was true of a dot — it does not apply to a 38×22 filled badge.)

Implement these values; adjust only if they read poorly when viewed in the app.

### 7. Wiring

`FilterSidebar.addFilterPanel` calls `panel.setAssertion(filter.checkAssertion(roster))` after
creating the panel. Whole-roster scope means no recompute on checkbox toggle; status is computed
once per roster load, which is already where `MainWindow` (~lines 464–478) rebuilds the sidebar.

`checkAssertion` implementations must not mutate the roster and must tolerate missing columns by
returning `AssertionResult.none()` rather than throwing — a filter whose backing column is absent
should render as "not an assertion", not crash the sidebar build. `addFilterPanel` already wraps
panel creation in a try/catch that logs and continues.

## Testing

The assertion logic is pure and headless. A new `AssertionTest` in `com.echo.filter` asserts
green/red outcomes over synthetic in-memory rosters, built with two local static helpers —
`rosterWithFlagValues(RosterHeader, String...)` and `rosterWithPrograms(String...)` — rather than
fixtures from `src/test/resources/testRosters/`, covering:

- each flag-column assertion, satisfied and violated
- the Programs Mixed-bucket assertion
- `AssignmentFilter`, `TextSearchFilter`, and `ActivityFilter` all return `none()`
- a filter whose backing column is absent returns `none()` and does not throw
- the checkbox-independence invariant: flipping a filter's own visibility state does not change its
  assertion's failure count

The rendering is covered by additions to `CollapsibleFilterPanelTest` and `FilterSidebarTest`, plus a
shared `FilterTestSupport` helper (`findAssertionLabel`) they both use to locate the badge in the
component tree by its Swing component name. Beyond the first iteration's cases, the presentation
described in §5 needs:

- the badge reports the same `getPreferredSize()` for `✓`, a single-digit count, and a double-digit
  count — the fixed-size requirement, which is the one most likely to regress silently;
- an applicable assertion inserts a claim label above the checkboxes; a non-applicable one inserts
  nothing;
- calling `setAssertion` twice does not stack duplicate claim blocks;
- the header's preferred height is unchanged at 30px whether or not a badge is present.

All of these are headless-safe — `JPanel`/`JLabel`/`JScrollPane` only, never a `JFrame`.

Appearance itself is verified by rendering `FilterSidebar` headlessly into a `BufferedImage` (set the
size, recurse `doLayout()` top-down, paint) and looking at the PNG, plus the maintainer running the
real app. Launching a window is impossible in a headless/background shell — it aborts the JVM
outright rather than throwing — so the offscreen render is the automatable path.

## Explicitly out of scope

- reordering, renaming, or restructuring the sidebar
- renaming filters, or retitling them as `Programs Filter → Rounds assigned are consistent…`. The
  claim now appears inside the panel (§5b), but the filter's own name is untouched.
- a roll-up summary anywhere in the window
- any change to what the filters actually filter
- any recompute of assertion status in response to filter interaction
- configurable expected round counts, or any other per-program configuration

## Porting note

Per `CLAUDE.md`, this repo is the public distribution point. The code changes here must be logged in
`~/Desktop/HappyCamper-pending-patches.md` (symptoms/intent, root cause or rationale, diff, porting
notes) so they can be ported to the dev repo. This spec is a docs-only file and needs no log entry.
The convention-bend ledger in `docs/sprint-conventions.md` should gain an entry noting that
`RosterFilter` now carries a non-filtering responsibility (its assertion), since that relaxes the
"a filter is a visibility predicate" convention.
