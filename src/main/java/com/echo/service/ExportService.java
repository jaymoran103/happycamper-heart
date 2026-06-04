package com.echo.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.filter.FilterManager;
import com.echo.logging.RosterException;

/**
 * Service for exporting roster data to files in various formats.
 *
 * The ExportService helps write roster data to CSV files with
 * options for filtering and selecting which headers to include.
 *
 * This service works with the FilterManager to allow exporting only campers
 * that match specific criteria, and supports exporting either all headers
 * or only those marked as visible in the roster.
 */
public class ExportService {

    /**
     * Exports a roster to a CSV file using the specified export settings.
     * This is the main export method that handles all export options.
     *
     * @param roster The roster containing the data to export
     * @param filterManager The filter manager to apply (can be null for no filtering)
     * @param settings The export settings to use
     * @throws RosterException if an error occurs during the export process
     */
    public void exportRosterToCSV(EnhancedRoster roster, FilterManager filterManager, ExportSettings settings) throws RosterException {
        if (settings == null || settings.getDestinationFile() == null) {
            throw new IllegalArgumentException("Export settings and destination file must not be null");
        }

        List<String> headers;
        FilterManager effectiveFilterManager = filterManager;

        
        headers = settings.getShowAllColumns() ? roster.getAllHeaders() : roster.getVisibleHeaders();
        effectiveFilterManager = settings.getShowAllRows() ? null : filterManager;

        // Export with the determined settings
        exportToCSV(roster, effectiveFilterManager, headers, settings.getDestinationFile(),settings.getUseEmptyPlaceholder());
    }

    /**
     * Exports a roster to a CSV file with customizable filtering and headers.
     * This is the main export method that other export methods delegate to.
     * It writes the roster data to a CSV file, applying any filters and including
     * only the specified headers.
     *
     * @param roster The roster containing the data to export
     * @param filterManager The filter manager to apply (can be null for no filtering)
     * @param visibleHeaders The list of headers to include in the export
     * @param file The destination file to write the CSV data to
     * @param useEmptyPlaceholder Whether to use a placeholder for empty values
     * @throws RosterException if an error occurs during the export process
     */
    private void exportToCSV(EnhancedRoster roster, FilterManager filterManager, List<String> visibleHeaders, File file, boolean useEmptyPlaceholder) throws RosterException {
        try (FileWriter writer = new FileWriter(file);
             // Create a CSV printer with the specified headers
             CSVPrinter printer = new CSVPrinter(writer,
                 CSVFormat.DEFAULT.builder()
                     .setHeader(visibleHeaders.toArray(new String[0]))
                     .setQuoteMode(QuoteMode.ALL)  // Force quotes around all values
                     .build())) {

            // Process each camper, applying filters if a filter manager is provided
            for (Camper camper : roster.getCampers()) {
                // Only include campers that pass all filters (or all campers if no filter manager)
                if (filterManager == null || filterManager.applyFilters(camper)) {
                    // Build a row with values for each requested header
                    List<String> rowData = new ArrayList<>();
                    for (String header : visibleHeaders) {
                        String value = camper.getValue(header);

                        // Handle empty values based on settings
                        if (value == null || value.trim().isEmpty()) {
                            value = useEmptyPlaceholder ? DataConstants.DISPLAY_NO_DATA : "";
                        }
                        rowData.add(value);
                    }
                    // Write the row to the CSV file
                    printer.printRecord(rowData);
                }
            }
        } catch (IOException e) {
            throw RosterException.create_normalWrapper("Error exporting to CSV with file "+file.getName(), e);
        }
    }

}
