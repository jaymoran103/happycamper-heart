package com.echo.feature;

import java.util.ArrayList;
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
 * Tests all aspects of the feature including:
 * - Feature metadata
 * - Medical notes validation
 * - Warning generation for missing notes
 * - Edge cases and error handling
 */
public class MedicalFeatureTest {

    private MedicalFeature feature;
    private EnhancedRoster roster;
    private WarningManager warningManager;

    @BeforeEach
    public void setUp() {
        // Initialize feature and dependencies
        feature = new MedicalFeature();
        roster = new EnhancedRoster();
        warningManager = new WarningManager();

        // Add required headers
        roster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        roster.addHeader(RosterHeader.MEDICAL_NOTES.camperRosterName);

        // Add campers with different medical notes scenarios
        // Camper 1: Has medical notes
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        camper1Data.put(RosterHeader.MEDICAL_NOTES.camperRosterName, "Allergic to peanuts");
        Camper camper1 = new Camper(camper1Data);
        roster.addCamper(camper1);

        // Camper 2: Empty medical notes
        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Jane");
        camper2Data.put(RosterHeader.LAST_NAME.camperRosterName, "Smith");
        camper2Data.put(RosterHeader.MEDICAL_NOTES.camperRosterName, "");
        Camper camper2 = new Camper(camper2Data);
        roster.addCamper(camper2);

        // Camper 3: Null medical notes
        Map<String, String> camper3Data = new HashMap<>();
        camper3Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Bob");
        camper3Data.put(RosterHeader.LAST_NAME.camperRosterName, "Johnson");
        camper3Data.put(RosterHeader.MEDICAL_NOTES.camperRosterName, null);
        Camper camper3 = new Camper(camper3Data);
        roster.addCamper(camper3);
    }

    @Test
    @DisplayName("Test feature metadata")
    public void testFeatureMetadata() {
        // Verify feature ID and name
        assertEquals("medical", feature.getFeatureId());
        assertEquals("Medical Notes", feature.getFeatureName());

        // Check required headers
        List<String> requiredHeaders = feature.getRequiredHeaders();
        assertTrue(requiredHeaders.contains(RosterHeader.MEDICAL_NOTES.camperRosterName));

        // This feature doesn't add any headers
        assertTrue(feature.getAddedHeaders().isEmpty());

        // This feature doesn't have any required formats
        assertTrue(feature.getRequiredFormats().isEmpty());
    }

    @Test
    @DisplayName("Test preValidate method")
    public void testPreValidate() {
        // Should return true if the medical notes header exists
        assertTrue(feature.preValidate(roster, warningManager));
        
        // Create a roster without the medical notes header
        EnhancedRoster incompleteRoster = new EnhancedRoster();
        incompleteRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        incompleteRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);

        // Should return false if the medical notes header doesn't exist
        assertFalse(feature.preValidate(incompleteRoster, warningManager));
    }

    @Test
    @DisplayName("Test applyFeature method")
    public void testApplyFeature() {
        MedicalFeature.setWarnOnMissingData(true);

        // Apply the feature
        feature.applyFeature(roster, warningManager);

        // Verify the feature is enabled
        assertTrue(roster.hasFeature("medical"));

        // Verify warnings were generated for campers with missing medical notes
        Map<RosterWarning.WarningType, ArrayList<RosterWarning>> warningLog = warningManager.getWarningLog();

        // Should have warnings of type CAMPER_MISSING_FIELD
        assertTrue(warningLog.containsKey(RosterWarning.WarningType.CAMPER_MISSING_FIELD));

        // Should have 2 warnings (one for empty notes, one for null notes)
        ArrayList<RosterWarning> warnings = warningLog.get(RosterWarning.WarningType.CAMPER_MISSING_FIELD);
        assertEquals(2, warnings.size());

        // Verify warning details
        boolean foundEmptyWarning = false;
        boolean foundNullWarning = false;

        for (RosterWarning warning : warnings) {
            String[] displayData = warning.getDisplayData();
            // First cell is camper name, second is missing field, third is feature name
            String camperName = displayData[0];
            String missingField = displayData[1];

            // Check if the warning is for the expected campers
            if (camperName.contains("Jane")) {
                foundEmptyWarning = true;
                assertEquals(RosterHeader.MEDICAL_NOTES.standardName, missingField);
            } else if (camperName.contains("Bob")) {
                foundNullWarning = true;
                assertEquals(RosterHeader.MEDICAL_NOTES.standardName, missingField);
            }
        }

        assertTrue(foundEmptyWarning, "Should have warning for camper with empty medical notes");
        assertTrue(foundNullWarning, "Should have warning for camper with null medical notes");
    }

    @Test
    @DisplayName("Test postValidate")
    public void testPostValidate() {
        // Should always return true for this feature
        assertTrue(feature.postValidate(roster, warningManager));
    }

    @Test
    @DisplayName("Test feature with no campers")
    public void testFeatureWithNoCampers() {
        // Create an empty roster
        EnhancedRoster emptyRoster = new EnhancedRoster();
        emptyRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        emptyRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        emptyRoster.addHeader(RosterHeader.MEDICAL_NOTES.camperRosterName);

        // Apply the feature
        feature.applyFeature(emptyRoster, warningManager);

        // Verify the feature is enabled
        assertTrue(emptyRoster.hasFeature("medical"));

        // Verify no warnings were generated
        assertTrue(warningManager.getWarningLog().isEmpty());
    }

    @Test
    @DisplayName("Test feature with valid medical notes")
    public void testFeatureWithValidMedicalNotes() {
        // Create a roster with only valid medical notes
        EnhancedRoster validRoster = new EnhancedRoster();
        validRoster.addHeader(RosterHeader.FIRST_NAME.camperRosterName);
        validRoster.addHeader(RosterHeader.LAST_NAME.camperRosterName);
        validRoster.addHeader(RosterHeader.MEDICAL_NOTES.camperRosterName);

        // Add campers with valid medical notes
        Map<String, String> camper1Data = new HashMap<>();
        camper1Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Valid");
        camper1Data.put(RosterHeader.LAST_NAME.camperRosterName, "Notes");
        camper1Data.put(RosterHeader.MEDICAL_NOTES.camperRosterName, "Allergic to peanuts");
        Camper camper1 = new Camper(camper1Data);
        validRoster.addCamper(camper1);

        Map<String, String> camper2Data = new HashMap<>();
        camper2Data.put(RosterHeader.FIRST_NAME.camperRosterName, "Also");
        camper2Data.put(RosterHeader.LAST_NAME.camperRosterName, "Valid");
        camper2Data.put(RosterHeader.MEDICAL_NOTES.camperRosterName, "No medical issues");
        Camper camper2 = new Camper(camper2Data);
        validRoster.addCamper(camper2);

        // Apply the feature
        WarningManager localWarningManager = new WarningManager();
        feature.applyFeature(validRoster, localWarningManager);

        // Verify the feature is enabled
        assertTrue(validRoster.hasFeature("medical"));

        // Verify no warnings were generated
        assertTrue(localWarningManager.getWarningLog().isEmpty());
    }
}
