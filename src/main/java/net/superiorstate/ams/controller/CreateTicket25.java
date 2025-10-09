package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.XP;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

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
        private TicketSubCategory category;
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
            };

            setContact(XP.getBestPerson(em,getContactNameField()));
            processTicketType(em);
            Ticket t = createTicketObject(em);
            em.refresh(t);
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            local.respondToActivityUpdate(em,"ADD_TICKET",t);
            request.getSession().setAttribute("local",local);
            em.close();
        }



        private Ticket createTicketObject(EntityManager em){
            em.getTransaction().begin();
            Ticket t = new Ticket();
            t.setAssignedTo(getCurrentUser());
            t.setDueDate(Date.valueOf(LocalDate.now().plusDays(7)));
            t.setDescription(getIssue());
            t.setTicketSubCategory(getCategory());
            t.setContact(getContact());
            t.setPrimaryContact(getContact());
            t.setComplete(false);
            t.setContactMethod(getContactMethod());
            t.setLoggedBy(getCurrentUser());
            t.setFullName(getContact().getFirstName().toUpperCase() + " " + getContact().getLastName().toUpperCase());
            em.persist(t);
            em.getTransaction().commit();

            CheckList c = createCheckListForTicket(em,t);
            CheckList checkList = dM.getCheckListById(em,c.getId());
            em.getTransaction().begin();
            t.setCheckList(checkList);
            em.persist(t);
            em.getTransaction().commit();
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
            return dM.getCheckListById(em,c.getId());
        }

        private void createToDoList(EntityManager em, CheckList c){
            List<SortedTask> sortedTaskList;
            try{
                sortedTaskList = dbTicket.getTasksRequiredForTheTicket(em,(Ticket) c.getAssignedTo());
                if(sortedTaskList==null)
                    sortedTaskList = new ArrayList<>();
            } catch (Exception ex){
                sortedTaskList = new ArrayList<>();
            }

            if(sortedTaskList.size()==0)
                sortedTaskList.add(new SortedTask(dM.getTaskById(em, 153L), 1000));

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
                CheckList checkList = dM.getCheckListById(em,c.getId());
                assert checkList != null;
                checkList.getToDoList().add(toDo);
                em.persist(checkList);
                em.getTransaction().commit();
            }
        }

        private TicketCategory getTicketCategory(EntityManager em){
            String textToCheck = getReasonField().toLowerCase();
            long catId = 21;
            if(textToCheck.contains("claim"))
                catId = 11;
            else if(textToCheck.contains("enrol"))
                catId = 16;
            else if (textToCheck.contains("access") || textToCheck.contains("online"))
                catId = 12;
            else if (textToCheck.contains("quote") || textToCheck.contains("fsa") || textToCheck.contains("hra") || textToCheck.contains("pop") || textToCheck.contains("hsa") || textToCheck.contains("cobra") || textToCheck.contains("transit"))
                catId = 17;
            return dM.getTicketCategoryById(em,catId);
        }

        private void processTicketType(EntityManager em){
            if(reasonId==0){
                String reason = getReasonField();
                int lp = reason.indexOf("[-");
                int sp = reason.indexOf("-]",lp);
                boolean createOne = true;
                if(reason.contains("[-") && reason.contains("-]") && (sp>lp)){
                    try{
                        int rId = Integer.parseInt(reason.substring(lp+2,sp));
                        setReasonId(rId);
                        setCategory(dM.getSubCategoryById(em,getReasonId()));
                        createOne = false;
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if(createOne){
                    TicketCategory tc = getTicketCategory(em);
                    TicketSubCategory tsc;
                    em.getTransaction().begin();
                    tsc = new TicketSubCategory();
                    tsc.setActive(false);
                    tsc.setTicketCategory(tc);
                    tsc.setDescription(getReasonField());
                    em.persist(tsc);
                    em.getTransaction().commit();
                    setCategory(tsc);
                }
            } else setCategory(dM.getSubCategoryById(em,getReasonId()));

        }






        private boolean getFormDataAndAssignToLocalVariables(HttpServletRequest request, EntityManager em){
            setContactMethodId(2);
            setContactNameField(request.getParameter("employeeList"));
            String rid1 = request.getParameter("ticketSubCategoryList");
            int rid1i = Integer.parseInt(rid1);
            setReasonId(rid1i);
            if(rid1i==0)
                setReasonField(request.getParameter("reasonNameTicket"));
            else
                setReasonField(dM.getSubCategoryById(em,rid1i).getDescription());
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

        public TicketSubCategory getCategory() {
            return category;
        }

        public void setCategory(TicketSubCategory category) {
            this.category = category;
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
