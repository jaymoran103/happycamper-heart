package com.echo.feature;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;

/**
 * Feature that ensures campers have medical notes information.
 * This feature doesn't modify the data, it just validates that the required field exists.
 */
public class MedicalFeature implements RosterFeature {

    /** Unique identifier for this feature */
    private static final String FEATURE_ID = "medical";

    /** Display name for this feature */
    public static final String FEATURE_NAME = "Medical Notes";

    /** Headers required by this feature */
    private static final List<String> REQUIRED_HEADERS = List.of(
        RosterHeader.MEDICAL_NOTES.camperRosterName
    );

    /** Headers added by this feature (none in this case) */
    private static final List<String> ADDED_HEADERS = Collections.emptyList();

    /** Required formats for fields (none in this case) */
    private static final Map<String, String> REQUIRED_FORMATS = new HashMap<>();

    /** Enables/disables warnings for missing medical notes, relevant here because its common for campers to have no such entry */
    private static boolean WARN_ON_MISSING_DATA = false;

    @Override
    public String getFeatureId() {
        return FEATURE_ID;
    }

    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }

    @Override
    public List<String> getRequiredHeaders() {
        return REQUIRED_HEADERS;
    }

    @Override
    public List<String> getAddedHeaders() {
        return ADDED_HEADERS;
    }

    @Override
    public Map<String, String> getRequiredFormats() {
        return REQUIRED_FORMATS;
    }

    @Override
    public void applyFeature(EnhancedRoster roster, WarningManager warningManager) {
        // This feature doesn't add any headers or modify data
        // It just checks that campers have the required medical notes field
        
        for (Camper camper : roster.getCampers()) {
            String medicalNotes = camper.getValue(RosterHeader.MEDICAL_NOTES.standardName);
            
            if (DataConstants.isEmpty(medicalNotes)) {
                camper.setValue(RosterHeader.MEDICAL_NOTES.standardName, DataConstants.DISPLAY_EMPTY);
                if (WARN_ON_MISSING_DATA){
                        RosterWarning warning = RosterWarning.create_camperMissingField(
                        camper.getData(),
                        RosterHeader.MEDICAL_NOTES.standardName,
                        FEATURE_NAME
                    );
                    warningManager.logWarning(warning);
                }
            } 
        }
        
        // Enable this feature
        roster.enableFeature(FEATURE_ID);
    }

    @Override
    public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) {
        // Ensure all required headers are present
        boolean lacksHeader = false;
        for (String header : REQUIRED_HEADERS) {
            if (!roster.hasHeader(header)) {
                RosterWarning warning = RosterWarning.create_missingFeatureHeader(header, FEATURE_NAME);
                warningManager.logWarning(warning);
                lacksHeader = true; // Mark that we're missing a required header
            }
        }
        
        return !lacksHeader; // Returns false if any required headers are missing
    }

    @Override
    public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) {
        // No specific post-validation needed for this feature
        return true;
    }

    public static void setWarnOnMissingData(boolean warn) {
        WARN_ON_MISSING_DATA = warn;
    }
}
