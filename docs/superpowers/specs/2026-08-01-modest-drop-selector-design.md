# Design: Modest drop-zone treatment for the import file pickers

**Date:** 2026-08-01
**Branch:** `worktree-modest-drop-selector`
**Status:** Approved design — prototype
**Estimated size:** a few hours
**Supersedes:** `2026-07-14-input-drag-selector-design.md` (visual treatment only; that
spec's *behavioural* decisions are carried forward unchanged)

## Problem

Drag-and-drop file selection for the two Import pickers was prototyped on branch
`proto-filedropselector` (commit `9a69d0e`). The behaviour is right. The visuals are not
the app's:

| Prototype choice | Why it reads as foreign |
|---|---|
| `BorderFactory.createDashedBorder(...)` | Dashed borders appear nowhere else in the codebase. The app's depth vocabulary is entirely `createEtchedBorder(RAISED/LOWERED)`. |
| Hardcoded `new Color(70, 130, 180)` steel blue | Not in `DialogConstants`, and the app already owns a pale-blue "active" tone at `(190, 210, 240)`. |
| `componentHeight = COMPONENT_HEIGHT_LARGE` | Grows both Import selectors from 85px to 120px purely to fit a border the app doesn't need. |

The app already has a consistent way to say "this is a recess that content sits in":
`createEtchedBorder(EtchedBorder.LOWERED)` compounded with `EmptyBorder` padding, as used
by `AssertionBadge` and the `CollapsibleFilterPanel` content region. This redesign
re-expresses the drop affordance in that existing language.

## Goal

Communicate droppability through the selector's **own frame**, in **two tiers** driven by how
close the drag is — at the unchanged standard height, adding **nothing** to the resting layout
and **no new colour** to the palette.

## Revision 2, 2026-08-01 — two-tier feedback

Single-tier feedback could not tell a user that a widget accepts files until the pointer was
already on top of it, which left the resting state with no affordance but its hint text. The
standard fix, and what every mainstream platform does, is to announce candidates when a drag
begins and escalate the one under the pointer:

| Tier | When | Treatment |
|---|---|---|
| `IDLE` | no drag | resting frame, resting fill, path or hint text |
| `ARMED` | drag is in the dialog, not over this picker | **dashed frame**, resting fill, text unchanged |
| `HOVERED` | drag is directly over this picker | dashed frame + **darkened fill** + `Drop your file here` |

`ARMED` deliberately changes *only* the border. If it also darkened or swapped text, a picker
would appear to claim a file that was actually headed for its neighbour.

The darkened fill is `DROP_COLOR_HOVER`, an alias of the existing `DIALOG_COLOR_BOTTOM`
`(220, 220, 220)` — so "darker" still costs no new colour. Rendering confirmed it reads
clearly, and specifically *because* it sits beside an un-darkened armed sibling; the same
20-step gray shift in isolation would be far weaker. The two tiers hold each other up.

### The ceiling on "any time a file is being dragged"

Arming cannot begin when the user first picks a file up. AWT delivers drag events to a
component only while the drag is over it, and there is no screen-wide notification for a drag
that originated outside the app — a Finder drag has no in-process `DragSource`, so
`DragSourceListener` is no help. Arming therefore begins the moment the drag **crosses into
the dialog**, which is the earliest moment available. In practice this is indistinguishable
from the intent, since the user is already dragging toward the dialog.

## Revision, 2026-08-01

An intermediate version of this design nested the path label in a recessed etched *well*
that was always visible, and made only that well droppable. Reviewed and revised:

- **The whole selector is the drop target**, not an inner zone. Dropping anywhere over the
  widget — including the Browse button and the title — counts.
- **The well is gone.** At rest the selector is byte-for-byte the layout it has today; the
  only resting cue is the hint text.
- **The feedback moved to the selector's own frame**, which swaps its etched border for a
  dashed one during a drag.

A `BevelBorder.LOWERED` "the whole selector gets deeper" variant was built and rendered
first, and **rejected on the evidence**: against a `(240, 240, 240)` dialog it is nearly
indistinguishable from the resting etched frame, because both are low-contrast 2px
treatments whose shading derives from the same background. There is not enough room at that
size for "deeper" to register.

The dashed swap was chosen instead because the **pattern** changes (solid → dashed), so the
signal does not depend on perceiving a hue shift — which is exactly what lets it stay gray
and add no colour. Crucially the dash is now a **transient state, never resting chrome**;
that is what separates it from the original prototype's permanent dashed box.

## Scope

- **In scope:** OPEN-mode `FileSelector`s — exactly two widgets, `ImportDialog.java:69`
  (Camper File) and `ImportDialog.java:75` (Activity File).
- **Out of scope:** the SAVE-mode selector in `ExportDialog.java:83`. Dropping a file onto
  a "save as" target is meaningless, so SAVE keeps today's plain row and is provably
  unaffected.
- **Deferred:** shared drop zone with header-sniffing auto-assignment, recent-files UI,
  inline preview. Unchanged from the 07-14 spec.

## Decisions

### Visual

1. **Nothing at rest.** The selector's resting layout is unchanged from today — path label
   directly in the Browse row, no wrapper, no extra border. Droppability is advertised by
   the hint text alone.
2. **The frame is the feedback.** Once a drag is in the dialog, the outer panel's etched
   border is replaced by a dashed one. The whole widget visibly becomes a drop zone rather
   than one region inside it. See the two-tier table above for which cue fires when.
3. **Dashed, not deeper.** A `BevelBorder.LOWERED` variant was rendered and rejected — see
   revision 1 above. The dashed pattern change is legible where a depth change is not.
4. **Fill and text are tier two only** — reserved for the picker actually under the pointer, so
   the two active tiers stay distinguishable. Between them the feedback changes three channels
   (pattern, fill, text), so it survives any single perception failure including colour-blindness
   — which the gray dash already guarantees on its own.
5. **Copy** — the three states stay in one voice, all anchored on "Drop":
   - empty: `Drop a CSV file, or Browse`
   - dragging: `Drop your file here`
   - selected: existing `getShortenedPath` output, e.g. `Downloads/campers.csv`

   The swap therefore reads as the sentence *focusing* rather than changing register.
6. **Height unchanged.** `componentHeight` stays `COMPONENT_HEIGHT_STANDARD` (85) — nothing
   was added to the layout, so nothing had to grow. A deliberate reversal of the prototype's
   jump to `COMPONENT_HEIGHT_LARGE`.
7. **No new colour.** `DROP_BORDER_ACTIVE` is `Color.GRAY`, already the app's grid and
   disabled-text gray. The dash pattern and the hint text carry the signal, so the stroke
   itself stays neutral. A steel-blue variant was offered and declined in favour of keeping
   the palette untouched.
8. **Insets are load-bearing.** The dashed border uses `2f` thickness because `StrokeBorder`
   derives insets from stroke width, and `2f` yields the same 2px inset as `EtchedBorder`. A
   mismatch would resize the content area mid-drag and make the path jump or re-ellipsize.
   `testSetDragActive_preservesInsets` guards this.
9. **Dash rhythm** — `6f` strokes with `4f` gaps. A finer `4/3` dash reads as busy along a
   580px edge.

### Behavioural (carried forward from the 07-14 spec)

7. **Per-file drop zones**, not a shared zone. Roles stay explicit; no header sniffing.
8. **Browse retained** as a fallback, unchanged.
9. **OPEN-only.** The SAVE code path is untouched.
10. **First file wins** when several files are dropped on one zone.

### Changed from the prototype

11. **The whole selector is droppable.** A `DropTarget` is installed on every component in
    the content subtree, so a drag anywhere over the widget registers. Not covered: the outer
    panel's own 7px border-and-padding ring, which `InputSelector.createPanel()` builds after
    `buildSelectorPanel` runs and so is unreachable from there. That ring is the selector's
    frame rather than its interior and is not a realistic aim point.
12. **Feedback is dialog-wide, not per-widget.** State lives in a shared `FileDropArbiter`
    rather than in each selector; see the Architecture section. This is the one change that
    widens the porting surface beyond `FileSelector` — `ImportDialog` participates too.
13. **Internal crossings must not flicker.** Handled by the arbiter's deferred `exit()`, also
    documented under Architecture.

## Architecture

All changes live inside `FileSelector`, guarded by `mode == SelectionMode.OPEN`. One widget
class, no new subclass, SAVE untouched.

### `DialogConstants`

One added constant, `DROP_BORDER_ACTIVE`, in the COLORS section.

### `FileSelector.buildSelectorPanel`

The row is built exactly as it is today for both modes — Browse at `WEST`, path label at
`CENTER`. OPEN mode additionally sets the hint text and installs the drop targets. Nothing
about the resting layout differs between the two modes.

`BorderLayout.CENTER` for the label remains load-bearing. Per the CLAUDE.md lesson, inside a
fixed-size container a FlowLayout wraps an overflowing path into a clipped, invisible second
row; BorderLayout constrains the label so it ellipsizes instead.

### `FileSelector.setDragState(DragState)`

Public, and the single source of truth for all three cues — the outer panel's border, its fill,
and the label text — so they can never disagree. The resting border **and fill** are **captured
on first use** rather than rebuilt, so this stays correct if `InputSelector.createPanel()` ever
changes how it frames a selector.

Restoring the text goes through `getShortenedPath(selectedFile)`, which already falls back to
the empty-state hint when nothing is selected, so one call handles both the empty and selected
cases. Guarded to a no-op unless `mode == SelectionMode.OPEN` — mode is the only thing
distinguishing a selector that shows drag feedback from one that must not.

**Why the fill needs a transparency walk.** The hover fill is painted by the outer panel, and any
opaque panel nested inside it paints over that fill. `makeContainersTransparent` clears
`setOpaque` on every nested `JPanel` — invisible at rest, since none carries a background of its
own, and it lets a single `setBackground` darken the whole section. It must be a **walk**, not a
couple of named panels: `DialogUtils.combineWithHelpButton` wraps the title in an opaque `JPanel`
whenever a help page is linked (both import pickers link one), and that wrapper painted the
resting colour as a lighter strip across the header of an otherwise darkened selector.

### Ordering is load-bearing (both walks)

`makeContainersTransparent` and `installFileDropTargets` both walk the component tree, so they run
at the **end** of `buildSelectorPanel`, once every child exists. Two real bugs came from running
the installer too early — before `panel.add(fileSelectionPanel)`:

1. **Drop coverage silently stopped at the title.** The Browse row, the Browse button and the path
   label were not yet children, so the walk never reached them. `FileDropArbiter`'s sentinel then
   found them unclaimed and registered *background* targets, so hovering directly over the label
   reported `enter(null)` — the selector armed instead of darkening, and its hint text reverted
   mid-hover.
2. **A lighter strip survived across the header**, per the help-button wrapper above.

Note the title is added by `InputSelector.createPanel()` *before* it calls `buildSelectorPanel`,
while the Browse row is added *inside* it — so no single point in the method sees the whole tree
except the end.

Guarded by `testDropTargets_coverTheWholeSelector` and
`testNestedPanels_areTransparentSoTheFillShows`, both confirmed to fail against the old ordering.

`installFileDropTargets` is idempotent (it records what it claimed), so `setDropArbiter` runs it
again on `cachedPanel` once a dialog wires the selector up. That picks up the outer panel and its
7px frame ring, which would otherwise be left to the sentinel and read as background right at the
widget's edge. The sentinel correspondingly **stops descending** at any component that already has
a drop target, so it can never claim a picker's interior.

### `FileDropArbiter`

A single arbiter per dialog owns the tier state, because the two tiers are a property of the
dialog rather than of any one widget: "a drag is somewhere in here" cannot be known by a
component that only receives events while the drag is over itself.

- Each picker reports `enter(this)` / `exit()` from its own drop targets.
- A **sentinel** drop target claims the dialog's main panel and any descendant no picker already
  claimed, reporting `enter(null)` — "in the dialog, but not on a target". Without it, dragging
  across the gap between the two pickers would disarm both.
- `apply()` recomputes every picker's state from `armed` + `hovered`.

`ImportDialog.installDragFeedback()` wires both pickers and installs the sentinel, after
`super()` so every panel exists and the sentinel can walk a complete tree. A `FileSelector`
used outside such a dialog lazily creates a solo arbiter, so it still behaves sensibly alone.

**Disarming is the fragile part**, and the piece needing hand-verification:

- `exit()` defers its clear by one event via `invokeLater`, and any incoming `enter` cancels it.
  Every picker registers its whole subtree and the sentinel registers the background, so moving
  the pointer anywhere fires an exit on what it left before the enter on what it reached;
  clearing immediately would flicker on every component boundary crossed.
- A drag abandoned outside the window may never deliver the closing `dragExit`, which would
  leave the feedback stuck on. The net is a plain `mouseMoved` listener on the main panel:
  ordinary mouse motion is **not** delivered during a native drag, so seeing any proves no drag
  is in progress and the state can be cleared. Deliberately *not* keyed off window focus, which
  could misfire mid-drag and disarm while the user is still dragging.

### Data flow: drop === Browse

The tail of `handleSelection` is extracted into `acceptSelectedFile(File)`:

```
selectedFile = file
updateFilePathLabel()
validateFile()
notifyUpdateCallback()
```

Both the Browse handler and the drop handler call it, so consequences follow from reuse with
**no new validation logic**:

- A dropped non-CSV or unreadable file surfaces the existing `ImportFileValidator` error in
  the error line — identical to Browse.
- `areInputsValid()` and the Import button gating are unchanged; they already read each
  selector's validation result.

## Headless safety (load-bearing)

`java.awt.dnd.DropTarget`'s constructor throws `HeadlessException` unconditionally when
`GraphicsEnvironment.isHeadless()` — it is the first thing the constructor checks. This was
verified with a standalone probe, not assumed.

This matters because `FileSelectorTest` matches `*SelectorTest`, which is exactly the
headless-safe subset Windows CI runs (see CLAUDE.md test caveats). Installing a `DropTarget`
unguarded would throw during `createPanel()` and break that job.

Therefore drop-target installation is guarded by `!GraphicsEnvironment.isHeadless()`.
`setDragState` is independent of the `DropTarget`, so the appearance and the state logic remain
fully testable headlessly — only the OS-level drag plumbing is skipped, and it could not
function without a display anyway. `FileDropArbiter.installSentinel` carries the same guard.

## Error handling

- Invalid/non-CSV drop → existing validation error in the error label; the selector reports
  no valid selection, so Import stays disabled by the existing gate.
- Non-file drag (text, etc.) → rejected at `dragOver`; no drop occurs and no visual change.
- Multiple files on one zone → first accepted, remainder ignored.
- Folder dropped → goes through the same path; existing validation rejects it as not a
  readable CSV.
- Transferable read failure → caught, `dropComplete(false)`, highlight cleared.

## Testing

Constructing real AWT drag events is fragile, so the drag *plumbing* is not unit-tested;
its two outcomes are tested directly instead.

- **`acceptSelectedFile(File)` funnel** — valid CSV fixture sets the value, validates, and
  fires the callback; a missing file is still recorded but reports invalid (so the error
  label can describe the problem while Import stays disabled).
- **`ARMED` is border-only** — dashed frame, fill and text untouched, so an armed picker cannot
  appear to claim a file headed for its neighbour.
- **`HOVERED` adds fill and text** — darkened to `DROP_COLOR_HOVER`, label reads
  `Drop your file here`.
- **`IDLE` restores exactly** — resting border and resting fill both back by identity.
- **Selected path survives a passing drag** — `HOVERED` then `IDLE` restores the shortened path,
  not the empty hint (the bug a naive "reset to empty text" would introduce).
- **Insets preserved in every state** — loops over all three `DragState` values, so nothing
  shifts mid-drag.
- **Resting layout unchanged** — the path label's parent is the same panel that holds the
  Browse button, proving no wrapper was introduced.
- **Drop coverage reaches the interior** — the claimed-target list contains the path label and the
  Browse button, not just the title. Guards ordering bug 1.
- **No nested panel is opaque** — asserted on a selector *with a help page linked*, since that is
  what produces the wrapper. Guards ordering bug 2.
- **Arbiter pairs the pickers** — entering one darkens it and leaves the sibling merely armed;
  crossing over moves the fill without either dropping to idle; `reset()` returns both to resting.
- **Deferred exit is cancellable** — enter-after-exit keeps the feedback up; a lone exit clears it
  once the queued task runs. Flushed with `SwingUtilities.invokeAndWait`.
- **SAVE mode untouched** — keeps `No file selected`, and neither its frame nor its fill changes
  even if `setDragState(HOVERED)` is called directly.

A trap worth recording: **`CompoundBorder` does not implement `equals()`**, and every
`createPanel()` builds its own instance. Comparing one selector's border against another's
captured value compares two distinct objects, so such an assertion passes or fails for reasons
unrelated to what it claims to test. Capture resting borders per panel.

Both are reached via `ReflectionUtils.invokeMethod`. Reflection is necessary rather than
lazy: `FileSelector` lives in `com.echo.ui.selector` while its test lives in
`com.echo.ui.dialog.selector`, so package-visibility would not reach across.

Existing structural tests stay green trivially: with the well gone, the component tree this
change produces is identical to the one before it. Only the border and the label text ever
differ, and only while a drag is overhead.

## Porting / logging

This is a direct code change in the public distribution repo, so it must be logged in
`~/Desktop/HappyCamper-pending-patches.md` (symptoms, root cause, diff, porting notes) for
porting to the dev repo. This spec doc lives under `docs/` and is docs-only, so it does not
itself require patch logging.

## Follow-on ideas (not now)

- Shared drop zone + header-sniffing auto-assignment of Camper vs Activity.
- Recent-files surface (today only implicit via the static `ImportSettings` cache).
- Inline preview (row count / detected headers) before committing to import.
