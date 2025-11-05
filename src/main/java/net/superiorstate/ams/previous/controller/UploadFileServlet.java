package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "UploadFileServlet", value = "/UploadFileServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    //1 MB
        maxFileSize = 1024 * 1024 * 10,         //10 MB
        maxRequestSize = 1024 * 1024 * 100      //100 MB
)
public class UploadFileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();
        Part filePart = request.getPart("file");
        String fileName = filePart.getSubmittedFileName();
        String extension = getExtensionByStringHandling(fileName).orElse("fnf");
        String newFileName = null;
        WebLink webLink = new WebLink();
        webLink.setLinkPath("https://superiorstate.net");
        webLink.setPlainText(fileName);
        LinkType linkType = ddC.getLinkTypeById(em,1);
        webLink.setLinkType(linkType);
        em.getTransaction().begin();
        em.persist(webLink);
        em.getTransaction().commit();
        em.close();
        emf.close();
        newFileName = webLink.getId().toString() + "." + extension;
        for(Part part : request.getParts()){
            part.write("C:\\Users\\kevinmurphy.SUPERIORSTATE\\IdeaProjects\\km_web_100\\src\\main\\webapp\\WEB-INF\\view\\weblink\\linkfiles\\" + newFileName);
        }
        response.getWriter().println("The file uploaded successfully");
    }
    public Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

}
