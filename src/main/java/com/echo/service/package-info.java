/**
 * Service package containing services for file operations and roster management.
 * 
 * This package provides services that handle the application's core functionality,
 * including importing data from files, applying features to rosters, and exporting
 * processed data. These services act as intermediaries between the UI and the domain
 * layer, handling the orchestration of complex operations.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.service.RosterService} - Core service for managing roster data and applying features</li>
 *   <li>{@link com.echo.service.ImportService} - Service for importing data from CSV files</li>
 *   <li>{@link com.echo.service.ExportService} - Service for exporting roster data to files</li>
 *   <li>{@link com.echo.service.ImportSettings} - Settings for the import process</li>
 *   <li>{@link com.echo.service.ExportSettings} - Settings for the export process</li>
 * </ul>
 * 
 * The service layer follows a clean architecture approach, separating business logic, UI concenrs, and data access.
 * 
 * Services are designed to be stateless whenever possible, with settings objects used to pass configuration between components.
 * 
 * The RosterService acts as the central program coordinator, 
 * managing the lifecycle of roster data from import through feature application to export, 
 * while delegating specific tasks to specialized services.
 */
package com.echo.service;
