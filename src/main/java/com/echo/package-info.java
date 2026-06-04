/**
 * Root package for the HappyCamper application.
 * 
 * HappyCamper is a Java application designed to process and analyze camp activity rosters.
 * It allows users to import camper and activity data, apply various features for analysis,
 * filter the data based on different criteria, and export the results.
 * 
 * The application is primarily intended for use with data exported from Campminder.com, 
 * but is designed to be source-agnostic and easily extensible to other data sources..
 * 
 * <ul>
 *   <li>Domain layer - Core data models and business logic</li>
 *   <li>Feature layer - Independent features that enhance roster data</li>
 *   <li>Filter layer - Filtering mechanisms for data visualization and customized exports</li>
 *   <li>Service layer - Services for file operations and roster management</li>
 *   <li>UI layer - User interface components</li>
 *   <li>Validation layer - Data validation utilities</li>
 *   <li>Logging layer - Error and warning handling</li>
 * </ul>
 * 
 * The main entry point for the application is {@link com.echo.HappyCamper}.
 * 
 * @version 2.2.0
 */
package com.echo;
