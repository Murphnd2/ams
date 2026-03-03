package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.data.util.SessionVar;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.BpoRegistration;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import java.io.IOException;
import java.util.Objects;

@WebServlet(name = "UpdateTask25", value = "/UpdateTask25")
public class UpdateTask25 extends HttpServlet {

    private boolean delegationChange;

    public boolean delegationChanged() {
        return delegationChange;
    }

    public void setDelegationChange(boolean delegationChange) {
        this.delegationChange = delegationChange;
    }

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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void processUpdate(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Task t = updateTask(em,request);
        em.refresh(t);
        if(t!=null){
            updateAutomation(request,em,t);
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            for(ToDoOut25 tdo: local.getCurrentActivity().getToDoList()){
                if(Objects.equals(tdo.getTask().getId(), t.getId())){
                    tdo.setAutomation(t.getAutomation());
                    tdo.setHasAutomation(t.hasAutomation());
                    tdo.setServletName(t.getServletName());
                    tdo.setAutomationText(t.getAutomationText());
                    tdo.setAllowEarly(t.allowEarly());
                    tdo.setAllowFuture(t.allowFuture());
                    tdo.setAllowNonOwner(t.allowNonOwner());
                    tdo.setHasOwner(t.hasOwner());
                    tdo.setTaskOwner(t.getOwner());
                    tdo.setSourced(t.isSourced());
                    tdo.setBpoRegistration(t.getBpoRegistration());
                    tdo.setHasGoto(t.hasGoTo());
                    tdo.setGotoLink(t.getGoToLink());
                    tdo.setHasInfo(t.hasInfo());
                    tdo.setInfoLink(t.getInfoLink());
                    break;
                }
            }
            if(delegationChanged()){
                AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
                global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
                local.setActivitiesWithDependencies(global.getActivitiesWithDelegation());
                local.getCurrentActivity().setReFilterOnExit(true);
                request.getServletContext().setAttribute("global",global);
            }
            request.getSession().setAttribute("local",local);
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
            Task t1 = EntityLookup.getTaskById(em,t.getId());
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
        Task task = EntityLookup.getTaskById(em,t.getId());
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
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
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
                owner = EntityLookup.getPersonById(em,Long.parseLong(request.getParameter("ownerId").toString()));
        } catch (Exception ignored){       }

        int isSourced=0;
        try{
            isSourced = Integer.parseInt(request.getParameter("isSourced").toString());
        } catch (Exception ignored){      }

        BpoRegistration bpoReg=null;
        try{
            if(isSourced>0)
                bpoReg = em.find(BpoRegistration.class, Long.parseLong(request.getParameter("sourceId").toString()));
        } catch (Exception ignored){}

        boolean hasOwner = owner != null && whoOwns != 0;
        boolean hasSource = bpoReg != null && isSourced != 0;
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

        Task t = EntityLookup.getTaskById(em,local.getCurrentToDoOut().getTask().getId());

        if(t==null)
            return null;

        // Capture old sourcing state before update (for Fix 2 & 3)
        boolean wasSourced = t.isSourced();
        BpoRegistration oldBpoReg = t.getBpoRegistration();

        setDelegationChange(hasOwner!=t.hasOwner());

        em.getTransaction().begin();
        t.setAllowEarly(allowEarly);
        t.setAllowFuture(allowFuture);
        t.setHasOwner(hasOwner);
        t.setOwner(owner);
        t.setSourced(hasSource);
        t.setBpoRegistration(bpoReg);
        t.setAllowNonOwner(allowNonOwner);
        t.setHasGoTo(hasGoTo);
        t.setGoToLink(goToPathLink);
        t.setHasInfo(hasInfo);
        t.setInfoLink(infoPathLink);
        em.persist(t);
        em.getTransaction().commit();
        em.refresh(t);

        // Fix 3: Handle sourcing transitions (Internal↔Sourced, Vendor A→Vendor B)
        if (!wasSourced && hasSource) {
            // Internal → Sourced: push all incomplete ToDos to the new BPO
            BpoTaskPushService.pushTaskTodos(em, t);
        } else if (wasSourced && !hasSource && oldBpoReg != null) {
            // Sourced → Internal: recall from old BPO
            BpoTaskPushService.recallTask(em, t, oldBpoReg);
        } else if (wasSourced && hasSource && oldBpoReg != null && bpoReg != null
                && !Objects.equals(oldBpoReg.getId(), bpoReg.getId())) {
            // Vendor A → Vendor B: recall from A, push to B
            BpoTaskPushService.recallTask(em, t, oldBpoReg);
            BpoTaskPushService.pushTaskTodos(em, t);
        } else if (wasSourced && hasSource) {
            // Fix 2: Same vendor, task metadata changed — push update
            BpoTaskPushService.pushTaskUpdate(em, t);
        }

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
        Automation a = EntityLookup.getAutomationById(em,a1.getId());
        em.getTransaction().begin();
        assert a != null;
        a.setContent(newHtml);
        a.setAutomationName(name.trim());
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }
    private void addAutomationToTask(EntityManager em, Automation a, String newTitle, Task task){
        Task t = EntityLookup.getTaskById(em,task.getId());
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
        if(!Validator.isValidURL(path))
            return null;
        LinkType l = EntityLookup.getLinkTypeById(em, 2);
        if (l == null) {
            throw new IllegalStateException("LinkType ID 2 is missing from the database.");
        }
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
