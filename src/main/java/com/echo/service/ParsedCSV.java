package com.echo.service;

import java.util.List;
import java.util.Map;

/**
 * Stores parsed CSV data to avoid multiple parsing operations.
 * Unlike CSVParser, this class is not consumable and can be used
 * for multiple validation and data extraction operations.
 */
public class ParsedCSV {
    private final List<Map<String, String>> rows;
    private final List<String> headers;
    
    public ParsedCSV(List<Map<String, String>> rows, List<String> headers) {
        this.rows = rows;
        this.headers = headers;
    }
    
    public List<Map<String, String>> getRows() {
        return rows;
    }
    
    public List<String> getHeaderNames() {
        return headers;
    }
    
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}