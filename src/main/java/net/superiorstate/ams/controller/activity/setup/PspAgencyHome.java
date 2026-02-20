package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "PspAgencyHome", value = "/PspAgencyHome")
public class PspAgencyHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();

        try {
            // Always load agency list
            List<Agency> agencyList = SalesDAO.getAgencyList(em, pspId);
            Collections.sort(agencyList);
            request.setAttribute("agencyList", agencyList);

            // Always load all rates (for assignment checkboxes)
            List<Rate> allRates = SalesDAO.getRateList(em, pspId);
            Collections.sort(allRates);
            request.setAttribute("allRates", allRates);

            // Load pending agents (users with agent role not yet assigned to an agency)
            List<Person> pendingAgents = SalesDAO.getPendingAgents(em, local.getCurrentPerson().getPsp());
            request.setAttribute("pendingAgents", pendingAgents);

            // If an agency is selected, load its details
            String agencyIdParam = request.getParameter("agencyId");
            if (agencyIdParam != null && !agencyIdParam.isEmpty()) {
                long agencyId = Long.parseLong(agencyIdParam);

                // Load agency with rates fetched
                Agency selectedAgency = SalesDAO.getAgencyFull(em, agencyId);
                request.setAttribute("selectedAgency", selectedAgency);

                // Load agents for this agency
                try {
                    List<Person> agents = SalesDAO.getAgencyAgents(em, agencyId);
                    request.setAttribute("agentList", agents);
                } catch (Exception e) {
                    request.setAttribute("agentList", List.of());
                }
            }

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/agencyManager25.jsp");
        dispatcher.forward(request, response);
    }
}