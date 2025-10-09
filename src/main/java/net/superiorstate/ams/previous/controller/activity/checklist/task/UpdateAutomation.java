package net.superiorstate.ams.previous.controller.activity.checklist.task;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;

@WebServlet(name = "UpdateAutomation", value = "/UpdateAutomation")
public class UpdateAutomation extends HttpServlet {
    private boolean allowEarly;
    private boolean allowFuture;
    private boolean allowNonOwner;
    private boolean hasOwner;
    private boolean isSourced;
    private boolean hasGoTo;
    private boolean hasInfo;
    private String goToPath;
    private String infoPath;
    private long ownerId;
    private long bpoId;

    private long toDoId;

    public boolean isAllowEarly() {
        return allowEarly;
    }

    public void setAllowEarly(boolean allowEarly) {
        this.allowEarly = allowEarly;
    }

    public boolean isAllowFuture() {
        return allowFuture;
    }

    public void setAllowFuture(boolean allowFuture) {
        this.allowFuture = allowFuture;
    }

    public boolean isAllowNonOwner() {
        return allowNonOwner;
    }

    public void setAllowNonOwner(boolean allowNonOwner) {
        this.allowNonOwner = allowNonOwner;
    }

    public boolean isHasOwner() {
        return hasOwner;
    }

    public void setHasOwner(boolean hasOwner) {
        this.hasOwner = hasOwner;
    }

    public boolean isSourced() {
        return isSourced;
    }

    public void setSourced(boolean sourced) {
        isSourced = sourced;
    }

    public boolean isHasGoTo() {
        return hasGoTo;
    }

    public void setHasGoTo(boolean hasGoTo) {
        this.hasGoTo = hasGoTo;
    }

    public boolean isHasInfo() {
        return hasInfo;
    }

    public void setHasInfo(boolean hasInfo) {
        this.hasInfo = hasInfo;
    }

    public String getGoToPath() {
        return goToPath;
    }

    public void setGoToPath(String goToPath) {
        this.goToPath = goToPath;
    }

    public String getInfoPath() {
        return infoPath;
    }

    public void setInfoPath(String infoPath) {
        this.infoPath = infoPath;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public long getBpoId() {
        return bpoId;
    }

    public void setBpoId(long bpoId) {
        this.bpoId = bpoId;
    }

    public long getToDoId() {
        return toDoId;
    }

    public void setToDoId(long toDoId) {
        this.toDoId = toDoId;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String upEmail;
        try{
            upEmail = request.getParameter("btnUpEmail");
        } catch (Exception e){
            upEmail = "0";
        }
        if(upEmail!=null && upEmail.equals("upEmail"))
            processAutomatedEmailUpdate(request,response);
        else
            processTaskStandardUpdate(request, response);

    }

    private void processTaskStandardUpdate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String cbEarly = request.getParameter("allowEarly");
        setAllowEarly(true);
        if(cbEarly==null || cbEarly.equals("0"))
            setAllowEarly(false);

        String cbFuture = request.getParameter("allowFuture");
        setAllowFuture(true);
        if(cbFuture==null || cbFuture.equals("0"))
            setAllowFuture(false);

        String toggleWhoOwns = request.getParameter("whoOwns");
        String toggleIsSourced = request.getParameter("isSourced");

        setHasOwner(true);
        if(toggleWhoOwns.equals("0"))
            setHasOwner(false);

        String ddOwner = request.getParameter("ownerId");
        if(ddOwner!=null)
            setOwnerId(Long.parseLong(ddOwner));


        setSourced(true);
        if(toggleIsSourced.equals("0"))
            setSourced(false);

        String ddSource = request.getParameter("sourceId");
        if(ddSource!=null)
            setBpoId(Long.parseLong(ddSource));


        setAllowNonOwner(true);
        if(toggleWhoOwns.equals("2") || toggleIsSourced.equals("2"))
            setAllowNonOwner(false);

        String pathGoTo = request.getParameter("goToPath");
        setGoToPath(null);
        if(pathGoTo!=null)
            setGoToPath(pathGoTo);

        String cbHasGoTo = request.getParameter("hasGoTo");
        setHasGoTo(true);
        if(cbHasGoTo==null || getGoToPath()==null || cbHasGoTo.equals("0"))
            setHasGoTo(false);

        String pathInfo = request.getParameter("infoPath");
        setInfoPath(null);
        if(pathInfo!=null)
            setInfoPath(pathInfo);

        String cbHasInfo = request.getParameter("hasInfo");
        setHasInfo(true);
        if(cbHasInfo == null || getInfoPath()==null || cbHasInfo.equals("0"))
            setHasInfo(false);



        String toDoIdString = request.getParameter("btnAuto1");
        setToDoId(Long.parseLong(toDoIdString));

        System.out.println("EARLY: " + cbEarly + ":" + isAllowEarly());
        System.out.println("FUTURE: " + cbFuture + ":" + isAllowFuture());
        System.out.println("HAS OWNER: " + toggleWhoOwns + ":" + isHasOwner());
        System.out.println("DD OWNER: " + ddOwner + ":" + getOwnerId());
        System.out.println("ALLOW NON OWNER: " + isAllowNonOwner());
        System.out.println("IS SOURCED: " + toggleIsSourced + ":" + isSourced());
        System.out.println("DD SOURCE: " + ddSource + ":" + getOwnerId());
        System.out.println("HAS GOTO: " + cbHasGoTo + ":" + isHasGoTo());
        System.out.println("GOTO PATH: " + pathGoTo  + ":" + getGoToPath());
        System.out.println("HAS INFO: " + cbHasInfo + ":" + isHasInfo());
        System.out.println("INFO PATH: " + pathInfo + ":" + getInfoPath());
        System.out.println("TODO ID: " + toDoIdString);

        updateTask(request);
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private Automation createAutomation(EntityManager em, String title, String html, Task t){
        em.getTransaction().begin();
        Automation a = new Automation();
        a.setId(t.getId().intValue());
        a.setContent(html);
        a.setAutomationName(title);
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }

    private void processAutomatedEmailUpdate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String newHtml = request.getParameter("autoText");
        String newTitle = request.getParameter("autoName");
        Long taskId = Long.parseLong(request.getParameter("taskId"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Task t = dM.getTaskById(em,taskId);
        Automation a;
        if(t.getAutomation()==null)
            a = createAutomation(em,newTitle,newHtml,t);
        else
            a = dM.getAutomationById(em,t.getAutomation().getId());

        newHtml = newHtml.replace("<pre>","");
        newHtml = newHtml.replace("</pre>","");
        newHtml = newHtml.replace("<code>","");
        newHtml = newHtml.replace("</code>","");
        newHtml = newHtml.replace("&lt;","<");
        newHtml = newHtml.replace("&gt;",">");
        newHtml = newHtml.trim();

        em.getTransaction().begin();
        if(newTitle!=null) {
            a.setAutomationName(newTitle);
        }
        a.setContent(newHtml);
        em.persist(a);
        em.getTransaction().commit();

        em.getTransaction().begin();
        t.setAutomationText(newTitle);
        t.setHasAutomation(true);
        t.setAutomation(a);
        String servletName = "SendAutoEmail?aeId="+a.getId();
        if(t.getServletName()==null || t.getServletName().equalsIgnoreCase(""))
            t.setServletName(servletName);
        em.persist(t);
        em.getTransaction().commit();
        em.close();
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void updateTask(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        ToDo toDo = dM.getToDoById(em,getToDoId());
        Task task = dM.getTaskById(em,toDo.getTask().getId());
        System.out.println("TASK ID: " + task.getId());

        //Allowances
        em.getTransaction().begin();
        task.setAllowEarly(allowEarly);
        task.setAllowFuture(allowFuture);
        task.setHasOwner(hasOwner);
        task.setSourced(isSourced);
        if(!isSourced)
            task.setSourceOwner(null);
        em.persist(task);
        em.getTransaction().commit();


        if(hasOwner || isSourced){
            Person owner;
            if(hasOwner)
                owner = dM.getPersonById(em,ownerId);
            else
                owner = dM.getPersonById(em,bpoId);

            em.getTransaction().begin();
            task.setOwner(owner);
            task.setAllowNonOwner(allowNonOwner);
        } else {
            em.getTransaction().begin();
            task.setOwner(null);
            task.setAllowNonOwner(true);
        }
        em.persist(task);
        em.getTransaction().commit();

        WebLink webLink = null;
        if(hasGoTo && getGoToPath()!=null && (task.getGoToLink()==null || task.getGoToLink().getLinkPath()==null || !task.getGoToLink().getLinkPath().equalsIgnoreCase(getGoToPath())))
            webLink = getWebLink(getGoToPath(),em,task);
        else if(hasGoTo & task.getGoToLink()!=null)
            webLink = task.getGoToLink();
        else
            System.out.println("Path Didn't Run");
        em.getTransaction().begin();
        task.setHasGoTo(hasGoTo);
        task.setGoToLink(webLink);
        em.persist(task);
        em.getTransaction().commit();

        webLink = null;
        if(hasInfo && getInfoPath()!=null && (task.getInfoLink()==null || task.getInfoLink().getLinkPath()==null || !task.getInfoLink().getLinkPath().equalsIgnoreCase(getInfoPath())))
            webLink = getWebLink(getInfoPath(),em,task);
        else if(hasInfo && task.getInfoLink()!=null)
            webLink = task.getInfoLink();
        else
            System.out.println("Info Didn't Run");
        em.getTransaction().begin();
        task.setHasInfo(hasInfo);
        task.setInfoLink(webLink);
        em.persist(task);
        em.getTransaction().commit();
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        ViewSelectedActivity.setActivityView(request,em,a);

        em.close();
    }

    private WebLink getWebLink(String path, EntityManager em, Task t){
        System.out.println("Passed Path: " + path);

        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setLinkType(dM.getLinkTypeById(em,2));
        w.setLinkPath(path.trim());
        w.setPlainText("GoTo for Task: " + t.getDescription());
        w.setActive(false);
        em.persist(w);
        em.getTransaction().commit();
        return w;
    }

}
