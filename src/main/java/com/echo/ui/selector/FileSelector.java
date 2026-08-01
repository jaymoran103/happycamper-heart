package com.echo.ui.selector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import com.echo.service.FileHandler;
import com.echo.ui.dialog.DialogConstants;
import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HoverButton;
import com.echo.validation.ExportFileValidator;
import com.echo.validation.ImportFileValidator;
import com.echo.validation.ValidationResult;

/**
 * FileSelector facilitates selecting files in an input dialog.
 * This class stores a file reference and builds a panel that allows the user to select a file.
 */
public class FileSelector extends InputSelector<File> {
    /**
     * Enum for file selection mode.
     */
    public enum SelectionMode {
        OPEN,
        SAVE
    }

    /**
     * How this selector is currently reacting to a file drag.
     *
     * Two tiers, so a user can see which widgets accept a file before committing to one:
     * {@link #ARMED} says "a file is in flight and I would take it", {@link #HOVERED} says "and it
     * will land here". Driven by {@link FileDropArbiter}, since only the dialog can know the
     * difference.
     */
    public enum DragState {
        /** No drag in progress; the selector shows its resting frame. */
        IDLE,
        /** A drag is somewhere in the dialog but not over this selector. */
        ARMED,
        /** A drag is directly over this selector. */
        HOVERED
    }

    private File selectedFile;
    private ValidationResult<File> validationResult;

    private final SelectionMode mode;
    private final String[] extensions;
    private final String extensionDescription;

    private JLabel filePathLabel;
    private JLabel errorLabel;

    // The selector's resting frame and fill, captured on first drag so they can be restored exactly
    private Border restingPanelBorder;
    private Color restingBackground;

    // Owns the two-tier feedback. Shared with sibling pickers when a dialog supplies one; otherwise
    // lazily created as a solo arbiter so a standalone FileSelector still behaves sensibly.
    private FileDropArbiter dropArbiter;

    // Every component this selector claimed as a drop target. Doubles as the idempotency record and
    // as the only way to assert drop coverage headlessly, where real DropTargets cannot exist.
    private final List<JComponent> claimedDropTargets = new ArrayList<>();

    // Text shown in the path label when nothing is selected. Becomes a drop hint in OPEN mode.
    private String emptyLabelText = DEFAULT_LABEL_TEXT;

    private static final String BUTTON_TEXT = "Browse";
    private static final String DEFAULT_LABEL_TEXT = "No file selected";
    private static final String DEFAULT_ERRORLABEL_TEXT = " ";

    // The empty and dragging hints stay in one voice, both anchored on "Drop", so the swap reads as
    // the sentence focusing rather than changing register.
    private static final String DROP_HINT_TEXT = "Drop a CSV file, or Browse";
    private static final String DROP_ACTIVE_TEXT = "Drop your file here";

    // Dashed drag border. The thickness must stay 2f to match EtchedBorder's 2px insets - see
    // buildDragBorder.
    private static final float DRAG_BORDER_THICKNESS = 2f;

    // Long strokes with wide gaps: a finer 4/3 dash reads as busy along a 580px edge
    private static final float DRAG_DASH_LENGTH = 6f;
    private static final float DRAG_DASH_SPACING = 4f;


    /**
     * Constructor for file selector with specified mode and extensions.
     *
     * @param title Label text at the top of selector box
     * @param mode enum indicating selection mode (OPEN or SAVE)
     * @param extensions File extensions to filter (e.g., "csv", "txt")
     * @param extensionDescription Description for the file filter
     */
    public FileSelector(String title, SelectionMode mode, String[] extensions, String extensionDescription) {
        super(title);
        this.mode = mode;
        this.extensions = extensions;
        this.extensionDescription = extensionDescription;
    }

    /**
     * Constructor for file selector with specified mode and no extension filter.
     *
     * @param title Label text at the top of selector box
     * @param mode The selection mode (OPEN or SAVE)
     */
    public FileSelector(String title, SelectionMode mode) {
        this(title, mode, null, null);
    }

    @Override
    protected void buildSelectorPanel(JPanel panel) {
        // BorderLayout constrains the label to the remaining width, so long paths
        // ellipsize ("...") instead of wrapping to a clipped second row like FlowLayout
        JPanel fileSelectionPanel = DialogUtils.createAlignedPanel(new BorderLayout(5, 0));

        // Create the browse button
        JButton browseButton = new HoverButton(BUTTON_TEXT);
        browseButton.addActionListener(this::handleSelection);

        // Create the file path label
        filePathLabel = new JLabel(DEFAULT_LABEL_TEXT);

        fileSelectionPanel.add(browseButton, BorderLayout.WEST);
        fileSelectionPanel.add(filePathLabel, BorderLayout.CENTER);

        if (mode == SelectionMode.OPEN) {
            // The whole selector is the drop target, and its own frame carries the feedback. So at
            // rest nothing is added to the layout at all - the affordance is purely the hint text,
            // and the frame only changes while a drag is overhead.
            emptyLabelText = DROP_HINT_TEXT;
        }

        // Create error label for validation messages
        errorLabel = new JLabel(DEFAULT_ERRORLABEL_TEXT);
        errorLabel.setForeground(DialogConstants.TEXT_COLOR_ERROR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add components to the main panel
        panel.add(fileSelectionPanel);
        DialogUtils.addVerticalSpacing(panel, 5);
        panel.add(errorLabel);

        // Both of these walk the component tree, so they must run only once every child exists -
        // including the title, which InputSelector.createPanel added before calling us. Doing either
        // earlier silently misses whatever had not been added yet.
        if (mode == SelectionMode.OPEN) {
            makeContainersTransparent(panel);
            installFileDropTargets(panel);
        }

        // componentHeight is deliberately left at the default. Nothing was added to the resting
        // layout, so the standard 85px still fits - no need for COMPONENT_HEIGHT_LARGE.

        // Update label if file is already selected
        updateFilePathLabel();

        // Validate the file if one is already selected
        if (selectedFile != null) {
            validateFile();
        }
    }


    private void handleSelection(ActionEvent e) {
        File selection = selectedFile;

        if (mode == SelectionMode.OPEN) {
            selection = FileHandler.getLoadFile(null, selectedFile);
        } else {
            selection = FileHandler.getSaveFile(null, selectedFile);
        }

        if (selection == null) {
            return;
        }

        // // Ensure file has the correct extension for save mode
        // if (mode == SelectionMode.SAVE && extensions != null && extensions.length > 0) {
        //     selection = ExportFileValidator.ensureExtension(selection, extensions);
        // }

        acceptSelectedFile(selection);
    }


    /**
     * Records a chosen file and runs the common post-selection steps: update the path label,
     * validate, and notify the parent dialog so it can re-gate its buttons.
     *
     * Shared by the Browse button and the drop handler so both routes behave identically - same
     * validation, same callback, no drop-specific logic.
     *
     * @param file The file the user chose, via Browse or via a drop
     */
    private void acceptSelectedFile(File file) {
        selectedFile = file;
        updateFilePathLabel();

        // Validate the file and update error message
        validateFile();

        notifyUpdateCallback();
    }


    /**
     * Applies one of the three drag states to the selector's own frame.
     *
     * Single source of truth for all three cues - border, fill and hint text - so they can never
     * disagree. Called by {@link FileDropArbiter}, which decides which state each picker is in.
     *
     * <ul>
     *   <li>{@code IDLE} - resting frame, resting fill, path or empty hint</li>
     *   <li>{@code ARMED} - dashed frame, resting fill, text unchanged: "I would take a file"</li>
     *   <li>{@code HOVERED} - dashed frame, darkened fill, "Drop your file here": "it lands here"</li>
     * </ul>
     *
     * Only the fill and the text separate the two active tiers, so a picker that is merely armed
     * never claims a file that is actually headed for its neighbour.
     *
     * The resting border and fill are captured rather than rebuilt, so this stays correct if
     * {@link InputSelector#createPanel()} ever changes how it frames a selector. The drag border
     * deliberately keeps the same 2px insets as the etched border it replaces, so the content area
     * never changes size and nothing shifts or re-ellipsizes mid-drag.
     *
     * @param state The state to show; ignored outside OPEN mode
     */
    public void setDragState(DragState state) {
        // SAVE mode never shows drag feedback, and cachedPanel is only assigned at the end of
        // createPanel - which always runs long before any drag can reach the widget
        if (mode != SelectionMode.OPEN || cachedPanel == null) {
            return;
        }

        if (restingPanelBorder == null) {
            restingPanelBorder = cachedPanel.getBorder();
            restingBackground = cachedPanel.getBackground();
        }

        cachedPanel.setBorder(state == DragState.IDLE ? restingPanelBorder : buildDragBorder());
        cachedPanel.setBackground(state == DragState.HOVERED
            ? DialogConstants.DROP_COLOR_HOVER
            : restingBackground);

        // getShortenedPath handles both restore cases: it falls back to emptyLabelText when nothing
        // is selected, and returns the shortened path when something is.
        filePathLabel.setText(state == DragState.HOVERED
            ? DROP_ACTIVE_TEXT
            : getShortenedPath(selectedFile));

        cachedPanel.repaint();
    }


    /**
     * Puts this selector's drag feedback under a shared arbiter, so sibling pickers in the same
     * dialog arm together.
     *
     * @param arbiter The dialog's arbiter
     */
    public void setDropArbiter(FileDropArbiter arbiter) {
        this.dropArbiter = arbiter;
        arbiter.register(this);

        // By the time a dialog wires this up the outer panel exists, so claim it too. Otherwise the
        // selector's own 7px frame ring is left for the arbiter's sentinel and reads as background -
        // armed rather than hovered - right at the widget's edge. Installation is idempotent.
        if (mode == SelectionMode.OPEN && cachedPanel != null) {
            installFileDropTargets(cachedPanel);
        }
    }


    /**
     * The arbiter driving this selector, created on demand so a selector used outside a dialog that
     * supplies one still shows sane single-widget feedback.
     */
    private FileDropArbiter arbiter() {
        if (dropArbiter == null) {
            setDropArbiter(new FileDropArbiter());
        }
        return dropArbiter;
    }


    /**
     * Builds the border shown while a drag is overhead, replacing the selector's resting frame.
     *
     * A dashed stroke is used only as a transient state, never as resting chrome - that is what
     * keeps it from reading as foreign next to the app's etched surfaces. The 2f thickness is
     * load-bearing: {@code StrokeBorder} derives its insets from the stroke width, so 2f yields the
     * same 2px inset as {@link javax.swing.border.EtchedBorder} and the swap costs no layout shift.
     */
    private Border buildDragBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(DialogConstants.DROP_BORDER_ACTIVE,
                DRAG_BORDER_THICKNESS, DRAG_DASH_LENGTH, DRAG_DASH_SPACING, false),
            new EmptyBorder(DialogConstants.COMPONENT_PADDING, DialogConstants.COMPONENT_PADDING,
                DialogConstants.COMPONENT_PADDING, DialogConstants.COMPONENT_PADDING));
    }


    /**
     * Registers the whole selector as a file drop target.
     *
     * Every component in the subtree gets its own target, so the entire selector is live rather than
     * one inner zone - a drag anywhere over it, including the Browse button and the title, counts.
     *
     * Not covered: the outer panel's own 7px border-and-padding ring, which is built by
     * {@link InputSelector#createPanel()} after this runs and so is not reachable here. That ring is
     * the selector's frame, not its interior, and is not a realistic aim point.
     *
     * Skipped when headless: {@link DropTarget}'s constructor throws {@link java.awt.HeadlessException}
     * unconditionally without a display, and FileSelectorTest runs in the headless-safe subset that
     * Windows CI executes (see CLAUDE.md test caveats). {@link #setDragState(DragState)} does not
     * depend on the DropTarget, so appearance and state stay fully testable; only the OS-level drag
     * plumbing is skipped, and it could not function without a display anyway.
     *
     * @param root The selector's content panel, whose whole subtree becomes droppable
     */
    private void installFileDropTargets(Component root) {
        // Idempotent, so this can safely run again from setDropArbiter to pick up the outer frame
        if (root instanceof JComponent jComponent && !claimedDropTargets.contains(jComponent)) {
            claimedDropTargets.add(jComponent);

            // Only the AWT registration is display-bound; the claim itself is recorded either way so
            // the coverage invariant stays assertable in the headless test subset
            if (!GraphicsEnvironment.isHeadless() && jComponent.getDropTarget() == null) {
                enableFileDrop(jComponent);
            }
        }

        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                installFileDropTargets(child);
            }
        }
    }


    /**
     * Clears the opaque flag on every nested panel so the outer panel's hover fill shows through.
     *
     * Invisible at rest, because none of these carry a background of their own. It has to be a walk
     * rather than a couple of named panels: the title is wrapped in an opaque {@code JPanel} by
     * {@link DialogUtils#combineWithHelpButton} whenever a help page is linked, and that wrapper kept
     * painting the resting colour as a lighter strip across the top of an otherwise darkened selector.
     * Anything added to a selector later would have the same problem.
     *
     * Deliberately only {@code JPanel}s - buttons and labels paint themselves and must keep their own
     * opacity.
     *
     * @param root The subtree to make transparent; the outer panel is excluded since it paints the fill
     */
    private void makeContainersTransparent(Component root) {
        if (root instanceof JPanel nested) {
            nested.setOpaque(false);
        }

        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                makeContainersTransparent(child);
            }
        }
    }




    /**
     * Makes a single component accept file drags, funnelling any drop through the shared
     * {@link #acceptSelectedFile(File)} path.
     *
     * @param target The component to accept drags on
     */
    private void enableFileDrop(JComponent target) {
        new DropTarget(target, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void dragEnter(DropTargetDragEvent dtde) { updateDrag(dtde); }
            @Override public void dragOver(DropTargetDragEvent dtde) { updateDrag(dtde); }
            @Override public void dragExit(DropTargetEvent dte) { arbiter().exit(); }
            @Override public void drop(DropTargetDropEvent dtde) { handleDrop(dtde); }

            // Only light up for drags actually carrying files; anything else is rejected outright,
            // so a text drag produces no visual change at all.
            private void updateDrag(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    arbiter().enter(FileSelector.this);
                } else {
                    dtde.rejectDrag();
                }
            }
        });
    }


    /**
     * Takes the first dropped file and hands it to the shared selection path.
     *
     * First-file-wins: dropping several files on one zone uses one and ignores the rest, keeping
     * each zone's role explicit rather than guessing assignments.
     *
     * @param dtde The drop event
     */
    private void handleDrop(DropTargetDropEvent dtde) {
        arbiter().reset();

        if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.rejectDrop();
            return;
        }

        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        try {
            Object data = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            List<?> files = (List<?>) data;

            if (files != null && !files.isEmpty() && files.get(0) instanceof File file) {
                // Same tail as Browse, so an unreadable or non-CSV drop surfaces the existing
                // ImportFileValidator error and leaves Import disabled by the existing gate.
                acceptSelectedFile(file);
                dtde.dropComplete(true);
                return;
            }
            dtde.dropComplete(false);
        } catch (Exception ex) {
            dtde.dropComplete(false);
        }
    }




    /**
     * Updates the file path label with the selected file path.
     */
    private void updateFilePathLabel() {
        if (filePathLabel != null) {
            filePathLabel.setText(getShortenedPath(selectedFile));
            // Full path on hover, since the label may be ellipsized
            filePathLabel.setToolTipText(selectedFile == null ? null : selectedFile.getAbsolutePath());
        }
    }


    /**
     * Validates the selected file and updates the error label.
     * For save mode, checks that the file can be written to and has a valid extension.
     * For open mode, checks that the file exists and can be read.
     *
     * @return true if the file is valid, false otherwise
     */
    private boolean validateFile() {
        if (errorLabel == null) {
            return false;
        }

        // Get validation result
        doValidation();

        if (!validationResult.isValid()) {
            errorLabel.setText(validationResult.getErrorMessage());
            return false;
        }

        // Clear error message
        errorLabel.setText(DEFAULT_ERRORLABEL_TEXT);
        return true;
    }

    @Override
    public File getValue() {
        return selectedFile;
    }

    @Override
    public void setValue(File value) {
        this.selectedFile = value;
        updateFilePathLabel();
        validateFile();
    }

    @Override
    public boolean hasSelection() {
        return selectedFile != null;
    }

    /**
     * Gets the validation result for the currently selected file.
     * This can be used by parent components to display validation errors.
     *
     * @return The validation result, or a failure result if no file is selected
     */
    public ValidationResult<File> getValidationResult() {
        return validationResult;
    }


    private void doValidation(){
        ValidationResult<File> result;
        if (selectedFile==null){
            result = ValidationResult.failure("No file selected", "Please select a file");
        } else if (mode == SelectionMode.OPEN || (extensions != null && extensions.length > 0)) {
            result = validateChosenPath(selectedFile);
        } else {
            result = ValidationResult.success(selectedFile);
        }
        this.validationResult = result;
    }

    private String getShortenedPath(File f){
        if (f==null){
            // Drop hint in OPEN mode, "No file selected" in SAVE mode
            return emptyLabelText;
        }

        // Show the immediate parent folder for context, e.g. "Downloads/campers.csv".
        // Note: File.separator is not regex-safe ("\" on Windows), so avoid String.split here.
        File parent = f.getParentFile();
        String name = f.getName();

        if (parent == null || parent.getName().isEmpty()){
            return name;
        }
        return parent.getName() + File.separator + name;
    }

    private ValidationResult<File> validateChosenPath(File selectedFile){
        return switch (mode) {
            case OPEN -> ImportFileValidator.<File>validateImportFile(selectedFile);
            case SAVE -> ExportFileValidator.validateExportFile(selectedFile, extensions);
        };
    }
    
    
    
}
