package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.WebLink;

import java.io.*;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet(name = "AddDocumentToActivity", value = "/AddDocumentToActivity")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    // 1 MB
        maxFileSize = 1024 * 1024 * 10,     // 10 MB
        maxRequestSize = 1024 * 1024 * 100  // 100 MB
)
public class AddDocumentToActivity extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET not supported.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addDocumentToActivity(request);
        forwardToActivityView(request, response);
    }

    private void forwardToActivityView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void addDocumentToActivity(HttpServletRequest request) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Activity currentActivity = local.getCurrentActivity().getActivity();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            uploadAndLinkFile(request, em, local, currentActivity);
        } finally {
            em.close();
        }
    }

    private void uploadAndLinkFile(HttpServletRequest request, EntityManager em, AmsDataLocal local, Activity a) throws ServletException, IOException {
        Part filePart = request.getPart("fileUpload");
        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        String optionalFileName = request.getParameter("fileName");
        String fileDescription = (optionalFileName != null && !optionalFileName.isEmpty()) ? optionalFileName : originalFileName;
        String sanitizedDescription = fileDescription.replaceAll(" ", "_");

        String extension = Validator.getExtensionByStringHandling(originalFileName).orElse("fnf");
        String storedFileName = UUID.randomUUID() + "." + extension;

        saveFile(filePart.getInputStream(), storedFileName, em);
        Activity activity = EntityLookup.getActivityById(em,a.getId());
        em.getTransaction().begin();
        WebLink link = new WebLink();
        link.setPlainText(sanitizedDescription);
        link.setLinkPath(storedFileName);
        link.setLinkType(SequenceDAO.getLinkTypeById(em, 1));
        em.persist(link);
        em.getTransaction().commit();

        if (activity != null) {
            em.getTransaction().begin();
            activity.addWebLink(link);
            em.persist(activity);
            em.getTransaction().commit();

            local.getCurrentActivity().setActivity(activity);
            request.getSession().setAttribute("local", local);
        }
    }

    private void saveFile(InputStream inputStream, String fileName, EntityManager em) throws IOException {
        String uploadPath = AppConstantDAO.getSavePath(em);
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }

        File file = new File(uploadDir, fileName);
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            inputStream.close();
        }
    }
}

