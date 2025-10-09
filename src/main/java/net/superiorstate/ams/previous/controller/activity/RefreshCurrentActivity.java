package net.superiorstate.ams.previous.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.misc.dbRenew;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.application.ApplicationModule;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RefreshCurrentActivity", value = "/RefreshCurrentActivity")
public class RefreshCurrentActivity extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        refreshData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        refreshData(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void refreshData(HttpServletRequest request){
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        String activityType = a.getClass().getSimpleName();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        switch (activityType) {
            case "Setup":
                refreshSetupData(request,em,a);
                break;
            case "Renewal":
                refreshRenewalData(request,em,a);
                break;
            case "Ticket":
                refreshTicketData(request,em,a);
                break;
            case "CheckList":

                break;
            default:
                em.close();
                return;
        }
        refreshActivityData(request,em,a);
        em.close();
    }

    private void refreshActivityData(HttpServletRequest request, EntityManager em, Activity a){
        Activity selectedActivity = dM.getActivityById(em,a.getId());
        assert selectedActivity != null;
        Person primaryContact = dActivity.getPrimaryContact(em,selectedActivity);
        request.getSession().setAttribute("currentPrimaryContact",primaryContact);
        request.getSession().setAttribute("otherContactList",selectedActivity.getAssigneeContactList());
        request.getSession().setAttribute("activityWebLinkList",selectedActivity.getWebLinkList());
        request.getSession().setAttribute("currentActivityId",a.getId());
        request.getSession().setAttribute("currentActivity",selectedActivity);
        request.getSession().setAttribute("pastActivities", ViewSelectedActivity.getPastActivities(request,em,selectedActivity));
        CheckList c = ViewSelectedActivity.getCheckListForActivity(em,selectedActivity);
        request.getSession().setAttribute("currentChecklist",c);
    }

    private void refreshRenewalData(HttpServletRequest request,EntityManager em, Activity a){
        Renewal r = (Renewal) a;
        List<Employee> contactList = dbRenew.getEmployeesAssignedToRenewal(em,r);
        request.getSession().setAttribute("contactList", dbTicket.getTicketEmployeeList(em,contactList));
        List<Employee> employeeList = dbRenew.getContactsNotAssigned(em,r);
        request.getSession().setAttribute("remainingEmployees", dbTicket.getTicketEmployeeList(em,employeeList));
        List<Benefit> benefitsNotInRenewal = dbRenew.getBenefitsNotInRenewal(em,r);
        request.getSession().setAttribute("benefitsNotInRenewal",benefitsNotInRenewal);
    }

    private void refreshSetupData(HttpServletRequest request, EntityManager em, Activity a){
        Setup s = (Setup) a;
        List<Person> setupContactList = s.getContactList();
        List<ApplicationModule> moduleList = s.getApplication().getApplicationModuleList();
        Person agent = s.getApplication().getProposal().getProspect().getAgent();
        Person setupContact = new Person();
        if(s.getPrimaryContact()==null && s.getPrimaryContactSetup()!=null)
            setupContact = s.getPrimaryContactSetup();
        else if(s.getPrimaryContact()!=null)
            setupContact = s.getPrimaryContact();
        request.getSession().setAttribute("setupContactList",setupContactList);
        request.getSession().setAttribute("setupContact",setupContact);
        request.getSession().setAttribute("moduleList",moduleList);
        request.getSession().setAttribute("setupAgent",agent);
        String propLink = request.getContextPath() + "/serviceProposal?guid=" + s.getApplication().getProposal().getApplicationGUID();
        String appLink = Q.ONLINE_APPLICATION_DATA + "&entry=" + s.getApplication().getProposal().getApplicationGUID();
        request.getSession().setAttribute("proposalLink",propLink);
        request.getSession().setAttribute("appLink",appLink);
        List<TemplatePurpose> remainingModules = ViewSelectedActivity.remainingModules(request,em,moduleList);
        request.getSession().setAttribute("remainingMods",remainingModules);
    }

    private void refreshTicketData(HttpServletRequest request, EntityManager em, Activity a){
        Ticket t = (Ticket) a;
    }


}
