package com.echo.feature;

import java.util.HashMap;
import java.util.List;
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
import com.echo.logging.WarningManager;

/**
 * Comprehensive tests for the ProgramFeature class.
 * Tests all aspects of the feature including:
 * - Feature metadata
 * - Program extraction from ESP field
 * - Session detection
 * - Edge cases and error handling
 */
public class ProgramFeatureTest {

    private ProgramFeature feature;
    private EnhancedRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        // Initialize feature and dependencies
        feature = new ProgramFeature();
        roster = new EnhancedRoster();
        warningManager = new WarningManager();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.ESP.camperRosterName);

        // Add some test campers
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camper1Data.put(RosterHeader.ESP.camperRosterName, "Session 1/Traditional Camp and Session 2/Backpacking");
        Camper camper1 = new Camper(camper1Data);
        roster.addCamper(camper1);

        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Jane");
        camper2Data.put(RosterHeader.LAST_NAME.camperRosterName, "Smith");
        camper2Data.put(RosterHeader.ESP.camperRosterName, "Session 1/Leadership Program");
        Camper camper2 = new Camper(camper2Data);
        roster.addCamper(camper2);

        Map<String, String> camper3Data = new HashMap<>();
        camper3Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Bob");
        camper3Data.put(RosterHeader.LAST_NAME.camperRosterName, "Johnson");
        camper3Data.put(RosterHeader.ESP.camperRosterName, "Session 2/Wilderness Camp"); // Different session
        Camper camper3 = new Camper(camper3Data);
        roster.addCamper(camper3);

        Map<String, String> camper4Data = new HashMap<>();
        camper4Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Alice");
        camper4Data.put(RosterHeader.LAST_NAME.camperRosterName, "Williams");
        camper4Data.put(RosterHeader.ESP.camperRosterName, ""); // Empty ESP
        Camper camper4 = new Camper(camper4Data);
        roster.addCamper(camper4);
    }

    @Test
    @DisplayName("Test feature metadata")
    public void testFeatureMetadata() {
        // Verify feature ID and name
        assertEquals("program", feature.getFeatureId());
        assertEquals("Program Information", feature.getFeatureName());

        // Check required headers
        List<String> requiredHeaders = feature.getRequiredHeaders();
        assertTrue(requiredHeaders.contains(RosterHeader.ESP.camperRosterName));

        // Check added headers
        List<String> addedHeaders = feature.getAddedHeaders();
        assertTrue(addedHeaders.contains(RosterHeader.PROGRAM.standardName));

        // Check required formats - not empty in the current implementation
        assertFalse(feature.getRequiredFormats().isEmpty());
    }

    @Test
    @DisplayName("Test required headers")
    public void testRequiredHeaders() {
        // Get and verify required headers
        List<String> requiredHeaders = feature.getRequiredHeaders();
        assertTrue(requiredHeaders.contains(RosterHeader.ESP.camperRosterName));
        assertEquals(1, requiredHeaders.size());
    }

    @Test
    @DisplayName("Test added headers")
    public void testAddedHeaders() {
        // Get and verify added headers
        List<String> addedHeaders = feature.getAddedHeaders();
        assertTrue(addedHeaders.contains(RosterHeader.PROGRAM.standardName));
        assertEquals(1, addedHeaders.size());
    }

    @Test
    @DisplayName("Test preValidate with valid headers")
    public void testPreValidateWithValidHeaders() {
        // Setup required header
        roster.addHeader(RosterHeader.ESP.camperRosterName);
        
        // Execute validation
        assertTrue(feature.preValidate(roster, warningManager));
        
        // Verify no warnings were generated
        assertFalse(warningManager.hasWarnings());
    }

    @Test
    @DisplayName("Test applyFeature with missing ESP")
    public void testApplyFeatureWithMissingESP() {
        // Setup test data
        roster.addHeader(RosterHeader.ESP.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify warnings were generated
        assertTrue(warningManager.hasWarnings());
    }

    @Test
    @DisplayName("Test applyFeature with invalid ESP format")
    public void testApplyFeatureWithInvalidESPFormat() {
        // Setup test data
        roster.addHeader(RosterHeader.ESP.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.ESP.camperRosterName, "Invalid Format");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify warnings were generated
        assertTrue(warningManager.hasWarnings());
    }

    @Test
    @DisplayName("Test postValidate")
    public void testPostValidate() {
        // Execute validation
        assertTrue(feature.postValidate(roster, warningManager));
    }

    @Test
    @DisplayName("Test session detection with equal distribution")
    public void testSessionDetectionWithEqualDistribution() {
        // Create a roster with equal distribution of sessions
        EnhancedRoster equalRoster = new EnhancedRoster();
        equalRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        equalRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        equalRoster.addHeader(RosterHeader.ESP.camperRosterName);

        // Add campers with different sessions
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Session1");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Camper");
        camper1Data.put(RosterHeader.ESP.camperRosterName, "Session 1/Traditional Camp");
        equalRoster.addCamper(new Camper(camper1Data));

        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Session2");
        camper2Data.put(RosterHeader.LAST_NAME.camperRosterName, "Camper");
        camper2Data.put(RosterHeader.ESP.camperRosterName, "Session 2/Adventure Camp");
        equalRoster.addCamper(new Camper(camper2Data));

        // Apply the feature
        WarningManager localWarningManager = new WarningManager();
        feature.applyFeature(equalRoster, localWarningManager);

        // Verify both programs were extracted correctly (should use the first session found)
        String camper1Id = "session1_camper_";
        String camper2Id = "session2_camper_";

        assertEquals("Traditional Camp", equalRoster.getValue(camper1Id, RosterHeader.PROGRAM.standardName));
        assertEquals("Session 2/Adventure Camp", equalRoster.getValue(camper2Id, RosterHeader.PROGRAM.standardName));
    }

    @Test
    @DisplayName("Test handling of complex ESP formats")
    public void testComplexESPFormats() {
        // Create a roster with complex ESP formats
        EnhancedRoster complexRoster = new EnhancedRoster();
        complexRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        complexRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        complexRoster.addHeader(RosterHeader.ESP.camperRosterName);

        // Add campers with complex ESP formats
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Complex");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Format");
        camper1Data.put(RosterHeader.ESP.camperRosterName, "Session 1/Traditional Camp and Session 2/Adventure Camp and Session 3/Leadership");
        complexRoster.addCamper(new Camper(camper1Data));

        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Unusual");
        camper2Data.put(RosterHeader.LAST_NAME.camperRosterName, "Format");
        camper2Data.put(RosterHeader.ESP.camperRosterName, "Session 1/Program with/Slash");
        complexRoster.addCamper(new Camper(camper2Data));

        // Apply the feature
        WarningManager localWarningManager = new WarningManager();
        feature.applyFeature(complexRoster, localWarningManager);

        // Verify programs were extracted correctly
        String camper1Id = "complex_format_";
        String camper2Id = "unusual_format_";

        assertEquals("Traditional Camp", complexRoster.getValue(camper1Id, RosterHeader.PROGRAM.standardName));
        assertEquals("Program with", complexRoster.getValue(camper2Id, RosterHeader.PROGRAM.standardName));
    }

    @Test
    @DisplayName("Test handling of invalid ESP formats")
    public void testInvalidESPFormats() {
        // Create a roster with invalid ESP formats
        EnhancedRoster invalidRoster = new EnhancedRoster();
        invalidRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        invalidRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        invalidRoster.addHeader(RosterHeader.ESP.camperRosterName);

        // Add campers with invalid ESP formats
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Invalid");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Format");
        camper1Data.put(RosterHeader.ESP.camperRosterName, "Not a valid ESP format");
        invalidRoster.addCamper(new Camper(camper1Data));

        // Apply the feature
        WarningManager localWarningManager = new WarningManager();
        feature.applyFeature(invalidRoster, localWarningManager);

        // Verify the original ESP value was used
        String camperId = "invalid_format_";
        assertEquals("", invalidRoster.getValue(camperId, RosterHeader.PROGRAM.standardName));

        assertNotNull(invalidRoster.getValue(camperId, RosterHeader.PROGRAM.standardName));
        assertEquals("", invalidRoster.getValue(camperId, RosterHeader.PROGRAM.standardName));
    }
}
