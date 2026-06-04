/**
 * Filter UI package containing UI components for the filter system.
 * 
 * This package provides UI components specific to the filter system, allowing users
 * to interact with filters through the ui sidebar. These components display
 * filter options and handle user input to update filter settings.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.ui.filter.FilterSidebar} - Sidebar containing all filter panels</li>
 *   <li>{@link com.echo.ui.filter.CollapsibleFilterPanel} - Base panel for filter UI</li>
 *   <li>{@link com.echo.ui.filter.FilterPanelFactory} - Factory for creating filter panels</li>
 *   <li>{@link com.echo.ui.filter.ProgramFilterBuilder} - Builder for program filter UI</li>
 *   <li>{@link com.echo.ui.filter.FilterOption} - Interface for filter options</li>
 * </ul>
 * 
 * The filter UI system is designed to be flexible and easilly extensible, with common behaviors
 * implemented in base classes and specialized functionality in derived classes. Filter
 * panels are created dynamically based on the filters available in the current roster,
 * so the user isn't overloaded with options irrelevant to their data.
 */
package com.echo.ui.filter;
