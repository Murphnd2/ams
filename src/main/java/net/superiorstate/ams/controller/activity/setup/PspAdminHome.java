package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

            // Load price items (fee types) — needed for add-row modal and fee types tab
            List<PriceItem> priceItemList = SalesDAO.getPriceItemList(em, pspId);
            Collections.sort(priceItemList);
            request.setAttribute("priceItemList", priceItemList);

            // Load service modules (still needed for modules/sort tab)
            List<ServiceModule> serviceModuleList = SalesDAO.getServiceModuleList(em, pspId);
            Collections.sort(serviceModuleList);
            request.setAttribute("serviceModuleList", serviceModuleList);

            // Load agency list (for assignment)
            List<Agency> agencyList = SalesDAO.getAgencyList(em, pspId);
            Collections.sort(agencyList);
            request.setAttribute("agencyList", agencyList);

            // Load LOS and Enhancement lists (non-suppressed, sorted) for add-row modal
            List<LOS> losList = em.createNamedQuery("LOS.getByPsp", LOS.class)
                    .setParameter("psp_id", (long) pspId)
                    .getResultList();
            losList.removeIf(LOS::isSuppressed);
            request.setAttribute("losList", losList);

            List<Enhancement> enhancementList = em.createQuery(
                    "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId ORDER BY e.sortOrder", Enhancement.class)
                    .setParameter("pspId", (long) pspId)
                    .getResultList();
            enhancementList.removeIf(Enhancement::isSuppressed);
            Collections.sort(enhancementList);
            request.setAttribute("enhancementList", enhancementList);

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

                    // Extract distinct modules in this rate's grid (for Grid Sort tab)
                    List<ServiceModule> rateModuleList = new ArrayList<>();
                    Set<Long> seenModuleIds = new HashSet<>();
                    for (RateTable rt : rateTableList) {
                        if (seenModuleIds.add(rt.getModule().getId())) {
                            rateModuleList.add(rt.getModule());
                        }
                    }
                    request.setAttribute("rateModuleList", rateModuleList);

                    // Load agencies assigned to this rate
                    try {
                        List<Agency> assignedAgencies = SalesDAO.getAgenciesAssignedToRate(em, rateId);
                        request.setAttribute("assignedAgencies", assignedAgencies);
                    } catch (Exception e) {
                        request.setAttribute("assignedAgencies", List.of());
                    }

                    // Build JSON map of used fee types per module for smart filtering
                    // Format: { "moduleId": [priceItemId1, priceItemId2, ...], ... }
                    Map<Long, Set<Long>> usedFees = new HashMap<>();
                    for (RateTable rt : rateTableList) {
                        long modId = rt.getModule().getId();
                        usedFees.computeIfAbsent(modId, k -> new HashSet<>()).add(rt.getPriceItem().getId());
                    }

                    // Also build a map of LOS/Enhancement → moduleId for the modal JS
                    // Format: { "los_5": moduleId, "enh_1": moduleId, ... }
                    Map<String, Long> entityModuleMap = new HashMap<>();
                    for (ServiceModule sm : serviceModuleList) {
                        if (sm.getLos() != null) {
                            entityModuleMap.put("los_" + sm.getLos().getId(), sm.getId());
                        }
                        if (sm.getEnhancement() != null) {
                            entityModuleMap.put("enh_" + sm.getEnhancement().getId(), sm.getId());
                        }
                    }

                    // Serialize usedFees to JSON
                    StringBuilder ufJson = new StringBuilder("{");
                    boolean first = true;
                    for (Map.Entry<Long, Set<Long>> entry : usedFees.entrySet()) {
                        if (!first) ufJson.append(",");
                        ufJson.append("\"").append(entry.getKey()).append("\":[");
                        ufJson.append(entry.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")));
                        ufJson.append("]");
                        first = false;
                    }
                    ufJson.append("}");
                    request.setAttribute("usedFeesJson", ufJson.toString());

                    // Serialize entityModuleMap to JSON
                    StringBuilder emJson = new StringBuilder("{");
                    first = true;
                    for (Map.Entry<String, Long> entry : entityModuleMap.entrySet()) {
                        if (!first) emJson.append(",");
                        emJson.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                        first = false;
                    }
                    emJson.append("}");
                    request.setAttribute("entityModuleMapJson", emJson.toString());
                }
            }

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/rateManager25.jsp");
        dispatcher.forward(request, response);
    }
}
