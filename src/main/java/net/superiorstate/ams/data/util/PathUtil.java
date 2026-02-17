package net.superiorstate.ams.data.util;

import jakarta.servlet.ServletContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtil {
    private PathUtil() {}

    public static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase().contains("win");
    }

    private static boolean looksLikeWindowsDrive(String p) {
        return p != null && p.matches("^[A-Za-z]:[\\\\/].*");
    }

    /** First non-blank string. */
    private static String first(String a, String b, String c) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        if (c != null && !c.isBlank()) return c;
        return null;
    }

    /** If on *nix and given a Windows-looking path, drop to fallback. */
    private static Path normalizeForOS(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return Paths.get(fallback);
        if (!isWindows() && looksLikeWindowsDrive(raw))
            return Paths.get(fallback);
        return Paths.get(raw);
    }

    /**
     * Resolve a directory with layered config:
     * 1) web.xml context-param (paramName)
     * 2) environment variable (envName)
     * 3) system property (sysProp)
     * 4) fallback (relative -> anchored to catalina.base)
     */
    public static Path resolveDir(ServletContext ctx,
                                  String paramName,
                                  String envName,
                                  String sysProp,
                                  String fallback) {
        String chosen = first(
                ctx != null ? ctx.getInitParameter(paramName) : null,
                System.getenv(envName),
                System.getProperty(sysProp)
        );

        Path p = normalizeForOS(chosen, fallback);

        // anchor relatives to catalina.base
        if (!p.isAbsolute()) {
            String base = System.getProperty("catalina.base", ".");
            p = Paths.get(base).resolve(p).normalize();
        }
        return p;
    }

    /** Create the directory (and parents) or throw with context. */
    public static Path ensureDir(Path p) {
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create directory: " + p, e);
        }
        return p;
    }

    /**
     * Convenient combo: resolve then ensure, and verify it’s usable.
     * Throws with a clear message if not.
     */
    public static Path resolveAndEnsureDir(ServletContext ctx,
                                           String paramName,
                                           String envName,
                                           String sysProp,
                                           String fallback) {
        Path dir = ensureDir(
                resolveDir(ctx, paramName, envName, sysProp, fallback)
        );
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(
                    "Not a directory: " + dir);
        }
        if (!Files.isWritable(dir)) {
            throw new IllegalStateException(
                    "Directory not writable by process: " + dir);
        }
        return dir;
    }

    /** A safe logs directory under catalina.base/logs by default. */
    public static Path appLogsDir() {
        String base = System.getProperty("catalina.base", ".");
        return Paths.get(base, "logs");
    }
}


