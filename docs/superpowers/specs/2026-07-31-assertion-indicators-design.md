# Filters as Assertion Indicators — Prototype Design

**Date:** 2026-07-31
**Status:** Implemented, on branch `assertion-indicators` (8 local commits, unpushed)
**Scope:** Demo prototype. Communicates the objective; not the eventual refactor.

## Objective

Every view filter in the sidebar doubles as an assertion indicator. Each filter header shows a
green checkmark when all campers satisfy that filter's assertion and a red dot with a failure count
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

**This prototype does none of that.** It only makes assertion status visible in the existing filter
headers. The one forward-looking concession is that each assertion carries its plain-English claim
string, surfaced as a tooltip, so the demo can show the eventual wording without restructuring the
sidebar.

## Decisions

| Question | Decision | Reason |
|---|---|---|
| Filters with no pass/fail meaning (Assignment, Activity Selector) | No indicator at all — headers unchanged | Honest: reads as "not an assertion". Keeps the green/red signal meaningful, and visibly shows the sidebar is mixed today. Search (`TextSearchFilter`) is not part of this decision: `createFilterPanel()` returns `null`, so it has no sidebar header to put an indicator on in the first place. |
| Evaluation scope | Always the whole roster, regardless of checkbox state | Stable and honest. Under a visible-rows scope, unchecking "show incompatible" would turn Aquatic Conflicts green while conflicts still exist — confusing on stage. |
| Visual treatment | Right-aligned green checkmark when satisfied, red dot plus failure count when not | Leaves the existing left side (toggle + bold title) untouched, scans as a column of checkmarks/dots down the sidebar, and the count conveys severity. |
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

### 5. `CollapsibleFilterPanel.setAssertion(AssertionResult)`

Appends a `JLabel` to `headerPanel` after the existing `Box.createHorizontalGlue()`, so it lands
right-aligned with no layout change. An 8px right inset matches the toggle label's existing padding.

- satisfied → `✓` in the pass color, no number
- violated → `● N` in the fail color
- not applicable → label invisible, header identical to today

On an applicable panel the header tooltip becomes `claim + " — " + status`, where status is
`"satisfied"` or `"N campers fail"` / `"N programs fail"` (pluralized from `unit`). Non-applicable
panels keep the existing `title + " - Click to expand/collapse"` text.

The satisfied state uses the `✓` glyph (U+2713 CHECK MARK) rather than a dot: the violated state
already draws the eye with its numeral, so encoding satisfied in *shape* as well as colour keeps it
from reading as the weakest signal on screen, and it reads faster and survives red/green colour
blindness. The violated state remains the `●` glyph (U+25CF) in a bold derived font, not custom
painting — adequate for a prototype. If either glyph fails to render on a target platform,
substitute a small `paintComponent` shape.

### 6. Colors

Declared in `FilterSidebar` next to the existing sidebar palette, which is where the
`FUTURE — centralize all colors somewhere?` note already lives.

`TableColors.FLAGGED_EVEN` (240,185,185) is the app's established "something is wrong" red, but as a
pale row tint it has too little contrast against the #C8C8C8 header to read as a 9px dot. The
indicator colors are therefore saturated members of the same hue families:

- `ASSERTION_PASS_COLOR = new Color(46, 125, 50)` — muted green
- `ASSERTION_FAIL_COLOR = new Color(178, 34, 34)` — firebrick, harmonizing with the flagged-row hue

Implement these values; adjust only if they read poorly against the header when viewed in the app.

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

The rendering change is covered by additions to `CollapsibleFilterPanelTest` and `FilterSidebarTest`,
plus a new shared `FilterTestSupport` helper (`findAssertionLabel`) they both use to locate the
indicator label in the component tree. The maintainer separately verified it by launching the app
against a demo roster and confirming the checkmarks and dots appear, align, and read correctly at a
glance.

## Explicitly out of scope

- reordering, renaming, or restructuring the sidebar
- the `Programs Filter → Rounds assigned are consistent…` subheading naming
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
