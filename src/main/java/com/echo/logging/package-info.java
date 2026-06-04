/**
 * Logging package containing error and warning handling components.
 * 
 * This package provides components for handling errors and warnings throughout the
 * application. It includes exception classes, warning classes, and managers for
 * collecting and organizing issues during roster processing.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.logging.RosterException} - Base exception class for roster-related errors</li>
 *   <li>{@link com.echo.logging.DetailedRosterException} - Exception with detailed context information</li>
 *   <li>{@link com.echo.logging.RosterWarning} - Class for non-fatal issues during processing</li>
 *   <li>{@link com.echo.logging.WarningManager} - Central manager for collecting warnings and errors</li>
 * </ul>
 * 
 * The logging system is designed to provide meaningful feedback to users about issues
 * that occur during roster processing. RosterExceptions represent critical errors that prevent
 * successful completion, while RosterWarnings represent non-fatal issues that users should be aware of,
 * but don't prevent the process from continuing.
 * 
 * The WarningManager acts as a central collection point for all issues, allowing them
 * to be displayed to the user in a consistent way through the UI.
 */
package com.echo.logging;
