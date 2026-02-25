package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves branding files (logos, favicon) from a persistent directory outside the webapp.
 *
 * This prevents uploaded branding files from being wiped on redeployment.
 * The external directory path is stored in the BRANDING_PATH database constant.
 *
 * URL pattern: /branding/{filename}
 * Example:     /branding/logo-login.png  →  reads from {BRANDING_PATH}/logo-login.png
 *
 * Only serves files with allowed extensions (png, ico, jpg, jpeg, gif, svg).
 * Returns 404 if the file doesn't exist or the extension is not allowed.
 */
@WebServlet(name = "ServeBrandingFile", value = "/branding/*")
public class ServeBrandingFile extends HttpServlet {

    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            "png", "ico", "jpg", "jpeg", "gif", "svg"
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        // Must have a filename
        if (pathInfo == null || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Extract just the filename — no subdirectories allowed (prevent path traversal)
        String filename = pathInfo.substring(1); // strip leading /
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Validate extension
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            ext = filename.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Get branding directory from AmsDataGlobal
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global == null) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        String brandingPath = global.getBrandingPath();
        if (brandingPath == null || brandingPath.isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path file = Paths.get(brandingPath, filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Set content type based on extension
        String contentType = switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(file));

        // Cache for 1 hour — branding doesn't change often
        response.setHeader("Cache-Control", "public, max-age=3600");

        // Stream the file
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
        }
    }
}
