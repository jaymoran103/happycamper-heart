package com.echo.ui.dialog.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.ReflectionUtils;
import com.echo.automation.TestFiles;
import com.echo.ui.dialog.DialogConstants;
import com.echo.ui.help.PageContentBuilder.HelpPage;
import com.echo.ui.selector.FileDropArbiter;
import com.echo.ui.selector.FileSelector;
import com.echo.ui.selector.FileSelector.SelectionMode;

/**
 * Tests for the FileSelector class.
 */
public class FileSelectorTest {

    private FileSelector selector;
    private final String TITLE = "Test File Selector";
    private final String[] EXTENSIONS = {"csv"};
    private final String EXTENSION_DESCRIPTION = "CSV Files";

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        selector = new FileSelector(TITLE, SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);
    }

    @Test
    @DisplayName("Test selector creation with title")
    public void testSelectorCreationWithTitle() {
        // Create the panel
        JPanel panel = selector.createPanel();

        // Verify the panel was created
        assertNotNull(panel);

        // Find the title label
        JLabel titleLabel = findTitleLabel(panel);
        assertNotNull(titleLabel);
        assertTrue(titleLabel.getText().contains(TITLE));
    }

    @Test
    @DisplayName("Test getValue and setValue")
    public void testGetValueAndSetValue() {
        // Initial value should be null
        assertNull(selector.getValue());

        // Set a new value
        File testFile = TestFiles.MINI_CAMPERS.toFile();
        selector.setValue(testFile);

        // Verify the value was set
        assertEquals(testFile, selector.getValue());
    }

    @Test
    @DisplayName("Test hasSelection")
    public void testHasSelection() {
        // Initial value is null, so hasSelection should be false
        assertFalse(selector.hasSelection());

        // Set a file
        File testFile = TestFiles.MINI_CAMPERS.toFile();
        selector.setValue(testFile);

        // Now hasSelection should be true
        assertTrue(selector.hasSelection());

        // Set to null
        selector.setValue(null);

        // hasSelection should be false again
        assertFalse(selector.hasSelection());
    }

    @Test
    @DisplayName("Test file validation - valid file")
    public void testFileValidation_ValidFile() {
        // Create a test file in the temp directory
        File testFile = new File(tempDir, "test.csv");

        // Set the file
        selector.setValue(testFile);

        // For a valid file, hasSelection should be true
        assertTrue(selector.hasSelection());
    }

    @Test
    @DisplayName("Test file validation - invalid extension")
    public void testFileValidation_InvalidExtension() {
        // Create a test file with wrong extension
        File testFile = new File(tempDir, "test.txt");

        // Set the file
        selector.setValue(testFile);

        // For a file with invalid extension, hasSelection should still be true
        // because we're in OPEN mode and the file exists
        assertTrue(selector.hasSelection());

        // Create a selector in SAVE mode
        FileSelector saveSelector = new FileSelector(TITLE, SelectionMode.SAVE, EXTENSIONS, EXTENSION_DESCRIPTION);

        // Create the panel to initialize components
        JPanel panel = saveSelector.createPanel();
        assertNotNull(panel);

        // Set the file
        saveSelector.setValue(testFile);

        // Check if the validation result is invalid
        // The hasSelection method only checks if selectedFile != null, not if it's valid
        assertFalse(saveSelector.getValidationResult().isValid());
    }

    @Test
    @DisplayName("Test UI components")
    public void testUIComponents() {
        // Create the panel
        JPanel panel = selector.createPanel();

        // Verify the browse button exists
        JButton browseButton = findBrowseButton(panel);
        assertNotNull(browseButton);

        // Verify the file path label exists
        JLabel filePathLabel = findFilePathLabel(panel);
        assertNotNull(filePathLabel);

        // Verify the error label exists
        JLabel errorLabel = findErrorLabel(panel);
        assertNotNull(errorLabel);
    }

    @Test
    @DisplayName("Test update callback")
    public void testUpdateCallback() {
        // Create a custom selector for this test
        FileSelector testSelector = new FileSelector(TITLE, SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);

        // Create the panel first to initialize all components
        JPanel panel = testSelector.createPanel();
        assertNotNull(panel);

        // Create a flag to track if the callback was called
        boolean[] callbackCalled = new boolean[1];

        // Set the update callback
        testSelector.setUpdateCallback(() -> callbackCalled[0] = true);

        // Initially the callback should not have been called
        assertFalse(callbackCalled[0]);

        // Simulate a button click to trigger the callback
        try {
            // Use reflection to access the protected method from the parent class
            ReflectionUtils.invokeMethod(testSelector,"notifyUpdateCallback");
        } catch (Exception e) {
            fail("Failed to invoke notifyUpdateCallback: " + e.getMessage());
        }

        // Verify the callback was called
        assertTrue(callbackCalled[0]);
    }

    // ===== Drag-and-drop (OPEN mode only) =====

    @Test
    @DisplayName("Drop-accepting a valid CSV sets the value, validates, and notifies")
    public void testAcceptSelectedFile_validFile() throws Exception {
        // Build the panel so the error label exists - validation is a no-op without it
        selector.createPanel();

        boolean[] callbackCalled = new boolean[1];
        selector.setUpdateCallback(() -> callbackCalled[0] = true);

        // A real, readable .csv file passes OPEN-mode import validation
        File droppedFile = new File(tempDir, "dropped.csv");
        assertTrue(droppedFile.createNewFile());

        // The drop handler funnels the file through the same tail as Browse
        ReflectionUtils.invokeMethod(selector, "acceptSelectedFile", droppedFile);

        assertEquals(droppedFile, selector.getValue());
        assertTrue(selector.getValidationResult().isValid());
        assertTrue(callbackCalled[0], "Drop should notify the update callback so the dialog re-gates");
    }

    @Test
    @DisplayName("Drop-accepting an invalid file still records it but reports invalid")
    public void testAcceptSelectedFile_invalidFile() throws Exception {
        selector.createPanel();

        // Nonexistent file -> fails OPEN-mode validation
        File missingFile = new File(tempDir, "does-not-exist.csv");

        ReflectionUtils.invokeMethod(selector, "acceptSelectedFile", missingFile);

        // The file is still recorded, so the error label can describe the problem...
        assertEquals(missingFile, selector.getValue());
        // ...but validation reports it invalid, keeping the Import button disabled
        assertFalse(selector.getValidationResult().isValid());
    }

    @Test
    @DisplayName("OPEN mode adds nothing to the resting layout beyond the drop hint")
    public void testOpenMode_restingLayoutIsUnchanged() {
        JPanel panel = selector.createPanel();

        // The whole selector is the drop target, so there is no inner box - the path label sits
        // directly in the Browse row exactly as it does in SAVE mode
        JLabel pathLabel = ReflectionUtils.getFieldValue(selector, "filePathLabel");
        JButton browseButton = findBrowseButton(panel);
        assertEquals(browseButton.getParent(), pathLabel.getParent(),
            "The path label shares the Browse button's row, with no wrapper in between");

        assertEquals("Drop a CSV file, or Browse", pathLabel.getText());
    }

    @Test
    @DisplayName("Drop coverage reaches the path label and Browse button, not just the title")
    public void testDropTargets_coverTheWholeSelector() {
        selector.createPanel();

        List<JComponent> claimed = ReflectionUtils.getFieldValue(selector, "claimedDropTargets");
        JLabel pathLabel = ReflectionUtils.getFieldValue(selector, "filePathLabel");

        // Regression: installation used to run before the Browse row was added to the content panel,
        // so the walk never reached these. The arbiter's sentinel then claimed them as *background*,
        // which made hovering the label arm instead of darken and reverted its hint text mid-hover.
        assertTrue(claimed.contains(pathLabel),
            "The path label must be the picker's own drop target, not left for the sentinel");
        assertTrue(claimed.stream().anyMatch(c -> c instanceof JButton),
            "The Browse button must be covered too - the whole selector is the target");
    }

    @Test
    @DisplayName("Every nested panel is transparent, so no lighter strip survives the hover fill")
    public void testNestedPanels_areTransparentSoTheFillShows() {
        // A linked help page is what wraps the title in an opaque JPanel, which is where the bug was
        FileSelector helped = new FileSelector(TITLE, SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);
        helped.linkHelpPage(HelpPage.CAMPER_FILE);
        JPanel panel = helped.createPanel();

        // The outer panel paints the fill, so it stays opaque; everything nested must not
        assertTrue(panel.isOpaque(), "The outer panel paints the hover fill and must stay opaque");
        assertNoOpaqueNestedPanel(panel, panel);
    }

    /** Fails if any JPanel below the outer panel would paint over the hover fill. */
    private void assertNoOpaqueNestedPanel(Container container, JPanel outer) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel nested && nested != outer) {
                assertFalse(nested.isOpaque(),
                    "Nested panel would paint a strip over the hover fill: " + nested.getLayout());
            }
            if (component instanceof Container child) {
                assertNoOpaqueNestedPanel(child, outer);
            }
        }
    }

    @Test
    @DisplayName("ARMED shows the dashed frame but no fill, so it cannot claim a neighbour's file")
    public void testDragState_armedIsBorderOnly() {
        JPanel panel = selector.createPanel();

        Border restingBorder = panel.getBorder();
        Color restingBackground = panel.getBackground();
        assertNotNull(restingBorder);

        JLabel pathLabel = ReflectionUtils.getFieldValue(selector, "filePathLabel");

        selector.setDragState(FileSelector.DragState.ARMED);

        assertNotEquals(restingBorder, panel.getBorder(), "Armed swaps in the dashed frame");
        assertEquals(restingBackground, panel.getBackground(), "Armed must NOT darken - that is tier two");
        assertEquals("Drop a CSV file, or Browse", pathLabel.getText(),
            "Armed leaves the text alone; only the hovered picker claims the file");
    }

    @Test
    @DisplayName("HOVERED adds the darker fill and the drop hint on top of the dashed frame")
    public void testDragState_hoveredDarkensAndSwapsText() {
        JPanel panel = selector.createPanel();

        Border restingBorder = panel.getBorder();
        JLabel pathLabel = ReflectionUtils.getFieldValue(selector, "filePathLabel");

        selector.setDragState(FileSelector.DragState.HOVERED);

        assertNotEquals(restingBorder, panel.getBorder(), "Hovered keeps the dashed frame");
        assertEquals(DialogConstants.DROP_COLOR_HOVER, panel.getBackground(),
            "Hovered darkens to the recessed panel gray");
        assertEquals("Drop your file here", pathLabel.getText());
    }

    @Test
    @DisplayName("IDLE restores the resting frame and fill exactly")
    public void testDragState_idleRestores() {
        JPanel panel = selector.createPanel();

        Border restingBorder = panel.getBorder();
        Color restingBackground = panel.getBackground();
        JLabel pathLabel = ReflectionUtils.getFieldValue(selector, "filePathLabel");

        selector.setDragState(FileSelector.DragState.HOVERED);
        selector.setDragState(FileSelector.DragState.IDLE);

        assertEquals(restingBorder, panel.getBorder(), "The resting frame is restored exactly");
        assertEquals(restingBackground, panel.getBackground(), "The resting fill is restored exactly");
        assertEquals("Drop a CSV file, or Browse", pathLabel.getText());
    }

    @Test
    @DisplayName("Both drag states keep the resting insets so nothing shifts mid-drag")
    public void testDragState_preservesInsets() {
        JPanel panel = selector.createPanel();

        Insets resting = panel.getBorder().getBorderInsets(panel);

        // A different inset would resize the content area and make the path re-ellipsize or jump
        for (FileSelector.DragState state : FileSelector.DragState.values()) {
            selector.setDragState(state);
            assertEquals(resting, panel.getBorder().getBorderInsets(panel),
                state + " border must match EtchedBorder's insets - see buildDragBorder");
        }
    }

    @Test
    @DisplayName("One arbiter arms every picker but darkens only the hovered one")
    public void testArbiter_armsSiblingsAndHoversOne() throws Exception {
        FileSelector camper = new FileSelector("Camper File", SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);
        FileSelector activity = new FileSelector("Activity File", SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);
        JPanel camperPanel = camper.createPanel();
        JPanel activityPanel = activity.createPanel();

        Color restingBackground = camperPanel.getBackground();

        // Each createPanel builds its own CompoundBorder, and CompoundBorder has no equals(), so
        // these must be captured per panel - comparing one panel against the other's border would
        // compare two distinct objects and pass or fail for the wrong reason
        Border camperResting = camperPanel.getBorder();
        Border activityResting = activityPanel.getBorder();

        FileDropArbiter arbiter = new FileDropArbiter();
        camper.setDropArbiter(arbiter);
        activity.setDropArbiter(arbiter);

        // A drag arriving over the Camper picker: it darkens, its sibling merely arms
        ReflectionUtils.invokeMethod(arbiter, "enter", camper);

        assertEquals(DialogConstants.DROP_COLOR_HOVER, camperPanel.getBackground(),
            "The hovered picker darkens");
        assertEquals(restingBackground, activityPanel.getBackground(),
            "The sibling arms without darkening");
        assertNotEquals(activityResting, activityPanel.getBorder(),
            "...but it does show the dashed frame, so the user can see it accepts files too");

        // Crossing to the sibling moves the fill without disarming either
        ReflectionUtils.invokeMethod(arbiter, "enter", activity);
        assertEquals(restingBackground, camperPanel.getBackground());
        assertEquals(DialogConstants.DROP_COLOR_HOVER, activityPanel.getBackground());
        assertNotEquals(camperResting, camperPanel.getBorder(),
            "The picker just left stays armed rather than dropping to idle");

        arbiter.reset();
        assertEquals(camperResting, camperPanel.getBorder(), "Reset returns both to resting");
        assertEquals(activityResting, activityPanel.getBorder());
    }

    @Test
    @DisplayName("A deferred exit is cancelled by an incoming enter, so crossings do not flicker")
    public void testArbiter_deferredExitIsCancelledByEnter() throws Exception {
        FileSelector camper = new FileSelector("Camper File", SelectionMode.OPEN, EXTENSIONS, EXTENSION_DESCRIPTION);
        JPanel camperPanel = camper.createPanel();
        Border restingBorder = camperPanel.getBorder();

        FileDropArbiter arbiter = new FileDropArbiter();
        camper.setDropArbiter(arbiter);

        // Leaving one child fires exit before the next child's enter. The queued clear must lose.
        ReflectionUtils.invokeMethod(arbiter, "enter", camper);
        ReflectionUtils.invokeMethod(arbiter, "exit");
        ReflectionUtils.invokeMethod(arbiter, "enter", camper);

        flushEventQueue();
        assertNotEquals(restingBorder, camperPanel.getBorder(),
            "An enter after an exit must cancel the queued disarm");

        // An exit with nothing following it does disarm, once the queued task runs
        ReflectionUtils.invokeMethod(arbiter, "exit");
        flushEventQueue();
        assertEquals(restingBorder, camperPanel.getBorder(),
            "A drag that truly leaves clears the feedback");
    }

    /** Runs any tasks the arbiter queued with invokeLater. */
    private void flushEventQueue() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    @DisplayName("Drag-exit restores the selected path, not the empty hint")
    public void testDragState_restoresSelectedPath() throws Exception {
        selector.createPanel();

        File chosen = new File(tempDir, "campers.csv");
        assertTrue(chosen.createNewFile());
        selector.setValue(chosen);

        String selectedText = ((JLabel) ReflectionUtils.getFieldValue(selector, "filePathLabel")).getText();
        assertTrue(selectedText.contains("campers.csv"));

        // A drag that passes over and leaves must not wipe an existing selection's label
        selector.setDragState(FileSelector.DragState.HOVERED);
        selector.setDragState(FileSelector.DragState.IDLE);

        JLabel pathLabel = ReflectionUtils.getFieldValue(selector, "filePathLabel");
        assertEquals(selectedText, pathLabel.getText());
        assertEquals(chosen, selector.getValue());
    }

    @Test
    @DisplayName("SAVE mode keeps the original text and never changes its frame")
    public void testSaveMode_isUntouched() throws Exception {
        FileSelector saveSelector = new FileSelector(TITLE, SelectionMode.SAVE, EXTENSIONS, EXTENSION_DESCRIPTION);
        JPanel panel = saveSelector.createPanel();

        // Dropping onto a "save as" target is meaningless, so Export keeps today's look
        JLabel pathLabel = ReflectionUtils.getFieldValue(saveSelector, "filePathLabel");
        assertEquals("No file selected", pathLabel.getText(),
            "SAVE mode keeps the original empty text, not the drop hint");

        // No drop target is installed, but prove the frame would survive a stray call anyway
        Border restingBorder = panel.getBorder();
        Color restingBackground = panel.getBackground();

        saveSelector.setDragState(FileSelector.DragState.HOVERED);

        assertEquals(restingBorder, panel.getBorder(), "SAVE mode's frame must never change");
        assertEquals(restingBackground, panel.getBackground(), "SAVE mode must never darken");
        assertEquals("No file selected", pathLabel.getText());
    }

    /**
     * Helper method to find the title label in a panel.
     */
    private JLabel findTitleLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText().contains(TITLE)) {
                    return label;
                }
            } else if (component instanceof Container) {
                JLabel label = findTitleLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the browse button in a panel.
     */
    private JButton findBrowseButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                return (JButton) component;
            } else if (component instanceof Container) {
                JButton button = findBrowseButton((Container) component);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the file path label in a panel.
     */
    private JLabel findFilePathLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && !(component.equals(findTitleLabel(container)))) {
                return (JLabel) component;
            } else if (component instanceof Container) {
                JLabel label = findFilePathLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the error label in a panel.
     */
    private JLabel findErrorLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getForeground().equals(java.awt.Color.RED)) {
                    return label;
                }
            } else if (component instanceof Container) {
                JLabel label = findErrorLabel((Container) component);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }
}
