package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.WebLink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet(name = "AddDocumentToActivity25", value = "/AddDocumentToActivity25")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 100
)
public class AddDocumentToActivity25 extends HttpServlet {

    private static final Logger log = LogManager.getLogger(AddDocumentToActivity25.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addFile(request);
        goToPage(request, response);
    }

    private void addFile(HttpServletRequest request) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

            Part filePart = request.getPart("fileUpload");
            if (filePart == null || filePart.getSize() == 0) return;

            String optionalName = request.getParameter("fileName");
            String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            String description;
            if (optionalName != null && !optionalName.isBlank())
                description = optionalName.replaceAll(" ", "_");
            else
                description = originalFileName.replaceAll(" ", "_");

            String extension = Validator.getExtensionByStringHandling(originalFileName).orElse("bin");
            String objectKey = UUID.randomUUID() + "." + extension;

            // Upload to Wasabi
            String pspName = local.getCurrentPerson().getPsp().getFullName();
            String contentType = filePart.getContentType();
            long contentLength = filePart.getSize();
            String displayName = description.endsWith("." + extension)
                    ? description
                    : description + "." + extension;

            StorageDAO.UploadResult result;
            try (InputStream fileContent = filePart.getInputStream()) {
                result = StorageDAO.uploadFileSafe(em, pspName, objectKey, displayName,
                        fileContent, contentLength, contentType);
            }

            if (!result.success) {
                log.error("Activity document upload failed: {}", result.errorMessage);
                request.setAttribute("uploadError", "File upload failed — please try again.");
                return;
            }

            // Persist WebLink and attach to Activity only after successful upload
            Activity activity = local.getCurrentActivity().getActivity();
            if (activity == null) return;

            Activity managedActivity = EntityLookup.getActivityById(em, activity.getId());

            em.getTransaction().begin();
            WebLink w = new WebLink();
            w.setPlainText(description);
            w.setLinkPath(objectKey);
            LinkType linkType = SequenceDAO.getLinkTypeById(em, 1); // type 1 = file
            w.setLinkType(linkType);
            w.setActive(true);
            em.persist(w);
            managedActivity.getWebLinkList().add(w);
            em.getTransaction().commit();

            // Update session cache
            activity.getWebLinkList().add(w);
            request.getSession().setAttribute("local", local);

        } catch (Exception e) {
            log.error("Activity document upload error", e);
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.setAttribute("uploadError", "File upload failed — please try again.");
        } finally {
            em.close();
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }
}
