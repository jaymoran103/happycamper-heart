package com.echo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DataConstantsTest {
    
    @Test
    @DisplayName("isEmpty should return true for null values")
    public void testIsEmptyWithNull() {
        assertTrue(DataConstants.isEmpty(null), "Null should be considered empty");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n", DataConstants.DISPLAY_NO_DATA, DataConstants.EXPORT_EMPTY})
    @DisplayName("isEmpty should return true for various empty values")
    public void testIsEmptyWithEmptyValues(String value) {
        assertTrue(DataConstants.isEmpty(value), "Value should be considered empty: " + value);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"data", "0", " data "})
    @DisplayName("isEmpty should return false for non-empty values")
    public void testIsEmptyWithNonEmptyValues(String value) {
        assertFalse(DataConstants.isEmpty(value), "Value should not be considered empty: " + value);
    }
    
    @Test
    @DisplayName("normalizeEmpty should return EMPTY_VALUE for null")
    public void testNormalizeEmptyWithNull() {
        assertEquals(DataConstants.EMPTY_VALUE, DataConstants.normalizeEmpty(null));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", DataConstants.DISPLAY_NO_DATA})
    @DisplayName("normalizeEmpty should return EMPTY_VALUE for empty strings")
    public void testNormalizeEmptyWithEmptyStrings(String value) {
        assertEquals(DataConstants.EMPTY_VALUE, DataConstants.normalizeEmpty(value));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"data", "0", " data "})
    @DisplayName("normalizeEmpty should return trimmed value for non-empty strings")
    public void testNormalizeEmptyWithNonEmptyStrings(String value) {
        assertEquals(value.trim(), DataConstants.normalizeEmpty(value));
    }
    
    @Test
    @DisplayName("getDefaultValue should return appropriate defaults for different headers")
    public void testGetDefaultValue() {
        assertEquals("0", DataConstants.getDefaultValue(RosterHeader.ROUND_COUNT.standardName));
        assertEquals(DataConstants.EMPTY_VALUE, DataConstants.getDefaultValue(RosterHeader.ROUND_1.standardName));
        assertEquals(DataConstants.DISPLAY_NO_DATA, DataConstants.getDefaultValue("unknown_header"));
    }
}