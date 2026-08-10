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
 * This is a summary, not anonymization - it does not and cannot reproduce a problem. Do not grow
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
        for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry
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
     * Renders an absent or empty cell visibly. A missing field is itself a useful signal - often
     * the whole point of the report - so it must not be silently indistinguishable from a cell
     * that was never part of the warning.
     */
    private static String blankMarker(String cell) {
        return (cell == null || cell.isBlank()) ? "(blank)" : cell;
    }

    /**
     * Replaces letters and digits with placeholders while preserving length, punctuation, and
     * spacing - enough to see why a value failed a format check, without revealing the value.
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
        String value = blankMarker(cell);
        return value.length() <= MAX_CELL_LENGTH
                ? value
                : value.substring(0, MAX_CELL_LENGTH - 1) + "…";
    }
}
