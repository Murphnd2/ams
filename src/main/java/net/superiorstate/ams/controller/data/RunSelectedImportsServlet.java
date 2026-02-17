package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import net.superiorstate.ams.data.util.BillingHelper;
import net.superiorstate.ams.data.service.Importer;
import net.superiorstate.ams.data.service.Importer.TableMapping;

import java.io.File;
import java.io.IOException;
import java.util.*;

@WebServlet("/RunSelectedImports")
public class RunSelectedImportsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        String[] selectedFiles = req.getParameterValues("selectedFile");
        if (selectedFiles == null) selectedFiles = new String[0];

        Map<String, String> overrides = new HashMap<>();
        for (String file : selectedFiles) {
            String override = req.getParameter("override_" + file);
            if (override != null && !override.isBlank()) {
                overrides.put(file, override);
            }
        }

        File uploadDir = new File(BillingHelper.CSV_DIR);
        File processedDir = new File(BillingHelper.PROCESSED_DIR);

        try {
            em.getTransaction().begin();
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            for (String fileName : selectedFiles) {
                File file = new File(uploadDir, fileName);
                if (!file.exists()) continue;

                TableMapping mapping = null;
                String forcedPrefix = overrides.get(fileName);

                if (forcedPrefix != null) {
                    mapping = Importer.getMappingByPrefix(forcedPrefix);
                } else {
                    mapping = Importer.findMatchingTableMapping(file.toPath());
                }

                if (mapping != null) {
                    System.out.println("📦 Importing " + fileName + " as " + mapping.filePrefix());
                    if (mapping.customHandler()) {
                        Importer.handleCustomImport(em, file, uploadDir, processedDir, mapping);
                    } else {
                        if (fileName.toLowerCase().endsWith(".csv")) {
                            Importer.convertFileToUTF8(file);
                        }
                        Importer.fallbackCsvInsert(em, file, mapping, processedDir);
                    }
                } else {
                    System.err.println("⚠️ No mapping found for " + fileName + " — skipping");
                }
            }

            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }

        res.sendRedirect("ViewActivity25");
    }
}

