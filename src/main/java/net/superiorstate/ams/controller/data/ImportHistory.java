package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.model.imports.ImportRunLog;

import java.io.IOException;
import java.util.List;

/**
 * Import History — shows import_run_log records for the current PSP.
 * PSP Admin (role 5) or BPO Admin (role 102) only.
 */
@WebServlet(name = "ImportHistory", value = "/ImportHistory")
public class ImportHistory extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        Boolean isBpoAdmin = (Boolean) request.getSession().getAttribute("isBpoAdmin");
        if ((isPspAdmin == null || !isPspAdmin) && (isBpoAdmin == null || !isBpoAdmin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        Object pspIdObj = request.getSession().getAttribute("pspId");
        long pspId = (pspIdObj instanceof Number) ? ((Number) pspIdObj).longValue() : 4L;

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<ImportRunLog> runs = em.createQuery(
                    "SELECT r FROM ImportRunLog r WHERE r.provider.pspId = :pspId ORDER BY r.id DESC",
                    ImportRunLog.class)
                    .setParameter("pspId", pspId)
                    .setMaxResults(100)
                    .getResultList();
            request.setAttribute("runs", runs);
            request.getRequestDispatcher("/WEB-INF/view/a/general/universalImport/importHistory.jsp")
                    .forward(request, response);
        } finally {
            em.close();
        }
    }
}
