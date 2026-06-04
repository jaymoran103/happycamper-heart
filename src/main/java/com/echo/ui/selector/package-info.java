/**
 * Selector package containing dialog windows for user interaction.
 * 
 * This package provides selectors that allow users to provide input for the program, typically as components within a modular InputsDialog.
 * Selectors exist independently of the UI, creating a JPanel that can be added to a dialog and will update the Selector's value.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.ui.selector.InputSelector} - Base class for all selectors</li>
 *   <li>{@link com.echo.ui.selector.BooleanSelector} - Selector for boolean values</li>
 *   <li>{@link com.echo.ui.selector.CheckBoxSelector} - Selector for multiple boolean options</li>
 *   <li>{@link com.echo.ui.selector.NumberInputSelector} - Selector for numeric values</li>
 *   <li>{@link com.echo.ui.selector.RadioButtonSelector} - Selector for enum values</li>
 *   <li>{@link com.echo.ui.selector.FileSelector} - Selector for file paths</li>
 *   <li>{@link com.echo.ui.selector.ActionButtonSelector} - Selector provides a panel of action buttons, enabling dynamic dialog behavior</li>
 * </ul>
 * 
 * The Selector system is designed to be extensible and consistent, with common behaviors implemented in the abstract base class.
 * Each child provides specific functionality for different types of input, and customizes a JComponent to be displayed in the dialog.
 */
package com.echo.ui.selector;
