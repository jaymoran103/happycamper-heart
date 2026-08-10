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
