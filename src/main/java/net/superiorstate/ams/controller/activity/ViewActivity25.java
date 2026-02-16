package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;

import java.io.IOException;

@WebServlet(name = "ViewActivity25", value = "/ViewActivity25")
public class ViewActivity25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    private void processData(HttpServletRequest request) {
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null && global.isDelegationDirty()) {
            EntityManager em = getOpenEntityManager(request);
            try {
                global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
                global.clearDelegationDirty();
            } finally {
                em.close();
            }
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/activityDetail/activityDetail25.jsp");
        dispatcher.forward(request,response);
    }
    private EntityManager getOpenEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf =
                (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}
