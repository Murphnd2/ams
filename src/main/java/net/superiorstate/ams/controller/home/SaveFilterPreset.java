package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.ActivityFilter;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.UserFilterPreset;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "SaveFilterPreset", value = "/SaveFilterPreset")
public class SaveFilterPreset extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentUser() == null) {
            response.sendRedirect("login");
            return;
        }

        int slotNumber;
        try {
            slotNumber = Integer.parseInt(request.getParameter("slotNumber"));
        } catch (Exception e) {
            response.sendRedirect("ViewHome25");
            return;
        }

        String label = request.getParameter("label");
        if (label == null || label.trim().isEmpty()) {
            label = "Preset " + slotNumber;
        }
        if (label.length() > 16) {
            label = label.substring(0, 16);
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            TypedQuery<UserFilterPreset> q = em.createQuery(
                "SELECT p FROM UserFilterPreset p WHERE p.user = :user AND p.slotNumber = :slot",
                UserFilterPreset.class);
            q.setParameter("user", local.getCurrentUser());
            q.setParameter("slot", slotNumber);
            UserFilterPreset preset = q.getSingleResult();

            ActivityFilter af = local.getActivityFilter();

            em.getTransaction().begin();
            preset.setLabel(label.trim());
            preset.setViewRenewal(af.isViewRenewal());
            preset.setViewSetup(af.isViewSetup());
            preset.setViewTicket(af.isViewTicket());
            preset.setViewOpportunity(af.isViewOpportunity());
            preset.setOwnershipFilter(af.getOwnershipFilter());
            preset.setAttentionFilter(af.getAttentionFilter());
            preset.setSortAlphabetically(af.isSortAlphabetically());
            em.merge(preset);
            em.getTransaction().commit();

            // Reload all 3 presets into session
            TypedQuery<UserFilterPreset> reload = em.createQuery(
                "SELECT p FROM UserFilterPreset p WHERE p.user = :user ORDER BY p.slotNumber",
                UserFilterPreset.class);
            reload.setParameter("user", local.getCurrentUser());
            local.setFilterPresets(reload.getResultList());
            request.getSession().setAttribute("local", local);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }

        response.sendRedirect("ViewHome25");
    }
}
