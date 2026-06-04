package com.echo.ui.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.echo.ui.dialog.DialogConstants;
import com.echo.ui.dialog.DialogUtils;
import com.echo.ui.elements.HelpButton;
import com.echo.ui.help.PageContentBuilder.HelpPage;

/**
 * Abstract base class for input selectors used in dialogs.
 * Provides common functionality for creating panels with consistent look and feel.
 */
public abstract class InputSelector<T> {

    // Component dimensions from DialogConstants
    public static final int STANDARD_COMPONENT_WIDTH = DialogConstants.COMPONENT_WIDTH_STANDARD;
    public static final int STANDARD_COMPONENT_HEIGHT = DialogConstants.COMPONENT_HEIGHT_STANDARD;
    private static final int COMPONENT_BORDER_DIM = DialogConstants.COMPONENT_PADDING;

    // Color constants from DialogConstants
    private static final Color ENABLED_TEXT_COLOR = DialogConstants.TEXT_COLOR_NORMAL;
    private static final Color DISABLED_TEXT_COLOR = DialogConstants.TEXT_COLOR_DISABLED;
    protected static final Color ERROR_TEXT_COLOR = DialogConstants.TEXT_COLOR_ERROR;

    protected final String title;
    protected Runnable updateCallback;

    // Panel created by createPanel() - cached for later access
    protected JPanel cachedPanel;
    protected JPanel contentPanel;
    protected JLabel titleLabel;

    protected int componentHeight;

    // Linked help page
    private HelpPage helpPage;



    /**
     * Creates a new InputSelector with the given title.
     *
     * @param title The title to display at the top of the selector
     */
    public InputSelector(String title) {
        this.title = title;
        componentHeight = STANDARD_COMPONENT_HEIGHT;
    }

    /**
     * Sets the component height.
     * This can be used to override the default height calculation.
     *
     * @param height The new height in pixels
     */
    public void setComponentHeight(int height) {
        this.componentHeight = height;
    }

    /**
     * Gets the component height.
     *
     * @return The current height in pixels
     */
    public int getComponentHeight() {
        return componentHeight;
    }

    /**
     * Creates a panel containing the selector components.
     * This method creates a standard panel with the title and then calls
     * buildSelectorPanel to add the specific selector components.
     *
     * @return A panel with the selector components
     */
    public final JPanel createPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            new EmptyBorder(COMPONENT_BORDER_DIM, COMPONENT_BORDER_DIM, COMPONENT_BORDER_DIM, COMPONENT_BORDER_DIM)
        ));

        // Create a content panel for better alignment
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.setBorder(null);


        //Create title label based on title field
        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent titleObject = helpPage!=null 
                               ? DialogUtils.combineWithHelpButton(titleLabel, new HelpButton(helpPage)) 
                               : titleLabel;

        titleObject.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleObject);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        

        // Let subclasses add their specific components to the content panel
        buildSelectorPanel(contentPanel);

        // Add content panel to main panel
        panel.add(contentPanel);
        cachedPanel = panel; //Cache panel for features like enable/disable that affect the whole component

        // Fix size after content panel is fully built, in case subclasses modified component height
        DialogUtils.fixSize(panel, new Dimension(STANDARD_COMPONENT_WIDTH, componentHeight));

        return panel;
    }

    /**
     * Sets a callback to be called when the selection changes.
     *
     * @param callback The callback to run when selection changes
     */
    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;

        // Initialize the selector if needed
        initializeSelector();
    }

    /**
     * Gets the current selected value.
     *
     * @return The current value
     */
    public abstract T getValue();

    /**
     * Sets the current value.
     *
     * @param value The new value to set
     */
    public abstract void setValue(T value);

    /**
     * Checks if the selector has a valid selection.
     *
     * @return true if the selector has a valid selection, false otherwise
     */
    public abstract boolean hasSelection();

    /**
     * Builds the selector-specific part of the panel.
     * Subclasses should override this method to add their specific components.
     *
     * @param panel The panel to add components to
     */
    protected abstract void buildSelectorPanel(JPanel panel);

    /**
     * Initializes the selector.
     * Called when the update callback is set.
     * Subclasses can override this method to perform initialization.
     */
    protected void initializeSelector() {
        // Default implementation does nothing
    }

    /**
     * Notifies the update callback if one is set.
     */
    protected void notifyUpdateCallback() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    /**
     * Sets the enabled state of this selector.
     * When disabled, the entire panel including title and all components are grayed out.
     *
     * @param enabled Whether this selector should be enabled
     */
    public void setEnabled(boolean enabled) {
        if (cachedPanel == null) {
            return;
        }

        // Update the appearance of all components in the panel
        DialogUtils.updateComponentsEnabledState(cachedPanel, enabled);

        // Update the title label if it exists
        if (titleLabel != null) {
            titleLabel.setForeground(enabled ? ENABLED_TEXT_COLOR : DISABLED_TEXT_COLOR);
        }
    }

    /**
     * Links a help page to this selector
     * During panel creation, a help button will be created and added alongside the title
     *
     * @param helpPage The help page to link to
     */
    public void linkHelpPage(HelpPage helpPage) {
        this.helpPage = helpPage;
    }
}
