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