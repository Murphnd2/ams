package net.superiorstate.ams.previous.archive.activityDetail.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDoOut;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "updateTaskInfo", value = "/updateTaskInfo")
public class updateTaskInfo extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processUpdate(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processUpdate(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/activityDetail/activityDetail.jsp");
        dispatcher.forward(request,response);
    }

    private void processUpdate(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Task t = updateTask(em,request);
        if(t!=null){
            updateAutomation(request,em,t);
            SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
            sVar.refreshToDoOutList(em);
            CheckList c = sVar.getCurrentCheckList();
            List<ToDoOut> toDoOutList;
            Query q = em.createQuery("SELECT t FROM ToDoOut t WHERE t.checkList.id = :id");
            q.setParameter("id",c.getId());
            toDoOutList = (List<ToDoOut>) q.getResultList();
            for(ToDoOut tdo:toDoOutList)
                em.refresh(tdo);
            sVar.refreshToDoOutList(em);
            request.getSession().setAttribute("sVar",sVar);
        }
        em.close();
    }

    private void updateAutomation(HttpServletRequest request, EntityManager em, Task t){
        System.out.println("RUN: updateAutomation method");
        String autoName=null;
        String autoText=null;
        try{
            autoName = request.getParameter("autoName").toString().trim();
            autoText = request.getParameter("autoText").toString().trim();
        } catch (Exception e){return;}

        boolean isNotBlank = !autoName.trim().equals("") && !autoText.trim().equals("");

        if(t.isAutomated()&& t.getAutomation()!=null){ // If Task is Automated and an Automation Object Exists
            System.out.println("STEP: isAutomated & automation not null");
            // Find out if anything has changed
            boolean hasChanged = (!autoName.trim().equals(t.getAutomation().getAutomationName().trim())) || (!autoText.trim().equals(t.getAutomation().getHtmlContent()));

            System.out.println("HAS CHANGED: "+hasChanged);
            if(hasChanged) {
                if(isNotBlank)
                    updateAutomation(em,t.getAutomation(),autoName,autoText);
                else
                    removeAutomation(em,t);
            }
        }

        else if(t.isAutomated()){ //If Task is mistakenly flagged Automated but no object exists

            System.out.println("STEP: isAutomated BUT automation is null");
            Task t1 = dM.getTaskById(em,t.getId());
            em.getTransaction().begin();
            t1.setHasAutomation(false);
            t1.setAutomationText(null);
            t1.setAutomation(null);
            em.persist(t1);
            em.getTransaction().commit();
            em.refresh(t1);
            em.refresh(t);

        }

        else if(isNotBlank){ //If there was no automation, and now content was entered
            //Create an Automation;

            System.out.println("STEP: is not blank entries");
            Automation a = createAutomation(em,autoName,autoText,t);
            addAutomationToTask(em,a,autoName,t);
        }


        System.out.println("isNotBlank: "+isNotBlank);
    }

    private void removeAutomation(EntityManager em, Task t){
        //Clear Task
        Automation a = t.getAutomation();
        Task task = dM.getTaskById(em,t.getId());
        if(task==null)
            return;
        em.getTransaction().begin();
        task.setAutomation(null);
        task.setHasAutomation(false);
        task.setAutomationText(null);
        task.setServletName(null);
        em.persist(task);
        em.getTransaction().commit();
        em.refresh(task);
        //Delete Automation
        em.getTransaction().begin();
        Query q = em.createQuery("DELETE FROM Automation a WHERE a.id =:id");
        q.setParameter("id",a.getId());
        q.executeUpdate();
        em.getTransaction().commit();
    }
    private Task updateTask(EntityManager em, HttpServletRequest request){
        // Retrieve form parameters
        boolean allowEarly = true;
        try{
            allowEarly = request.getParameter("allowEarly").toString().equals("1");
        } catch (Exception ignored){        }

        boolean allowFuture = true;
        try{
            allowFuture = request.getParameter("allowFuture").toString().equals("1");
        } catch (Exception ignored){        }

        int whoOwns=0;
        try{
            whoOwns = Integer.parseInt(request.getParameter("whoOwns").toString());
        } catch (Exception ignored){        }

        Person owner=null;
        try{
            if(whoOwns>0)
                owner = dM.getPersonById(em,Long.parseLong(request.getParameter("ownerId").toString()));
        } catch (Exception ignored){       }

        int isSourced=0;
        try{
            isSourced = Integer.parseInt(request.getParameter("isSourced").toString());
        } catch (Exception ignored){      }

        Person bpo=null;
        try{
            if(isSourced>0)
                bpo = dM.getPersonById(em,Long.parseLong(request.getParameter("sourceId").toString()));
        } catch (Exception ignored){}

        boolean hasOwner = owner != null && whoOwns != 0;
        boolean hasSource = bpo != null && isSourced != 0;
        boolean allowNonOwner = whoOwns != 2 && isSourced != 2;


        WebLink goToPathLink=null;
        try{
            goToPathLink = createWebLink(em,request.getParameter("goToPath").toString());
        } catch (Exception ignored){ }

        boolean hasGoTo=false;
        try{
            hasGoTo = request.getParameter("hasGoTo").toString().equals("1") && goToPathLink!=null;
        } catch (Exception ignored){
        }

        if(!hasGoTo)
            goToPathLink = null;


        WebLink infoPathLink=null;
        try{
            infoPathLink = createWebLink(em,request.getParameter("infoPath").toString());
        } catch (Exception ignored){        }

        boolean hasInfo=false;
        try{
            hasInfo = request.getParameter("hasInfo").toString().equals("1") && infoPathLink!=null;
        } catch (Exception ignored){        }

        if(!hasInfo)
            infoPathLink = null;

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");

        Task t = dM.getTaskById(em,sVar.getCurrentToDoOut().getTask().getId());
        if(t==null)
            return null;
        em.getTransaction().begin();
        t.setAllowEarly(allowEarly);
        t.setAllowFuture(allowFuture);
        t.setHasOwner(hasOwner);
        t.setOwner(owner);
        t.setSourced(hasSource);
        t.setSourceOwner(bpo);
        t.setAllowNonOwner(allowNonOwner);
        t.setHasGoTo(hasGoTo);
        t.setGoToLink(goToPathLink);
        t.setHasInfo(hasInfo);
        t.setInfoLink(infoPathLink);
        em.persist(t);
        em.getTransaction().commit();
        em.refresh(t);
        return t;
    }
    private Automation createAutomation(EntityManager em, String title, String html, Task t){
        em.getTransaction().begin();
        Automation a = new Automation();
        a.setId(t.getId().intValue());
        em.persist(a);
        em.getTransaction().commit();

        return updateAutomation(em,a,title,html);
    }

    private Automation updateAutomation(EntityManager em, Automation a1, String name, String html){
        String newHtml = html.replace("<pre>","");
        newHtml = newHtml.replace("</pre>","");
        newHtml = newHtml.replace("<code>","");
        newHtml = newHtml.replace("</code>","");
        newHtml = newHtml.replace("&lt;","<");
        newHtml = newHtml.replace("&gt;",">");
        newHtml = newHtml.trim();
        Automation a = dM.getAutomationById(em,a1.getId());
        em.getTransaction().begin();
        assert a != null;
        a.setContent(newHtml);
        a.setAutomationName(name.trim());
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }
    private void addAutomationToTask(EntityManager em, Automation a, String newTitle, Task task){
        Task t = dM.getTaskById(em,task.getId());
        if(t==null)
            return;
        em.getTransaction().begin();
        t.setAutomationText(newTitle);
        t.setHasAutomation(true);
        t.setAutomation(a);
        String servletName = "SendAutoEmail?aeId="+a.getId();
        if(t.getServletName()==null || t.getServletName().equalsIgnoreCase(""))
            t.setServletName(servletName);
        em.persist(t);
        em.getTransaction().commit();
    }
    private WebLink createWebLink(EntityManager em, String path){
        if(!V.isValidURL(path))
            return null;
        LinkType l = dM.getLinkTypeById(em,2);
        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setActive(true);
        w.setLinkPath(path);
        w.setPlainText(path);
        w.setLinkType(l);
        em.persist(w);
        em.getTransaction().commit();
        return w;
    }



}
