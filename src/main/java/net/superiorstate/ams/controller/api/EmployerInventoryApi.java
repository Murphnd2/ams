package net.superiorstate.ams.controller.api;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmployerInventoryDAO;
import net.superiorstate.ams.model.dto.inventory.EmployerInventoryDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Read-only JSON endpoint returning per-employer service-scope inventory.
 *
 * <p>Authentication: any logged-in PSP session ({@code AmsDataLocal} present
 * in session). No admin role required. Returns 401 if unauthenticated.
 *
 * <p>Routing (all via GET):
 * <ul>
 *   <li>{@code ?orgId=N}  — single employer by organization_id; 404 if not found</li>
 *   <li>{@code ?name=X}   — case-insensitive name search; returns array (possibly empty)</li>
 *   <li>(no params)       — all in-scope employers; returns array</li>
 * </ul>
 *
 * <p>Response content-type: {@code application/json; charset=UTF-8}
 */
@WebServlet(name = "EmployerInventoryApi", value = "/api/v1/employer-inventory")
public class EmployerInventoryApi extends HttpServlet {

    private static final Logger log = LogManager.getLogger(EmployerInventoryApi.class);
    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        // ── auth check ─────────────────────────────────────────────────────
        // Require any logged-in PSP session.  No role gate — start permissive.
        // (SkillManager pattern: pull local from session; for a JSON endpoint
        //  return 401 instead of redirecting.)
        HttpSession session = req.getSession(false);
        AmsDataLocal local = (session != null)
                ? (AmsDataLocal) session.getAttribute("local")
                : null;
        if (local == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        // ── route on query parameters ──────────────────────────────────────
        String orgIdParam = req.getParameter("orgId");
        String nameParam  = req.getParameter("name");

        EntityManagerFactory emf =
                (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            if (orgIdParam != null) {
                handleOrgId(orgIdParam, em, resp);
            } else if (nameParam != null) {
                handleName(nameParam, em, resp);
            } else {
                handleAll(em, resp);
            }
        } catch (Exception e) {
            log.error("Unexpected error in EmployerInventoryApi", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Internal error\"}");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    // ── route handlers ────────────────────────────────────────────────────────

    /** GET ?orgId=N — single employer lookup */
    private void handleOrgId(String orgIdParam,
                              EntityManager em,
                              HttpServletResponse resp) throws IOException {
        long orgId;
        try {
            orgId = Long.parseLong(orgIdParam.trim());
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid orgId\"}");
            return;
        }

        EmployerInventoryDTO dto = EmployerInventoryDAO.getByOrganizationId(em, orgId);
        if (dto == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\":\"Not found\",\"orgId\":" + orgId + "}");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(dto));
    }

    /** GET ?name=X — case-insensitive name search, returns array */
    private void handleName(String nameParam,
                             EntityManager em,
                             HttpServletResponse resp) throws IOException {
        if (nameParam.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"name parameter must not be blank\"}");
            return;
        }

        List<EmployerInventoryDTO> results =
                EmployerInventoryDAO.searchByName(em, nameParam.trim());
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(results));
    }

    /** GET (no params) — all in-scope employers, returns array */
    private void handleAll(EntityManager em,
                           HttpServletResponse resp) throws IOException {
        List<EmployerInventoryDTO> results = EmployerInventoryDAO.getAllInScope(em);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(results));
    }
}
