package net.superiorstate.ams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads infrastructure configuration from ssa.properties at startup.
 * <p>
 * Lookup order:
 * <ol>
 *   <li>System property {@code ssa.config} (e.g. {@code -Dssa.config=/path/to/ssa.properties})</li>
 *   <li>Fallback: {@code {catalina.base}/conf/ssa.properties}</li>
 * </ol>
 * <p>
 * Thread-safe after {@link #load()} completes — all data is read-only.
 */
public final class AppConfig {

    private static final Properties props = new Properties();
    private static boolean loaded = false;
    private static String resolvedPath = null;
    private static volatile String cachedSystemType = null;
    private static volatile String cachedAnthropicApiKey = null;
    private static volatile String cachedHealthSherpaApiKey = null;
    private static volatile String cachedHealthSherpaBaseUrl = null;
    private static volatile boolean masterFlag = false;

    private AppConfig() {}

    /**
     * Load properties from disk. Safe to call multiple times — only the first call reads the file.
     *
     * @return true if the file was found and loaded, false otherwise
     */
    public static synchronized boolean load() {
        if (loaded) return !props.isEmpty();

        loaded = true;
        Path path = resolvePath();
        if (path == null || !Files.exists(path)) {
            System.out.println("⚠️ ssa.properties not found — checked: " + describeLookup(path));
            return false;
        }

        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            resolvedPath = path.toString();
            System.out.println("✅ ssa.properties loaded from: " + resolvedPath
                    + " (PSP_ID=" + get("PSP_ID", "UNSET") + ")");
            return true;
        } catch (IOException e) {
            System.out.println("❌ Failed to read ssa.properties at " + path + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get a property value, or the default if not found.
     */
    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Get a property value, or null if not found.
     */
    public static String get(String key) {
        return props.getProperty(key);
    }

    /**
     * Returns the path the properties were loaded from, or null if not loaded.
     */
    public static String getResolvedPath() {
        return resolvedPath;
    }

    /**
     * Check if properties were successfully loaded.
     */
    public static boolean isLoaded() {
        return loaded && !props.isEmpty();
    }

    public static String getSystemType() {
        if (cachedSystemType != null) return cachedSystemType;
        return get("SYSTEM_TYPE", "PSP");
    }

    /**
     * Cache the authoritative system type read from the DB constants table.
     * Called by AmsDataGlobal during global data initialization.
     */
    public static void setSystemType(String type) {
        if (type != null && !type.isBlank()) {
            cachedSystemType = type.strip().toUpperCase();
        }
    }

    public static boolean isPsp() {
        return "PSP".equalsIgnoreCase(getSystemType());
    }

    public static boolean isBpo() {
        return "BPO".equalsIgnoreCase(getSystemType());
    }

    /**
     * Returns true if this installation is the master management node.
     * Set from the DB constant IS_MASTER or ssa.properties IS_MASTER.
     * A master installation is still a PSP — it just also has the super dashboard.
     */
    public static boolean isMaster() {
        return masterFlag;
    }

    /**
     * Cache the master flag read from the DB constants table.
     * Called by AmsDataGlobal during global data initialization.
     */
    public static void setMaster(boolean value) {
        masterFlag = value;
    }

    // --- Anthropic API Key (DB-first, ssa.properties fallback) ---

    /**
     * Cache the API key read from the DB constants table.
     * Called by AmsDataGlobal during global data initialization.
     *
     * @param key the DB value, or null if no DB constant exists (falls back to ssa.properties)
     */
    public static void setAnthropicApiKey(String key) {
        cachedAnthropicApiKey = key;
    }

    /**
     * Resolves the Anthropic API key: DB constant first, ssa.properties fallback.
     * Returns null if no valid key is configured anywhere.
     */
    public static String getAnthropicApiKey() {
        String key = cachedAnthropicApiKey;
        if (key == null) {
            // No DB constant loaded yet — fall back to properties
            key = get("ANTHROPIC_API_KEY");
        }
        if (key == null || key.isBlank() || "FILL_ME_IN".equals(key)) {
            return null;
        }
        return key;
    }

    /**
     * Returns true if a valid Anthropic API key is configured (DB or ssa.properties).
     */
    public static boolean hasAnthropicApiKey() {
        return getAnthropicApiKey() != null;
    }

    // --- HealthSherpa API Key (DB-first, ssa.properties fallback) ---

    /**
     * Cache the HealthSherpa API key read from the DB constants table.
     * Called by AmsDataGlobal during global data initialization.
     *
     * @param key the DB value, or null if no DB constant exists (falls back to ssa.properties)
     */
    public static void setHealthSherpaApiKey(String key) {
        cachedHealthSherpaApiKey = key;
    }

    /**
     * Resolves the HealthSherpa API key: DB constant first, ssa.properties fallback.
     * Returns null if no valid key is configured anywhere.
     */
    public static String getHealthSherpaApiKey() {
        String key = cachedHealthSherpaApiKey;
        if (key == null) {
            // No DB constant loaded yet — fall back to properties
            key = get("HEALTHSHERPA_API_KEY");
        }
        if (key == null || key.isBlank() || "FILL_ME_IN".equals(key)) {
            return null;
        }
        return key;
    }

    /**
     * Returns true if a valid HealthSherpa API key is configured (DB or ssa.properties).
     */
    public static boolean hasHealthSherpaApiKey() {
        return getHealthSherpaApiKey() != null;
    }

    // --- HealthSherpa Base URL (DB-first, ssa.properties fallback, no default — fails closed) ---

    /**
     * Cache the HealthSherpa base URL read from the DB constants table.
     * Called by AmsDataGlobal during global data initialization.
     *
     * @param baseUrl the DB value, or null if no DB constant exists (falls back to ssa.properties, then null)
     */
    public static void setHealthSherpaBaseUrl(String baseUrl) {
        cachedHealthSherpaBaseUrl = baseUrl;
    }

    /**
     * Resolves the HealthSherpa base URL: DB constant first, ssa.properties fallback,
     * null when neither is configured. There is deliberately no default: a missing base
     * URL means HealthSherpa is not configured on this installation, never a guess at
     * which environment (staging or production) to call.
     */
    public static String getHealthSherpaBaseUrl() {
        String url = cachedHealthSherpaBaseUrl;
        if (url == null || url.isBlank()) {
            url = get("HEALTHSHERPA_BASE_URL");
        }
        if (url == null || url.isBlank()) {
            return null;
        }
        return url;
    }

    // --- ICHRA demo override (ssa.properties only, no default — fails closed) ---

    /**
     * T150 — true only when {@code ICHRA_DEMO_ALLOW_STAGING_PROPOSAL} is explicitly
     * {@code "true"} in ssa.properties. Absent, blank, or any other value means OFF,
     * mirroring {@link #getHealthSherpaBaseUrl()}'s fail-closed contract: absence is a
     * decision, never a default.
     * <p>
     * <b>Deliberately NOT a DB constant.</b> A demo flag must not travel with a database
     * restore — a properties entry is per-installation by construction and cannot be
     * replicated onto another environment by a dump.
     * <p>
     * ⚠️ <b>Read from the {@code props} object loaded once by {@link #load()} at startup,
     * never from disk per call.</b> Changing this line in ssa.properties therefore requires
     * a Tomcat restart to take effect.
     * <p>
     * <b>This is one half of a two-condition gate, never a gate by itself.</b> Every call
     * site must additionally require a PSP-admin session. It enables the ICHRA proposal
     * hand-off button and the snapshot write on staging-sourced rates; it does not and must
     * not affect what renders on the public {@code /proposal/*} path (LA-17), and it
     * suppresses no staging banner.
     */
    public static boolean isIchraDemoStagingAllowed() {
        return "true".equalsIgnoreCase(get("ICHRA_DEMO_ALLOW_STAGING_PROPOSAL"));
    }

    // --- Build Info ---

    private static String appVersion;
    private static String buildTimestamp;

    static {
        // 1. App version: prefer /opt/ssa/current_version.txt (git tag from update.sh)
        try {
            Path versionFile = Paths.get("/opt/ssa/current_version.txt");
            if (Files.exists(versionFile)) {
                String tag = Files.readString(versionFile).trim();
                if (!tag.isBlank()) appVersion = tag;
            }
        } catch (Exception ignored) {}

        // 2. Fall back to Maven-filtered build.properties (pom version)
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("build.properties")) {
            if (in != null) {
                Properties bp = new Properties();
                bp.load(in);
                if (appVersion == null) {
                    String pomVer = bp.getProperty("app.version", "");
                    // Skip if Maven filtering didn't run (literal ${...} still present)
                    if (!pomVer.isBlank() && !pomVer.contains("${")) appVersion = pomVer;
                }
                String ts = bp.getProperty("build.timestamp", "");
                if (!ts.isBlank() && !ts.contains("${")) buildTimestamp = ts;
            }
        } catch (Exception ignored) {}

        // 3. Build timestamp fallback: use AppConfig.class file last-modified
        if (buildTimestamp == null) {
            try {
                java.net.URL classUrl = AppConfig.class.getProtectionDomain().getCodeSource().getLocation();
                if (classUrl != null) {
                    Path classPath = Paths.get(classUrl.toURI());
                    java.time.Instant lastMod = Files.getLastModifiedTime(classPath).toInstant();
                    buildTimestamp = java.time.LocalDateTime.ofInstant(lastMod,
                            java.time.ZoneId.systemDefault()).format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
            } catch (Exception ignored) {}
        }

        if (appVersion == null) appVersion = "Unknown";
        if (buildTimestamp == null) buildTimestamp = "Unknown";
    }

    /** Application version — git tag on deployed machines, pom version in dev. */
    public static String getAppVersion() { return appVersion; }

    /** Build timestamp (e.g., "2026-03-15 14:30") */
    public static String getBuildTimestamp() { return buildTimestamp; }

    // --- Internal ---

    private static Path resolvePath() {
        // 1. System property override
        String override = System.getProperty("ssa.config");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }

        // 2. Fallback: catalina.base/conf/ssa.properties
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            return Paths.get(catalinaBase, "conf", "ssa.properties");
        }

        return null;
    }

    private static String describeLookup(Path attempted) {
        StringBuilder sb = new StringBuilder();
        String override = System.getProperty("ssa.config");
        if (override != null) {
            sb.append("ssa.config=").append(override);
        } else {
            sb.append("ssa.config not set");
        }
        sb.append(", catalina.base=").append(System.getProperty("catalina.base", "(not set)"));
        if (attempted != null) {
            sb.append(", resolved=").append(attempted);
        }
        return sb.toString();
    }
}