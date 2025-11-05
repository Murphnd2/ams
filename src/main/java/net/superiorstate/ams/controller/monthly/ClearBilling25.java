package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Cleaner;
import net.superiorstate.ams.data.Helper;

import java.io.IOException;

@WebServlet(name = "ClearBilling25", value = "/ClearBilling25")
public class ClearBilling25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
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
            Cleaner cleaner = new Cleaner(em) {}; // anonymous or subclass
            em.getTransaction().begin();

            int monthId = Helper.resolveMonthId(em);
            if (monthId != -1) {
                cleaner.deleteByMonthId("BillingGrid", "billingMonth.monthId", monthId);
                cleaner.deleteByMonthDate("CoverageStatus", "monthFor", Helper.getMonthFor());
                cleaner.deleteByMonthId("BillingMonth", "monthId", monthId);
            }

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
