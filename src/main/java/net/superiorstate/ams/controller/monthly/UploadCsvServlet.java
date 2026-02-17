package net.superiorstate.ams.controller.monthly;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.service.Importer;
import net.superiorstate.ams.data.util.PathUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet(name = "UploadCsvServlet", value = "/UploadCsvServlet")
@MultipartConfig
public class UploadCsvServlet extends HttpServlet {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("csv", "txt", "xlsx", "xls");

    private static final SimpleDateFormat TS =
            new SimpleDateFormat("yyyyMMdd_HHmmss");

    private static String extOf(String fileName) {
        if (fileName == null) return null;
        int i = fileName.lastIndexOf('.');
        return (i > -1 && i < fileName.length() - 1)
                ? fileName.substring(i + 1)
                : null;
    }

    private static boolean allowed(String fileName) {
        String ext = extOf(fileName);
        return ext != null && ALLOWED_EXTENSIONS.contains(ext.toLowerCase());
    }

    private static String safeSubmittedName(Part part) {
        String s = part.getSubmittedFileName();
        if (s != null && !s.isBlank()) {
            return Paths.get(s).getFileName().toString();
        }
        String cd = part.getHeader("content-disposition");
        if (cd != null) {
            for (String token : cd.split(";")) {
                token = token.trim();
                if (token.startsWith("filename")) {
                    String raw = token.substring(token.indexOf('=') + 1)
                            .replace("\"", "");
                    return Paths.get(raw).getFileName().toString();
                }
            }
        }
        return null;
    }

    /** Delete any existing uploaded files for the given prefix. */
    private static void deleteExistingByPrefix(Path dir, String prefix)
            throws IOException {
        // e.g., I3_Employee_*  (match any prior "processed" or otherwise)
        String glob = prefix + "_*";
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, glob)) {
            for (Path p : ds) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignore) {
                    // Best effort; continue deleting others
                }
            }
        }
    }

    /** Ensure target path stays inside uploadDir (no traversal). */
    private static Path safeChild(Path uploadDir, String filename) {
        Path p = uploadDir.resolve(filename).normalize();
        if (!p.startsWith(uploadDir.normalize())) {
            throw new IllegalArgumentException("Invalid filename.");
        }
        return p;
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // Resolve portable upload dir:
        // 1) web.xml context-param AMS_UPLOAD_DIR
        // 2) env var AMS_UPLOAD_DIR
        // 3) -Dams.upload.dir
        // 4) fallback "uploads" under catalina.base
        Path uploadDir = PathUtil.resolveAndEnsureDir(
                getServletContext(),
                "AMS_UPLOAD_DIR",     // context-param name
                "AMS_UPLOAD_DIR",     // env var
                "ams.upload.dir",     // -D system property
                "work/ams-uploads"    // <== fallback now lives under catalina.base/work
        );



        int uploadCount = 0;
        List<String> matchedFiles = new ArrayList<>();
        List<String> unmatchedFiles = new ArrayList<>();

        // Parse matchedPrefix[]=prefix||filename pairs (may be null)
        Map<String, String> fileToPrefix = new HashMap<>();
        String[] params = request.getParameterValues("matchedPrefix");
        if (params != null) {
            for (String p : params) {
                if (p == null || !p.contains("||")) continue;
                String[] parts = p.split("\\|\\|", 2);
                if (parts.length == 2) {
                    // key=filename, val=prefix
                    fileToPrefix.put(parts[1], parts[0]);
                }
            }
        }

        for (Part part : request.getParts()) {
            // Skip non-file parts
            if (part.getContentType() == null && part.getSubmittedFileName() == null)
                continue;

            String original = safeSubmittedName(part);
            if (original == null || !allowed(original)) {
                unmatchedFiles.add(
                        (original != null ? original : "(unknown)")
                                + " (disallowed extension)");
                continue;
            }

            String matchedPrefix = fileToPrefix.get(original);
            if (matchedPrefix == null) {
                unmatchedFiles.add(original + " (no matching prefix in form)");
                continue;
            }

            Importer.TableMapping mapping = Importer.TABLE_MAPPINGS.stream()
                    .filter(m -> m.filePrefix().equalsIgnoreCase(matchedPrefix))
                    .findFirst().orElse(null);

            if (mapping == null) {
                unmatchedFiles.add(original + " (prefix not found in mappings)");
                continue;
            }

            // **Hard replace mode**: delete any prior files for this prefix
            deleteExistingByPrefix(uploadDir, matchedPrefix);

            String ext = extOf(original);
            String stamped = matchedPrefix + "_processed_"
                    + TS.format(new Date()) + "." + ext;

            Path target = safeChild(uploadDir, stamped);
            Path tmp = safeChild(uploadDir, "." + stamped + ".part");

            // Write to temp file first, then atomically move into place
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

            // Hint to container to drop any temp
            try { part.delete(); } catch (Exception ignore) {}

            matchedFiles.add(original + " → " + stamped + " ✅ (" + matchedPrefix + ")");
            uploadCount++;
        }

        request.setAttribute("uploadCount", uploadCount);
        request.setAttribute("matchedFiles", matchedFiles);
        request.setAttribute("unmatchedFiles", unmatchedFiles);
        request.getRequestDispatcher(
                "/WEB-INF/view/a/z_acessory/uploadSummary.jsp"
        ).forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("UploadCsvServlet alive");
    }
}






