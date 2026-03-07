package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;

import java.io.IOException;

@WebServlet(name = "LogOut", value = "/LogOut")
public class LogOut extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logOutUser(request);
        response.sendRedirect("login");
    }

    private void logOutUser(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        request.getSession().setAttribute("local", new AmsDataLocal(emf));;
        request.getSession().setAttribute("isAuthenticated",false);
        request.getSession().setAttribute("currentUser",new User());
        request.getSession().setAttribute("currentPerson",new Person());
        request.getSession().setAttribute("psp",new PSP());
        AuthDAO.assignUserRoles(request,new User());
    }
}
