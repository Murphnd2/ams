package net.superiorstate.ams.previous.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.previous.data.activity.aList;
import net.superiorstate.ams.previous.data.misc.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.ActivityOut;
import net.superiorstate.ams.previous.model.activity.ActivityShell;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.CheckListOut;
import net.superiorstate.ams.previous.model.activity.checklist.CheckListShell;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDoOut;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalEmployer;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.TimeStretch;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SessionVar {
    private List<User> staffList;
    private boolean userIsIn;
    private List<TimeStretch> myTimeHistory;
    private List<ActivityOut> openActivityList;
    private List<RenewalEmployer> employerRenewalList;
    private List<Employer> pspEmployerList;
    private List<TicketSubCategory> ticketReasonList;
    private List<ActivityOut> filteredActivityList;
    private List<ActivityShell> filteredShellList;
    private boolean filterTicket;
    private boolean filterSetup;
    private boolean filterRenewal;
    private boolean filterAlphabetical;
    private int filterWhoseActivities;
    private boolean filterNeedsContact;
    private boolean filterWaitingOnUs;

    private ToDoOut currentToDoOut;
    private List<CheckListShell> myCurrentCheckLists;
    private List<CheckList> myClosedTodayCheckLists;
    private List<CheckListShell> myFutureCheckLists;
    private Activity currentActivity;
    private Setup currentSetup;
    private Renewal currentRenewal;
    private Ticket currentTicket;
    private CheckList currentCheckList;
    private List<ToDo> currentOpenToDos;
    private List<ToDo> currentClosedToDos;
    private List<Note> currentActivityNotes;
    private Person currentActivityOwner;
    private int viewPort;
    private Person primaryContactForActivity;
    private List<Person> additionalContactsForActivity;
    private List<Employee> employeeList;
    private Person currentPerson;
    private User currentUser;

    private ToDo currentToDo;

    private List<ToDoOut> currentActivityToDos;

    private boolean needsActivityRefresh;

    private ActivityOut activityOutNeedingRefresh;
    private int daysWarning;
    private int daysDanger;

    private List<Activity25u> activity25usAll;
    private List<Activity25u> activity25usParticipant;

    public SessionVar(){};

    public String getClassName() {
        if(getCurrentActivity()!=null)
            return getCurrentActivity().getClass().getSimpleName().toString();
        return null;
    }

    public List<Activity25u> getActivity25usAll() {
        return activity25usAll;
    }

    public void setActivity25usAll(List<Activity25u> activity25usAll) {
        this.activity25usAll = activity25usAll;
    }

    public List<Activity25u> getActivity25usParticipant() {
        return activity25usParticipant;
    }

    public void setActivity25usParticipant(List<Activity25u> activity25usParticipant) {
        this.activity25usParticipant = activity25usParticipant;
    }

    public int getDaysWarning() {
        return daysWarning;
    }

    public void setDaysWarning(int daysWarning) {
        this.daysWarning = daysWarning;
    }

    public int getDaysDanger() {
        return daysDanger;
    }

    public void setDaysDanger(int daysDanger) {
        this.daysDanger = daysDanger;
    }

    public boolean isNeedsActivityRefresh() {
        return needsActivityRefresh;
    }

    public void setNeedsActivityRefresh(boolean needsActivityRefresh) {
        this.needsActivityRefresh = needsActivityRefresh;
    }

    public ActivityOut getActivityOutNeedingRefresh() {
        return activityOutNeedingRefresh;
    }

    public void setActivityOutNeedingRefresh(ActivityOut activityOutNeedingRefresh) {
        this.activityOutNeedingRefresh = activityOutNeedingRefresh;
    }

    public Person getCurrentPerson() {
        return currentPerson;
    }

    public void setCurrentPerson(Person currentPerson) {
        this.currentPerson = currentPerson;
    }

    public ToDoOut getCurrentToDoOut() {
        return currentToDoOut;
    }

    public void setCurrentToDoOut(ToDoOut currentToDoOut) {
        this.currentToDoOut = currentToDoOut;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public List<User> getStaffList() {
        return staffList;
    }
    public void setStaffList(List<User> staffList) {
        this.staffList = staffList;
    }

    public boolean getUserIsIn() {
        return userIsIn;
    }
    public void setUserIsIn(boolean userIsIn) {
        this.userIsIn = userIsIn;
    }
    public List<TimeStretch> getMyTimeHistory() {
        return myTimeHistory;
    }
    public void setMyTimeHistory(List<TimeStretch> myTimeHistory) {
        this.myTimeHistory = myTimeHistory;
    }
    public List<ActivityOut> getOpenActivityList() {
        return openActivityList;
    }
    public void setOpenActivityList(List<ActivityOut> openActivityList) {
        this.openActivityList = openActivityList;
    }

    public List<RenewalEmployer> getEmployerRenewalList() {
        return employerRenewalList;
    }
    public void setEmployerRenewalList(List<RenewalEmployer> employerRenewalList) {
        this.employerRenewalList = employerRenewalList;
    }

    public List<Employer> getPspEmployerList() {
        return pspEmployerList;
    }
    public void setPspEmployerList(List<Employer> pspEmployerList) {
        this.pspEmployerList = pspEmployerList;
    }

    public List<TicketSubCategory> getTicketReasonList() {
        return ticketReasonList;
    }
    public void setTicketReasonList(List<TicketSubCategory> ticketReasonList) {
        this.ticketReasonList = ticketReasonList;
    }

    public ToDo getCurrentToDo() {
        return currentToDo;
    }

    public void setCurrentToDo(ToDo currentToDo) {
        this.currentToDo = currentToDo;
    }

    public boolean isUserIsIn() {
        return userIsIn;
    }

    public List<ActivityShell> getFilteredShellList() {
        return filteredShellList;
    }

    public void setFilteredShellList(List<ActivityShell> filteredShellList) {
        this.filteredShellList = filteredShellList;
    }

    public List<ActivityOut> getFilteredActivityList() {
        return filteredActivityList;
    }
    public void setFilteredActivityList(List<ActivityOut> filteredActivityList) {
        this.filteredActivityList = filteredActivityList;
    }
    public boolean isFilterTicket() {
        return filterTicket;
    }
    public void setFilterTicket(boolean filterTicket) {
        this.filterTicket = filterTicket;
    }
    public boolean isFilterSetup() {
        return filterSetup;
    }
    public void setFilterSetup(boolean filterSetup) {
        this.filterSetup = filterSetup;
    }
    public boolean isFilterRenewal() {
        return filterRenewal;
    }
    public void setFilterRenewal(boolean filterRenewal) {
        this.filterRenewal = filterRenewal;
    }
    public boolean isFilterAlphabetical() {
        return filterAlphabetical;
    }
    public void setFilterAlphabetical(boolean filterAlphabetical) {
        this.filterAlphabetical = filterAlphabetical;
    }
    public int getFilterWhoseActivities() {
        return filterWhoseActivities;
    }
    public void setFilterWhoseActivities(int filterWhoseActivities) {
        this.filterWhoseActivities = filterWhoseActivities;
    }
    public boolean isFilterNeedsContact() {
        return filterNeedsContact;
    }
    public void setFilterNeedsContact(boolean filterNeedsContact) {
        this.filterNeedsContact = filterNeedsContact;
    }
    public boolean isFilterWaitingOnUs() {
        return filterWaitingOnUs;
    }
    public void setFilterWaitingOnUs(boolean filterWaitingOnUs) {
        this.filterWaitingOnUs = filterWaitingOnUs;
    }

    public List<CheckListShell> getMyCurrentCheckLists() {
        return myCurrentCheckLists;
    }
    public void setMyCurrentCheckLists(List<CheckListShell> myCurrentCheckLists) {
        this.myCurrentCheckLists = myCurrentCheckLists;
    }

    public List<CheckList> getMyClosedTodayCheckLists() {
        return myClosedTodayCheckLists;
    }
    public void setMyClosedTodayCheckLists(List<CheckList> myClosedTodayCheckLists) {
        this.myClosedTodayCheckLists = myClosedTodayCheckLists;
    }

    public List<CheckListShell> getMyFutureCheckLists() {
        return myFutureCheckLists;
    }
    public void setMyFutureCheckLists(List<CheckListShell> myFutureCheckLists) {
        this.myFutureCheckLists = myFutureCheckLists;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public Setup getCurrentSetup() {
        return currentSetup;
    }

    public List<ToDoOut> getCurrentActivityToDos() {
        return currentActivityToDos;
    }

    public void setCurrentActivityToDos(List<ToDoOut> currentActivityToDos) {
        this.currentActivityToDos = currentActivityToDos;
    }

    public void setCurrentSetup(Setup currentSetup) {
        this.currentSetup = currentSetup;
    }

    public Renewal getCurrentRenewal() {
        return currentRenewal;
    }

    public void setCurrentRenewal(Renewal currentRenewal) {
        this.currentRenewal = currentRenewal;
    }

    public CheckList getCurrentCheckList() {
        return currentCheckList;
    }
    public Ticket getCurrentTicket() {
        return currentTicket;
    }
    public void setCurrentTicket(Ticket currentTicket) {
        this.currentTicket = currentTicket;
    }
    public void setCurrentCheckList(CheckList currentCheckList) {
        this.currentCheckList = currentCheckList;
    }
    public List<ToDo> getCurrentOpenToDos() {
        return currentOpenToDos;
    }
    public void setCurrentOpenToDos(List<ToDo> currentOpenToDos) {
        this.currentOpenToDos = currentOpenToDos;
    }
    public List<ToDo> getCurrentClosedToDos() {
        return currentClosedToDos;
    }
    public void setCurrentClosedToDos(List<ToDo> currentClosedToDos) {
        this.currentClosedToDos = currentClosedToDos;
    }
    public List<Note> getCurrentActivityNotes() {
        return currentActivityNotes;
    }
    public void setCurrentActivityNotes(List<Note> currentActivityNotes) {
        this.currentActivityNotes = currentActivityNotes;
    }
    public Person getCurrentActivityOwner() {
        return currentActivityOwner;
    }
    public void setCurrentActivityOwner(Person currentActivityOwner) {
        this.currentActivityOwner = currentActivityOwner;
    }
    public int getViewPort() {
        return viewPort;
    }
    public void setViewPort(int viewPort) {
        this.viewPort = viewPort;
    }
    public Person getPrimaryContactForActivity() {
        return primaryContactForActivity;
    }
    public void setPrimaryContactForActivity(Person primaryContactForActivity) {
        this.primaryContactForActivity = primaryContactForActivity;
    }
    public List<Person> getAdditionalContactsForActivity() {
        return additionalContactsForActivity;
    }
    public void setAdditionalContactsForActivity(List<Person> additionalContactsForActivity) {
        this.additionalContactsForActivity = additionalContactsForActivity;
    }
    public List<Employee> getEmployeeList() {
        return employeeList;
    }
    public void setEmployeeList(List<Employee> employeeList) {
        this.employeeList = employeeList;
    }
    public void refreshUserIsIn(EntityManager em){
        setUserIsIn(dbTime.getMyLastPunch(em,getCurrentUser()).isIn());
    }
    public void refreshStaffList(EntityManager em){
        setStaffList(dbAuth.getPspStaff(em,getCurrentUser().getPerson().getPsp()));
    }
    public void refreshMyTimeHistory(EntityManager em){
        setMyTimeHistory(dbTime.getTodaysTimeHistory(em,getCurrentPerson()));
        System.out.println("REFRESH HAPPENED -------------------------------------");
        System.out.println(getMyTimeHistory().size() + ": Rows");
        for(TimeStretch t: getMyTimeHistory()){
            System.out.println("ID: " + t.getId());
            System.out.println("In Time: " + t.getInTime());
            System.out.println("Out Time: " + t.getOutTime());
            System.out.println("WHO: " + getCurrentPerson().getId() + ":" + getCurrentPerson().getFirstName());
            System.out.println("--------------------------------------");
        }
    }
    public void refreshEmployerRenewalLIst(EntityManager em){
        setEmployerRenewalList(dbRenew.getEmployerRenewals(em));
    }

    public void refreshTicketReasonList(EntityManager em){
        setTicketReasonList(dbTicket.getTicketSubCats(em));
    }
    public void refreshOpenActivityList(EntityManager em){
        Query q = em.createQuery("SELECT a FROM ActivityOut a");
        List<ActivityOut> activityOutList = new ArrayList<>();
        try{
            activityOutList = (List<ActivityOut>) q.getResultList();
        } catch (Exception ignored){
        }
        setOpenActivityList(activityOutList);
    }
    public void refreshFilteredActivityList(EntityManager em){
        refreshOpenActivityList(em);
        reFilterActivityList();
    }
    public void reFilterActivityList(){
        List<ActivityOut> startingList = getOpenActivityList();
        if(isFilterAlphabetical()){
            startingList.sort(new Comparator<ActivityOut>() {
                @Override
                public int compare(ActivityOut o1, ActivityOut o2) {
                    return o1.getFullName().compareTo(o2.getFullName());
                }
            });
        } else {
            startingList.sort(new Comparator<ActivityOut>() {
                @Override
                public int compare(ActivityOut o1, ActivityOut o2) {
                    return o1.getDueDate().compareTo(o2.getDueDate());
                }
            });
        }
        List<ActivityOut> filteredList = startingList;
        System.out.println("(1) Unfiltered: " + filteredList.size());
        if(!isFilterSetup()||!isFilterTicket()||!isFilterRenewal()){
            if(!isFilterRenewal())
                filteredList = startingList.stream().filter(act -> !act.getDtype().equals("Renewal")).collect(Collectors.toList());
            startingList = filteredList;
            System.out.println("(2a) Remove Renewal: " + filteredList.size());
            if(!isFilterSetup())
                filteredList = startingList.stream().filter(act -> !act.getDtype().equals("Setup")).collect(Collectors.toList());
            startingList = filteredList;
            System.out.println("(2b) Remove Setup: " + filteredList.size());
            if(!isFilterTicket())
                filteredList = startingList.stream().filter(act -> !act.getDtype().equals("Ticket")).collect(Collectors.toList());
            startingList = filteredList;
            System.out.println("(2c) Remove Ticket: " + filteredList.size());
        } else System.out.println("(2) Unfiltered: " + filteredList.size());
        if(isFilterWaitingOnUs() || isFilterNeedsContact()){
            Predicate<ActivityOut> predWaitOnUs = ActivityOut::isWaitingOnUs;
            Predicate<ActivityOut> predNeedsContact = ActivityOut::isNeedingContact;
            if(isFilterWaitingOnUs() && isFilterNeedsContact()) {
                filteredList = startingList.stream().filter(predWaitOnUs.or(predNeedsContact)).collect(Collectors.toList());
            } else if (isFilterNeedsContact())
                filteredList = startingList.stream().filter(predNeedsContact).collect(Collectors.toList());
            else filteredList = startingList.stream().filter(predWaitOnUs).collect(Collectors.toList());
            startingList = filteredList;

            System.out.println("(3) Remove Priority: " + filteredList.size());
        } else System.out.println("(3) No Priority Filter: " + filteredList.size());

        System.out.println("Current Person ID: " + getCurrentPerson().getId());
        if(getFilterWhoseActivities()>0){
            Predicate<ActivityOut> predAssignedToMe = act -> act.getAssignedTo()!=null && Objects.equals(act.getAssignedTo().getId(), getCurrentPerson().getId());
            Predicate<ActivityOut> predTaskForMe = act -> act.getTaskOwner()!=null && Objects.equals(act.getTaskOwner().getId(), getCurrentPerson().getId());
            if(getFilterWhoseActivities()==1)
                filteredList = startingList.stream().filter(predAssignedToMe.or(predTaskForMe)).collect(Collectors.toList());
            else if(getFilterWhoseActivities()==2)
                filteredList = startingList.stream().filter(predAssignedToMe).collect(Collectors.toList());
            else filteredList = startingList.stream().filter(predTaskForMe).collect(Collectors.toList());
            startingList = filteredList;

            System.out.println("(4) Remove Whose: " + filteredList.size());
        } else System.out.println("(4) No Remove Whose: " + filteredList.size());
        setFilteredActivityList(startingList);
    }
    public void refreshOpenChecklists(EntityManager em){
        List<CheckListShell> theList = aList.getMyOpenItems(em,getCurrentPerson());
        List<CheckListShell> currentItems = new ArrayList<>();
        List<CheckListShell> futureItems = new ArrayList<>();
        LocalDate today = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth()).plusDays(1L);
        for(CheckListShell cs:theList){
            if(cs.getShowDate().toLocalDate().isBefore(today))
                currentItems.add(cs);
            else futureItems.add(cs);
        }
        setMyCurrentCheckLists(currentItems);
        setMyFutureCheckLists(futureItems);
    }
    public void refreshClosedCheckLists(EntityManager em){
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.dateCompleted = :date AND c.assignedTo.id = :id order by c.fullName");
        Date theDate = Date.valueOf(LocalDate.of(LocalDate.now().getYear(),LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth()));
        q.setParameter("date", theDate);
        q.setParameter("id",getCurrentPerson().getId());
        List<CheckList> checkLists;
        try{
            checkLists = (List<CheckList>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        for(CheckList c:checkLists)
            em.refresh(c);
        setMyClosedTodayCheckLists(checkLists);
    }
    public void refreshActivityHistory(EntityManager em){
        Query q = em.createQuery("SELECT n FROM Note n WHERE n.activity.id = :id order by n.id desc ");
        q.setParameter("id",getCurrentActivity().getId());
        List<Note> noteList = new ArrayList<>();
        try{
            noteList = (List<Note>) q.getResultList();
        } catch (Exception ignored){}
        setCurrentActivityNotes(noteList);
    }
    public void refreshCurrentChecklistFromCurrentActivity(){
        CheckList c = null;
        if(getClassName().equals("Renewal")){
            Renewal r = (Renewal) getCurrentActivity();
            c = r.getCheckList();
        } else if (getClassName().equals("Setup")){
            Setup s = (Setup) getCurrentActivity();
            c = s.getCheckList();
        } else if (getClassName().equals("Ticket")){
            Ticket t = (Ticket) getCurrentActivity();
            c = t.getCheckList();
        }
        if(c!=null)
            setCurrentCheckList(c);
    }
    private void refreshCurrentOpenToDos(EntityManager em){
        List<ToDo> startList = getCheckListToDos(em);
        List<ToDo> endList = startList.stream().filter(t -> !t.isComplete()).toList();
        setCurrentOpenToDos(endList);
    }
    private void refreshCurrentClosedToDos(EntityManager em){
        List<ToDo> startList = getCheckListToDos(em);
        List<ToDo> endList = startList.stream().filter(ToDo::isComplete).toList();
        setCurrentClosedToDos(endList);
    }
    public void refreshToDoList(EntityManager em){
        if(getCurrentCheckList()==null)
            return;
        refreshCurrentOpenToDos(em);
        refreshCurrentClosedToDos(em);
    }
    public void refreshContactListFromCurrentActivity(EntityManager em){
        if(getCurrentActivity()==null)
            return;
        if(getCurrentActivity().getPrimaryContact()!=null)
            setPrimaryContactForActivity(getCurrentActivity().getPrimaryContact());
        else {
            Renewal r = null;
            Setup s = null;
            Ticket t = null;
            CheckList c = null;
            if(getClassName().equals("Renewal"))
                r = (Renewal) getCurrentActivity();
            else if(getClassName().equals("Setup"))
                s = (Setup) getCurrentActivity();
            else if(getClassName().equals("Ticket"))
                t = (Ticket) getCurrentActivity();
            else if(getClassName().equals("CheckList"))
                c = (CheckList) getCurrentActivity();
            if(t!=null && t.getContact()!=null)
                setPrimaryContactForActivity(t.getContact());
            else if(s!=null && s.getPrimaryContactSetup()!=null)
                setPrimaryContactForActivity(s.getPrimaryContactSetup());
            else if(getCurrentActivity().getAssigneeContactList()!=null && getCurrentActivity().getAssigneeContactList().size()>0)
                setPrimaryContactForActivity(getCurrentActivity().getAssigneeContactList().get(0));
            else if(r!=null){
                if(r.getEmployer()!=null && r.getEmployer().getContactList()!=null && r.getEmployer().getContactList().size()>0) {
                    Employee ee = r.getEmployer().getContactList().get(0);
                    Person p = dP.getPersonByEmployee(em,ee);
                    if(p!=null)
                        setPrimaryContactForActivity(p);
                }
            } else if(c!=null && c.getPrimaryContact()!=null){
                setPrimaryContactForActivity(c.getPrimaryContact());
            } else if(c!=null && c.getAssignedTo()!=null){
                try{
                    Person p = (Person) c.getAssignedTo();
                    setPrimaryContactForActivity(p);
                } catch (Exception ignored){}
            }
        }
        Query q = em.createQuery("SELECT a FROM Activity a JOIN FETCH a.assigneeContactList where a.id = :id");
        q.setParameter("id",getCurrentActivity().getId());
        List<Person> additionalContacts;
        Activity a;
        Activity a1 = null;
        try{
            a = (Activity) q.getSingleResult();
            a1 = dM.getActivityById(em,a.getId());
            em.refresh(a1);
        } catch (Exception ignored) {}

        if(a1!=null && a1.getAssigneeContactList()!=null && a1.getAssigneeContactList().size()>0)
            setAdditionalContactsForActivity(a1.getAssigneeContactList());
        else setAdditionalContactsForActivity(new ArrayList<>());

    }
    private List<ToDo> getCheckListToDos(EntityManager em){
        if(getCurrentCheckList()==null)
            return new ArrayList<>();
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :id order by t.sortOrder,t.id");
        q.setParameter("id",getCurrentCheckList().getId());
        List<ToDo> toDoList = new ArrayList<>();
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (Exception ignored){}
        return toDoList;
    }
    public void refreshToDoOutList(EntityManager em){
        if(getCurrentCheckList()==null)
            return;
        Query q = em.createQuery("SELECT t FROM ToDoOut t WHERE t.checkList.id = :id");
        q.setParameter("id",getCurrentCheckList().getId());
        List<ToDoOut> toDoOutList;
        try{
            toDoOutList = (List<ToDoOut>) q.getResultList();
            for(ToDoOut t: toDoOutList)
                em.refresh(t);
        } catch (Exception e){
            toDoOutList = new ArrayList<>();
        }

        setCurrentActivityToDos(toDoOutList);
    }

    public void refreshThisActivity(EntityManager em, Activity a,SessionVar sVar){
        if(!a.getClass().getSimpleName().equals("CheckList")){
            sVar.setCurrentActivity(a);

            Query q = em.createQuery("SELECT ao FROM ActivityOut ao");
            List<ActivityOut> aol=null;
            try{
                aol = (List<ActivityOut>) q.getResultList();
            } catch (Exception ignored){}

            if(aol!=null && aol.size()>0){
                for(ActivityOut ao:aol)
                    if(ao.getActivity().getId().equals(a.getId()))
                        em.refresh(ao);
                sVar.setOpenActivityList(aol);
                sVar.reFilterActivityList();
            }
        } else {
            sVar.setCurrentActivity(a);
            sVar.setCurrentCheckList((CheckList) a);
            Query q = em.createQuery("SELECT co FROM CheckListOut co");
            List<CheckListOut> col = null;
            try{
                col = (List<CheckListOut>) q.getResultList();
            } catch (Exception ignored){}

            if(col!=null && col.size()>0){
                for(CheckListOut co:col)
                    if(co.getCheckList().getId().equals(a.getId()))
                        em.refresh(co);
                sVar.refreshOpenChecklists(em);
            }
        }
    }

    public void refreshEmployeeList(EntityManager em, Activity a, SessionVar sessionVar){
        List<Employee> employees = new ArrayList<>();
        Query q;
        q = em.createQuery("SELECT e FROM Employee e WHERE e.employer.id = :id order by e.lastName, e.firstName");
        if(a.getClass().getSimpleName().equals("Renewal")){
            Renewal r = (Renewal) a;
            q.setParameter("id", r.getEmployer().getId());
            try{
                employees = (List<Employee>) q.getResultList();
            } catch (Exception e){employees = new ArrayList<>();}
        }else if(a.getClass().getSimpleName().equals("Ticket")){
            Person p = getPrimaryContactForActivity();
            if(p.getEmployee()!=null) {
                q.setParameter("id", p.getEmployee().getEmployer().getId());
                try{
                    employees = (List<Employee>) q.getResultList();
                } catch (Exception e){employees = new ArrayList<>();}
            }
        }
        setEmployeeList(employees);
    }
}
