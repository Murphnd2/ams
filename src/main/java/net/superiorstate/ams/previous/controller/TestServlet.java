package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.Activity;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "TestServlet", value = "/TestServlet")
public class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        getActivityData(em);
        em.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        getActivityData(em);
        em.close();
    }

    private void getActivityData(EntityManager em){
        Query q= em.createQuery("SELECT a FROM Activity a LEFT OUTER JOIN Note n ON a.id = n.activity.id WHERE n.id is null OR n.id=(SELECT MAX(n.id) FROM Note x WHERE  (x.status.id = 3 OR x.status.id=1) and x.activity.id = a.id) ");
        List<Activity> activityList = null;
        try{
            activityList = (List<Activity>) q.getResultList();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("SIZE: " + activityList.size());
        System.out.println("");

    }
}
