package net.superiorstate.ams.controller.activity.contact;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.tix;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ModifyContact25", value = "/ModifyContact25")
public class ModifyContact25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        modifyContact(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void modifyContact(HttpServletRequest request){
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        boolean isEmployee = local.getCurrentActivity().getPrimaryContact()!=null && local.getCurrentActivity().getPrimaryContact().getEmployee()!=null;

        String currentFirst = local.getCurrentActivity().getPrimaryContact().getFirstName().toUpperCase().trim();
        String currentLast = local.getCurrentActivity().getPrimaryContact().getLastName().toUpperCase().trim();
        String currentEmail = "";
        if(local.getCurrentActivity().getPrimaryContact().getEmail() !=null)
            currentEmail = local.getCurrentActivity().getPrimaryContact().getEmail().toLowerCase().trim();
        if(isEmployee){
            Employee ee = local.getCurrentActivity().getPrimaryContact().getEmployee();
            if(ee.getHrEmail()!=null && !ee.getHrEmail().equals("") && Validator.isValidEmail(ee.getHrEmail()))
                currentEmail = ee.getHrEmail().toLowerCase().trim();
            else if(ee.getEmail()!=null && !ee.getEmail().equals("") && Validator.isValidEmail(ee.getEmail()))
                currentEmail = ee.getEmail().toLowerCase().trim();
        }


        //Retrieve Form Parameters
        String newFirst=null;
        String newLast=null;
        String newEmail=null;
        try{
            newFirst = request.getParameter("contactFirst").toString().toUpperCase().trim();
            newLast = request.getParameter("contactLast").toString().toUpperCase().trim();
            newEmail = request.getParameter("contactEmail").toString().toLowerCase().trim();
        } catch (Exception e){
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        //Name Modification Check
        boolean updateFirst = !newFirst.equals("") && !newFirst.equalsIgnoreCase(currentFirst);
        boolean updateLast = !newLast.equals("") && !newLast.equalsIgnoreCase(currentLast);
        boolean updateEmail = Validator.isValidEmail(newEmail) && !newEmail.equalsIgnoreCase(currentEmail);

        //End Method if Nothing has Changed
        if(!updateFirst && !updateLast && !updateEmail)
            return;

        //Update Employee Info
        Employee ee = null;
        if(isEmployee){
            ee = EntityLookup.getEmployeeById(em,local.getCurrentActivity().getPrimaryContact().getEmployee().getId());
            if(ee!=null){
                em.getTransaction().begin();
                ee.setFirstName(newFirst);
                ee.setLastName(newLast);
                ee.setEmail(newEmail);
                ee.setHrEmail(newEmail);
                em.persist(ee);
                em.getTransaction().commit();
                em.refresh(ee);
            }
        }

        //Update Person Info
        Person p = EntityLookup.getPersonById(em,local.getCurrentActivity().getPrimaryContact().getId());
        if(p!=null){ em.getTransaction().begin();
            p.setFirstName(newFirst);
            p.setLastName(newLast);
            p.setEmail(newEmail);
            p.setFullName(newFirst.toUpperCase() + " " + newLast.toUpperCase());
            if(isEmployee)
                p.setEmployee(ee);
            em.persist(p);
            em.getTransaction().commit();
            em.refresh(p);
        }

        //Update Activity Info
        local.getCurrentActivity().setPrimaryContact(p);
        Activity a = EntityLookup.getActivityById(em,local.getCurrentActivity().getActivity().getId());
        if(a!=null){
            em.getTransaction().begin();
            a.setPrimaryContact(p);
            if(a.getClass().getSimpleName().equals("Ticket"))
                a.setFullName(a.getPrimaryContact().getFullName());
            em.persist(a);
            em.getTransaction().commit();
            em.refresh(a);
        }
        local.getCurrentActivity().setActivity(a);

        if(isEmployee)
            tix.createTicket(em, local.getCurrentActivity().getPrimaryContact(), currentEmail,currentFirst,currentLast,local, global);
        if(isEmployee && local.getCurrentActivity().getActivity().getClass().getSimpleName().equals("Ticket") && (!currentLast.equalsIgnoreCase(newLast) || !currentFirst.equalsIgnoreCase(newFirst))){
            Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
            q.setParameter("id",local.getCurrentActivity().getActivity().getId());
            Activity25 a25 = (Activity25) q.getSingleResult();
            Activity25u a25u = new Activity25u(a25);
            List<Activity25u> activity25us = new ArrayList<>(local.getActivitiesAllOpen());
            int i;
            for(i=0; i < activity25us.size(); i++){
                if(activity25us.get(i).getActivity().getId().equals(a25u.getActivity().getId())){
                    break;
                }
            }
            activity25us.remove(activity25us.get(i));
            activity25us.add(i,a25u);
            local.setActivitiesAllOpen(activity25us);
            local.setFilteredActivityList(local.filterActivityListing());
        }
        local.getCurrentEmail().initializeEmail();
        local.getCurrentEmail().fillRecipientList();
        request.getSession().setAttribute("local",local);
        em.close();
    }

}
