package com.echo.web;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.echo.domain.EnhancedRoster;
import com.echo.service.RosterService;

/**
 * Embedded HTTP server for the browser-style UI prototype.
 *
 * <p>This is a deliberately thin, dependency-free front end that runs entirely on the JDK's built-in
 * {@link HttpServer}. It reuses the existing (Swing-free) {@link RosterService} core: the server's
 * only job is to import rosters and hand the result to the browser as JSON. All interaction —
 * search, filtering, column toggles, view settings — happens client-side with no popups.</p>
 *
 * <p>Launched via {@code HappyCamper --web}. See {@code src/main/resources/web/} for the SPA.</p>
 */
public class WebServer {

    /** Ports tried in order; first free one wins. */
    private static final int[] PORT_CANDIDATES = {8080, 8081, 8082, 8090, 0};

    /** Classpath location of the static SPA assets. */
    private static final String WEB_ROOT = "/web";

    /** Directory (relative to the working dir) holding the bundled demo rosters. */
    private static final String DEMO_DIR = "demo-data";

    private final RosterService rosterService;
    private HttpServer server;

    public WebServer(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    /** Binds a port, registers handlers, starts serving, and opens the system browser. */
    public void start() throws IOException {
        server = bind();
        server.createContext("/", this::handleStatic);
        server.createContext("/api/demo", this::handleDemo);
        server.createContext("/api/import", this::handleImport);
        server.setExecutor(null); // default executor; fine for a single-user prototype
        server.start();

        int port = server.getAddress().getPort();
        String url = "http://localhost:" + port;
        System.out.println("HappyCamper web prototype running at " + url);
        System.out.println("(Press Ctrl+C to stop.)");
        openBrowser(url);
    }

    private static HttpServer bind() throws IOException {
        IOException last = null;
        for (int port : PORT_CANDIDATES) {
            try {
                return HttpServer.create(new InetSocketAddress("localhost", port), 0);
            } catch (BindException e) {
                last = e; // port in use, try the next candidate
            }
        }
        throw last != null ? last : new IOException("Could not bind any port");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.out.println("Could not auto-open a browser; visit " + url + " manually.");
        }
    }

    // ------------------------------------------------------------------ handlers

    /** Serves the SPA's static assets from the classpath ({@code /web/...}). */
    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        // Guard against path traversal; only serve known asset names.
        if (path.contains("..")) {
            sendText(exchange, 400, "Bad request");
            return;
        }
        String resource = WEB_ROOT + path;
        try (InputStream in = WebServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                sendText(exchange, 404, "Not found: " + path);
                return;
            }
            byte[] body = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType(path));
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    /**
     * {@code GET /api/demo} → JSON array of demo preset folder names.
     * {@code GET /api/demo?preset=<name>} → roster JSON for that preset.
     */
    private void handleDemo(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String preset = query.get("preset");

        File demoRoot = new File(DEMO_DIR);
        if (preset == null) {
            // List available presets (sub-directories of demo-data/)
            List<String> presets = new ArrayList<>();
            File[] dirs = demoRoot.listFiles(File::isDirectory);
            if (dirs != null) {
                Arrays.sort(dirs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File d : dirs) {
                    presets.add(d.getName());
                }
            }
            String json = presets.stream()
                .map(WebServer::jsonQuote)
                .collect(Collectors.joining(",", "[", "]"));
            sendJson(exchange, 200, json);
            return;
        }

        // Load a specific preset
        File presetDir = new File(demoRoot, preset);
        if (preset.contains("..") || !presetDir.isDirectory()) {
            sendJson(exchange, 404, errorJson("Unknown demo preset: " + preset));
            return;
        }
        File camperFile = findCsv(presetDir, "camper");
        File activityFile = findCsv(presetDir, "activit");
        if (camperFile == null || activityFile == null) {
            sendJson(exchange, 422, errorJson("Demo preset is missing a camper or activity CSV"));
            return;
        }
        buildAndSend(exchange, camperFile, activityFile, allFeatureIds());
    }

    /**
     * {@code POST /api/import} with body {@code {"camper":"<base64>","activity":"<base64>",
     * "features":["activity",...]}} → roster JSON. The CSV contents are base64-encoded so the
     * request body stays trivially parseable without a JSON library.
     */
    private void handleImport(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorJson("Use POST"));
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String camperB64 = extractString(body, "camper");
        String activityB64 = extractString(body, "activity");
        if (camperB64 == null || activityB64 == null) {
            sendJson(exchange, 400, errorJson("Request must include 'camper' and 'activity' CSV data"));
            return;
        }
        List<String> features = extractStringArray(body, "features");
        if (features.isEmpty()) {
            features = allFeatureIds();
        }

        File camperFile = null;
        File activityFile = null;
        try {
            camperFile = writeTemp("camper", Base64.getDecoder().decode(camperB64));
            activityFile = writeTemp("activity", Base64.getDecoder().decode(activityB64));
            buildAndSend(exchange, camperFile, activityFile, features);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, errorJson("Invalid base64 CSV payload"));
        } finally {
            deleteQuietly(camperFile);
            deleteQuietly(activityFile);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Runs the import pipeline and writes the roster JSON (or an error) to the response. */
    private void buildAndSend(HttpExchange exchange, File camperFile, File activityFile,
                              List<String> features) throws IOException {
        EnhancedRoster roster = rosterService.createEnhancedRoster(camperFile, activityFile, features);
        if (roster == null) {
            sendJson(exchange, 422, errorJson("Import failed — the roster could not be processed "
                + "(check that the CSVs are valid camper/activity files)."));
            return;
        }
        sendJson(exchange, 200, RosterJson.toJson(roster, rosterService));
    }

    private List<String> allFeatureIds() {
        return rosterService.getAvailableFeatures().stream()
            .map(f -> f.getFeatureId())
            .collect(Collectors.toList());
    }

    /** Finds the first CSV in {@code dir} whose name contains {@code keyword} (case-insensitive). */
    private static File findCsv(File dir, String keyword) {
        File[] files = dir.listFiles((d, name) ->
            name.toLowerCase().endsWith(".csv") && name.toLowerCase().contains(keyword));
        return (files != null && files.length > 0) ? files[0] : null;
    }

    private static File writeTemp(String prefix, byte[] data) throws IOException {
        Path tmp = Files.createTempFile("hc-" + prefix + "-", ".csv");
        Files.write(tmp, data);
        File f = tmp.toFile();
        f.deleteOnExit();
        return f;
    }

    private static void deleteQuietly(File f) {
        if (f != null) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new java.util.HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    /** Extracts a top-level string value {@code "key":"value"} from a flat JSON body. */
    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\"";
        int k = json.indexOf(needle);
        if (k < 0) {
            return null;
        }
        int colon = json.indexOf(':', k + needle.length());
        if (colon < 0) {
            return null;
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return null;
        }
        int endQuote = json.indexOf('"', firstQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        return json.substring(firstQuote + 1, endQuote);
    }

    /** Extracts a top-level array of quoted strings {@code "key":["a","b"]}. */
    private static List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String needle = "\"" + key + "\"";
        int k = json.indexOf(needle);
        if (k < 0) {
            return result;
        }
        int open = json.indexOf('[', k);
        int close = json.indexOf(']', open + 1);
        if (open < 0 || close < 0) {
            return result;
        }
        String inner = json.substring(open + 1, close);
        for (String token : inner.split(",")) {
            String t = token.trim();
            if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
                result.add(t.substring(1, t.length() - 1));
            }
        }
        return result;
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js"))   return "text/javascript; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }

    private static String jsonQuote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String errorJson(String message) {
        return "{\"error\":" + jsonQuote(message) + "}";
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
