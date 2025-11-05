package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ResetEmailView", value = "/ResetEmailView")
public class ResetEmailView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoEmailHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        setRecipientList(request,em);
        clearCache(request);
        em.close();
    }

    private void clearCache(HttpServletRequest request) {

        String optionParam = request.getParameter("opt");
        if(optionParam==null)
            optionParam="";

        String optionParameter;
        try {
            optionParameter = request.getAttribute("opt").toString();
        } catch (Exception e){
            optionParameter=null;
        }
        if(optionParameter==null)
            optionParameter="";

        request.getSession().setAttribute("attachmentList", new ArrayList<>());
        request.getSession().setAttribute("currentEmailString", "");
        request.getSession().setAttribute("personNotFound", false);
        if(optionParameter.equals("doc") || optionParam.equals("doc")){
            String curEmailSub = request.getSession().getAttribute("tempSubject").toString();
            String curEmailMessage = request.getSession().getAttribute("tempMessage").toString();
            request.getSession().setAttribute("currentEmailSubject", curEmailSub);
            request.getSession().setAttribute("messageBody", curEmailMessage);
        } else {
            request.getSession().setAttribute("currentEmailSubject", "");
            request.getSession().setAttribute("messageBody", null);
        }
    }
    private void setRecipientList(HttpServletRequest request, EntityManager em){
        List<Person> recipientList = new ArrayList<>();
        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        if(sVar!=null && sVar.getCurrentActivity()!=null) {
            Activity a = sVar.getCurrentActivity();
            if(a!=null) {
                Person primaryContact;
                if(a.getPrimaryContact()==null)
                    primaryContact = dActivity.getPrimaryContact(em,a);
                else
                    primaryContact = a.getPrimaryContact();
                if(primaryContact==null)
                    return;

                if(primaryContact.getEmployee()!=null && primaryContact.getEmployee().getEmail()!=null && V.isValidEmail(primaryContact.getEmployee().getEmail()))
                    recipientList.add(getPerson(em,primaryContact));
                else if(primaryContact.getEmail()!=null && V.isValidEmail(primaryContact.getEmail()))
                    recipientList.add(primaryContact);

                if(a.getAssigneeContactList()!=null && a.getAssigneeContactList().size()>0){
                    for(Person p: a.getAssigneeContactList()){
                        if(p.getEmployee()!=null && p.getEmployee().getEmail()!=null && V.isValidEmail(p.getEmployee().getEmail()))
                            recipientList.add(getPerson(em,p));
                        else if(p.getEmail()!=null && V.isValidEmail(p.getEmail()))
                            recipientList.add(p);
                    }
                }
            }
        }
        request.getSession().setAttribute("recipientList",recipientList);
    }

    private Person getPerson(EntityManager em, Person primaryContact){
        if(primaryContact.getEmail()==null || !primaryContact.getEmail().equalsIgnoreCase(primaryContact.getEmployee().getEmail())){
            Person p = dM.getPersonById(em,primaryContact.getId());
            em.getTransaction().begin();
            p.setEmail(primaryContact.getEmail());
            em.persist(p);
            em.getTransaction().commit();
            em.refresh(p);
            return p;
        } else return primaryContact;
    }

}
