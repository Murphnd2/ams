package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.AddContactToActivity;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendTheEmployerBilling", value = "/SendTheEmployerBilling")
public class SendTheEmployerBilling extends HttpServlet {
    private List<Person> distributionList;

    public List<Person> getDistributionList() {
        return distributionList;
    }

    public void setDistributionList(List<Person> distributionList) {
        this.distributionList = distributionList;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        sendTheBilling(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetBillingView");
        dispatcher.forward(request,response);
    }

    private void sendTheBilling(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        initializeDistributionList(request,em);

        //Anyone to send it to?
        if(getDistributionList()==null || getDistributionList().size()==0)
            return;



        em.close();
    }

    private void initializeDistributionList(HttpServletRequest request, EntityManager em){
        List<Person> disList = new ArrayList<>();

        //Review Preset List
        List<Person> availableEmails = (List<Person>) request.getSession().getAttribute("employerContactList");
        if(availableEmails!=null && availableEmails.size()>0){
            for(int i=0;i<availableEmails.size();i++){
                int j = i+1;
                String parameterName = "eCheck" + j;
                String parameterValue = request.getParameter(parameterName);
                if(parameterValue==null)
                    continue;
                if(!disList.contains(availableEmails.get(i)))
                    disList.add(availableEmails.get(i));
            }
        }

        //Review Free Text Box Entries (if any)
        String additionalEmails = request.getParameter("additionalEmails");
        if(additionalEmails!=null && !additionalEmails.equals("")){
            Employer employer = (Employer) request.getSession().getAttribute("currentBillingEmployer");
            List<String> emailList = getEmailList(additionalEmails);
            for(String e: emailList){
                Person p = AddContactToActivity.investigateEmail(em,e,employer);
                if(!disList.contains(p))
                    disList.add(p);
            }
        }
        setDistributionList(disList);
    }

    private List<String> getEmailList(String start){
        if(start==null || start.equals(""))
            return null;
        List<String> emailList = new ArrayList<>();
        if(!start.contains(";")){
            emailList.add(start.toLowerCase());
        } else {
            String remainingText = start.trim();
            if(remainingText.endsWith(";"))
                remainingText = remainingText.substring(0,remainingText.length()-1);

            String email;
            while(remainingText.contains(";")){
                int semiLoc = remainingText.indexOf(";");
                email = remainingText.substring(0,semiLoc).trim().toLowerCase();
                if(!emailList.contains(email))
                    emailList.add(email);
                remainingText = remainingText.substring(semiLoc+1);
            }
            if(!emailList.contains(remainingText))
                emailList.add(remainingText);
        }
        return emailList;
    }
}
