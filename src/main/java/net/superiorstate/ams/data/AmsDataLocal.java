package net.superiorstate.ams.data;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.model.*;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.data.dao.PersonDAO;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.UpcomingSequence;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoOut;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.questionnaire.Questionnaire;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalEmployer;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.superiorstate.ams.model.activity.Opportunity;
public class AmsDataLocal implements AutoCloseable {
    private final EntityManager em;
    private boolean userIsIn;
    private final int RENEWAL_DAYS_OUT = 45;
    private List<TimeStretch> myTimeHistory;
    private User currentUser;
    private Person currentPerson;
    private List<Activity25u> activitiesAllOpen;
    private List<Activity25p> activitiesWithDependencies;

    private final Set<Long> pendingCloseIds = new HashSet<>();
    private long awaitingReviewCount = -1; // -1 = not yet loaded
    private List<Activity25u> filteredActivityList;
    private List<Checklist25u> checklistsAll;
    private List<Checklist25u> checklistsCurrent;
    private List<Checklist25u> checklistsClosed;
    private List<Checklist25u> checklistsFuture;

    private List<RenewalEmployer> renewalEmployers;
    private List<UserFilterPreset> filterPresets;
    private CurrentActivity currentActivity;
    private CurrentChecklist currentChecklist;

    private CurrentEmail currentEmail;
    private ActivityFilter activityFilter;
    private int daysSinceContactWarning;
    private String nextView;
    private boolean isPspAdmin;
    private boolean isAuthenticated;

    private ToDoOut currentToDoOut;
    private ToDo currentToDo;

    public AmsDataLocal(EntityManagerFactory emf){
        this.em = emf.createEntityManager();
    };

    public void intializeLocalData(EntityManager em, HttpServletRequest request){
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        Person p = (Person) request.getSession().getAttribute("currentPerson");
        setCurrentPerson(p);
        setCurrentUser(getUserFromPerson(em,getCurrentPerson()));
        setCurrentActivity(new CurrentActivity());
        setCurrentChecklist(new CurrentChecklist());
        setDaysSinceContactWarning(global.getDaysSinceWarning());
        // Load user's 3 filter presets
        Query presetQuery = em.createQuery(
                "SELECT p FROM UserFilterPreset p WHERE p.user = :user ORDER BY p.slotNumber",
                UserFilterPreset.class);
        presetQuery.setParameter("user", getCurrentUser());
        setFilterPresets(presetQuery.getResultList());

        // Initialize filter from preset slot 1 (My Actionable) if available
        setActivityFilter(new ActivityFilter());
        if (getFilterPresets() != null && !getFilterPresets().isEmpty()) {
            applyPresetToFilter(getFilterPresets().get(0));
        } else {
            getActivityFilter().initializeFilter();
        }
        boolean isPspSales = Boolean.TRUE.equals(request.getSession().getAttribute("isPspSales"));
        boolean isPspAdminRole = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (isPspSales || isPspAdminRole) {
            getActivityFilter().setViewOpportunity(true);
        }
        // Agent-only at PSP home: default to Ticket + Opportunity only
        boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
        if ((isAgent || isAgencyAdmin) && !isPspUser && !isPspAdminRole) {
            getActivityFilter().setViewRenewal(false);
            getActivityFilter().setViewSetup(false);
            getActivityFilter().setViewTicket(true);
            getActivityFilter().setViewOpportunity(true);
        }
        setCurrentEmail(new CurrentEmail());
        getCurrentEmail().initializeEmail();
        if (AppConfig.isPsp()) {
            setActivitiesAllOpen(global.getActivitiesAllOpen());
            setActivitiesWithDependencies(global.getActivitiesWithDelegation());
            setFilteredActivityList(filterActivityListing());
            setRenewalEmployers(fillRenewalEmployers(em));
        } else {
            setActivitiesAllOpen(new ArrayList<>());
            setActivitiesWithDependencies(new ArrayList<>());
            setFilteredActivityList(new ArrayList<>());
        }
        setChecklistsAll(retrieveMyChecklists(em));
        splitChecklists();
        global.setWebPath(global.getConstantValue(em,"WEB_PATH"));
    }

    public void refreshRenewals(EntityManager em){
        setRenewalEmployers(fillRenewalEmployers(em));
    }

    public List<UserFilterPreset> getFilterPresets() { return filterPresets; }
    public void setFilterPresets(List<UserFilterPreset> filterPresets) { this.filterPresets = filterPresets; }

    public CurrentEmail getCurrentEmail() {
        return currentEmail;
    }

    public List<RenewalEmployer> getRenewalEmployers() {
        return renewalEmployers;
    }

    public void setRenewalEmployers(List<RenewalEmployer> renewalEmployers) {
        this.renewalEmployers = renewalEmployers;
    }

    public void setCurrentEmail(CurrentEmail currentEmail) {
        this.currentEmail = currentEmail;
    }

    public ToDo getCurrentToDo() {
        return currentToDo;
    }

    public void setCurrentToDo(ToDo currentToDo) {
        this.currentToDo = currentToDo;
    }

    public ToDoOut getCurrentToDoOut() {
        return currentToDoOut;
    }

    public void setCurrentToDoOut(ToDoOut currentToDoOut) {
        this.currentToDoOut = currentToDoOut;
    }

    public boolean isUserIsIn() {
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

    public void markToDoClosed(long toDoId) {
        this.pendingCloseIds.add(toDoId);
    }

    // Replace the getter
    public Set<Long> getPendingCloseIds() {
        return new HashSet<>(this.pendingCloseIds); // mutable copy
    }

    public void clearPendingCloseIds() {
        this.pendingCloseIds.clear();
    }

    /** Lazy-loaded count of applications with status SUBMITTED (awaiting review). */
    public long getAwaitingReviewCount() {
        if (awaitingReviewCount < 0) {
            refreshAwaitingReviewCount();
        }
        return awaitingReviewCount;
    }

    public void refreshAwaitingReviewCount() {
        try {
            long pspId = getCurrentPerson().getPsp().getId();
            awaitingReviewCount = em.createQuery(
                    "SELECT COUNT(a) FROM Application a " +
                    "WHERE a.proposal.prospect.contact.psp.id = :pspId " +
                    "AND a.status = 'SUBMITTED' " +
                    "AND a.proposal.status NOT IN ('APPROVED', 'DENIED') " +
                    "AND a.proposal.isInactive = false", Long.class)
                .setParameter("pspId", pspId)
                .getSingleResult();
        } catch (Exception e) {
            awaitingReviewCount = 0;
        }
    }

    public void invalidateAwaitingReviewCount() {
        awaitingReviewCount = -1;
    }

    public User getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    public Person getCurrentPerson() {
        return currentPerson;
    }
    public void setCurrentPerson(Person currentPerson) {
        this.currentPerson = currentPerson;
    }
    public List<Activity25u> getActivitiesAllOpen() {
        return activitiesAllOpen;
    }
    public void setActivitiesAllOpen(List<Activity25u> activitiesAllOpen) {
        this.activitiesAllOpen = activitiesAllOpen;
    }
    public List<Activity25p> getActivitiesWithDependencies() {
        return activitiesWithDependencies;
    }
    public void setActivitiesWithDependencies(List<Activity25p> activitiesWithDependencies) {
        this.activitiesWithDependencies = activitiesWithDependencies;
    }
    public List<Checklist25u> getChecklistsAll() {
        return checklistsAll;
    }
    public void setChecklistsAll(List<Checklist25u> checklistsAll) {
        this.checklistsAll = checklistsAll;
    }
    public CurrentActivity getCurrentActivity() {
        return currentActivity;
    }
    public void setCurrentActivity(CurrentActivity currentActivity) {
        this.currentActivity = currentActivity;
    }
    public CurrentChecklist getCurrentChecklist() {
        return currentChecklist;
    }
    public void setCurrentChecklist(CurrentChecklist currentChecklist) {
        this.currentChecklist = currentChecklist;
    }
    public ActivityFilter getActivityFilter() {
        return activityFilter;
    }
    public void setActivityFilter(ActivityFilter activityFilter) {
        this.activityFilter = activityFilter;
    }
    public int getDaysSinceContactWarning() {
        return daysSinceContactWarning;
    }
    public void setDaysSinceContactWarning(int daysSinceContactWarning) {
        this.daysSinceContactWarning = daysSinceContactWarning;
    }
    public void setFilteredActivityList(List<Activity25u> filteredActivityList) {
        this.filteredActivityList = filteredActivityList;
    }

    public List<Checklist25u> getChecklistsCurrent() {
        return checklistsCurrent;
    }

    public void setChecklistsCurrent(List<Checklist25u> checklistsCurrent) {
        this.checklistsCurrent = checklistsCurrent;
    }

    public List<Checklist25u> getChecklistsClosed() {
        return checklistsClosed;
    }

    public void setChecklistsClosed(List<Checklist25u> checklistsClosed) {
        this.checklistsClosed = checklistsClosed;
    }

    public List<Checklist25u> getChecklistsFuture() {
        return checklistsFuture;
    }

    public void setChecklistsFuture(List<Checklist25u> checklistsFuture) {
        this.checklistsFuture = checklistsFuture;
    }

    public List<Activity25u> getFilteredActivityList() {
        return filteredActivityList;
    }

    public String getNextView() {
        return nextView;
    }
    public void setNextView(String nextView) {
        this.nextView = nextView;
    }

    public boolean isPspAdmin() {
        return isPspAdmin;
    }

    public void setPspAdmin(boolean pspAdmin) {
        isPspAdmin = pspAdmin;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    private User getUserFromPerson(EntityManager em, Person p){
        Query q = em.createQuery("SELECT u FROM User u WHERE u.person.id = :id");
        q.setParameter("id",p.getId());
        User u;
        try{
            u = (User) q.getSingleResult();
        } catch (NoResultException e){return null;}
        return u;
    }

    public List<Activity25u> filterActivityListing(){

        Set<Long> otherIds = retrieveMyDependentActivities().stream()
                .map(other->other.getActivity().getId())
                .collect(Collectors.toSet());

        List<Activity25u> filterList = new ArrayList<>(getActivitiesAllOpen());
        filterList.forEach(au->au.setDelegated(otherIds.contains(au.getActivity().getId())));

        System.out.println("-----------FILTERING WITH FILTER ID # " + getActivityFilter().getOwnershipFilter() + " -------------------");
        System.out.println("A: List Count = "+filterList.size());

        if(getActivityFilter().getOwnershipFilter()==1 || getActivityFilter().getOwnershipFilter()==2){
            filterList = filterList.stream()
                    .filter(a->a.getActivity().getAssignedTo().getId().equals(getCurrentPerson().getId()))
                    .collect(Collectors.toList());
            System.out.println("B: List Count = "+filterList.size());
        }
        if(getActivityFilter().getOwnershipFilter()==3) {
            filterList = new ArrayList<>();
            System.out.println("C: List Count = " + filterList.size());
        }

        if(getActivityFilter().getOwnershipFilter()==1 || getActivityFilter().getOwnershipFilter() ==3){
            for(Activity25u a: retrieveMyDependentActivities())
                if(!filterList.contains(a)) {
                    a.setDelegated(true);
                    if((a.getdType().equals("Renewal") && getActivityFilter().isViewRenewal()) ||
                            (a.getdType().equals("Setup") && getActivityFilter().isViewSetup()) ||
                            (a.getdType().equals("Ticket")&&getActivityFilter().isViewTicket()) )
                        filterList.add(a);
                }
            System.out.println("D: List Count = "+filterList.size());
        }

        if(!getActivityFilter().isViewRenewal())
            filterList = filterList.stream().filter(a -> !a.getdType().equals("Renewal")).collect(Collectors.toList());
        if(!getActivityFilter().isViewSetup())
            filterList = filterList.stream().filter(a -> !a.getdType().equals("Setup")).collect(Collectors.toList());
        if(!getActivityFilter().isViewTicket())
            filterList = filterList.stream().filter(a-> !a.getdType().equals("Ticket")).collect(Collectors.toList());
        System.out.println("E: List Count = "+filterList.size());

        if(getActivityFilter().isViewNeedsContact() || getActivityFilter().isViewWaitingOnUs()){
            Predicate<Activity25u> predWaitOnUs = Activity25u::isWaitingOnUs;
            Predicate<Activity25u> predNeedsContact = (a -> a.getDaysSinceContact()>getDaysSinceContactWarning());
            if(getActivityFilter().isViewNeedsContact() && getActivityFilter().isViewWaitingOnUs())
                filterList = filterList.stream().filter(predNeedsContact.or(predWaitOnUs)).collect(Collectors.toList());
            else if(getActivityFilter().isViewNeedsContact())
                filterList = filterList.stream().filter(predNeedsContact).collect(Collectors.toList());
            else
                filterList = filterList.stream().filter(predWaitOnUs).collect(Collectors.toList());
            System.out.println("F: List Count = "+filterList.size());
        }

        if(!getActivityFilter().isSortAlphabetically()){
            filterList.sort(new Comparator<Activity25u>() {
                @Override
                public int compare(Activity25u o1, Activity25u o2) {
                    return o1.getDueDate().compareTo(o2.getDueDate());
                }
            });
        } else Collections.sort(filterList);

        System.out.println("G: List Count = "+filterList.size());
        return filterList;
    }
    private List<Activity25u> retrieveMyDependentActivities(){
        List<Activity25u> startList = getActivitiesAllOpen().stream().map(au->{Activity25u newAu = new Activity25u(au); newAu.setDelegated(true); return newAu;}).toList();
        List<Activity25p> pList = getActivitiesWithDependencies().stream().filter(obj1 -> obj1.getTaskOwner().getId().equals(getCurrentPerson().getId())).toList();
        Set<Long> keySet = pList.stream().map(obj2->obj2.getActivity().getId()).collect(Collectors.toSet());
        return startList.stream().filter(obj1 -> keySet.contains(obj1.getActivity().getId())).toList();
    }
    private List<Checklist25u> retrieveMyChecklists(EntityManager em){
        Query q= em.createQuery("SELECT c FROM Checklist25 c where c.owner.id = :id");
        q.setParameter("id",getCurrentPerson().getId());
        List<Checklist25> checklist25s;
        try{
            checklist25s = (List<Checklist25>) q.getResultList();
        } catch (NoResultException e){return new ArrayList<>();}
        List<Checklist25u> checklist25us = new ArrayList<>();
        for(Checklist25 c:checklist25s) {
            em.refresh(c);
            checklist25us.add(new Checklist25u(c));
        }
        return  checklist25us;
    }
    private List<RenewalEmployer> fillRenewalEmployers(EntityManager entityManager) {
        LocalDate today = LocalDate.now();
        Date todayDate = Date.valueOf(today);
        Date cutoffDate = Date.valueOf(today.plusDays(RENEWAL_DAYS_OUT));
        Date stage1Cutoff = Date.valueOf(today.plusDays(30));

        // Query for qualifying benefits: active, not terminated, employer active, due soon, not in active renewal
        String jpql = "SELECT b FROM Benefit b " +
                "JOIN FETCH b.employer e " +
                "WHERE b.isActive = true " +
                "AND e.isActive = true " +
                "AND b.nextRenewalDue < :cutoff " +
                "AND (b.terminationDate IS NULL OR b.terminationDate >= :today) " +
                "AND NOT EXISTS (SELECT ri FROM RenewalItem ri " +
                "WHERE ri.benefit = b AND ri.renewal.isComplete = false)";
        Query query = entityManager.createQuery(jpql);
        query.setParameter("cutoff", cutoffDate);
        query.setParameter("today", todayDate);
        List<Benefit> benefits = query.getResultList();

        // Group by employer and aggregate
        Map<Employer, List<Benefit>> benefitsByEmployer = benefits.stream()
                .collect(Collectors.groupingBy(Benefit::getEmployer));

        List<RenewalEmployer> renewalEmployers = new ArrayList<>();
        for (Map.Entry<Employer, List<Benefit>> entry : benefitsByEmployer.entrySet()) {
            Employer employer = entry.getKey();
            List<Benefit> empBenefits = entry.getValue();

            // Earliest nextRenewalDue for stage
            Date earliestNextDue = empBenefits.stream()
                    .map(Benefit::getNextRenewalDue)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);

            if (earliestNextDue == null) continue; // Skip if no valid dates

            int stage;
            if (earliestNextDue.before(todayDate)) {
                stage = 0;
            } else if (earliestNextDue.before(stage1Cutoff)) {
                stage = 1;
            } else {
                stage = 2;
            }

            // Max lastRenewed (most recent); handle nulls by ignoring or setting to a default if needed
            Date latestLastRenewed = empBenefits.stream()
                    .map(Benefit::getLastRenewed)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElse(null); // Or use effectiveDate from a Benefit if null

            RenewalEmployer renewalEmployer = new RenewalEmployer();
            renewalEmployer.setEmployer(employer);
            renewalEmployer.setLastRenewed(latestLastRenewed);
            renewalEmployer.setStage(stage);
            renewalEmployers.add(renewalEmployer);
        }

        Collections.sort(renewalEmployers);
        System.out.println("RENEWAL ITEMS COUNT: " + renewalEmployers.size()); // Replace with logger

        return renewalEmployers;
    }
    public void respondToActivityUpdate(EntityManager em, String action, Object o){
        Long activityId;
        Activity25u au;
        Note n;
        Long toDoId;
        ToDoOut25 t;
        Long checklistId;
        int index;

        switch (action) {
            case "CHECK_UNDO" -> {
                CheckList  c = (CheckList) o;
                Checklist25u cu = retrieveChecklist25uFromList(getChecklistsAll(),c.getId());
                cu.setSortKey(1);
                cu.setComplete(false);
                cu.getActivity().setComplete(false);
                cu.getActivity().setDateCompleted(null);
                cu.getActivity().setCompletedBy(null);
                if(c.getRecurringTaskList()!=null){
                    getChecklistsAll().remove(cu);
                    List<Checklist25u> myList = new ArrayList<>(getChecklistsAll());
                    myList = myList.stream().filter(obj->obj.getRecurringTaskList()!=null).collect(Collectors.toList());
                    Optional<Checklist25u> cu1 = myList.stream().filter(obj->obj.getRecurringTaskList().getId().equals(c.getRecurringTaskList().getId())).findFirst();
                    if(cu1.isPresent()){
                        Checklist25u cu2 = cu1.get();
                        getChecklistsAll().remove(cu2);
                        getChecklistsAll().add(cu);
                    }
                }
                getChecklistsAll().sort(new Comparator<Checklist25u>() {
                    @Override
                    public int compare(Checklist25u o1, Checklist25u o2) {
                        if(o1.getSortKey()<o2.getSortKey())
                            return -1;
                        else if(o1.getSortKey()>o2.getSortKey())
                            return 1;
                        else return o1.getDueDate().compareTo(o2.getDueDate());
                    }
                });
                splitChecklists();
            }
            case "CHECK_REMINDER" ->{
                CheckList c = (CheckList) o;
                Query q = em.createQuery("SELECT c FROM Checklist25 c WHERE c.activity.id = :id");
                q.setParameter("id",c.getId());
                Checklist25 c25 = (Checklist25) q.getSingleResult();
                em.refresh(c25);
                Checklist25u c25u = new Checklist25u(c25);
                getChecklistsAll().add(c25u);
                getChecklistsAll().sort(new Comparator<Checklist25u>() {
                    @Override
                    public int compare(Checklist25u o1, Checklist25u o2) {
                        if(o1.getSortKey()==o2.getSortKey())
                            return  o1.getDueDate().compareTo(o2.getDueDate());
                        if(o1.getSortKey()<o2.getSortKey())
                            return -1;
                        return 1;
                    }
                });
                splitChecklists();

            }
            case "CHECK_DATE" -> {

                CheckList c = (CheckList) o;
                Query q = em.createQuery("SELECT c FROM Checklist25 c WHERE c.activity.id = :id");
                q.setParameter("id",c.getId());
                Checklist25 c25 = (Checklist25) q.getSingleResult();
                em.refresh(c25);
                System.out.println("GOT HERE: CLUY");
                System.out.println(c25.getDueDate());
                System.out.println(c25.getSortKey());
                List<Checklist25u> cl1 = new ArrayList<>(getChecklistsAll());
                for(Checklist25u cu: cl1){
                    if(c25.getActivity().getId().equals(cu.getActivity().getId())){
                        Checklist25u c25u = new Checklist25u(c25);
                        cl1.remove(cu);
                        cl1.add(c25u);
                        break;
                    }
                }
                cl1.sort(new Comparator<Checklist25u>() {
                    @Override
                    public int compare(Checklist25u o1, Checklist25u o2) {
                        if(o1.getSortKey()==o2.getSortKey())
                            return  o1.getDueDate().compareTo(o2.getDueDate());
                        if(o1.getSortKey()<o2.getSortKey())
                            return -1;
                        return 1;
                    }
                });
                setChecklistsAll(cl1);
                splitChecklists();
            }
            case "CHECK_OWNER" -> {
                CheckList c = (CheckList) o;
                Checklist25u cu = retrieveChecklist25uFromList(getChecklistsAll(),c.getId());
                getChecklistsAll().remove(cu);
                splitChecklists();
            }
            case "CHECK_CLOSE" -> {
                CheckList c = (CheckList) o;
                if(c.getRecurringTaskList()!=null) { // Recurring Task Needing Regeneration
                    UpcomingSequence us = RecurringChecklistDAO.getUpcomingSequence(em,c.getRecurringTaskList());
                    if(us!=null){
                        CheckList c1 = RecurringChecklistDAO.createNewRecurringChecklist(em,us, getCurrentPerson());
                        BpoTaskPushService.pushDelegatedTasks(em, c1);
                        Query q = em.createQuery("SELECT c FROM Checklist25 c WHERE c.activity.id = :id");
                        q.setParameter("id",c1.getId());
                        Checklist25 c25 = (Checklist25) q.getSingleResult();
                        Checklist25u c25u = new Checklist25u(c25);
                        getChecklistsAll().add(c25u);
                    }
                }
                List<Checklist25u> newList = new ArrayList<>(getChecklistsAll());
                newList = newList.stream().filter(obj-> !Objects.equals(obj.getActivity().getId(), c.getId())).collect(Collectors.toList());
                TypedQuery<Checklist25> q = em.createQuery("SELECT c FROM Checklist25 c WHERE c.activity.id = :id",Checklist25.class);
                q.setParameter("id",c.getId());
                Checklist25 c25 = q.getSingleResult();
                Checklist25u c25u = new Checklist25u(c25);
                c25u.setSortKey(2);
                c25u.setComplete(true);
                newList.add(c25u);
                setChecklistsAll(newList);
                splitChecklists();
            }
            case "VIEW_ACTIVITY" -> {
                activityId = (Long) o;
                getCurrentActivity().intializeActivity(em, activityId);
                setNextView("activityDetail");
            }
            case "VIEW_CHECKLIST" -> {
                checklistId = (Long) o;
                getCurrentActivity().intializeActivity(em,checklistId);
                setNextView("checklistDetail");
            }
            case "NOTE" -> {
                n = (Note) o;
                // Add note to the current activity history
                List<Note> newNoteList = new ArrayList<>(getCurrentActivity().getNotes());
                newNoteList.add(0, n);
                getCurrentActivity().setNotes(newNoteList);
                // Determine if Activity25u should be updated
                getCurrentActivity().setReFilterOnExit(n.getReasonCreated().isOutbound() || n.getStatus().getId() != 2);
                Optional<Activity25u> au1 = getActivitiesAllOpen().stream().filter(obj-> Objects.equals(obj.getActivity().getId(), getCurrentActivity().getActivity().getId())).findFirst();
                if(au1.isPresent() && getCurrentActivity().isReFilterOnExit()){
                    Activity25u activity25u = au1.get();
                    if(n.getReasonCreated().isOutbound())
                        activity25u.setDaysSinceContact(0);
                    if(n.getStatus().getId()==1)
                        activity25u.setWaitingOnUs(false);
                    else if (n.getStatus().getId()==3)
                        activity25u.setWaitingOnUs(true);
                    List<Activity25u> newList = getActivitiesAllOpen().stream().filter(a->a.getActivity().getId()!=activity25u.getActivity().getId()).collect(Collectors.toList());
                    newList.add(activity25u);
                    setActivitiesAllOpen(newList);

                }
                setNextView("activityDetail");
                System.out.println("Refilter?: " + getCurrentActivity().isReFilterOnExit());
            }
            case "AUTO_CLOSE" -> {
                Automation a = (Automation) o;
                long taskId = a.getId();
                Optional<ToDoOut25> tdo = getCurrentActivity().getToDoList().stream().filter(obj->obj.getTask().getId()==taskId).findFirst();
                if(tdo.isPresent()){
                    ToDo toDo = EntityLookup.getToDoById(em,tdo.get().getToDo().getId());
                    if(toDo!=null) {
                        em.getTransaction().begin();
                        toDo.setComplete(true);
                        toDo.setCompletedBy(getCurrentPerson());
                        toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
                        em.persist(toDo);
                        em.getTransaction().commit();
                        em.refresh(toDo);
                    }
                    t = retrieveToDoOutFromList(getCurrentActivity().getToDoList(),toDo.getId());
                    t.setComplete(true);
                    t.getToDo().setDateCompleted(Date.valueOf(LocalDate.now()));
                    t.getToDo().setCompletedBy(getCurrentPerson());
                    t.getToDo().setComplete(true);
                }
                getCurrentActivity().reSortToDoList();
            }
            case "TODO_CLOSE" -> {
                toDoId = (Long) o;
                t = retrieveToDoOutFromList(getCurrentActivity().getToDoList(), toDoId);
                if (t != null) {
                    t.setComplete(true);
                    t.getToDo().setDateCompleted(Date.valueOf(LocalDate.now()));
                    t.getToDo().setCompletedBy(getCurrentPerson());
                    t.getToDo().setComplete(true);
                }
                getCurrentActivity().reSortToDoList();                     // ← new line
                setNextView(null);                    // ← stay on same page
            }

            case "TODO_REOPEN" -> {
                toDoId = (Long) o;
                t = retrieveToDoOutFromList(getCurrentActivity().getToDoList(), toDoId);
                if (t != null) {
                    t.setComplete(false);
                    t.getToDo().setComplete(false);
                    t.getToDo().setDateCompleted(null);
                    t.getToDo().setCompletedBy(null);
                }
                getCurrentActivity().reSortToDoList();                     // ← new line
                setNextView(null);                    // ← stay on same page
            }

            case "TODO_TOGGLE" -> {
                // this case is only used for the “toggle all” button — keep your existing logic
                getCurrentActivity().setReFilterOnExit(true);
                setNextView(null);
            }
            case "TODO_ADD" -> {
                toDoId = (Long) o;
                t = retrieveToDoOutFromList(getCurrentActivity().getToDoList(), toDoId);
                if (t != null) {
                    index = getIndexOfInsertLocation(getCurrentActivity().getToDoList(), t);
                    List<ToDoOut25> newList = new ArrayList<>(getCurrentActivity().getToDoList());
                    newList.add(index,t);
                    getCurrentActivity().setToDoList(newList);
                }
                setNextView("activityDetail");
            }
            case "TD_ADD" ->{
                ToDo toDo = (ToDo) o;
                List<ToDoOut25> openList = getCurrentActivity().getToDoList().stream().filter(obj -> !obj.isComplete()).toList();
                OptionalInt location = IntStream.range(0,openList.size()).filter(i-> openList.get(i).getSortOrder()>toDo.getSortOrder()).findFirst();
                int i =  location.isPresent() ? location.getAsInt() : openList.size();
                List<ToDoOut25> newList = new ArrayList<>(getCurrentActivity().getToDoList());
                ToDoOut25 t25 = new ToDoOut25(toDo);
                newList.add(i,t25);
                getCurrentActivity().setToDoList(newList);
                getCurrentActivity().reSortToDoList();
            }
            case "CLOSE_ACTIVITY" -> {
                Activity activity = (Activity) o;


            }
            case "CLOSE_CHECK" -> {
                CheckList c = (CheckList) o;
                if(c.getRecurringTaskList()!=null) { // Recurring Task Needing Regeneration
                    UpcomingSequence us = RecurringChecklistDAO.getUpcomingSequence(em,c.getRecurringTaskList());
                    if(us!=null){
                        CheckList c1 = RecurringChecklistDAO.createNewRecurringChecklist(em,us, getCurrentPerson());
                        BpoTaskPushService.pushDelegatedTasks(em, c1);
                        Query q = em.createQuery("SELECT c FROM Checklist25 c WHERE c.activity.id = :id");
                        q.setParameter("id",c1.getId());
                        Checklist25 c25 = (Checklist25) q.getSingleResult();
                        Checklist25u c25u = new Checklist25u(c25);
                        getChecklistsAll().add(c25u);
                    }
                }

                Checklist25u cu = retrieveChecklist25uFromList(getChecklistsAll(),c.getId());
                if(cu!=null){
                    cu.setComplete(true);
                    cu.getActivity().setComplete(true);
                    cu.getActivity().setCompletedBy(getCurrentPerson());
                    cu.getActivity().setDateCompleted(Date.valueOf(LocalDate.now()));
                    cu.setSortKey(2);
                }
                splitChecklists();
            }
            case "OWNER" -> {
                Person p = (Person) o;

            }
            case "DATE" -> {
                Date date1 = (Date) o;
                getCurrentActivity().getActivity().setDueDate(date1);
                if(getCurrentActivity().getActivity().getClass().getSimpleName().equals("CheckList")){
                    CheckList c = (CheckList) getCurrentActivity().getActivity();
                    Query q = em.createQuery("SELECT c FROM Checklist25 c WHERE c.activity.id = :id");
                    q.setParameter("id",c.getId());
                    Checklist25 c25 = (Checklist25) q.getSingleResult();
                    em.refresh(c25);
                    Checklist25u cu = retrieveChecklist25uFromList(getChecklistsAll(),c.getId());
                    cu.setDueDate(date1);
                    cu.setSortKey(c25.getSortKey());
                    getChecklistsAll().sort(new Comparator<Checklist25u>() {
                        @Override
                        public int compare(Checklist25u o1, Checklist25u o2) {
                            if(o1.getSortKey()==o2.getSortKey())
                                return  o1.getDueDate().compareTo(o2.getDueDate());
                            if(o1.getSortKey()<o2.getSortKey())
                                return -1;
                            return 1;
                        }
                    });
                    splitChecklists();
                } else {
                    au = retrieveActivity25uFromList(getActivitiesAllOpen(), getCurrentActivity().getActivity().getId());
                    if (au != null) {
                        au.setDueDate(date1);
                        au.getActivity().setDueDate(date1);
                    }
                    getCurrentActivity().setReFilterOnExit(true);
                    setNextView("activityDetail");
                }
            }
            case "ADD_TICKET" -> {
                Ticket tk = (Ticket) o;
                Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
                q.setParameter("id",tk.getId());
                Activity25 ap = (Activity25) q.getSingleResult();
                au = new Activity25u(ap);
                List<Activity25u> listToModify = new ArrayList<>(getActivitiesAllOpen());
                listToModify.add(au);
                setActivitiesAllOpen(listToModify);
                getCurrentActivity().setReFilterOnExit(true);
            }
            case "ADD_RENEWAL" -> {
                Renewal rn = (Renewal) o;
                Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
                q.setParameter("id",rn.getId());
                Activity25 ap = (Activity25) q.getSingleResult();
                au = new Activity25u(ap);
                getActivitiesAllOpen().add(au);
                getCurrentActivity().setReFilterOnExit(true);
            }
            case "ADD_SETUP" -> {
                Setup st = (Setup) o;
                Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
                q.setParameter("id",st.getId());
                Activity25 ap = (Activity25) q.getSingleResult();
                au = new Activity25u(ap);
                List<Activity25u> listToModify = new ArrayList<>(getActivitiesAllOpen());
                listToModify.add(au);
                setActivitiesAllOpen(listToModify);
                getCurrentActivity().setReFilterOnExit(true);
            }
            case "REMOVE_CONTACT" -> {
                Person p = (Person) o;
                List<Person> newList = new ArrayList<>(getCurrentActivity().getAdditionalContacts());
                newList = newList.stream().filter(obj-> !Objects.equals(obj.getId(), p.getId())).collect(Collectors.toList());
                getCurrentActivity().setAdditionalContacts(newList);
            }
            case "ADD_CONTACT" -> {
                Person p = (Person) o;
                boolean hasThatEmail = false;
                if(getCurrentActivity().getAdditionalContacts()==null)
                    getCurrentActivity().setAdditionalContacts(new ArrayList<>());
                if(p.getEmail()!=null)
                  hasThatEmail = getCurrentActivity().getAdditionalContacts().stream().anyMatch(obj->p.getEmail().equalsIgnoreCase(obj.getEmail()));

                if(!hasThatEmail)
                    getCurrentActivity().getAdditionalContacts().add(p);

                hasThatEmail = getCurrentEmail().getRecipientList().stream().anyMatch((obj->p.getEmail().equalsIgnoreCase(obj.getEmail())));
                if(!hasThatEmail)
                    getCurrentEmail().getRecipientList().add(p);
            }
            case "SWAP_CONTACT" -> {
                Person p = (Person) o;
                Person cp = getCurrentActivity().getPrimaryContact();
                getCurrentActivity().setPrimaryContact(p);
                List<Person> newList = getCurrentActivity().getAdditionalContacts().stream().filter(obj-> !Objects.equals(obj.getEmail(), p.getEmail())).collect(Collectors.toList());
                getCurrentActivity().setAdditionalContacts(newList);
                boolean hasThatEmail = getCurrentActivity().getAdditionalContacts().stream().anyMatch(obj->cp.getEmail().equalsIgnoreCase(obj.getEmail()));
                if(!hasThatEmail)
                    getCurrentActivity().getAdditionalContacts().add(cp);
            }
        }

    }

    public Activity25u getActivity25u(EntityManager em, Renewal r){
        Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
        q.setParameter("id",r.getId());
        Activity25 ap = (Activity25) q.getSingleResult();
        return new Activity25u(ap);
    }
    public Activity25u getActivity25u(EntityManager em, net.superiorstate.ams.model.activity.Opportunity o){
        Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
        q.setParameter("id",o.getId());
        Activity25 ap = (Activity25) q.getSingleResult();
        return new Activity25u(ap);
    }
    private int getIndexOfInsertLocation(List<ToDoOut25> toDoOuts, ToDoOut25 t){
        List<ToDoOut25> openList = toDoOuts.stream().filter(obj -> !obj.isComplete()).toList();
        OptionalInt location = IntStream.range(0,openList.size()).filter(i-> openList.get(i).getSortOrder()>t.getSortOrder()).findFirst();
        return location.isPresent() ? location.getAsInt() : openList.size();

    }
    private Activity25u retrieveActivity25uFromList(List<Activity25u> openList, Long a){
        Optional<Activity25u> auo = openList.stream().filter(obj-> Objects.equals(obj.getActivity().getId(), a)).findFirst();
        return auo.orElse(null);
    }
    public ToDoOut25 retrieveToDoOutFromList(List<ToDoOut25> toDoOuts, Long toDoId){
        Optional<ToDoOut25> toDoOutO = toDoOuts.stream().filter(obj->obj.getToDo().getId().equals(toDoId)).findFirst();
        return toDoOutO.orElse(null);
    }
    public Checklist25u retrieveChecklist25uFromList(List<Checklist25u> checklist25us, Long checklistId){
        Optional<Checklist25u> checklist25u = checklist25us.stream().filter(obj->obj.getActivity().getId().equals(checklistId)).findFirst();
        return checklist25u.orElse(null);
    }
    public void splitChecklists(){
        List<Checklist25u> list1;
        list1 = getChecklistsAll().stream().filter(obj->obj.getSortKey()==1).toList();
        setChecklistsCurrent(list1);

        list1 = getChecklistsAll().stream().filter(obj->obj.getSortKey()==2).toList();
        setChecklistsClosed(list1);

        list1 = getChecklistsAll().stream().filter(obj->obj.getSortKey()==3).toList();
        setChecklistsFuture(list1);
    }
    public void applyPresetToFilter(UserFilterPreset preset) {
        ActivityFilter af = getActivityFilter();
        af.setViewRenewal(preset.isViewRenewal());
        af.setViewSetup(preset.isViewSetup());
        af.setViewTicket(preset.isViewTicket());
        af.setViewOpportunity(preset.isViewOpportunity());
        af.setOwnershipFilter(preset.getOwnershipFilter());
        af.setAttentionFilter(preset.getAttentionFilter());
        af.setSortAlphabetically(preset.isSortAlphabetically());

        // When contact tracking is disabled (99), remap contact-dependent filters
        if (getDaysSinceContactWarning() >= 99) {
            int attn = af.getAttentionFilter();
            if (attn == 1) af.setAttentionFilter(2);      // Needs Attention → Waiting on Us
            else if (attn == 3) af.setAttentionFilter(0);  // Needs Contact → Show All
        }
    }

    @Override
    public void close() {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    public class CurrentChecklist{
        private CheckList checkList;
        private List<Note> notes;
        private List<ToDoOut25> toDoList;

        private List<ToDoOut25> currentToDos;
        private List<ToDoOut25> closedToDos;
        private List<ToDoOut25> futureToDos;
        private String reassignUrl;

        public String getReassignUrl() {
            return reassignUrl;
        }

        public void setReassignUrl(String reassignUrl) {
            this.reassignUrl = reassignUrl;
        }

        public CurrentChecklist (){}

        public CheckList getCheckList() {
            return checkList;
        }

        public void setCheckList(CheckList checkList) {
            this.checkList = checkList;
        }

        public List<Note> getNotes() {
            return notes;
        }

        public void setNotes(List<Note> notes) {
            this.notes = notes;
        }

        public List<ToDoOut25> getToDoList() {
            return toDoList;
        }

        public void setToDoList(List<ToDoOut25> toDoList) {
            this.toDoList = toDoList;
        }

        public void initializeCurrentCheckList(EntityManager em, Long id){
            setCheckList(EntityLookup.getCheckListById(em,id));
            setToDoList(getToDosForCurrentActivity(em));
            setNotes(getNotesForActivity(em));
        }
        private List<ToDoOut25> getToDosForCurrentActivity(EntityManager em){
            Query q= em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :id");
            q.setParameter("id",getCheckList().getId());
            List<ToDo> toDos;
            try{
                toDos = (List<ToDo>) q.getResultList();
            } catch (Exception e){return new ArrayList<>();}
            List<ToDoOut25> toDoOut25s = new ArrayList<>();
            for(ToDo t:toDos)
                toDoOut25s.add(new ToDoOut25(t));
            return toDoOut25s;
        }
        private List<Note> getNotesForActivity(EntityManager em){
            Query q= em.createQuery("SELECT n FROM Note n where n.activity.id = :id order by n.id desc");
            q.setParameter("id",getCheckList().getId());
            List<Note> notes;
            try{
                notes = (List<Note>) q.getResultList();
            } catch (Exception e){return new ArrayList<>();}
            return notes;
        }


    }
    public class CurrentActivity{
        private Activity activity;
        private List<Note> notes;
        private List<ToDoOut25> toDoList;
        private List<Activity> pastActivities;
        private List<Employee> employees;
        private Person primaryContact;
        private List<Person> additionalContacts;
        private List<Benefit> benefitsNotInRenewal;
        private boolean reFilterOnExit;
        private CheckList checkList;

        private List<ServiceItem> modsNotInSetup;
        private List<ServiceItem> modsInSetup;
        private List<Proposal> opportunityProposals;
        private List<Proposal> allProspectProposals;
        private List<QuestionnaireInstance> questionnaireInstances;
        private List<Questionnaire> availableQuestionnaires;

        public CurrentActivity(){};

        public Activity getActivity() {
            return activity;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

        public List<Note> getNotes() {
            return notes;
        }

        public void setNotes(List<Note> notes) {
            this.notes = notes;
        }

        public List<ToDoOut25> getToDoList() {
            return toDoList;
        }

        public void setToDoList(List<ToDoOut25> toDoList) {
            this.toDoList = toDoList;
        }

        public List<Activity> getPastActivities() {
            return pastActivities;
        }

        public void setPastActivities(List<Activity> pastActivities) {
            this.pastActivities = pastActivities;
        }

        public List<Employee> getEmployees() {
            return employees;
        }

        public void setEmployees(List<Employee> employees) {
            this.employees = employees;
        }

        public CheckList getCheckList() {
            return checkList;
        }

        public void setCheckList(CheckList checkList) {
            this.checkList = checkList;
        }

        public boolean isReFilterOnExit() {
            return reFilterOnExit;
        }

        public void setReFilterOnExit(boolean reFilterOnExit) {
            this.reFilterOnExit = reFilterOnExit;
        }

        public List<Benefit> getBenefitsNotInRenewal() {
            return benefitsNotInRenewal;
        }

        public void setBenefitsNotInRenewal(List<Benefit> benefitsNotInRenewal) {
            this.benefitsNotInRenewal = benefitsNotInRenewal;
        }

        public Person getPrimaryContact() {
            return primaryContact;
        }

        public void setPrimaryContact(Person primaryContact) {
            this.primaryContact = primaryContact;
        }

        public List<Person> getAdditionalContacts() {
            return additionalContacts;
        }

        public void setAdditionalContacts(List<Person> additionalContacts) {
            this.additionalContacts = additionalContacts;
        }

        public List<ServiceItem> getModsNotInSetup() {
            return modsNotInSetup;
        }

        public void setModsNotInSetup(List<ServiceItem> modsNotInSetup) {
            this.modsNotInSetup = modsNotInSetup;
        }

        public List<ServiceItem> getModsInSetup() {
            return modsInSetup;
        }

        public void setModsInSetup(List<ServiceItem> modsInSetup) {
            this.modsInSetup = modsInSetup;
        }

        public List<Proposal> getOpportunityProposals() {
            return opportunityProposals;
        }

        public void setOpportunityProposals(List<Proposal> opportunityProposals) {
            this.opportunityProposals = opportunityProposals;
        }

        public List<Proposal> getAllProspectProposals() {
            return allProspectProposals;
        }

        public void setAllProspectProposals(List<Proposal> allProspectProposals) {
            this.allProspectProposals = allProspectProposals;
        }

        public List<QuestionnaireInstance> getQuestionnaireInstances() {
            return questionnaireInstances;
        }

        public void setQuestionnaireInstances(List<QuestionnaireInstance> questionnaireInstances) {
            this.questionnaireInstances = questionnaireInstances;
        }

        public List<Questionnaire> getAvailableQuestionnaires() {
            return availableQuestionnaires;
        }

        public void setAvailableQuestionnaires(List<Questionnaire> availableQuestionnaires) {
            this.availableQuestionnaires = availableQuestionnaires;
        }

        public void intializeActivity(EntityManager em, Long activityId){
            long start = System.currentTimeMillis();
            System.out.println("** INITIALIZATION OF ACTIVITY **");
            setActivity(EntityLookup.getActivityById(em,activityId));
            if (getActivity() == null) {
                System.err.println("⚠ intializeActivity: no Activity found for id=" + activityId);
                return;
            }
            if(getActivity().getClass().getSimpleName().equals("CheckList")){
                CheckList c = (CheckList) getActivity();
                setCheckList(c);
                getCurrentChecklist().setCheckList(c);
            } else {
                setCheckList(getChecklistByActivity(em,getActivity()));
                fillPrimaryContacts(em);
            }
            setNotes(getNotesForActivity(em,getActivity()));
            setToDoList(getToDosForCurrentActivity(em,getCheckList()));
            System.out.println("🔑 isPspAdmin = " + isPspAdmin);
            ToDoOut25.computeAllDisplayStates(getToDoList(), getCurrentPerson().getId(), isPspAdmin, getActivity().getAssignedTo().getId());
            if(getActivity().getClass().getSimpleName().equals("Renewal")){
                fillEmployeeList(em);
                setBenefitsNotInRenewal(getBenefitsNotInRenewal(em));
            }
            else if(getActivity().getClass().getSimpleName().equalsIgnoreCase("Ticket")){
                fillEmployeeList(em);
            }
            else if(getActivity().getClass().getSimpleName().equalsIgnoreCase("Setup")) {
                fillModsNotInSetup(em);
                Setup s = (Setup) getActivity();
                setModsInSetup(getModsInSetup(s));
            }
            else if(getActivity().getClass().getSimpleName().equalsIgnoreCase("Opportunity")) {
                Opportunity opp = (Opportunity) getActivity();
                setOpportunityProposals(SalesDAO.getProposalsBySourceActivity(em, opp.getId()));
                if (opp.getProspect() != null) {
                    setAllProspectProposals(SalesDAO.getProposalListFull(em, opp.getProspect()));
                } else {
                    setAllProspectProposals(new ArrayList<>());
                }
            }
            setPastActivities(fillPastActivities(em));

            // Load attached questionnaire instances + available for manual attach
            setQuestionnaireInstances(QuestionnaireService.getInstancesForActivity(em, activityId));
            long pspId = getCurrentPerson().getPsp().getId();
            setAvailableQuestionnaires(QuestionnaireService.getAvailableQuestionnaires(em, pspId, activityId));

            System.out.println("⏱️ intializeActivity took " + (System.currentTimeMillis() - start) + "ms");
        }

        private List<ServiceItem> getModsInSetup(Setup s) {
            if (s == null || s.getApplication() == null || s.getApplication().getApplicationModuleList() == null) {
                return Collections.emptyList();
            }

            return s.getApplication()
                    .getApplicationModuleList()
                    .stream()
                    .map(ApplicationModule::getServiceItem)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }


        private void fillModsNotInSetup(EntityManager em) {
            Setup s = (Setup) getActivity();
            if (s == null || s.getApplication() == null || s.getApplication().getApplicationModuleList() == null) {
                setModsNotInSetup(Collections.emptyList());
                return;
            }

            List<ServiceItem> allMods = em.createQuery(
                    "SELECT t FROM ServiceItem t WHERE t.activityCategory.id = 2 ORDER BY t.sortOrder",
                    ServiceItem.class
            ).getResultList();

            if (allMods.isEmpty()) {
                setModsNotInSetup(Collections.emptyList());
                return;
            }

            Set<ServiceItem> modsInSetup = s.getApplication()
                    .getApplicationModuleList()
                    .stream()
                    .map(ApplicationModule::getServiceItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<ServiceItem> modsNotInSetup = allMods.stream()
                    .filter(tp -> !modsInSetup.contains(tp))
                    .collect(Collectors.toList());

            setModsNotInSetup(modsNotInSetup);
        }



        private List<Benefit> getBenefitsNotInRenewal(EntityManager em){
            Renewal r = (Renewal) getActivity();
            List<Benefit> allEmployerBenefits;
            Query q1 = em.createQuery("SELECT b FROM Benefit b WHERE b.employer.id = :id and b.isActive=true");
            q1.setParameter("id",r.getEmployer().getId());
            try{
                allEmployerBenefits = (List<Benefit>) q1.getResultList();
            } catch (NoResultException e1){
                return new ArrayList<>();
            }
            System.out.println("ALL ER BEN COUNT: " + allEmployerBenefits.size());

            Query q2 = em.createQuery("SELECT r FROM Renewal r WHERE r.isComplete = false AND r.employer.id = :id");
            q2.setParameter("id",r.getEmployer().getId());
            List<Renewal> openRenewals;
            try{
                openRenewals = (List<Renewal>) q2.getResultList();
            } catch (NoResultException e2){
                return allEmployerBenefits;
            }
            if(openRenewals==null || openRenewals.size()==0)
                return allEmployerBenefits;
            List<RenewalItem> allRenewalItems = openRenewals.stream().filter(renewal -> !renewal.isComplete()).flatMap(renewal->renewal.getRenewalItemList().stream()).collect(Collectors.toList());
            System.out.println("ALL REN ITEM COUNT: "+ allRenewalItems.size());
            Set<Integer> associatedBenefitIds = allRenewalItems.stream().map(item -> item.getBenefit().getId()).collect(Collectors.toSet());
            List<Benefit> benefitList = allEmployerBenefits.stream().filter(benefit -> !associatedBenefitIds.contains(benefit.getId())).collect(Collectors.toList());
            System.out.println("BEN LIST FINAL COUNT: "+benefitList.size());
            return benefitList;
        }
        private void fillEmployeeList(EntityManager em){
            List<Employee> employeeList = new ArrayList<>();
            setEmployees(employeeList);
            Employer er;
            if(getActivity().getClass().getSimpleName().equals("Renewal")){
                Renewal r = (Renewal) getActivity();
                er = r.getEmployer();
            } else if(getActivity().getClass().getSimpleName().equals("Ticket")){
                if(getActivity().getPrimaryContact().getEmployee()==null)
                    return;
                Employee ee = getActivity().getPrimaryContact().getEmployee();
                er = ee.getEmployer();
            } else return;
            try{
                Query q = em.createQuery("SELECT e FROM Employee e WHERE e.employer.id = :id order by  e.lastName,e.firstName");
                q.setParameter("id",er.getId());
                employeeList = (List<Employee>) q.getResultList();

            } catch (Exception e){
                return;
            }
            if(employeeList.size()>0)
                setEmployees(employeeList);
        }
        private void fillPrimaryContacts(EntityManager em){
            if(getCurrentActivity()==null || getActivity()==null)
                return;
            //Primary Contact First
            if(getActivity().getPrimaryContact()!=null) {
                System.out.println("** PRIMARY NOT NULL **");
                setPrimaryContact(getActivity().getPrimaryContact());
            }
            else {
                Renewal r = null;
                Setup s = null;
                Ticket t = null;
                if (getActivity().getClass().getSimpleName().equals("Renewal"))
                    r = (Renewal) getActivity();
                else if (getActivity().getClass().getSimpleName().equals("Setup"))
                    s = (Setup) getActivity();
                else if (getActivity().getClass().getSimpleName().equals("Ticket"))
                    t = (Ticket) getActivity();
                if (t != null && t.getContact() != null)
                    setPrimaryContact(t.getContact());
                else if (s != null && s.getPrimaryContactSetup() != null)
                    setPrimaryContact(s.getPrimaryContactSetup());
                else if (getActivity().getAssigneeContactList() != null && getActivity().getAssigneeContactList().size() > 0)
                    setPrimaryContact(getActivity().getAssigneeContactList().get(0));
                else if(r!=null){
                    System.out.println("** Is a Renewal **");
                    if(r.getEmployer()!=null) {
                        if (r.getEmployer().getContactList() != null) {
                            if (r.getEmployer().getContactList().size() > 0) {
                                System.out.println("------------ CHECKING EMPLOYER CONTACT LIST -----------------------");
                                Employee ee = r.getEmployer().getContactList().get(0);
                                Person p = PersonDAO.getPersonByEmployee(em, ee);
                                if (p != null)
                                    setPrimaryContact(p);
                            }
                            else System.out.println("SIZE NOT GREATER THAN ZERO");
                        }
                        else System.out.println("CONTACT LIST IS NULL");
                    }
                    else System.out.println("EMPLOYER IS NULL");

                }
            }
            // Additional Contacts
            List<Person> newList;
            if(getActivity()!=null && getActivity().getAssigneeContactList()!=null && getActivity().getAssigneeContactList().size()>0 ) {
                newList = new ArrayList<>(getActivity().getAssigneeContactList());
                if(newList.contains(getPrimaryContact()))
                    newList.remove(getPrimaryContact());
                setAdditionalContacts(getActivity().getAssigneeContactList());
            }
            else {
                Query q = em.createQuery("SELECt a FROM Activity a JOIN FETCH a.assigneeContactList where a.id = :id");
                q.setParameter("id",getActivity().getId());
                Activity a = null;
                try{
                    a = (Activity) q.getSingleResult();
                } catch (Exception ignored){}
                if(a!=null)
                    em.refresh(a);
                if(a!=null && a.getAssigneeContactList()!=null && a.getAssigneeContactList().size()>0) {
                    newList = new ArrayList<>(a.getAssigneeContactList());
                    if (newList.contains(getPrimaryContact()))
                        newList.remove(getPrimaryContact());
                    setAdditionalContacts(newList);
                }
            }


        }

        private List<Activity> fillPastActivities(EntityManager em){
            List<Activity> pastActivities = new ArrayList<>();
            Activity a = getActivity();
            String dType = a.getClass().getSimpleName();
            Query q;
            try{
                switch (dType) {
                    case "Renewal":
                        Renewal r = (Renewal) a;
                        q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer.id = :eId AND r.id <> :rId order by r.id desc");
                        q.setParameter("eId", r.getEmployer().getId());
                        q.setParameter("rId", r.getId());
                        break;
                    case "Setup":
                        Setup s = (Setup) a;
                        q = em.createQuery("SELECT s FROM Setup s WHERE s.primaryContact.email = :email1 or s.primaryContactSetup.email = :email2 order by s.id desc ");
                        q.setParameter("email1", s.getPrimaryContact().getEmail());
                        q.setParameter("email2", s.getPrimaryContactSetup().getEmail());
                        break;
                    case "Ticket":
                        Ticket t = (Ticket) a;
                        q = em.createQuery("SELECT t FROM Ticket t WHERE t.primaryContact.email = :email or t.contact.email = :email1 order by t.id desc");
                        q.setParameter("email", t.getPrimaryContact().getEmail());
                        q.setParameter("email1",t.getContact().getEmail());
                        break;
                    case "Opportunity":
                        Opportunity opp = (Opportunity) a;
                        if(opp.getProspect() == null) return pastActivities;
                        q = em.createQuery("SELECT o FROM Opportunity o WHERE o.prospect.id = :pId AND o.id <> :oId ORDER BY o.id DESC");
                        q.setParameter("pId", opp.getProspect().getId());
                        q.setParameter("oId", opp.getId());
                        break;
                    default:
                        return pastActivities;
                }
            } catch (Exception ex){
                return pastActivities;
            }

            try{
                pastActivities = (List<Activity>) q.getResultList();
            } catch (NoResultException e){
                return pastActivities;
            }
            return pastActivities;

        }
        private List<ToDoOut25> getToDosForCurrentActivity(EntityManager em, CheckList c){
            if (c == null) return new ArrayList<>();
            Query q= em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id  = :id order by t.isComplete, t.sortOrder, t.id");
            q.setParameter("id",c.getId());
            List<ToDo> toDos;
            try{
                toDos = (List<ToDo>) q.getResultList();
            } catch (Exception e){return new ArrayList<>();}
            List<ToDoOut25> toDoOut25s = new ArrayList<>();
            for(ToDo t: toDos)
                toDoOut25s.add(new ToDoOut25(t));
            return toDoOut25s;
        }
        private List<Note> getNotesForActivity(EntityManager em, Activity a){
            Query q= em.createQuery("SELECT n FROM Note n where n.activity.id = :id order by n.id desc");
            q.setParameter("id",a.getId());
            List<Note> notes;
            try{
                notes = (List<Note>) q.getResultList();
            } catch (Exception e){return new ArrayList<>();}
            return notes;
        }
        private CheckList getChecklistByActivity(EntityManager em, Activity a){
            Query q = em.createQuery("SELECT c FROM  CheckList c WHERE c.assignedTo.id = :id");
            q.setParameter("id",a.getId());
            CheckList c;
            try{
                c = (CheckList) q.getSingleResult();
            } catch (NoResultException e){
                System.err.println("⚠ getChecklistByActivity: no CheckList for activity id=" + a.getId() + " (" + a.getClass().getSimpleName() + ")");
                return null;
            } catch (Exception e){
                System.err.println("⚠ getChecklistByActivity: query failed for activity id=" + a.getId() + " (" + a.getClass().getSimpleName() + "): " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace();
                return null;
            }
            return c;
        }
        private void reSortToDoList() {
            getToDoList().sort(Comparator
                    .comparing(ToDoOut25::isComplete)           // false (open) first
                    .thenComparing(t -> t.getToDo().getSortOrder())
                    .thenComparing(t -> t.getToDo().getId()));
            ToDoOut25.computeAllDisplayStates(getToDoList(), getCurrentPerson().getId(), isPspAdmin, getActivity().getAssignedTo().getId());
        }
        public void clearCurrentActivityContent(){

        }


    }
    public class CurrentEmail{
        private List<Person> recipientList;
        private String subject;
        private String body;
        private Person sender;
        private Activity activity;
        private List<WebLink> attachments;
        private String emailToAdd;

        private String firstName;
        private String lastName;

        private boolean personNotFound;
        private WebLink attachmentToAdd;
        private boolean messageSent;

        private String fileUploadText;

        private Part filePart;

        public CurrentEmail(){}

        public void initializeEmail(){
            setSender(getCurrentPerson());
            clearEmailForm();
        }
        public void clearEmailForm(){
            setRecipientList(new ArrayList<>());
            setSubject("");
            setBody("");
            setActivity(null);
            setAttachments(new ArrayList<>());
            setEmailToAdd(null);
            setAttachmentToAdd(null);
        }

        public List<Person> getRecipientList() {
            return recipientList;
        }

        public void setRecipientList(List<Person> recipientList) {
            this.recipientList = recipientList;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public boolean isPersonNotFound() {
            return personNotFound;
        }

        public void setPersonNotFound(boolean personNotFound) {
            this.personNotFound = personNotFound;
        }

        public Person getSender() {
            return sender;
        }

        public void setSender(Person sender) {
            this.sender = sender;
        }

        public Part getFilePart() {
            return filePart;
        }

        public void setFilePart(Part filePart) {
            this.filePart = filePart;
        }

        public Activity getActivity() {
            return activity;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

        public List<WebLink> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<WebLink> attachments) {
            this.attachments = attachments;
        }

        public String getEmailToAdd() {
            return emailToAdd;
        }

        public void setEmailToAdd(String emailToAdd) {
            this.emailToAdd = emailToAdd;
        }

        public WebLink getAttachmentToAdd() {
            return attachmentToAdd;
        }

        public void setAttachmentToAdd(WebLink attachmentToAdd) {
            this.attachmentToAdd = attachmentToAdd;
        }

        public boolean isMessageSent() {
            return messageSent;
        }

        public void setMessageSent(boolean messageSent) {
            this.messageSent = messageSent;
        }

        public String getFileUploadText() {
            return fileUploadText;
        }

        public void setFileUploadText(String fileUploadText) {
            this.fileUploadText = fileUploadText;
        }

        private Email createEmailNotPersisted(){
            Email e = new Email();
            e.setCreatedBy(getSender());
            e.setRecipientList(getRecipientList());
            e.setSubject(getSubject());
            e.setDetail(getBody());
            e.setDateGenerated(Date.valueOf(LocalDate.now()));
            e.setWebLinkList(getAttachments());
            return e;
        }
        private Email createEmail(EntityManager em){
            em.getTransaction().begin();
            Email email = createEmailNotPersisted();
            email.setStatus(EntityLookup.getActivityStatusById(em,1));
            email.setReasonCreated(EntityLookup.getReasonById(em,7));
            em.persist(email);
            em.getTransaction().commit();
            return email;
        }
        private Message getMessage(AmsDataGlobal global){
            Properties prop = new Properties();
            prop.put("mail.smtp.auth",true);
            prop.put("mail.smtp.starttls.enable","true");
            prop.put("mail.smtp.host",global.getSmtpServer());
            prop.put("mail.smtp.port",global.getSmtpPort());
            prop.put("mail.smtp.ssl.trust",global.getSmtpServer());
            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication(){
                    return new PasswordAuthentication(global.getSmtpUser(),global.getSmtpPassword());
                }
            });
            return new MimeMessage(session);
        }

        private boolean isReadyToSend(){
            if(getSender()==null || getSender().getEmail()==null || !Validator.isValidEmail(getSender().getEmail()))
                return false;
            if(getSubject()==null || getSubject().equals("") || getBody()==null || getBody().equals(""))
                return false;
            if(getRecipientList()==null || getRecipientList().size()==0)
                return false;
            boolean foundValidEmail = false;
            for(Person p: getRecipientList())
                if(p.getEmail()!=null && Validator.isValidEmail(p.getEmail())){
                    foundValidEmail = true;
                    break;
                }
            return foundValidEmail;
        }
        public void sendEmail(EntityManager em, AmsDataGlobal global){
            setMessageSent(true);
            if(!isReadyToSend()){
                setMessageSent(false);
                return;
            }
            try{
                Message m = getMessage(global);
                m.setFrom(new InternetAddress(getSender().getEmail()));
                StringBuilder whoToArray= new StringBuilder();
                for(Person p:getRecipientList()){
                    whoToArray.append(p.getEmail().toLowerCase().trim()).append(",");
                }
                String whoTo = whoToArray.toString();
                whoTo = whoTo.substring(0,whoTo.length()-1);
                InternetAddress[] parse = InternetAddress.parse(whoTo,true);
                m.setRecipients(Message.RecipientType.TO,parse);
                m.setSubject(getSubject());
                MimeBodyPart mimeBodyPart = new MimeBodyPart();
                mimeBodyPart.setContent(getBody(),"text/html; charset=utf-8");
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(mimeBodyPart);
                m.setContent(multipart);
                Transport.send(m);
            } catch (MessagingException e) {
                setMessageSent(false);
                throw new RuntimeException(e);
            }
            Email e = createEmail(em);
            if(getCurrentActivity().getActivity()!=null)
                respondToActivityUpdate(em,"NOTE",e);
        }
        public void fillRecipientList(){
            List<Person> recipients = new ArrayList<>();
            // Add primary contact if valid
            Person primary = getCurrentActivity().getPrimaryContact();
            if (isValidPerson(primary)) {
                recipients.add(primary);
            }

            // Add valid additional contacts
            List<Person> additional = getCurrentActivity().getAdditionalContacts();
            if (additional != null) {
                for (Person p : additional) {
                    if (isValidPerson(p)) {
                        recipients.add(p);
                    }
                }
            }
            setRecipientList(recipients);
        }
        private boolean isValidPerson(Person person) {
            return person != null && person.getEmail() != null && Validator.isValidEmail(person.getEmail());
        }
    }

}
