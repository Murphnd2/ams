package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.dao.TimeTrackingDAO;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.TimeLog;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.UserRole;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AuthenticateUser", value = "/AuthenticateUser")
public class AuthenticateUser extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       doThis(request,response);
    }

    private void doThis(HttpServletRequest request,HttpServletResponse response){
        try {
            if (validatedLogin(request)){
                goToPage(request,response);
            } else {
                displayLoginFailure(request,response);
            }
        } catch (NoSuchAlgorithmException | ServletException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void displayLoginFailure(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getSession().setAttribute("loginError", "Invalid username or password");
        response.sendRedirect("login");
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
        boolean isBpo = Boolean.TRUE.equals(request.getSession().getAttribute("isBpo"));
        boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
        boolean isBpoUser = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoUser"));

        if (isBpo || isBpoAdmin || isBpoUser) {
            response.sendRedirect("BpoHome");
        } else if (isPspUser || isPspAdmin) {
            response.sendRedirect("ViewHome25");
        } else if (isAgent || isAgencyAdmin) {
            response.sendRedirect("AgentHome");
        } else {
            response.sendRedirect("ViewHome25");
        }
    }

    private boolean validatedLogin(HttpServletRequest request) throws NoSuchAlgorithmException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        // Get Credentials ----------------
        String userName = request.getParameter("userName");
        String password = request.getParameter("userPassword");
        // Validate Credentials
        boolean validated = AuthDAO.validateLogin(em,userName,password);
        if(validated){
            User currentUser = AuthDAO.getUserByUserName(em,userName);
            if (!currentUser.isActive()) {
                validated = false;
            } else {
                loadSessionData25(request, em, currentUser);
            }
        }
        em.close();
        return validated;
    }

    private void loadSessionData25(HttpServletRequest request, EntityManager em, User u){
        Person p = AuthDAO.getPersonByUser(em,u);
        request.getSession().setAttribute("currentPerson",p);
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        AmsDataLocal local = new AmsDataLocal(emf);
        local.setAuthenticated(true);
        local.intializeLocalData(em,request);
        AuthDAO.assignUserRoles(request, u);
        local.setPspAdmin((boolean) request.getSession().getAttribute("isPspAdmin"));
        // Initialize timeclock state from DB
        try {
            TimeLog lastPunch = TimeTrackingDAO.getMyLastPunch(em, u);
            local.setUserIsIn(lastPunch.isIn());
        } catch (Exception e) {
            local.setUserIsIn(false);
        }
        local.setMyTimeHistory(TimeTrackingDAO.getTodaysTimeHistory(em, p));
        request.getSession().setAttribute("local", local);
    }

    private static List<Person> getBpoUsers(EntityManager em){
        return getUsersByRole(em,101);
    }

    public static List<Person> getUsersByRole(EntityManager em, int roleId){
        Query q = em.createQuery("SELECT ur FROM UserRole ur WHERE ur.id = :id");
        q.setParameter("id",roleId);
        UserRole ur = (UserRole) q.getSingleResult();
        List<User> users = ur.getUserList();
        List<Person> personList = new ArrayList<>();
        if(users==null || users.size()==0)
            return personList;
        for(User u:users){
            if(u.isActive() && !personList.contains(u.getPerson()))
                personList.add(u.getPerson());
        }
        Collections.sort(personList);
        return personList;    }



}
