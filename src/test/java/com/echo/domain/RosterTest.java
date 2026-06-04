package com.echo.domain;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.echo.automation.TestPreset;
import com.echo.logging.WarningManager;

/**
 * Tests for the Roster class.
 */
public class RosterTest {

    private Roster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        roster = new Roster();
        warningManager = new WarningManager();
    }

    @Test
    @DisplayName("addCamper should add a camper to the roster")
    public void testAddCamper() {
        // Create a camper
        Map<String, String> camperData = new HashMap<>();
        camperData.put("field1", "value1");
        camperData.put("field2", "value2");

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
        camperData.put("field1", "value1");
        camperData.put("field2", "value2");

        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Retrieve the camper
        Camper retrievedCamper = roster.getCamperById(camper.getId());

        // Check that the correct camper was retrieved
        assertNotNull(retrievedCamper, "Should retrieve a camper");
        assertEquals(camper, retrievedCamper, "Should retrieve the correct camper");

        // Non-existent ID should return null
        assertNull(roster.getCamperById("non-existent-id"), "Should return null for non-existent ID");
    }

    @ParameterizedTest
    @EnumSource(value = TestPreset.class, names = {"MINI_NORMAL"})
    @DisplayName("loadFromCSV should load data from a CSV file")
    public void testLoadFromCSV(TestPreset preset) {
        File file = preset.getCamperFile();

        // Load data from CSV
        assertDoesNotThrow(() -> {
            roster.loadFromCSV(file);
        });

        // Check that data was loaded
        assertFalse(roster.getCampers().isEmpty(), "Should have loaded campers");
    }
}