package net.superiorstate.ams.previous.controller.activity.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "ChangeChecklistDueDate", value = "/ChangeChecklistDueDate")
public class ChangeChecklistDueDate extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateSequence(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateSequence(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void updateSequence(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        request.getSession().setAttribute("lastTab",4);
        CheckList c = (CheckList) request.getSession().getAttribute("currentChecklist");
        Date newDueDate = Date.valueOf(request.getParameter("newDueDate"));
        em.getTransaction().begin();
        CheckList checkList = dM.getCheckListById(em,c.getId());
        checkList.setDueDate(newDueDate);
        em.persist(checkList);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentChecklist", new CheckList());
        request.getSession().setAttribute("adminView",99);
    }
}
