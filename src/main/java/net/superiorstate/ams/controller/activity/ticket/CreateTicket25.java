package net.superiorstate.ams.controller.activity.ticket;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.PersonResolver;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateTicket25", value = "/CreateTicket25")
public class CreateTicket25 extends HttpServlet {

        private int contactMethodId;
        private ContactMethod contactMethod;
        private String contactNameField;
        private String reasonField;
        private int reasonId;
        private ServiceItem serviceItem;
        private Person contact;
        private String issue;
        private String firstName;
        private String lastName;
        private String fullName;
        private int employeeId;
        private Employee employee;
        private String phone;
        private String email;

        private Person currentUser;

        private PSP psp;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            createTicketAlt(request);
            goToPage(request,response);
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            createTicketAlt(request);
            goToPage(request,response);

        }
        private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
            dispatcher.forward(request,response);
        }

    private void createTicketAlt(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        if(!getFormDataAndAssignToLocalVariables(request, em)){
            em.close();
            return;
        }

        // ── PATH 1: Employee picked from typeahead (hidden field has ID) ──
        String eeIdParam = request.getParameter("employeeId");
        if(eeIdParam != null && !eeIdParam.trim().isEmpty()){
            try {
                int eeId = Integer.parseInt(eeIdParam.trim());
                Employee ee = EntityLookup.getEmployeeById(em, eeId, true);
                if(ee != null){
                    setContact(PersonResolver.getPersonFromEmployee(em, ee));
                }
            } catch (NumberFormatException ignored) {}
        }

        // ── PATH 2: Freeform text — resolve with priority chain ──
        if(getContact() == null && getContactNameField() != null && !getContactNameField().isEmpty()){
            setContact(resolveContactFromFreeform(em, getContactNameField()));
        }

        if(getContact() == null){
            em.close();
            return;
        }

        processTicketType(em);
        Ticket t = createTicketObject(em);
        em.refresh(t);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        local.respondToActivityUpdate(em,"ADD_TICKET",t);
        request.getSession().setAttribute("local",local);
        em.close();
    }

    /**
     * Resolves a freeform text entry to a Person.
     * Priority chain:
     *   1. Valid email → Employee by email → Person from that Employee
     *   2. Valid email → Person by email
     *   3. Valid email → no match → create Person, parse name from email
     *   4. Not email → treat as name → Employee by name → Person from that Employee
     *   5. Not email → treat as name → Person by name
     *   6. Not email → treat as name → no match → create new Person from name
     */
    private Person resolveContactFromFreeform(EntityManager em, String input){
        String text = input.trim();

        // ── EMAIL PATH ──
        if(EmailDAO.isValidEmail(text)){
            // 1. Email → try Employee table first
            Employee ee = PersonResolver.getEmployee(em, text);
            if(ee != null){
                return PersonResolver.getPersonFromEmployee(em, ee);
            }
            // 2. Email → try Person table
            Person p = PersonResolver.getBestPersonFromString(em, text);
            if(p != null){
                return p;
            }
            // 3. Email → no match anywhere → create Person, parse name from email
            return PersonResolver.createPersonFromEmail(em, text);
        }

        // ── NAME PATH ──
        // 4. Name → try Employee table (handles "last, first" and "first last")
        Employee ee = PersonResolver.getEmployee(em, text);
        if(ee != null){
            return PersonResolver.getPersonFromEmployee(em, ee);
        }
        // 5. Name → try Person table
        Person p = PersonResolver.getBestPersonFromString(em, text);
        if(p != null){
            return p;
        }
        // 6. Name → no match → create new Person from the name
        //    PersonResolver.createPersonFromAll crashes on single-word names (no space or comma),
        //    so handle that case here before calling it.
        if(!text.contains(" ") && !text.contains(",")){
            // Single word — treat as last name, first name unknown
            em.getTransaction().begin();
            Person newP = new Person();
            newP.setPsp(EntityLookup.getPspById(em, 4L));
            newP.setLastName(text.toUpperCase());
            newP.setFirstName("UNKNOWN");
            newP.setFullName("UNKNOWN " + text.toUpperCase());
            em.persist(newP);
            em.getTransaction().commit();
            return newP;
        }
        return PersonResolver.createPersonFromAll(em, text, null, null);
    }


        private Ticket createTicketObject(EntityManager em){
            em.getTransaction().begin();
            Ticket t = new Ticket();
            t.setAssignedTo(getCurrentUser());
            t.setDueDate(Date.valueOf(LocalDate.now().plusDays(7)));
            t.setDescription(getIssue());
            t.setTicketServiceItem(getServiceItem());
            t.setContact(getContact());
            t.setPrimaryContact(getContact());
            t.setComplete(false);
            t.setContactMethod(getContactMethod());
            t.setLoggedBy(getCurrentUser());
            t.setFullName(getContact().getFirstName().toUpperCase() + " " + getContact().getLastName().toUpperCase());
            em.persist(t);
            em.getTransaction().commit();

            CheckList c = createCheckListForTicket(em,t);
            CheckList checkList = EntityLookup.getCheckListById(em,c.getId());
            em.getTransaction().begin();
            t.setCheckList(checkList);
            em.persist(t);
            em.getTransaction().commit();

            // Auto-attach matching questionnaire instances
            QuestionnaireService.attachMatchingQuestionnaires(em, t, "TICKET",
                    getCurrentUser().getPsp().getId());

            return t;

        }

        private CheckList createCheckListForTicket(EntityManager em, Ticket t){
            em.getTransaction().begin();
            CheckList c = new CheckList();
            c.setAssignedTo(t);
            c.setComplete(false);
            c.setFullName(t.getFullName() + " Checklist");
            c.setDueDate(t.getDueDate());
            c.setLoggedBy(getCurrentUser());
            em.persist(c);
            em.getTransaction().commit();
            createToDoList(em,c);
            CheckList freshChecklist = EntityLookup.getCheckListById(em,c.getId());
            BpoTaskPushService.pushDelegatedTasks(em, freshChecklist);
            return freshChecklist;
        }

        private void createToDoList(EntityManager em, CheckList c){
            List<SortedTask> sortedTaskList;
            try{
                sortedTaskList = TicketQueryDAO.getTasksRequiredForTheTicket(em,(Ticket) c.getAssignedTo());
                if(sortedTaskList==null)
                    sortedTaskList = new ArrayList<>();
            } catch (Exception ex){
                sortedTaskList = new ArrayList<>();
            }

            if(sortedTaskList.size()==0)
                sortedTaskList.add(new SortedTask(EntityLookup.getTaskById(em, 153L), 1000));

            for(SortedTask st: sortedTaskList){
                em.getTransaction().begin();
                ToDo toDo = new ToDo();
                toDo.setTask(st.getTask());
                toDo.setSortOrder(st.getSortOrder());
                toDo.setCheckList(c);
                toDo.setComplete(false);
                if(st.getTask().getId()==153L)
                    toDo.setComplete(true);
                em.persist(toDo);
                em.getTransaction().commit();

                em.getTransaction().begin();
                assert c != null;
                CheckList checkList = EntityLookup.getCheckListById(em,c.getId());
                assert checkList != null;
                checkList.getToDoList().add(toDo);
                em.persist(checkList);
                em.getTransaction().commit();
            }
        }

    private TicketCategory getTicketCategory(EntityManager em){
        String t = getReasonField().toLowerCase();
        long catId = 21; // default = General
        if(t.contains("claim"))
            catId = 11;
        else if(t.contains("access") || t.contains("online") || t.contains("log in") || t.contains("portal"))
            catId = 12;
        else if(t.contains("debit") || t.contains("card"))
            catId = 13;
        else if(t.contains("cobra"))
            catId = 14;
        else if(t.contains("hsa"))
            catId = 15;
        else if(t.contains("enrol") || t.contains("new hire") || t.contains("life event"))
            catId = 16;
        else if(t.contains("quote") || t.contains("fsa") || t.contains("hra") || t.contains("pop") || t.contains("transit"))
            catId = 17;
        else if(t.contains("bill") || t.contains("invoice") || t.contains("payment"))
            catId = 18;
        return EntityLookup.getTicketCategoryById(em, catId);
    }

        private void processTicketType(EntityManager em){
            if(reasonId==0){
                // "Enter my own reason" — create a new ServiceItem for this custom reason
                String reason = getReasonField();
                int lp = reason.indexOf("[-");
                int sp = reason.indexOf("-]",lp);
                boolean createOne = true;
                if(reason.contains("[-") && reason.contains("-]") && (sp>lp)){
                    try{
                        int rId = Integer.parseInt(reason.substring(lp+2,sp));
                        setReasonId(rId);
                        setServiceItem(EntityLookup.getServiceItemById(em, getReasonId()));
                        createOne = false;
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if(createOne){
                    TicketCategory tc = getTicketCategory(em);
                    ActivityCategory tg = em.find(ActivityCategory.class, 3); // group 3 = Ticket
                    em.getTransaction().begin();
                    ServiceItem si = new ServiceItem();
                    si.setDescription(getReasonField());
                    si.setActivityCategory(tg);
                    si.setPsp(EntityLookup.getPspById(em, 4L));
                    si.setSourceType("MANUAL");
                    si.setTicketCategory(tc);
                    si.setSuppressed(true); // Custom one-off reasons start suppressed
                    si.setSortOrder(100);
                    em.persist(si);
                    em.getTransaction().commit();
                    setServiceItem(si);
                }
            } else {
                // Selected from dropdown — reasonId is a ServiceItem ID
                setServiceItem(EntityLookup.getServiceItemById(em, getReasonId()));
            }
        }






    private boolean getFormDataAndAssignToLocalVariables(HttpServletRequest request, EntityManager em){
        // ── Reset all instance vars to prevent stale data from prior requests ──
        setContact(null);
        setEmployee(null);
        setEmployeeId(0);
        setContactMethod(null);
        setContactNameField(null);
        setReasonField(null);
        setReasonId(0);
        setServiceItem(null);
        setIssue(null);
        setFirstName(null);
        setLastName(null);
        setFullName(null);
        setPhone(null);
        setEmail(null);
        setCurrentUser(null);
        setPsp(null);

        setContactMethodId(2);

        // Read contactName (the visible text field)
        String contactName = request.getParameter("contactName");
        if(contactName == null || contactName.trim().isEmpty()){
            // Backward compat: old form used "employeeList"
            contactName = request.getParameter("employeeList");
        }
        setContactNameField(contactName != null ? contactName.trim() : "");

        String rid1 = request.getParameter("serviceItemList");
        int rid1i = Integer.parseInt(rid1);
        setReasonId(rid1i);
        if(rid1i==0)
            setReasonField(request.getParameter("reasonNameTicket"));
        else {
            // Dropdown now sends ServiceItem IDs
            ServiceItem si = EntityLookup.getServiceItemById(em, rid1i);
            setReasonField(si != null ? si.getDescription() : "Unknown");
        }
        String text=null;
        try{
            text = request.getParameter("ticketDescription");
            setIssue(text);
        } catch (Exception e){
            return false;
        }
        if(text==null || text.trim().equals(""))
            return false;
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        setCurrentUser(local.getCurrentPerson());
        return true;
    }

        public int getContactMethodId() {
            return contactMethodId;
        }

        public void setContactMethodId(int contactMethodId) {
            this.contactMethodId = contactMethodId;
        }

        public ContactMethod getContactMethod() {
            return contactMethod;
        }

        public void setContactMethod(ContactMethod contactMethod) {
            this.contactMethod = contactMethod;
        }

        public String getContactNameField() {
            return contactNameField;
        }

        public void setContactNameField(String contactNameField) {
            this.contactNameField = contactNameField;
        }

        public String getReasonField() {
            return reasonField;
        }

        public void setReasonField(String reasonField) {
            this.reasonField = reasonField;
        }

        public int getReasonId() {
            return reasonId;
        }

        public void setReasonId(int reasonId) {
            this.reasonId = reasonId;
        }

        public ServiceItem getServiceItem() {
            return serviceItem;
        }

        public void setServiceItem(ServiceItem serviceItem) {
            this.serviceItem = serviceItem;
        }

        public Person getContact() {
            return contact;
        }

        public void setContact(Person contact) {
            this.contact = contact;
        }

        public String getIssue() {
            return issue;
        }

        public void setIssue(String issue) {
            this.issue = issue;
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

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public int getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(int employeeId) {
            this.employeeId = employeeId;
        }

        public Employee getEmployee() {
            return employee;
        }

        public void setEmployee(Employee employee) {
            this.employee = employee;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Person getCurrentUser() {
            return currentUser;
        }

        public void setCurrentUser(Person currentUser) {
            this.currentUser = currentUser;
        }

        public PSP getPsp() {
            return psp;
        }

        public void setPsp(PSP psp) {
            this.psp = psp;
        }
}
