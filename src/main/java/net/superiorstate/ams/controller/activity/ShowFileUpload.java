package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
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

            WebLink w = getWebLinkByGuid(em, docGuid);
            if (w == null) {
                response.sendError(404, "File not found");
                return;
            }

            // Get PSP name for the storage prefix
            String pspName = getPspName(em);

            // Generate pre-signed download URL (1 hour)
            String downloadUrl = StorageDAO.getDownloadUrl(em, pspName, w.getLinkPath());

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