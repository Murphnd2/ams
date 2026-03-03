package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.util.BillingHelper;

import java.io.IOException;

/**
 * Clears all billing data for the current month.
 * Deletes child tables in correct FK order to avoid constraint violations.
 *
 * URL: /ClearBilling25
 * Location: src/main/java/net/superiorstate/ams/controller/monthly/ClearBilling25.java
 */
@WebServlet(name = "ClearBilling25", value = "/ClearBilling25")
public class ClearBilling25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        clearMonthBilling();
        forwardToHome(request, response);
    }

    private void clearMonthBilling() {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            int monthId = BillingHelper.resolveMonthId(em);
            if (monthId == -1) return; // no billing month exists, nothing to clear

            em.getTransaction().begin();

            // Delete in FK-safe order: children first, then parent

            // 1. BillingLink references BillingMonth
            em.createQuery("DELETE FROM BillingLink bl WHERE bl.billingMonth.monthId = :mId")
                    .setParameter("mId", monthId)
                    .executeUpdate();

            // 2. BillingGrid references BillingMonth
            em.createQuery("DELETE FROM BillingGrid bg WHERE bg.billingMonth.monthId = :mId")
                    .setParameter("mId", monthId)
                    .executeUpdate();

            // 3. BillingItem references BillingMonth
            em.createQuery("DELETE FROM BillingItem bi WHERE bi.billingMonth.monthId = :mId")
                    .setParameter("mId", monthId)
                    .executeUpdate();

            // 4. CoverageStatus by month date
            em.createQuery("DELETE FROM CoverageStatus cs WHERE cs.monthFor = :mf")
                    .setParameter("mf", BillingHelper.getMonthFor())
                    .executeUpdate();

            // 5. Coverage (staging table, no month FK — full clear)
            em.createQuery("DELETE FROM Coverage").executeUpdate();

            // 6. Enrollment2 (staging table — full clear)
            em.createQuery("DELETE FROM Enrollment2").executeUpdate();

            // 7. Now safe to delete BillingMonth
            em.createQuery("DELETE FROM BillingMonth bm WHERE bm.monthId = :mId")
                    .setParameter("mId", monthId)
                    .executeUpdate();

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    private void forwardToHome(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }
}
