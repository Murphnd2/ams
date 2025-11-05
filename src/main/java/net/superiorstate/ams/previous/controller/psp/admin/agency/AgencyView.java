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
import net.superiorstate.ams.previous.model.sales.agency.Prospect;

import java.io.IOException;

@WebServlet(name = "AgencyView", value = "/AgencyView")
public class AgencyView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillAgencyData(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillAgencyData(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }

    private void fillAgencyData(HttpServletRequest request,HttpServletResponse response){
        int agencyId = Integer.parseInt(request.getParameter("agencySelectButton"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Agency agency = dG.getAgencyFull(em,agencyId);
        Address address = agency.getAddress();
        Person person = agency.getPrimaryContact();
        Person currentAgent = new Person();
        request.getSession().setAttribute("hasCurrentRate",false);
        request.getSession().setAttribute("pspAgencyHomeSender",2);
        request.getSession().setAttribute("hasCurrentAgency",true);
        request.getSession().setAttribute("currentAgency",agency);
        request.getSession().setAttribute("currentAddress", address);
        request.getSession().setAttribute("currentContact", person);
        request.getSession().setAttribute("formDisable",true);
        request.getSession().setAttribute("formDisable2",true);
        request.getSession().setAttribute("hasCurrentAgent",false);
        request.getSession().setAttribute("currentAgent",currentAgent);
        Prospect prospect = new Prospect();
        request.getSession().setAttribute("hasCurrentProspect",false);
        request.getSession().setAttribute("currentProspect",prospect);
        dG.setAgencyAccordion(request,1);
        em.close();
    }
}
