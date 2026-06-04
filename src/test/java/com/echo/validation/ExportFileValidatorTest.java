package com.echo.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the ExportFileValidator class.
 * These tests verify that export file validation works correctly for various scenarios.
 */
class ExportFileValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("validateExportFile should return valid result for valid file")
    void testValidateExportFile_ValidFile() throws IOException {
        // Create a valid file
        File validFile = tempDir.resolve("valid.csv").toFile();
        Files.createFile(tempDir.resolve("valid.csv"));

        ValidationResult<File> result = ExportFileValidator.validateExportFile(validFile, "csv");

        assertTrue(result.isValid(), "Result should be valid for a valid file");
        assertEquals(validFile, result.getValue(), "Result value should be the input file");
    }

    @Test
    @DisplayName("validateExportFile should return invalid result for null file")
    void testValidateExportFile_NullFile() {
        ValidationResult<File> result = ExportFileValidator.validateExportFile(null, "csv");

        assertFalse(result.isValid(), "Result should be invalid for a null file");
        assertEquals("No file selected", result.getErrorSummary(), "Error summary should indicate no file selected");
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "csv", "xlsx"})
    @DisplayName("validateExportFile should return valid result for file with allowed extension")
    void testValidateExportFile_AllowedExtension(String extension) throws IOException {
        // Create a file with the given extension
        File file = tempDir.resolve("test." + extension).toFile();
        Files.createFile(tempDir.resolve("test." + extension));

        ValidationResult<File> result = ExportFileValidator.validateExportFile(file, "txt", "csv", "xlsx");

        assertTrue(result.isValid(), "Result should be valid for a file with allowed extension: " + extension);
    }

    @Test
    @DisplayName("validateExportFile should return invalid result for file with disallowed extension")
    void testValidateExportFile_DisallowedExtension() throws IOException {
        // Create a file with a disallowed extension
        File file = tempDir.resolve("test.pdf").toFile();
        Files.createFile(tempDir.resolve("test.pdf"));

        ValidationResult<File> result = ExportFileValidator.validateExportFile(file, "txt", "csv");

        assertFalse(result.isValid(), "Result should be invalid for a file with disallowed extension");
        assertEquals("Invalid file extension", result.getErrorSummary(), "Error summary should indicate invalid extension");
    }

    @Test
    @DisplayName("validateExportFile should create parent directory if it doesn't exist")
    void testValidateExportFile_CreateDirectory() {
        // Create a file in a non-existent directory
        File file = new File(tempDir.toFile(), "nonexistent/test.csv");

        ValidationResult<File> result = ExportFileValidator.validateExportFile(file, "csv");

        assertTrue(result.isValid(), "Result should be valid after creating the directory");
        assertTrue(file.getParentFile().exists(), "Parent directory should be created");
    }

    @Test
    @DisplayName("validateExportFile should return invalid result for file with invalid characters in name")
    void testValidateExportFile_InvalidFileName() throws IOException {
        // Create a file with invalid characters in the name
        File file = tempDir.resolve("test?.csv").toFile();

        ValidationResult<File> result = ExportFileValidator.validateExportFile(file, "csv");

        assertFalse(result.isValid(), "Result should be invalid for a file with invalid characters in name");
        assertEquals("Invalid file name", result.getErrorSummary(), "Error summary should indicate invalid file name");
    }

    @Test
    @DisplayName("validateCSVExportFile should return valid result for valid CSV file")
    void testValidateCSVExportFile_ValidFile() throws IOException {
        // Create a valid CSV file
        File validFile = tempDir.resolve("valid.csv").toFile();
        Files.createFile(tempDir.resolve("valid.csv"));

        ValidationResult<File> result = ExportFileValidator.validateCSVExportFile(validFile);

        assertTrue(result.isValid(), "Result should be valid for a valid CSV file");
    }

    @Test
    @DisplayName("validateCSVExportFile should return invalid result for non-CSV file")
    void testValidateCSVExportFile_NonCSVFile() throws IOException {
        // Create a non-CSV file
        File nonCSVFile = tempDir.resolve("test.txt").toFile();
        Files.createFile(tempDir.resolve("test.txt"));

        ValidationResult<File> result = ExportFileValidator.validateCSVExportFile(nonCSVFile);

        assertFalse(result.isValid(), "Result should be invalid for a non-CSV file");
        assertEquals("Invalid file extension", result.getErrorSummary(), "Error summary should indicate invalid extension");
    }

    @Test
    @DisplayName("validateTextExportFile should return valid result for valid text file (CSV)")
    void testValidateTextExportFile_ValidCSVFile() throws IOException {
        // Create a valid CSV file
        File validFile = tempDir.resolve("valid.csv").toFile();
        Files.createFile(tempDir.resolve("valid.csv"));

        ValidationResult<File> result = ExportFileValidator.validateTextExportFile(validFile);

        assertTrue(result.isValid(), "Result should be valid for a valid CSV file");
    }

    @Test
    @DisplayName("validateTextExportFile should return valid result for valid text file (TXT)")
    void testValidateTextExportFile_ValidTXTFile() throws IOException {
        // Create a valid TXT file
        File validFile = tempDir.resolve("valid.txt").toFile();
        Files.createFile(tempDir.resolve("valid.txt"));

        ValidationResult<File> result = ExportFileValidator.validateTextExportFile(validFile);

        assertTrue(result.isValid(), "Result should be valid for a valid TXT file");
    }

    @Test
    @DisplayName("validateTextExportFile should return invalid result for non-text file")
    void testValidateTextExportFile_NonTextFile() throws IOException {
        // Create a non-text file
        File nonTextFile = tempDir.resolve("test.xlsx").toFile();
        Files.createFile(tempDir.resolve("test.xlsx"));

        ValidationResult<File> result = ExportFileValidator.validateTextExportFile(nonTextFile);

        assertFalse(result.isValid(), "Result should be invalid for a non-text file");
        assertEquals("Invalid file extension", result.getErrorSummary(), "Error summary should indicate invalid extension");
    }

    @Test
    @DisplayName("ensureExtension should return file with added extension if missing")
    void testEnsureExtension_AddExtension() {
        // Create a file without extension
        File file = new File(tempDir.toFile(), "test");

        File result = ExportFileValidator.ensureExtension(file, "csv");

        assertEquals("test.csv", result.getName(), "File should have the extension added");
    }

    @Test
    @DisplayName("ensureExtension should return original file if it already has a valid extension")
    void testEnsureExtension_KeepValidExtension() {
        // Create a file with a valid extension
        File file = new File(tempDir.toFile(), "test.csv");

        File result = ExportFileValidator.ensureExtension(file, "csv", "txt");

        assertEquals(file, result, "Original file should be returned if it already has a valid extension");
    }

    @Test
    @DisplayName("ensureExtension should add first extension if file has invalid extension")
    void testEnsureExtension_ReplaceInvalidExtension() {
        // Create a file with an invalid extension
        File file = new File(tempDir.toFile(), "test.pdf");

        File result = ExportFileValidator.ensureExtension(file, "csv", "txt");

        assertEquals("test.pdf.csv", result.getName(), "First allowed extension should be added");
    }

    @Test
    @DisplayName("ensureExtension should handle null file")
    void testEnsureExtension_NullFile() {
        File result = ExportFileValidator.ensureExtension(null, "csv");

        // The method returns null for null input
        assertEquals(null, result, "Null file should be returned as is");
    }

    @Test
    @DisplayName("ensureExtension should handle empty extensions array")
    void testEnsureExtension_EmptyExtensions() {
        File file = new File(tempDir.toFile(), "test");

        File result = ExportFileValidator.ensureExtension(file);

        assertEquals(file, result, "Original file should be returned if no extensions are provided");
    }

    @Test
    @DisplayName("validateExportFile should handle read-only files")
    void testValidateExportFile_ReadOnlyFile() throws IOException {
        // Skip this test on Windows as file permissions work differently
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            // Create a read-only file
            Path readOnlyPath = tempDir.resolve("readonly.csv");
            Files.createFile(readOnlyPath);

            try {
                // Make the file read-only
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                Files.setPosixFilePermissions(readOnlyPath, perms);

                File readOnlyFile = readOnlyPath.toFile();

                ValidationResult<File> result = ExportFileValidator.validateExportFile(readOnlyFile, "csv");

                assertFalse(result.isValid(), "Result should be invalid for a read-only file");
                assertEquals("Cannot write to file", result.getErrorSummary(), "Error summary should indicate write permission issue");
            } finally {
                // Restore permissions to allow cleanup
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
                Files.setPosixFilePermissions(readOnlyPath, perms);
            }
        }
    }
}
