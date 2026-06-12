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
  independent columns ("Aquatic Assignment Validity" via SWIMCONFLICTS, "Swim Lesson Validity" via
  SWIMLESSON) and two independently-toggling filters.
- **Why safe:** the core gate convention is intact (still keyed on `hasFeature("swimlevel")`); only the
  cardinality changed. The swim-lessons activity is the literal string `"Swimming"`; level `Red` = 0.

## B2 — a filter that also drives a sort (the outlier)
- **Where:** `ActivityFilter`.
- **Bend (threefold):** (1) a *compound OR subject-selector* (multi-select activity set) rather than a
  uniform visibility toggle; (2) a filter that *also drives table ordering* (demand mode); (3) it
  introduces a **non-visible numeric helper column** used solely as a sort key (no header, no arrow).
- **Subtlety:** demand rank uses the activity's **original** preference rank (`indexOf+1` in the raw
  "Activity Preferences" list), NOT its compacted position in the Unfulfilled Preferences column.
- **Why safe:** restrict (which campers) and order (demand) are one coherent mode; the hidden column
  reuses the existing `CustomTableRowSorter` NUMERIC path verbatim.

## D1 — aggregate reports + derived activity catalog
- **Where:** `PreferenceReportData`.
- **Bend:** produces aggregate report tables (not roster columns) and derives its activity catalog at
  runtime (distinct non-empty activities across all parsed preferences ∪ all assignments) — there is no
  master activity list to read from.
- **Why safe:** read-only aggregation over existing per-camper data; no roster mutation.

## TextSearchFilter — the only non-gated filter
- **Where:** `FilterManager`.
- **Bend:** every other filter is registered behind a `hasFeature(...)` gate; the universal search
  filter is always-on.
- **Why safe:** it has no feature dependency — it matches plain camper fields and an empty term passes
  everyone, so an always-present instance is inert until used.
