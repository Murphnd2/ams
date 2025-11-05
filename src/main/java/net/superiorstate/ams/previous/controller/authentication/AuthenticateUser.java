package net.superiorstate.ams.previous.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.checklist.dbCheck;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.misc.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplateGroup;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.general.UserRole;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

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
                displayLoginFailure(request);
            }
        } catch (NoSuchAlgorithmException | ServletException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void displayLoginFailure(HttpServletRequest request){
        //FIXME: show user the problem, maybe go to the login help page?
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
        boolean validated = dbAuth.validateLogin(em,userName,password);
        if(validated){
            User currentUser = dbAuth.getUserByUserName(em,userName);
            loadSessionData25(request,em,currentUser);
        }
        em.close();
        return validated;
    }

    private void loadSessionData25(HttpServletRequest request, EntityManager em, User u){
        Person p = dbAuth.getPersonByUser(em,u);
        request.getSession().setAttribute("currentPerson",p);
        AmsDataLocal local = new AmsDataLocal();
        local.setAuthenticated(true);
        local.intializeLocalData(em,request);
        dbAuth.assignUserRoles(request,u);

        request.getSession().setAttribute("local",local);
    }



    private void loadSessionData(HttpServletRequest se, EntityManager em, User u){
        Person currentPerson = dbAuth.getPersonByUser(em,u);
        se.getSession().setAttribute("isAuthenticated",true);
        se.getSession().setAttribute("currentUser",u);
        se.getSession().setAttribute("currentPerson",currentPerson);
        se.getSession().setAttribute("psp",currentPerson.getPsp());
        List<Person> userList = dbRec.getPspUserList(em,currentPerson.getPsp());
        List<LOS> losList = dPSP.getLOS(currentPerson.getPsp());
        List<Rate> rateList = dG.getRateList(em,Integer.parseInt(currentPerson.getPsp().getId().toString()));
        se.getSession().setAttribute("losList",losList);
        se.getSession().setAttribute("rateList",rateList);
        se.getSession().setAttribute("pspUserList",userList);
        PSP psp = dM.getPspById(em,4);
        se.getSession().setAttribute("psp",psp);
        dbAuth.assignUserRoles(se,u);
        List<TemplateGroup> templateGroupList = ddC.getTemplateGroups(em);
        List<TemplatePurpose> templatePurposeList = ddC.getTemplatePurposes(em);
        List<TaskFrequency> taskFrequencyList = dbCheck.getTaskFrequencies(em);
        String savePath = "C:\\Users\\kevinmurphy.SUPERIORSTATE\\IdeaProjects\\km_web_100\\src\\main\\webapp\\WEB-INF\\view\\weblink\\linkfiles\\";
        se.getSession().setAttribute("savePath",savePath);
        se.getSession().setAttribute("templateGroupList",templateGroupList);
        se.getSession().setAttribute("templatePurposeList",templatePurposeList);
        se.getSession().setAttribute("taskFrequencyList", taskFrequencyList);
        se.getSession().setAttribute("ticketSubCategories", dbTicket.getTicketSubCategoryList(em));
        se.getSession().setAttribute("ticketCategories",dbTicket.getTicketCategories(em));
        se.getSession().setAttribute("ticketReasonList", dbTicket.getTicketSubCats(em));
        se.getSession().setAttribute("contactMethods",dbTicket.getContactMethods(em));
        se.getSession().setAttribute("statusList", dbTicket.getActivityStatuses(em));
        se.getSession().setAttribute("reasonList",dbTicket.getReasons(em));
        se.getSession().setAttribute("setupModules", ddC.getSetupModuleList(em));
        se.getSession().setAttribute("insertLinkList",dbTicket.getInsertLinkList(em));
        se.getSession().setAttribute("onlyPast","N");
        se.getSession().setAttribute("fS","");
        se.getSession().setAttribute("fR","");
        se.getSession().setAttribute("fT","");
        se.getSession().setAttribute("fU","checked");
        se.getSession().setAttribute("renewalListView",1);
        se.getSession().setAttribute("hasCurrentRate", false);
        se.getSession().setAttribute("hasCurrentLos",false);
        se.getSession().setAttribute("hasCurrentModule",false);
        se.getSession().setAttribute("hasCurrentPriceItem",false);
        se.getSession().setAttribute("hasCurrentServiceItem",false);
        se.getSession().setAttribute("hasCurrentAgency",false);
        se.getSession().setAttribute("sortHow","DATE");
        se.getSession().setAttribute("vA","MINE");
        se.getSession().setAttribute("vR", "ON");
        se.getSession().setAttribute("vS","ON");
        se.getSession().setAttribute("vT","ON");
        se.getSession().setAttribute("onUs",0);
        se.getSession().setAttribute("followUp",0);
        se.getSession().setAttribute("rFlag","");
        se.getSession().setAttribute("vRn","1");
        se.getSession().setAttribute("vSt","3");
        se.getSession().setAttribute("vTk","5");
        se.getSession().setAttribute("qNwf","1");
        se.getSession().setAttribute("bpoUserList",getBpoUsers(em));
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
