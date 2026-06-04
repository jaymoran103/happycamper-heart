package com.echo.validation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.echo.automation.TestFiles;
import com.echo.logging.RosterException;
import com.echo.service.ImportUtils;
import com.echo.service.ParsedCSV;

/**
 * Tests for the ImportFileValidator class.
 * These tests verify that file validation works correctly for various scenarios.
 */
class ImportValidatorTest {

    @TempDir
    Path tempDir;

    private static final List<String> REQUIRED_HEADERS = Arrays.asList("First Name", "Last Name", "Grade");

    @Test
    @DisplayName("validateBasicFile should not throw exception with valid file")
    void testValidateBasicFile_ValidFile() throws IOException {
        // Create a valid CSV file
        File validFile = tempDir.resolve("valid.csv").toFile();
        try (FileWriter writer = new FileWriter(validFile)) {
            writer.write("\"First Name\",\"Last Name\",\"Grade\",\"Cabin\"\n");
            writer.write("\"John\",\"Doe\",\"5th\",\"Lions Lodge\"\n");
        }
        assertDoesNotThrow(() -> ImportFileValidator.validateBasicFile(validFile));
    }

    @Test
    @DisplayName("validateBasicFile should throw exception with null file")
    void testValidateBasicFile_NullFile() {
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateBasicFile(null));
        assertEquals("Invalid File", exception.getSummary());
    }

    @Test
    @DisplayName("validateBasicFile should throw exception with non-existent file")
    void testValidateBasicFile_NonExistentFile() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.csv");
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateBasicFile(nonExistentFile));
        assertEquals("File Not Found", exception.getSummary());
    }

    @Test
    @DisplayName("validateBasicFile should throw exception with invalid extension")
    void testValidateBasicFile_InvalidExtension() throws IOException {
        File txtFile = tempDir.resolve("test.txt").toFile();
        Files.createFile(tempDir.resolve("test.txt"));

        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateBasicFile(txtFile));
        assertEquals("Invalid File Extension", exception.getSummary());
    }

    @Test
    @DisplayName("validateCSVFile should not throw exception with valid file and headers")
    void testValidateCSVFile_ValidFile() throws IOException, RosterException {
        // Create a valid CSV file
        File validFile = tempDir.resolve("valid.csv").toFile();
        try (FileWriter writer = new FileWriter(validFile)) {
            writer.write("\"First Name\",\"Last Name\",\"Grade\",\"Cabin\"\n");
            writer.write("\"John\",\"Doe\",\"5th\",\"Lions Lodge\"\n");
        }

        ParsedCSV parsedCSV = ImportUtils.parseFile(validFile);
        assertDoesNotThrow(() -> ImportFileValidator.validateCSVFile(validFile, parsedCSV, REQUIRED_HEADERS));
    }

    @Test
    @DisplayName("validateCSVFile should throw exception with missing headers")
    void testValidateCSVFile_MissingHeaders() throws IOException, RosterException {
        // Create a CSV file with missing required headers
        File invalidFile = tempDir.resolve("missing_headers.csv").toFile();
        try (FileWriter writer = new FileWriter(invalidFile)) {
            writer.write("\"First Name\",\"Cabin\"\n"); // Missing Last Name and Grade
            writer.write("\"John\",\"Lions Lodge\"\n");
        }

        ParsedCSV parsedCSV = ImportUtils.parseFile(invalidFile);
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateCSVFile(invalidFile, parsedCSV, REQUIRED_HEADERS));
        assertEquals("File missing_headers.csv lacks required headers", exception.getSummary());
    }

    @Test
    @DisplayName("validateCSVFile should throw exception with empty file (no headers)")
    void testValidateCSVFile_NoHeaders() throws IOException, RosterException {
        // Create an empty CSV file
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        try (FileWriter writer = new FileWriter(emptyFile)) {
            // Empty file
        }

        ParsedCSV parsedCSV = ImportUtils.parseFile(emptyFile);
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateCSVFile(emptyFile, parsedCSV, REQUIRED_HEADERS));
        assertEquals("Missing data detected", exception.getSummary());
    }

    @Test
    @DisplayName("validateCSVFile should throw exception with empty file (headers but no rows)")
    void testValidateCSVFile_NoRows() throws IOException, RosterException {
        // Create a CSV file with headers but no data rows
        File noRowsFile = tempDir.resolve("no_rows.csv").toFile();
        try (FileWriter writer = new FileWriter(noRowsFile)) {
            writer.write("\"First Name\",\"Last Name\",\"Grade\",\"Cabin\"\n");
        }

        ParsedCSV parsedCSV = ImportUtils.parseFile(noRowsFile);
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateCSVFile(noRowsFile, parsedCSV, REQUIRED_HEADERS));
        assertEquals("Missing data detected", exception.getSummary());
    }

    @Test
    @DisplayName("validateCSVFile should throw exception with inconsistent row lengths")
    void testValidateCSVFile_InconsistentRows() throws IOException {
        // This test is tricky because the CSV parser automatically handles missing cells
        // by filling them with empty strings. We need to manually create a situation
        // where the row length is inconsistent.

        // Instead of testing with a real file, we'll mock the ParsedCSV object
        // to simulate inconsistent row lengths
        File testFile = tempDir.resolve("mock_inconsistent.csv").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("\"First Name\",\"Last Name\",\"Grade\",\"Cabin\"\n");
            writer.write("\"John\",\"Doe\",\"5th\",\"Lions Lodge\"\n");
        }

        // Create a mock ParsedCSV with inconsistent row lengths
        List<String> headers = Arrays.asList("First Name", "Last Name", "Grade", "Cabin");
        List<Map<String, String>> rows = new ArrayList<>();

        // First row has all columns
        Map<String, String> row1 = new HashMap<>();
        row1.put("First Name", "John");
        row1.put("Last Name", "Doe");
        row1.put("Grade", "5th");
        row1.put("Cabin", "Lions Lodge");
        rows.add(row1);

        // Second row is missing a column (simulating inconsistent length)
        Map<String, String> row2 = new HashMap<>();
        row2.put("First Name", "Jane");
        row2.put("Last Name", "Smith");
        row2.put("Grade", "6th");
        // Missing "Cabin" column
        rows.add(row2);

        ParsedCSV parsedCSV = new ParsedCSV(rows, headers);

        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateCSVFile(testFile, parsedCSV, REQUIRED_HEADERS));
        assertEquals("Malformed data detected - double check file contents before selecting", exception.getSummary());
    }

    @Test
    @DisplayName("Test with real test file - valid mini campers")
    void testWithRealFile_ValidMiniCampers() {
        File testFile = TestFiles.MINI_CAMPERS.toFile();
        assertDoesNotThrow(() -> {
            ImportFileValidator.validateBasicFile(testFile);
            ParsedCSV parsedCSV = ImportUtils.parseFile(testFile);
            ImportFileValidator.validateCSVFile(testFile, parsedCSV, null);
        });
    }

    @Test
    @DisplayName("Test with file that has a missing cell")
    void testWithFileMissingCell() throws IOException, RosterException {
        // Create a CSV file with a missing cell (empty value)
        File missingCellFile = tempDir.resolve("missing_cell.csv").toFile();
        try (FileWriter writer = new FileWriter(missingCellFile)) {
            writer.write("\"First Name\",\"Last Name\",\"Grade\",\"Cabin\"\n");
            writer.write("\"John\",\"Doe\",\"5th\",\"Lions Lodge\"\n");
            writer.write("\"Jane\",\"Smith\",\"\",\"Friendship Lodge\"\n"); // Empty Grade cell
        }

        // First validate the basic file properties
        ImportFileValidator.validateBasicFile(missingCellFile);

        // Then parse the file and validate the CSV content
        ParsedCSV parsedCSV = ImportUtils.parseFile(missingCellFile);

        // This should not throw an exception because the CSV parser fills in missing cells with empty strings
        // and empty values are allowed in the validation
        assertDoesNotThrow(() -> ImportFileValidator.validateCSVFile(missingCellFile, parsedCSV, null));
    }

    @Test
    @DisplayName("Test with real test file - missing header")
    void testWithRealFile_MissingHeader() throws IOException, RosterException {
        // Create a CSV file with missing required headers
        File testFile = tempDir.resolve("missing_header.csv").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("\"First Name\",\"Last Name\",\"Cabin\"\n"); // Missing Grade header
            writer.write("\"John\",\"Doe\",\"Lions Lodge\"\n");
        }

        // First validate the basic file properties
        ImportFileValidator.validateBasicFile(testFile);

        // Then parse the file and validate the CSV content
        ParsedCSV parsedCSV = ImportUtils.parseFile(testFile);

        // Should throw exception when validating with required headers
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateCSVFile(testFile, parsedCSV, REQUIRED_HEADERS));
        assertEquals("File missing_header.csv lacks required headers", exception.getSummary());
    }

    @Test
    @DisplayName("Test with real test file - text file instead of CSV")
    void testWithRealFile_TextFile() {
        File testFile = TestFiles.MINI_CAMPERS_TEXT_FILE.toFile();
        RosterException exception = assertThrows(RosterException.class,
                () -> ImportFileValidator.validateBasicFile(testFile));
        assertEquals("Invalid File Extension", exception.getSummary());
    }
}
