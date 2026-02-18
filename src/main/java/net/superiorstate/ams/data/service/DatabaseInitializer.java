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
import net.superiorstate.ams.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplateGroup;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.PriceItem;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.agency.RateTable;
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
            s.setSequenceCount(200);
            em.persist(s);
            em.getTransaction().commit();
        }
    }


    public static void initializeDataBase(HttpServletRequest request,EntityManager em)  {

        //Retrieve Form Data
        retrieveFormData(request);
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
        Person p1 = createMainContact(em,50L,"Fred","Flintstone",a,psp,"fred.flintstone@srag.com");
        //Update PSP with Person
        updatePspWithContact(em,psp,p);
        //Create Agency
        Agency agency = createAgency(em,14L,getPspName(),getPhone(),getTaxId(),a,p,psp);
        //Create Employer
        String fullName = getFirstName().trim() + " " + getLastName().trim();
        Employer er = createEmployer(em,-1,getPspName(),getEmail(),fullName,-1);
        Employer er1 = createEmployer(em,-2,"Slate Rock and Gravel","fred.flintstone@srag.com","Fred Flintstone",-2);
        //Create Employee
        Employee ee = createEmployee(em,-1,p,er);
        Employee ee1 = createEmployee(em,-2,p1,er1);
        //Assign Person to Employee
        em.getTransaction().begin();
        p.setEmployee(ee);
        p1.setEmployee(ee1);
        em.persist(p);
        em.persist(p1);
        em.getTransaction().commit();
        //Create Billing Group
        BillingGroup bg0 = createBillingGroup(em,7,"Other");
        BillingGroup bg1 = createBillingGroup(em,1, "FSA");
        BillingGroup bg2 = createBillingGroup(em,2,"HRA");
        BillingGroup bg3 = createBillingGroup(em,3,"COBRA");
        BillingGroup bg4 = createBillingGroup(em,4,"HSA");
        BillingGroup bg5 = createBillingGroup(em,5,"Transit");
        BillingGroup bg6 = createBillingGroup(em,6,"POP");
        BillingGroup bg7 = createBillingGroup(em,7,"MERP");
        BillingGroup bg8 = createBillingGroup(em,8,"LSA");
        //Create Template Group
        TemplateGroup tg1 = createTemplateGroup(em,1,"Renewal");
        TemplateGroup tg2 = createTemplateGroup(em,2,"Setup");
        TemplateGroup tg3 = createTemplateGroup(em,3,"Ticket");
        //Create Template Purposes
        TemplatePurpose tp1 = createTemplatePurpose(em,2,"Health FSA",2,tg1);
        TemplatePurpose tp5 = createTemplatePurpose(em,12,"Health FSA",12,tg2);

        TemplatePurpose tp2 = createTemplatePurpose(em,3,"Dependent Care",3,tg1);
        TemplatePurpose tp02 = createTemplatePurpose(em,21,"Dep Care",12,tg2);

        TemplatePurpose tp3 = createTemplatePurpose(em,5,"HRA",5,tg1);
        TemplatePurpose tp6 = createTemplatePurpose(em,13,"HRA",13, tg2);

        TemplatePurpose tp4 = createTemplatePurpose(em,7,"COBRA Insurance",7,tg1);
        TemplatePurpose tp7 = createTemplatePurpose(em,14,"COBRA", 14, tg2);

        TemplatePurpose tp08 = createTemplatePurpose(em,22,"MERP",6,tg1);
        TemplatePurpose tp8 = createTemplatePurpose(em,12,"MERP",5,tg2);

        TemplatePurpose tp09 = createTemplatePurpose(em,23,"HSA",6,tg1);
        TemplatePurpose tp9 = createTemplatePurpose(em,16,"HSA",16,tg2);

        TemplatePurpose tpA = createTemplatePurpose(em,24,"Transit",9,tg1);
        TemplatePurpose tp10 = createTemplatePurpose(em,15,"Transit",15,tg2);

        TemplatePurpose tpB = createTemplatePurpose(em,26,"POP",1,tg1);
        TemplatePurpose tp11 = createTemplatePurpose(em,11,"POP",11,tg2);

        TemplatePurpose tpC = createTemplatePurpose(em,27,"LSA",10,tg1);
        TemplatePurpose tp12 = createTemplatePurpose(em,17,"LSA",20,tg2);

        //Create Plan Types
        PlanType pt1 = createPlanType(em,1,"DCA","Dependent Care Account",bg1,tp2);
        PlanType pt2 = createPlanType(em,2,"FSA","Health Flexible Spending Account",bg1,tp1);
        PlanType pt3 = createPlanType(em,3,"HRA","Health Reimbursement Arrangement",bg2,tp3);
        PlanType pt7 = createPlanType(em,4,"HSA","HSA",bg4,tp9);
        PlanType pt05 = createPlanType(em,5,"LFSA","Limited Purpose FSA",bg1,tp1);
        PlanType pt06 = createPlanType(em,6,"MERP","Medical Expense Reimbursement Plan",bg7,tp8);
        PlanType pt07 = createPlanType(em,7,"PRK","Parking Plan",bg5,tp10);
        PlanType pt8 = createPlanType(em,8,"TRN","Transportation Plan",bg5,tp10);
        PlanType pt5 = createPlanType(em,9,"Dental","Dental",bg3,tp4);
        PlanType pt10 = createPlanType(em,10,"EAP","EAP",bg3,tp4);
        PlanType pt11 = createPlanType(em,11,"Life","Life",bg3,tp4);
        PlanType pt4 = createPlanType(em,12,"Medical","Medical",bg3,tp4);
        PlanType pt13 = createPlanType(em,13,"Pharmacy","Pharmacy",bg3,tp4);
        PlanType pt6 = createPlanType(em,14,"Vision","Vision",bg3,tp4);
        PlanType pt15 = createPlanType(em,15,"NEFSA","NEFSA",bg3,tp4);
        PlanType pt16 = createPlanType(em,16,"LSA","Lifestyle Spending Account",bg8,tp12);

        //Create Benefits
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = LocalDate.of(today.getYear(),today.getMonthValue(),1);
        LocalDate lastMonth = firstOfMonth.minusMonths(1L);
        LocalDate sixMonthsFromNow = firstOfMonth.plusMonths(6L);
        Benefit b1 = createBenefit(em,-3,"Demo HRA",er,pt3,Date.valueOf(firstOfMonth));
        Benefit b2 = createBenefit(em,-2,"Demo FSA",er,pt2,Date.valueOf(lastMonth));
        Benefit b3 = createBenefit(em,-1,"Demo COBRA",er,pt4,Date.valueOf(sixMonthsFromNow));
        //Create Contact Methods
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
        //Create Link Types
        createLinkType(em,1,"File Upload");
        createLinkType(em,2,"Hyperlink");
        createLinkType(em,3,"Insert Link");
        //Create Lines of Service
        LOS los0 = createLos(em,5L, "Premium Only Plan", "POP",psp);
        LOS los1 = createLos(em,6L,"Flexible Spending Accounts","FSA",psp);
        LOS los2 = createLos(em,7L,"Health Reimbursement Arrangements","HRA/MERP",psp);
        LOS los3 = createLos(em,8L,"COBRA Administration","COBRA",psp);
        LOS los4 = createLos(em,9L,"Health Savings Accounts","HSA",psp);
        LOS los5 = createLos(em,10L,"Transit/Commuter Plans","TRANSIT",psp);
        //Create Service Modules
        ServiceModule sm0 = createServiceModule(em,15L,"Section 125 Premium Only Plans","POP",100,psp);
        ServiceModule sm1 = createServiceModule(em,16L,"Section 125 Full Flex Plan with FSAs","FSA",200,psp);
        ServiceModule sm2 = createServiceModule(em,18L,"Section 105 HRAs / MERPs","HRA",300,psp);
        ServiceModule sm3 = createServiceModule(em,23L,"COBRA Administration","COBRA",800,psp);
        ServiceModule sm4 = createServiceModule(em,21L,"Debit Card Services","Cards",600,psp);
        ServiceModule sm5 = createServiceModule(em,19L,"Health Savings Accounts (HSAs)","HSA",400,psp);
        ServiceModule sm6 = createServiceModule(em,25L,"Transit Plans","Transit",450,psp);
        //Associate Service Modules
        associateModuleToLos(em,los0,sm0);
        associateModuleToLos(em,los1,sm1);
        associateModuleToLos(em,los1,sm4);
        associateModuleToLos(em,los2,sm2);
        associateModuleToLos(em,los2,sm4);
        associateModuleToLos(em,los3,sm3);
        associateModuleToLos(em,los4,sm5);
        associateModuleToLos(em,los5,sm6);
        //Create Price Items
        PriceItem pi1 = createPriceItem(em,31L,"Setup (One-time) Fee",100,psp);
        PriceItem pi2 = createPriceItem(em,32L,"Annual Administration Fee",200,psp);
        PriceItem pi3 = createPriceItem(em,33L,"Base Monthly Fee per Participant",300,psp);
        //Create Rate
        Rate rate = createRate(em,52L,"Standard Rate",psp);
        //Create Rate Table
        createPricing(em,199,sm0,pi1,rate);
        createPricing(em,350,sm1,pi1,rate);
        createPricing(em,350,sm1,pi2,rate);
        createPricing(em,5,sm1,pi3,rate);
        createPricing(em,350,sm2,pi1,rate);
        createPricing(em,500,sm2,pi2,rate);
        createPricing(em,4.5,sm2,pi3,rate);
        createPricing(em,100,sm3,pi1,rate);
        createPricing(em,120,sm3,pi2,rate);
        createPricing(em,1,sm3,pi3,rate);
        createPricing(em,5,sm5,pi3,rate);
        createPricing(em,200,sm6,pi2,rate);
        //Create Reasons Created List
        ReasonCreated rc = createReasonCreated(em,1,"Internal Note",false);
        createReasonCreated(em,2,"Received Call",false);
        createReasonCreated(em,3,"Received Voicemail",false);
        createReasonCreated(em,4,"Received Email",false);
        createReasonCreated(em,5,"Made Call",true);
        createReasonCreated(em,6,"Left Voicemail",true);
        createReasonCreated(em,7,"Sent Email Message",true);
        createReasonCreated(em,8,"Quick Action",true);
        //Create Tasks Used in Monthly Imports
        createTask(em,10L,"Clear Import Tables","ClearImport",psp,p);
        createTask(em,11L,"Clear Monthly Billing","ClearMonthlyBilling",psp,p);
        createTask(em,12L,"Import Summit Export Files","",psp,p,false);
        createTask(em,13L,"Update Tables From Imports","UpdateTables",psp,p);
        createTask(em,14L,"Create Monthly Billing","CreateMonthlyBilling",psp,p);
        createTask(em,15L,"Refresh Employee List","RefreshTicketEmployees",psp,p);
        createTask(em,153L,"Default","",psp,p,false);
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
        //Create Ticket Categories
        TicketCategory tc = createTicketCategory(em,18L,"How Do I?","HOW");
        TemplatePurpose tp800 = createTemplatePurpose(em,25,"Get Online",100,tg3);
        TicketSubCategory tsc = createTicketSubCategory(em,10L,tc,tp800);
        TicketCategory tc1 = createTicketCategory(em,19L,"Please Update My...","UPDATE");
        TemplatePurpose tp101 = createTemplatePurpose(em,30,"Banking Information",200,tg3);
        TicketSubCategory tsc1 = createTicketSubCategory(em,11L,tc1,tp101);
        TicketCategory tc2 = createTicketCategory(em,20L,"Something is Wrong","FIX");
        createRequiredTaskList(em,tp8,psp);
        createRequiredTaskList(em,tp101,psp);

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
        UserRole ur6 = createUserRole(em,6,"Pending Agent");
        UserRole ur7 = createUserRole(em,7,"Anonymous");
        UserRole ur8 = createUserRole(em,8,"Agency Admin");
        UserRole ur9 = createUserRole(em,9,"PSP Super User");
        UserRole ur10 = createUserRole(em,10,"Other");
        UserRole ur11 = createUserRole(em,101, "Accelergent BPO");
        UserRole ur12 = createUserRole(em,102, "Accelergent Admin");
        UserRole ur13 = createUserRole(em,103, "Accelergent User");
        //Assign User To PSP Roles
        assignRoles(em,user,ur5,ur1);
        //Add Accelergent Background Data
        Person ac1 = createMainContact(em,101L,"Accelergent","BPO",a,psp,"bpo@dpath.com");
        Person ac2 = createMainContact(em,102L,"Accelergent","Admin",a,psp,"bpoAdmin@dpath.com");
        User au1 = createUser(em,ac1,ac1.getEmail(),"Passw0rd!");
        User au2 = createUser(em,ac2,ac2.getEmail(),"Passw0rd!");
        assignRoles(em,au1,ur11,ur13);
        assignRoles(em,au2,ur12,ur13);
        // Add PSP Constants
        addPspConstants(em);
        // Create Initialization Checklist
        createInitializationChecklist(em);
        // Set Note To Show Initialization Completed
        setInitializationNote(em,p,rc,tsc,as);

    }

    private static void createInitializationChecklist(EntityManager em) {
        Person person = EntityLookup.getPersonById(em, 104L);
        PSP psp = EntityLookup.getPspById(em, 4L);

        CheckList checkList = new CheckList();
        checkList.setId(29L);
        checkList.setDueDate(Date.valueOf(LocalDate.now()));
        checkList.setAssignedTo(person);
        checkList.setFullName("Initialize Database");
        checkList.setLoggedBy(person);

        List<ToDo> toDoList = new ArrayList<>();
        toDoList.add(createInitialToDo(em,checkList,psp,5));
        toDoList.add(createToDoWithTask(em, checkList, psp, 30L, "UploadInitialData", "Upload Exports from Summit", 10));
        toDoList.add(createToDoWithTask(em, checkList, psp, 32L, "ImportInitialData", "Import Data from Uploads", 20));
        toDoList.add(createToDoWithTask(em, checkList, psp, 34L, "UpdateInitialTables", "Update Working Tables from Imports", 30));

        checkList.setToDoList(toDoList);
        em.persist(checkList);
    }
    private static final String EXPORT_INSTRUCTIONS_LINK =
            "https://docs.google.com/document/d/1Z8I_-5z53AiDNZu2B6wiMe8yRcOK1pZ6B53TBJl3pHg/edit?usp=sharing";

    private static ToDo createInitialToDo(EntityManager em, CheckList checklist, PSP psp, int sortOrder) {
        LinkType infoType = em.find(LinkType.class, 2);  // Direct JPA primary key lookup

        WebLink infoLink = new WebLink();
        infoLink.setActive(true);
        infoLink.setLinkPath(EXPORT_INSTRUCTIONS_LINK);
        infoLink.setPlainText("Export Instructions");
        infoLink.setLinkType(infoType);
        em.persist(infoLink);

        ToDo todo = createToDoWithTask(em, checklist, psp, 28L, "", "", sortOrder);

        Task task = todo.getTask();
        task.setDescription("Get Summit Exports");
        task.setHasInfo(true);
        task.setInfoLink(infoLink);

        return todo;
    }



    private static ToDo createToDoWithTask(EntityManager em, CheckList checklist, PSP psp, Long taskId, String servlet, String label, int sortOrder) {
        Task task = new Task();
        task.setId(taskId);
        task.setPsp(psp);
        task.setDescription(createAnchor(servlet, label));
        task.setReUsable(false);
        task.setHasAutomation(false);
        em.persist(task);

        ToDo toDo = new ToDo();
        toDo.setId(taskId+1L); // Replace with proper ID generator if applicable
        toDo.setTask(task);
        toDo.setCheckList(checklist);
        toDo.setSortOrder(sortOrder);
        em.persist(toDo);

        return toDo;
    }

    private static String createAnchor(String servletName, String description) {
        return "<a href=\"" + servletName + "\">" + description + "</a>";
    }

    private static Long generateToDoId() {
        return 33L; // Replace this with a real ID generation strategy if needed
    }

    private static void createRequiredTaskList(EntityManager em, TemplatePurpose tp, PSP psp){
        em.getTransaction().begin();
        RequiredTaskList rtl = new RequiredTaskList();
        rtl.setPsp(psp);
        rtl.setDescription(tp.getDescription());
        rtl.setTemplatePurpose(tp);
        rtl.setInActive(false);
        em.persist(rtl);
        em.getTransaction().commit();

    }

    private static void addPspConstants(EntityManager em){
        if(getConstantByName(em,"FALSE_CLOSE")==null)
            createConstant(em,"FALSE_CLOSE",Date.valueOf(LocalDate.of(2000,1,1)).toString());
        if(getConstantByName(em,"SAVE_PATH")==null)
            createConstant(em,"SAVE_PATH","C:\\data\\");
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

    private static void setInitializationNote(EntityManager em, Person p, ReasonCreated reasonCreated, TicketSubCategory tsc, ActivityStatus as){

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
        t.setTicketSubCategory(tsc);
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

    public static TicketSubCategory createTicketSubCategory(EntityManager em, Long id,  TicketCategory tc, TemplatePurpose tp){
        if(EntityLookup.getSubCategoryById(em,id)!=null)
            return EntityLookup.getSubCategoryById(em,id);
        em.getTransaction().begin();
        TicketSubCategory t = new TicketSubCategory();
        t.setId(id);
        t.setDescription(tp.getDescription());
        t.setTicketCategory(tc);
        t.setTemplatePurpose(tp);
        t.setActive(true);
        em.persist(t);
        em.getTransaction().commit();
        return t;
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

    public static LOS createLos(EntityManager em, long id, String name, String shortText, PSP psp){
        if(EntityLookup.getLosById(em,id)!=null)
            return EntityLookup.getLosById(em,id);
        em.getTransaction().begin();
        LOS l = new LOS();
        l.setId(id);
        l.setDescription(name);
        l.setShortText(shortText);
        l.setPsp(psp);
        em.persist(l);
        em.getTransaction().commit();
        return l;
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
    public static Benefit createBenefit(EntityManager em, int id, String name, Employer er, PlanType pt, Date theDate){
        LocalDate currentDate = theDate.toLocalDate();
        LocalDate effectiveDate = currentDate.minusYears(1L);
        Date dateEffective = Date.valueOf(effectiveDate);

        if(EntityLookup.getBenefitById(em,id,true)!=null)
            return EntityLookup.getBenefitById(em,id,true);
        em.getTransaction().begin();
        Benefit b = new Benefit();
        b.setId(id);

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

    public static PlanType createPlanType(EntityManager em, int id, String code, String name, BillingGroup bg, TemplatePurpose tp){
        if(EntityLookup.getPlanTypeById(em,id)!=null)
            return EntityLookup.getPlanTypeById(em,id);
        em.getTransaction().begin();
        PlanType pt = new PlanType();
        pt.setPlanTypeId(id);
        pt.setCode(code);
        pt.setPlanTypeName(name);
        pt.setBillingGroup(bg);
        pt.setTemplatePurpose(tp);
        em.persist(pt);
        em.getTransaction().commit();
        return pt;
    }

    public static TemplatePurpose createTemplatePurpose(EntityManager em, int id, String name, int sortOrder, TemplateGroup tg){
        if(EntityLookup.getTemplatePurposeById(em,id,true)!=null)
            return EntityLookup.getTemplatePurposeById(em,id,true);
        em.getTransaction().begin();
        TemplatePurpose tp = new TemplatePurpose();
        tp.setTemplateGroup(tg);
        tp.setSortOrder(sortOrder);
        tp.setDescription(name);
        tp.setId(id);
        em.persist(tp);
        em.getTransaction().commit();
        return tp;
    }

    public static TemplateGroup createTemplateGroup(EntityManager em, int id, String name){
        if(EntityLookup.getTemplateGroupById(em,id)!=null)
            return EntityLookup.getTemplateGroupById(em,id);
        em.getTransaction().begin();
        TemplateGroup tg = new TemplateGroup();
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

}
