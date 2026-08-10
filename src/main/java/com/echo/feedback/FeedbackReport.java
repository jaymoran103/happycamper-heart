package com.echo.feedback;

import java.util.List;

import com.echo.HappyCamper;

/**
 * The complete contents of one feedback submission, already in final form.
 *
 * {@link #toPlainText()} is the single definition of the wire format: the dialog preview renders
 * it verbatim and the saved file contains exactly it, so what the user reviews cannot drift from
 * what is written.
 *
 * @param kind which entry point produced this, used only to word the summary line
 * @param environment app version and platform, from {@link #environmentString()}
 * @param columnNames names of the loaded roster's columns, feature-generated ones included -
 *                    names of columns, never their contents
 * @param quirkLines summary lines from {@link WarningSummarizer}, already free of camper names
 * @param userNote whatever the user typed; may be blank
 */
public record FeedbackReport(Kind kind,
                             String environment,
                             List<String> columnNames,
                             List<String> quirkLines,
                             String userNote) {

    /** Which entry point produced the report. The entry point implies the kind; there is no picker. */
    public enum Kind { DATA_QUIRK, GENERAL }

    public FeedbackReport {
        columnNames = List.copyOf(columnNames);
        quirkLines = List.copyOf(quirkLines);
        userNote = userNote == null ? "" : userNote;
    }

    /** Returns a copy carrying a different note, for live preview as the user types. */
    public FeedbackReport withNote(String note) {
        return new FeedbackReport(kind, environment, columnNames, quirkLines, note);
    }

    /** App version and platform, e.g. {@code "HappyCamper 2.5.0 · Windows 11 · Java 25"}. */
    public static String environmentString() {
        return String.format("%s · %s %s · Java %s",
                HappyCamper.NAME_VERSION,
                System.getProperty("os.name", "unknown OS"),
                System.getProperty("os.version", ""),
                System.getProperty("java.version", "unknown"));
    }

    /** The exact text that gets previewed, copied, and written to disk. */
    public String toPlainText() {
        StringBuilder text = new StringBuilder();
        text.append(environment).append("\n");

        if (!columnNames.isEmpty()) {
            text.append("\nRoster columns: ").append(String.join(", ", columnNames)).append("\n");
        }

        if (!quirkLines.isEmpty()) {
            text.append("\nData quirks (").append(quirkLines.size())
                .append("), camper names removed:\n");
            for (String line : quirkLines) {
                text.append("  ").append(line).append("\n");
            }
        }

        if (!userNote.isBlank()) {
            text.append("\nNote from user:\n  ")
                .append(userNote.strip().replace("\n", "\n  "))
                .append("\n");
        }

        return text.toString();
    }
}
