package com.echo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the Camper class.
 */
public class CamperTest {

    @Test
    @DisplayName("Constructor with ID should initialize correctly")
    public void testConstructorWithId() {
        // Create test data
        String id = "test_id";
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", "value2");

        // Create camper with ID
        Camper camper = new Camper(id, data);

        // Verify ID and data
        assertEquals(id, camper.getId(), "ID should match the provided value");
        assertEquals("value1", camper.getValue("field1"), "Field value should match");
        assertEquals("value2", camper.getValue("field2"), "Field value should match");
    }

    @Test
    @DisplayName("Constructor without ID should generate ID from data")
    public void testConstructorWithoutId() {
        // Create test data with fields used for ID generation
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.camperRosterName, "John");
        data.put(RosterHeader.LAST_NAME.camperRosterName, "Doe");
        data.put(RosterHeader.GRADE.camperRosterName, "7");

        // Create camper without explicit ID
        Camper camper = new Camper(data);

        // Verify ID was generated
        assertNotNull(camper.getId(), "ID should be generated");
        assertTrue(camper.getId().contains("john"), "ID should contain first name");
        assertTrue(camper.getId().contains("doe"), "ID should contain last name");

        // Verify data
        assertEquals("John", camper.getValue(RosterHeader.FIRST_NAME.camperRosterName), "Field value should match");
        assertEquals("Doe", camper.getValue(RosterHeader.LAST_NAME.camperRosterName), "Field value should match");
        assertEquals("7", camper.getValue(RosterHeader.GRADE.camperRosterName), "Field value should match");
    }

    @Test
    @DisplayName("getValue should return field value")
    public void testGetValue() {
        // Create test data
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", "value2");
        data.put("field3", null);

        // Create camper
        Camper camper = new Camper("test_id", data);

        // Verify field values
        assertEquals("value1", camper.getValue("field1"), "Should return correct value for field1");
        assertEquals("value2", camper.getValue("field2"), "Should return correct value for field2");
        assertEquals(null, camper.getValue("field3"), "Should return null for field3");
        assertEquals(null, camper.getValue("non_existent"), "Should return null for non-existent field");
    }

    @Test
    @DisplayName("setValue should update field value")
    public void testSetValue() {
        // Create test data
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");

        // Create camper
        Camper camper = new Camper("test_id", data);

        // Update existing field
        camper.setValue("field1", "new_value1");
        assertEquals("new_value1", camper.getValue("field1"), "Field value should be updated");

        // Add new field
        camper.setValue("field2", "value2");
        assertEquals("value2", camper.getValue("field2"), "New field should be added");

        // Set field to null
        camper.setValue("field3", null);
        assertEquals(null, camper.getValue("field3"), "Null value should be allowed");
    }

    @Test
    @DisplayName("getData should return a defensive copy")
    public void testGetData() {
        // Create test data
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", "value2");

        // Create camper
        Camper camper = new Camper("test_id", data);

        // Get data
        Map<String, String> retrievedData = camper.getData();

        // Verify data
        assertEquals(data.size(), retrievedData.size(), "Data size should match");
        assertEquals("value1", retrievedData.get("field1"), "Field value should match");
        assertEquals("value2", retrievedData.get("field2"), "Field value should match");

        // Modify retrieved data
        retrievedData.put("field3", "value3");
        retrievedData.put("field1", "modified");

        // Verify original data is unchanged
        assertEquals("value1", camper.getValue("field1"), "Original data should be unchanged");
        assertEquals(null, camper.getValue("field3"), "Original data should be unchanged");
    }

    @Test
    @DisplayName("hasValue should check field existence correctly")
    public void testHasValue() {
        // Create test data
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", null);

        // Create camper
        Camper camper = new Camper("test_id", data);

        // Verify hasValue
        assertTrue(camper.hasValue("field1"), "Should return true for field with value");
        assertFalse(camper.hasValue("field2"), "Should return false for field with null value");
        assertFalse(camper.hasValue("non_existent"), "Should return false for non-existent field");
    }
}
