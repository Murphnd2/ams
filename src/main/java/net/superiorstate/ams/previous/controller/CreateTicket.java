package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbAuth;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "CreateTicket2", value = "/CreateTicket2")
public class CreateTicket extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createTicket(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createTicket(request);
        goToPage(request,response);

    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private int getEmployeeIdFromString(String employeeString){
        int firstBracket = employeeString.indexOf("(");
        int secondBracket = -1;
        int employeeId = -1;
        if(firstBracket!=-1)
         secondBracket = employeeString.indexOf(")",firstBracket);
        if(secondBracket!=-1)
            employeeId = Integer.parseInt(employeeString.substring(firstBracket+1,secondBracket));
        return employeeId;
    }

    public static String findPhone(String textString){
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

    public static String findEmail(String textString){
        int atSign = textString.indexOf("@");
        if(atSign==-1)
            return null;
        int dot = textString.indexOf(".",atSign);
        if(dot==-1)
            return null;

        int fSpace = textString.indexOf(" ");
        if(fSpace==-1)
            return textString;
        if(fSpace>dot)
            return textString.substring(0,fSpace);

        //find start of email (the final space that is before the @ sign
        int lastSpaceBeforeAtSign = -1;
        int previousSpaceInLoop;
        do{
            previousSpaceInLoop = lastSpaceBeforeAtSign;
            lastSpaceBeforeAtSign = textString.indexOf(" ",lastSpaceBeforeAtSign+1);
        } while (lastSpaceBeforeAtSign!=-1 && lastSpaceBeforeAtSign < atSign);

        int fIndex = previousSpaceInLoop + 1;
        int lIndex = textString.indexOf(" ",dot);
        if(lIndex == -1)
            return textString.substring(fIndex);
        return textString.substring(fIndex,lIndex);
    }

    private Person createPerson(EntityManager em, String personString, PSP psp, String ticketText){
        int fB = personString.indexOf(",");
        String lName;
        String fName;
        if(fB!=-1){
            lName=personString.substring(0,fB);
            fName=personString.substring(fB+1);
        } else{
            int fS = personString.indexOf(" ");
            if(fS!=-1){
                fName = personString.substring(0,fS);
                lName = personString.substring(fS+1);
            } else{
                lName = personString;
                fName = "Mr./Mrs.";
            }
        }
        em.getTransaction().begin();
        Person p = new Person();
        p.setPsp(psp);
        p.setFullName(fName +" "+lName);
        p.setFirstName(fName);
        p.setLastName(lName);
        p.setEmail(findEmail(ticketText));
        p.setPhone(findPhone(ticketText));
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private int getSubCategoryFromString(String reasonString){
        int firstBracket = reasonString.indexOf("(");
        int secondBracket = -1;
        int reasonId = -1;
        if(firstBracket!=-1)
            secondBracket = reasonString.indexOf(")",firstBracket);
        if(secondBracket!=-1)
            reasonId = Integer.parseInt(reasonString.substring(firstBracket+1,secondBracket));
        return reasonId;
    }


    private TicketSubCategory getNewTicketCategory(EntityManager em, String reasonString, PSP psp){

        String fl = reasonString.substring(0,1);
        TicketCategory tc;
        switch (fl){
            case "H":tc=dM.getTicketCategoryById(em,1L);break;
            case "W":tc=dM.getTicketCategoryById(em,2L);break;
            case "N":tc=dM.getTicketCategoryById(em,6L);break;
            case "G":tc=dM.getTicketCategoryById(em,3L);break;
            default:tc=dM.getTicketCategoryById(em,4L);break;
        }
        int fD = reasonString.indexOf("-");
        String desc;
        if(fD!=-1){
            desc = reasonString.substring(fD+1);
        }else{
            desc = reasonString;
        }
        em.getTransaction().begin();
        TicketSubCategory tsc = new TicketSubCategory();
        tsc.setTicketCategory(tc);
        tsc.setDescription(desc);
        tsc.setActive(false);
        em.persist(tsc);
        em.getTransaction().commit();
        return tsc;
    }

    private void createTicket(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person logger = (Person) request.getSession().getAttribute("currentPerson");
        String employeeString = request.getParameter("employeeList");
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        String ticketText = request.getParameter("ticketDescription");
        int employeeId = getEmployeeIdFromString(employeeString);
        Person p;
        if(employeeId!=-1){
            Employee e = dM.getEmployeeById(em,employeeId);
            p = dbTicket.getPersonByEmployee(em,e);
            if(p==null){
                p = dbAuth.createPersonFromEmployee(em,e,psp);
            }
        } else{
            p = createPerson(em,employeeString,psp,ticketText);
        }
        int contactMethodId = Integer.parseInt(request.getParameter("contactMethodList"));
        String reasonString = request.getParameter("ticketSubCategoryList");
        int subCatId = getSubCategoryFromString(reasonString);
        TicketSubCategory ticketSubCategory;
        if(subCatId==-1){
            ticketSubCategory = getNewTicketCategory(em,reasonString,psp);
        } else {
            ticketSubCategory = dM.getSubCategoryById(em,subCatId);
        }
        em.getTransaction().begin();
        Ticket t = new Ticket();
        t.setFullName(p.getFirstName() + " " + p.getLastName());
        t.setComplete(false);
        t.setAssignedTo(logger);
        t.setLoggedBy(logger);
        t.setDueDate(Date.valueOf(LocalDate.now().plusDays(7)));
        t.setDescription(ticketText);
        t.setContact(p);
        t.setContactMethod(dM.getMethodById(em,contactMethodId));
        t.setTicketSubCategory(ticketSubCategory);
        em.persist(t);
        em.getTransaction().commit();

        ViewSelectedActivity.setActivityView(request,em,t);
        em.close();
    }

    public static void createToDoList(EntityManager em, Ticket t, CheckList c){
        List<SortedTask> sortedTaskList = dbTicket.getTasksRequiredForTicket(em,t);
        if(sortedTaskList.size()==0)
            sortedTaskList.add(new SortedTask(dM.getTaskById(em,129L),1000));
        for(SortedTask st: sortedTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(false);
            if(st.getTask().getId()==129L)
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
}
