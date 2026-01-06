package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.ActivityLandingDao;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ActivityLandingFilter;
import net.superiorstate.ams.model.ActivityLandingRow;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ViewHome25", value = "/ViewHome25")
public class ViewHome25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processData(request);
        loadLandingRows(request);      // <-- Step 1 add
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processData(request);
        loadLandingRows(request);      // <-- Step 1 add
        goToPage(request, response);
    }

    private void processData(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        // NOTE: Leaving your existing behavior intact for now.
        // We'll remove/replace the refilter/session-list logic in a later step.

        if (local.getCurrentActivity().isReFilterOnExit()) {
            local.setFilteredActivityList(local.filterActivityListing());
            System.out.println("-----I REFILTERED !!!!------------------------------------------------------------");
        }

        if (local.getCurrentActivity() != null && local.getCurrentActivity().isReFilterOnExit()) {
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(null);
        } else if (local.getCurrentActivity() != null && local.getCurrentActivity().getActivity() != null) {
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(new ArrayList<>());
        }

        local.getCurrentEmail().setRecipientList(new ArrayList<>());
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setSubject("");
        local.getCurrentEmail().setBody("");

        // Persist ToDo completion changes (your existing block)
        if (local.getCurrentActivity().isReFilterOnExit()) {
            EntityManagerFactory emf =
                    (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                for (ToDoOut25 t : local.getCurrentActivity().getToDoList()) {
                    if (t.isComplete() != t.wasComplete() && t.getToDo().getId() != null) {
                        ToDo managed = em.find(ToDo.class, t.getToDo().getId());
                        if (managed != null) {
                            managed.setComplete(t.isComplete());
                            managed.setCompletedBy(t.getToDo().getCompletedBy());
                            managed.setDateCompleted(t.getToDo().getDateCompleted());
                        }
                    }
                }
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }
        }

        request.getSession().setAttribute("local", local);
    }

    /**
     * Step 1: Populate requestScope.activityRows for the JSP.
     * This is the new SQL-first landing dataset.
     */
    private void loadLandingRows(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        long me = local.getCurrentPerson().getId();

        AmsDataGlobal global =
                (AmsDataGlobal) getServletContext().getAttribute("global");

        int daysWarn = (global != null) ? global.getDaysSinceWarning() : 7;

        ActivityLandingFilter f = new ActivityLandingFilter();

        int own = local.getActivityFilter().getOwnershipFilter();
        f.myOpenOnly = (own == 1 || own == 2);

        f.includeRenewal = local.getActivityFilter().isViewRenewal();
        f.includeSetup   = local.getActivityFilter().isViewSetup();
        f.includeTicket  = local.getActivityFilter().isViewTicket();

        f.viewNeedsContact = local.getActivityFilter().isViewNeedsContact();
        f.viewWaitingOnUs  = local.getActivityFilter().isViewWaitingOnUs();

        f.sortAlphabetically = local.getActivityFilter().isSortAlphabetically();

        f.pageSize = 500;
        f.offset = 0;

        EntityManagerFactory emf =
                (EntityManagerFactory) getServletContext().getAttribute("emf");

        ActivityLandingDao dao = new ActivityLandingDao(emf);

        List<ActivityLandingRow> rows = dao.fetchLandingRows(me, daysWarn, f);

        request.setAttribute("activityRows", rows);
    }


    private void goToPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher =
                request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome25.jsp");
        dispatcher.forward(request, response);
    }
}
