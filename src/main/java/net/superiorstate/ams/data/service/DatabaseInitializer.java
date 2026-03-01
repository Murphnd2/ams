package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;

import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.PriceItem;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.agency.RateTable;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ServiceModule;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class DatabaseInitializer {
    //Initializing Data Set
    private static String pspName;
    private static String firstName;
    private static String lastName;
    private static String email;
    private static String password;
    private static String address;
    private static String city;
    private static String state;
    private static String zip;
    private static String domain;
    private static String smtpServer;
    private static String smtpUsername;
    private static String smtpPassword;
    private static String smtpPort;
    private static String phone;
    private static String summitPath;
    private static String taxId;

    public static String getTaxId() {
        return taxId;
    }

    public static void setTaxId(String taxId) {
        DatabaseInitializer.taxId = taxId;
    }

    public static String getPspName() {
        return pspName;
    }

    public static void setPspName(String pspName) {
        DatabaseInitializer.pspName = pspName;
    }

    public static String getFirstName() {
        return firstName;
    }

    public static void setFirstName(String firstName) {
        DatabaseInitializer.firstName = firstName;
    }

    public static String getLastName() {
        return lastName;
    }

    public static void setLastName(String lastName) {
        DatabaseInitializer.lastName = lastName;
    }

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String email) {
        DatabaseInitializer.email = email;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        DatabaseInitializer.password = password;
    }

    public static String getAddress() {
        return address;
    }

    public static void setAddress(String address) {
        DatabaseInitializer.address = address;
    }

    public static String getCity() {
        return city;
    }

    public static void setCity(String city) {
        DatabaseInitializer.city = city;
    }

    public static String getState() {
        return state;
    }

    public static void setState(String state) {
        DatabaseInitializer.state = state;
    }

    public static String getZip() {
        return zip;
    }

    public static void setZip(String zip) {
        DatabaseInitializer.zip = zip;
    }

    public static String getDomain() {
        return domain;
    }

    public static void setDomain(String domain) {
        DatabaseInitializer.domain = domain;
    }

    public static String getSmtpServer() {
        return smtpServer;
    }

    public static void setSmtpServer(String smtpServer) {
        DatabaseInitializer.smtpServer = smtpServer;
    }

    public static String getSmtpUsername() {
        return smtpUsername;
    }

    public static void setSmtpUsername(String smtpUsername) {
        DatabaseInitializer.smtpUsername = smtpUsername;
    }

    public static String getSmtpPassword() {
        return smtpPassword;
    }

    public static void setSmtpPassword(String smtpPassword) {
        DatabaseInitializer.smtpPassword = smtpPassword;
    }

    public static String getSmtpPort() {
        return smtpPort;
    }

    public static void setSmtpPort(String smtpPort) {
        DatabaseInitializer.smtpPort = smtpPort;
    }

    public static String getPhone() {
        return phone;
    }

    public static void setPhone(String phone) {
        DatabaseInitializer.phone = phone;
    }

    public static String getSummitPath() {
        return summitPath;
    }

    public static void setSummitPath(String summitPath) {
        DatabaseInitializer.summitPath = summitPath;
    }

    private static void retrieveFormData(HttpServletRequest request){
        setPspName(request.getParameter("pspName"));
        System.out.println(getPspName());
        setFirstName(request.getParameter("firstName"));
        System.out.println(getFirstName());
        setLastName(request.getParameter("lastName"));
        setEmail(request.getParameter("email"));
        setPassword(request.getParameter("password"));
        setAddress(request.getParameter("address"));
        setCity(request.getParameter("city"));
        setState(request.getParameter("state"));
        setZip(request.getParameter("zipCode"));
        setDomain(request.getParameter("domain"));
        setSmtpServer(request.getParameter("smtpServer"));
        setSmtpPort(request.getParameter("smtpPort"));
        setSmtpUsername(request.getParameter("smtpUser"));
        setSmtpPassword(request.getParameter("smtpPwd"));
        setPhone(request.getParameter("phone"));
        setSummitPath(request.getParameter("summit"));
        setTaxId(request.getParameter("taxId"));
    }


    private static void seedSequence(EntityManager em){
        Query q = em.createQuery("SELECT s FROM SequenceTracker s WHERE s.sequenceName= :name");
        q.setParameter("name","SEQ_GEN");
        SequenceTracker s;
        try{
            s = (SequenceTracker) q.getSingleResult();
        } catch (NoResultException e){
            em.getTransaction().begin();
            s = new SequenceTracker();
            s.setSequenceName("SEQ_GEN");
            s.setSequenceCount(1000);
            em.persist(s);
            em.getTransaction().commit();
        }
    }


    public static void initializeDataBase(HttpServletRequest request,EntityManager em)  {

        //Retrieve Form Data
        retrieveFormData(request);
        performInitialization(em);
    }

    /**
     * Core initialization logic — creates all seed data using values already set
     * in the static fields (via retrieveFormData or direct setter calls).
     * Separated so ReSeedDb can call it after setting fields from saved state.
     */
    public static void performInitialization(EntityManager em) {
        System.out.println("Retrieved Data: PSP Name = " + getPspName());
        //Load Sequence Seed Number
        seedSequence(em);
        //Activity Status Listing
        ActivityStatus as = createActivityStatus(em,1,"Waiting on Them");
        createActivityStatus(em,2,"No Change");
        createActivityStatus(em,3,"Waiting on Us");
        //Create First Address
        Address a = createAddress(em,3,address,"",getCity(),getState(),getZip());
        //Create PSP
        PSP psp = createPSP(em,4L,getPspName(),a);
        //Create First Person
        Person p = createMainContact(em,104L,getFirstName(),getLastName(),a,psp,getEmail());
        //Update PSP with Person
        updatePspWithContact(em,psp,p);
        //Create Agency
        Agency agency = createAgency(em,14L,getPspName(),getPhone(),getTaxId(),a,p,psp);
        //Create Employer
        String fullName = getFirstName().trim() + " " + getLastName().trim();
        Employer er = createEmployer(em,-1,getPspName(),getEmail(),fullName,-1);
        //Create Employee
        Employee ee = createEmployee(em,-1,p,er);
        //Assign Person to Employee
        em.getTransaction().begin();
        p.setEmployee(ee);
        em.persist(p);
        em.getTransaction().commit();
        //Create default Billing Group (PSP configures specific groups later)
        BillingGroup bg = createBillingGroup(em,99,"Other");
        //Create Activity Categories (IDs 1-3 hard-referenced for ServiceItem grouping;
        //  4=Opportunity for sales channel activities tracked in activity view)
        ActivityCategory tg1 = createTemplateGroup(em,1,"Renewal");
        ActivityCategory tg2 = createTemplateGroup(em,2,"Setup");
        ActivityCategory tg3 = createTemplateGroup(em,3,"Ticket");
        ActivityCategory tg4 = createTemplateGroup(em,4,"Opportunity");

        //Create Setup Service Items (1:1 with LOS/Enhancement; PSP creates additional as needed)
        ServiceItem siCobra = createServiceItemWithCode(em,1,"COBRA","COBRA",1,tg2,psp);
        ServiceItem siCdh   = createServiceItemWithCode(em,2,"Flexible Spending Accounts","FSA",2,tg2,psp);
        ServiceItem siDebit = createServiceItemWithCode(em,3,"Debit Cards","CARDS",3,tg2,psp);

        //Create Plan Types — each auto-creates a 1:1 Renewal ServiceItem
        PlanType pt1  = createPlanTypeWithRenewal(em,1,"DCA","Dependent Care Account",bg,tg1,psp);
        PlanType pt2  = createPlanTypeWithRenewal(em,2,"FSA","Health Flexible Spending Account",bg,tg1,psp);
        PlanType pt3  = createPlanTypeWithRenewal(em,3,"HRA","Health Reimbursement Arrangement",bg,tg1,psp);
        PlanType pt4  = createPlanTypeWithRenewal(em,4,"HSA","HSA",bg,tg1,psp);
        PlanType pt5  = createPlanTypeWithRenewal(em,5,"LFSA","Limited Purpose FSA",bg,tg1,psp);
        PlanType pt6  = createPlanTypeWithRenewal(em,6,"MERP","Medical Expense Reimbursement Plan",bg,tg1,psp);
        PlanType pt7  = createPlanTypeWithRenewal(em,7,"PRK","Parking Plan",bg,tg1,psp);
        PlanType pt8  = createPlanTypeWithRenewal(em,8,"TRN","Transportation Plan",bg,tg1,psp);
        PlanType pt9  = createPlanTypeWithRenewal(em,9,"Dental","Dental",bg,tg1,psp);
        PlanType pt10 = createPlanTypeWithRenewal(em,10,"EAP","EAP",bg,tg1,psp);
        PlanType pt11 = createPlanTypeWithRenewal(em,11,"Life","Life",bg,tg1,psp);
        PlanType pt12 = createPlanTypeWithRenewal(em,12,"Medical","Medical",bg,tg1,psp);
        PlanType pt13 = createPlanTypeWithRenewal(em,13,"Pharmacy","Pharmacy",bg,tg1,psp);
        PlanType pt14 = createPlanTypeWithRenewal(em,14,"Vision","Vision",bg,tg1,psp);
        PlanType pt15 = createPlanTypeWithRenewal(em,15,"NEFSA","NEFSA",bg,tg1,psp);
        PlanType pt16 = createPlanTypeWithRenewal(em,16,"LSA","Lifestyle Spending Account",bg,tg1,psp);

        //Benefits: not seeded — populated via Summit import. SeedDemoData creates demo benefits.

        //Create Contact Methods (FK on Ticket, used in CreateTicket25 and AmsDataGlobal)
        createContactMethod(em,1,"Phone");
        createContactMethod(em,2,"Email");
        createContactMethod(em,3,"Mail");
        createContactMethod(em,4,"Walk-in");

        //Create Days of Week
        createDayOfWeek(em,1,"Monday");
        createDayOfWeek(em,2,"Tuesday");
        createDayOfWeek(em,3,"Wednesday");
        createDayOfWeek(em,4,"Thursday");
        createDayOfWeek(em,5,"Friday");
        createDayOfWeek(em,6,"Saturday");
        createDayOfWeek(em,7,"Sunday");

        //Create Link Types (IDs 1-3 hard-referenced by WebLink, attachment, and URL servlets)
        createLinkType(em,1,"File Upload");
        createLinkType(em,2,"Hyperlink");
        createLinkType(em,3,"Insert Link");

        //Create Lines of Service (1:1 with Setup ServiceItems; PSP adds more)
        LOS losCobra = createLos(em,1L,"COBRA Administration","COBRA",psp,siCobra);
        LOS losCdh   = createLos(em,2L,"Consumer Directed Healthcare","CDH",psp,siCdh);

        //Create Enhancement (1:1 with Setup ServiceItem)
        Enhancement enhDebit = createEnhancement(em,1L,"Debit Cards","CARDS",psp,siDebit);

        //Create Service Modules (matching LOS + enhancement; PSP configures pricing via sales channel)
        ServiceModule smCobra = createServiceModule(em,1L,"COBRA Administration","COBRA",100,psp);
        ServiceModule smCdh   = createServiceModule(em,2L,"Flexible Spending Accounts","FSA",200,psp);
        ServiceModule smCards = createServiceModule(em,3L,"Debit Card Services","Cards",300,psp);

        //Associate Service Modules to LOS
        associateModuleToLos(em,losCobra,smCobra);
        associateModuleToLos(em,losCdh,smCdh);
        associateModuleToLos(em,losCdh,smCards);

        //Create Price Items (standard fee structure)
        PriceItem pi1 = createPriceItem(em,1L,"Setup (One-time) Fee",100,psp);
        PriceItem pi2 = createPriceItem(em,2L,"Annual Administration Fee",200,psp);
        PriceItem pi3 = createPriceItem(em,3L,"Base Monthly Fee per Participant",300,psp);

        //Create Rate
        Rate rate = createRate(em,1L,"Standard Rate",psp);

        //Create Reasons Created List
        ReasonCreated rc = createReasonCreated(em,1,"Internal Note",false);
        createReasonCreated(em,2,"Received Call",false);
        createReasonCreated(em,3,"Received Voicemail",false);
        createReasonCreated(em,4,"Received Email",false);
        createReasonCreated(em,5,"Made Call",true);
        createReasonCreated(em,6,"Left Voicemail",true);
        createReasonCreated(em,7,"Sent Email Message",true);
        createReasonCreated(em,8,"Quick Action",true);

        //Create Task Frequencies
        createTaskFrequency(em,1,"Daily (Weekdays)");
        createTaskFrequency(em,2,"Weekly");
        createTaskFrequency(em,3,"Bi-Weekly");
        createTaskFrequency(em,4,"1st and 15th");
        createTaskFrequency(em,5,"15th and Last");
        createTaskFrequency(em,6,"Monthly");
        createTaskFrequency(em,7,"Quarterly");
        createTaskFrequency(em,8,"Semi-Annually");
        createTaskFrequency(em,9,"Annually");
        createTaskFrequency(em,10,"First Monday of Month");
        createTaskFrequency(em,11,"First Tuesday of Month");
        createTaskFrequency(em,12,"First Wednesday of Month");
        createTaskFrequency(em,13,"First Thursday of Month");
        createTaskFrequency(em,14,"First Friday of Month");
        createTaskFrequency(em,15,"Last Monday of Month");
        createTaskFrequency(em,16,"Last Tuesday of Month");
        createTaskFrequency(em,17,"Last Wednesday of Month");
        createTaskFrequency(em,18,"Last Thursday of Month");
        createTaskFrequency(em,19,"Last Friday of Month");

        //Create Sentinel Tasks — required by activity creation servlets as fallback/close tasks
        createTask(em,129L,"System Close","",psp,p,false);
        createTask(em,153L,"System Close","",psp,p,false);

        //Create Ticket Category + Service Item (PSP defines additional categories)
        TicketCategory tc = createTicketCategory(em,1L,"General","GEN");
        ServiceItem siTicket = createServiceItem(em,10,"General Ticket",1,tg3,psp);
        siTicket = setHasRequiredTasks(em, siTicket, true);
        setTicketCategoryOnServiceItem(em, siTicket, tc);
        createRequiredTaskList(em,siTicket,psp);

        //Create Initial Time Log Entry
        createTimeEntry(em,p);
        //Create User
        User user = createUser(em,p,getEmail(),getPassword());
        //Create User Roles
        UserRole ur1 = createUserRole(em,1,"PSP User");
        UserRole ur2 = createUserRole(em,2,"Agent");
        UserRole ur3 = createUserRole(em,3,"Client");
        UserRole ur4 = createUserRole(em,4,"Applicant");
        UserRole ur5 = createUserRole(em,5,"PSP Admin");
        UserRole ur8 = createUserRole(em,8,"Agency Admin");
        UserRole ur9 = createUserRole(em,9,"PSP Super User");
        UserRole ur12 = createUserRole(em,102, "BPO Admin");
        UserRole ur13 = createUserRole(em,103, "BPO User");
        //Assign User To PSP Roles
        assignRoles(em,user,ur5,ur1);
        // Seed default filter presets for the new user
        seedFilterPresets(em, user);
        // Vendor users removed — configure via admin UI (future backlog item)
        // Add PSP Constants
        addPspConstants(em);
        // Seed baseline Application Sections (Company, Contact, Address)
        createApplicationSections(em, psp);
        // Create Initialization Checklist
        createInitializationChecklist(em);
        // Set Note To Show Initialization Completed
        setInitializationNote(em,p,rc,siTicket,as);

    }

    public static void seedDemoData(EntityManager em, String demoTag) {
        System.out.println("🎭 seedDemoData called with tag: " + demoTag);
        switch (demoTag.toUpperCase()) {
            // Future: case "DEMO_SALES": seedSalesDemo(em); break;
            // Future: case "DEMO_FULL": seedFullDemo(em); break;
            default:
                System.out.println("⚠️ Unknown demo tag: " + demoTag + " — skipping");
        }
    }

    /**
     * Creates the onboarding checklist for Summit-to-AMS data transfer.
     * Steps guide the PSP admin through the full initial data pipeline.
     */
    private static void createInitializationChecklist(EntityManager em) {
        Person person = EntityLookup.getPersonById(em, 104L);
        PSP psp = EntityLookup.getPspById(em, 4L);
        LinkType hyperlinkType = em.find(LinkType.class, 2);

        em.getTransaction().begin();

        CheckList checkList = new CheckList();
        checkList.setId(29L);
        checkList.setDueDate(Date.valueOf(LocalDate.now().plusDays(30)));
        checkList.setAssignedTo(person);
        checkList.setFullName("Summit Data Transfer");
        checkList.setLoggedBy(person);

        List<ToDo> toDoList = new ArrayList<>();

        // Step 1: Setup exports in Summit
        toDoList.add(createOnboardingStep(em, checkList, psp, 28L,
                "Setup Exports in Summit",
                "Configure Summit to generate the required export files (J1–J4, J7, Plan Types).",
                SUMMIT_EXPORT_SETUP_LINK, "Summit Export Setup Guide", hyperlinkType, 10));

        // Step 2: Download plan types from Summit
        toDoList.add(createOnboardingStep(em, checkList, psp, 30L,
                "Download Plan Types from Summit",
                "Export the Plan Type list from Summit and save locally for upload.",
                SUMMIT_PLANTYPE_DOWNLOAD_LINK, "Plan Type Download Guide", hyperlinkType, 20));

        // Step 3: Import Summit exports into AMS
        toDoList.add(createOnboardingStep(em, checkList, psp, 32L,
                createAnchor("SummitImport", "Import Summit Exports into AMS"),
                "Upload J1–J4, J7, and Plan Type files via the Summit Import Wizard.",
                null, null, null, 30));

        // Step 4: Modify benefit renewal frequencies
        toDoList.add(createOnboardingStep(em, checkList, psp, 34L,
                "Modify Benefit Renewal Frequencies",
                "Review imported benefits and adjust renewal month intervals as needed.",
                null, null, null, 40));

        checkList.setToDoList(toDoList);
        em.persist(checkList);
        em.getTransaction().commit();
    }

    //TODO: Update doc links when PSP-facing guides are published
    private static final String SUMMIT_EXPORT_SETUP_LINK =
            "https://docs.google.com/document/d/1Z8I_-5z53AiDNZu2B6wiMe8yRcOK1pZ6B53TBJl3pHg/edit?usp=sharing";
    private static final String SUMMIT_PLANTYPE_DOWNLOAD_LINK =
            "https://docs.google.com/document/d/1Z8I_-5z53AiDNZu2B6wiMe8yRcOK1pZ6B53TBJl3pHg/edit?usp=sharing";

    private static ToDo createOnboardingStep(EntityManager em, CheckList checklist, PSP psp, Long taskId,
                                              String description, String notes,
                                              String linkUrl, String linkText, LinkType linkType, int sortOrder) {
        Task task = new Task();
        task.setId(taskId);
        task.setPsp(psp);
        task.setDescription(description);
        task.setReUsable(false);
        task.setHasAutomation(false);

        if (linkUrl != null && linkType != null) {
            WebLink infoLink = new WebLink();
            infoLink.setActive(true);
            infoLink.setLinkPath(linkUrl);
            infoLink.setPlainText(linkText);
            infoLink.setLinkType(linkType);
            em.persist(infoLink);
            task.setHasInfo(true);
            task.setInfoLink(infoLink);
        }

        em.persist(task);

        ToDo toDo = new ToDo();
        toDo.setId(taskId + 1L);
        toDo.setTask(task);
        toDo.setCheckList(checklist);
        toDo.setSortOrder(sortOrder);
        em.persist(toDo);

        return toDo;
    }

    private static String createAnchor(String servletName, String description) {
        return "<a href=\"" + servletName + "\">" + description + "</a>";
    }

    private static void createRequiredTaskList(EntityManager em, ServiceItem tp, PSP psp){
        em.getTransaction().begin();
        RequiredTaskList rtl = new RequiredTaskList();
        rtl.setPsp(psp);
        rtl.setDescription(tp.getDescription());
        rtl.setServiceItem(tp);
        rtl.setInActive(false);
        em.persist(rtl);
        em.getTransaction().commit();
    }

    private static void addPspConstants(EntityManager em){
        if(getConstantByName(em,"FALSE_CLOSE")==null)
            createConstant(em,"FALSE_CLOSE",Date.valueOf(LocalDate.of(2000,1,1)).toString());

        if(getConstantByName(em,"SMTP_PASSWORD")==null)
            createConstant(em,"SMTP_PASSWORD",getSmtpPassword());
        if(getConstantByName(em,"SMTP_PORT")==null)
            createConstant(em,"SMTP_PORT",getSmtpPort());
        if(getConstantByName(em,"SMTP_SERVER")==null)
            createConstant(em,"SMTP_SERVER",getSmtpServer());
        if(getConstantByName(em,"SMTP_USER")==null)
            createConstant(em,"SMTP_USER",getSmtpUsername());
        if(getConstantByName(em,"WEB_PATH")==null)
            createConstant(em,"WEB_PATH",getDomain());
        if(getConstantByName(em,"SUMMIT_PATH")==null)
            createConstant(em,"SUMMIT_PATH",getSummitPath());
        if(getConstantByName(em,"SSL_PORT")==null)
            createConstant(em,"SSL_PORT","443");

        // Logo and favicon (defaults — PSP can upload custom via admin)
        if(getConstantByName(em,"LOGO_NAVBAR")==null)
            createConstant(em,"LOGO_NAVBAR","/images/logoA.png");
        if(getConstantByName(em,"LOGO_LOGIN")==null)
            createConstant(em,"LOGO_LOGIN","/images/logoA.png");
        if(getConstantByName(em,"FAVICON")==null)
            createConstant(em,"FAVICON","/favicon.ico");
        if(getConstantByName(em,"EMAIL_FOOTER_TEXT")==null)
            createConstant(em,"EMAIL_FOOTER_TEXT",getPspName());
        if(getConstantByName(em,"USE_TIMECLOCK")==null)
            createConstant(em,"USE_TIMECLOCK","true");
    }

    private static void createConstant(EntityManager em, String name, String value){
        em.getTransaction().begin();
        Constant c = new Constant();
        c.setName(name);
        c.setValue(value);
        em.persist(c);
        em.getTransaction().commit();
    }

    private static Constant getConstantByName(EntityManager em, String name){
        Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = :name");
        q.setParameter("name",name);
        List<Constant> constantList;
        try{
            constantList = (List<Constant>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(constantList.size()==0)
            return null;
        return constantList.get(0);
    }

    private static void assignRoles(EntityManager em, User u, UserRole adminUser, UserRole basicUser){
        em.getTransaction().begin();
        u.addUserToRole(adminUser);
        u.addUserToRole(basicUser);
        em.persist(u);
        em.getTransaction().commit();
    }

    private static void assignRoles(EntityManager em, User u, UserRole ur){
        em.getTransaction().begin();
        u.addUserToRole(ur);
        em.persist(u);
        em.getTransaction().commit();
    }

    private static void setInitializationNote(EntityManager em, Person p, ReasonCreated reasonCreated, ServiceItem serviceItem, ActivityStatus as){

        em.getTransaction().begin();
        Ticket t = new Ticket();
        t.setDescription("Initialization of Database");
        t.setFullName("Initialization of Database");
        t.setAssignedTo(p);
        t.setComplete(true);
        t.setId(99L);
        t.setLoggedBy(p);
        t.setDateCompleted(Date.valueOf(LocalDate.now()));
        t.setDueDate(t.getDateCompleted());
        t.setTicketServiceItem(serviceItem);
        t.setContact(p);
        t.setPrimaryContact(p);
        t.setCompletedBy(p);
        em.persist(t);
        em.getTransaction().commit();
        em.getTransaction().begin();
        Note n = new Note();
        n.setId(1L);
        n.setDetail("Initialization");
        n.setStatus(as);
        n.setReasonCreated(reasonCreated);
        n.setCreatedBy(p);
        n.setDateGenerated(t.getDateCompleted());
        em.persist(n);
        em.getTransaction().commit();
    }

    private static Note getNoteById(EntityManager em, Long id){
        Query q = em.createQuery("SELECT n FROM Note n WHERE n.id = :id");
        q.setParameter("id",id);
        Note n;
        try{
            n = (Note) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return n;
    }

    public static UserRole createUserRole(EntityManager em, int id, String name){
        if(EntityLookup.getUserRoleById(em,id)!=null)
            return EntityLookup.getUserRoleById(em,id);
        em.getTransaction().begin();
        UserRole u = new UserRole();
        u.setDescription(name);
        u.setId(id);
        em.persist(u);
        em.getTransaction().commit();
        return u;
    }

    public static User createUser(EntityManager em, Person p, String email, String pw) {
        em.getTransaction().begin();
        User u = new User();
        u.setUserName(email);
        u.setEmail(email);
        u.setPerson(p);
        u.setEmailVerified(true);
        u.setTempGuid(UUID.randomUUID().toString());
        u.setGuidExpiration(Date.valueOf(LocalDate.now()));
        String salt = null;
        try {
            salt = AuthDAO.generateSalt();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String passwordHash = null;
        try {
            passwordHash = AuthDAO.generatePasswordHash(pw,salt);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        u.setSalt(salt);
        u.setPasswordHash(passwordHash);
        u.setAllowSetPassword(false);
        u.setGuidUsed(true);
        try{
            em.persist(u);
            em.getTransaction().commit();
        } catch (Exception e){
            if(em.getTransaction().isActive())
                em.getTransaction().rollback();
            Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :email");
            q.setParameter("email",email);
            return (User) q.getSingleResult();
        }

        return u;
    }

    public static void createTimeEntry(EntityManager em, Person p){
        createTimeEntry(em,p,1L,2L);
    }

    public static void createTimeEntry(EntityManager em, Person p,Long one, Long two){
        em.getTransaction().begin();
        TimeLog t = new TimeLog();
        if(one!=null)
            t.setId(one);
        t.setIn(true);
        t.setPunchDate(Date.valueOf(LocalDate.of(2022,1,1)));
        t.setPunchTime(Time.valueOf(LocalTime.now()));
        t.setPerson(p);
        em.persist(t);
        em.getTransaction().commit();
        em.getTransaction().begin();
        t = new TimeLog();
        if(two!=null)
            t.setId(two);
        t.setIn(false);
        t.setPunchDate(Date.valueOf(LocalDate.of(2022,1,1)));
        t.setPunchTime(Time.valueOf(LocalTime.now().plusSeconds(1L)));
        t.setPerson(p);
        em.persist(t);
        em.getTransaction().commit();
    }

    private static void setTicketCategoryOnServiceItem(EntityManager em, ServiceItem si, TicketCategory tc){
        em.getTransaction().begin();
        si.setTicketCategory(tc);
        em.persist(si);
        em.getTransaction().commit();
    }

    public static TicketCategory createTicketCategory(EntityManager em, Long id, String name, String shortText){
        if(EntityLookup.getTicketCategoryById(em,id)!=null)
            return EntityLookup.getTicketCategoryById(em,id);
        em.getTransaction().begin();
        TicketCategory t = new TicketCategory();
        t.setId(id);
        t.setDescription(name);
        t.setShortText(shortText);
        t.setActive(true);
        em.persist(t);
        em.getTransaction().commit();
        return t;
    }

    public static TaskFrequency createTaskFrequency(EntityManager em, int id, String name){
        if(EntityLookup.getTaskFrequencyById(em,id,true)!=null)
            return EntityLookup.getTaskFrequencyById(em,id,true);
        em.getTransaction().begin();
        TaskFrequency t = new TaskFrequency();
        t.setId(id);
        t.setDescription(name);
        em.persist(t);
        em.getTransaction().commit();
        return t;
    }

    public static Task createTask(EntityManager em, Long id, String name, String servletName, PSP psp, Person p, boolean hasAutomation){
        if(EntityLookup.getTaskById(em,id)!=null)
            return EntityLookup.getTaskById(em,id);
        em.getTransaction().begin();
        Task t = new Task();
        t.setId(id);
        t.setDescription(name);
        t.setReUsable(false);
        t.setPsp(psp);
        t.setHasAutomation(hasAutomation);
        t.setServletName(servletName);
        t.setAutomationText(name);
        t.setOwner(p);
        t.setHasOwner(true);
        t.setHasGoTo(false);
        t.setHasInfo(false);
        t.setSourced(false);
        t.setAllowNonOwner(true);
        t.setAllowEarly(false);
        t.setAllowFuture(false);
        em.persist(t);
        em.getTransaction().commit();
        return t;
    }

    public static Task createTask(EntityManager em, Long id, String name, String servletName,PSP psp, Person p){
        return createTask(em,id,name,servletName,psp,p,true);
    }

    public static ReasonCreated createReasonCreated(EntityManager em, int id, String name, boolean outbound){
        if(EntityLookup.getReasonById(em,id)!=null)
            return EntityLookup.getReasonById(em,id);
        em.getTransaction().begin();
        ReasonCreated r  = new ReasonCreated();
        r.setId(id);
        r.setDescription(name);
        r.setOutbound(outbound);
        em.persist(r);
        em.getTransaction().commit();
        return r;
    }

    private static void seedFilterPresets(EntityManager em, User user) {
        em.getTransaction().begin();

        UserFilterPreset p1 = new UserFilterPreset();
        p1.setUser(user);
        p1.setSlotNumber(1);
        p1.setLabel("My Actionable");
        p1.setViewRenewal(true);
        p1.setViewSetup(true);
        p1.setViewTicket(true);
        p1.setViewOpportunity(true);
        p1.setOwnershipFilter(1);
        p1.setAttentionFilter(1);
        p1.setSortAlphabetically(false);
        em.persist(p1);

        UserFilterPreset p2 = new UserFilterPreset();
        p2.setUser(user);
        p2.setSlotNumber(2);
        p2.setLabel("All Open");
        p2.setViewRenewal(true);
        p2.setViewSetup(true);
        p2.setViewTicket(true);
        p2.setViewOpportunity(true);
        p2.setOwnershipFilter(0);
        p2.setAttentionFilter(0);
        p2.setSortAlphabetically(true);
        em.persist(p2);

        UserFilterPreset p3 = new UserFilterPreset();
        p3.setUser(user);
        p3.setSlotNumber(3);
        p3.setLabel("My Renewals");
        p3.setViewRenewal(true);
        p3.setViewSetup(false);
        p3.setViewTicket(false);
        p3.setViewOpportunity(false);
        p3.setOwnershipFilter(1);
        p3.setAttentionFilter(0);
        p3.setSortAlphabetically(true);
        em.persist(p3);

        em.getTransaction().commit();
    }

    private static void createPricing(EntityManager em, double price, ServiceModule sm, PriceItem pi, Rate r){
        em.getTransaction().begin();
        RateTable rt = new RateTable();
        rt.setPrice(price);
        rt.setModule(sm);
        rt.setPriceItem(pi);
        rt.setRate(r);
        em.persist(rt);
        em.getTransaction().commit();
        em.getTransaction().begin();
        r.getRateTable().add(rt);
        em.persist(r);
        em.getTransaction().commit();
    }

    public static Rate createRate(EntityManager em, Long id, String name, PSP psp){
        if(EntityLookup.getRateById(em,id,true)!=null)
            return EntityLookup.getRateById(em,id,true);
        em.getTransaction().begin();
        Rate r = new Rate();
        r.setId(id);
        r.setDescription(name);
        r.setPsp(psp);
        r.setSuppressed(false);
        em.persist(r);
        em.getTransaction().commit();
        return r;
    }

    public static PriceItem createPriceItem(EntityManager em, Long id,String name, int sortOrder, PSP psp){
        if(EntityLookup.getPriceItemById(em,id)!=null)
            return EntityLookup.getPriceItemById(em,id);
        em.getTransaction().begin();
        PriceItem p = new PriceItem();
        p.setSortOrder(sortOrder);
        p.setId(id);
        p.setDescription(name);
        p.setPsp(psp);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    public static void associateModuleToLos(EntityManager em, LOS los, ServiceModule sm){
        em.getTransaction().begin();
        los.addServiceModule(sm);
        sm.getListOfLosWithThisModule().add(los);
        em.persist(los);
        em.persist(sm);
        em.getTransaction().commit();
    }

    public static ServiceModule createServiceModule(EntityManager em, Long id, String name,String shortText, int sortOrder, PSP psp){
        if(EntityLookup.getServiceModuleById(em,id.intValue(),true)!=null)
            return EntityLookup.getServiceModuleById(em,id.intValue(),true);
        em.getTransaction().begin();
        ServiceModule s = new ServiceModule();
        s.setDescription(name);
        s.setId(id);
        s.setShortText(shortText);
        s.setSortOrder(sortOrder);
        s.setPsp(psp);
        em.persist(s);
        em.getTransaction().commit();
        return s;
    }

    public static LOS createLos(EntityManager em, long id, String name, String shortText, PSP psp, ServiceItem si){
        if(EntityLookup.getLosById(em,id)!=null)
            return EntityLookup.getLosById(em,id);
        em.getTransaction().begin();
        LOS l = new LOS();
        l.setId(id);
        l.setDescription(name);
        l.setShortText(shortText);
        l.setPsp(psp);
        l.setServiceItem(si);
        em.persist(l);
        em.getTransaction().commit();
        return l;
    }

    public static Enhancement createEnhancement(EntityManager em, long id, String name, String shortText, PSP psp, ServiceItem si){
        em.getTransaction().begin();
        Enhancement e = new Enhancement();
        e.setId(id);
        e.setDescription(name);
        e.setShortText(shortText);
        e.setPsp(psp);
        e.setServiceItem(si);
        em.persist(e);
        em.getTransaction().commit();
        return e;
    }

    public static LinkType createLinkType(EntityManager em, int id, String name){
        if(EntityLookup.getLinkTypeById(em,id)!=null)
            return EntityLookup.getLinkTypeById(em,id);
        em.getTransaction().begin();
        LinkType lt = new LinkType();
        lt.setId(id);
        lt.setTypeName(name);
        em.persist(lt);
        em.getTransaction().commit();
        return lt;
    }

    public static DoW createDayOfWeek(EntityManager em, int id, String name){
        em.getTransaction().begin();
        DoW d = new DoW();
        try{
            d.setId(id+10);
            d.setWeekdayId(id);
            d.setName(name);
            em.persist(d);
            em.getTransaction().commit();
        } catch (Exception e){
            if(em.getTransaction().isActive())
                em.getTransaction().rollback();
            Query q = em.createQuery("SELECT d FROM DoW d WHERE d.weekdayId=:id");
            return (DoW) q.getSingleResult();
        }
        return d;
    }

    public static ContactMethod createContactMethod(EntityManager em, int id, String name){
        em.getTransaction().begin();
        ContactMethod cm = new ContactMethod();
        cm.setId(id);
        cm.setDescription(name);
        try{
            em.persist(cm);
            em.getTransaction().commit();
        } catch (Exception e){
            if(em.getTransaction().isActive())
                em.getTransaction().rollback();
            Query q = em.createQuery("SELECT c FROM ContactMethod c WHERE c.id = :id");
            q.setParameter("id",id);
            return (ContactMethod) q.getSingleResult();
        }
        return cm;
    }

    public static Benefit createBenefit(EntityManager em, int summitId, String sourceType, String name, Employer er, PlanType pt, Date theDate){
        LocalDate currentDate = theDate.toLocalDate();
        LocalDate effectiveDate = currentDate.minusYears(1L);
        Date dateEffective = Date.valueOf(effectiveDate);

        Benefit existing = EntityLookup.getBenefitBySummitKey(em, sourceType, summitId);
        if(existing != null)
            return existing;
        em.getTransaction().begin();
        Benefit b = new Benefit();
        b.setSummitId(summitId);
        b.setSourceType(sourceType);
        b.setEffectiveDate(dateEffective);
        b.setNextRenewalDue(theDate);
        b.setPlanDescription(name);
        b.setPlanName(name);
        b.setEmployer(er);
        b.setActive(true);
        b.setPlanType(pt);
        b.setPbBenId(0);
        em.persist(b);
        em.getTransaction().commit();
        return b;
    }

    public static PlanType createPlanType(EntityManager em, int id, String code, String name, BillingGroup bg, ServiceItem tp){
        if(EntityLookup.getPlanTypeById(em,id)!=null)
            return EntityLookup.getPlanTypeById(em,id);
        em.getTransaction().begin();
        PlanType pt = new PlanType();
        pt.setPlanTypeId(id);
        pt.setCode(code);
        pt.setPlanTypeName(name);
        pt.setBillingGroup(bg);
        pt.setServiceItem(tp);
        em.persist(pt);
        em.getTransaction().commit();
        return pt;
    }

    /**
     * Creates a PlanType and its 1:1 Renewal ServiceItem in one step.
     * Mirrors the auto-creation pattern used during Summit import.
     */
    public static PlanType createPlanTypeWithRenewal(EntityManager em, int id, String code, String name,
                                                      BillingGroup bg, ActivityCategory renewalCategory, PSP psp){
        if(EntityLookup.getPlanTypeById(em,id)!=null)
            return EntityLookup.getPlanTypeById(em,id);
        // Create the 1:1 Renewal ServiceItem first
        ServiceItem si = new ServiceItem();
        si.setActivityCategory(renewalCategory);
        si.setSortOrder(id);
        si.setDescription(name);
        si.setCode(code);
        si.setPsp(psp);
        si.setSourceType("SYSTEM");
        si.setProviderRef(String.valueOf(id));
        si.setDefaultRenewalMonths(12);
        em.getTransaction().begin();
        em.persist(si);
        em.getTransaction().commit();
        // Create PlanType linked to that ServiceItem
        em.getTransaction().begin();
        PlanType pt = new PlanType();
        pt.setPlanTypeId(id);
        pt.setCode(code);
        pt.setPlanTypeName(name);
        pt.setBillingGroup(bg);
        pt.setServiceItem(si);
        em.persist(pt);
        em.getTransaction().commit();
        return pt;
    }

    public static ServiceItem createServiceItemWithCode(EntityManager em, int id, String name, String code, int sortOrder, ActivityCategory tg, PSP psp){
        if(EntityLookup.getServiceItemById(em,id,true)!=null)
            return EntityLookup.getServiceItemById(em,id,true);
        em.getTransaction().begin();
        ServiceItem tp = new ServiceItem();
        tp.setActivityCategory(tg);
        tp.setSortOrder(sortOrder);
        tp.setDescription(name);
        tp.setCode(code);
        tp.setId(id);
        tp.setPsp(psp);
        tp.setSourceType("SYSTEM");
        em.persist(tp);
        em.getTransaction().commit();
        return tp;
    }

    public static ServiceItem createServiceItem(EntityManager em, int id, String name, int sortOrder, ActivityCategory tg, PSP psp){
        if(EntityLookup.getServiceItemById(em,id,true)!=null)
            return EntityLookup.getServiceItemById(em,id,true);
        em.getTransaction().begin();
        ServiceItem tp = new ServiceItem();
        tp.setActivityCategory(tg);
        tp.setSortOrder(sortOrder);
        tp.setDescription(name);
        tp.setId(id);
        tp.setPsp(psp);
        tp.setSourceType("SYSTEM");
        em.persist(tp);
        em.getTransaction().commit();
        return tp;
    }

    private static ServiceItem setHasRequiredTasks(EntityManager em, ServiceItem si, boolean value){
        em.getTransaction().begin();
        si.setHasRequiredTasks(value);
        em.merge(si);
        em.getTransaction().commit();
        return si;
    }

    public static ActivityCategory createTemplateGroup(EntityManager em, int id, String name){
        if(EntityLookup.getTemplateGroupById(em,id)!=null)
            return EntityLookup.getTemplateGroupById(em,id);
        em.getTransaction().begin();
        ActivityCategory tg = new ActivityCategory();
        tg.setDescription(name);
        tg.setId(id);
        em.persist(tg);
        em.getTransaction().commit();
        return tg;
    }

    public static BillingGroup createBillingGroup(EntityManager em, int id, String name){
        if(EntityLookup.getBillingGroupById(em,id,true)!=null)
            return EntityLookup.getBillingGroupById(em,id,true);
        em.getTransaction().begin();
        BillingGroup bg = new BillingGroup();
        bg.setId(id);
        bg.setDescription(name);
        em.persist(bg);
        em.getTransaction().commit();
        return bg;
    }

    public static Employee createEmployee(EntityManager em, int id, Person p, Employer er){
        if(EntityLookup.getEmployeeById(em,id,true)!=null)
            return EntityLookup.getEmployeeById(em,id,true);
        em.getTransaction().begin();
        Employee ee = new Employee();
        ee.setId(id);
        ee.setFirstName(p.getFirstName());
        ee.setLastName(p.getLastName());
        ee.setAddress1(p.getAddress().getAddress1());
        ee.setAddress2(p.getAddress().getAddress2());
        ee.setCity(p.getAddress().getCity());
        ee.setState(p.getAddress().getState());
        ee.setZipCode(p.getAddress().getZipCode());
        ee.setMmKey(id);
        ee.setActive(true);
        ee.setEmployer(er);
        em.persist(ee);
        em.getTransaction().commit();
        return ee;
    }

    public static Employer createEmployer(EntityManager em, int id, String name, String email, String contactName, int erKey ){
        if(EntityLookup.getEmployerById(em,id,true)!=null)
            return EntityLookup.getEmployerById(em,id,true);
        em.getTransaction().begin();
        Employer er = new Employer();
        er.setEmployerName(name);
        er.setActive(true);
        er.setId(id);
        er.setHasPop(true);
        er.setHasPb(true);
        er.setHasCdh(true);
        er.setEmail(email);
        er.setContactName(contactName);
        er.setErKey(erKey);
        er.setAltId(id);
        er.setBillable(false);
        em.persist(er);
        em.getTransaction().commit();
        return er;
    }

    private static void updatePspWithContact(EntityManager em, PSP psp, Person p){
        PSP thePsp = EntityLookup.getPspById(em,psp.getId());
        em.getTransaction().begin();
        thePsp.setContact(p);
        em.persist(thePsp);
        em.getTransaction().commit();
    }

    public static Person createMainContact(EntityManager em, long id, String firstName, String lastName, Address a, PSP psp, String email){
        if(EntityLookup.getPersonById(em,id,true)!=null)
            return EntityLookup.getPersonById(em,id,true);
        em.getTransaction().begin();
        Person p = new Person();
        p.setId(id);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setAddress(a);
        p.setPsp(psp);
        p.setFullName(firstName+" "+lastName);
        p.setTitle("Primary Admin");
        p.setEmail(email);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    public static PSP createPSP(EntityManager em, long id, String name, Address a){
        if(EntityLookup.getPspById(em,id)!=null)
            return EntityLookup.getPspById(em,id);
        em.getTransaction().begin();
        PSP p = new PSP();
        p.setId(id);
        p.setFullName(name);
        p.setAddress(a);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    public static Agency createAgency(EntityManager em, long id, String name, String phone, String taxId, Address address, Person contact, PSP psp){
        if(EntityLookup.getAgencyById(em,id)!=null)
            return EntityLookup.getAgencyById(em,id);
        em.getTransaction().begin();
        Agency a = new Agency();
        a.setId(id);
        a.setPhone(phone);
        a.setAddress(address);
        a.setName(name);
        a.setPsp(psp);
        a.setTaxId(taxId);
        a.setPrimaryContact(contact);
        em.persist(a);
        em.getTransaction().commit();
        em.getTransaction().begin();
        a.getAgentList().add(contact);
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }

    public static Address createAddress(EntityManager em, long id, String ad1, String ad2, String city, String state, String zip){
        if(EntityLookup.getAddressById(em,id)!=null)
            return null;
        em.getTransaction().begin();
        Address a = new Address();
        a.setId(id);
        a.setZipCode(zip);
        a.setAddress2(ad2);
        a.setAddress1(ad1);
        a.setCity(city);
        a.setState(state);
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }

    public static ActivityStatus createActivityStatus(EntityManager em, int id, String name){
        if(EntityLookup.getActivityStatusById(em,id)!=null)
            return null;
        em.getTransaction().begin();
        ActivityStatus activityStatus = new ActivityStatus();
        activityStatus.setId(id);
        activityStatus.setDescription(name);
        em.persist(activityStatus);
        em.getTransaction().commit();
        return  activityStatus;
    }

    // ═══════════════════════════════════════════════════════════════
    //  APPLICATION SECTIONS (baseline form template for proposals)
    // ═══════════════════════════════════════════════════════════════

    private static void createApplicationSections(EntityManager em, PSP psp) {
        // Section 1 — Company Information
        ApplicationSection s1 = createAppSection(em, 1L, "Company Information",
                "Legal business name, EIN, and basic company details.", "ALL", 100, psp);
        createAppField(em, "company_legal_name", "Legal Company Name",    "TEXT",  true,  100, null, s1);
        createAppField(em, "company_dba",        "DBA (Doing Business As)","TEXT", false, 200, null, s1);
        createAppField(em, "company_ein",        "Employer Identification Number (EIN)","TEXT",true,300,null,s1);
        createAppField(em, "company_employees",  "Number of Employees",   "NUMBER",true, 400, null, s1);
        createAppField(em, "company_eligible",   "Number of Eligible Employees","NUMBER",false,500,null,s1);
        createAppField(em, "company_phone",      "Company Phone Number",  "TEXT",  true,  600, null, s1);
        createAppField(em, "company_website",    "Company Website",       "TEXT",  false, 700, null, s1);

        // Section 2 — Primary Contact
        ApplicationSection s2 = createAppSection(em, 2L, "Primary Contact",
                "Main point of contact for benefits administration.", "ALL", 200, psp);
        createAppField(em, "contact_name",  "Contact Name",   "TEXT",  true,  100, null, s2);
        createAppField(em, "contact_title", "Title",          "TEXT",  false, 200, null, s2);
        createAppField(em, "contact_email", "Email Address",  "EMAIL", true,  300, null, s2);
        createAppField(em, "contact_phone", "Phone Number",   "TEXT",  true,  400, null, s2);
        createAppField(em, "contact_fax",   "Fax Number",     "TEXT",  false, 500, null, s2);

        // Section 3 — Mailing Address
        ApplicationSection s3 = createAppSection(em, 3L, "Mailing Address",
                "Company mailing address for correspondence.", "ALL", 300, psp);
        createAppField(em, "address_street1", "Street Address Line 1", "TEXT", true,  100, null, s3);
        createAppField(em, "address_street2", "Street Address Line 2", "TEXT", false, 200, null, s3);
        createAppField(em, "address_city",    "City",                  "TEXT", true,  300, null, s3);
        createAppField(em, "address_state",   "State",                 "TEXT", true,  400, null, s3);
        createAppField(em, "address_zip",     "ZIP Code",              "TEXT", true,  500, null, s3);
    }

    private static ApplicationSection createAppSection(EntityManager em, Long id, String name,
                                                        String description, String scope, int sortOrder, PSP psp) {
        em.getTransaction().begin();
        ApplicationSection s = new ApplicationSection();
        s.setId(id);
        s.setName(name);
        s.setDescription(description);
        s.setScope(scope);
        s.setSortOrder(sortOrder);
        s.setSuppressed(false);
        s.setPsp(psp);
        em.persist(s);
        em.getTransaction().commit();
        return s;
    }

    private static void createAppField(EntityManager em, String fieldKey, String label,
                                        String fieldType, boolean required, int sortOrder,
                                        String selectOptions, ApplicationSection section) {
        em.getTransaction().begin();
        ApplicationField f = new ApplicationField();
        f.setFieldKey(fieldKey);
        f.setLabel(label);
        f.setFieldType(fieldType);
        f.setRequired(required);
        f.setSortOrder(sortOrder);
        f.setSelectOptions(selectOptions);
        f.setSuppressed(false);
        f.setApplicationSection(section);
        em.persist(f);
        em.getTransaction().commit();
    }

}
