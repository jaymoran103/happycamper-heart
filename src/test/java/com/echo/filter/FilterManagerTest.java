package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.ui.filter.CollapsibleFilterPanel;

/**
 * Tests for the FilterManager class.
 */
public class FilterManagerTest {
    private FilterManager filterManager;
    private EnhancedRoster roster;
    private Camper camper1, camper2, camper3;

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
        camper1 = createCamper("John", "Doe", "3", "Traditional Camp");
        camper2 = createCamper("Jane", "Smith", "2", "Adventure Camp");
        camper3 = createCamper("Bob", "Johnson", "0", "Traditional Camp");

        // Add campers to roster
        roster.addCamper(camper1);
        roster.addCamper(camper2);
        roster.addCamper(camper3);

        // Enable features
        roster.enableFeature("activity");
        roster.enableFeature("program");
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
    @DisplayName("Test adding and retrieving filters")
    public void testAddAndGetFilter() {
        // Create a test filter
        RosterFilter testFilter = new AssignmentFilter();

        // Add the filter
        filterManager.addFilter(testFilter);

        // Verify the filter was added
        assertEquals(1, filterManager.getFilterCount());
        assertNotNull(filterManager.getFilter(testFilter.getFilterId()));
        assertEquals(testFilter, filterManager.getFilter(testFilter.getFilterId()));
    }

    @Test
    @DisplayName("Test getting all filters")
    public void testGetAllFilters() {
        // Create and add test filters
        RosterFilter filter1 = new AssignmentFilter();
        RosterFilter filter2 = new CamperRoundsFilter();

        filterManager.addFilter(filter1);
        filterManager.addFilter(filter2);

        // Verify all filters are returned
        assertEquals(2, filterManager.getAllFilters().size());
        assertTrue(filterManager.getAllFilters().contains(filter1));
        assertTrue(filterManager.getAllFilters().contains(filter2));
    }

    @Test
    @DisplayName("Test creating filters for roster")
    public void testCreateFiltersForRoster() {
        // Create filters for the roster
        filterManager.createFiltersForRoster(roster);

        // Verify the correct filters were created
        assertTrue(filterManager.getFilterCount() >= 2);
        assertNotNull(filterManager.getFilter("assignment"));
        assertNotNull(filterManager.getFilter("program-list"));
    }

    @Test
    @DisplayName("Test applying filters to campers")
    public void testApplyFilters() {
        // Create a custom filter that only shows campers with 3 rounds
        RosterFilter customFilter = new RosterFilter() {
            @Override
            public boolean apply(Camper camper) {
                String roundCount = camper.getValue(RosterHeader.ROUND_COUNT.standardName);
                return roundCount != null && roundCount.equals("3");
            }

            @Override
            public String getFilterId() {
                return "test-custom";
            }

            @Override
            public String getFilterName() {
                return "Custom Test Filter";
            }

            @Override
            public CollapsibleFilterPanel createFilterPanel() {
                return null;
            }
        };

        filterManager.addFilter(customFilter);

        // Verify filter application
        assertTrue(filterManager.applyFilters(camper1)); // Should pass (3 rounds)
        assertFalse(filterManager.applyFilters(camper2)); // Should fail (2 rounds)
        assertFalse(filterManager.applyFilters(camper3)); // Should fail (0 rounds)
    }

    @Test
    @DisplayName("Test applying multiple filters")
    public void testApplyMultipleFilters() {
        // Create filters that check different conditions
        RosterFilter roundFilter = new RosterFilter() {
            @Override
            public boolean apply(Camper camper) {
                String roundCount = camper.getValue(RosterHeader.ROUND_COUNT.standardName);
                return roundCount != null && Integer.parseInt(roundCount) >= 2;
            }

            @Override
            public String getFilterId() {
                return "assignment";
            }

            @Override
            public String getFilterName() {
                return "Assignment Filter";
            }

            @Override
            public CollapsibleFilterPanel createFilterPanel() {
                return null;
            }
        };

        RosterFilter programFilter = new RosterFilter() {
            @Override
            public boolean apply(Camper camper) {
                String program = camper.getValue(RosterHeader.PROGRAM.standardName);
                return program != null && program.equals("Traditional Camp");
            }

            @Override
            public String getFilterId() {
                return "program-list";
            }

            @Override
            public String getFilterName() {
                return "Programs Filter";
            }

            @Override
            public CollapsibleFilterPanel createFilterPanel() {
                return null;
            }
        };

        filterManager.addFilter(roundFilter);
        filterManager.addFilter(programFilter);

        // Verify filter application
        assertTrue(filterManager.applyFilters(camper1)); // Should pass (3 rounds, Traditional Camp)
        assertFalse(filterManager.applyFilters(camper2)); // Should fail (2 rounds but Adventure Camp)
        assertFalse(filterManager.applyFilters(camper3)); // Should fail (0 rounds but Traditional Camp)
    }

    @Test
    @DisplayName("Test empty filter list")
    public void testEmptyFilterList() {
        // No filters added

        // All campers should pass when there are no filters
        assertTrue(filterManager.applyFilters(camper1));
        assertTrue(filterManager.applyFilters(camper2));
        assertTrue(filterManager.applyFilters(camper3));
    }

    @Test
    @DisplayName("Test filter with null camper data")
    public void testFilterWithNullData() {
        // Create a camper with null round count
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Test");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "User");
        // Deliberately not setting round count
        Camper camperWithNullData = new Camper("test_user_", data);

        // Add a filter that checks round count
        RosterFilter roundFilter = new AssignmentFilter();
        filterManager.addFilter(roundFilter);

        // Verify the filter handles null data correctly
        assertTrue(filterManager.applyFilters(camperWithNullData));
    }
}
