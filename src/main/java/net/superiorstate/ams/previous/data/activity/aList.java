package net.superiorstate.ams.previous.data.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.ActivityOut;
import net.superiorstate.ams.previous.model.activity.ActivityShell;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.CheckListOut;
import net.superiorstate.ams.previous.model.activity.checklist.CheckListShell;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.general.Person;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class aList {

    public static List<Activity> getFullList(EntityManager em){
        Query q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.isComplete = false order by a.fullName");
        return (List<Activity>) q.getResultList();
    }

    private static List<CheckListOut> getMyCheckLists(EntityManager em, Person p, int viewCode){
        System.out.println("Person ID: "+p.getId());
        Query q;
        if(viewCode==0)
            q = em.createQuery("SELECT c FROM CheckListOut c WHERE c.assignedTo.id = :id OR (c.delegatedEmployee is not null AND c.delegatedEmployee.id = :id)");
        else if(viewCode==1)
            q = em.createQuery("SELECT c FROM CheckListOut c WHERE c.assignedTo.id = :id");
        else q = em.createQuery("SELECT c FROM CheckListOut c WHERE c.delegatedEmployee.id = :id");
        q.setParameter("id",p.getId());
        List<CheckListOut> checkListOutList;
        try{
            checkListOutList = (List<CheckListOut>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
        return checkListOutList;
    }

    public static List<CheckListOut> getAllMyCheckLists(EntityManager em, Person p){
        return getMyCheckLists(em,p,0);
    }
    public static List<CheckListOut> getMyDelegatedLists(EntityManager em, Person p){
        return getMyCheckLists(em,p,2);
    }
    public static List<CheckListOut> getJustMyChecklists(EntityManager em, Person p){
        return getMyCheckLists(em,p,1);
    }

    public static void setMyCurrentItems(EntityManager em, Person p, HttpServletRequest request){
        List<CheckListShell> allItems = getMyOpenItems(em,p);
        System.out.println(allItems.size()+" is all");
        List<CheckListShell> curItems = new ArrayList<>();
        List<CheckListShell> futItems = new ArrayList<>();
        LocalDate today = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth()).plusDays(1L);
        for(CheckListShell cs:allItems){
            if(cs.getShowDate().toLocalDate().isBefore(today))
                curItems.add(cs);
            else futItems.add(cs);
        }
        request.getSession().setAttribute("myCurrentItems",curItems);
        request.getSession().setAttribute("myFutureItems",futItems);
    }

    public static void getMyListsClosedToday(EntityManager em, Person p, HttpServletRequest request){
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.dateCompleted = :date AND c.assignedTo.id = :id order by c.fullName");
        Date theDate = Date.valueOf(LocalDate.of(LocalDate.now().getYear(),LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth()));
        System.out.println("Date: " + theDate);
        q.setParameter("date", theDate);
        q.setParameter("id",p.getId());
        List<CheckList> checkLists;
        try{
            checkLists = (List<CheckList>) q.getResultList();
        } catch (NoResultException e){
            request.getSession().setAttribute("myClosedLists",new ArrayList<>());
            return;
        }
        System.out.println("Lines: " + checkLists.size());
        for(CheckList c:checkLists)
            em.refresh(c);
        request.getSession().setAttribute("myClosedLists",checkLists);
    }

    public static List<CheckListShell> getMyOpenItems(EntityManager em, Person p){
        List<CheckListOut> theList = getAllMyCheckLists(em,p);
        System.out.println("The List is: " + theList.size());
        List<CheckList> crossCheck = new ArrayList<>();
        List<CheckListShell> myList = new ArrayList<>();
        if(theList==null || theList.size()==0)
            return new ArrayList<>();
        for(CheckListOut c:theList){
            em.refresh(c);
            CheckListShell cs = new CheckListShell();
            cs.setId(c.getCheckList().getId());
            cs.setName(c.getName());
            if(c.getDelegatedEmployee()!=null)
                cs.setDelegate(c.getDelegatedEmployee());
            cs.setDueDate(c.getDueDate());
            cs.setShowDate(c.getShowDate());
            cs.setOwner(c.getAssignedTo());
            if(c.getBpoEmployee()!=null)
                cs.setBpoUser(c.getBpoEmployee());
            cs.setToDoCount(c.getToDoCount());
            cs.setHasDelegate(false);
            if(!Objects.equals(c.getAssignedTo().getId(), p.getId()))
                if((c.hasOwner() || c.isSourced()) && c.getDelegatedEmployee()!=null && Objects.equals(c.getDelegatedEmployee().getId(), p.getId()))
                    cs.setHasDelegate(true);
            if(!crossCheck.contains(c.getCheckList())){
                myList.add(cs);
                crossCheck.add(c.getCheckList());
            }
        }
        return myList;
    }

    public static String buildSubmitButton(String id, String value, String longText, String shortText, String iconName, String color, boolean outline, boolean mouseover){
        String button;
        button = "<button type=\"submit\" id=\""+id+"\" value=\""+value+"\" class=\"btn btn-";
        if(outline)
            button +="outline-";
        button +="color";
        if(!mouseover)
            button +=" pe-none";
        button +="\">";
        button +="<span class=\"d-none d-lg-line\">"+longText+"</span>";
        button +="<span class=\"d-lg-none\">" + shortText +"</span></button>";
        return button;
    }

    public static List<ActivityShell> getFinalList(EntityManager em, Person p, boolean sort, boolean alpha, int whoFilter, int needsWorkFilter, int dtypeFilter){
        List<ActivityShell> unfilteredList = getTheList(em,p,sort,alpha,whoFilter,needsWorkFilter);
        List<ActivityShell> filteredList = new ArrayList<>();
        System.out.println("Size: "+ unfilteredList.size());
        for(ActivityShell a: unfilteredList){
            System.out.println(a.getDtype());
            if(dtypeFilter==9){
                filteredList.add(a);
            } else if(dtypeFilter==8){
                if(a.getDtype().equals("Setup") || a.getDtype().equals("Ticket"))
                    filteredList.add(a);
            } else if(dtypeFilter==6) {
                if(a.getDtype().equals("Renewal") || a.getDtype().equals("Ticket"))
                    filteredList.add(a);
            } else if(dtypeFilter==5){
                if(a.getDtype().equals("Ticket"))
                    filteredList.add(a);
            } else if(dtypeFilter==4){
                if(a.getDtype().equals("Renewal")||a.getDtype().equals("Setup"))
                    filteredList.add(a);
            } else if(dtypeFilter==3){
                if(a.getDtype().equals("Setup"))
                    filteredList.add(a);
            } else if(dtypeFilter==1){
                if(a.getDtype().equals("Renewal"))
                    filteredList.add(a);
            }
        }

        return filteredList;
    }

    private static List<ActivityShell> getTheList(EntityManager em, Person p, boolean sort, boolean alpha, int whoFilter, int whatFilter){
        List<ActivityShell> unfilteredList = getActivityList(em,p,sort,alpha,whoFilter);
        List<ActivityShell> filteredList = new ArrayList<>();
        for(ActivityShell a:unfilteredList){
            switch(whatFilter){
                case 1:
                    if(a.isNeedingFollowUp() || a.isWaitingOnUs())
                        filteredList.add(a);
                    break;
                case 2:
                    if(a.isNeedingFollowUp())
                        filteredList.add(a);
                    break;
                case 3:
                    if(a.isWaitingOnUs())
                        filteredList.add(a);
                    break;
                default:
                    filteredList.add(a);
                    break;
            }
        }
        return filteredList;
    }

    private static List<ActivityShell> getActivityList(EntityManager em, Person p, boolean sortAscending, boolean alphabetical, int filter){
        List<ActivityOut> activityList;
        if(filter == 1)
            activityList = getAllMyActivities(em,p,sortAscending,alphabetical);
        else if(filter == 2)
            activityList = getMyActivities(em,p,sortAscending,alphabetical);
        else if(filter == 3)
            activityList = getActivitiesWhereTasked(em,p,sortAscending,alphabetical);
        else
            activityList = getAllActivities(em,sortAscending,alphabetical);

        if(activityList!=null && activityList.size()>0){
            List<Activity> aList = new ArrayList<>();
            List<ActivityShell> asList = new ArrayList<>();
            for(ActivityOut a: activityList){
                em.refresh(a);
                Activity activity = dM.getActivityById(em,a.getActivity().getId());
                if(!aList.contains(activity)){
                    ActivityShell as = new ActivityShell();
                    as.setActivityId(a.getActivity().getId());
                    as.setDtype(a.getActivity().getClass().getSimpleName());
                    as.setFullName(a.getFullName());
                    as.setContactLevel(a.getContactStatus());
                    as.setWaitingOnUs(a.isWaitingOnUs());
                    as.setNeedingFollowUp(a.isNeedingContact());
                    as.setDueDate(a.getDueDate());
                    if(a.getAssignedTo()!=null && a.getAssignedTo().getId()!=null && p.getId()!=null && Objects.equals(a.getAssignedTo().getId(), p.getId()))
                        as.setOwnershipLevel(1);
                    else if(a.getTaskOwner()!=null && a.getTaskOwner().getId()!=null && p.getId()!=null && Objects.equals(a.getTaskOwner().getId(), p.getId()))
                        as.setOwnershipLevel(2);
                    else if(a.getSourceOwner()!=null && a.getSourceOwner().getId()!=null && p.getId()!=null && Objects.equals(a.getSourceOwner().getId(), p.getId()))
                        as.setOwnershipLevel(2);
                    else
                        as.setOwnershipLevel(3);
                    asList.add(as);
                    aList.add(activity);
                }
            }
            return asList;
        } else return new ArrayList<>();
    }

    private static List<ActivityOut> getAllActivities(EntityManager em, boolean sort, boolean alpha){
        Query q;
        if(sort && alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a ORDER BY a.fullName");
        else if(alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a ORDER BY a.fullName DESC");
        else if(sort)
            q = em.createQuery("SELECT a FROM ActivityOut a ORDER BY a.dueDate");
        else
            q = em.createQuery("SELECT a FROM ActivityOut a ORDER BY a.dueDate DESC");
        List<ActivityOut> activityOutList;
        try{
            activityOutList = (List<ActivityOut>) q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
        return activityOutList;
    }

    private static List<ActivityOut> getActivitiesWhereTasked(EntityManager em, Person p, boolean sort, boolean alpha){
        Query q;
        if(sort && alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.fullName");
        else if(alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.fullName DESC");
        else if(sort)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.dueDate");
        else
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.dueDate DESC");
        q.setParameter("id",p.getId());
        List<ActivityOut> activityOutList;
        try{
            activityOutList = (List<ActivityOut>) q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
        return activityOutList;
    }

    private static List<ActivityOut> getMyActivities(EntityManager em, Person p, boolean sort, boolean alpha){
        Query q;
        if(sort && alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id ORDER BY a.fullName");
        else if(alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id ORDER BY a.fullName DESC");
        else if(sort)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id ORDER BY a.dueDate");
        else
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id ORDER BY a.dueDate DESC");
        q.setParameter("id",p.getId());
        List<ActivityOut> activityOutList;
        try{
            activityOutList = (List<ActivityOut>) q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
        return activityOutList;
    }

    private static List<ActivityOut> getAllMyActivities(EntityManager em, Person p, boolean sort, boolean alpha){
        Query q;
        if(sort && alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id OR a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.fullName");
        else if(alpha)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id OR a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.fullName DESC");
        else if(sort)
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id OR a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.dueDate");
        else
            q = em.createQuery("SELECT a FROM ActivityOut a WHERE a.assignedTo.id = :id OR a.taskOwner.id = :id OR a.sourceOwner.id = :id ORDER BY a.dueDate DESC");
        q.setParameter("id",p.getId());
        List<ActivityOut> activityOutList;
        try{
            activityOutList = (List<ActivityOut>) q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
        return activityOutList;
    }

    private static boolean ifNotBlocked(EntityManager em, ActivityShell as, Person p){
        Activity a = dM.getActivityById(em,as.getActivityId());
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.assignedTo.id = :id and t.isComplete=false order by t.sortOrder");
        q.setParameter("id",a.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return false;
        }
        if(toDoList.size()==0)
            return false;
        int topSort = toDoList.get(0).getSortOrder();
        for(ToDo t:toDoList){
            if(t.getTask().hasOwner() && t.getTask().getOwner()!=null && Objects.equals(t.getTask().getOwner().getId(), p.getId())){
                if(t.getTask().allowEarly() || t.getSortOrder() <= topSort)
                    return true;
            } else if(t.getTask().isSourced() && t.getTask().getSourceOwner()!=null && Objects.equals(t.getTask().getSourceOwner().getId(), p.getId()))
                if(t.getTask().allowEarly() || t.getSortOrder() <= topSort)
                    return true;
            if(!t.getTask().allowFuture())
                break;
        }
        return false;
    }
    private static List<ActivityShell> getOpenActivityShells1(EntityManager em, Person p, int whoFilter, boolean sortDesc, boolean alpha){
        List<ToDo> toDoList = getOpenActivitiesByTodo(em,p,whoFilter,sortDesc,alpha);
        List<ActivityShell> activityShells = new ArrayList<>();
        long lastActivityId = -1;
        long currentActivityId = -1;
        boolean isOwner = false;
        boolean isDelegate = false;
        boolean activityChanged = false;
        for(int i = 0; i < toDoList.size(); i++){
            Activity a = (Activity) toDoList.get(i).getCheckList().getAssignedTo();
            ToDo t = toDoList.get(i);
            // Do We Have a Change in Activity Id??
            if(a.getId()!=currentActivityId && i==0) {
                currentActivityId = a.getId();
                System.out.println("NEW ACTIVITY ID: " + currentActivityId + " ------------------------------------------------------");
            }
            else if(a.getId()!=currentActivityId){
                activityChanged = true;
                lastActivityId = toDoList.get(i-1).getCheckList().getAssignedTo().getId();
                currentActivityId = a.getId();
            } else activityChanged = false;

            // Process Previous Activity Data on Activity Change
            if(activityChanged){
                ToDo lt = toDoList.get(i-1);
                ActivityShell as = getActivityShell(em,lastActivityId,isOwner,isDelegate,lt);
                boolean notBlocked = ifNotBlocked(em,as,p);
                if(whoFilter==0 && as.getOwnershipLevel()==2 && !notBlocked)
                    as.setOwnershipLevel(3);
                if(whoFilter==0 || whoFilter==2 || !isDelegate || notBlocked)
                    activityShells.add(as);
                isOwner = false;
                isDelegate = false;
                System.out.println("NEW ACTIVITY ID: " + currentActivityId + " ------------------------------------------------------");
            }
            System.out.println("....TODO ID: "+t.getId() + " .. Activity ID: " + a.getId() +" " );
            // Process Current Activity Queries
            if(isOwner)
                continue;
            else if(Objects.equals(a.getAssignedTo().getId(), p.getId())) {
                isOwner = true;
                isDelegate = false;
            } else if(t.isComplete())
                continue;
            else if(t.getTask().hasOwner() && t.getTask().getOwner()!=null && Objects.equals(t.getTask().getOwner().getId(), p.getId()))
                isDelegate = true;
            else if (t.getTask().isSourced() && t.getTask().getSourceOwner()!=null && Objects.equals(t.getTask().getSourceOwner().getId(), p.getId()))
                isDelegate = true;
        }
        if(toDoList.size()>0){
            ActivityShell asf = getActivityShell(em,currentActivityId,isOwner,isDelegate,toDoList.get(toDoList.size()-1));
            boolean nb = ifNotBlocked(em,asf,p);
            if(whoFilter==0 && asf.getOwnershipLevel()==2 && !nb)
                asf.setOwnershipLevel(3);
            if(whoFilter==0 || whoFilter==2 || !isDelegate || nb)
                activityShells.add(asf);
        }
        return  activityShells;
    }

    public static List<ActivityShell> getActivityShellList1(EntityManager em, Person p, int whoFilter, boolean sortDesc, boolean alpha, int nwf, int dtype){
        List<ActivityShell> starterList = getOpenActivityShells1(em,p,whoFilter,sortDesc,alpha);
        List<ActivityShell> sendList = new ArrayList<>();
        System.out.println("NWF: " + nwf +" --- dtype: "+dtype);
        for(ActivityShell a:starterList){
            System.out.println("Activity "+a.getFullName()+" ***********************************************************");
            if(shouldAddActivity(a,dtype) && shouldAddUrgent(a,nwf))
                sendList.add(a);
        }
        System.out.println("Activity Shells 2: " + sendList.size());
        return sendList;
    }
    private static boolean shouldAddUrgent(ActivityShell a, int nwf){
        if(nwf==0)
            return true;
        boolean shouldAdd = false;
        if(nwf==1 && (a.isWaitingOnUs() || a.isNeedingFollowUp()))
            shouldAdd = true;
        if(nwf==2 && a.isNeedingFollowUp())
            shouldAdd = true;
        if(nwf==3 && a.isWaitingOnUs())
            shouldAdd = true;
        return shouldAdd;
    }
    private static boolean shouldAddActivity(ActivityShell a, int dtype){

        if(dtype==0)
            return false;
        if(dtype == 9)
            return true;
        boolean shouldSend = false;
        if((dtype==1 || dtype==4 || dtype==6)&& a.getDtype().equals("Renewal"))
            shouldSend = true;
        if((dtype==3 || dtype==4 || dtype==8)&& a.getDtype().equals("Setup"))
            shouldSend = true;
        if((dtype==5 || dtype==6 || dtype==8)&& a.getDtype().equals("Ticket"))
            shouldSend = true;
        return shouldSend;
    }

    private static boolean getWaitingOnUs(EntityManager em, Activity a){
        Query q = em.createQuery("SELECT n FROM Note n INNER JOIN Activity a ON a.id = n.activity.id WHERE a.id = :id AND (n.status.id = 3 OR n.status.id = 1) order by n.id desc ");
        q.setParameter("id",a.getId());
        List<Note> noteList;
        try{
            noteList = (List<Note>) q.getResultList();
        } catch (NoResultException e){
            System.out.println("ACTIVITY ID: " + a.getId() + " no status notes--------------------");
            return true;
        }
        if(noteList==null || noteList.size()==0){
            System.out.println("ACTIVITY ID: " + a.getId() + " no status notes--------------------");
            return  true;
        }
        else if(noteList.get(0).getStatus().getId()==1) {
            System.out.println("ACTIVITY ID: " + a.getId() + " status is 1 AND note is " + noteList.get(0).getId() + " -------------------");
            return false;
        } else {
            System.out.println("ACTIVITY ID: " + a.getId() + " status is 3 AND note is " + noteList.get(0).getId() + " -------------------");
            return true;
        }
    }

    private static Date getLastContactDate(EntityManager em, Activity a){
        Query q = em.createQuery("SELECT n FROM Note n INNER JOIN Activity a ON a.id = n.activity.id WHERE a.id = :id AND n.reasonCreated.outbound = true ORDER BY n.id desc ");
        q.setParameter("id",a.getId());
        List<Note> noteList;
        try{
            noteList = (List<Note>) q.getResultList();
        } catch (NoResultException e){
            System.out.println("ACTIVITY ID: " + a.getId() + " no outbound notes--------------------");
            return null;
        }
        if(noteList==null || noteList.size()==0){
            System.out.println("ACTIVITY ID: " + a.getId() + " no outbound notes--------------------");
            return null;
        } else{
            System.out.println("ACTIVITY ID: " + a.getId() + " OUTBOUND AND note is " + noteList.get(0).getId() + " -------------------");
            return noteList.get(0).getDateGenerated();
        }
    }

    private static boolean getNeedsContact(EntityManager em, Activity a){
        Date lastDate = getLastContactDate(em,a);
        if(lastDate==null)
            return true;
        LocalDate today = LocalDate.now();
        LocalDate lastDateLocal = lastDate.toLocalDate();
        if(lastDateLocal.isAfter(today.minusDays(7L)))
            return false;
        return true;
    }
    private static ActivityShell getActivityShell(EntityManager em, long id, boolean isOwner, boolean isDelegate, ToDo t){
        Activity a = (Activity) t.getCheckList().getAssignedTo();
        ActivityShell as = new ActivityShell();
        as.setActivityId(id);
        as.setFullName(a.getFullName());
        as.setDtype(a.getClass().getSimpleName());
        as.setDueDate(a.getDueDate());
        if(isOwner)
            as.setOwnershipLevel(1);
        else if(isDelegate)
            as.setOwnershipLevel(2);
        else as.setOwnershipLevel(3);
        as.setWaitingOnUs(getWaitingOnUs(em,a));
        as.setNeedingFollowUp(getNeedsContact(em,a));
        as.setContactLevel(getContactLevel(em,a));
        return as;
    }

    private static int getContactLevel(EntityManager em, Activity a){
        Date lastContact = getLastContactDate(em,a);
        if(lastContact==null)
            return 2;
        LocalDate today = LocalDate.now();
        LocalDate lcDate = lastContact.toLocalDate();
        if(lcDate.isAfter(today.minusDays(7L)))
            return 0;
        else if(lcDate.isBefore(today.minusDays(14L)))
            return 2;
        else return 1;
    }



    private static List<ToDo> getOpenActivitiesByTodo(EntityManager em, Person p, int whoFilter, boolean sort, boolean alpha){
        Query q = getToDoQuery(em,whoFilter,sort,alpha);
        if(whoFilter!=0)
            q.setParameter("id",p.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        System.out.println("TODO COUNT: " + toDoList.size());
        List<ToDo> tList = new ArrayList<>();
        for(ToDo t: toDoList) {
            boolean addIt = false;
            try{
                Activity a = (Activity) t.getCheckList().getAssignedTo();
                if (!a.getClass().getSimpleName().equals("CheckList"))
                    addIt = true;
            } catch (Exception ignored){}
            if(addIt)
                tList.add(t);
        }
        return tList;
    }

    private static String getQueryString(int whoFilter, boolean sortDesc, boolean alpha){
        String select = "SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id INNER JOIN Activity a ON c.assignedTo.id = a.id ";

        // WHERE Statement Builder
        String wIsOwner = "a.assignedTo.id = :id";
        String wIsDelegate = "(t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id))";
        String wIsSourceOwner = "(t.isComplete=false AND (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id))";
        String where = "WHERE a.isComplete = false";
        if(whoFilter == 1)
            where += " AND (("+wIsOwner+") OR ("+wIsDelegate+") OR ("+wIsSourceOwner+"))";
        else if(whoFilter ==2)
            where += " AND ("+wIsOwner+")";
        else if(whoFilter==3)
            where += " AND (("+wIsDelegate+") OR ("+wIsSourceOwner+"))";

        // ORDER BY Statement Builder
        String orderBy = " ORDER BY ";
        if(!sortDesc && !alpha)
            orderBy += "a.dueDate, a.fullName,";
        else if(!alpha)
            orderBy += "a.dueDate DESC, a.fullName,";
        else if(!sortDesc)
            orderBy += "a.fullName,";
        else
            orderBy += "a.fullName DESC,";
        orderBy += "t.isComplete, t.sortOrder";
        return select + where + orderBy;
    }

    private static Query getToDoQuery(EntityManager em, int whoFilter, boolean sortDesc, boolean alpha){
        return em.createQuery(getQueryString(whoFilter,sortDesc,alpha));
        /*Query q;
        if(whoFilter==0 && !sortDesc && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " ORDER BY a.id,a.dueDate,t.isComplete,t.sortOrder");
        else if(whoFilter==0 && !sortDesc)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " ORDER BY a.id,a.fullName,t.isComplete,t.sortOrder");
        else if(whoFilter==0 && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " ORDER BY a.id,a.dueDate DESC,t.isComplete,t.sortOrder");
        else if(whoFilter==0)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " ORDER BY a.id,a.fullName DESC,t.isComplete,t.sortOrder");
        else if(whoFilter==1 && !sortDesc && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (a.assignedTo.id = :id OR (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id)))" +
                    " ORDER BY a.id,a.dueDate,t.isComplete,t.sortOrder");
        else if(whoFilter==1 && !sortDesc)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (a.assignedTo.id = :id OR (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id)))" +
                    " ORDER BY a.id,a.fullName,t.isComplete,t.sortOrder");
        else if(whoFilter==1 && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false  " +
                    " AND (a.assignedTo.id = :id OR (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id)))" +
                    " ORDER BY a.id, a.dueDate DESC, t.isComplete,t.sortOrder");
        else if(whoFilter==1)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (a.assignedTo.id = :id OR (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id)))" +
                    " ORDER BY a.id,a.fullName DESC,t.isComplete,t.sortOrder");
        else if(whoFilter==2 && !sortDesc && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND(a.assignedTo.id = :id )" +
                    " ORDER BY a.id,a.dueDate,t.isComplete,t.sortOrder");
        else if(whoFilter==2 && !sortDesc)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND(a.assignedTo.id = :id )" +
                    " ORDER BY a.id,a.fullName,t.isComplete,t.sortOrder");
        else if(whoFilter==2 && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND(a.assignedTo.id = :id )" +
                    " ORDER BY a.id,a.dueDate DESC,t.isComplete,t.sortOrder");
        else if(whoFilter==2)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND(a.assignedTo.id = :id )" +
                    " ORDER BY a.id,a.fullName DESC,t.isComplete,t.sortOrder");
        else if(!sortDesc && !alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id))" +
                    " ORDER BY a.id,a.dueDate,t.isComplete,t.sortOrder");
        else if(!sortDesc)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id))" +
                    " ORDER BY a.id,a.fullName,t.isComplete,t.sortOrder");
        else if(!alpha)
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id))" +
                    " ORDER BY a.id,a.dueDate DESC,t.isComplete,t.sortOrder");
        else
            q = em.createQuery("SELECT t FROM ToDo t INNER JOIN CheckList c ON t.checkList.id = c.id" +
                    " INNER JOIN Activity a ON c.assignedTo.id = a.id WHERE a.isComplete = false" +
                    " AND (t.isComplete=false AND (t.task.hasOwner=true AND t.task.owner is not null AND t.task.owner.id = :id)" +
                    " OR (t.task.isSourced=true AND t.task.sourceOwner is not null AND t.task.sourceOwner.id = :id))" +
                    " ORDER BY a.id,a.fullName DESC,t.isComplete,t.sortOrder");
        return q;*/
    }


}
