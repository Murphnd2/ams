package net.superiorstate.ams.previous.controller.activity.ticket;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CreateAutoCategory", value = "/CreateAutoCategory")
public class CreateAutoCategory extends HttpServlet {

    private String categoryDropDown;
    private long categoryId;
    private TicketCategory category;
    private String nameOfAutomation;
    private long subCategoryId;
    private TicketSubCategory subCategory;
    private int templatePurposeId;
    private TemplatePurpose templatePurpose;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createAutoCategory(request,response);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createAutoCategory(request,response);
        goToPage(request,response);
    }

    private void createAutoCategory(HttpServletRequest request, HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        retrieveFormDataAndAssignLocally(request,em);
        createTemplatePurpose(em);
        createTicketSubcategory(em);
        repopList(request,em);

        em.close();

    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SequenceHome");
        dispatcher.forward(request,response);
    }

    private void createTemplatePurpose(EntityManager em){
        em.getTransaction().begin();
        TemplatePurpose tp = new TemplatePurpose();
        tp.setDescription(nameOfAutomation);
        tp.setSortOrder(100);
        tp.setTemplateGroup(dM.getTemplateGroupById(em,3));
        em.persist(tp);
        em.getTransaction().commit();
        templatePurpose = tp;
        templatePurposeId = tp.getId();
    }
    private void createTicketSubcategory(EntityManager em){
        em.getTransaction().begin();
        TicketSubCategory tsc = new TicketSubCategory();
        tsc.setTicketCategory(category);
        tsc.setActive(true);
        tsc.setTemplatePurpose(templatePurpose);
        tsc.setDescription(nameOfAutomation);
        em.persist(tsc);
        em.getTransaction().commit();
        subCategory = tsc;
        subCategoryId = tsc.getId();
    }

    private void retrieveFormDataAndAssignLocally(HttpServletRequest request, EntityManager em){
        categoryDropDown = request.getParameter("tCategory");
        categoryId = Long.parseLong(categoryDropDown);
        category = dM.getTicketCategoryById(em,categoryId);
        nameOfAutomation = request.getParameter("automationName");

    }

    private void repopList(HttpServletRequest request,EntityManager em){
        Query q = em.createQuery("SELECT tp FROM TemplatePurpose tp order by tp.templateGroup.id,tp.description");
        List<TemplatePurpose> tpList = (List<TemplatePurpose>) q.getResultList();
        request.getSession().setAttribute("templatePurposeList",tpList);
    }
}
