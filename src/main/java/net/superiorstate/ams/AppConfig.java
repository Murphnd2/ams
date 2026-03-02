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