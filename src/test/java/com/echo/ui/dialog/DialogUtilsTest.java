package com.echo.ui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the DialogUtils class.
 */
public class DialogUtilsTest {

    @Test
    @DisplayName("Test fixSize method")
    public void testFixSize() {
        JPanel panel = new JPanel();
        Dimension dimension = new Dimension(200, 100);

        // Apply fixSize
        DialogUtils.fixSize(panel, dimension);

        // Verify all size properties are set
        assertEquals(dimension, panel.getPreferredSize());
        assertEquals(dimension, panel.getMinimumSize());
        assertEquals(dimension, panel.getMaximumSize());
    }

    @Test
    @DisplayName("Test createFormattedLabel method")
    public void testCreateFormattedLabel() {
        String text = "This is a test label with some text that should wrap.";
        int width = 100;

        // Create formatted label
        JLabel label = DialogUtils.createFormattedLabel(text, width);

        // Verify the label was created
        assertNotNull(label);

        // Verify the text contains HTML formatting
        String labelText = label.getText();
        assertTrue(labelText.startsWith("<html>"));
        assertTrue(labelText.endsWith("</html>"));
        assertTrue(labelText.contains("width: " + width + "px"));
        assertTrue(labelText.contains(text));
    }

    @Test
    @DisplayName("Test formatHTMLWithLines method")
    public void testFormatHTMLWithLines() {
        String text = "Line 1\nLine 2\nLine 3";
        int width = 200;

        // Format the text
        String formattedText = DialogUtils.formatHTMLWithLines(text, width);

        // Verify the formatting
        assertTrue(formattedText.startsWith("<html>"));
        assertTrue(formattedText.endsWith("</html>"));
        assertTrue(formattedText.contains("width: " + width + "px"));
        assertTrue(formattedText.contains("Line 1<br>Line 2<br>Line 3"));
    }

    @Test
    @DisplayName("Test formatHTMLWithLines with HTML special characters")
    public void testFormatHTMLWithLinesSpecialChars() {
        String text = "Line with <html> & </html> tags";
        int width = 200;

        // Format the text
        String formattedText = DialogUtils.formatHTMLWithLines(text, width);

        // Verify the formatting and escaping
        assertTrue(formattedText.contains("&lt;html&gt; &amp; &lt;/html&gt;"));
    }

    @Test
    @DisplayName("Test createAlignedPanel method")
    public void testCreateAlignedPanel() {
        // Create panel with FlowLayout
        JPanel panel = DialogUtils.createAlignedPanel(new FlowLayout());

        // Verify the panel was created with the correct layout
        assertNotNull(panel);
        assertTrue(panel.getLayout() instanceof FlowLayout);
        assertEquals(Component.LEFT_ALIGNMENT, panel.getAlignmentX(), 0.001);
    }

    @Test
    @DisplayName("Test createAlignedFlowPanel method")
    public void testCreateAlignedFlowPanel() {
        // Create flow panel
        JPanel panel = DialogUtils.createAlignedFlowPanel();

        // Verify the panel was created with the correct layout
        assertNotNull(panel);
        assertTrue(panel.getLayout() instanceof FlowLayout);
        assertEquals(Component.LEFT_ALIGNMENT, panel.getAlignmentX(), 0.001);

        // Verify the flow layout properties
        FlowLayout layout = (FlowLayout) panel.getLayout();
        assertEquals(FlowLayout.LEFT, layout.getAlignment());
    }

    @Test
    @DisplayName("Test createAlignedBoxPanel method")
    public void testCreateAlignedBoxPanel() {
        // Create box panel
        JPanel panel = DialogUtils.createAlignedBoxPanel();

        // Verify the panel was created with the correct layout
        assertNotNull(panel);
        assertTrue(panel.getLayout() instanceof BoxLayout);
        assertEquals(Component.LEFT_ALIGNMENT, panel.getAlignmentX(), 0.001);

        // Verify the box layout properties
        BoxLayout layout = (BoxLayout) panel.getLayout();
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
    }

    @Test
    @DisplayName("Test addVerticalSpacing method")
    public void testAddVerticalSpacing() {
        JPanel panel = new JPanel();
        int initialComponentCount = panel.getComponentCount();
        int spacingHeight = 10;

        // Add vertical spacing
        DialogUtils.addVerticalSpacing(panel, spacingHeight);

        // Verify a component was added
        assertEquals(initialComponentCount + 1, panel.getComponentCount());

        // Verify the component is a rigid area with the correct height
        Component spacer = panel.getComponent(initialComponentCount);
        assertEquals(new Dimension(0, spacingHeight), spacer.getPreferredSize());
    }

    @Test
    @DisplayName("Test updateComponentsEnabledState method")
    public void testUpdateComponentsEnabledState() {
        // Create a panel with nested components
        JPanel panel = new JPanel();
        JButton button = new JButton("Test");
        JTextField textField = new JTextField("Test");
        JLabel label = new JLabel("Test");

        JPanel nestedPanel = new JPanel();
        JButton nestedButton = new JButton("Nested");
        nestedPanel.add(nestedButton);

        panel.add(button);
        panel.add(textField);
        panel.add(label);
        panel.add(nestedPanel);

        // Initially all components should be enabled
        assertTrue(panel.isEnabled());
        assertTrue(button.isEnabled());
        assertTrue(textField.isEnabled());
        assertTrue(label.isEnabled());
        assertTrue(nestedPanel.isEnabled());
        assertTrue(nestedButton.isEnabled());

        // Disable all components
        DialogUtils.updateComponentsEnabledState(panel, false);

        // Verify all components are disabled
        assertFalse(panel.isEnabled());
        assertFalse(button.isEnabled());
        assertFalse(textField.isEnabled());
        assertFalse(label.isEnabled());
        assertFalse(nestedPanel.isEnabled());
        assertFalse(nestedButton.isEnabled());

        // Enable all components again
        DialogUtils.updateComponentsEnabledState(panel, true);

        // Verify all components are enabled again
        assertTrue(panel.isEnabled());
        assertTrue(button.isEnabled());
        assertTrue(textField.isEnabled());
        assertTrue(label.isEnabled());
        assertTrue(nestedPanel.isEnabled());
        assertTrue(nestedButton.isEnabled());
    }

    @Test
    @DisplayName("Test setDialogLocation method")
    public void testSetDialogLocation() {
        // Create parent and child windows
        JFrame parentFrame = new JFrame();
        JDialog childDialog = new JDialog();

        // Set parent frame size and location
        parentFrame.setSize(400, 300);
        parentFrame.setLocation(100, 100);

        // Set child dialog size
        childDialog.setSize(200, 150);

        // Set dialog location
        DialogUtils.setDialogLocation(parentFrame, childDialog);

        // Verify the dialog is centered relative to the parent
        assertEquals(100 + (400 - 200) / 2, childDialog.getX());
        assertEquals(100 + (300 - 150) / 2, childDialog.getY());
    }

    @Test
    @DisplayName("Test formatHTMLWithLines with null input")
    public void testFormatHTMLWithLinesNull() {
        String formattedText = DialogUtils.formatHTMLWithLines(null, 200);
        assertEquals("<html></html>", formattedText);
    }

    @Test
    @DisplayName("Test formatHTMLWithLines with empty input")
    public void testFormatHTMLWithLinesEmpty() {
        String formattedText = DialogUtils.formatHTMLWithLines("", 200);
        assertEquals("<html></html>", formattedText);
    }

    @Test
    @DisplayName("Test createFormattedLabel with complex text")
    public void testCreateFormattedLabelComplex() {
        String text = "Line 1 with <special> characters\nLine 2 with & and \"quotes\"\nLine 3 with multiple   spaces";
        int width = 150;

        JLabel label = DialogUtils.createFormattedLabel(text, width);

        String labelText = label.getText();
        assertTrue(labelText.contains("&lt;special&gt;"));
        assertTrue(labelText.contains("&amp;"));
        assertTrue(labelText.contains("\"quotes\""));
        assertTrue(labelText.contains("<br>"));
    }

    @Test
    @DisplayName("Test panel creation methods with components")
    public void testPanelCreationWithComponents() {
        // Create components to add
        JLabel label = new JLabel("Test Label");
        JButton button = new JButton("Test Button");

        // Test flow panel
        JPanel flowPanel = DialogUtils.createAlignedFlowPanel();
        flowPanel.add(label);
        flowPanel.add(button);

        assertEquals(2, flowPanel.getComponentCount());
        assertEquals(label, flowPanel.getComponent(0));
        assertEquals(button, flowPanel.getComponent(1));
        assertTrue(flowPanel.getLayout() instanceof FlowLayout);

        // Test box panel
        JPanel boxPanel = DialogUtils.createAlignedBoxPanel();
        boxPanel.add(label);
        boxPanel.add(button);

        assertEquals(2, boxPanel.getComponentCount());
        assertEquals(label, boxPanel.getComponent(0));
        assertEquals(button, boxPanel.getComponent(1));
        assertTrue(boxPanel.getLayout() instanceof BoxLayout);
    }

    @Test
    @DisplayName("Test nested panel creation and component layout")
    public void testNestedPanelCreation() {
        // Create a main panel
        JPanel mainPanel = DialogUtils.createAlignedBoxPanel();

        // Create a nested flow panel
        JPanel flowPanel = DialogUtils.createAlignedFlowPanel();
        JLabel label1 = new JLabel("Label 1");
        JLabel label2 = new JLabel("Label 2");
        flowPanel.add(label1);
        flowPanel.add(label2);

        // Add the flow panel to the main panel
        mainPanel.add(flowPanel);

        // Add a vertical spacer
        DialogUtils.addVerticalSpacing(mainPanel, 10);

        // Add another component
        JButton button = new JButton("Button");
        mainPanel.add(button);

        // Verify the structure
        assertEquals(3, mainPanel.getComponentCount());
        assertEquals(flowPanel, mainPanel.getComponent(0));
        assertEquals(button, mainPanel.getComponent(2));

        // Verify the nested panel
        assertEquals(2, flowPanel.getComponentCount());
        assertEquals(label1, flowPanel.getComponent(0));
        assertEquals(label2, flowPanel.getComponent(1));
    }
}
