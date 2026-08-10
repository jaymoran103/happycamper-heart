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

    @Test void marksMissingCellsAsBlank() {
        WarningManager manager = new WarningManager();
        // build_unmatchedActivity reads Rounds Assigned, which an unmatched row often lacks.
        manager.logWarning(RosterWarning.build_unmatchedActivity(camperRow("Zed", "Zephyr")));

        String out = joined(manager);
        assertFalse(out.contains("Zed"), "camper name leaked: " + out);
        assertTrue(out.contains("(blank)"),
                   "a missing field should be visible, not an empty gap: " + out);
    }

    @Test void emptyManagerProducesNoLines() {
        assertTrue(WarningSummarizer.summarize(new WarningManager()).isEmpty());
    }
}
