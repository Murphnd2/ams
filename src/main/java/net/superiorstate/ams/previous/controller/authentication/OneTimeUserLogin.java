package net.superiorstate.ams.previous.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbAuth;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

@WebServlet(name = "OneTimeUserLogin", value = "/OneTimeUserLogin")
public class OneTimeUserLogin extends HttpServlet {
    private String guid;
    private Date rightNow;
    private String errorText;
    private String landingPage;

    private boolean allowPasswordReset;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setInitialVariables(request);
        User user = getUserFromGuid();
        setLandingPage("/failurePage.jsp");
        if(user != null){
            if(user.isAllowSetPassword()){
                processPasswordReset(user,request);
            } else {
                processOneTimeLogin(user,request);
            }
        }
        request.getSession().setAttribute("errorText",getErrorText());
        goToPage(request,response,getLandingPage());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Date getRightNow() {
        return rightNow;
    }

    public void setRightNow(Date rightNow) {
        this.rightNow = rightNow;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public String getLandingPage() {
        return landingPage;
    }

    public void setLandingPage(String landingPage) {
        this.landingPage = landingPage;
    }

    public boolean isAllowPasswordReset() {
        return allowPasswordReset;
    }

    public void setAllowPasswordReset(boolean allowPasswordReset) {
        this.allowPasswordReset = allowPasswordReset;
    }

    private void processPasswordReset(User user, HttpServletRequest request){
        request.getSession().setAttribute("hiddenText","");
        Date expirationDate = user.getGuidExpiration();
        if(expirationDate.compareTo(getRightNow())>=0){
            if(!user.isGuidUsed()){
                request.getSession().setAttribute("tempUser",user);
                setLandingPage("/WEB-INF/view/authentication/resetPassword.jsp");
                setErrorText("");
                guidIsUsed();
            } else {
                setErrorText("Temporary Login Has Already Been Used!");
            }
        }
    }
    private void processOneTimeLogin(User user, HttpServletRequest request){
        Date expirationDate = user.getGuidExpiration();
        if(expirationDate.compareTo(getRightNow())>=0){
            if(!user.isGuidUsed()){
                authenticateUserNow(request,user);
                setLandingPage("/index.jsp");
                setErrorText("");
                guidIsUsed();
            } else {
                setErrorText("Temporary Login Has Already Been Used!");
            }
        }
    }
    private void authenticateUserNow(HttpServletRequest request, User user){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        User currentUser = dbAuth.getUserByUserName(em,user.getUserName());
        Person currentPerson = dbAuth.getPersonByUser(em,currentUser);
        request.getSession().setAttribute("isAuthenticated",true);
        request.getSession().setAttribute("currentUser",currentUser);
        request.getSession().setAttribute("currentPerson",currentPerson);
        request.getSession().setAttribute("psp",currentPerson.getPsp());
        dbAuth.assignUserRoles(request,currentUser);
        request.getSession().setAttribute("staffList",dbAuth.getPspStaff(em,currentPerson.getPsp()));
        em.close();
    }
    private void setInitialVariables(HttpServletRequest request){
        setGuid(request.getParameter("guid"));
        setRightNow(Date.from(Instant.now()));
        setErrorText("Invalid Login Code");
        setLandingPage("/index.jsp");
        request.getSession().setAttribute("hiddenText","hidden");
    }

    private EntityManager getEntityManager(){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }

    private void guidIsUsed(){
        EntityManager em = getEntityManager();
        Query q = em.createQuery("SELECT u FROM User u WHERE u.tempGuid = :guid");
        q.setParameter("guid",getGuid());
        User user = (User) q.getSingleResult();
        em.getTransaction().begin();
        user.setGuidUsed(true);
        em.persist(user);
        em.getTransaction().commit();
        em.close();
    }

    private User getUserFromGuid(){
        EntityManager em = getEntityManager();
        Query q = em.createQuery("SELECT u FROM User u WHERE u.tempGuid = :guid");
        q.setParameter("guid",getGuid());
        User user = null;
        try{
            user = (User) q.getSingleResult();
            setErrorText("Temporary Login Has Expired!");
            System.out.println("FOUND USER FROM GUID");
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            em.close();
            return user;
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response, String landingPage) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher(landingPage);
        dispatcher.forward(request,response);
    }
}
