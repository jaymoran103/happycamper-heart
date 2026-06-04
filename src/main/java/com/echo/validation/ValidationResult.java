package com.echo.validation;

import java.util.function.Function;

import com.echo.logging.RosterException;

/**
 * Generic validation result that can be chained with additional validation steps.
 * @param <T> The type of object being validated
 */
public class ValidationResult<T> {
    private final T value;
    private final String errorSummary;
    private final String errorMessage;
    
    private ValidationResult(T value, String errorSummary, String errorMessage) {
        this.value = value;
        this.errorSummary = errorSummary;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Creates a successful validation result.
     * 
     * @param value The validated value
     * @return A successful validation result
     */
    public static <T> ValidationResult<T> success(T value) {
        return new ValidationResult<>(value, null, null);
    }
    
    /**
     * Creates a failed validation result with an error message.
     * 
     * @param errorSummary The error summary
     * @param errorMessage The detailed error message
     * @return A failed validation result
     */
    public static <T> ValidationResult<T> failure(String errorSummary, String errorMessage) {
        return new ValidationResult<>(null, errorSummary, errorMessage);
    }
    
    /**
     * Applies a validation check to the value, if the previous check passed.
     * 
     * @param nextCheck validation function to apply to the value
     * @return ValidationResult indicating the current validation state
     */
    public <R> ValidationResult<R> andThen(Function<T, ValidationResult<R>> nextCheck) {
        if (!isValid()) {
            return ValidationResult.failure(errorSummary, errorMessage);
        }
        return nextCheck.apply(value);
    }
    
    /**
     * Checks if the validation was successful.
     * 
     * @return true if the validation was successful, false otherwise
     */
    public boolean isValid() {
        return errorSummary == null && errorMessage == null;
    }
    
    /**
     * Gets the validated value if the validation was successful.
     * 
     * @return The validated value, or null if the validation failed
     */
    public T getValue() {
        return value;
    }
    
    /**
     * Gets the error summary if the validation failed.
     * 
     * @return The error summary, or null if the validation was successful
     */
    public String getErrorSummary() {
        return errorSummary;
    }
    
    /**
     * Gets the error message if the validation failed.
     * 
     * @return The error message, or null if the validation was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Converts this validation result to a RosterException.
     * 
     * @return A RosterException if validation failed, null if successful
     */
    public RosterException toException() {
        if (isValid()) {
            return null;
        }
        return RosterException.fileException(errorSummary, errorMessage);
    }
}