package net.superiorstate.ams.controller.api;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read and write constants on this installation.
 * GET  /api/v1/system/constants — returns all constants
 * PUT  /api/v1/system/constants — update a single constant (name + value)
 * Authenticated via ApiTokenFilter (master token required).
 */
@WebServlet("/api/v1/system/constants")
public class SystemConstantsApi extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"System not initialized\"}");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            List<Constant> constants = em.createQuery(
                    "SELECT c FROM Constant c ORDER BY c.name", Constant.class)
                    .getResultList();

            List<Map<String, String>> result = new ArrayList<>();
            for (Constant c : constants) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("name", c.getName());
                entry.put("value", c.getValue());
                entry.put("note", c.getNote());
                result.add(entry);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(result));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Failed to read constants\"}");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPut(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"System not initialized\"}");
            return;
        }

        Map<String, String> body = ApiClient.readJsonBody(request);
        String name = body.get("name");
        String value = body.get("value");

        if (name == null || name.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"name is required\"}");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Constant c = em.find(Constant.class, name);
            if (c == null) {
                // Create new constant
                c = new Constant();
                c.setName(name);
                c.setValue(value);
                em.persist(c);
            } else {
                c.setValue(value);
                em.merge(c);
            }

            em.getTransaction().commit();

            // Refresh global constants cache
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            if (global != null) {
                EntityManager em2 = emf.createEntityManager();
                try {
                    global.initializeGlobalData(em2);
                } finally {
                    if (em2.isOpen()) em2.close();
                }
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\": \"OK\", \"name\": \"" + name + "\"}");
            System.out.println("[SYSTEM-API] Constant updated: " + name + " = " + value);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Failed to update constant\"}");
            System.out.println("[SYSTEM-API] SystemConstantsApi PUT error: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}
