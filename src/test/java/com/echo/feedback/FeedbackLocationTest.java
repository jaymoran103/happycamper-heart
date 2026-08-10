package com.echo.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class FeedbackLocationTest {

    private static final Path HOME = Path.of("/home/user");

    /** Stands in for "this directory exists and is writable" without touching disk. */
    private static Predicate<Path> usable(Path... paths) {
        Set<Path> set = Set.of(paths);
        return set::contains;
    }

    @Test void xdgDownloadDirWinsWhenSet() {
        Path dir = FeedbackLocation.resolveDownloadsDir(
                Map.of("XDG_DOWNLOAD_DIR", "/home/user/Hämtningar"),
                HOME,
                usable(HOME.resolve("Downloads")));
        assertEquals(Path.of("/home/user/Hämtningar"), dir);
    }

    @Test void blankXdgIsIgnored() {
        Path dir = FeedbackLocation.resolveDownloadsDir(
                Map.of("XDG_DOWNLOAD_DIR", "   "),
                HOME,
                usable(HOME.resolve("Downloads")));
        assertEquals(HOME.resolve("Downloads"), dir);
    }

    @Test void usesHomeDownloadsWhenPresent() {
        Path dir = FeedbackLocation.resolveDownloadsDir(
                Map.of(), HOME, usable(HOME.resolve("Downloads")));
        assertEquals(HOME.resolve("Downloads"), dir);
    }

    @Test void fallsBackToHomeWhenDownloadsMissing() {
        Path dir = FeedbackLocation.resolveDownloadsDir(Map.of(), HOME, usable());
        assertEquals(HOME, dir);
    }

    @Test void fallsBackToHomeWhenDownloadsNotWritable() {
        // usable() models "exists AND writable" as one predicate; an unwritable
        // Downloads folder is indistinguishable from a missing one here, by design.
        Path dir = FeedbackLocation.resolveDownloadsDir(Map.of(), HOME, p -> false);
        assertEquals(HOME, dir);
    }
}
