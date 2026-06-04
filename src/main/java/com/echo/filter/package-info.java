/**
 * Filter package containing components for filtering roster data.
 * 
 * This package implements the filtering system that allows users to dynamically
 * show or hide campers based on various criteria. Filters are applied to the roster
 * data in real-time, affecting what's displayed in the UI and what's included in exports.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.filter.RosterFilter} - Interface defining the contract for all filters</li>
 *   <li>{@link com.echo.filter.FilterManager} - Central manager for applying multiple filters</li>
 *   <li>{@link com.echo.filter.AssignmentFilter} - Filter for activity assignment visibility</li>
 *   <li>{@link com.echo.filter.SortedProgramFilter} - Filter for program-specific visibility</li>
 *   <li>{@link com.echo.filter.PreferenceFilter} - Filter for preference-based visibility</li>
 *   <li>{@link com.echo.filter.SwimLevelFilter} - Filter for swim level-based visibility</li>
 *  *   <li>{@link com.echo.filter.MedicalFilter} - Filter for medical needs visibility</li>

 * </ul>
 * 
 * The filter system is designed to allow multiple filters to be applied simultaneously. 
 * Each filter can create its own UI panel for user interaction,
 * and the FilterManager coordinates the application of all active filters.
 * 
 * Filters are dynamically created based on the features enabled in the current roster,
 * ensuring that only relevant filters are available to the user.
 */
package com.echo.filter;
