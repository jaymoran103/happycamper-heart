package com.echo.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings for the import process.
 * This class encapsulates all settings related to importing roster data,
 * making it easier to pass around and modify import options.
 */
public class ImportSettings {
    
    private File camperFile;
    private File activityFile;
    private List<String> enabledFeatureIds = new ArrayList<>();
    
    /**
     * Creates a new ImportSettings instance with default values.
     */
    public ImportSettings() {
        // Default constructor
        // Activity feature is always enabled
        enabledFeatureIds.add("activity");
    }
    
    /**
     * Creates a new ImportSettings instance with the specified files.
     * 
     * @param camperFile The file containing camper data
     * @param activityFile The file containing activity data
     */
    public ImportSettings(File camperFile, File activityFile) {
        this();
        this.camperFile = camperFile;
        this.activityFile = activityFile;
    }
    
    /**
     * Gets the camper file.
     * 
     * @return The file containing camper data
     */
    public File getCamperFile() {
        return camperFile;
    }
    
    /**
     * Sets the camper file.
     * 
     * @param camperFile The file containing camper data
     * @return This ImportSettings instance for method chaining
     */
    public ImportSettings setCamperFile(File camperFile) {
        this.camperFile = camperFile;
        return this;
    }
    
    /**
     * Gets the activity file.
     * 
     * @return The file containing activity data
     */
    public File getActivityFile() {
        return activityFile;
    }
    
    /**
     * Sets the activity file.
     * 
     * @param activityFile The file containing activity data
     * @return This ImportSettings instance for method chaining
     */
    public ImportSettings setActivityFile(File activityFile) {
        this.activityFile = activityFile;
        return this;
    }
    
    /**
     * Gets the list of enabled feature IDs.
     * 
     * @return A list of enabled feature IDs
     */
    public List<String> getEnabledFeatureIds() {
        return new ArrayList<>(enabledFeatureIds); // Return a copy to prevent modification
    }
    
    /**
     * Sets the list of enabled feature IDs.
     * Note: The "activity" feature is always enabled and will be added if not present.
     * 
     * @param enabledFeatureIds A list of enabled feature IDs
     * @return This ImportSettings instance for method chaining
     */
    public ImportSettings setEnabledFeatureIds(List<String> enabledFeatureIds) {
        this.enabledFeatureIds = new ArrayList<>(enabledFeatureIds); // Store a copy to prevent modification
        
        // Ensure activity feature is always enabled
        if (!this.enabledFeatureIds.contains("activity")) {
            this.enabledFeatureIds.add("activity");
        }
        
        return this;
    }
    
    /**
     * Adds a feature ID to the list of enabled features.
     * 
     * @param featureId The feature ID to enable
     * @return This ImportSettings instance for method chaining
     */
    public ImportSettings addEnabledFeature(String featureId) {
        if (!enabledFeatureIds.contains(featureId)) {
            enabledFeatureIds.add(featureId);
        }
        return this;
    }
    
    /**
     * Removes a feature ID from the list of enabled features.
     * Note: The "activity" feature cannot be removed.
     * 
     * @param featureId The feature ID to disable
     * @return This ImportSettings instance for method chaining
     */
    public ImportSettings removeEnabledFeature(String featureId) {
        // Don't allow removing the activity feature
        if (!featureId.equals("activity")) {
            enabledFeatureIds.remove(featureId);
        }
        return this;
    }
    
    /**
     * Checks if a feature is enabled.
     * 
     * @param featureId The feature ID to check
     * @return true if the feature is enabled, false otherwise
     */
    public boolean isFeatureEnabled(String featureId) {
        return enabledFeatureIds.contains(featureId);
    }
    
    /**
     * Checks if the settings are valid for import.
     * 
     * @return true if both camper and activity files are set, false otherwise
     */
    public boolean isValid() {
        return camperFile != null && activityFile != null;
    }
}
