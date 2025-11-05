package net.superiorstate.ams.previous.controller.activity.ticket;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.XP;
import net.superiorstate.ams.previous.data.eV;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateTicket", value = "/CreateTicket")
public class CreateTicket2 extends HttpServlet {

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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void createTicketAlt(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        getFormDataAndAssignToLocalVariables(request, em);
        if (getIssue() == null || getIssue().trim().equals("")){
            em.close();
            return;
        }
        setContact(XP.getBestPerson(em,getContactNameField()));
        processTicketType(em);
        Ticket t = createTicketObject(em);
        ViewSelectedActivity.setActivityView(request,em,t);

        em.close();
    }

    private void createTicket(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        getFormDataAndAssignToLocalVariables(request,em);
        if(eV.getBestPersonFromString(em,getContactNameField())!=null)
            setContact(eV.getBestPersonFromString(em,getContactNameField()));
        else if(findEmail(getIssue())!=null && eV.getBestPersonFromString(em,findEmail(getIssue()))!=null)
            setContact(eV.getBestPersonFromString(em,findEmail(getIssue())));
        else
            setContact(eV.createPersonFromAll(em,getContactNameField(),findEmail(getIssue()),getPhoneFromText()));
        processTicketType(em);
        Ticket t = createTicketObject(em);
        ViewSelectedActivity.setActivityView(request,em,t);

        em.close();
    }

    private Person createPersonFromName(EntityManager em){
        String textString = getContactNameField();
        em.getTransaction().begin();
        Person p = new Person();
        if(textString.contains(" ")) {
            p.setFirstName(textString.substring(0, textString.indexOf(" ")).toUpperCase());
            p.setLastName(textString.substring(textString.indexOf(" ")+1).toUpperCase().trim());
        } else {
            p.setFirstName(textString.toUpperCase().trim());
            p.setLastName("UNKNOWN");
        }
        if(getPhoneFromText()!=null)
            p.setPhone(getPhoneFromText());
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private Ticket createTicketObject(EntityManager em){
        em.getTransaction().begin();
        Ticket t = new Ticket();
        t.setAssignedTo(getCurrentUser());
        t.setDueDate(Date.valueOf(LocalDate.now().plusDays(7)));
        t.setDescription(getIssue());
        t.setTicketSubCategory(getCategory());
        t.setContact(getContact());
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

    private void setPersonData(EntityManager em){
        if(validEmployeeSelected(em)){
            setEmployee(dM.getEmployeeById(em,getEmployeeId()));
            assureEmployeeIsPerson(em);
            setPersonByEmployee(em,getEmployee());
            updatePhone(em);
        } else if(nameFieldContainsEmail(em) && emailIsValidEmployee(em,findEmail(getContactNameField()))){
            setEmployee(dM.getEmployeeById(em,getEmployeeId()));
            assureEmployeeIsPerson(em);
            setPersonByEmployee(em,getEmployee());
            updatePhone(em);
        } else if(descriptionContainsEmailOfValidEmployee(em)){
            setEmployee(dM.getEmployeeById(em,getEmployeeId()));
            assureEmployeeIsPerson(em);
            setPersonByEmployee(em,getEmployee());
            updatePhone(em);
        } else if(nameEnteredMatchesAnEmployeesName(em)){
            setEmployee(dM.getEmployeeById(em,getEmployeeId()));
            assureEmployeeIsPerson(em);
            setPersonByEmployee(em,getEmployee());
            updatePhone(em);
        } else if (nameFieldContainsEmail(em) && emailIsValidPerson(em,findEmail(getContactNameField()))){
            updatePhone(em);
        } else if(descriptionContainsEmailOfValidPerson(em)){
            updatePhone(em);
        } else if(nameEnteredMatchesAnotherPerson(em)){
            updatePhone(em);
        } else {
            parseNames();
            PSP psp = dM.getPspById(em,4);
            Address address = dM.getAddressById(em,3L);
            if(emailExistsInText(getContactNameField()))
                setEmail(findEmail(getContactNameField()));
            else
                setEmail(findEmail(getIssue()));
            em.getTransaction().begin();
            Person p = new Person();
            p.setFirstName(getFirstName());
            p.setLastName(getLastName());
            p.setFullName(getFirstName() + " " + getLastName());
            p.setAddress(address);
            p.setPsp(psp);
            p.setEmail(getEmail());
            p.setPhone(getPhoneFromText());
            em.persist(p);
            em.getTransaction().commit();
            setContact(p);
        }
    }

    private void updatePhone(EntityManager em){
        String phone = getPhoneFromText();
        Person p = getContact();
        p.getEmployee().getEmployer().getEmployerName();
        em.getTransaction().begin();
        p.setPhone(phone);
        em.persist(p);
        em.getTransaction().commit();
        setContact(p);
    }

    private String getPhoneFromText(){
        String textString = getIssue();
        String phone = null;
        int fD = textString.indexOf("-");
        if(fD==-1)
            return null;
        int sD = textString.indexOf("-",fD+1);
        if(sD!=-1){
            phone = textString.substring(fD-3,sD+5);
        } else {
            int fP = textString.indexOf("(");
            if(fP==-1)
                return null;
            else{
                int sP = textString.indexOf(")",fP);
                if(sP!=-1){
                    phone = textString.substring(fP+1,fP+4)+"-"+textString.substring(fD-3,fD+5);
                } else{
                    return null;
                }
            }
        }
        return phone;
    }

    private boolean nameEnteredMatchesAnotherPerson(EntityManager em){
        parseNames();
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.firstName is not null and p.lastName is not null order by p.firstName,p.lastName");
        List<Person> personList = (List<Person>) q.getResultList();
        List<String> nameList = new ArrayList<>();
        String checkName = getFirstName() + " " + getLastName();
        for(Person p:personList)
            nameList.add(p.getFirstName().trim().toUpperCase()+" "+p.getLastName().trim().toUpperCase());
        if(nameList.contains(checkName)){
            setContact(personList.get(nameList.indexOf(checkName)));
            return true;
        } else return false;
    }

    private void setPersonByEmployee(EntityManager em, Employee ee){
        Query q = em.createQuery("SELECT p FROM Person p INNER JOIN Employee e ON p.employee = e WHERE p.employee.id = :id");
        q.setParameter("id",ee.getId());
        Person p = (Person) q.getSingleResult();
        setContact(p);
    }

    private Person getPersonByEmail(EntityManager em, String email) {
        Query q = em.createQuery("SELECT p FROM Person p where p.email is not null order by p.email");
        List<Person> personList = (List<Person>) q.getResultList();
        List<String> emailList = new ArrayList<>();
        for(Person p:personList)
            emailList.add(p.getEmail().trim().toLowerCase());
        if(emailList.contains(email))
            return personList.get(emailList.indexOf(email));
        return null;
    }

    private boolean emailIsValidPerson(EntityManager em, String email){
        Person p = getPersonByEmail(em, email);
        if(p==null){
            return false;
        } else {
            setContact(p);
            return true;
        }
    }

    private boolean descriptionContainsEmailOfValidPerson(EntityManager em){
        if(emailExistsInText(getIssue())){
            String email = findEmail(getIssue());
            return emailIsValidPerson(em, email);
        } else
            return false;
    }
    private void parseNames(){
        String potentialEmail = getContactNameField();
        if(findEmail(potentialEmail)!=null){
            String preAt = potentialEmail.substring(0,potentialEmail.indexOf("@"));
            String postAt = potentialEmail.substring(potentialEmail.indexOf("@")+1);
            if(preAt.contains(".")){
                setFirstName(preAt.substring(0,preAt.indexOf(".")).toUpperCase());
                setLastName(preAt.substring(preAt.indexOf(".")+1).toUpperCase());
            } else {
                setFirstName(preAt.toUpperCase());
                setLastName("AT " + postAt.substring(0,postAt.indexOf(".")).toUpperCase());
            }
        } else if(getContactNameField().contains(",")){
            setLastName(getContactNameField().substring(0,getContactNameField().indexOf(",")).trim().toUpperCase());
            setFirstName(getContactNameField().substring(getContactNameField().indexOf(",")+1).trim().toUpperCase());
        } else if(getContactNameField().trim().contains(" ")){
            String trimName = getContactNameField().trim();
            setFirstName(trimName.substring(0,trimName.indexOf(" ")).toUpperCase());
            setLastName(trimName.substring(trimName.indexOf(" ")+1).trim().toUpperCase());
        } else{
            setFirstName(getContactNameField().trim().toUpperCase());
            setLastName("-UNKNOWN-");
        }
    }

    private boolean nameEnteredMatchesAnEmployeesName(EntityManager em){
        parseNames();
        String checkName = getFirstName()+" "+getLastName();
        Query q = em.createQuery("SELECT e FROM Employee e where e.firstName is not null AND e.lastName is not null order by e.firstName,e.lastName");
        List<Employee> employeeList = (List<Employee>) q.getResultList();
        List<String> nameList = new ArrayList<>();
        for(Employee e:employeeList)
            nameList.add(e.getFirstName().trim().toUpperCase()+" "+e.getLastName().trim().toUpperCase());
        if(nameList.contains(checkName)){
            setEmployeeId(employeeList.get(nameList.indexOf(checkName)).getId());
            return true;
        } return false;
    }

    private Employee getEmployeeByEmail(EntityManager em, String email){
        Query q = em.createQuery("Select e FROM Employee e where e.email is not null");
        List<Employee> employeeList = (List<Employee>) q.getResultList();
        List<String> emailList = new ArrayList<>();
        for(Employee e:employeeList)
            emailList.add(e.getEmail().toLowerCase());
        if(emailList.contains(email))
            return employeeList.get(emailList.indexOf(email));
        return null;

    }

    private boolean nameFieldContainsEmail(EntityManager em){
        String potentialEmail = findEmail(getContactNameField());
        if(potentialEmail==null)
            return false;
        return true;

    }

    private String findEmail(String textToCheck){
        String afterAtText = textToCheck.substring(textToCheck.indexOf("@")+1);
        if(!afterAtText.contains("."))
            return null;
        String preAtText = textToCheck.substring(0,textToCheck.indexOf("@"));
        while(preAtText.contains(" ")){
            String holdText = preAtText;
            int spaceLoc = preAtText.indexOf(" ");
            preAtText = holdText.substring(spaceLoc+1);
        }
        int dotLoc = afterAtText.indexOf(".");
        String domain = afterAtText.substring(0,dotLoc);
        String remainder = afterAtText.substring(dotLoc+1);
        String ending;
        if(remainder.contains(" "))
            ending = remainder.substring(0,remainder.indexOf(" "));
        else
            ending = remainder;
        String fetchedEmail = preAtText + "@" + domain + "." + ending;
        if(!dbEmail.isValidEmail(fetchedEmail))
            return null;
        return fetchedEmail;
    }

    private boolean emailExistsInText(String textToCheck){
        if(textToCheck.contains("@") && textToCheck.contains(".") && (textToCheck.indexOf("@")<textToCheck.indexOf(".",textToCheck.indexOf("@"))))
            return true;
        return false;
    }
    private boolean descriptionContainsEmailOfValidEmployee(EntityManager em){
        if(emailExistsInText(getIssue())){
            String foundEmail = findEmail(getIssue());
            return emailIsValidEmployee(em,foundEmail);
        } else
            return false;
    }

    private boolean emailIsValidEmployee(EntityManager em, String email){
        Employee e = getEmployeeByEmail(em, email);
        if(e==null){
            return false;
        } else {
            setEmployeeId(e.getId());
            return true;
        }
    }
    private void assureEmployeeIsPerson(EntityManager em){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.employee is not null AND p.employee.id = :id");
        q.setParameter("id",getEmployeeId());
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            personList = null;
        }
        if(personList!=null && personList.size()>0)
           setContact(personList.get(0));
        else{
            Employee ee = dM.getEmployeeById(em,getEmployeeId(em));
            PSP psp = dM.getPspById(em,4);
            Address a = dM.getAddressById(em, 3L);
            em.getTransaction().begin();
            Person p = new Person();
            p.setPsp(psp);
            p.setEmail(getEmployee().getEmail());
            p.setFirstName(getEmployee().getFirstName().toUpperCase());
            p.setLastName(getEmployee().getLastName().toUpperCase());
            p.setFullName(getEmployee().getFirstName().toUpperCase()+" "+getEmployee().getLastName().toUpperCase());
            p.setAddress(a);
            p.setPhone(getPhoneFromText());
            p.setEmployee(ee);
            em.persist(p);
            em.getTransaction().commit();
            setContact(p);
        }
    }


    private int getEmployeeId(EntityManager em){
        if(contactNameField.contains("(") && (contactNameField.indexOf("(") < contactNameField.indexOf(")")))
            setEmployeeId(Integer.parseInt(contactNameField.substring(contactNameField.indexOf("(")+1,contactNameField.indexOf(")"))));
        else
            setEmployeeId(0);
        return getEmployeeId();
    }
    private boolean validEmployeeSelected(EntityManager em){
        if (getEmployeeId(em)==0)
            return false;
        return true;
    }

    private void getFormDataAndAssignToLocalVariables(HttpServletRequest request, EntityManager em){
        setContactMethodId(2);
        setContactNameField(request.getParameter("employeeList"));
        String rid1 = request.getParameter("ticketSubCategoryList");
        int rid1i = Integer.parseInt(rid1);
        setReasonId(rid1i);
        if(rid1i==0)
            setReasonField(request.getParameter("reasonNameTicket"));
        else
            setReasonField(dM.getSubCategoryById(em,rid1i).getDescription());
        setIssue(request.getParameter("ticketDescription"));
        setCurrentUser((Person) request.getSession().getAttribute("currentPerson"));

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
