package com.echo.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;

public class DuplicateActivityFeature implements RosterFeature {

    private static final String FEATURE_ID = "duplicateactivity";
    public static final String FEATURE_NAME = "Duplicate Activity Check";

    private final List<String> addedHeaders = Arrays.asList(RosterHeader.DUPLICATE_ACTIVITY.standardName);

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
        return Collections.emptyList();
    }

    @Override
    public List<String> getAddedHeaders() {
        return addedHeaders;
    }

    @Override
    public Map<String, String> getRequiredFormats() {
        return Collections.emptyMap();
    }

    @Override
    public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) {
        if (!roster.hasFeature("activity")) {
            RosterWarning warning = RosterWarning.create_missingFeatureHeader("Activity feature", FEATURE_NAME);
            warningManager.logWarning(warning);
            return false;
        }
        return true;
    }

    @Override
    public void applyFeature(EnhancedRoster roster, WarningManager warningManager) {
        roster.addHeader(RosterHeader.DUPLICATE_ACTIVITY.standardName);
        for (Camper camper : roster.getCampers()) {
            applyToCamper(camper);
        }
        roster.enableFeature(FEATURE_ID);
    }

    @Override
    public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) {
        return true;
    }

    private void applyToCamper(Camper camper) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> dupes = new LinkedHashSet<>();
        for (int i = 1; i <= ActivityFeature.MAX_ROUNDS; i++) {
            String activity = ActivityFeature.getActivityForCamper(camper, i);
            if (!DataConstants.isEmpty(activity) && !seen.add(activity)) {
                dupes.add(activity);
            }
        }
        camper.setValue(RosterHeader.DUPLICATE_ACTIVITY.standardName,
                dupes.isEmpty() ? "" : String.join(", ", dupes));
    }
}
