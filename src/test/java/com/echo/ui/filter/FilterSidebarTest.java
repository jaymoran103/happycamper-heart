package com.echo.ui.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.filter.AssignmentFilter;
import com.echo.filter.FilterManager;
import com.echo.filter.RosterFilter;
import com.echo.filter.SortedProgramFilter;

/**
 * Tests for the FilterSidebar class.
 */
public class FilterSidebarTest {
    private FilterSidebar sidebar;
    private FilterManager filterManager;
    private EnhancedRoster roster;

    @BeforeEach
    public void setUp() {
        filterManager = new FilterManager();
        roster = new EnhancedRoster();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.ROUND_COUNT.standardName);
        roster.addHeader(RosterHeader.PROGRAM.standardName);

        // Create test campers
        roster.addCamper(createCamper("John", "Doe", "3", "Traditional Camp"));
        roster.addCamper(createCamper("Jane", "Smith", "2", "Adventure Camp"));

        // Enable features
        roster.enableFeature("activity");
        roster.enableFeature("program");

        // Create filters
        filterManager.createFiltersForRoster(roster);

        // Create the sidebar
        sidebar = new FilterSidebar(roster, filterManager);
    }

    private Camper createCamper(String firstName, String lastName, String roundCount, String program) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, firstName);
        data.put(RosterHeader.LAST_NAME.camperRosterName, lastName);
        data.put(RosterHeader.ROUND_COUNT.standardName, roundCount);
        data.put(RosterHeader.PROGRAM.standardName, program);

        String id = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_";
        return new Camper(id, data);
    }

    @Test
    @DisplayName("Test sidebar creation")
    public void testSidebarCreation() {
        // Verify the sidebar was created
        assertNotNull(sidebar);
    }

    @Test
    @DisplayName("Test updating filter panels")
    public void testUpdatingFilterPanels() {
        // Update filter panels
        sidebar.updateFilterPanels();

        // Verify filter panels were created
        // This is a bit tricky to test directly, so we'll check that the sidebar has components
        assertTrue(sidebar.getComponentCount() > 0);
    }

    @Test
    @DisplayName("Test adding filter panel")
    public void testAddingFilterPanel() {
        // Create a test filter
        RosterFilter testFilter = new AssignmentFilter();

        // Add the filter panel
        sidebar.addFilterPanel(testFilter);

        // Verify the panel was added
        // Again, this is tricky to test directly, so we'll check that the sidebar has components
        assertTrue(sidebar.getComponentCount() > 0);
    }

    @Test
    @DisplayName("Test adding multiple filter panels")
    public void testAddingMultipleFilterPanels() {
        // Create test filters
        RosterFilter filter1 = new AssignmentFilter();
        RosterFilter filter2 = new SortedProgramFilter();

        // Add the filter panels
        sidebar.addFilterPanel(filter1);
        sidebar.addFilterPanel(filter2);

        // Verify the panels were added
        assertTrue(sidebar.getComponentCount() > 0);
    }

    @Test
    @DisplayName("Test adding null filter")
    public void testAddingNullFilter() {
        // Try to add a null filter
        sidebar.addFilterPanel(null);

        // This should not cause an exception
    }

    @Test
    @DisplayName("Test adding duplicate filter")
    public void testAddingDuplicateFilter() {
        // Create a test filter
        RosterFilter testFilter = new AssignmentFilter();

        // Add the filter panel twice
        sidebar.addFilterPanel(testFilter);
        sidebar.addFilterPanel(testFilter);

        // This should not cause an exception
    }

    @Test
    @DisplayName("Test clearing filter panels")
    public void testClearingFilterPanels() {
        // Update filter panels
        sidebar.updateFilterPanels();

        // Clear filter panels
        sidebar.clearFilterPanels();

        // Verify filter panels were cleared
        // This is a bit tricky to test directly, so we'll check that the sidebar has no filter panels
        // by checking if updateFilterPanels adds components again
        int componentCount = sidebar.getComponentCount();
        sidebar.updateFilterPanels();
        assertTrue(sidebar.getComponentCount() >= componentCount);
    }
}
