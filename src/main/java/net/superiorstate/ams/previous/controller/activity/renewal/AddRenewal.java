package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddRenewal", value = "/AddRenewal")
public class AddRenewal extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addRenewal(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addRenewal(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void addRenewal(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        Renewal renewal = createRenewal(request,em);
        dR.createCheckListForRenewal(em, renewal, currentPerson);
        createRenewalItems(request,em,renewal);
        refreshRenewalsToStartList(request,em,renewal);
        request.getSession().setAttribute("adminView",2);
        request.getSession().setAttribute("currentChecklist", new CheckList());
        request.getSession().setAttribute("currentToDoList",new ArrayList<>());
        request.getSession().setAttribute("currentSetup", new Setup());
        request.getSession().setAttribute("currentTicket", new Ticket());
        request.getSession().setAttribute("currentRenewal",renewal);
        request.getSession().setAttribute("currentActivity", renewal);
        ViewSelectedActivity.setActivityView(request,em,renewal);
        em.close();
    }


    private Renewal createRenewal(HttpServletRequest request, EntityManager em){
        Employer currentEmployer = (Employer) request.getSession().getAttribute("currentEmployer");
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        Renewal r = dR.createRenewal(em,currentEmployer,currentPerson);
        request.getSession().setAttribute("currentRenewal", r);
        request.getSession().setAttribute("currentActivity",r);
        return r;
    }

    private void createRenewalItems(HttpServletRequest request, EntityManager em, Renewal renewal){
        List<Benefit> benefitList = (List<Benefit>) request.getSession().getAttribute("benefitsForRenewalList");
        for(Benefit b:benefitList){
            String buttonId = "btnBen" + b.getId();
            String buttonVal = request.getParameter(buttonId);
            if(buttonVal!=null && !buttonVal.isEmpty()){
                dR.addBenefitToRenewal(request,em,b,renewal);
            }
        }
    }

    private void refreshRenewalsToStartList(HttpServletRequest request, EntityManager em, Renewal renewal){
        Employer employer = renewal.getEmployer();
        List<Benefit> benefitList = dR.getBenefitsByEmployerSortedForRenewal(em,employer);
        List<Renewal> pastRenewalsList = dR.getPastRenewalsForEmployer(em,employer);
        request.getSession().setAttribute("currentEmployer",new Employer());
        request.getSession().setAttribute("currentRenewal", renewal);
        request.getSession().setAttribute("currentActivity", renewal);
        ViewSelectedActivity.setActivityView(request,em,renewal);
        request.getSession().setAttribute("benefitsForRenewalList",benefitList);
        request.getSession().setAttribute("pastRenewalList",pastRenewalsList);
        request.getSession().setAttribute("renewalView",2);
    }
}
