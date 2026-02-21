package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;

@WebServlet(name = "ShowFileUpload", value = "/ShowFileUpload")
public class ShowFileUpload extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            String docGuid = request.getParameter("doc");
            if (docGuid == null || docGuid.isBlank()) {
                response.sendError(400, "Missing doc parameter");
                return;
            }

            // Try WebLink first (email attachments, task files)
            String objectKey = null;
            WebLink w = getWebLinkByGuid(em, docGuid);
            if (w != null) {
                objectKey = w.getLinkPath();
            }

            // Fall back to MarketingMaterial (library resources)
            if (objectKey == null) {
                objectKey = getLibraryResourceGuid(em, docGuid);
            }

            if (objectKey == null) {
                response.sendError(404, "File not found");
                return;
            }

            // Get PSP name for the storage prefix
            String pspName = getPspName(em);

            // Generate pre-signed download URL (1 hour)
            String downloadUrl = StorageDAO.getDownloadUrl(em, pspName, objectKey);

            // Redirect the user's browser directly to Wasabi
            response.sendRedirect(downloadUrl);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error generating download link");
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    private WebLink getWebLinkByGuid(EntityManager em, String guid) {
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.linkPath = :id");
        q.setParameter("id", guid);
        try {
            return (WebLink) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Checks if the GUID matches a MarketingMaterial storageGuid.
     * Returns the storageGuid (which is also the object key) if found.
     */
    private String getLibraryResourceGuid(EntityManager em, String guid) {
        Query q = em.createQuery("SELECT m.storageGuid FROM MarketingMaterial m WHERE m.storageGuid = :guid");
        q.setParameter("guid", guid);
        try {
            return (String) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private String getPspName(EntityManager em) {
        // PSP ID 4 matches AmsDataGlobal.initializeGlobalData
        try {
            Query q = em.createQuery("SELECT p FROM PSP p WHERE p.id = 4");
            PSP psp = (PSP) q.getSingleResult();
            return psp.getFullName();
        } catch (Exception e) {
            return "default";
        }
    }
}
