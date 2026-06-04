package com.echo.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.echo.domain.ActivityRoster;
import com.echo.domain.Camper;
import com.echo.domain.CamperRoster;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.ActivityFeature;
import com.echo.feature.MedicalFeature;
import com.echo.feature.PreferenceFeature;
import com.echo.feature.RosterFeature;
import com.echo.feature.SwimLevelFeature;
import com.echo.filter.FilterManager;
import com.echo.logging.RosterException;
import com.echo.logging.WarningManager;

/**
 * RosterService is the core service for managing roster data and applying RosterFeatures.
 *
 * The RosterService coordinates the process of importing, enhancing, and exporting rosters.
 * It manages a collection of available features that can be applied to rosters and handles
 * the application of these features in the correct order with appropriate validation.
 *
 * This service acts as the central coordinator between the UI, import/export services,
 * and the feature implementations.
 */
public class RosterService {
    private final ImportService importService;
    private final ExportService exportService;
    private final List<RosterFeature> availableFeatures;
    private WarningManager warningManager; // Created new for each processing operation


    //TODO manage exportSettings similarly?
    private ViewSettings viewSettings;

    /**
     * Creates a new RosterService with the given import and export services.
     * Initializes the list of available features that can be applied to rosters.
     *
     * @param importService The service for importing data from files
     * @param exportService The service for exporting data to files
     */
    public RosterService(ImportService importService, ExportService exportService) {
        this.importService = importService;
        this.exportService = exportService;

        this.viewSettings = new ViewSettings();

        // Register available features
        availableFeatures = new ArrayList<>();
        availableFeatures.add(new ActivityFeature());

        // Try to add ProgramFeature if available
        try {
            Class<?> programFeatureClass = Class.forName("com.echo.feature.ProgramFeature");
            RosterFeature programFeature = (RosterFeature) programFeatureClass.getDeclaredConstructor().newInstance();
            availableFeatures.add(programFeature);
        } catch (Exception e) {
            System.out.println("ProgramFeature not available: " + e.getMessage());
            // Continue without ProgramFeature
        }

        availableFeatures.add(new PreferenceFeature());
        availableFeatures.add(new SwimLevelFeature());
        availableFeatures.add(new MedicalFeature());
        // Add additional features here
    }

    /**
     * Creates an enhanced roster from camper and activity files.
     * This is the main method that orchestrates the entire roster enhancement process:
     * 1. Validates input files
     * 2. Imports camper and activity data
     * 3. Creates an enhanced roster with camper data
     * 4. Applies each enabled feature in sequence
     * 5. Sorts headers for consistent display
     *
     * @param camperFile The file containing camper data
     * @param activityFile The file containing activity data
     * @param enabledFeatureIds The IDs of features to enable and apply
     * @return A new EnhancedRoster with all features applied, or null if a critical error occurred
     */
    public EnhancedRoster createEnhancedRoster(File camperFile, File activityFile, List<String> enabledFeatureIds){

        //Create a new WarningManager. doesn't need to be cleared if a new one is created for each process
        warningManager = new WarningManager();

        try {
            // Import tosters (with basic validation)
            CamperRoster camperRoster = importService.importCamperRoster(camperFile);
            ActivityRoster activityRoster = importService.importActivityRoster(activityFile);

            // Standardize program names
            camperRoster.normalizePrograms();

            // Validate rosters
            camperRoster.validate(warningManager);
            activityRoster.validate(warningManager);

            // Create enhanced roster, add camper headers and data.
            EnhancedRoster enhancedRoster = new EnhancedRoster();
            for (String header : camperRoster.getHeaderMap().keySet()) {
                enhancedRoster.addHeader(header);
            }

            // Copy camper data
            for (Camper camper : camperRoster.getCampers()) {
                enhancedRoster.addCamper(camper);
            }

            // Apply each enabled feature
            //System.out.println("RosterService.createEnhancedRoster: Enabled feature IDs: " + enabledFeatureIds);
            for (String featureId : enabledFeatureIds) {
                //System.out.println("RosterService.createEnhancedRoster: Processing feature ID: " + featureId);
                RosterFeature feature = findFeature(featureId);
                if (feature != null) {
                    //System.out.println("RosterService.createEnhancedRoster: Found feature: " + feature.getFeatureId() + " (" + feature.getFeatureName() + ")");

                    // Prevalidate feature, skipping on failure
                    //System.out.println("RosterService.createEnhancedRoster: Prevalidating feature: " + feature.getFeatureId());
                    boolean preValidated = feature.preValidate(enhancedRoster, warningManager);
                    //System.out.println("RosterService.createEnhancedRoster: Prevalidation result: " + preValidated);

                    if (!preValidated) {
                        //System.out.println("RosterService.createEnhancedRoster: Prevalidation failed for feature: " + feature.getFeatureId());
                        // If ActivityFeature fails validation, abort the entire process
                        // since it's a core feature that other features depend on
                        if (feature instanceof ActivityFeature) {
                            //System.out.println("RosterService.createEnhancedRoster: ActivityFeature failed validation, aborting");
                            return null;
                        }
                        // For other features, just skip this feature and continue
                        //System.out.println("RosterService.createEnhancedRoster: Skipping feature: " + feature.getFeatureId());
                        continue;
                    }

                    // Apply feature, with special handling for ActivityFeature
                    // which needs access to the activity roster
                    //System.out.println("RosterService.createEnhancedRoster: Applying feature: " + feature.getFeatureId());
                    try {
                        if (feature instanceof ActivityFeature activityFeature) {
                            //System.out.println("RosterService.createEnhancedRoster: Applying ActivityFeature with activity roster");
                            activityFeature.applyFeature(enhancedRoster, activityRoster, warningManager);
                        } else {
                            //System.out.println("RosterService.createEnhancedRoster: Applying regular feature");
                            feature.applyFeature(enhancedRoster, warningManager);
                        }
                        //System.out.println("RosterService.createEnhancedRoster: Feature applied successfully: " + feature.getFeatureId());
                    } catch (Exception e) {
                        //System.out.println("RosterService.createEnhancedRoster: Error applying feature: " + feature.getFeatureId() + ": " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }


                    // Perform post-validation to ensure the feature was applied correctly
                    //System.out.println("RosterService.createEnhancedRoster: Post-validating feature: " + feature.getFeatureId());
                    boolean postValidated = feature.postValidate(enhancedRoster, warningManager);
                    //System.out.println("RosterService.createEnhancedRoster: Post-validation result: " + postValidated);

                    // If post-validation fails, the problem with the roster is critical enough to scrap the whole process
                    // FUTURE - Save an enhanced roster copy before applying each feature, revert to valid version if post-validation fails
                    if (!postValidated) {
                        //System.out.println("RosterService.createEnhancedRoster: Post-validation failed for feature: " + feature.getFeatureId() + ", aborting");
                        return null;
                    }

                    //System.out.println("RosterService.createEnhancedRoster: Feature " + feature.getFeatureId() + " successfully applied and validated");
                    //System.out.println("RosterService.createEnhancedRoster: Enabled features in roster: " + enhancedRoster.getEnabledFeatures());

                }



            }

            // Sort headers by order once they're all added for consistent display
            // RosterHeader.updateHeaderMapOrder ensures headers appear in a logical order
            // regardless of the order in which features were applied
            RosterHeader.updateHeaderMapOrder(enhancedRoster.getHeaderMap());



            return enhancedRoster;
        }
        catch (RosterException e){
            warningManager.logError(e);
            return null;
        }
        catch (Exception e) {
            RosterException exceptionWrapper = RosterException.create_normalWrapper("An error occurred while merging rosters: " + e.getMessage(), e);
            warningManager.logError(exceptionWrapper);
            return null;
        }
    }

    /**
     * Finds a feature by its unique ID.
     * Searches through the list of available features to find one with a matching ID.
     *
     * @param featureId The ID of the feature to find
     * @return The matching RosterFeature instance, or null if not found
     */
    private RosterFeature findFeature(String featureId) {
        //System.out.println("RosterService.findFeature: Looking for feature with ID: " + featureId);

        //System.out.println("RosterService.findFeature: Available features:");
        //for (RosterFeature feature : availableFeatures) {
        //    System.out.println("  - " + feature.getFeatureId() + " (" + feature.getFeatureName() + ")");
        //}

        for (RosterFeature feature : availableFeatures) {
            if (feature.getFeatureId().equals(featureId)) {
                //System.out.println("RosterService.findFeature: Found feature: " + feature.getFeatureId() + " (" + feature.getFeatureName() + ")");
                return feature;
            }
        }

        //System.out.println("RosterService.findFeature: Feature not found: " + featureId);
        return null;
    }

    /**
     * Gets all available features that can be applied to rosters.
     * Returns a defensive copy to prevent modification of the internal list.
     *
     * @return A new list containing all available RosterFeature instances
     */
    public List<RosterFeature> getAvailableFeatures() {
        return new ArrayList<>(availableFeatures);
    }

    /**
     * Gets the current warning manager.
     * The warning manager contains all warnings and errors encountered during processing.
     *
     * @return The current WarningManager instance
     * @throws IllegalStateException if called before processing has started
     */
    public WarningManager getWarningManager() {
        if (warningManager == null) {
            throw new IllegalStateException("Warning manager is null. New instance should be created whenever a service starts.");
        }
        return warningManager;
    }

    /**
     * Gets the current view settings.
     */
    public ViewSettings getViewSettings() {
        return viewSettings;
    }


    /**
     * Exports a roster using the specified export settings.
     * This method provides a unified interface for all export options.
     *
     * @param roster The enhanced roster to export
     * @param filterManager The filter manager to apply (can be null for non-filtered exports)
     * @param settings The export settings to use
     * @throws RosterException if an error occurs during the export process
     */
    public void exportRoster(EnhancedRoster roster, FilterManager filterManager, ExportSettings settings) throws RosterException {
        try {
            // Use the ExportService to handle the export with the specified settings
            exportService.exportRosterToCSV(roster, filterManager, settings);
        } catch (Exception e) {
            throw RosterException.create_normalWrapper("An error occurred while exporting: " + e.getClass().getName(), e);
        }
    }

}


