package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.model.general.User;

import java.io.IOException;

@WebServlet(name = "ResetLogin", value = "/ResetLogin")
public class ResetLogin extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        initialAction(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        initialAction(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

    private void goBack(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/resetPassword.jsp");
        dispatcher.forward(request,response);
    }
    private void initialAction(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        if(validateResetPassword(request)){
            goToPage(request,response);
        } else {
            request.getSession().setAttribute("hiddenText","");
            goBack(request,response);
        }
    }

    private boolean validateResetPassword(HttpServletRequest request) {
        String pass1 = request.getParameter("newPassword1");
        String pass2 = request.getParameter("newPassword2");
        User tempUser = (User) request.getSession().getAttribute("tempUser");
        boolean passwordWasReset = false;
        if(pass1.compareTo(pass2)==0){
            passwordWasReset = updatePassword(request,pass1,tempUser);
            if(!passwordWasReset)
                request.getSession().setAttribute("errorText", "Unable to Complete Action - Contact Administrator");
        } else {
            request.getSession().setAttribute("errorText","Passwords Do Not Match - Try Again");
        }
        return passwordWasReset;
    }
    private boolean updatePassword(HttpServletRequest request, String passwordToHash,User user){
        boolean updated = false;
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();
        try {
            String salt = AuthDAO.generateSalt();
            String hash = AuthDAO.generatePasswordHash(passwordToHash,salt);
            Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :username");
            q.setParameter("username",user.getUserName());
            User currentUser = (User) q.getSingleResult();
            em.getTransaction().begin();
            currentUser.setGuidUsed(true);
            currentUser.setAllowSetPassword(false);
            currentUser.setSalt(salt);
            currentUser.setPasswordHash(hash);
            em.persist(currentUser);
            em.getTransaction().commit();
            updated = true;
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
            return updated;
        }
    }
}
