package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.GenSeq;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@WebServlet(name = "TaskBuilder25", value = "/TaskBuilder25")
public class TaskBuilder25 extends HttpServlet {
    private int taskIndex;
    private RequestDispatcher dispatcher;
    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }
    public void setDispatcher(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    private String radioTikRenSet;
    private String radioTikExNew;
    private String selectTikEx;
    private String textTikNew;
    private String radioRenExNew;
    private String selectRenEx;
    private String selectRenNew;
    private String radioSetExNew;
    private String selectSetEx;
    private String selectSetNew;
    private String selectedSequenceIdString;
    private Long selectedSequenceId;

    private long categoryId;
    private String categoryIdString;


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        startHere(request,response);
        getDispatcher().forward(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        startHere(request,response);
        getDispatcher().forward(request,response);
    }

    private void setButtonVariables(HttpServletRequest request){
        radioTikRenSet = request.getParameter("activityType");
        radioTikExNew = request.getParameter("oldNewTicket");
        selectTikEx = request.getParameter("ticketSequenceList");
        textTikNew = request.getParameter("ticketDescription");
        radioRenExNew = request.getParameter("oldNewRenewal");
        selectRenEx = request.getParameter("renewalSequenceList");
        selectRenNew = request.getParameter("renewalDescription");
        radioSetExNew = request.getParameter("oldNewSetups");
        selectSetEx = request.getParameter("setupSequenceList");
        selectSetNew = request.getParameter("setupDescription");
        categoryIdString = request.getParameter("ticketCategory");
        categoryId = Long.parseLong(categoryIdString);
        request.getSession().setAttribute("tikCatId",categoryId);
        request.getSession().setAttribute("r1sel",radioTikRenSet);
        request.getSession().setAttribute("tSel",radioTikExNew);
        request.getSession().setAttribute("rSel",radioRenExNew);
        request.getSession().setAttribute("sSel",radioSetExNew);
        request.getSession().setAttribute("tikDescription",textTikNew);
        if(radioTikRenSet.equals("2") && radioSetExNew.equals("0"))
            selectedSequenceIdString = selectSetEx;
        else if(radioTikRenSet.equals("2"))
            selectedSequenceIdString = selectSetNew;
        else if(radioTikRenSet.equals("1") && radioRenExNew.equals("0"))
            selectedSequenceIdString = selectRenEx;
        else if(radioTikRenSet.equals("1"))
            selectedSequenceIdString = selectRenNew;
        else if(radioTikRenSet.equals("0") && radioTikExNew.equals("0"))
            selectedSequenceIdString = selectTikEx;
        else selectedSequenceIdString = "-1";
        selectedSequenceId = Long.parseLong(selectedSequenceIdString);

        request.getSession().setAttribute("selectedSequenceId",selectedSequenceId);
    }

    private void startHere(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setDispatcher(request.getRequestDispatcher("/WEB-INF/view/a/general/sequenceBuilder/sequenceBuilderForm.jsp"));
        setButtonVariables(request);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        saveTaskState(request,em);
        String buttonCode = request.getParameter("btnTb");
        String actionId = buttonCode.substring(0,2);
        taskIndex = -1;
        if(buttonCode.length()>3){
            try{
                taskIndex = Integer.parseInt(buttonCode.substring(3));
            } catch (Exception ignored){}
        }

        switch (actionId) {
            case "LT" -> loadSelectedTicketSequence(request, em);
            case "LR" -> loadSelectedRenewalSequence(request, em);
            case "LS" -> loadSelectedSetupSequence(request, em);
            case "UP" -> moveTaskUpOne(request, em);
            case "DN" -> moveTaskDownOne(request, em);
            case "AD" -> addTaskToSequence(request, em);
            case "DE" -> deleteTaskFromSequence(request, em);
            case "ST" -> startNewTaskSequence(request, em);
            case "NT" -> startNewTicketSequence(request,em);
            case "NR" -> startNewRenewalSequence(request,em);
            case "NS" -> startNewSetupSequence(request);
            case "CH" -> updateSequence(request,em);
        }
        request.getSession().setAttribute("availableTasks",getTaskList(em,request));
        if(actionId.charAt(0) == 'L' || actionId.charAt(0)=='N'){
            request.getSession().setAttribute("lockTemplateSelector",1);
            String lText;
            if(selectedSequenceId==-1) {
                TicketCategory tc = EntityLookup.getTicketCategoryById(em, categoryId);
                assert tc != null;
                lText = tc.getShortText()+": " + textTikNew;
            } else if(radioTikRenSet.equals("2") && radioSetExNew.equals("1"))
                lText = "(Setup) " + EntityLookup.getServiceItemById(em,selectedSequenceId.intValue()).getDescription();
            else if(radioTikRenSet.equals("1") && radioRenExNew.equals("1"))
                lText ="(Renewal) " + EntityLookup.getServiceItemById(em,selectedSequenceId.intValue()).getDescription();
            else {
                lText = EntityLookup.getReqListById(em,selectedSequenceId).getDescription();
            }

            request.getSession().setAttribute("namePlate",lText);
        }

        em.close();
        if(actionId.equals("CH")) {
            System.out.println("I DID THE CH");
            setDispatcher(request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome25.jsp"));
        } else
        {
            System.out.println("I SKIPPED IT");
        }
    }

    private void updateSequence(HttpServletRequest request,EntityManager em){
        // Create Required Task List if New
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        boolean isNewList = false;
        RequiredTaskList rtl;
        PSP psp = EntityLookup.getPspById(em,4L);
        if(radioTikRenSet.equals("0") && radioTikExNew.equals("1"))
            isNewList=true;
        else if(radioTikRenSet.equals("1") && radioRenExNew.equals("1"))
            isNewList=true;
        else if(radioTikRenSet.equals("2") && radioSetExNew.equals("1"))
            isNewList = true;
        ServiceItem tp;
        if(isNewList){
            if(radioTikRenSet.equals("0")){
                TicketCategory tc = EntityLookup.getTicketCategoryById(em,categoryId);
                ActivityCategory tg = EntityLookup.getTemplateGroupById(em,3);
                em.getTransaction().begin();
                tp = new ServiceItem();
                tp.setSortOrder(100);
                tp.setDescription(textTikNew);
                tp.setActivityCategory(tg);
                tp.setPsp(psp);
                tp.setSourceType("MANUAL");
                tp.setTicketCategory(tc);
                em.persist(tp);
                em.getTransaction().commit();

                List<ServiceItem> newList = new ArrayList<>(global.getTicketServiceItems());
                newList.add(tp);
                global.setTicketServiceItems(newList);
                request.getServletContext().setAttribute("global",global);


            } else {
                tp = EntityLookup.getServiceItemById(em,selectedSequenceId.intValue());
            }
            em.getTransaction().begin();
            rtl = new RequiredTaskList();
            rtl.setInActive(false);
            rtl.setPsp(psp);
            rtl.setServiceItem(tp);
            if(radioTikRenSet.equals("2"))
                rtl.setDescription("(Setup) "+tp.getDescription());
            else if(radioTikRenSet.equals("1"))
                rtl.setDescription("(Renewal) "+tp.getDescription());
            else rtl.setDescription(tp.getDescription());
            em.persist(rtl);
            em.getTransaction().commit();
        } else {
            rtl = EntityLookup.getReqListById(em,selectedSequenceId);
        }
        em.getTransaction().begin();
        Query q = em.createQuery("DELETE FROM TaskSequenceTable t WHERE t.taskSequence.id = :id");
        q.setParameter("id",rtl.getId());
        q.executeUpdate();
        em.getTransaction().commit();

        List<GenSeq> gsList = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        if(gsList==null || gsList.size()==0)
            return;
        List<TaskSequenceTable> tstList = new ArrayList<>();
        int sortOrder;
        for(GenSeq g: gsList){
            sortOrder = g.getSequenceNumber()*10;
            Task t;
            if(g.getTask()==null || g.getTask().getId()==null){
                em.getTransaction().begin();
                t = new Task();
                t.setPsp(psp);
                t.setDescription(g.getDescription());
                t.setReUsable(g.isPublicTask());
                t.setSourced(false);
                t.setHasOwner(false);
                t.setHasAutomation(false);
                t.setHasGoTo(false);
                t.setHasInfo(false);
                t.setAllowNonOwner(true);
                t.setAllowEarly(true);
                t.setAllowFuture(true);
                em.persist(t);
                em.getTransaction().commit();
            } else t = g.getTask();
            em.getTransaction().begin();
            TaskSequenceTable tst = new TaskSequenceTable();
            tst.setTaskSequence(rtl);
            tst.setSortOrder(sortOrder);
            tst.setTask(t);
            em.persist(tst);
            em.getTransaction().commit();
            tstList.add(tst);
        }
        em.getTransaction().begin();
        rtl.setTaskSequenceTableList(tstList);
        em.persist(rtl);
        em.getTransaction().commit();
    }


    private void doThis(HttpServletRequest request){
        request.getSession().setAttribute("showTaskBuilder","Y");
        List<GenSeq> theList = new ArrayList<>();
        GenSeq g = new GenSeq();
        g.setDescription("");
        theList.add(g);
        request.getSession().setAttribute("listBuilder",theList);
    }
    private void startNewSetupSequence(HttpServletRequest request){
        request.getSession().setAttribute("sSequenceSelected",selectedSequenceId);
        doThis(request);
    }
    private void startNewRenewalSequence(HttpServletRequest request, EntityManager em){
        request.getSession().setAttribute("rSequenceSelectedId",selectedSequenceId);
        doThis(request);
    }
    private void startNewTicketSequence(HttpServletRequest request,EntityManager em){
        request.getSession().setAttribute("tikCatId",categoryId);
        doThis(request);
    }

    private void loadSelectedSequence(HttpServletRequest request, EntityManager em, String listId){
        long listIdLong = Long.parseLong(listId);
        request.getSession().setAttribute("selectedSequenceId",listIdLong);
        request.getSession().setAttribute("showTaskBuilder","Y");
        RequiredTaskList rtl = EntityLookup.getReqListById(em,listIdLong);
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id order by tst.sortOrder");
        q.setParameter("id",rtl.getId());
        List<TaskSequenceTable> tstList;
        try{
            tstList = (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            tstList = new ArrayList<>();
        }
        List<GenSeq> theList = new ArrayList<>();
        for(int i = 0; i<tstList.size();i++){
            int seqNum = i*1000;
            TaskSequenceTable tst = tstList.get(i);
            GenSeq gs = new GenSeq();
            gs.setDescription(tst.getTask().getDescription());
            gs.setTask(tst.getTask());
            gs.setPublicTask(tst.getTask().isReUsable());
            gs.setSequenceNumber(seqNum);
            theList.add(gs);
        }
        if(theList.size()==0){
            GenSeq g = new GenSeq();
            g.setDescription("");
            theList.add(g);
        }
        request.getSession().setAttribute("listBuilder",theList);
    }
    private void loadSelectedTicketSequence(HttpServletRequest request,EntityManager em){
        loadSelectedSequence(request,em,selectedSequenceIdString);
    }
    private void loadSelectedRenewalSequence(HttpServletRequest request,EntityManager em){
        loadSelectedSequence(request,em,selectedSequenceIdString);
    }
    private void loadSelectedSetupSequence(HttpServletRequest request, EntityManager em){
        loadSelectedSequence(request,em,selectedSequenceIdString);
    }

    private void saveTaskState(HttpServletRequest request, EntityManager em) {
        List<GenSeq> currentList = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        if (currentList == null || currentList.isEmpty()) return;

        List<GenSeq> updatedList = new ArrayList<>();
        for (int x = 0; x < currentList.size(); x++) {
            GenSeq gs = currentList.get(x);
            if (gs.getTask() == null) {
                Task task = extractOrCreateTask(request, em, x);
                gs.setTask(task);
                gs.setDescription(task.getDescription());
            }
            updatedList.add(gs);
        }
        request.getSession().setAttribute("listBuilder", updatedList);
    }

    private Task extractOrCreateTask(HttpServletRequest request, EntityManager em, int index) {
        String radioParam = request.getParameter("whichRadio" + index);
        boolean isExistingTask = "1".equals(radioParam);

        if (isExistingTask) {
            String taskIdParam = request.getParameter("selTask" + index);
            if (taskIdParam != null) {
                return EntityLookup.getTaskById(em, Long.parseLong(taskIdParam));
            }
            return null; // or handle gracefully if unexpected
        }

        // Create new Task
        PSP psp = EntityLookup.getPspById(em, 4L);
        Task task = new Task();
        task.setDescription(Objects.requireNonNull(request.getParameter("taskDesc" + index)));
        task.setReUsable(request.getParameter("saveTheTask" + index) != null);
        task.setAllowFuture(true);
        task.setAllowEarly(true);
        task.setHasInfo(false);
        task.setHasOwner(false);
        task.setSourced(false);
        task.setHasGoTo(false);
        task.setHasAutomation(false);
        task.setPsp(psp);
        task.setAllowNonOwner(true);

        // Transaction handling
        boolean transactionActive = em.getTransaction().isActive();
        if (!transactionActive) em.getTransaction().begin();
        em.persist(task);
        if (!transactionActive) em.getTransaction().commit();

        return task;
    }

    private List<Task> getTaskList(EntityManager em,HttpServletRequest request){
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.reUsable = true order by t.id");
        List<Task> reUsableTasks;
        try{
            reUsableTasks = (List<Task>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }

        List<GenSeq> listBuilder = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        List<Long> tasksAlreadyPresent = new ArrayList<>();
        for(GenSeq g:listBuilder){
            if(g.getTask()!=null){
                System.out.println("Task Present: "+g.getTask().getId());
                tasksAlreadyPresent.add(g.getTask().getId());
            } else{
                System.out.println("Task Ignored Sequence @ Index " + g.getSequenceNumber());
            }
        }
        System.out.println("***********************************");
        List<Task> returnList = new ArrayList<>();
        for(Task t: reUsableTasks){
            if(tasksAlreadyPresent.contains(t.getId())){
                System.out.println("Sequence Contains " + t.getId() + " : Not Added");
            } else {
                System.out.println("Sequence DOES NOT Contain " + t.getId());
                returnList.add(t);
            }
        }

        return returnList;
    }
    private void moveTaskUp(HttpServletRequest request, EntityManager em, int start){
        List<GenSeq> startList = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        List<GenSeq> endList = new ArrayList<>();

        // STEP 1: Leave all prior in the same spot
        if(start>1){
            for(int i = 0; i < start-1; i++)
                endList.add(startList.get(i));
        }
        // STEP 2: Swap task with prior task
        int seqNumMinusOne = startList.get(start-1).getSequenceNumber();
        int seqNum = startList.get(start).getSequenceNumber();
        startList.get(start).setSequenceNumber(seqNumMinusOne);
        endList.add(startList.get(start));
        startList.get(start-1).setSequenceNumber(seqNum);
        endList.add(startList.get(start-1));

        // STEP 3: Add remainder tasks to Session List;
        if(start<startList.size()-1){
            for(int j = start+1; j < startList.size(); j++)
                endList.add(startList.get(j));
        }
        request.getSession().setAttribute("listBuilder",endList);
    }
    private void moveTaskUpOne(HttpServletRequest request, EntityManager em){
        moveTaskUp(request,em,taskIndex);
    }
    private void moveTaskDownOne(HttpServletRequest request, EntityManager em){
        moveTaskUp(request,em,taskIndex+1);
    }
    private void addTaskToSequence(HttpServletRequest request, EntityManager em){
        List<GenSeq> startList = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        List<GenSeq> endList = new ArrayList<>();

        for(int i = 0; i < taskIndex+1;i++)
            endList.add(startList.get(i));

        int newSequenceNumber;
        if(taskIndex < startList.size()-1){
            newSequenceNumber = (int) Math.round((startList.get(taskIndex).getSequenceNumber() + startList.get(taskIndex+1).getSequenceNumber())/2.0);
        } else newSequenceNumber = startList.get(taskIndex).getSequenceNumber()+1000;

        GenSeq gs = new GenSeq();
        gs.setPublicTask(false);
        gs.setSequenceNumber(newSequenceNumber);
        gs.setDescription("");
        endList.add(gs);

        for(int j = taskIndex + 1; j < startList.size(); j++)
            endList.add(startList.get(j));

        request.getSession().setAttribute("listBuilder",endList);
    }
    private void deleteTaskFromSequence(HttpServletRequest request, EntityManager em){
        List<GenSeq> startList = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        List<GenSeq> endList = new ArrayList<>();
        for(int i = 0; i < startList.size();i++){
            if(i!=taskIndex)
                endList.add(startList.get(i));
        }
        for(int j = 0; j < endList.size();j++)
            endList.get(j).setSequenceNumber(j*1000);
        request.getSession().setAttribute("listBuilder",endList);
    }
    private void startNewTaskSequence(HttpServletRequest request, EntityManager em){
        List<GenSeq> theList = new ArrayList<>();
        String isNew = request.getParameter("whichRadio");
        GenSeq gs = new GenSeq();
        if(isNew.equals("0")){
            String taskName = request.getParameter("taskDescI");
            boolean saveTask = false;
            String st = request.getParameter("saveTask1");
            if(st!=null && st.equals("1"))
                saveTask = true;
            gs.setSequenceNumber(0);
            gs.setPublicTask(saveTask);
            gs.setDescription(taskName);
        } else {
            String taskId = request.getParameter("selTaskI");
            Long taskIdLong = Long.parseLong(taskId);
            Task t = EntityLookup.getTaskById(em,taskIdLong);
            gs.setDescription(t.getDescription());
            gs.setPublicTask(t.isReUsable());
            gs.setSequenceNumber(1);
            gs.setTask(t);
        }
        theList.add(gs);
        request.getSession().setAttribute("listBuilder",theList);
    }
}
