/**
 * Dialog package containing dialog windows for user interaction.
 * 
 * This package provides dialog windows that allow users to interact with the application.
 * These dialogs are often used to specify settings before activating a service, or updating the table display.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.ui.dialog.DialogBase} - Abstract base class for all dialogs</li>
 *   <li>{@link com.echo.ui.dialog.CardDialog} - Abstract dialog with multiple cards/panels</li>
 *   <li>{@link com.echo.ui.dialog.InputsDialog} - Abstract dialog for collecting user input</li>
 *   <li>{@link com.echo.ui.dialog.ImportDialog} - Dialog for importing roster data</li>
 *   <li>{@link com.echo.ui.dialog.ExportDialog} - Dialog for exporting roster data</li>
 *   <li>{@link com.echo.ui.dialog.WarningDialog} - Dialog for displaying warnings</li>
 *   <li>{@link com.echo.ui.dialog.ErrorDialog} - Dialog for displaying errors</li>
 *   <li>{@link com.echo.ui.dialog.ViewSettingsDialog} - Dialog for configuring view settings</li>
 * </ul>
 * 
 * The dialog system is designed to be extensible and consistent, with common behaviors
 * implemented in base classes and specialized functionality in derived classes. 
 */
package com.echo.ui.dialog;
