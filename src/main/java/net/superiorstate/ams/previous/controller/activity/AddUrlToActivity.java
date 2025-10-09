package net.superiorstate.ams.previous.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;

@WebServlet(name = "AddUrlToActivity", value = "/AddUrlToActivity")
public class AddUrlToActivity extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addDocNow(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void addDocNow(HttpServletRequest request) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManager em = getEntityManager();

        try {
            request.getSession().setAttribute("lastTab", 3);

            Activity currentActivity = local.getCurrentActivity().getActivity();
            if (currentActivity == null) return;

            String urlPath = request.getParameter("urlUpload");
            String urlName = request.getParameter("urlName");
            String description = (urlName != null && !urlName.isEmpty()) ? urlName : urlPath;

            WebLink newLink = createAndPersistWebLink(em, description, urlPath);

            Activity refreshedActivity = dM.getActivityById(em, currentActivity.getId());
            if (refreshedActivity == null) return;

            attachLinkToActivity(em, refreshedActivity, newLink);

            local.getCurrentActivity().setActivity(refreshedActivity);
            request.getSession().setAttribute("local",local);
        } finally {
            em.close();
        }
    }

    private EntityManager getEntityManager() {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }

    private WebLink createAndPersistWebLink(EntityManager em, String description, String urlPath) {
        LinkType linkType = ddC.getLinkTypeById(em, 2);
        WebLink link = new WebLink();
        link.setPlainText(description);
        link.setLinkPath(urlPath);
        link.setLinkType(linkType);

        em.getTransaction().begin();
        em.persist(link);
        em.getTransaction().commit();

        return link;
    }

    private void attachLinkToActivity(EntityManager em, Activity activity, WebLink link) {
        em.getTransaction().begin();
        activity.addWebLink(link);
        em.persist(activity);
        em.getTransaction().commit();
    }

}
