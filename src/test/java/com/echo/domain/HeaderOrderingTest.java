package com.echo.domain;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Tests for the header ordering system in RosterHeader.
 */
public class HeaderOrderingTest {

    @Test
    public void testUpdateHeaderMapOrder() {
        // Create a map with header positions
        Map<String, Integer> headerPositions = new HashMap<>();
        headerPositions.put(RosterHeader.ACTIVITY.standardName, 0);
        headerPositions.put(RosterHeader.LAST_NAME.standardName, 1);
        headerPositions.put(RosterHeader.ROUND_1.standardName, 2);
        headerPositions.put(RosterHeader.FIRST_NAME.standardName, 3);
        headerPositions.put(RosterHeader.GRADE.standardName, 4);
        headerPositions.put("Custom Header", 5);

        // Update the header map order
        RosterHeader.updateHeaderMapOrder(headerPositions);

        // Check that the positions have been updated to match the sorted order
        // The positions should now be sequential (0, 1, 2, 3, 4, 5) based on the
        // order defined in RosterHeader enum

        assertEquals(0, (int) headerPositions.get(RosterHeader.FIRST_NAME.standardName));
        assertEquals(1, (int) headerPositions.get(RosterHeader.LAST_NAME.standardName));
        assertEquals(2, (int) headerPositions.get(RosterHeader.GRADE.standardName));
        assertEquals(3, (int) headerPositions.get(RosterHeader.ACTIVITY.standardName));
        assertEquals(4, (int) headerPositions.get(RosterHeader.ROUND_1.standardName));
        assertEquals(5, (int) headerPositions.get("Custom Header"));
    }
}
