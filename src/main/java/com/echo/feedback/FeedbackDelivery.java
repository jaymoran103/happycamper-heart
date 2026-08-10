package com.echo.feedback;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Saves a feedback file, copies its text to the clipboard, and opens a prefilled WhatsApp link.
 *
 * The three steps are ordered so each is useful if the next fails: the file exists even when the
 * clipboard is unavailable, and the text is on the clipboard even when no browser can be opened.
 * The user is never left with nothing.
 *
 * Nothing here transmits. Opening the link hands off to the user's browser; they still have to
 * attach the file and press send.
 */
public final class FeedbackDelivery {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private final Consumer<URI> browser;
    private final Consumer<String> clipboard;

    /** Seams exist so tests can assert what would be opened and copied without doing either. */
    public FeedbackDelivery(Consumer<URI> browser, Consumer<String> clipboard) {
        this.browser = browser;
        this.clipboard = clipboard;
    }

    /** Wires up the real browser and system clipboard, each degrading quietly if unavailable. */
    public static FeedbackDelivery production() {
        return new FeedbackDelivery(FeedbackDelivery::browseQuietly,
                                    FeedbackDelivery::copyQuietly);
    }

    /**
     * The suggested file name. Called "feedback" rather than "report" on purpose: the app's input
     * files are themselves reports exported from another service, and one of those may well be
     * sitting in the same folder.
     */
    public static String defaultFileName(LocalDateTime when) {
        return "HappyCamper-feedback-" + STAMP.format(when) + ".txt";
    }

    /** The default full path, in the user's Downloads folder. */
    public static Path defaultTarget(LocalDateTime when) {
        return FeedbackLocation.downloadsDir().resolve(defaultFileName(when));
    }

    /**
     * Writes the report and copies its text to the clipboard.
     *
     * @return the path actually written
     * @throws IOException if the file cannot be written; the caller shows this to the user
     */
    public Path save(FeedbackReport report, Path target) throws IOException {
        String text = report.toPlainText();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(target, text, StandardCharsets.UTF_8);
        clipboard.accept(text);
        return target;
    }

    /**
     * Opens a prefilled WhatsApp link, if this build has a number configured.
     *
     * Returns true when a link was dispatched, not when the browser demonstrably opened -
     * browseQuietly swallows failures by design, because at that point the file is already saved
     * and there is nothing better to offer the user.
     *
     * @return false when no number is configured, in which case the caller should tell the user
     *         to send the saved file however they normally would
     */
    public boolean openWhatsApp(FeedbackReport report) {
        String summary = switch (report.kind()) {
            case DATA_QUIRK -> "HappyCamper feedback: something looks wrong with my roster data";
            case GENERAL -> "HappyCamper feedback";
        };
        Optional<URI> uri = FeedbackContact.whatsappUri(FeedbackContact.whatsappNumber(),
                                                        summary + " (" + report.environment() + ")");
        uri.ifPresent(browser);
        return uri.isPresent();
    }

    private static void browseQuietly(URI uri) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception ignored) {
            // Locked-down desktops throw here. The file is already saved and the text copied,
            // so the user still has everything they need; the dialog says where it went.
        }
    }

    private static void copyQuietly(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                   .setContents(new StringSelection(text), null);
        } catch (Exception ignored) {
            // Headless or clipboard-less environments. Saving already succeeded.
        }
    }
}
