package com.echo.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a camper in the roster system.
 * Contains basic camper information and a unique identifier.
 *
 * The fundamental data unit in the roster system
 * Each camper instance store data about an individual camper, initially based on data from a User Report
 * Each camper is identified by a unique ID, generated from fields also present in Elective Rosters so data can easilly be linked
 */
public class Camper {
    private final String id;
    private final Map<String, String> data;

    /**
     * Creates a new Camper with the given ID and data.
     *
     * @param id The unique identifier for this camper
     * @param data Map of camper data
     */
    public Camper(String id, Map<String, String> data) {
        this.id = id;
        this.data = new HashMap<>(data);
    }

    /**
     * Alternate constructor creates a new Camper and generates its ID from the given data.
     *
     * @param data Map of camper data used to generate the ID and populate the camper's information
     */
    public Camper(Map<String, String> data) {
        this.id = Roster.generateCamperId(data);
        this.data = new HashMap<>(data);
    }

    /**
     * Gets the unique identifier for this camper.
     *
     * @return The camper's ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the value for a specific field.
     *
     * @param field The field name (typically a header name from RosterHeader class)
     * @return The value of the field, or null if the field doesn't exist
     */
    public String getValue(String field) {
        return data.get(field);
    }

    /**
     * Sets the value for a specific field.
     *
     * @param field The field name (typically a header name from RosterHeader class)
     * @param value The value to set for the field
     */
    public void setValue(String field, String value) {
        data.put(field, value);
    }

    /**
     * Gets all data for this camper.
     *
     * @return A defensive copy of the map containing all camper data
     */
    public Map<String, String> getData() {
        return new HashMap<>(data);
    }

    /**
     * Checks if the camper has a value for the specified field.
     *
     * @param field The field name to check
     * @return true if the field exists and has a non-null value, false otherwise
     */
    public boolean hasValue(String field) {
        return data.containsKey(field) && data.get(field) != null;
    }
}
