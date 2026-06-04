package com.echo.service;

import java.io.File;
import java.util.Map;

import com.echo.domain.ActivityRoster;
import com.echo.domain.Camper;
import com.echo.domain.CamperRoster;
import com.echo.logging.RosterException;
import com.echo.validation.ImportFileValidator;

/**
 * Service for importing data from CSV files into roster objects.
 *
 * The ImportService provides methods for reading, validating, and converting CSV files
 * into various roster types. It handles file validation, header extraction, and data
 * conversion while providing appropriate error handling through RosterExceptions.
 *
 * This service is used by the RosterService and UI components to load data from files
 * selected by the user.
 * 
 * FUTURE - user a helper method to reduce redundant code in roster import methods. will need a more common key generation method
 */
public class ImportService {

    /**
     * Imports data from a CSV file into a list of maps, wrapped in a ParsedCSV object.
     * Each map represents a row of data with column headers as keys.
     *
     * @param file The CSV file to import
     * @return ParsedCSV object, representing each row of data as a map of header-value pairs
     * @throws RosterException if the file is invalid or an error occurs during import
     */
    public ParsedCSV importCSV(File file) throws RosterException {

        // Basic file validation
        ImportFileValidator.validateBasicFile(file);

        // Parse the file
        ParsedCSV parsedCSV = ImportUtils.parseFile(file);

        // Validate the parsed content
        ImportFileValidator.validateCSVFile(file, parsedCSV, null);

        return parsedCSV;
    }

    /**
     * Imports data from a CSV file into a camper roster.
     * This method reads the file, extracts headers and data, and creates a CamperRoster
     * with Camper objects for each row. Each camper is assigned a unique ID based on their
     * name and grade information.
     *
     * @param file The CSV file to import
     * @return A new CamperRoster containing the imported data
     * @throws RosterException if the file is invalid or an error occurs during import
     */
    public CamperRoster importCamperRoster(File file) throws RosterException {

        // Basic file validation
        ImportFileValidator.validateBasicFile(file);

        // Parse the file
        ParsedCSV parsedCSV = ImportUtils.parseFile(file);

        // Validate the parsed content
        ImportFileValidator.validateCSVFile(file, parsedCSV, CamperRoster.getRequiredHeaders());

        //Create roster instance to represent data as its proper type
        CamperRoster roster = new CamperRoster();

        // Add headers
        for (String header : parsedCSV.getHeaderNames()) {
            roster.addHeader(header);
        }

        // Add campers
        for (Map<String, String> row : parsedCSV.getRows()) {
            String camperId = CamperRoster.generateCamperId(row);
            Camper camper = new Camper(camperId, row);
            roster.addCamper(camper);
        }

        return roster;
    
    }

    /**
     * Imports data from a CSV file into an activity roster.
     * This method reads the file, extracts headers and data, and creates an ActivityRoster
     * with activity entries for each row. Each activity is assigned a unique ID based on
     * the camper and activity information.
     *
     * @param file The CSV file to import
     * @return A new ActivityRoster containing the imported data
     * @throws RosterException if the file is invalid or an error occurs during import
     */
    public ActivityRoster importActivityRoster(File file) throws RosterException {

        // Basic file validation
        ImportFileValidator.validateBasicFile(file);

        // Parse the file
        ParsedCSV parsedCSV = ImportUtils.parseFile(file);

        // Validate the parsed content
        ImportFileValidator.validateCSVFile(file, parsedCSV, ActivityRoster.getRequiredHeaders());

        //Create roster instance to represent data as its proper type
        ActivityRoster roster = new ActivityRoster();

        // Add headers
        for (String header : parsedCSV.getHeaderNames()) {
            roster.addHeader(header);
        }

        // Add activities
        for (Map<String, String> row : parsedCSV.getRows()) {
            String activityId = ActivityRoster.generateCamperIdFromActivity(row);
            Camper activity = new Camper(activityId, row);
            roster.addCamper(activity);
        }

        return roster;
        
    }

    /**
     * Checks if a file exists and is readable.
     * This is a utility method used for basic file validation before attempting to read.
     *
     * @param file The file to check
     * @return true if the file exists, is a regular file, and is readable; false otherwise
     */

    public boolean isFileValid(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }

    // /**
    //  * Generalized import method for any roster type.
    //  * Not yet ready to implement - needs a commonly named key generation method
    //  */
    // public <T extends Roster> T importRoster(File file, Class<T> rosterClass) throws RosterException {
    //     // Basic file validation
    //     ImportFileValidator.validateBasicFile(file);
    //     // Parse the file
    //     ParsedCSV parsedCSV = ImportUtils.parseFile(file);
    //     // Validate the parsed content
    //     ImportFileValidator.validateCSVFile(file, parsedCSV, ActivityRoster.getRequiredHeaders());
    //     //Create roster instance to represent data as its proper type
    //     try {
    //         T roster = rosterClass.getDeclaredConstructor().newInstance();
    //         // Add headers
    //     for (String header : parsedCSV.getHeaderNames()) {
    //         roster.addHeader(header);
    //     }
    //     // Add activities
    //     for (Map<String, String> row : parsedCSV.getRows()) {
    //         String activityId = T.generateCamperId(row);
    //         Camper activity = new Camper(activityId, row);
    //         roster.addCamper(activity);
    //     }
    //     return roster;
    //     } catch (Exception e) {
    //         throw RosterException.create_normalWrapper("Failed to create roster instance: " + e.getMessage(), e);
    //     }
    // }

}
