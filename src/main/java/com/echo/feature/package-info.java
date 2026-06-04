/**
 * Feature package containing pluggable features that enhance roster data.
 * 
 * This package implements the feature system that allows the application to extend
 * roster functionality in a modular way. Each feature can be enabled or disabled
 * independently, and they work together to provide a comprehensive analysis of
 * camp activity data.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.feature.RosterFeature} - Interface defining the contract for all features</li>
 *   <li>{@link com.echo.feature.ActivityFeature} - Core feature for processing activity assignments</li>
 *   <li>{@link com.echo.feature.ProgramFeature} - Feature for extracting and analyzing program information</li>
 *   <li>{@link com.echo.feature.PreferenceFeature} - Feature for analyzing camper activity preferences</li>
 *   <li>{@link com.echo.feature.SwimLevelFeature} - Feature for tracking swim levels</li>
 *   <li>{@link com.echo.feature.MedicalFeature} - Feature for tracking medical needs</li>
 * </ul>
 * 
 * Features follow a consistent lifecycle:
 * <ol>
 *   <li>Pre-validation - Verify prerequisites are met</li>
 *   <li>Application - Process data and enhance the roster</li>
 *   <li>Post-validation - Verify the enhanced data</li>
 * </ol>
 * 
 * Each feature can add new headers, modify existing data, and generate warnings
 * during its application. The feature system is designed to be extensible, allowing
 * new features to be added without modifying existing code.
 * 
 * Note: As the base feature of this version, ActivityFeature is enabled by default. 
 * An extension of this project could disable it and provide features not dependent on activity data,
 * but this isn't relevant to the app's immediate use case.
 */
package com.echo.feature;
