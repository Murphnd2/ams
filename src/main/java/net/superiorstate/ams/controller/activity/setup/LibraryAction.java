package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.offering.MarketingMaterial;
import net.superiorstate.ams.model.sales.offering.ResourceCategory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@WebServlet(name = "LibraryAction", value = "/LibraryAction")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB
        maxFileSize = 1024 * 1024 * 25,          // 25 MB
        maxRequestSize = 1024 * 1024 * 50        // 50 MB
)
public class LibraryAction extends HttpServlet {

    private static final Logger log = LogManager.getLogger(LibraryAction.class);

    /** Allowed file extensions for document uploads */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "xlsx", "docx", "csv");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String action = request.getParameter("action");
        String resIdParam = request.getParameter("resId");
        String catIdParam = request.getParameter("catId");
        String errorMsg = null;

        try {
            switch (action) {

                // ── Resource CRUD ───────────────────────────────────────

                case "createResource" -> {
                    String materialType = request.getParameter("materialType");

                    // For DOCUMENT type, validate and upload file
                    String storageGuid = null;
                    if ("DOCUMENT".equals(materialType)) {
                        Part filePart = request.getPart("fileUpload");
                        if (filePart == null || filePart.getSize() == 0) {
                            errorMsg = "File upload required for Document resources.";
                            break;
                        }
                        String ext = getFileExtension(filePart);
                        if (!ALLOWED_EXTENSIONS.contains(ext)) {
                            errorMsg = "Invalid file type. Allowed: .pdf, .xlsx, .docx, .csv";
                            break;
                        }
                        storageGuid = uploadToWasabi(em, psp, filePart);
                    }

                    MarketingMaterial m = new MarketingMaterial();
                    m.setTitle(request.getParameter("title").trim());
                    m.setDescription(request.getParameter("description") != null ? request.getParameter("description").trim() : "");
                    m.setMaterialType(materialType);
                    m.setAudience(request.getParameter("audience"));
                    m.setSortOrder(9999);
                    m.setPsp(psp);

                    // Category
                    String catParam = request.getParameter("categoryId");
                    if (catParam != null && !catParam.isEmpty()) {
                        ResourceCategory cat = em.find(ResourceCategory.class, Long.parseLong(catParam));
                        m.setCategory(cat);
                    }

                    // URL (for LINK or VIDEO)
                    String url = request.getParameter("url");
                    if (url != null && !url.isBlank() && !"DOCUMENT".equals(materialType)) {
                        m.setUrl(url.trim());
                    }

                    if (storageGuid != null) {
                        m.setStorageGuid(storageGuid);
                    }

                    em.getTransaction().begin();
                    em.persist(m);
                    em.getTransaction().commit();
                    resIdParam = m.getId().toString();
                }

                case "editResource" -> {
                    long resId = Long.parseLong(resIdParam);
                    MarketingMaterial m = em.find(MarketingMaterial.class, resId);
                    String materialType = request.getParameter("materialType");

                    // Handle file replacement for DOCUMENT type
                    Part filePart = request.getPart("fileUpload");
                    if (filePart != null && filePart.getSize() > 0) {
                        String ext = getFileExtension(filePart);
                        if (!ALLOWED_EXTENSIONS.contains(ext)) {
                            errorMsg = "Invalid file type. Allowed: .pdf, .xlsx, .docx, .csv";
                            break;
                        }
                        // Upload new file first, then delete old on success
                        String oldGuid = m.getStorageGuid();
                        String guid = uploadToWasabi(em, psp, filePart);
                        m.setStorageGuid(guid);
                        // Delete old file after successful upload
                        if (oldGuid != null && !oldGuid.isBlank()) {
                            try {
                                StorageDAO.deleteFile(em, psp.getFullName(), oldGuid);
                            } catch (Exception e) {
                                log.warn("[LibraryAction] Failed to delete old file (non-fatal): {}", e.getMessage());
                            }
                        }
                    }

                    em.getTransaction().begin();
                    m.setTitle(request.getParameter("title").trim());
                    m.setDescription(request.getParameter("description") != null ? request.getParameter("description").trim() : "");
                    m.setMaterialType(materialType);
                    m.setAudience(request.getParameter("audience"));

                    // Category
                    String catParam = request.getParameter("categoryId");
                    if (catParam != null && !catParam.isEmpty()) {
                        ResourceCategory cat = em.find(ResourceCategory.class, Long.parseLong(catParam));
                        m.setCategory(cat);
                    } else {
                        m.setCategory(null);
                    }

                    // URL
                    String url = request.getParameter("url");
                    if (url != null && !url.isBlank() && !"DOCUMENT".equals(materialType)) {
                        m.setUrl(url.trim());
                    }

                    em.merge(m);
                    em.getTransaction().commit();
                }

                case "deleteResource" -> {
                    long resId = Long.parseLong(resIdParam);
                    MarketingMaterial m = em.find(MarketingMaterial.class, resId);
                    if (m != null) {
                        if (m.getStorageGuid() != null && !m.getStorageGuid().isBlank()) {
                            try {
                                StorageDAO.deleteFile(em, psp.getFullName(), m.getStorageGuid());
                            } catch (Exception e) {
                                log.warn("[LibraryAction] Failed to delete file (non-fatal): {}", e.getMessage());
                            }
                        }
                        em.getTransaction().begin();
                        em.remove(m);
                        em.getTransaction().commit();
                    }
                    resIdParam = null;
                }

                // ── Category CRUD ───────────────────────────────────────

                case "createCategory" -> {
                    ResourceCategory cat = new ResourceCategory();
                    cat.setName(request.getParameter("name").trim());
                    cat.setIconClass(request.getParameter("iconClass") != null ? request.getParameter("iconClass").trim() : "bi-folder");
                    cat.setSortOrder(9999);
                    cat.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(cat);
                    em.getTransaction().commit();
                }

                case "editCategory" -> {
                    long catId = Long.parseLong(catIdParam);
                    ResourceCategory cat = em.find(ResourceCategory.class, catId);
                    em.getTransaction().begin();
                    cat.setName(request.getParameter("name").trim());
                    if (request.getParameter("iconClass") != null) {
                        cat.setIconClass(request.getParameter("iconClass").trim());
                    }
                    em.merge(cat);
                    em.getTransaction().commit();
                }

                case "deleteCategory" -> {
                    long catId = Long.parseLong(catIdParam);
                    ResourceCategory cat = em.find(ResourceCategory.class, catId);
                    if (cat != null) {
                        em.getTransaction().begin();
                        em.createQuery("UPDATE MarketingMaterial m SET m.category = NULL WHERE m.category.id = :catId")
                                .setParameter("catId", catId)
                                .executeUpdate();
                        em.remove(cat);
                        em.getTransaction().commit();
                    }
                }

                default -> log.warn("[LibraryAction] Unknown action: {}", action);
            }
        } catch (Exception e) {
            log.error("[LibraryAction] Error during action={}", action, e);
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (errorMsg == null) errorMsg = "Operation failed — please try again.";
        } finally {
            em.close();
        }

        // Redirect back to LibraryHome with context preserved
        StringBuilder redirect = new StringBuilder("LibraryHome");
        String sep = "?";
        if (errorMsg != null) {
            redirect.append(sep).append("error=").append(java.net.URLEncoder.encode(errorMsg, "UTF-8"));
            sep = "&";
        }
        if (resIdParam != null && !resIdParam.isEmpty()) {
            redirect.append(sep).append("resId=").append(resIdParam);
            sep = "&";
        }
        if (catIdParam != null && !catIdParam.isEmpty() && !"deleteCategory".equals(action)) {
            redirect.append(sep).append("catId=").append(catIdParam);
        }
        response.sendRedirect(redirect.toString());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String getFileExtension(Part filePart) {
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        return Validator.getExtensionByStringHandling(fileName).orElse("").toLowerCase();
    }

    private String uploadToWasabi(EntityManager em, PSP psp, Part filePart) throws IOException {
        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String extension = Validator.getExtensionByStringHandling(fileName).orElse("bin");
        String objectKey = UUID.randomUUID() + "." + extension;

        String displayName = fileName.replaceAll(" ", "_");
        String contentType = filePart.getContentType();
        long contentLength = filePart.getSize();

        try (InputStream is = filePart.getInputStream()) {
            StorageDAO.uploadFile(em, psp.getFullName(), objectKey, displayName, is, contentLength, contentType);
        }
        return objectKey;
    }
}
