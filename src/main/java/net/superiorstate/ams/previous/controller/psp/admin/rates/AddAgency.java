package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;

import java.io.IOException;

@WebServlet(name = "AddAgency", value = "/AddAgency")
public class AddAgency extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createAgency(request);
        goToAdminHomePage(request,response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createAgency(request);
        goToAdminHomePage(request,response);
    }

    private void createAgency(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");

        Address address = new Address();
        address.setAddress1(request.getParameter("address1"));
        address.setAddress2(request.getParameter("address2"));
        address.setCity(request.getParameter("city"));
        address.setState(request.getParameter("stateList"));
        address.setZipCode(request.getParameter("zipCode"));
        em.getTransaction().begin();
        em.persist(address);
        em.getTransaction().commit();

        Person person = new Person();
        person.setFirstName(request.getParameter("firstName"));
        person.setLastName(request.getParameter("lastName"));
        person.setMiddleInit("");
        person.setEmail(request.getParameter("email"));
        String pPhone = request.getParameter("personPhone");
        if(pPhone!=null && !pPhone.isEmpty() && !pPhone.trim().isEmpty())
            person.setPhone(pPhone);
        String pTitle = request.getParameter("title");
        if(pTitle!=null && !pTitle.isEmpty() && !pTitle.trim().isEmpty())
            person.setTitle(pTitle);
        person.setPsp(psp);
        person.setAddress(address);
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();

        Agency agency = new Agency();
        agency.setAddress(address);
        agency.setPrimaryContact(person);
        agency.setName(request.getParameter("agencyName"));
        agency.setPhone(request.getParameter("agencyPhone"));
        agency.setPsp((PSP) request.getSession().getAttribute("psp"));
        em.getTransaction().begin();
        em.persist(agency);
        em.getTransaction().commit();

        em.close();
        request.getSession().setAttribute("hasCurrentProspect",false);
        request.getSession().setAttribute("currentProspect",new Prospect());
        request.getSession().setAttribute("hasCurrentAgent",false);
        request.getSession().setAttribute("currentAgent",new Person());
        request.getSession().setAttribute("hasCurrentAgency",true);
        request.getSession().setAttribute("currentAgency",agency);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }
}
