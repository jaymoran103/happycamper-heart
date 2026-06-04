package com.echo.automation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple utility class to find test files by their names.
 * This class uses recursion to find old test files without complex path resolution.
 */
public class TestFileFinder {
    
    // Cache of file locations to avoid repeated file system lookups
    private static final Map<String, File> fileCache = new HashMap<>();
    
    // Base directory for test resources
    private static final String TEST_RESOURCES_DIR = "redo/src/test/resources/testRosters";
    
    /**
     * Find a file by its name in the test resources directory.
     * 
     * @param fileName The name of the file to find
     * @return The File object if found, or null if not found
     */
    public static File findFile(String fileName) {
        // Check cache first
        if (fileCache.containsKey(fileName)) {
            return fileCache.get(fileName);
        }
        
        // Search in the test resources directory
        File file = findFileInDirectory(new File(TEST_RESOURCES_DIR), fileName);
        
        if (file != null) {
            // Cache the result
            fileCache.put(fileName, file);
            return file;
        }
        
        // If not found, try without the "redo/" prefix
        file = findFileInDirectory(new File("src/test/resources/testRosters"), fileName);
        
        if (file != null) {
            // Cache the result
            fileCache.put(fileName, file);
            return file;
        }
        
        return null;
    }
    
    /**
     * Recursively search for a file in a directory.
     * 
     * @param directory The directory to search in
     * @param fileName The name of the file to find
     * @return The File object if found, or null if not found
     */
    private static File findFileInDirectory(File directory, String fileName) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileInDirectory(file, fileName);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().equals(fileName)) {
                return file;
            }
        }
        
        return null;
    }
    
    /**
     * Clear the file cache.
     */
    public static void clearCache() {
        fileCache.clear();
    }
}
