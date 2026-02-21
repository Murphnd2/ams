package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.sales.offering.MarketingMaterial;
import net.superiorstate.ams.model.sales.offering.ResourceCategory;

import java.io.IOException;
import java.util.*;

@WebServlet(name = "LibraryHome", value = "/LibraryHome")
public class LibraryHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        long pspId = local.getCurrentPerson().getPsp().getId();

        try {
            // Load all categories for this PSP
            List<ResourceCategory> categoryList = em.createQuery(
                    "SELECT c FROM ResourceCategory c WHERE c.psp.id = :pspId ORDER BY c.sortOrder", ResourceCategory.class)
                    .setParameter("pspId", pspId)
                    .getResultList();
            request.setAttribute("categoryList", categoryList);

            // Determine active category filter (null = show all)
            String catIdParam = request.getParameter("catId");
            Long activeCatId = null;
            if (catIdParam != null && !catIdParam.isEmpty()) {
                activeCatId = Long.parseLong(catIdParam);
                request.setAttribute("activeCatId", activeCatId);
            }

            // Load resources — filtered by category if selected, otherwise all
            List<MarketingMaterial> resourceList;
            if (activeCatId != null) {
                resourceList = em.createQuery(
                        "SELECT m FROM MarketingMaterial m WHERE m.psp.id = :pspId AND m.category.id = :catId ORDER BY m.sortOrder, m.title", MarketingMaterial.class)
                        .setParameter("pspId", pspId)
                        .setParameter("catId", activeCatId)
                        .getResultList();
            } else {
                resourceList = em.createQuery(
                        "SELECT m FROM MarketingMaterial m WHERE m.psp.id = :pspId ORDER BY m.sortOrder, m.title", MarketingMaterial.class)
                        .setParameter("pspId", pspId)
                        .getResultList();
            }
            request.setAttribute("resourceList", resourceList);

            // If a resource is selected, load it for the detail panel
            String resIdParam = request.getParameter("resId");
            if (resIdParam != null && !resIdParam.isEmpty()) {
                long resId = Long.parseLong(resIdParam);
                MarketingMaterial selected = findById(resourceList, resId);
                if (selected != null) {
                    request.setAttribute("selectedResource", selected);
                }
            }

        } finally {
            em.close();
        }

        request.setAttribute("adminCurrentPage", "library");
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/sales/library25.jsp");
        dispatcher.forward(request, response);
    }

    private MarketingMaterial findById(List<MarketingMaterial> list, long id) {
        for (MarketingMaterial m : list) {
            if (m.getId() == id) return m;
        }
        return null;
    }
}
