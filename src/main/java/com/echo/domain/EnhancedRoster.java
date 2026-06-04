package com.echo.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Extended roster that supports feature tracking and enhancement.
 *
 * The EnhancedRoster extends the base Roster class by adding the ability to track
 * which features have been applied to the roster. This allows features to check if
 * their prerequisites are met and prevents duplicate application of features.
 *
 * Features are identified by their unique feature IDs, which are stored in a map
 * along with their enabled state. This class is the primary data structure used
 * by the RosterService when processing and enhancing roster data.
 */
public class EnhancedRoster extends Roster {
    private final Map<String, Boolean> enabledFeatures = new HashMap<>();

    /**
     * Creates a new empty EnhancedRoster with no enabled features.
     */
    public EnhancedRoster() {
        super();
    }

    /**
     * Enables a feature for this roster.
     *
     * @param featureId The ID of the feature to enable (typically from RosterFeature.getFeatureId())
     */
    public void enableFeature(String featureId) {
        enabledFeatures.put(featureId, true);
    }

    /**
     * Checks if a feature is enabled for this roster.
     *
     * @param featureId The ID of the feature to check
     * @return true if the feature is enabled
     */
    public boolean hasFeature(String featureId) {
        return enabledFeatures.getOrDefault(featureId, false);
    }

    /**
     * Gets all enabled features.
     *
     * @return A defensive copy of the map containing feature IDs to their enabled state
     */
    public Map<String, Boolean> getEnabledFeatures() {
        return new HashMap<>(enabledFeatures);
    }
}
