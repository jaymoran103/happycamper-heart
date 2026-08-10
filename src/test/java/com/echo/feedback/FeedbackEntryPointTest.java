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
