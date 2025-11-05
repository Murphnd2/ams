package net.superiorstate.ams.previous.controller.xtra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.PersonV;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RefreshPersonNames", value = "/RefreshPersonNames")
public class RefreshPersonNames extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void doThisFirst(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.firstName=:fName OR p.lastName = :lName");
        q.setParameter("fName","EMPLOYER");
        q.setParameter("lName","CONTACT");
        List<PersonV> personList;
        try{
            personList = (List<PersonV>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(personList == null || personList.size()>0)
            return;
        for(PersonV person:personList){
            Person p = dM.getPersonById(em,person.getId());
            String fName;
            String lName;
            if(p.getEmail()!=null && dbEmail.isValidEmail(p.getEmail())){
                fName = getFirstFromEmail(p.getEmail());
                lName = getLastFromEmail(p.getEmail());
                em.getTransaction().begin();
                p.setFullName(fName.toUpperCase().trim()+ " " + lName.toUpperCase().trim());
                em.persist(p);
                em.getTransaction().commit();
            }

        }
    }
    private String getFirstFromEmail(String email){
        if(email.contains("@") && email.contains(".") && email.indexOf(".") < email.indexOf("@")){
            return email.substring(0,email.indexOf("."));
        } else {
            return email.substring(0,email.indexOf("@"));
        }
    }

    private String getLastFromEmail(String email){
        if(email.contains("@") && email.contains(".") && email.indexOf(".") < email.indexOf("@")){
            return email.substring(email.indexOf(".")+1,email.indexOf("@"));
        } else {
            return email.substring(email.indexOf("@")+1,email.indexOf("."));
        }
    }
}
