/**
 * Validation package containing utilities for validating data.
 * 
 * This package provides utilities for validating data throughout the application,
 * including file validation, data format validation, and header consistency
 * The modular method chaining approach is key in providing users specific feedback when something isn't right.
 * 
 * Key components:
 * 
 * <ul>
 *   <li>{@link com.echo.validation.ImportFileValidator} - Validates file properties and content</li>
 *   <li>{@link com.echo.validation.ExportFileValidator} - Validates export file settings</li>
 *   <li>{@link com.echo.validation.RosterRegexBuilder} - Builds regex patterns for data validation</li>
 *   <li>{@link com.echo.validation.ValidationResult} - Result of a validation operation</li>
 * </ul>
 * 
 * The validation system uses a functional approach with method chaining to combine
 * multiple validation steps. Each validation method returns a ValidationResult that
 * can be used to continue the validation chain or extract an exception if validation fails.
 *
 */
package com.echo.validation;
