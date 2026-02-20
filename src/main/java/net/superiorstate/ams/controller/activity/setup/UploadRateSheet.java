package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.util.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;

@WebServlet(name = "UploadRateSheet", value = "/uploadRateSheet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 20
)
public class UploadRateSheet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            Part filePart = request.getPart("rateSheet");
            if (filePart == null || filePart.getSize() == 0) {
                response.setStatus(400);
                out.print("{\"error\":\"No file provided\"}");
                return;
            }

            String originalName = filePart.getSubmittedFileName();
            String extension = Validator.getExtensionByStringHandling(originalName).orElse("bin");

            // Only allow pdf, xlsx, xls, csv
            if (!extension.matches("pdf|xlsx|xls|csv")) {
                response.setStatus(400);
                out.print("{\"error\":\"Only PDF, XLSX, XLS, and CSV files are accepted\"}");
                return;
            }

            String storageKey = UUID.randomUUID() + "." + extension;
            String displayName = originalName.replaceAll(" ", "_");
            String contentType = filePart.getContentType();
            long contentLength = filePart.getSize();

            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();

            try (InputStream fileContent = filePart.getInputStream()) {
                // Use a fixed prefix for application uploads
                StorageDAO.uploadFile(em, "applications", storageKey, displayName,
                        fileContent, contentLength, contentType);
            } finally {
                em.close();
            }

            out.print("{\"storageKey\":\"" + storageKey + "\",\"fileName\":\"" + escapeJson(displayName) + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Upload failed\"}");
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}