package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.model.general.Person;
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
        request.setAttribute("errorMessage", "Invalid username or password");
        RequestDispatcher dispatcher = request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
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
            loadSessionData25(request,em,currentUser);
        }
        em.close();
        return validated;
    }

    private void loadSessionData25(HttpServletRequest request, EntityManager em, User u){
        Person p = AuthDAO.getPersonByUser(em,u);
        request.getSession().setAttribute("currentPerson",p);
        AmsDataLocal local = new AmsDataLocal();
        local.setAuthenticated(true);
        local.intializeLocalData(em,request);
        AuthDAO.assignUserRoles(request,u);

        request.getSession().setAttribute("local",local);
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
            if(!personList.contains(u.getPerson()))
                personList.add(u.getPerson());
        }
        Collections.sort(personList);
        return personList;    }



}
