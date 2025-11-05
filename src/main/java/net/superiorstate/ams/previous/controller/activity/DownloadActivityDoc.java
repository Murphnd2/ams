package net.superiorstate.ams.previous.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbA;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@WebServlet(name = "DownloadActivityDoc", value = "/DownloadActivityDoc")
public class DownloadActivityDoc extends HttpServlet {
    private final int ARBITRARY_SIZE = 1048;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileId = request.getParameter("fileId");
        WebLink w = getWebLinkByGuid(fileId);
        String docGuid = w.getLinkPath();
        digOceanDoGet(request,response,docGuid, dbA.getSavePath(request),w.getPlainText());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private WebLink getWebLinkByGuid(String guid){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.linkPath = :id");
        q.setParameter("id",guid);
        WebLink webLink;
        try{
            webLink = (WebLink) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        em.close();
        return webLink;
    }

    private void digOceanDoGet(HttpServletRequest request, HttpServletResponse response, String fileName, String filePath, String description) throws ServletException, IOException {

        if(fileName == null || fileName.equals("")){
            throw new ServletException("File Name can't be null or empty");
        }
        System.out.println("File Path = " +filePath + fileName);
        File file = new File(filePath+fileName);
        if(!file.exists()){
            throw new ServletException("File doesn't exists on server.");
        }
        String useName;
        if(hasExtension(description)){
            useName = description.replaceAll(" ","_");
        } else {
            useName = description.replaceAll(" ","_") + getFileExtension(fileName);
        }
        System.out.println("File location on server::"+file.getAbsolutePath());
        ServletContext ctx = getServletContext();
        InputStream fis = new FileInputStream(file);
        String mimeType = ctx.getMimeType(file.getAbsolutePath());
        response.setContentType(mimeType != null? mimeType:"application/octet-stream");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + useName + "\"");

        ServletOutputStream os = response.getOutputStream();
        byte[] bufferData = new byte[1024];
        int read=0;
        while((read = fis.read(bufferData))!= -1){
            os.write(bufferData, 0, read);
        }
        os.flush();
        os.close();
        fis.close();
        System.out.println("File downloaded at client successfully");
    }
    private String getFileExtension(String fileName){
        String trimName = fileName.trim();
        int theDot = trimName.indexOf('.');
        return trimName.substring(theDot);
    }

    private boolean hasExtension(String fileName){
        int theDot = -1;
        String trimName = fileName.trim();
        try{
            theDot = trimName.indexOf('.');
        } catch (Exception e){
            return false;
        }
        if(theDot>-1)
            return true;
        return false;
    }
}
