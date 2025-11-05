package net.superiorstate.ams.previous.controller.summit.twtw;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Helper;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "UpdateTables", value = "/UpdateTables")
public class UpdateTables extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateTheTablesAlt();
        goToPage(request,response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        updateTheTablesAlt();
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

    private void updateTheTablesAlt(){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        createHsaEmployers(em);
        em.close();

    }

    private void createHsaEmployers(EntityManager em){

    }
    public static Date getMonthFor(){
        return Helper.getMonthFor();
    }


}
