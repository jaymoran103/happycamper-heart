package com.echo.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.echo.logging.RosterException;
import com.echo.service.ImportUtils;
import com.echo.service.ParsedCSV;

/**
 * Base class for all roster types in the system.
 *
 * Provides core functionality for storing and accessing roster data,
 * including campers, headers, and visibility settings.
 * Extended by:
 * - CamperRoster - represents input data from a campminder User Report, used as the initial source of data for any roster service
 * - ActivityRoster - represents data from a campminder Elective Roster, used as the initial source of all activity assignments
 * - EnhancedRoster - output roster, created based on CamperRoster data and contributed to by some number of RosterFeatures
 *
 * Key responsibilities here:
 * - Managing a collection of Camper objects
 * - Tracking header information and visibility
 * - Providing methods to access and modify roster data
 * - Loading roster data from CSV files
 */
public class Roster {
    private final List<Camper> campers = new ArrayList<>();
    private final Map<String, Integer> headerMap = new LinkedHashMap<>();
    private final Map<String, Boolean> headerVisibility = new HashMap<>();

    /**
     * Creates a new empty Roster.
     */
    public Roster() {
        // Empty constructor
    }

    /**
     * Gets all campers in the roster.
     *
     * @return List of Camper objects
     */
    public List<Camper> getCampers() {
        return Collections.unmodifiableList(campers);
    }

    /**
     * Gets a map of header names to their column indices.
     *
     * @return Map of header names to column indices
     */
    public Map<String, Integer> getHeaderMap() {
        // Note: This returns a direct reference to the headerMap to allow for header reordering,replacing Collections.unmodifiableMap
        return headerMap;
    }

    /**
     * Gets the value for a specific camper and header.
     *
     * @param camperId The unique identifier for the camper
     * @param header The header name
     * @return The value at the specified cell
     */
    public String getValue(String camperId, String header) {
        Camper camper = getCamperById(camperId);
        if (camper == null) {
            return null;
        }
        return camper.getValue(header);
    }

    /**
     * Sets the value for a specific camper and header.
     *
     * @param camperId The unique identifier for the camper
     * @param header The header name
     * @param value The value to set
     */
    public void setValue(String camperId, String header, String value) {
        Camper camper = getCamperById(camperId);
        if (camper == null) {
            return;
        }

        // Add header if it doesn't exist
        if (!headerMap.containsKey(header)) {
            addHeader(header);
        }

        camper.setValue(header, value);
    }

    /**
     * Gets the list of headers that are currently visible.
     *
     * @return List of visible header names
     */
    public List<String> getVisibleHeaders() {
        List<String> visibleHeaders = new ArrayList<>();
        for (String header: headerMap.keySet()){
            boolean isVisible = headerVisibility.getOrDefault(header, true);
            if (isVisible){
                visibleHeaders.add(header);
            }
        }



        return visibleHeaders;
    }

    /**
     * Checks if a header is visible.
     *
     * @param header The header name
     * @return true if the header is visible
     */
    public boolean isHeaderVisible(String header) {
        return headerVisibility.getOrDefault(header, true);
    }

    /**
     * Checks if a header is visible using a RosterHeader enum.
     *
     * @param header The RosterHeader enum
     * @return true if the header is visible
     */
    public boolean isHeaderVisible(RosterHeader header) {
        return isHeaderVisible(header.standardName);
    }

    /**
     * Gets the list of headers that are currently visible, ordered according to RosterHeader enum order.
     *
     * @return List of visible header names in the correct display order
     */
    public List<String> getOrderedVisibleHeaders() {
        List<String> visibleHeaders = getVisibleHeaders();
        List<String> orderedHeaders = new ArrayList<>();

        // First add headers that match RosterHeader enums in their defined order
        for (RosterHeader header : RosterHeader.values()) {
            if (visibleHeaders.contains(header.standardName)) {
                orderedHeaders.add(header.standardName);
            }
        }

        // Then add any remaining headers that weren't matched
        for (String header : visibleHeaders) {
            if (!orderedHeaders.contains(header)) {
                orderedHeaders.add(header);
            }
        }

        return orderedHeaders;
    }

    /**
     * Sets the visibility of a specific header.
     *
     * @param header The header name
     * @param visible Whether the header should be visible
     */
    public void setHeaderVisibility(String header, boolean visible) {
        if (headerMap.containsKey(header)) {
            headerVisibility.put(header, visible);
        }
    }

    /**
     * Sets the visibility of a header using a RosterHeader enum.
     *
     * @param header The RosterHeader enum
     * @param visible Whether the header should be visible
     */
    public void setHeaderVisibility(RosterHeader header, boolean visible) {
        setHeaderVisibility(header.standardName, visible);
    }

    /**
     * Resets all header visibility settings to their default values.
     * Headers that correspond to RosterHeader enums will use the enum's defaultVisibility.
     * Custom headers will be set to visible.
     */
    public void resetHeaderVisibility() {
        for (String header : headerMap.keySet()) {
            RosterHeader rosterHeader = RosterHeader.determineHeaderType(header);
            boolean defaultVisibility = true;

            if (rosterHeader != null) {
                defaultVisibility = rosterHeader.defaultVisibility;
                headerVisibility.put(header, defaultVisibility);
            } else {
                headerVisibility.put(header, true);
            }
        }
    }

    /**
     * Sets all headers to visible or hidden.
     *
     * @param visible Whether all headers should be visible
     */
    public void setAllHeadersVisibility(boolean visible) {
        for (String header : headerMap.keySet()) {
            headerVisibility.put(header, visible);
        }
    }

    /**
     * Gets all headers in the roster.
     *
     * @return List of all header names
     */
    public List<String> getAllHeaders() {
        return new ArrayList<>(headerMap.keySet());
    }

    /**
     * Gets all headers in the roster, ordered according to RosterHeader enum order.
     *
     * @return List of all header names in the correct display order
     */
    public List<String> getOrderedHeaders() {
        List<String> allHeaders = getAllHeaders();
        List<String> orderedHeaders = new ArrayList<>();

        // First add headers that match RosterHeader enums in their defined order
        for (RosterHeader header : RosterHeader.values()) {
            if (allHeaders.contains(header.standardName)) {
                orderedHeaders.add(header.standardName);
            }
        }

        // Then add any remaining headers that weren't matched
        for (String header : allHeaders) {
            if (!orderedHeaders.contains(header)) {
                orderedHeaders.add(header);
            }
        }

        return orderedHeaders;
    }

    /**
     * Sets visibility for a group of headers.
     *
     * @param headers List of headers to set visibility for
     * @param visible Whether the headers should be visible
     */
    public void setHeadersVisibility(List<String> headers, boolean visible) {
        for (String header : headers) {
            if (headerMap.containsKey(header)) {
                headerVisibility.put(header, visible);
            }
        }
    }

    /**
     * Adds a new header to the roster.
     *
     * @param header The header name
     */
    public void addHeader(String header) {
        if (!headerMap.containsKey(header)) {
            headerMap.put(header, headerMap.size());

            // Set visibility based on RosterHeader defaults if this is a known header type
            RosterHeader rosterHeader = RosterHeader.determineHeaderType(header);
            if (rosterHeader != null) {
                headerVisibility.put(header, rosterHeader.defaultVisibility);
            } else {
                headerVisibility.put(header, true);
            }
        }
    }

    /**
     * Adds a new header to the roster using a RosterHeader enum.
     * The header will be added with its default visibility setting.
     *
     * @param header The RosterHeader enum
     */
    public void addHeader(RosterHeader header) {
        if (!headerMap.containsKey(header.standardName)) {
            headerMap.put(header.standardName, headerMap.size());
            headerVisibility.put(header.standardName, header.defaultVisibility);
        }
    }

    /**
     * Adds a new header to the roster with a default value.
     *
     * @param header The header name
     * @param defaultValue The default value for this header
     */
    public void addHeader(String header, String defaultValue) {
        addHeader(header);

        // Add default value to all existing campers
        if (defaultValue != null) {
            for (Camper camper : campers) {
                camper.setValue(header, defaultValue);
            }
        }
    }

    /**
     * Adds a new header to the roster using a RosterHeader enum with a default value.
     * The header will be added with its default visibility setting.
     *
     * @param header The RosterHeader enum
     * @param defaultValue The default value for this header
     */
    public void addHeader(RosterHeader header, String defaultValue) {
        addHeader(header);

        // Add default value to all existing campers
        if (defaultValue != null) {
            for (Camper camper : campers) {
                camper.setValue(header.standardName, defaultValue);
            }
        }
    }

    /**
     * Checks if the roster has a specific header.
     *
     * @param header The header name
     * @return true if the header exists
     */
    public boolean hasHeader(String header) {
        return headerMap.containsKey(header);
    }

    /**
     * Checks if the roster has a specific header using a RosterHeader enum.
     *
     * @param header The RosterHeader enum
     * @return true if the header exists
     */
    public boolean hasHeader(RosterHeader header) {
        return hasHeader(header.standardName);
    }

    /**
     * Adds a camper to the roster.
     *
     * @param camper The camper to add
     */
    public void addCamper(Camper camper) {
        campers.add(camper);
    }

    /**
     * Gets a camper by ID.
     *
     * @param camperId The ID of the camper to get
     * @return The camper, or null if not found
     */
    public Camper getCamperById(String camperId) {
        for (Camper camper : campers) {
            if (camper.getId().equals(camperId)) {
                return camper;
            }
        }
        return null;
    }

    /**
     * Generates a unique identifier for a camper based on their data.
     * This method creates a consistent ID by combining first name, last name, and grade.
     *
     * @param camperData Map of camper data containing at least first name and last name
     * @return A unique identifier string in the format "firstname_lastname_grade"
     */
    public static String generateCamperId(Map<String, String> camperData) {
        // Default implementation uses first name, last name, and grade if available
        String firstName = camperData.getOrDefault(RosterHeader.FIRST_NAME.camperRosterName, "");
        String lastName = camperData.getOrDefault(RosterHeader.LAST_NAME.camperRosterName, "");
        String grade = camperData.getOrDefault(RosterHeader.GRADE.camperRosterName, "");

        return String.format("%s_%s_%s", firstName, lastName, grade)
                .toLowerCase()
                .replace(" ", "_");
    }

    /**
     * Loads roster data from a CSV file.
     * This method reads the CSV file, extracts headers and row data, and creates
     * Camper objects for each row. Empty values are normalized to empty strings.
     *
     * @param file The CSV file to load (must have a header row)
     * @throws RosterException if there are issues with the file format, missing headers, or I/O errors
     */
    public void loadFromCSV(File file) throws RosterException {
        try {
            ParsedCSV parsedCSV = ImportUtils.parseFile(file);
            // Get headers from parser
            List<String> headerList = parsedCSV.getHeaderNames();
            Map<String, Integer> parsedHeaders = new HashMap<>();
            for (int i = 0; i < headerList.size(); i++) {
                parsedHeaders.put(headerList.get(i), i);
            }

            // Check for case where headers are missing
            if (parsedHeaders.isEmpty()) {
                throw RosterException.create_normalWrapper("No headers found in file '"+file.getName()+"'", null);
            }

            // Add headers to roster
            for (String header : parsedHeaders.keySet()) {
                addHeader(header);
            }

            // Process rows
            for (Map<String,String> record : parsedCSV.getRows()) {
                Map<String, String> rowData = new HashMap<>();

                // Convert record to map
                for (String header : parsedHeaders.keySet()) {
                    String value = record.get(header);
                    // Normalize empty values
                    if (value == null || value.trim().isEmpty()) {
                        value = "";
                    }
                    rowData.put(header, value);
                }

                // Create and add camper
                Camper camper = new Camper(rowData);
                addCamper(camper);
            }

        } catch (Exception e) {
            if (e instanceof RosterException) {
                throw (RosterException) e;
            }
            throw RosterException.create_normalWrapper("Error loading CSV file: "+file.getName() +" "+ e.getMessage(), e);
        }
    }

    /**
     * Helper method to validate that a row contains all required headers.
     *
     * @param checkedHeaders The set of headers to check
     * @param requiredHeaders The list of required headers
     * @return true if all required headers are present
     */
    public static void validateHeaders(Set<String> checkedHeaders, List<String> requiredHeaders) throws RosterException{

        List<String> missingHeaders = new ArrayList<>();
        for (String header : requiredHeaders) {
            if (!checkedHeaders.contains(header)) {
                missingHeaders.add(header);
            }
        }
        if (!missingHeaders.isEmpty()){
            throw RosterException.missingHeaders_Basic(missingHeaders);
        }
    }
}
