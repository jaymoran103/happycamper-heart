/**
 * Domain package containing the core data models for the HappyCamper application.
 * 
 * This package defines the fundamental data structures used to manager roster data.
 * The domain layer is independent of UI or external services and represents
 * the core business concepts of the application.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.domain.Roster} - Base class for all roster types with core functionality</li>
 *   <li>{@link com.echo.domain.Camper} - Represents an individual camper with their data</li>
 *   <li>{@link com.echo.domain.CamperRoster} - Roster extension for camper enrollment data - Typically from a Campminder User Report</li>
 *   <li>{@link com.echo.domain.ActivityRoster} - Roster Extension for activity assignment data - Typically from a Campminder Elective Roster</li>
 *   <li>{@link com.echo.domain.EnhancedRoster} - Combined roster linking campers to activity assignments</li>
 *   <li>{@link com.echo.domain.RosterHeader} - Enum standardizing standard header names and properties across roster types</li>
 *   <li>{@link com.echo.domain.DataConstants} - Constants for data handling and representation </li>
 * </ul>
 * 
 * The domain layer implements a clean separation between data structures and behavior,
 * with each roster type handling specific validation and data processing tasks relevant to its purpose.
 */
package com.echo.domain;
