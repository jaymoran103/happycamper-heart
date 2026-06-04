package com.echo.automation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enum for managing test file paths without hardcoding.
 * Provides methods to create File objects for test resources.
 *
 * TODO ensure path related methods work across different environments, jar, dmg
 */
public enum TestFiles {
    // Mini roster files
    MINI_CAMPERS("miniRosters", "miniCampers.csv"),
    MINI_ACTIVITIES("miniRosters", "miniActivities.csv"),
    MINI_CAMPERS_EXTRA_CELL("miniRosters", "miniCampers_extraCell.csv"),
    MINI_CAMPERS_MISSING_CELL("miniRosters", "miniCampers_missingCell.csv"),
    MINI_CAMPERS_MISSING_HEADER("miniRosters", "miniCampers_missingHeader.csv"),
    MINI_CAMPERS_TEXT_FILE("miniRosters", "miniCampers_textFile.txt"),
    MINI_CAMPERS_NO_ROWS("miniRosters", "campers_noRows.csv"),
    MINI_ACTIVITIES_FULL("miniRosters", "miniActivitiesFull.csv"),
    MINI_ACTIVITIES_DUPLICATES("miniRosters","miniActivitiesFull_DuplicateActivities.csv"),
    MINI_EMPTY_FILE("miniRosters", "emptyFile.csv"),

    // Basic roster files
    BASIC_CAMPERS("basicRosters", "singleCabinCampers.csv"),
    BASIC_CAMPERS_PREFERENCES("basicRosters", "singleCabinCampers_preferences.csv"),
    BASIC_CAMPERS_PREFERENCES_MEDICAL("basicRosters", "singleCabinCampers_preferences_medical.csv"),
    BASIC_CAMPERS_SWIMCOLORS("basicRosters", "singleCabinCampers_swimColors.csv"),
    BASIC_CAMPERS_TRIPLE_FEATURE("basicRosters","singleCabinCampers_tripleFeature.csv"),

    BASIC_ACTIVITIES("basicRosters", "singleCabinActivities.csv"),
    BASIC_CAMPERS_BADNAMES("basicRosters", "singleCabinCampers_badNames.csv"),
    BADHEADER_CAMPERS("basicRosters", "buggyheadercharacters_campers.csv"),


    // Demo roster files
    DEMO_S5_CAMPERS("demoRosters", "Campers_S5_demo.csv"),
    DEMO_S5_ACTIVITIES("demoRosters", "Activities_S5_demo.csv"),
    DEMO_S6_CAMPERS("demoRosters", "Campers_S6_demo.csv"),
    DEMO_S6_ACTIVITIES("demoRosters", "Activities_S6_demo.csv"),
    DEMO_S6_CAMPERS_PREFERENCES("demoRosters", "Campers_S6_demo_preferences.csv"),

    //Merged roster files
    MERGED_SINGLE_MAINCOLUMNS("mergedRosters","singleCabinMerged_mainColumns.csv"),
    MERGED_SINGLE_ALLCOLUMNS("mergedRosters","singleCabinMerged_allColumns.csv"),
    MERGED_SINGLE_ALLCOLUMNS_CASCADENULLS("mergedRosters","singleCabinMerged_allColumns_cascadeNulls.csv"),
    MERGED_TWO_PROGRAMS("mergedRosters","doubleProgram_allColumns.csv"),
    GENERIC_DATA(".","genericData.csv");

    private final String directory;
    public final String filename;

    // Base path for test resources
    private static final String TEST_RESOURCES_PATH = "src/test/resources/testRosters/";

    // Cache of file locations to avoid repeated file system lookups
    private static final Map<TestFiles, File> fileCache = new HashMap<>();

    TestFiles(String directory, String filename) {
        this.directory = directory;
        this.filename = filename;
    }

    /**
     * Gets the relative path to this test file
     * @return The relative path as a string
     */
    public String getPath() {
        return directory + "/" + filename;
    }

    /**
     * Gets the full path to this test file
     * @return The full path as a string
     */
    public String getFullPath() {
        return TEST_RESOURCES_PATH + directory + "/" + filename;
    }

    /**
     * Creates a File object for this test file
     * @return A File object representing this test file
     */
    public File toFile() {
        // Check cache first
        if (fileCache.containsKey(this)) {
            return fileCache.get(this);
        }

        // List of possible locations for the file
        List<String> possiblePaths = new ArrayList<>();

        // Standard path in the correct directory
        possiblePaths.add(TEST_RESOURCES_PATH + directory + "/" + filename);

        // Special case for GENERIC_DATA which is in the root testRosters directory
        if (this == GENERIC_DATA) {
            possiblePaths.add(TEST_RESOURCES_PATH + filename);
        }

        // Check if the file is in miniRosters directory
        possiblePaths.add(TEST_RESOURCES_PATH + "miniRosters/" + filename);

        // Try each path
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                // Cache the result
                fileCache.put(this, file);
                return file;
            }
        }

        // If file still not found, throw exception with helpful message
        throw new RuntimeException("Could not find test file: " + getPath() + ". Tried paths: " + String.join(", ", possiblePaths));
    }

    /**
     * Checks if this test file exists
     * @return true if the file exists, false otherwise
     */
    public boolean exists() {
        try {
            toFile();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Checks all test files to ensure they exist and are properly named
     * @return A list of missing files, empty if all files exist
     */
    public static List<TestFiles> checkAllFiles() {
        List<TestFiles> missingFiles = new ArrayList<>();

        for (TestFiles testFile : TestFiles.values()) {
            if (!testFile.exists()) {
                missingFiles.add(testFile);
            }
        }

        return missingFiles;
    }

    /**
     * Utility method to verify all test files exist
     * @return true if all files exist, false otherwise
     */
    public static boolean verifyAllFilesExist() {
        List<TestFiles> missingFiles = checkAllFiles();

        if (!missingFiles.isEmpty()) {
            System.err.println("Missing test files:");
            for (TestFiles missingFile : missingFiles) {
                System.err.println("- " + missingFile.name() + ": " + missingFile.getFullPath());
            }
            return false;
        }

        return true;
    }

    /**
     * Clears the file cache, forcing files to be re-located on next access
     */
    public static void clearCache() {
        fileCache.clear();
    }
}
