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
 * Tests for the AssignmentFilter class.
 */
public class AssignmentFilterTest {
    private AssignmentFilter filter;
    private EnhancedRoster roster;

    @BeforeEach
    public void setUp() {
        filter = new AssignmentFilter();
        roster = new EnhancedRoster();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.ROUND_COUNT.standardName);
    }

    private Camper createCamper(String firstName, String lastName, String roundCount) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, firstName);
        data.put(RosterHeader.LAST_NAME.camperRosterName, lastName);
        data.put(RosterHeader.ROUND_COUNT.standardName, roundCount);

        String id = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_";
        return new Camper(id, data);
    }

    @Test
    @DisplayName("Test filter ID and name")
    public void testFilterIdAndName() {
        assertEquals("assignment", filter.getFilterId());
        assertEquals("Assignment Filter", filter.getFilterName());
    }

    @Test
    @DisplayName("Test default visibility settings")
    public void testDefaultVisibilitySettings() {
        // By default, all round counts should be visible
        assertTrue(filter.isRoundVisible(0));
        assertTrue(filter.isRoundVisible(1));
        assertTrue(filter.isRoundVisible(2));
        assertTrue(filter.isRoundVisible(3));
    }

    @Test
    @DisplayName("Test changing visibility settings")
    public void testChangingVisibilitySettings() {
        // Change visibility for round count 0
        filter.setRoundVisible(0, false);

        // Verify the change
        assertFalse(filter.isRoundVisible(0));
        assertTrue(filter.isRoundVisible(1));
        assertTrue(filter.isRoundVisible(2));
        assertTrue(filter.isRoundVisible(3));

        // Change visibility for round count 3
        filter.setRoundVisible(3, false);

        // Verify the change
        assertFalse(filter.isRoundVisible(0));
        assertTrue(filter.isRoundVisible(1));
        assertTrue(filter.isRoundVisible(2));
        assertFalse(filter.isRoundVisible(3));
    }

    @Test
    @DisplayName("Test applying filter with different round counts")
    public void testApplyingFilterWithDifferentRoundCounts() {
        // Create campers with different round counts
        Camper camper0 = createCamper("Zero", "Rounds", "0");
        Camper camper1 = createCamper("One", "Round", "1");
        Camper camper2 = createCamper("Two", "Rounds", "2");
        Camper camper3 = createCamper("Three", "Rounds", "3");

        // By default, all campers should be visible
        assertTrue(filter.apply(camper0));
        assertTrue(filter.apply(camper1));
        assertTrue(filter.apply(camper2));
        assertTrue(filter.apply(camper3));

        // Hide campers with 0 rounds
        filter.setRoundVisible(0, false);

        // Verify filter application
        assertFalse(filter.apply(camper0));
        assertTrue(filter.apply(camper1));
        assertTrue(filter.apply(camper2));
        assertTrue(filter.apply(camper3));

        // Hide campers with 3 rounds
        filter.setRoundVisible(3, false);

        // Verify filter application
        assertFalse(filter.apply(camper0));
        assertTrue(filter.apply(camper1));
        assertTrue(filter.apply(camper2));
        assertFalse(filter.apply(camper3));
    }

    @Test
    @DisplayName("Test applying filter with null round count")
    public void testApplyingFilterWithNullRoundCount() {
        // Create a camper with null round count
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Null");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "Rounds");
        // Deliberately not setting round count
        Camper camperWithNullRounds = new Camper("null_rounds_", data);

        // Campers with null round count should always be visible
        assertTrue(filter.apply(camperWithNullRounds));

        // Even if we hide all round counts
        filter.setRoundVisible(0, false);
        filter.setRoundVisible(1, false);
        filter.setRoundVisible(2, false);
        filter.setRoundVisible(3, false);

        // Campers with null round count should still be visible
        assertTrue(filter.apply(camperWithNullRounds));
    }

    @Test
    @DisplayName("Test applying filter with invalid round count")
    public void testApplyingFilterWithInvalidRoundCount() {
        // Create a camper with invalid round count
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Invalid");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "Rounds");
        data.put(RosterHeader.ROUND_COUNT.standardName, "not a number");
        Camper camperWithInvalidRounds = new Camper("invalid_rounds_", data);

        // Campers with invalid round count should always be visible
        assertTrue(filter.apply(camperWithInvalidRounds));

        // Even if we hide all round counts
        filter.setRoundVisible(0, false);
        filter.setRoundVisible(1, false);
        filter.setRoundVisible(2, false);
        filter.setRoundVisible(3, false);

        // Campers with invalid round count should still be visible
        assertTrue(filter.apply(camperWithInvalidRounds));
    }

    @Test
    @DisplayName("Test creating filter panel")
    public void testCreatingFilterPanel() {
        // Create the filter panel
        CollapsibleFilterPanel panel = filter.createFilterPanel(roster);

        // Verify the panel was created
        assertNotNull(panel);
    }
}
