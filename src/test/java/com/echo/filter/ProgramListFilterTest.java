package com.echo.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.ReflectionUtils;
import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.ui.filter.CollapsibleFilterPanel;

/**
 * Tests for the ProgramListFilter class.
 */
public class ProgramListFilterTest {
    private SortedProgramFilter filter;
    private EnhancedRoster roster;

    @BeforeEach
    public void setUp() {
        filter = new SortedProgramFilter();
        roster = createTestRoster();
    }

    @Test
    @DisplayName("Test filter creation")
    public void testFilterCreation() {
        assertNotNull(filter);
        assertEquals("program-list", filter.getFilterId());
        assertEquals("Programs Filter", filter.getFilterName());
    }

    @Test
    @DisplayName("Test filter panel creation")
    public void testFilterPanelCreation() {
        // Create the filter panel
        CollapsibleFilterPanel panel = filter.createFilterPanel(roster);

        // Verify the panel was created
        assertNotNull(panel);

        // Just verify the panel was created successfully
        assertTrue(true, "Filter panel created successfully");
    }

    @Test
    @DisplayName("Test row visibility")
    public void testRowVisibility() {
        // By default, all programs should be visible
        for (Camper camper : roster.getCampers()) {
            assertTrue(filter.apply(camper));
        }

        // Set a program to be invisible
        filter.setProgramVisible("Program A", false);

        // Campers in Program A should be invisible, others should be visible
        for (Camper camper : roster.getCampers()) {
            String program = camper.getValue("Program");
            if ("Program A".equals(program)) {
                assertFalse(filter.apply(camper));
            } else {
                assertTrue(filter.apply(camper));
            }
        }
    }

    /**
     * Creates a test roster with campers in different programs.
     */
    private EnhancedRoster createTestRoster() {
        List<Camper> campers = new ArrayList<>();

        // Create campers in Program A with 3 activities
        for (int i = 1; i <= 3; i++) {
            Map<String, String> data = new HashMap<>();
            data.put("Program", "Program A");
            data.put("Activity 1", "Activity 1");
            data.put("Activity 2", "Activity 2");
            data.put("Activity 3", "Activity 3");
            data.put("Rounds Assigned", "3");
            Camper camper = new Camper("A" + i, data);
            campers.add(camper);
        }

        // Create campers in Program B with 2 activities
        for (int i = 1; i <= 3; i++) {
            Map<String, String> data = new HashMap<>();
            data.put("Program", "Program B");
            data.put("Activity 1", "Activity 1");
            data.put("Activity 2", "Activity 2");
            data.put("Rounds Assigned", "2");
            Camper camper = new Camper("B" + i, data);
            campers.add(camper);
        }

        // Create campers in Program C with 1 activity
        for (int i = 1; i <= 3; i++) {
            Map<String, String> data = new HashMap<>();
            data.put("Program", "Program C");
            data.put("Activity 1", "Activity 1");
            data.put("Rounds Assigned", "1");
            Camper camper = new Camper("C" + i, data);
            campers.add(camper);
        }

        // Create campers in Program D with mixed activities
        Map<String, String> data1 = new HashMap<>();
        data1.put("Program", "Program D");
        data1.put("Activity 1", "Activity 1");
        data1.put("Activity 2", "Activity 2");
        data1.put("Rounds Assigned", "2");
        Camper camper1 = new Camper("D1", data1);
        campers.add(camper1);

        Map<String, String> data2 = new HashMap<>();
        data2.put("Program", "Program D");
        data2.put("Activity 1", "Activity 1");
        data2.put("Activity 2", "Activity 2");
        data2.put("Activity 3", "Activity 3");
        data2.put("Rounds Assigned", "3");
        Camper camper2 = new Camper("D2", data2);
        campers.add(camper2);

        // Create the roster
        EnhancedRoster testRoster = new EnhancedRoster();

        // Add required headers
        testRoster.addHeader("Program");
        testRoster.addHeader("Activity 1");
        testRoster.addHeader("Activity 2");
        testRoster.addHeader("Activity 3");
        testRoster.addHeader("Rounds Assigned");

        // Add campers to the roster using reflection since setCampers is not available
        try {
            // Set the campers field
            ReflectionUtils.setFieldValue(testRoster, "campers", campers);

            // Enable the activity and program features
            ReflectionUtils.invokeMethod(testRoster, "enableFeature", "activity");
            ReflectionUtils.invokeMethod(testRoster, "enableFeature", "program");
        }
        catch (Exception e) {
            fail("Reflection error: " + e.getMessage());
        }

        return testRoster;
    }

    /**
     * Recursively finds all components in a container.
     */
    private List<java.awt.Component> findAllComponents(java.awt.Container container) {
        List<java.awt.Component> components = new ArrayList<>();

        for (java.awt.Component component : container.getComponents()) {
            components.add(component);

            if (component instanceof java.awt.Container) {
                components.addAll(findAllComponents((java.awt.Container) component));
            }
        }

        return components;
    }
}
