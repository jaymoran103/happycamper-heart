/**
 * UI package containing user interface components for the HappyCamper application.
 * 
 * This package implements the graphical user interface of the application, including
 * the main window, dialogs, and custom components. The UI layer is responsible for
 * presenting data to the user and handling user interactions.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.ui.MainWindow} - Main application window with roster display and controls</li>
 *   <li>{@link com.echo.ui.component.RosterTable} - Custom table for displaying roster data</li>
 *   <li>{@link com.echo.ui.dialog.ImportDialog} - Dialog for importing roster data</li>
 *   <li>{@link com.echo.ui.dialog.ExportDialog} - Dialog for exporting roster data</li>
 *   <li>{@link com.echo.ui.dialog.WarningDialog} - Dialog for displaying warnings</li>
 *   <li>{@link com.echo.ui.dialog.ErrorDialog} - Dialog for displaying errors</li>
 *   <li>{@link com.echo.ui.filter.FilterSidebar} - Sidebar for filter controls</li>
 * </ul>
 * 
 * The UI is organized into several sub-packages:
 * <ul>
 *   <li>component - Reusable UI components</li>
 *   <li>dialog - Dialog windows for user interaction</li>
 *   <li>filter - UI components specific to the filter system</li>
 * </ul>
 * 
 * The UI layer is cleanly separated from business logic, interacting with the domain and service layers through interfaces. 
 * UI components are designed to be reusable and consistent throughout the application.
 */
package com.echo.ui;
