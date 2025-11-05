package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Agency;

import java.io.IOException;

@WebServlet(name = "ModAgency", value = "/ModAgency")
public class ModAgency extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateAgency(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateAgency(request,response);
        goToAdminHomePage(request,response);
    }

    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ToggleState");
        dispatcher.forward(request,response);
    }

    private void updateAgency(HttpServletRequest request, HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Agency currentAgency = (Agency) request.getSession().getAttribute("currentAgency");

        Agency agency = dG.getAgencyFull(em,currentAgency.getId());
        Address address = new Address();
        Person person = agency.getPrimaryContact();

        address.setAddress1(request.getParameter("address1"));
        address.setAddress2(request.getParameter("address2"));
        address.setCity(request.getParameter("city"));
        address.setState(request.getParameter("stateList"));
        address.setZipCode(request.getParameter("zipCode"));
        em.getTransaction().begin();
        em.persist(address);
        em.getTransaction().commit();

        person.setFirstName(request.getParameter("firstName"));
        person.setLastName(request.getParameter("lastName"));
        person.setEmail(request.getParameter("email"));
        String pPhone = request.getParameter("personPhone");
        if(pPhone!=null && !pPhone.isEmpty() && !pPhone.trim().isEmpty())
            person.setPhone(pPhone);
        String pTitle = request.getParameter("title");
        if(pTitle!=null && !pTitle.isEmpty() && !pTitle.trim().isEmpty())
            person.setTitle(pTitle);
        em.getTransaction().begin();
        em.persist(person);
        em.getTransaction().commit();

        agency.setName(request.getParameter("agencyName"));
        agency.setPhone(request.getParameter("agencyPhone"));
        agency.setAddress(address);
        em.getTransaction().begin();
        em.persist(agency);
        em.getTransaction().commit();
        em.close();

        request.getSession().setAttribute("currentAgency", agency);
    }
}
