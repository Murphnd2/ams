package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.misc.dbAuth;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.general.UserRole;
import net.superiorstate.ams.previous.model.sales.agency.Agency;

import java.io.IOException;

@WebServlet(name = "AssignPendingAgent", value = "/AssignPendingAgent")
public class AssignPendingAgent extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeAgent(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeAgent(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }
    private void removeAgent(HttpServletRequest request,HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        long personId = Long.parseLong(request.getParameter("pendingAgentList"));
        Person person = dM.getPersonById(em,personId);

        UserRole urAgent = dbAuth.getUserRoleById(em,2);
        UserRole urPending = dbAuth.getUserRoleById(em,6);

        Agency currentAgency = (Agency) request.getSession().getAttribute("currentAgency");
        Agency agency = dG.getAgencyFull(em,currentAgency.getId());

        em.getTransaction().begin();

        agency.addAgent(person);
        User u = dbAuth.getUserFromPerson(em,person);
        u.removeUserFromRole(urPending);
        u.addUserToRole(urAgent);

        em.persist(u);
        em.persist(agency);

        em.getTransaction().commit();
        em.close();

    }
}
