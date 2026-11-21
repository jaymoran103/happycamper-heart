package com.echo.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.List;

import javax.swing.JFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.echo.feedback.FeedbackReport;
import com.echo.feedback.FeedbackReport.Kind;

class FeedbackDialogTest {

    /**
     * These construct real windows. Without a display that crashes the surefire fork outright
     * rather than failing cleanly, so skip instead - same guard the other dialog tests use.
     */
    @BeforeEach
    void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless());
    }

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
