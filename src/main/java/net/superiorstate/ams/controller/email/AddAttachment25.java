package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.controller.activity.checklist.task.AddFileToTask;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.*;
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
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addFile(request);
        goToPage(request,response);
    }

    private void addFile(HttpServletRequest request) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Part filePart = request.getPart("fileUpload");
        String optionalFileName = request.getParameter("fileUploadText");

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        String fileDescription;
        if(optionalFileName!=null && !optionalFileName.equals(""))
            fileDescription = optionalFileName;
        else
            fileDescription = fileName;
        String correctedDescription = fileDescription.replaceAll(" ","_");
        String extension = AddFileToTask.getExtensionByStringHandling(fileName).orElse("fnf");
        String newFileName = UUID.randomUUID() +"."+ extension;


        InputStream fileContent = filePart.getInputStream();
        String uploadPath = global.getSavePath();
        File uploadDir = new File(uploadPath);
        if(!uploadDir.exists())
            uploadDir.mkdir();
        File file = new File(uploadPath + File.separator + newFileName);

        OutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int length;
        while ((length=fileContent.read(buffer))>0){
            out.write(buffer,0,length);
        }
        out.close();
        fileContent.close();

        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setPlainText(correctedDescription);
        w.setLinkPath(newFileName);
        LinkType linkType = ddC.getLinkTypeById(em,1);
        w.setLinkType(linkType);
        em.persist(w);
        em.getTransaction().commit();

        local.getCurrentEmail().getAttachments().add(w);

        request.getSession().setAttribute("local",local);

        em.close();
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        dispatcher.forward(request,response);
    }
}
