package net.superiorstate.ams.controller.emailItems;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "AddRecipient25", value = "/AddRecipient25")
public class AddRecipient25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addRecipient(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addRecipient(request);
        goToPage(request,response);
    }

    private void addRecipient(HttpServletRequest request){
        request.getSession().setAttribute("emailNotFound",false);
        request.getSession().setAttribute("personNotFound", false);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if(!V.isValidEmail(local.getCurrentEmail().getEmailToAdd())){
            request.getSession().setAttribute("emailNotFound",true);
            return;
        }
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String emailString = local.getCurrentEmail().getEmailToAdd().toLowerCase().trim();

        if(!foundPerson(em,local)) {
            request.getSession().setAttribute("personNotFound", true);
            return;
        }

        Person p = dbEmail.getPersonByEmail(em, emailString,local.getCurrentPerson().getPsp());

        if(p!=null && local.getCurrentEmail().getRecipientList().stream().noneMatch(obj->obj.getId().equals(p.getId())))
            local.getCurrentEmail().getRecipientList().add(p);

        local.getCurrentEmail().setEmailToAdd("");
        local.getCurrentEmail().setFirstName("");
        local.getCurrentEmail().setLastName("");
        request.getSession().setAttribute("local",local);

        em.close();
    }
    private boolean foundPerson(EntityManager em, AmsDataLocal local){
        String emailToAdd = local.getCurrentEmail().getEmailToAdd();
        if(dbEmail.getPersonByEmail(em,emailToAdd,local.getCurrentPerson().getPsp())!=null)
            return true;

        String fName = local.getCurrentEmail().getFirstName();
        if(fName==null || fName.equals(""))
            return false;

        String lName = local.getCurrentEmail().getLastName();
        if(lName==null || lName.equals(""))
            return false;

        createPerson(local,em);
        return true;
    }
    private void createPerson(AmsDataLocal local,EntityManager em){
        em.getTransaction().begin();
        Person p = new Person();
        p.setEmail(local.getCurrentEmail().getEmailToAdd());
        p.setLastName(local.getCurrentEmail().getLastName());
        p.setFirstName(local.getCurrentEmail().getFirstName());
        p.setFullName(local.getCurrentEmail().getFirstName() + " " + local.getCurrentEmail().getLastName());
        p.setPsp(local.getCurrentPerson().getPsp());
        em.persist(p);
        em.getTransaction().commit();
        em.refresh(p);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        dispatcher.forward(request,response);
    }
}
