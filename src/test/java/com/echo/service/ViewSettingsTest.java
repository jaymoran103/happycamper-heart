package com.echo.service;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ui.component.TableColors;

/**
 * Tests for the ViewSettings class.
 */
public class ViewSettingsTest {

    private ViewSettings settings;
    private boolean originalAlternateShadesEnabled;
    private boolean originalHighlightEmptyDataEnabled;

    @BeforeEach
    public void setUp() {
        // Save original TableColors settings
        originalAlternateShadesEnabled = TableColors.isAlternateShadesEnabled();
        originalHighlightEmptyDataEnabled = TableColors.isHighlightEmptyDataEnabled();

        // Create a new ViewSettings instance
        settings = new ViewSettings();
    }

    @AfterEach
    public void tearDown() {
        // Restore original TableColors settings
        TableColors.setAlternateShades(originalAlternateShadesEnabled);
        TableColors.setHighlightEmptyData(originalHighlightEmptyDataEnabled);
    }

    @Test
    @DisplayName("Default settings should be initialized correctly")
    public void testDefaultSettings() {
        // Verify default settings
        assertFalse(settings.isUseDisplayPlaceholder(), "Display placeholder should be disabled by default");
        assertTrue(settings.isUseRowContrast(), "Row contrast should be enabled by default");
        assertTrue(settings.isHighlightEmptyData(), "Highlight empty data should be enabled by default");
    }

    @Test
    @DisplayName("Setters should update settings correctly")
    public void testSetters() {
        // Change settings
        settings.setUseDisplayPlaceholder(false);
        settings.setUseRowContrast(false);
        settings.setHighlightEmptyData(true);

        // Verify settings were updated
        assertFalse(settings.isUseDisplayPlaceholder(), "Display placeholder should be disabled");
        assertFalse(settings.isUseRowContrast(), "Row contrast should be disabled");
        assertTrue(settings.isHighlightEmptyData(), "Highlight empty data should be enabled");
    }

    @Test
    @DisplayName("apply() should update TableColors settings")
    public void testApply() {
        // Set specific settings
        settings.setUseRowContrast(true);
        settings.setHighlightEmptyData(true);

        // Apply settings
        settings.apply();

        // Verify TableColors settings were updated
        assertTrue(TableColors.isAlternateShadesEnabled(), "Alternate shades should be enabled");
        assertTrue(TableColors.isHighlightEmptyDataEnabled(), "Highlight empty data should be enabled");

        // Change settings
        settings.setUseRowContrast(false);
        settings.setHighlightEmptyData(false);

        // Apply settings again
        settings.apply();

        // Verify TableColors settings were updated
        assertFalse(TableColors.isAlternateShadesEnabled(), "Alternate shades should be disabled");
        assertFalse(TableColors.isHighlightEmptyDataEnabled(), "Highlight empty data should be disabled");
    }

}
