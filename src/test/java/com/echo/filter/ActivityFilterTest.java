package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.RosterHeader;

/**
 * Tests for the Activity restrict filter (B2): OR membership across the selected set and round scope.
 * The demand sort is a separate concern (see {@code DemandSortServiceTest}).
 */
public class ActivityFilterTest {
    private ActivityFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new ActivityFilter();
    }

    private Camper camper(String r1, String r2, String r3) {
        Map<String, String> data = new HashMap<>();
        if (r1 != null) data.put(RosterHeader.ROUND_1.standardName, r1);
        if (r2 != null) data.put(RosterHeader.ROUND_2.standardName, r2);
        if (r3 != null) data.put(RosterHeader.ROUND_3.standardName, r3);
        return new Camper("id_", data);
    }

    @Test
    @DisplayName("Empty selection is inactive (passes everyone)")
    public void testEmptySelectionPassesAll() {
        assertTrue(filter.apply(camper("Archery", "Sailing", "Biking")));
    }

    @Test
    @DisplayName("OR membership across the selected set, any round")
    public void testOrMembershipAnyRound() {
        Camper c = camper("Archery", "Sailing", "Biking");

        filter.addActivity("Sailing");
        assertTrue(filter.apply(c)); // assigned in R2

        filter.clear();
        filter.addActivity("Fishing"); // not assigned
        assertFalse(filter.apply(c));

        filter.addActivity("Biking"); // OR set {Fishing, Biking}; Biking in R3
        assertTrue(filter.apply(c));
    }

    @Test
    @DisplayName("Round scope restricts to a single round")
    public void testRoundScope() {
        Camper c = camper("Archery", "Sailing", "Biking");
        filter.addActivity("Sailing");

        filter.setRoundScope(2);
        assertTrue(filter.apply(c)); // Sailing in R2

        filter.setRoundScope(1);
        assertFalse(filter.apply(c)); // R1 is Archery

        filter.setRoundScope(ActivityFilter.ROUND_ANY);
        assertTrue(filter.apply(c));
    }

    @Test
    @DisplayName("clear() resets selection and round scope")
    public void testClear() {
        filter.addActivity("Archery");
        filter.setRoundScope(2);
        filter.clear();
        assertTrue(filter.getSelectedActivities().isEmpty());
        assertEquals(ActivityFilter.ROUND_ANY, filter.getRoundScope());
    }

    @Test
    @DisplayName("Filter id and name")
    public void testIdName() {
        assertEquals("activity-select", filter.getFilterId());
        assertEquals("Activity Selector", filter.getFilterName());
    }
}
