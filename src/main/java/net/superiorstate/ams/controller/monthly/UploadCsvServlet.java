package net.superiorstate.ams.controller.monthly;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.data.Helper;
import net.superiorstate.ams.data.Importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet(name = "UploadCsvServlet", value = "/UploadCsvServlet")
@MultipartConfig
public class UploadCsvServlet extends HttpServlet {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "txt", "xlsx", "xls");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        File uploadDir = new File(Helper.CSV_DIR);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        int uploadCount = 0;
        List<String> matchedFiles = new ArrayList<>();
        List<String> unmatchedFiles = new ArrayList<>();

        // Parse the matchedPrefix inputs (format: prefix||filename)
        Map<String, String> fileToPrefixMap = new HashMap<>();
        for (String param : request.getParameterValues("matchedPrefix")) {
            if (param == null || !param.contains("||")) continue;
            String[] parts = param.split("\\|\\|", 2);
            if (parts.length == 2) {
                fileToPrefixMap.put(parts[1], parts[0]); // key = filename, value = prefix
            }
        }

        for (Part part : request.getParts()) {
            String originalName = extractFileName(part);
            if (originalName == null || !isAllowedExtension(originalName)) {
                unmatchedFiles.add((originalName != null ? originalName : "(unknown)") + " (disallowed extension)");
                continue;
            }

            File tempFile = File.createTempFile("upload_", "_" + System.nanoTime());
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String matchedPrefix = fileToPrefixMap.get(originalName);
                if (matchedPrefix == null) {
                    unmatchedFiles.add(originalName + " (no matching prefix in form)");
                    continue;
                }

                Importer.TableMapping mapping = Importer.TABLE_MAPPINGS.stream()
                        .filter(m -> m.filePrefix().equalsIgnoreCase(matchedPrefix))
                        .findFirst()
                        .orElse(null);

                if (mapping == null) {
                    unmatchedFiles.add(originalName + " (prefix not found in table mappings)");
                    continue;
                }

                // Delete older files with same prefix
                File[] existing = uploadDir.listFiles((dir, name) -> name.startsWith(matchedPrefix + "_"));
                if (existing != null) {
                    for (File old : existing) old.delete();
                }

                String ext = getFileExtension(originalName);
                String newName = matchedPrefix + "_processed_" + TIMESTAMP_FORMAT.format(new Date()) + "." + ext;
                File finalFile = new File(uploadDir, newName);
                Files.move(tempFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                matchedFiles.add(originalName + " → " + newName + " ✅ (" + matchedPrefix + ")");
                uploadCount++;

            } finally {
                if (tempFile.exists()) tempFile.delete();
            }
        }

        request.setAttribute("uploadCount", uploadCount);
        request.setAttribute("matchedFiles", matchedFiles);
        request.setAttribute("unmatchedFiles", unmatchedFiles);
        request.getRequestDispatcher("/WEB-INF/view/a/z_acessory/uploadSummary.jsp")
                .forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("UploadCsvServlet alive");
    }
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp == null) return null;
        for (String token : contentDisp.split(";")) {
            if (token.trim().startsWith("filename")) {
                return Paths.get(token.substring(token.indexOf('=') + 1).replace("\"", "")).getFileName().toString();
            }
        }
        return null;
    }

    private boolean isAllowedExtension(String fileName) {
        String ext = getFileExtension(fileName);
        return ext != null && ALLOWED_EXTENSIONS.contains(ext.toLowerCase());
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1)
                : null;
    }
}







