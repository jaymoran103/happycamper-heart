package com.echo.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;

/**
 * Comprehensive tests for the SwimLevelFeature class.
 * Tests all aspects of the feature including:
 * - Feature metadata
 * - Swim level validation
 * - Activity compatibility checking
 * - Warning generation for missing or invalid data
 */
public class SwimLevelFeatureTest {

    private SwimLevelFeature feature;
    private EnhancedRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        // Initialize feature and dependencies
        feature = new SwimLevelFeature();
        roster = new EnhancedRoster();
        warningManager = new WarningManager();
    }

    @Test
    @DisplayName("Test feature metadata")
    public void testFeatureMetadata() {
        // Verify feature ID and name
        assertEquals("swimlevel", feature.getFeatureId());
        assertEquals("Swim Level Validation", feature.getFeatureName());
    }

    @Test
    @DisplayName("Test required headers")
    public void testRequiredHeaders() {
        // Get and verify required headers
        List<String> requiredHeaders = feature.getRequiredHeaders();
        assertTrue(requiredHeaders.contains(RosterHeader.SWIMCOLOR.camperRosterName));
        assertEquals(1, requiredHeaders.size());
    }

    @Test
    @DisplayName("Test added headers")
    public void testAddedHeaders() {
        // Get and verify added headers
        List<String> addedHeaders = feature.getAddedHeaders();
        assertTrue(addedHeaders.contains(RosterHeader.SWIMCONFLICTS.standardName));
        assertEquals(1, addedHeaders.size());
    }

    @Test
    @DisplayName("Test preValidate with missing header")
    public void testPreValidateWithMissingHeader() {
        // Execute validation without required header
        assertFalse(feature.preValidate(roster, warningManager));
        // Verify correct warnings were generated
        assertTrue(warningManager.getWarningLog().containsKey(RosterWarning.WarningType.MISSING_FEATURE_HEADER));
    }

    @Test
    @DisplayName("Test preValidate with valid headers")
    public void testPreValidateWithValidHeaders() {
        // Setup required header
        roster.addHeader(RosterHeader.SWIMCOLOR.camperRosterName);
        
        // Execute validation
        assertTrue(feature.preValidate(roster, warningManager));
        
        // Verify no warnings were generated
        assertFalse(warningManager.hasWarnings());
    }

    @Test
    @DisplayName("Test applyFeature with valid swim level")
    public void testApplyFeatureWithValidSwimLevel() {
        // Setup test data
        roster.addHeader(RosterHeader.SWIMCOLOR.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.SWIMCOLOR.camperRosterName, "Blue");
        camperData.put(RosterHeader.buildRoundString(1), "Sailing");
        camperData.put(RosterHeader.buildRoundString(2), "Paddlesports");
        camperData.put(RosterHeader.buildRoundString(3), "Skiing");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify results
        assertFalse(warningManager.hasWarnings());
    }

    @Test
    @DisplayName("Test applyFeature with incompatible activity")
    public void testApplyFeatureWithIncompatibleActivity() {
        // Setup test data
        roster.addHeader(RosterHeader.SWIMCOLOR.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.SWIMCOLOR.camperRosterName, "Red");
        camperData.put(RosterHeader.buildRoundString(1), "Sailing");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify results
        assertFalse(warningManager.hasWarnings());
        assertEquals("Sailing", camper.getValue(RosterHeader.SWIMCONFLICTS.standardName));
        
    }

    @Test
    @DisplayName("Test applyFeature with missing swim level")
    public void testApplyFeatureWithMissingSwimLevel() {
        // Setup test data
        roster.addHeader(RosterHeader.SWIMCOLOR.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify correct warnings were generated
        assertTrue(warningManager.getWarningLog().containsKey(RosterWarning.WarningType.CAMPER_MISSING_FIELD));
    }

    @Test
    @DisplayName("Test applyFeature with unknown swim level")
    public void testApplyFeatureWithUnknownSwimLevel() {
        // Setup test data
        roster.addHeader(RosterHeader.SWIMCOLOR.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.SWIMCOLOR.camperRosterName, "Unknown");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify correct warnings were generated
        assertTrue(warningManager.getWarningLog().containsKey(RosterWarning.WarningType.UNKNOWN_SWIM_LEVEL));
    }

    @Test
    @DisplayName("Test applyFeature with multiple activities")
    public void testApplyFeatureWithMultipleActivities() {
        // Setup test data
        roster.addHeader(RosterHeader.SWIMCOLOR.camperRosterName);
        Map<String, String> camperData = new HashMap<>();
        camperData.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camperData.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camperData.put(RosterHeader.SWIMCOLOR.camperRosterName, "White");
        camperData.put(RosterHeader.buildRoundString(1), "Sailing");
        camperData.put(RosterHeader.buildRoundString(2), "Archery");
        Camper camper = new Camper(camperData);
        roster.addCamper(camper);

        // Execute feature
        feature.applyFeature(roster, warningManager);

        // Verify results
        assertFalse(warningManager.hasWarnings());
        assertEquals("Sailing", camper.getValue(RosterHeader.SWIMCONFLICTS.standardName));
    }

    @Test
    @DisplayName("Test postValidate")
    public void testPostValidate() {
        // Execute validation
        assertTrue(feature.postValidate(roster, warningManager));
    }
} 