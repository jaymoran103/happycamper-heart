# June Experimental Sprint — Convention-Bend Ledger

These are deliberate, documented departures from the codebase's usual conventions, made for the
June 2026 experimental prototype sprint. They are prototype-grade and intentionally narrow. Each
entry names what bent, where, and why it's safe.

## A2 — ordinal display vs inverted-points scoring
- **Where:** `PreferenceFeature.setValue_roundScores` + `PreferenceFeatureUtils.pointsToRankLabel`.
- **Bend:** the `Preference by Round` column now shows a human ordinal (`1st, —, 3rd`) instead of the
  raw inverted choice-points (`10, 0, 8`).
- **Why safe:** purely presentational. Scoring still consumes `determineRoundPoints` directly; the
  label conversion is one-way and never feeds back into the math. `rank = 11 - points`; 0 → "—".

## C1 — one feature, two columns/filters
- **Where:** `SwimLevelFeature` (adds `SWIMLESSON` alongside `SWIMCONFLICTS`) + the `swimlevel` block in
  `FilterManager.createFiltersForRoster` (registers both `SwimLevelFilter` and `SwimLessonFilter`).
- **Bend:** relaxes the usual 1-feature→1-column / 1-filter habit. One `swimlevel` gate now yields two
  independent columns ("Aquatic Conflicts" via SWIMCONFLICTS, "Swim Lesson" via
  SWIMLESSON) and two independently-toggling filters.
- **Why safe:** the core gate convention is intact (still keyed on `hasFeature("swimlevel")`); only the
  cardinality changed. The swim-lessons activity is the literal string `"Swimming"`; level `Red` = 0.

## B2 — Activity filter + demand sort (two separate controls)
- **Where:** `ActivityFilter` + `ui.filter.ActivityFilterPanel` (the restrict filter); `service.DemandSort`
  + the top-bar combo in `MainWindow` (the sort); `RosterTable.setCamperOrdering` (model-level ordering hook).
- **Design history:** an earlier version conflated these into one "filter that also drives a sort" — a
  multi-select set with a demand toggle and chip UI. It was confusing and convention-breaking and was
  **scrapped**. They are now two independent, clearly-labeled controls that compose freely.
- **Control 1 — Activity filter:** a conventional multi-select visibility filter (checkbox list + a
  Round-scope combo). A camper passes iff *assigned* any selected activity within the round scope. No
  chips, no sort — no longer an outlier.
- **Control 2 — Demand sort:** a top-bar `Sort by demand for: [activity]` control. Orders campers by their
  **original** preference rank for the one chosen activity (`indexOf+1` in the raw "Activity Preferences"
  list; non-wanters sort last). A pure sort — it hides no one.
- **Remaining bend — derived sort with no column/arrow:** the demand sort has no backing table column.
  `RosterTable.updateModel` pre-orders the filtered list with an optional `Comparator<Camper>`
  (`setCamperOrdering`); selecting a demand activity clears the RowSorter sort keys so there is no arrow.
- **One indicator per sort type:** demand sort and column-header sort are **mutually exclusive**, and each
  has exactly one cue — demand sort shows in the status bar ("Sorted by: demand for …", since it has no
  arrow); a column sort shows via the header arrow only (not echoed in the footer, which would duplicate
  the arrow). Entering a demand sort clears the column keys; a column-header click clears the demand sort
  (`MainWindow` RowSorterListener → `clearDemandSortForColumnSort`), resetting the combo to None and
  flashing a one-shot amber status note ("demand sort cleared by column sort") that clears on next refresh.
- **Why safe:** the filter is now a plain filter; the sort is a pure comparator over campers that mutates
  no roster data. Neither depends on the other.

## D1 — aggregate report + derived activity catalog
- **Where:** `ActivityReportData` (service) + `ReportDialog` (ui/dialog).
- **Bend:** produces an aggregate report table (not roster columns) and derives its activity catalog at
  runtime (distinct non-empty activities across all parsed preferences ∪ all assignments) — there is no
  master activity list to read from. One button ("Activity Report") opens a popup with one row per
  activity and six columns: Activity, R1 count, R2 count, R3 count, Avg Rank (In), Avg Rank (Not In).
  "In/Not In" split the mean preference rank between campers enrolled vs. not enrolled; campers who
  didn't preference the activity are excluded from both averages (shown as "—").
- **Two row types:** `toRow()` returns `List<Object>` for numeric sort in JTable; `toCsvRow()` returns
  `List<String>` for CSV export. `ReportDialog` accepts both.
- **TableLook:** only `doHeaderLook` is applied (sort arrows, hand cursor, header colors); `doCellLook`
  is skipped because its renderer casts values to String, which conflicts with the numeric Object types.
- **Why safe:** read-only aggregation over existing per-camper data; no roster mutation.

## TextSearchFilter — the only non-gated filter
- **Where:** `FilterManager`.
- **Bend:** every other filter is registered behind a `hasFeature(...)` gate; the universal search
  filter is always-on.
- **Why safe:** it has no feature dependency — it matches plain camper fields and an empty term passes
  everyone, so an always-present instance is inert until used.
