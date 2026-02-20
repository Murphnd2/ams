package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.util.*;

@WebServlet(name = "PspAdminHome", value = "/PspAdminHome")
public class PspAdminHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        int pspId = local.getCurrentPerson().getPsp().getId().intValue();

        try {
            // Always load the full rate list
            List<Rate> rateList = SalesDAO.getRateList(em, pspId);
            Collections.sort(rateList);
            request.setAttribute("rateList", rateList);

            // Build set of locked rate IDs (any rate referenced by a proposal)
            Set<Long> lockedRateIds = new HashSet<>();
            for (Rate r : rateList) {
                long count = SalesDAO.getProposalCountByRate(em, r.getId());
                if (count > 0) {
                    lockedRateIds.add(r.getId());
                }
            }
            request.setAttribute("lockedRateIds", lockedRateIds);

            // Always load price items and service modules (needed for add-rate-table-row modal)
            List<PriceItem> priceItemList = SalesDAO.getPriceItemList(em, pspId);
            Collections.sort(priceItemList);
            request.setAttribute("priceItemList", priceItemList);

            List<ServiceModule> serviceModuleList = SalesDAO.getServiceModuleList(em, pspId);
            Collections.sort(serviceModuleList);
            request.setAttribute("serviceModuleList", serviceModuleList);

            // Always load agency list (for assignment)
            List<Agency> agencyList = SalesDAO.getAgencyList(em, pspId);
            Collections.sort(agencyList);
            request.setAttribute("agencyList", agencyList);

            // If a rate is selected, load its details
            String rateIdParam = request.getParameter("rateId");
            if (rateIdParam != null && !rateIdParam.isEmpty()) {
                long rateId = Long.parseLong(rateIdParam);

                // Find the selected rate from the list
                Rate selectedRate = null;
                for (Rate r : rateList) {
                    if (r.getId() == rateId) {
                        selectedRate = r;
                        break;
                    }
                }

                if (selectedRate != null) {
                    request.setAttribute("selectedRate", selectedRate);
                    request.setAttribute("isLocked", lockedRateIds.contains(rateId));

                    // Load rate table rows (ordered by module sort, then price item sort)
                    List<RateTable> rateTableList = SalesDAO.getRateTableList(em, rateId);
                    request.setAttribute("rateTableList", rateTableList);

                    // Load agencies assigned to this rate
                    try {
                        List<Agency> assignedAgencies = SalesDAO.getAgenciesAssignedToRate(em, rateId);
                        request.setAttribute("assignedAgencies", assignedAgencies);
                    } catch (Exception e) {
                        // No agencies assigned yet — that's fine
                        request.setAttribute("assignedAgencies", List.of());
                    }
                }
            }

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/rateManager25.jsp");
        dispatcher.forward(request, response);
    }
}
