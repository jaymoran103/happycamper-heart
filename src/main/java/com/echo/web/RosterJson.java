package com.echo.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.logging.RosterWarning;
import com.echo.logging.RosterWarning.WarningType;
import com.echo.logging.WarningManager;
import com.echo.service.RosterService;

/**
 * UI-agnostic JSON projection of an {@link EnhancedRoster} for the browser prototype.
 *
 * <p>This is the web front end's equivalent of the Swing {@code RosterTableModel}: it flattens the
 * roster into headers + rows that a browser can render. All roster cell values are {@code String},
 * so a small hand-rolled JSON writer (with proper escaping) is enough and keeps the prototype
 * dependency-free.</p>
 *
 * <p>Output shape:</p>
 * <pre>
 * { "headers":  [ ...all headers, in display order... ],
 *   "visible":  [ ...default-visible headers, in display order... ],
 *   "features": [ {"id":"activity","name":"Activity"}, ... ],   // only enabled features
 *   "rows":     [ {"_id":"bob_smith_5", "First Name":"Bob", ...}, ... ],
 *   "warningCount": 0, "errorCount": 0 }
 * </pre>
 */
public final class RosterJson {

    private RosterJson() {}

    /**
     * Projects a roster (plus its originating service, for feature/warning metadata) to a JSON string.
     *
     * @param roster  the enhanced roster to serialize (must not be null)
     * @param service the service that produced it, used for feature names and the warning manager
     * @return a JSON document describing the roster
     */
    public static String toJson(EnhancedRoster roster, RosterService service) {
        List<String> allHeaders = roster.getOrderedHeaders();
        List<String> visibleHeaders = roster.getOrderedVisibleHeaders();

        StringBuilder sb = new StringBuilder(1024);
        sb.append('{');

        appendStringArray(sb, "headers", allHeaders);
        sb.append(',');
        appendStringArray(sb, "visible", visibleHeaders);

        // "Problem" columns — the browser table flags these the way the desktop does: a problem
        // column is highlighted when it HAS data (the inverse of normal columns). Mirrors the set in
        // com.echo.ui.component.TableLook so the web view stays faithful to the Swing highlighting.
        List<String> problemHeaders = new ArrayList<>();
        for (RosterHeader h : new RosterHeader[] {
                RosterHeader.UNREQUESTED_ACTIVITIES, RosterHeader.SWIMCONFLICTS, RosterHeader.SWIMLESSON }) {
            if (allHeaders.contains(h.standardName)) {
                problemHeaders.add(h.standardName);
            }
        }
        sb.append(',');
        appendStringArray(sb, "problemHeaders", problemHeaders);

        // Enabled features only (the roster records which features actually applied)
        sb.append(",\"features\":[");
        boolean firstFeature = true;
        for (var feature : service.getAvailableFeatures()) {
            if (!roster.hasFeature(feature.getFeatureId())) {
                continue;
            }
            if (!firstFeature) {
                sb.append(',');
            }
            firstFeature = false;
            sb.append('{');
            appendKey(sb, "id");
            appendString(sb, feature.getFeatureId());
            sb.append(',');
            appendKey(sb, "name");
            appendString(sb, feature.getFeatureName());
            sb.append('}');
        }
        sb.append(']');

        // Rows: one object per camper, keyed by header name (only headers that exist on the roster)
        sb.append(",\"rows\":[");
        boolean firstRow = true;
        for (Camper camper : roster.getCampers()) {
            if (!firstRow) {
                sb.append(',');
            }
            firstRow = false;
            sb.append('{');
            appendKey(sb, "_id");
            appendString(sb, camper.getId());
            for (String header : allHeaders) {
                sb.append(',');
                appendKey(sb, header);
                String value = camper.getValue(header);
                appendString(sb, value == null ? "" : value);
            }
            sb.append('}');
        }
        sb.append(']');

        // Warnings: grouped by type, each with a human-readable label/detail, a count, and a few
        // sample rows. This drives the workflow's "Run → warnings" step with real import feedback.
        int warningTotal = 0;
        int errorTotal = 0;
        sb.append(",\"warnings\":[");
        try {
            WarningManager wm = service.getWarningManager();
            boolean firstGroup = true;
            if (wm.hasWarnings()) {
                for (Map.Entry<WarningType, ArrayList<RosterWarning>> entry : wm.getWarningLog().entrySet()) {
                    WarningType type = entry.getKey();
                    List<RosterWarning> group = entry.getValue();
                    if (group.isEmpty()) {
                        continue;
                    }
                    warningTotal += group.size();
                    if (!firstGroup) {
                        sb.append(',');
                    }
                    firstGroup = false;
                    appendWarningGroup(sb, type, group);
                }
            }
            errorTotal = wm.hasErrors() ? wm.getErrorLog().values().stream().mapToInt(List::size).sum() : 0;
        } catch (RuntimeException ignored) {
            // No warning manager yet (no processing run) — leave the warnings array empty.
        }
        sb.append(']');
        sb.append(",\"warningCount\":").append(warningTotal);
        sb.append(",\"errorCount\":").append(errorTotal);

        sb.append('}');
        return sb.toString();
    }

    /** Serializes one warning type group: {@code {label, detail, count, headers, samples}}. */
    private static void appendWarningGroup(StringBuilder sb, WarningType type, List<RosterWarning> group) {
        sb.append('{');
        appendKey(sb, "label");
        appendString(sb, type.getGeneralExplanation());
        sb.append(',');
        appendKey(sb, "detail");
        appendString(sb, type.getSecondaryExplanation());
        sb.append(',');
        appendKey(sb, "count");
        sb.append(group.size());

        // Column headers describing each sample row's cells.
        sb.append(',');
        appendKey(sb, "headers");
        String[] headers = type.getDisplayHeaders();
        sb.append('[');
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendString(sb, headers[i]);
        }
        sb.append(']');

        // Up to 5 sample rows of context for this warning type.
        sb.append(',');
        appendKey(sb, "samples");
        sb.append('[');
        int limit = Math.min(group.size(), 5);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String[] cells = group.get(i).getDisplayData();
            sb.append('[');
            for (int c = 0; c < cells.length; c++) {
                if (c > 0) {
                    sb.append(',');
                }
                appendString(sb, cells[c] == null ? "" : cells[c]);
            }
            sb.append(']');
        }
        sb.append(']');
        sb.append('}');
    }

    private static void appendStringArray(StringBuilder sb, String key, List<String> values) {
        appendKey(sb, key);
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendString(sb, values.get(i));
        }
        sb.append(']');
    }

    private static void appendKey(StringBuilder sb, String key) {
        appendString(sb, key);
        sb.append(':');
    }

    /** Appends a JSON-escaped, double-quoted string. */
    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
