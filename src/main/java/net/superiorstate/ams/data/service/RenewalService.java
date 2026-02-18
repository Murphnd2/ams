package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class RenewalService {

    public static Renewal createRenewal(EntityManager em, Employer er, Person initiatedBy){
        Renewal renewal = new Renewal();
        renewal.setEmployer(er);
        renewal.setComplete(false);
        renewal.setLoggedBy(initiatedBy);
        renewal.setAssignedTo(initiatedBy);
        renewal.setFullName(er.getEmployerName());
        renewal.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        em.getTransaction().begin();
        em.persist(renewal);
        em.getTransaction().commit();
        return renewal;
    }
    public static void createCheckListForRenewal(EntityManager em, Renewal r, Person initiatedBy){
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setComplete(false);
        c.setRenewal(r);
        c.setAssignedTo(r);
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setLoggedBy(initiatedBy);
        c.setFullName(r.getFullName() + " Checklist");
        em.persist(c);
        em.getTransaction().commit();

        em.getTransaction().begin();
        ToDo t = new ToDo();
        t.setComplete(true);
        t.setCheckList(c);
        t.setTask(EntityLookup.getTaskById(em,153L));
        t.setSortOrder(0);
        t.setDateCompleted(Date.valueOf(LocalDate.now()));
        t.setCompletedBy(initiatedBy);
        em.persist(t);
        em.getTransaction().commit();

        em.getTransaction().begin();
        c.getToDoList().add(t);
        em.persist(c);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Renewal renewal = EntityLookup.getRenewalById(em,r.getId());
        renewal.setCheckList(c);
        em.persist(renewal);
        em.getTransaction().commit();
    }
    public static void addBenefitToRenewal(HttpServletRequest request, EntityManager em, Benefit b, Renewal r){
        Person currentUser = (Person) request.getSession().getAttribute("currentPerson");
        // (1) CREATE RENEWAL ITEM
        RenewalItem ri = createRenewalItemFromBenefit(em,b,r);
        // (2) ADD RENEWAL ITEM TO RENEWAL ITEM LIST IN RENEWAL
        updateRenewalItemListInRenewal(em,r);
        // (3) UPDATE NEXT RENEWAL DATE IN BENEFIT TABLE
        updateNextRenewalDateInBenefitTable(em,b);
        // (4) ADD ANY NEW TASKS NECESSARY FOR RENEWAL TO CHECKLIST
        addMissingTasksToRenewalForNewRenewalItem(em,ri,r);
        // (5) ADD NOTE IDENTIFYING BENEFIT ADDED
        createNoteIdentifyingBenefitAdded(em,b,r,currentUser);
    }
    public static void removeBenefitFromRenewal(HttpServletRequest request, EntityManager em, RenewalItem ri){
        int renewalItemCount = getRenewalItemCount(em,ri);
        Person currenUser = (Person) request.getSession().getAttribute("currentPerson");
        Renewal r = ri.getRenewal();
        Benefit b = ri.getBenefit();
        Date dateFor = ri.getDateFor();
        // (1) Remove Item From Renewal
        removeRenewalItemFromRenewal(em,ri);
        // (2) Update Renewal Item List in Renewal
        updateRenewalItemListInRenewal(em,r);
        // (3) Revert Next Renewal Date in Benefit Table
        revertRenewalDateForBenefit(em,b,dateFor);
        // (4) Close Any Tasks Solely Required by That Renewal Item
        closeUnnecessaryToDos(em,r,b,currenUser);
        // (5) Add Note Identifying Item Removed
        createNoteIdentifyingBenefitRemoved(em,b,r,currenUser);
        // (6) If Last Renewal Item, Close the Renewal
        //if(renewalItemCount==1)
          //  closeThisRenewal(em,r,currenUser, request);
    }

    private static void closeThisRenewal(EntityManager em, Renewal r, Person p, HttpServletRequest request){
        em.getTransaction().begin();
        Note n = new Note();
        n.setActivity(r);
        n.setCreatedBy(p);
        n.setDateGenerated(Date.valueOf(LocalDate.now()));
        n.setStatus(EntityLookup.getActivityStatusById(em,1));
        n.setReasonCreated(EntityLookup.getReasonById(em,1));
        n.setDetail("Closed automatically after last renewal benefit was removed.");
        em.persist(n);
        em.getTransaction().commit();

        CheckList c = EntityLookup.getCheckListById(em,r.getCheckList().getId());
        em.getTransaction().begin();
        c.setComplete(true);
        c.setDateCompleted(Date.valueOf(LocalDate.now()));
        c.setCompletedBy(p);
        em.persist(c);
        em.getTransaction().commit();

        Renewal renewal = EntityLookup.getRenewalById(em,r.getId());
        em.getTransaction().begin();
        renewal.setComplete(true);
        renewal.setCompletedBy(p);
        renewal.setDateCompleted(Date.valueOf(LocalDate.now()));
        em.persist(renewal);
        em.getTransaction().commit();

    }

    private static int getRenewalItemCount(EntityManager em, RenewalItem ri){
        Renewal r = ri.getRenewal();
        Query q = em.createQuery("SELECT ri FROM RenewalItem ri WHERE ri.renewal.id = :rId");
        q.setParameter("rId", r.getId());
        List<RenewalItem> renewalItemList;
        try{
            renewalItemList = (List<RenewalItem>) q.getResultList();
        } catch (NoResultException e){
            return 0;
        }
        if(renewalItemList!=null && renewalItemList.size()>0)
            return renewalItemList.size();
        return 0;
    }

    private static void revertRenewalDateForBenefit(EntityManager em, Benefit benefit, Date dateFor){
        em.getTransaction().begin();
        Benefit b = EntityLookup.getBenefitById(em,benefit.getId());
        b.setNextRenewalDue(dateFor);
        b.setLastRenewed(null);
        em.persist(b);
        em.getTransaction().commit();
    }

    private static void removeRenewalItemFromRenewal(EntityManager em, RenewalItem ri){
        em.getTransaction().begin();
        Query q = em.createQuery("DELETE FROM RenewalItem ri WHERE ri.id = :id");
        q.setParameter("id",ri.getId());
        q.executeUpdate();
        em.getTransaction().commit();
    }

    private static void createNoteIdentifyingBenefitRemoved(EntityManager em, Benefit b, Renewal r, Person p){
        String detail = b.getPlanDescription()+ " was removed from the renewal on " + Date.valueOf(LocalDate.now().toString());
        createNote(em,b,r,p,detail);
    }

    private static void createNoteIdentifyingBenefitAdded(EntityManager em,Benefit benefit, Renewal r, Person initiatedBy){
        String detail = benefit.getPlanDescription()+ " was added to the renewal on " + Date.valueOf(LocalDate.now().toString());
        createNote(em,benefit,r,initiatedBy,detail);
    }

    private static void createNote(EntityManager em, Benefit b, Renewal r, Person p, String detail){
        // CREATE NOTE IDENTIFYING THAT BENEFIT WAS ADDED TO RENEWAL
        em.getTransaction().begin();
        Note n = new Note();
        n.setDetail(detail);
        n.setDateGenerated(Date.valueOf(LocalDate.now()));
        n.setActivity(r);
        n.setCreatedBy(p);
        n.setReasonCreated(EntityLookup.getReasonById(em,1));
        n.setStatus(EntityLookup.getActivityStatusById(em,2));
        em.persist(n);
        em.getTransaction().commit();
    }


    private static void updateRenewalItemListInRenewal(EntityManager em, Renewal r){
        //ADD RENEWAL ITEM TO THE RENEWAL ITEM LIST IN RENEWAL
        em.getTransaction().begin();
        Query q = em.createQuery("SELECT ri FROM RenewalItem ri WHERE ri.renewal.id = :id");
        q.setParameter("id",r.getId());
        List<RenewalItem> renewalItemList;
        try{
            renewalItemList = (List<RenewalItem>) q.getResultList();
        } catch (NoResultException e){
            renewalItemList = new ArrayList<>();
        }
        Renewal renewal1 = EntityLookup.getRenewalById(em,r.getId());
        assert renewal1 != null;
        renewal1.setRenewalItemList(renewalItemList);
        em.persist(renewal1);
        em.getTransaction().commit();
    }

    private static void updateNextRenewalDateInBenefitTable(EntityManager em, Benefit benefit){
        // UPDATE NEXT RENEWAL DATE IN BENEFIT TABLE
        em.getTransaction().begin();
        Benefit b = EntityLookup.getBenefitById(em, benefit.getId());
        b.setLastRenewed(b.getNextRenewalDue());
        b.setNextRenewalDue(Date.valueOf(b.getNextRenewalDue().toLocalDate().plusYears(1)));
        em.persist(b);
        em.getTransaction().commit();
    }

    private static RenewalItem createRenewalItemFromBenefit(EntityManager em, Benefit benefit, Renewal renewal){
        // CREATE RENEWAL ITEM FROM BENEFIT
        em.getTransaction().begin();
        Renewal r = EntityLookup.getRenewalById(em,renewal.getId());
        RenewalItem ri = new RenewalItem();
        ri.setRenewal(r);
        ri.setBenefit(benefit);
        ri.setDateFor(benefit.getNextRenewalDue());
        em.persist(ri);
        em.getTransaction().commit();
        return ri;
    }

    private static void addMissingTasksToRenewalForNewRenewalItem(EntityManager em, RenewalItem ri, Renewal r){
        List<SortedTask> requiredList = getTasksRequiredForRenewalItem(em,ri);
        if(requiredList==null || requiredList.size()==0)
            return;
        List<ToDo> existingList = getChecklistsToDos(em,r.getCheckList());
        for(SortedTask st:requiredList){
            boolean inList = false;
            assert existingList != null;
            for(ToDo t:existingList){
                if(Objects.equals(st.getTask().getId(), t.getTask().getId())){
                    inList=true;
                    if(t.isComplete() && t.getDateCompleted()== AppConstantDAO.getFalseCloseDate(em)){
                        em.getTransaction().begin();
                        ToDo toDo = EntityLookup.getToDoById(em,t.getId());
                        toDo.setComplete(false);
                        toDo.setDateCompleted(null);
                        em.persist(toDo);
                        em.getTransaction().commit();
                    }
                    break;
                }
            }
            if(!inList)
                addNewToDoToCheckList(em,st,r.getCheckList());
        }
    }

    private static void addNewToDoToCheckList(EntityManager em, SortedTask st, CheckList c){
        em.getTransaction().begin();
        ToDo t = new ToDo();
        t.setCheckList(c);
        t.setTask(st.getTask());
        t.setSortOrder(st.getSortOrder());
        t.setComplete(false);
        em.persist(t);
        em.getTransaction().commit();

        em.getTransaction().begin();
        CheckList checkList = EntityLookup.getCheckListById(em,c.getId());
        assert checkList != null;
        List<ToDo> toDoList = getToDosForCheckList(em,checkList);
        checkList.setToDoList(toDoList);
        em.persist(checkList);
        em.getTransaction().commit();
    }

    private static List<ToDo> getToDosForCheckList(EntityManager em, CheckList c){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId order by t.sortOrder");
        q.setParameter("cId",c.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return toDoList;
    }

    private static void closeUnnecessaryToDos(EntityManager em, Renewal r, Benefit benefitRemoved, Person initiatedBy){
        List<SortedTask> tasksToRemove = getTasksNoLongerRequired(em,r,benefitRemoved);
        List<ToDo> toDoList = r.getCheckList().getToDoList();
        for(ToDo t: toDoList){
            for(SortedTask st: tasksToRemove){
                if(Objects.equals(st.getTask().getId(), t.getTask().getId())){
                    closeUnnecessaryToDo(em,t,initiatedBy);
                }
            }
        }
        updateToDoListForRenewal(em,r);
    }

    private static void updateToDoListForRenewal(EntityManager em, Renewal r){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId");
        q.setParameter("cId",r.getCheckList().getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        }catch (NoResultException e){
            toDoList = new ArrayList<>();
        }
        em.getTransaction().begin();
        CheckList c = EntityLookup.getCheckListById(em,r.getCheckList().getId());
        assert c != null;
        c.setToDoList(toDoList);
        em.persist(c);
        em.getTransaction().commit();
    }

    private static void closeUnnecessaryToDo(EntityManager em, ToDo t, Person initiatedBy){
        em.getTransaction().begin();
        ToDo toDo = EntityLookup.getToDoById(em,t.getId());
        assert toDo != null;
        toDo.setComplete(true);
        toDo.setDateCompleted(AppConstantDAO.getFalseCloseDate(em));
        toDo.setCompletedBy(initiatedBy);
        em.persist(toDo);
        em.getTransaction().commit();
    }

    private static List<SortedTask> getTasksNoLongerRequired(EntityManager em, Renewal r, Benefit benefitRemoved){
        List<SortedTask> tasksRequiredList = getTasksRequiredForRenewal(em,r);
        List<SortedTask> tasksPreviouslyRequired = getTasksRequiredForBenefit(em,benefitRemoved);
        List<SortedTask> tasksToRemove = new ArrayList<>();
        if(tasksPreviouslyRequired!=null && tasksPreviouslyRequired.size()>0){
            for(SortedTask stPrevious:tasksPreviouslyRequired){
                boolean stillRequired = false;
                for(SortedTask stStill: tasksRequiredList){
                    if(Objects.equals(stStill.getTask().getId(), stPrevious.getTask().getId())){
                        stillRequired = true;
                        break;
                    }
                }
                if(!stillRequired)
                    tasksToRemove.add(stPrevious);
            }
        }
        return tasksToRemove;
    }

   public static List<SortedTask> getTasksRequiredForRenewal2(EntityManager em, RenewalItem ri){

        List<TemplatePurpose> templatePurposeList = new ArrayList<>();
        templatePurposeList.add(ri.getBenefit().getPlanType().getTemplatePurpose());

        List<RequiredTaskList> requiredTaskLists = new ArrayList<>();
        for(TemplatePurpose tp:templatePurposeList){
            Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.templatePurpose.id = :id");
            q.setParameter("id",tp.getId());
            List<RequiredTaskList> requiredTaskLists1;
            try{
                requiredTaskLists1 = (List<RequiredTaskList>) q.getResultList();
            } catch (NoResultException e){
                continue;
            }
            if(requiredTaskLists1==null || requiredTaskLists1.size()==0)
                continue;
            for(RequiredTaskList rtl:requiredTaskLists1){
                if(!requiredTaskLists.contains(rtl))
                    requiredTaskLists.add(rtl);
            }
        }
        List<Task> taskList = new ArrayList<>();
        List<SortedTask> sortedTaskList = new ArrayList<>();
        for(RequiredTaskList rtl:requiredTaskLists){
            List<TaskSequenceTable> taskSequenceTableList;
            Query q = em.createQuery("SELECT tst from TaskSequenceTable tst WHERE tst.taskSequenceID = :id");
            q.setParameter("id",rtl.getId());
            try{
                taskSequenceTableList = (List<TaskSequenceTable>) q.getResultList();
            } catch (NoResultException e){
                continue;
            }
            if(taskSequenceTableList==null || taskSequenceTableList.size()==0)
                continue;
            for(TaskSequenceTable tst:taskSequenceTableList){
                if(!taskList.contains(tst.getTask())){
                    taskList.add(tst.getTask());
                    sortedTaskList.add(new SortedTask(tst.getTask(),tst.getSortOrder()));
                }
            }
        }
        return sortedTaskList;
    }
    public static List<SortedTask> getTasksRequiredForRenewal(EntityManager em, Renewal r){
        List<SortedTask> requiredTaskList = new ArrayList<>();
        for(RenewalItem ri: r.getRenewalItemList()){
            List<RequiredTaskList> requiredTaskLists;
            Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.templatePurpose.id = :tpId");
            q.setParameter("tpId",ri.getBenefit().getPlanType().getTemplatePurpose().getId());
            try{
                requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
            } catch (NoResultException e){
                requiredTaskLists = null;
            }
            if(requiredTaskLists!=null && requiredTaskLists.size()>0){
                for(RequiredTaskList rtl: requiredTaskLists){
                    List<TaskSequenceTable> taskSequenceTableList;
                    Query q1 = em.createQuery("SELECT tst from TaskSequenceTable tst WHERE tst.taskSequence.id = :id");
                    q1.setParameter("id",rtl.getId());
                    try{
                        taskSequenceTableList = (List<TaskSequenceTable>) q1.getResultList();
                    } catch (NoResultException e1){
                        continue;
                    }
                    if(taskSequenceTableList==null || taskSequenceTableList.size()==0)
                        continue;
                    for(TaskSequenceTable tst:taskSequenceTableList){
                        boolean inList = false;
                        for(SortedTask st:requiredTaskList){
                            if(Objects.equals(tst.getTask().getId(), st.getTask().getId())){
                                inList = true;
                                break;
                            }
                        }
                        if(!inList)
                            requiredTaskList.add(new SortedTask(tst.getTask(),tst.getSortOrder()));
                    }
                }
            }
        }
        return requiredTaskList;
    }

    private static List<SortedTask> getTasksRequiredForBenefit(EntityManager em, Benefit b){
        List<SortedTask> requiredList = new ArrayList<>();
        Query q = em.createQuery("SELECT r FROM RequiredTaskList r WHERE r.templatePurpose.id = :id");
        q.setParameter("id",b.getPlanType().getTemplatePurpose().getId());
        List<RequiredTaskList> requiredTaskLists;
        try{
            requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
        } catch (NoResultException e){
            requiredTaskLists = null;
        }
        if(requiredTaskLists==null || requiredTaskLists.size()==0)
            return null;
        for (RequiredTaskList rtl : requiredTaskLists) {
            List<TaskSequenceTable> tstList;
            Query q1 = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id");
            q1.setParameter("id",rtl.getId());
            try{
                tstList = (List<TaskSequenceTable>) q1.getResultList();
            } catch (NoResultException e1){
                continue;
            }
            if(tstList==null || tstList.size() == 0)
                continue;
            for (TaskSequenceTable tst : tstList) {
                boolean inList = false;
                for (SortedTask st : requiredList) {
                    if (Objects.equals(st.getTask().getId(), tst.getTask().getId())) {
                        inList = true;
                        break;
                    }
                }
                if (!inList)
                    requiredList.add(new SortedTask(tst.getTask(), tst.getSortOrder()));
            }
        }
        return  requiredList;
    }

    private static List<SortedTask> getTasksRequiredForRenewalItem(EntityManager em, RenewalItem ri){
        Benefit b = ri.getBenefit();
        return getTasksRequiredForBenefit(em,b);
    }
    private static List<ToDo> getChecklistsToDos(EntityManager em, CheckList c){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId order by t.sortOrder,t.task.description");
        q.setParameter("cId",c.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return toDoList;
    }

    public static List<RenewalItem> getRenewalItems(EntityManager em, Long renewalId){
        List<RenewalItem> renewalItemList;
        try{
            Query q = em.createQuery("SELECT r FROM RenewalItem r WHERE r.renewal.id = :id");
            q.setParameter("id",renewalId);
            renewalItemList = (List<RenewalItem>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            renewalItemList = new ArrayList<>();
        }
        return renewalItemList;
    }


    private static List<Renewal> getRenewalList(EntityManager em, String openClosedOrAll){
        List<Renewal> renewalList;
        Query q = em.createQuery("SELECT r FROM Renewal r order by r.fullName");
        if(openClosedOrAll.equalsIgnoreCase("OPEN"))
            q = em.createQuery("SELECT r FROM Renewal r WHERE r.isComplete = false order by r.fullName");
        if(openClosedOrAll.equalsIgnoreCase("CLOSED"))
            q= em.createQuery("SELECT r FROM Renewal r WHERE r.isComplete = true order by r.fullName");
        try{
            renewalList = (List<Renewal>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            renewalList = new ArrayList<>();
        }
        return  renewalList;
    }
    public static List<Renewal> getOpenRenewals(EntityManager em){
        return getRenewalList(em,"OPEN");
    }
    public static List<Renewal> getClosedRenewals(EntityManager em){
        return getRenewalList(em,"CLOSED");
    }

    public static List<Renewal> getMyRenewals(EntityManager em, Person person){
        List<Renewal> renewalList;
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.assignedTo = :person AND r.isComplete = false ORDER BY r.fullName");
        q.setParameter("person", person);
        try{
            renewalList = (List<Renewal>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            renewalList = new ArrayList<>();
        }
        return  renewalList;
    }

    public static List<Renewal> getPastRenewalsForEmployer(EntityManager em, Employer employer){
        List<Renewal> renewalList;
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer = :employer");
        q.setParameter("employer", employer);
        try{
            renewalList = (List<Renewal>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            renewalList = new ArrayList<>();
        }
        return  renewalList;
    }

    public static List<Employer> getEmployersNeedingRenewal(EntityManager em){
        List<Employer> employerList = new ArrayList<>();
        Date cutOff = Date.valueOf(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()).plusDays(90L));
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.nextRenewalDue < :date");
        q.setParameter("date",cutOff);
        List<Benefit> benefitList;
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            benefitList = new ArrayList<>();
        }
        for(Benefit b : benefitList){
            if(!employerList.contains(b.getEmployer()))
                employerList.add(b.getEmployer());
        }
        return employerList;
    }

    public static List<Benefit> getBenefitsByEmployerSortedForRenewal(EntityManager em, Employer employer){
        List<Renewal> renewalList;
        Query query = em.createQuery("SELECT r FROM Renewal r WHERE r.isComplete = false and r.employer = :employer");
        query.setParameter("employer",employer);
        try{
            renewalList = (List<Renewal>) query.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            renewalList = new ArrayList<>();
        }
        List<Benefit> benefitList;
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.employer = :employer and b.isActive=true ORDER BY b.nextRenewalDue,b.planType.planTypeId,b.planDescription");
        q.setParameter("employer", employer);
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            benefitList = new ArrayList<>();
        }
        System.out.println("************* BENEFITS *****************");
        for(Benefit b:benefitList){
            System.out.println(b.getId()+"-"+b.getPlanName());
        }
        for(Renewal r:renewalList){
            System.out.println("**** RENEWAL ****");
            System.out.println(r.getEmployer().getEmployerName());
            for(RenewalItem ri:r.getRenewalItemList()){
                System.out.println(ri.getBenefit().getId()+"-"+ri.getBenefit().getPlanName());
                benefitList.remove(ri.getBenefit());
            }
        }
        return  benefitList;
    }


}
