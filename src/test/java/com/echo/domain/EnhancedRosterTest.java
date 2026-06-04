package com.echo.domain;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.logging.WarningManager;

/**
 * Tests for the EnhancedRoster class.
 */
public class EnhancedRosterTest {

    private EnhancedRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        roster = new EnhancedRoster();
        warningManager = new WarningManager();
    }

    @Test
    @DisplayName("addCamper should add a camper to the roster")
    public void testAddCamper() {
        // Create a camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.GRADE.camperRosterName, "7");

        Camper camper = new Camper(camperData);

        // Add the camper
        roster.addCamper(camper);

        // Check that the camper was added
        assertEquals(1, roster.getCampers().size(), "Should have 1 camper");
        assertTrue(roster.getCampers().contains(camper), "Should contain the added camper");
    }

    @Test
    @DisplayName("getCamper should retrieve a camper by ID")
    public void testGetCamper() {
        // Create and add a camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.GRADE.camperRosterName, "7");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Retrieve the camper
        Camper retrievedCamper = roster.getCamperById(camper.getId());

        // Check that the correct camper was retrieved
        assertNotNull(retrievedCamper, "Should retrieve a camper");
        assertEquals(camper, retrievedCamper, "Should retrieve the correct camper");

        // Non-existent ID should return null
        assertNull(roster.getCamperById("non_existent_id"), "Should return null for non-existent ID");
    }

    @Test
    @DisplayName("getValue should retrieve a field value for a camper")
    public void testGetValue() {
        // Create and add a camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.GRADE.camperRosterName, "7");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Retrieve field values
        assertEquals("John", roster.getValue(camper.getId(), RosterHeader.FIRST_NAME.camperRosterName),
                "Should retrieve first name");
        assertEquals("Doe", roster.getValue(camper.getId(), RosterHeader.LAST_NAME.camperRosterName),
                "Should retrieve last name");
        assertEquals("7", roster.getValue(camper.getId(), RosterHeader.GRADE.camperRosterName),
                "Should retrieve grade");

        // Non-existent field should return null
        assertNull(roster.getValue(camper.getId(), "non_existent_field"),
                "Should return null for non-existent field");

        // Non-existent camper should return null
        assertNull(roster.getValue("non_existent_id", RosterHeader.FIRST_NAME.camperRosterName),
                "Should return null for non-existent camper");
    }

    @Test
    @DisplayName("setValue should update a field value for a camper")
    public void testSetValue() {
        // Create and add a camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.GRADE.camperRosterName, "7");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Update a field
        roster.setValue(camper.getId(), RosterHeader.FIRST_NAME.camperRosterName, "Jane");

        // Check that the field was updated
        assertEquals("Jane", roster.getValue(camper.getId(), RosterHeader.FIRST_NAME.camperRosterName),
                "Field should be updated");

        // Add a new field
        roster.setValue(camper.getId(), "new_field", "new_value");

        // Check that the new field was added
        assertEquals("new_value", roster.getValue(camper.getId(), "new_field"),
                "New field should be added");

        // Setting a value for a non-existent camper should do nothing
        roster.setValue("non_existent_id", RosterHeader.FIRST_NAME.camperRosterName, "New Name");
        assertNull(roster.getValue("non_existent_id", RosterHeader.FIRST_NAME.camperRosterName),
                "Should not set value for non-existent camper");
    }

    @Test
    @DisplayName("addHeader should add a header to the roster")
    public void testAddHeader() {
        // Add a header
        roster.addHeader("test_header");

        // Check that the header was added
        assertTrue(roster.getAllHeaders().contains("test_header"), "Header should be added");

        // Adding the same header again should not duplicate it
        roster.addHeader("test_header");
        assertEquals(1, roster.getAllHeaders().size(), "Should not duplicate headers");
    }

    @Test
    @DisplayName("getAllHeaders should return all headers")
    public void testGetAllHeaders() {
        // Add some headers
        roster.addHeader("header1");
        roster.addHeader("header2");
        roster.addHeader("header3");

        // Check that all headers are returned
        assertEquals(3, roster.getAllHeaders().size(), "Should return all headers");
        assertTrue(roster.getAllHeaders().contains("header1"), "Should contain header1");
        assertTrue(roster.getAllHeaders().contains("header2"), "Should contain header2");
        assertTrue(roster.getAllHeaders().contains("header3"), "Should contain header3");
    }

    @Test
    @DisplayName("getHeaderMap should return the header map")
    public void testGetHeaderMap() {
        // Add some headers
        roster.addHeader("header1");
        roster.addHeader("header2");
        roster.addHeader("header3");

        // Get the header map
        Map<String, Integer> headerMap = roster.getHeaderMap();

        // Check that the map contains all headers
        assertEquals(3, headerMap.size(), "Map should contain all headers");
        assertTrue(headerMap.containsKey("header1"), "Map should contain header1");
        assertTrue(headerMap.containsKey("header2"), "Map should contain header2");
        assertTrue(headerMap.containsKey("header3"), "Map should contain header3");

        // Check that indices are assigned correctly
        assertEquals(0, headerMap.get("header1").intValue(), "header1 should have index 0");
        assertEquals(1, headerMap.get("header2").intValue(), "header2 should have index 1");
        assertEquals(2, headerMap.get("header3").intValue(), "header3 should have index 2");
    }

    @Test
    @DisplayName("enableFeature should enable a feature")
    public void testEnableFeature() {
        // Initially, no features are enabled
        assertFalse(roster.hasFeature("test-feature"));

        // Enable a feature
        roster.enableFeature("test-feature");

        // Verify the feature is enabled
        assertTrue(roster.hasFeature("test-feature"));
    }

    @Test
    @DisplayName("hasFeature should return false for non-existent features")
    public void testHasFeatureNonExistent() {
        // Check a feature that doesn't exist
        assertFalse(roster.hasFeature("non-existent-feature"));
    }

    @Test
    @DisplayName("getEnabledFeatures should return a map of all enabled features")
    public void testGetEnabledFeatures() {
        // Enable multiple features
        roster.enableFeature("feature1");
        roster.enableFeature("feature2");
        roster.enableFeature("feature3");

        // Get the enabled features
        Map<String, Boolean> enabledFeatures = roster.getEnabledFeatures();

        // Verify the map contains all enabled features
        assertNotNull(enabledFeatures);
        assertEquals(3, enabledFeatures.size());
        assertTrue(enabledFeatures.containsKey("feature1"));
        assertTrue(enabledFeatures.containsKey("feature2"));
        assertTrue(enabledFeatures.containsKey("feature3"));
        assertTrue(enabledFeatures.get("feature1"));
        assertTrue(enabledFeatures.get("feature2"));
        assertTrue(enabledFeatures.get("feature3"));
    }

    @Test
    @DisplayName("getEnabledFeatures should return a defensive copy")
    public void testGetEnabledFeaturesDefensiveCopy() {
        // Enable a feature
        roster.enableFeature("feature1");

        // Get the enabled features
        Map<String, Boolean> enabledFeatures = roster.getEnabledFeatures();

        // Modify the returned map
        enabledFeatures.put("feature2", true);

        // Get the enabled features again
        Map<String, Boolean> enabledFeatures2 = roster.getEnabledFeatures();

        // Verify the modification didn't affect the internal state
        assertFalse(enabledFeatures2.containsKey("feature2"));
    }
}