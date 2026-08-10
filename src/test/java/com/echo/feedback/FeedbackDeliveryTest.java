package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.feedback.FeedbackReport.Kind;

class FeedbackDeliveryTest {

    private final List<URI> opened = new ArrayList<>();
    private final List<String> copied = new ArrayList<>();

    private FeedbackDelivery delivery() {
        return new FeedbackDelivery(opened::add, copied::add);
    }

    private static FeedbackReport report() {
        return new FeedbackReport(Kind.DATA_QUIRK, "HappyCamper 2.5.0",
                                  List.of("Grade"), List.of("BAD_DATA_FORMAT  Grade | 6th "),
                                  "grade looks wrong");
    }

    @Test void fileNameIsFeedbackNotReport() {
        String name = FeedbackDelivery.defaultFileName(LocalDateTime.of(2026, 8, 9, 14, 32));
        assertEquals("HappyCamper-feedback-2026-08-09-1432.txt", name);
        assertFalse(name.contains("report"), "input files are already called reports: " + name);
    }

    @Test void savesTheExactPreviewText(@TempDir Path dir) throws IOException {
        Path written = delivery().save(report(), dir.resolve("out.txt"));
        assertEquals(report().toPlainText(), Files.readString(written));
    }

    @Test void savingAlsoCopiesToClipboard(@TempDir Path dir) throws IOException {
        delivery().save(report(), dir.resolve("out.txt"));
        assertEquals(List.of(report().toPlainText()), copied);
    }

    @Test void savingCreatesMissingParentDirectories(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("nested").resolve("deeper").resolve("out.txt");
        assertTrue(Files.exists(delivery().save(report(), target)));
    }

    @Test void noBrowserOpensWithoutAConfiguredNumber() {
        // Local builds leave the number empty, so there is nothing to open.
        assertFalse(delivery().openWhatsApp(report()));
        assertTrue(opened.isEmpty());
    }
}
