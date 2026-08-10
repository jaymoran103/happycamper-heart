package com.echo.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.echo.feedback.FeedbackDelivery;
import com.echo.feedback.FeedbackReport;
import com.echo.ui.elements.HoverButton;

/**
 * Shows the user exactly what a feedback submission contains, takes their note, and saves it.
 *
 * The preview is the backstop for WarningSummarizer: if a future warning type is summarized
 * wrongly, the user sees it here before anything is written or sent.
 */
public class FeedbackDialog extends DialogBase {

    /** Taller than DIALOG_HEIGHT_STANDARD so the preview shows a usable number of lines. */
    private static final int DIALOG_HEIGHT = 430;

    /** Roughly eight lines of the preview - enough to read a short report without scrolling. */
    private static final int PREVIEW_HEIGHT = 150;

    private final FeedbackDelivery delivery = FeedbackDelivery.production();
    private final LocalDateTime openedAt = LocalDateTime.now();

    private FeedbackReport report;
    private Path target;

    private final JTextArea previewArea = new JTextArea();
    private final JTextArea noteArea = new JTextArea(3, 20);
    private final JLabel destinationLabel = new JLabel();

    public FeedbackDialog(Window parentWindow, FeedbackReport initialReport) {
        super(parentWindow, true, DialogConstants.DIALOG_WIDTH_STANDARD);
        this.report = initialReport;
        this.target = FeedbackDelivery.defaultTarget(openedAt);

        setTitle("Send feedback");

        // DialogBase fixes every dialog at DIALOG_HEIGHT_STANDARD, which squashes the preview
        // to about two lines here. Override before laying out; setResizable(false) still holds.
        setSize(DialogConstants.DIALOG_WIDTH_STANDARD, DIALOG_HEIGHT);

        buildContents();
        refreshPreview();
        refreshDestination();

        // WarningDialog sets alwaysOnTop; without this the dialog opens behind its own parent.
        setAlwaysOnTop(true);
        setLocationRelativeTo(parentWindow);
    }

    /** Test seam: the exact text that would be written. */
    String previewText() {
        return previewArea.getText();
    }

    /** Test seam: simulates the user typing a note. */
    void setNoteForTesting(String note) {
        noteArea.setText(note);
    }

    private void buildContents() {
        JPanel panel = getMainPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DialogConstants.DIALOG_COLOR_MAIN);

        // Deliberately short and un-wrapped: an HTML width hint did not reliably force a wrap
        // here, and the fixed-width dialog silently clips the overflow.
        panel.add(wrap(new JLabel("Nothing is sent automatically - you choose whether to send it.")));

        panel.add(wrap(new JLabel("What gets sent:")));
        previewArea.setEditable(false);
        previewArea.setLineWrap(false);
        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setPreferredSize(new Dimension(
                DialogConstants.COMPONENT_WIDTH_STANDARD, PREVIEW_HEIGHT));
        panel.add(wrap(previewScroll));

        panel.add(wrap(new JLabel("Anything you'd like to add?")));
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { refreshPreview(); }
            public void removeUpdate(DocumentEvent e)  { refreshPreview(); }
            public void changedUpdate(DocumentEvent e) { refreshPreview(); }
        });
        panel.add(wrap(new JScrollPane(noteArea)));

        JPanel destinationRow = new JPanel(new BorderLayout(
                DialogConstants.COMPONENT_SPACING, 0));
        destinationRow.setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        destinationRow.add(destinationLabel, BorderLayout.CENTER);
        JButton chooseButton = new HoverButton("Save elsewhere…");
        chooseButton.addActionListener(e -> chooseTarget());
        destinationRow.add(chooseButton, BorderLayout.EAST);
        panel.add(wrap(destinationRow));

        // Action buttons belong in the base class's PAGE_END panel, which every other dialog
        // here uses for navigation. Leaving it empty would show as a dead grey band.
        JButton sendButton = new HoverButton("Save & Open WhatsApp");
        sendButton.addActionListener(e -> saveThen(true));
        JButton saveButton = new HoverButton("Just Save");
        saveButton.addActionListener(e -> saveThen(false));
        JButton cancelButton = new HoverButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JPanel bottom = getBottomPanel();
        bottom.add(sendButton);
        bottom.add(saveButton);
        bottom.add(cancelButton);
    }

    /**
     * BoxLayout stretches children to the container width and lets them keep their own
     * alignment; wrapping in a BorderLayout panel keeps labels from being clipped, per the
     * fixed-size-container lesson in CLAUDE.md.
     */
    private JPanel wrap(Component component) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(DialogConstants.DIALOG_COLOR_MAIN);
        holder.setBorder(BorderFactory.createEmptyBorder(
                DialogConstants.COMPONENT_PADDING, DialogConstants.COMPONENT_PADDING,
                DialogConstants.COMPONENT_PADDING, DialogConstants.COMPONENT_PADDING));
        holder.add(component, BorderLayout.CENTER);
        return holder;
    }

    private void refreshPreview() {
        report = report.withNote(noteArea.getText());
        previewArea.setText(report.toPlainText());
        previewArea.setCaretPosition(0);
    }

    private void refreshDestination() {
        Path parent = target.getParent();
        String shown = parent == null
                ? target.getFileName().toString()
                : parent.getFileName() + File.separator + target.getFileName();
        destinationLabel.setText("Saves to: " + shown);
    }

    private void chooseTarget() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(target.toFile());
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            target = chooser.getSelectedFile().toPath();
            refreshDestination();
        }
    }

    private void saveThen(boolean openWhatsApp) {
        try {
            Path written = delivery.save(report, target);
            boolean opened = openWhatsApp && delivery.openWhatsApp(report);
            String message = opened
                    ? "Saved to:\n" + written + "\n\nWhatsApp is opening - attach this file and send it."
                    : "Saved to:\n" + written
                      + "\n\nThe text is also on your clipboard. Send the file to Jay however you normally would.";
            JOptionPane.showMessageDialog(this, message, "Feedback saved",
                                          JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save the file: " + ex.getMessage()
                  + "\n\nTry \"Save elsewhere…\" and pick a different folder.",
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
