package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.AgentSetupRow;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent portal: list of Setups visible to the current agent or agency manager.
 *
 * Scope:
 *   - Agent (role=2): Setups where prospect.agent=me, OR any ToDo on the
 *     Setup's checklist is delegated to me (overrideOwnership=true, owner=me).
 *   - Agency Manager (role=8): Setups where prospect.agent ∈ myAgency.agentList,
 *     OR any delegated ToDo owner ∈ myAgency.agentList.
 *
 * Renders a compact row view that surfaces progress and "my open ToDos" counts
 * without leaking PSP-only activity detail.
 */
@WebServlet(name = "AgentSetupList", value = "/AgentSetupList")
public class AgentSetupList extends HttpServlet {

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
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/agentSetupList25.jsp");
        request.setAttribute("pageTitle", "My Setups");
        request.setAttribute("pageIcon", "bi-clipboard-check");
        dispatcher.forward(request, response);
    }

    private void loadData(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local == null || local.getCurrentPerson() == null) {
                request.setAttribute("setupRows", new ArrayList<>());
                return;
            }

            Person currentUser = local.getCurrentPerson();
            long myId = currentUser.getId();
            boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
            request.setAttribute("isAgencyAdmin", isAgencyAdmin);

            List<Long> scopedAgentIds = buildScopedAgentIds(em, currentUser, isAgencyAdmin);
            request.setAttribute("scopedAgentCount", scopedAgentIds.size());

            List<Setup> setups = loadScopedSetups(em, scopedAgentIds);
            List<AgentSetupRow> rows = buildRows(setups, myId, scopedAgentIds);
            request.setAttribute("setupRows", rows);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("setupRows", new ArrayList<>());
        } finally {
            em.close();
        }
    }

    /**
     * Scoped agent IDs:
     *   - Agent: just [me]
     *   - Agency Manager: every agent in every agency I manage, plus me
     *     (manager may themselves be listed as an agent on a sale).
     */
    private List<Long> buildScopedAgentIds(EntityManager em, Person me, boolean isAgencyAdmin) {
        List<Long> ids = new ArrayList<>();
        ids.add(me.getId());
        if (!isAgencyAdmin) return ids;

        Query q = em.createQuery(
                "SELECT DISTINCT ag.id FROM Agency a JOIN a.agentList ag " +
                "WHERE a.manager.id = :mgrId");
        q.setParameter("mgrId", me.getId());
        @SuppressWarnings("unchecked")
        List<Long> agentIds = (List<Long>) q.getResultList();
        for (Long id : agentIds) if (!ids.contains(id)) ids.add(id);
        return ids;
    }

    /**
     * Setups visible in scope. Union of:
     *   (a) Setups whose selling agent is in scope
     *   (b) Setups that have a delegated ToDo whose owner is in scope
     *
     * Done as a single DISTINCT query with OR across the two paths to avoid
     * duplicate result rows.
     */
    @SuppressWarnings("unchecked")
    private List<Setup> loadScopedSetups(EntityManager em, List<Long> scopedAgentIds) {
        if (scopedAgentIds.isEmpty()) return new ArrayList<>();

        Query q = em.createQuery(
                "SELECT DISTINCT s FROM Setup s " +
                "LEFT JOIN FETCH s.application app " +
                "LEFT JOIN FETCH s.checkList cl " +
                "WHERE s.isComplete = false " +
                "  AND (" +
                "    app.proposal.prospect.agent.id IN :ids " +
                "    OR EXISTS (" +
                "       SELECT td FROM ToDo td " +
                "       WHERE td.checkList.id = s.checkList.id " +
                "         AND td.overrideOwnership = true " +
                "         AND td.hasOwner = true " +
                "         AND td.owner.id IN :ids " +
                "    )" +
                "  ) " +
                "ORDER BY s.dueDate ASC, s.id DESC");
        q.setParameter("ids", scopedAgentIds);
        List<Setup> setups = (List<Setup>) q.getResultList();

        // Force-initialize chain pieces the JSP renders while EM is open
        for (Setup s : setups) {
            if (s.getCheckList() != null && s.getCheckList().getToDoList() != null) {
                s.getCheckList().getToDoList().size();
            }
            if (s.getApplication() != null && s.getApplication().getProposal() != null) {
                if (s.getApplication().getProposal().getProspect() != null) {
                    Person ag = s.getApplication().getProposal().getProspect().getAgent();
                    if (ag != null) ag.getFullName();
                    s.getApplication().getProposal().getProspect().getName();
                }
            }
        }
        return setups;
    }

    private List<AgentSetupRow> buildRows(List<Setup> setups, long myId, List<Long> scopedAgentIds) {
        List<AgentSetupRow> rows = new ArrayList<>();
        for (Setup s : setups) {
            AgentSetupRow r = toRow(s, myId, scopedAgentIds);
            rows.add(r);
            System.out.println("[AgentSetupList] row id=" + r.getId() + " name=" + r.getName()
                    + " due=" + r.getDueDate() + " total=" + r.getTotalToDos()
                    + " done=" + r.getDoneToDos() + " myOpen=" + r.getMyOpenToDos()
                    + " progressPct=" + r.getProgressPct()
                    + " sellingAgent=" + r.getSellingAgentName());
        }
        return rows;
    }

    private AgentSetupRow toRow(Setup s, long myId, List<Long> scopedAgentIds) {
        AgentSetupRow row = new AgentSetupRow();
        row.setId(s.getId());
        row.setName(s.getFullName());
        row.setDueDate(s.getDueDate());

        if (s.getApplication() != null && s.getApplication().getProposal() != null
                && s.getApplication().getProposal().getProspect() != null) {
            row.setProspectName(s.getApplication().getProposal().getProspect().getName());
            Person agent = s.getApplication().getProposal().getProspect().getAgent();
            if (agent != null) {
                row.setSellingAgentName(agent.getFullName());
                row.setSellingAgentId(agent.getId());
                row.setSellingAgentIsMe(agent.getId() != null && agent.getId() == myId);
            }
        }

        // ToDo counts (delegation visible only — non-delegated PSP ToDos count
        // toward the overall progress bar but aren't attributed to any agent)
        CheckList cl = s.getCheckList();
        int total = 0, done = 0, myOpen = 0, scopedOpen = 0;
        if (cl != null && cl.getToDoList() != null) {
            for (ToDo td : cl.getToDoList()) {
                total++;
                if (td.isComplete()) { done++; continue; }
                Long ownerId = (td.isOverrideOwnership() && td.hasOwner() && td.getOwner() != null)
                        ? td.getOwner().getId() : null;
                if (ownerId != null && ownerId == myId) myOpen++;
                if (ownerId != null && scopedAgentIds.contains(ownerId)) scopedOpen++;
            }
        }
        row.setTotalToDos(total);
        row.setDoneToDos(done);
        row.setMyOpenToDos(myOpen);
        row.setScopedOpenToDos(scopedOpen);
        row.setProgressPct(total == 0 ? 0 : (int) Math.round((done * 100.0) / total));
        return row;
    }

}
