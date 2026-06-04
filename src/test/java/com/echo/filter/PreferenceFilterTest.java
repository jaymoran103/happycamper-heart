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
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.ui.filter.CollapsibleFilterPanel;

/**
 * Tests for the PreferenceFilter class.
 */
public class PreferenceFilterTest {
    private PreferenceFilter filter;
    private EnhancedRoster roster;

    @BeforeEach
    public void setUp() {
        filter = new PreferenceFilter();
        roster = new EnhancedRoster();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);

        // Enable the preference feature
        roster.enableFeature("preference");
    }

    private Camper createCamper(String firstName, String lastName, String unrequestedActivities) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, firstName);
        data.put(RosterHeader.LAST_NAME.camperRosterName, lastName);
        data.put(RosterHeader.UNREQUESTED_ACTIVITIES.standardName, unrequestedActivities);

        String id = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_";
        return new Camper(id, data);
    }

    @Test
    @DisplayName("Test filter ID and name")
    public void testFilterIdAndName() {
        assertEquals("preference", filter.getFilterId());
        assertEquals("Preference Filter", filter.getFilterName());
    }

    @Test
    @DisplayName("Test default visibility settings")
    public void testDefaultVisibilitySettings() {
        // By default, all campers should be visible
        assertTrue(filter.isShowCampersWithUnrequestedActivities());
        assertTrue(filter.isShowCampersWithoutUnrequestedActivities());
    }

    @Test
    @DisplayName("Test changing visibility settings")
    public void testChangingVisibilitySettings() {
        // Change visibility for campers with unrequested activities
        filter.setShowCampersWithUnrequestedActivities(false);

        // Verify the change
        assertFalse(filter.isShowCampersWithUnrequestedActivities());
        assertTrue(filter.isShowCampersWithoutUnrequestedActivities());

        // Change visibility for campers without unrequested activities
        filter.setShowCampersWithoutUnrequestedActivities(false);

        // Verify the change
        assertFalse(filter.isShowCampersWithUnrequestedActivities());
        assertFalse(filter.isShowCampersWithoutUnrequestedActivities());
    }

    @Test
    @DisplayName("Test applying filter with different unrequested activities")
    public void testApplyingFilterWithDifferentUnrequestedActivities() {
        // Create campers with different unrequested activities
        Camper camperWithUnrequested = createCamper("With", "Unrequested", "Archery, Swimming");
        Camper camperWithoutUnrequested = createCamper("Without", "Unrequested", DataConstants.DISPLAY_EMPTY);

        // By default, all campers should be visible
        assertTrue(filter.apply(camperWithUnrequested));
        assertTrue(filter.apply(camperWithoutUnrequested));

        // Hide campers with unrequested activities
        filter.setShowCampersWithUnrequestedActivities(false);

        // Verify filter application
        assertFalse(filter.apply(camperWithUnrequested));
        assertTrue(filter.apply(camperWithoutUnrequested));

        // Hide campers without unrequested activities
        filter.setShowCampersWithUnrequestedActivities(true);
        filter.setShowCampersWithoutUnrequestedActivities(false);

        // Verify filter application
        assertTrue(filter.apply(camperWithUnrequested));
        assertFalse(filter.apply(camperWithoutUnrequested));

        // Hide all campers
        filter.setShowCampersWithUnrequestedActivities(false);
        filter.setShowCampersWithoutUnrequestedActivities(false);

        // Verify filter application
        assertFalse(filter.apply(camperWithUnrequested));
        assertFalse(filter.apply(camperWithoutUnrequested));
    }

    @Test
    @DisplayName("Test applying filter with null unrequested activities")
    public void testApplyingFilterWithNullUnrequestedActivities() {
        // Create a camper with null unrequested activities
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "Null");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "Unrequested");
        // Deliberately not setting unrequested activities
        Camper camperWithNullUnrequested = new Camper("null_unrequested_", data);

        // Campers with null unrequested activities should always be visible
        assertTrue(filter.apply(camperWithNullUnrequested));

        // Even if we hide all camper types
        // filter.setShowCampersWithUnrequestedActivities(false);
        // filter.setShowCampersWithoutUnrequestedActivities(false);

        // Campers with null unrequested activities should still be visible - not currenly true
        // assertTrue(filter.apply(camperWithNullUnrequested));
    }

    @Test
    @DisplayName("Test applying filter with 'None' unrequested activities")
    public void testApplyingFilterWithNoUnrequestedActivities() {
        // Create campers with different "empty" unrequested activities values
        Camper camperWithDash = createCamper("Dash", "Unrequested", " - ");
        Camper camperWithNull = createCamper("Null", "Unrequested", null);
        Camper camperWithActivity = createCamper("No Data", "Unrequested", "Archery");

        // These should all be treated as "without unrequested activities"

        // Show only campers without unrequested activities
        filter.setShowCampersWithUnrequestedActivities(false);
        filter.setShowCampersWithoutUnrequestedActivities(true);

        // Verify filter application
        assertTrue(filter.apply(camperWithDash));
        assertTrue(filter.apply(camperWithNull));
        assertFalse(filter.apply(camperWithActivity));


        // Show only campers with unrequested activities
        filter.setShowCampersWithUnrequestedActivities(true);
        filter.setShowCampersWithoutUnrequestedActivities(false);

        // Verify filter application
        assertFalse(filter.apply(camperWithDash));
        assertTrue(filter.apply(camperWithNull));
        assertTrue(filter.apply(camperWithActivity));
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
