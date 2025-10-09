package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbA;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.summit.bill;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "EmailBillingToEmployer", value = "/EmailBillingToEmployer")
public class EmailBillingToEmployer extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendEmployerBilling(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendEmployerBilling(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/billing/sendBillingForm.jsp");
        dispatcher.forward(request,response);
    }

    private void sendEmployerBilling(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Employer employer = (Employer) request.getSession().getAttribute("currentBillingEmployer");
        String guid = bill.getLastGuid(em,employer);
        String path = dbA.getWebPath(em) + "EmployerBillingDetail?uid=" + guid;
        List<Person> employerContactList = getEmployerContactList(em, employer);
        request.getSession().setAttribute("employerContactList",employerContactList);
        List<Person> remainingEmployeesWithEmails = getRemainingContactsWithEmails(em,employer,employerContactList);
        request.getSession().setAttribute("remainingContacts",remainingEmployeesWithEmails);
        request.getSession().setAttribute("erLastGuid",guid);
        request.getSession().setAttribute("bcLink",path);
        em.close();
    }

    private List<Person> getRemainingContactsWithEmails(EntityManager em, Employer er, List<Person> employerContacts){
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.employer.id = :id AND e.email is not null");
        q.setParameter("id",er.getId());
        List<Employee> employees;
        List<Person> remainingContacts = new ArrayList<>();
        try{
            employees = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            return remainingContacts;
        }
        if(employees.size()==0)
            return remainingContacts;
        for(Employee e:employees){
            Query q1 = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id order by p.id desc");
            q1.setParameter("id",e.getId());
            List<Person> personList;
            try{
                personList = (List<Person>) q1.getResultList();
            } catch (NoResultException e1){
                continue;
            }
            if(personList.size()==0)
                continue;
            for(Person p: personList){
                if(p.getEmail()!=null && dbEmail.isValidEmail(p.getEmail()) && !employerContacts.contains(p) && !remainingContacts.contains(p)){
                    remainingContacts.add(p);
                    break;
                }
            }
        }
        return remainingContacts;
    }
    private List<Person> getContactList(EntityManager em, Activity a){
        List<Person> contactList = new ArrayList<>();
        if(a.getPrimaryContact()!=null && a.getPrimaryContact().getEmail()!=null && dbEmail.isValidEmail(a.getPrimaryContact().getEmail()))
            contactList.add(a.getPrimaryContact());
        if(a.getAssigneeContactList()!=null && a.getAssigneeContactList().size()>0){
            for(Person p: a.getAssigneeContactList()){
                if(dbEmail.isValidEmail(p.getEmail()) && !contactList.contains(p))
                    contactList.add(p);
            }
        }
        return contactList;
    }

    private List<Person> getEmployerContactList(EntityManager em, Employer er){
        //Any past tickets?
        Ticket lastTicket = getMostRecentTicketForBilling(em, er);
        if(lastTicket!=null)
            return getContactList(em,lastTicket);

        //Any past renewals?
        Renewal lastRenewal = getMostRecentRenewal(em,er);
        if(lastRenewal!=null)
            return getContactList(em,lastRenewal);

        return new ArrayList<>();
    }

    private Ticket getMostRecentTicketForBilling(EntityManager em, Employer er){
        Query q = em.createQuery("SELECT t FROM Ticket t WHERE t.ticketSubCategory.id = :tId AND t.primaryContact.employee.employer.id=:eId order by t.id desc");
        q.setParameter("tId",56328L);
        q.setParameter("eId",er.getId());
        List<Ticket> tickets;
        try{
            tickets = (List<Ticket>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(tickets.size()==0)
            return null;
        return tickets.get(0);
    }

    private Renewal getMostRecentRenewal(EntityManager em, Employer er){
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer.id = :id order by r.id desc");
        q.setParameter("id",er.getId());
        List<Renewal> renewalList;
        try{
            renewalList = (List<Renewal>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        if(renewalList.size()==0)
            return null;
        return renewalList.get(0);
    }
}
