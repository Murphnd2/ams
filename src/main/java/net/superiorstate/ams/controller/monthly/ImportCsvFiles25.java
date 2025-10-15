package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Importer;
import net.superiorstate.ams.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@WebServlet(name = "ImportCsvFiles25", value = "/ImportCsvFiles25")
public class ImportCsvFiles25 extends HttpServlet {

    // ImportCsvFiles25.java  (replace the two helpers with these)

    private Path resolveUploadDir() {
        // writable by default on Tomcat: <catalina.base>/work/ams-uploads
        return PathUtil.resolveAndEnsureDir(
                getServletContext(),
                "AMS_UPLOAD_DIR",
                "AMS_UPLOAD_DIR",
                "ams.upload.dir",
                "work/ams-uploads"
        );
    }

    private Path resolveProcessedDir(Path uploadDir) {
        // Try configured value (context-param/env/-D). If absent, use uploadDir/processed.
        String configured =
                Optional.ofNullable(getServletContext().getInitParameter("AMS_PROCESSED_DIR"))
                        .orElseGet(() -> {
                            String v = System.getenv("AMS_PROCESSED_DIR");
                            return (v != null && !v.isBlank()) ? v : System.getProperty("ams.processed.dir");
                        });

        if (configured == null || configured.isBlank()) {
            return PathUtil.ensureDir(uploadDir.resolve("processed"));
        }

        // We have something configured: resolve it portably and ensure it exists
        Path p = PathUtil.resolveDir(
                getServletContext(),
                "AMS_PROCESSED_DIR",
                "AMS_PROCESSED_DIR",
                "ams.processed.dir",
                "work/ams-uploads/processed" // not used because value is present, but harmless
        );
        return PathUtil.ensureDir(p);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Path uploadDir = resolveUploadDir();
        Path processedDir = resolveProcessedDir(uploadDir);

        // Optional: breadcrumb for quick troubleshooting
        getServletContext().log("ImportCsvFiles25 uploadDir=" + uploadDir);
        getServletContext().log("ImportCsvFiles25 processedDir=" + processedDir);

        EntityManagerFactory emf =
                (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            try {
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            } catch (Exception ignore) {}

            Importer.importAllMatchingFilesInMappingOrder(
                    em, uploadDir.toFile(), processedDir.toFile()
            );

            try {
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            } catch (Exception ignore) {}
            em.getTransaction().commit();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.setAttribute("importError", e.getMessage());
            throw new ServletException("Import failed", e);
        } finally {
            em.close();
        }

        response.sendRedirect("ViewHome25");
    }
}




