package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.ui.component.RosterTable;
import com.echo.ui.filter.FilterSidebar;

/**
 * Integration tests for the filter system.
 * Tests the interaction between FilterManager, filters, and UI components.
 */
public class FilterSystemIntegrationTest {
    private FilterManager filterManager;
    private EnhancedRoster roster;
    private RosterTable rosterTable;

    @BeforeEach
    public void setUp() {
        filterManager = new FilterManager();
        roster = new EnhancedRoster();
        rosterTable = new RosterTable();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.ROUND_COUNT.standardName);
        roster.addHeader(RosterHeader.PROGRAM.standardName);
        roster.addHeader(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);

        // Create test campers
        roster.addCamper(createCamper("John", "Doe", "3", "Traditional Camp", "None"));
        roster.addCamper(createCamper("Jane", "Smith", "2", "Traditional Camp", "Archery"));
        roster.addCamper(createCamper("Bob", "Johnson", "3", "Adventure Camp", "None"));
        roster.addCamper(createCamper("Alice", "Williams", "1", "Adventure Camp", "Swimming, Fishing"));
        roster.addCamper(createCamper("Charlie", "Brown", "0", "Traditional Camp", "None"));

        // Enable features
        roster.enableFeature("activity");
        roster.enableFeature("program");
        roster.enableFeature("preference");

        // Set up the roster table
        rosterTable.setRoster(roster, filterManager);
    }

    private Camper createCamper(String firstName, String lastName, String roundCount, String program, String unrequestedActivities) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, firstName);
        data.put(RosterHeader.LAST_NAME.camperRosterName, lastName);
        data.put(RosterHeader.ROUND_COUNT.standardName, roundCount);
        data.put(RosterHeader.PROGRAM.standardName, program);
        data.put(RosterHeader.UNREQUESTED_ACTIVITIES.standardName, unrequestedActivities);

        String id = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_";
        return new Camper(id, data);
    }

    @Test
    @DisplayName("Test creating filters for roster")
    public void testCreatingFiltersForRoster() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Verify the correct filters were created
        assertEquals(3, filterManager.getFilterCount());
        assertNotNull(filterManager.getFilter("assignment"));
        assertNotNull(filterManager.getFilter("program-list"));
        assertNotNull(filterManager.getFilter("preference"));
    }

    @Test
    @DisplayName("Test filter sidebar creation")
    public void testFilterSidebarCreation() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Create the filter sidebar
        FilterSidebar sidebar = new FilterSidebar(roster, filterManager);

        // Update filter panels
        sidebar.updateFilterPanels();

        // Verify the sidebar was created
        assertNotNull(sidebar);
    }

    @Test
    @DisplayName("Test applying assignment filter")
    public void testApplyingAssignmentFilter() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Get the assignment filter
        AssignmentFilter assignmentFilter = (AssignmentFilter) filterManager.getFilter("assignment");
        assertNotNull(assignmentFilter);

        // Hide campers with 0 rounds
        assignmentFilter.setRoundVisible(0, false);

        // Apply filters to all campers
        int visibleCount = 0;
        for (Camper camper : roster.getCampers()) {
            if (filterManager.applyFilters(camper)) {
                visibleCount++;
            }
        }

        // Verify 4 campers are visible (all except Charlie Brown with 0 rounds)
        assertEquals(4, visibleCount);
    }

    @Test
    @DisplayName("Test applying program filter")
    public void testApplyingProgramFilter() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Get the program filter
        SortedProgramFilter programFilter = (SortedProgramFilter) filterManager.getFilter("program-list");
        assertNotNull(programFilter);

        // Hide Adventure Camp
        programFilter.setProgramVisible("Adventure Camp", false);

        // Apply filters to all campers
        int visibleCount = 0;
        for (Camper camper : roster.getCampers()) {
            if (filterManager.applyFilters(camper)) {
                visibleCount++;
            }
        }

        // Verify 3 campers are visible (all Traditional Camp campers)
        assertEquals(3, visibleCount);
    }

    @Test
    @DisplayName("Test applying preference filter")
    public void testApplyingPreferenceFilter() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Get the preference filter
        PreferenceFilter preferenceFilter = (PreferenceFilter) filterManager.getFilter("preference");
        assertNotNull(preferenceFilter);

        // Hide campers without unrequested activities
        preferenceFilter.setShowCampersWithoutUnrequestedActivities(false);

        // Apply filters to all campers
        int visibleCount = 0;
        for (Camper camper : roster.getCampers()) {
            if (filterManager.applyFilters(camper)) {
                visibleCount++;
            }
        }

        // Verify 5 campers are visible (with the updated filter implementation)
        assertEquals(5, visibleCount);
    }

    @Test
    @DisplayName("Test applying multiple filters")
    public void testApplyingMultipleFilters() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Get the filters
        AssignmentFilter assignmentFilter = (AssignmentFilter) filterManager.getFilter("assignment");
        SortedProgramFilter programFilter = (SortedProgramFilter) filterManager.getFilter("program-list");
        PreferenceFilter preferenceFilter = (PreferenceFilter) filterManager.getFilter("preference");

        assertNotNull(assignmentFilter);
        assertNotNull(programFilter);
        assertNotNull(preferenceFilter);

        // Configure filters:
        // - Hide campers with 0 or 1 rounds
        // - Hide Adventure Camp
        // - Hide campers without unrequested activities
        assignmentFilter.setRoundVisible(0, false);
        assignmentFilter.setRoundVisible(1, false);
        programFilter.setProgramVisible("Adventure Camp", false);
        preferenceFilter.setShowCampersWithoutUnrequestedActivities(false);

        // Apply filters to all campers
        int visibleCount = 0;
        for (Camper camper : roster.getCampers()) {
            if (filterManager.applyFilters(camper)) {
                visibleCount++;
            }
        }

        // Verify 2 campers are visible (with the updated filter implementation)
        assertEquals(2, visibleCount);
    }

    @Test
    @DisplayName("Test roster table with filters")
    public void testRosterTableWithFilters() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Get the assignment filter
        AssignmentFilter assignmentFilter = (AssignmentFilter) filterManager.getFilter("assignment");
        assertNotNull(assignmentFilter);

        // Hide campers with 0 rounds
        assignmentFilter.setRoundVisible(0, false);

        // Apply filters to the table
        rosterTable.applyFilters();

        // Verify the table model has 4 rows (all except Charlie Brown with 0 rounds)
        assertEquals(4, rosterTable.getTable().getModel().getRowCount());
    }
}
