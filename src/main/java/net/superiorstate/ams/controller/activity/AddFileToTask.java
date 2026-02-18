package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;

import java.io.IOException;
import java.util.UUID;

@WebServlet(name = "AddFileToTask", value = "/AddFileToTask")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    //1 MB
        maxFileSize = 1024 * 1024 * 10,         //10 MB
        maxRequestSize = 1024 * 1024 * 100      //100 MB
        )
public class AddFileToTask extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        uploadAndAssignFile(request,response);
        goToPage(request,response);
    }

    private void uploadAndAssignFile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Task currentTask = (Task) request.getSession().getAttribute("currentTask");
        Part filePart = request.getPart("file");
        String fileName = filePart.getSubmittedFileName();
        String extension = Validator.getExtensionByStringHandling(fileName).orElse("fnf");
        String savePath = request.getSession().getAttribute("savePath").toString();
        String optionalText = request.getParameter("optionalText");
        String newFileName = UUID.randomUUID() +"."+ extension;
        String thePath= savePath+newFileName;
        String fileDescriptionRaw;
        String fileDescription;

        if(optionalText != null && !optionalText.equals("")){
            fileDescriptionRaw = optionalText +"." + extension;
            fileDescription = fileDescriptionRaw.replaceAll(" ","_");
        } else {
            fileDescription = fileName;
        }

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();

        WebLink webLink = new WebLink();
        webLink.setLinkPath(newFileName);

        webLink.setPlainText(fileDescription);
        LinkType linkType = SequenceDAO.getLinkTypeById(em,1);
        webLink.setLinkType(linkType);
        em.getTransaction().begin();
        em.persist(webLink);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Task task = EntityLookup.getTaskById(em,currentTask.getId());
        task.addWebLink(webLink);
        em.persist(task);
        em.persist(webLink);
        em.getTransaction().commit();
        em.close();
        emf.close();

        for(Part part : request.getParts()){
            part.write(thePath);
        }
       System.out.println("The file uploaded successfully");
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("TaskDetailView");
        dispatcher.forward(request,response);
    }
}
