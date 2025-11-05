package net.superiorstate.ams.previous.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbAuth;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.model.general.User;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@WebServlet(name = "HelpUserLogin", value = "/HelpUserLogin")
public class HelpUserLogin extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            processRequest(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            processRequest(request,response);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    private void processRequest(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException, MessagingException {

        User user = findUser(request);
        if(user == null){
            goToFailure(request,response);
        } else {
            sendRequestEmail(user,request);
            goToSuccess(request,response);
        }
    }
    private void sendRequestEmail(User user, HttpServletRequest request) throws MessagingException {
        int helpMethod = Integer.parseInt(request.getParameter("submitButton"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String guid = UUID.randomUUID().toString();
        String subject="";
        String message = "";
        if(helpMethod == 0){
            subject = "ONE TIME LOGIN LINK";
            updateUserData(em,user,false,guid);
            message = oneTimeLoginMessageHTML(guid);
        } else if(helpMethod == 1){
            updateUserData(em,user,true,guid);
            subject = "PASSWORD RESET REQUEST";
            message = resetPasswordMessageHTML(guid);
        }
        dbEmail.sendEmail("noreply@superiorstate.net",user.getEmail(),subject,message,em);
        em.close();
    }

    private String resetPasswordMessageHTML(String guid){
        String message = "";
        message += "<a href=\"https://superiorstate.biz/OneTimeUserLogin?guid=" + guid + "\" target=\"_blank\">LINK</a>";
        return message;
    }
    private String oneTimeLoginMessageHTML(String guid){
        String message = "";
        message += "<a href=\"https://superiorstate.biz/OneTimeUserLogin?guid=" + guid + "\" target=\"_blank\">LINK</a>";
        return message;
    }

    public static void updateUserData(EntityManager em, User user, boolean allowSetPassword, String guid) {
        em.getTransaction().begin();
        Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :username");
        q.setParameter("username",user.getUserName());
        User thisUser = (User) q.getSingleResult();
        thisUser.setTempGuid(guid);
        thisUser.setGuidUsed(false);
        thisUser.setAllowSetPassword(allowSetPassword);
        LocalDateTime expirationDateTime = LocalDateTime.now().plus(Duration.of(10, ChronoUnit.MINUTES));
        Date expirationDate = Date.from(expirationDateTime.atZone(ZoneId.systemDefault()).toInstant());
        thisUser.setGuidExpiration(expirationDate);
        em.persist(thisUser);
        em.getTransaction().commit();
    }
    private User findUser(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String userNameOrEmail = request.getParameter("userName");
        User user = dbAuth.getUserByUserName(em,userNameOrEmail);
        em.close();
        return user;
    }

    private void goToSuccess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

    private void goToFailure(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

}
