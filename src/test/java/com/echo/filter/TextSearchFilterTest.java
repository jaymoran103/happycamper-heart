package com.echo.filter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.RosterHeader;

/**
 * Tests for the always-on universal TextSearchFilter (B1).
 */
public class TextSearchFilterTest {
    private TextSearchFilter filter;
    private Camper camper;

    @BeforeEach
    public void setUp() {
        filter = new TextSearchFilter();

        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.standardName, "Archibald");
        data.put(RosterHeader.PREFERRED_NAME.standardName, "Archie");
        data.put(RosterHeader.LAST_NAME.standardName, "Bald");
        data.put(RosterHeader.ROUND_1.standardName, "Sailing");
        data.put(RosterHeader.ROUND_2.standardName, "Archery");
        data.put(RosterHeader.ROUND_3.standardName, "Biking");
        camper = new Camper("id_", data);
    }

    @Test
    @DisplayName("Empty term matches everyone (always-on, inert)")
    public void testEmptyTermMatchesAll() {
        filter.setSearchTerm("");
        assertTrue(filter.apply(camper));
        filter.setSearchTerm(null);
        assertTrue(filter.apply(camper));
    }

    @Test
    @DisplayName("All Names scope matches first, preferred, and last")
    public void testAllNamesScope() {
        filter.setScope(TextSearchFilter.SCOPE_ALL_NAMES);

        filter.setSearchTerm("archibald");
        assertTrue(filter.apply(camper)); // first
        filter.setSearchTerm("archie");
        assertTrue(filter.apply(camper)); // preferred
        filter.setSearchTerm("bald");
        assertTrue(filter.apply(camper)); // last

        // Activity value is not searched under name scope
        filter.setSearchTerm("sailing");
        assertFalse(filter.apply(camper));
    }

    @Test
    @DisplayName("Assigned Activities scope matches a round value")
    public void testAssignedActivitiesScope() {
        filter.setScope(TextSearchFilter.SCOPE_ASSIGNED_ACTIVITIES);

        filter.setSearchTerm("sailing");
        assertTrue(filter.apply(camper));
        filter.setSearchTerm("biking");
        assertTrue(filter.apply(camper));

        // A name is not searched under activities scope
        filter.setSearchTerm("archibald");
        assertFalse(filter.apply(camper));
    }

    @Test
    @DisplayName("Per-column scope searches a single header")
    public void testPerColumnScope() {
        filter.setScope(RosterHeader.ROUND_2.standardName);
        filter.setSearchTerm("archery");
        assertTrue(filter.apply(camper));

        filter.setSearchTerm("sailing"); // Round 1, out of scope
        assertFalse(filter.apply(camper));
    }

    @Test
    @DisplayName("Case-insensitive substring match")
    public void testCaseInsensitiveSubstring() {
        filter.setScope(TextSearchFilter.SCOPE_ALL_NAMES);
        filter.setSearchTerm("ARCH");
        assertTrue(filter.apply(camper));
        filter.setSearchTerm("cHiB");
        assertTrue(filter.apply(camper));
    }

    @Test
    @DisplayName("Null/empty scope searches every field")
    public void testAllFieldsScope() {
        filter.setScopeFields("All Fields", null);
        filter.setSearchTerm("biking");
        assertTrue(filter.apply(camper));
        filter.setSearchTerm("archie");
        assertTrue(filter.apply(camper));
    }
}
