package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet(name = "AddAttachment25", value = "/AddAttachment25")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    //1 MB
        maxFileSize = 1024 * 1024 * 10,         //10 MB
        maxRequestSize = 1024 * 1024 * 100      //100 MB
)
public class AddAttachment25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addFile(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addFile(request);
        goToPage(request, response);
    }

    private void addFile(HttpServletRequest request) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        Part filePart = request.getPart("fileUpload");
        String optionalFileName = request.getParameter("fileUploadText");

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        String fileDescription;
        if (optionalFileName != null && !optionalFileName.isEmpty())
            fileDescription = optionalFileName;
        else
            fileDescription = fileName;
        String correctedDescription = fileDescription.replaceAll(" ", "_");

        String extension = Validator.getExtensionByStringHandling(fileName).orElse("bin");
        String newFileName = UUID.randomUUID() + "." + extension;

        // Upload to Wasabi via StorageDAO
        String pspName = local.getCurrentPerson().getPsp().getFullName();
        String contentType = filePart.getContentType();
        long contentLength = filePart.getSize();
        String displayName = correctedDescription.endsWith("." + extension)
                ? correctedDescription
                : correctedDescription + "." + extension;

        try (InputStream fileContent = filePart.getInputStream()) {
            StorageDAO.uploadFile(em, pspName, newFileName, displayName, fileContent, contentLength, contentType);
        }

        // Persist WebLink record
        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setPlainText(correctedDescription);
        w.setLinkPath(newFileName);
        LinkType linkType = SequenceDAO.getLinkTypeById(em, 1);
        w.setLinkType(linkType);
        w.setActive(true);
        em.persist(w);
        em.getTransaction().commit();

        local.getCurrentEmail().getAttachments().add(w);
        request.getSession().setAttribute("local", local);

        em.close();
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        dispatcher.forward(request, response);
    }
}