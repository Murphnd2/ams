package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "AgentHome", value = "/AgentHome")
public class AgentHome extends HttpServlet {

    // Ordered stage list for pipeline display
    private static final List<String> STAGE_ORDER = List.of(
            "NEW", "CONTACTED", "QUALIFIED", "PROPOSAL_SENT", "NEGOTIATION", "ON_HOLD", "WON", "LOST"
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        loadData(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        loadData(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/agentHome25.jsp");
        request.setAttribute("pageTitle", "Agent Pipeline");
        request.setAttribute("pageIcon", "bi-kanban");
        dispatcher.forward(request, response);
    }

    private void loadData(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();
            boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));

            // Find agency for this user
            Agency agency = findAgencyForUser(em, currentUser);
            request.setAttribute("agency", agency);

            if (agency == null) {
                request.setAttribute("opportunities", new ArrayList<>());
                request.setAttribute("pipelineMap", new LinkedHashMap<>());
                request.setAttribute("prospects", new ArrayList<>());
                request.setAttribute("stageOrder", STAGE_ORDER);
                return;
            }

            // Load opportunities — Agency Manager sees all, Agent sees own only
            List<Opportunity> opportunities;
            if (isAgencyAdmin) {
                opportunities = getOpportunitiesByAgency(em, agency.getId());
            } else {
                opportunities = getOpportunitiesByAgent(em, currentUser.getId());
            }
            request.setAttribute("opportunities", opportunities);

            // Force-initialize lazy collections while EM is open (needed for JSP rendering)
            for (Opportunity opp : opportunities) {
                if (opp.getProspect() != null && opp.getProspect().getProposalList() != null) {
                    opp.getProspect().getProposalList().size(); // trigger lazy load
                    for (var prop : opp.getProspect().getProposalList()) {
                        if (prop.getLosList() != null) prop.getLosList().size();
                    }
                }
            }

            // Group by stage for pipeline display
            Map<String, List<Opportunity>> pipelineMap = new LinkedHashMap<>();
            for (String stage : STAGE_ORDER) {
                pipelineMap.put(stage, new ArrayList<>());
            }
            for (Opportunity opp : opportunities) {
                String stage = opp.getStage() != null ? opp.getStage() : "NEW";
                pipelineMap.computeIfAbsent(stage, k -> new ArrayList<>()).add(opp);
            }
            request.setAttribute("pipelineMap", pipelineMap);
            request.setAttribute("stageOrder", STAGE_ORDER);

            // Quick stats
            long activeCount = opportunities.stream().filter(o -> !o.isComplete() && !"WON".equals(o.getStage()) && !"LOST".equals(o.getStage())).count();
            long wonCount = opportunities.stream().filter(o -> "WON".equals(o.getStage())).count();
            long lostCount = opportunities.stream().filter(o -> "LOST".equals(o.getStage())).count();
            double pipelineValue = opportunities.stream()
                    .filter(o -> !o.isComplete() && !"WON".equals(o.getStage()) && !"LOST".equals(o.getStage()))
                    .mapToDouble(o -> o.getEstimatedValue() != null ? o.getEstimatedValue() : 0.0)
                    .sum();
            request.setAttribute("activeCount", activeCount);
            request.setAttribute("wonCount", wonCount);
            request.setAttribute("lostCount", lostCount);
            request.setAttribute("pipelineValue", pipelineValue);

            // Load prospects for "New Opportunity" modal
            List<Prospect> prospects;
            if (isAgencyAdmin) {
                prospects = getProspectsByAgency(em, agency.getId());
            } else {
                prospects = SalesDAO.getAgentProspects(em, currentUser);
            }
            request.setAttribute("prospects", prospects);

            // Load agents list (for Agency Manager filter)
            if (isAgencyAdmin) {
                try {
                    List<Person> agents = SalesDAO.getAgencyAgents(em, agency.getId());
                    request.setAttribute("agentList", agents);
                } catch (Exception e) {
                    request.setAttribute("agentList", new ArrayList<>());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private Agency findAgencyForUser(EntityManager em, Person user) {
        // Check if user is a manager of an agency
        Query q = em.createQuery("SELECT a FROM Agency a WHERE a.manager.id = :userId");
        q.setParameter("userId", user.getId());
        try {
            return (Agency) q.getSingleResult();
        } catch (NoResultException ignored) {}

        // Check if user is an agent in an agency
        Query q2 = em.createQuery("SELECT a FROM Agency a JOIN a.agentList ag WHERE ag.id = :userId");
        q2.setParameter("userId", user.getId());
        try {
            List<Agency> agencies = (List<Agency>) q2.getResultList();
            if (!agencies.isEmpty()) return agencies.get(0);
        } catch (NoResultException ignored) {}

        return null;
    }

    private List<Opportunity> getOpportunitiesByAgency(EntityManager em, long agencyId) {
        Query q = em.createQuery(
                "SELECT DISTINCT o FROM Opportunity o " +
                "LEFT JOIN FETCH o.prospect p " +
                "LEFT JOIN FETCH p.proposalList " +
                "WHERE o.agency.id = :agencyId ORDER BY o.stage, o.id DESC");
        q.setParameter("agencyId", agencyId);
        try {
            return (List<Opportunity>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<Opportunity> getOpportunitiesByAgent(EntityManager em, long agentId) {
        Query q = em.createQuery(
                "SELECT DISTINCT o FROM Opportunity o " +
                "LEFT JOIN FETCH o.prospect p " +
                "LEFT JOIN FETCH p.proposalList " +
                "WHERE o.assignedTo.id = :agentId ORDER BY o.stage, o.id DESC");
        q.setParameter("agentId", agentId);
        try {
            return (List<Opportunity>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<Prospect> getProspectsByAgency(EntityManager em, long agencyId) {
        Query q = em.createQuery(
                "SELECT p FROM Prospect p WHERE p.agent.id IN " +
                "(SELECT ag.id FROM Agency a JOIN a.agentList ag WHERE a.id = :agencyId) ORDER BY p.name");
        q.setParameter("agencyId", agencyId);
        try {
            return (List<Prospect>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }
}
