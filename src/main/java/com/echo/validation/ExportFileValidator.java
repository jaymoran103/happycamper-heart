package com.echo.validation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validator for export files.
 * Provides methods to validate that a file can be written to and has a valid extension.
 */
public class ExportFileValidator {

    /**
     * Validates that a file can be exported to.
     * Checks that:
     * - The file path is valid
     * - The file has a valid extension
     * - The directory exists or can be created
     * - The file can be written to
     *
     * @param file The file to validate
     * @param allowedExtensions The allowed file extensions (without the dot)
     * @return A validation result
     */
    public static ValidationResult<File> validateExportFile(File file, String... allowedExtensions) {
        return validateNotNull(file)
            .andThen(f -> validateExtension(f, allowedExtensions))
            .andThen(ExportFileValidator::validateDirectory)
            .andThen(ExportFileValidator::validateWritable)
            .andThen(ExportFileValidator::validateFileName);
    }

    private static ValidationResult<File> validateNotNull(File file) {
        if (file == null) {
            return ValidationResult.failure("No file selected", "Please select a file to export to.");
        }
        return ValidationResult.success(file);
    }

    private static ValidationResult<File> validateExtension(File file, String... allowedExtensions) {
        if (allowedExtensions.length == 0) {
            return ValidationResult.success(file);
        }

        String fileName = file.getName().toLowerCase();
        boolean hasValidExtension = false;

        for (String ext : allowedExtensions) {
            if (fileName.endsWith("." + ext.toLowerCase())) {
                hasValidExtension = true;
                break;
            }
        }

        if (!hasValidExtension) {
            return ValidationResult.failure(
                "Invalid file extension",
                "Invalid file extension. Allowed extensions: " + String.join(", ", allowedExtensions)
            );
        }

        return ValidationResult.success(file);
    }

    private static ValidationResult<File> validateDirectory(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            try {
                // Try to create the directory
                Path dirPath = Paths.get(parentDir.getAbsolutePath());
                Files.createDirectories(dirPath);
            } catch (Exception e) {
                return ValidationResult.failure(
                    "Cannot create directory",
                    "Cannot create directory: " + parentDir.getAbsolutePath()
                );
            }
        }
        return ValidationResult.success(file);
    }

    private static ValidationResult<File> validateWritable(File file) {
        if (file.exists() && !file.canWrite()) {
            return ValidationResult.failure(
                "Cannot write to file",
                "Cannot write to file. It may be in use by another application or you don't have permission."
            );
        }
        return ValidationResult.success(file);
    }

    private static ValidationResult<File> validateFileName(File file) {
        String fileName = file.getName();
        if (fileName.matches(".*[<>:\"|?*$].*")) {
            return ValidationResult.failure(
                "Invalid file name",
                "File name contains invalid characters"
            );
        }
        return ValidationResult.success(file);
    }

    /**
     * Validates that a file can be exported to with CSV extension.
     *
     * @param file The file to validate
     * @return A validation result
     */
    public static ValidationResult<File> validateCSVExportFile(File file) {
        return validateExportFile(file, "csv");
    }

    /**
     * Validates that a file can be exported to with TXT or CSV extension.
     *
     * @param file The file to validate
     * @return A validation result
     */
    public static ValidationResult<File> validateTextExportFile(File file) {
        return validateExportFile(file, "txt", "csv");
    }

    /**
     * Ensures a file has the specified extension.
     * If the file doesn't have any of the allowed extensions, the first one is added.
     *
     * @param file The file to check
     * @param allowedExtensions The allowed extensions (without the dot)
     * @return A file with a valid extension
     */
    public static File ensureExtension(File file, String... allowedExtensions) {
        if (file == null || allowedExtensions.length == 0) {
            return file;
        }

        String fileName = file.getName().toLowerCase();
        for (String ext : allowedExtensions) {
            if (fileName.endsWith("." + ext.toLowerCase())) {
                return file; // File already has a valid extension
            }
        }

        // Add the first allowed extension
        return new File(file.getAbsolutePath() + "." + allowedExtensions[0]);
    }
}
