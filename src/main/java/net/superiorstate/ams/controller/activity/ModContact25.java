package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;

@WebServlet(name = "ModContact25", value = "/ModContact25")
public class ModContact25 extends HttpServlet {
    private Person primaryContact;
    private Person currentPerson;
    private Activity currentActivity;
    private String currentFirst;
    private String currentLast;
    private String currentEmail;
    private String newFirst;
    private String newLast;
    private String newEmail;

    public Person getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(Person primaryContact) {
        this.primaryContact = primaryContact;
    }

    public Person getCurrentPerson() {
        return currentPerson;
    }

    public void setCurrentPerson(Person currentPerson) {
        this.currentPerson = currentPerson;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public String getCurrentFirst() {
        return currentFirst;
    }

    public void setCurrentFirst(String currentFirst) {
        this.currentFirst = currentFirst;
    }

    public String getCurrentLast() {
        return currentLast;
    }

    public void setCurrentLast(String currentLast) {
        this.currentLast = currentLast;
    }

    public String getCurrentEmail() {
        return currentEmail;
    }

    public void setCurrentEmail(String currentEmail) {
        this.currentEmail = currentEmail;
    }

    public String getNewFirst() {
        return newFirst;
    }

    public void setNewFirst(String newFirst) {
        this.newFirst = newFirst;
    }

    public String getNewLast() {
        return newLast;
    }

    public void setNewLast(String newLast) {
        this.newLast = newLast;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        modifyContact(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }
    private void modifyContact(HttpServletRequest request){
        //Retrieve Session Variables
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        setCurrentActivity(local.getCurrentActivity().getActivity());
        setCurrentPerson(local.getCurrentPerson());
        setPrimaryContact(local.getCurrentActivity().getPrimaryContact());
        setCurrentFirst(local.getCurrentActivity().getPrimaryContact().getFirstName());
        setCurrentLast(local.getCurrentActivity().getPrimaryContact().getLastName());
        setCurrentEmail(local.getCurrentActivity().getPrimaryContact().getEmail());

        //Retrieve Form Parameters;
        try{
            setNewFirst(request.getParameter("contactFirst").toString());
            setNewLast(request.getParameter("contactLast").toString());
            setNewEmail(request.getParameter("contactEmail").toString());
        } catch (Exception e){return;}

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        //

        //Name Modification Check
        boolean updateFirstName = false;
        if(getNewFirst()!=null && !getNewFirst().equals("") && (getCurrentFirst()==null || !getNewFirst().equalsIgnoreCase(getCurrentFirst())))
            updateFirstName = true;

        boolean updateLastName = false;
        if(getNewLast()!=null && !getNewLast().equals("") && (getCurrentLast()==null || !getNewLast().equalsIgnoreCase(getCurrentLast())))
            updateLastName = true;

        boolean updateEmail = false;
        if(getNewEmail()!=null && Validator.isValidEmail(getNewEmail()) && (getCurrentEmail()==null || !getNewEmail().equalsIgnoreCase(getCurrentEmail())))
            updateEmail = true;

        if(!updateFirstName && !updateLastName && !updateEmail)
            return;

        Person contact = EntityLookup.getPersonById(em, getPrimaryContact().getId());
        em.getTransaction().begin();
        if(updateEmail)
            contact.setEmail(getNewEmail().toLowerCase().trim());
        if(updateFirstName)
            contact.setFirstName(getNewFirst().toUpperCase().trim());
        if(updateLastName)
            contact.setLastName(getNewLast().toUpperCase().trim());
        em.persist(contact);
        em.getTransaction().commit();
        em.refresh(contact);

        local.getCurrentActivity().setPrimaryContact(contact);

        Activity a = EntityLookup.getActivityById(em,local.getCurrentActivity().getActivity().getId());
        if(a!=null){
            em.getTransaction().begin();
            a.setPrimaryContact(contact);
            em.persist(contact);
            em.getTransaction().commit();
            em.refresh(a);
        }
        local.getCurrentActivity().setActivity(a);

        request.getSession().setAttribute("local", local);

        em.close();
    }
}
