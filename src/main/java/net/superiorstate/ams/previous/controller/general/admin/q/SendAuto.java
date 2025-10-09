package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.ActivityStatus;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.temp.Automation2;

import jakarta.mail.MessagingException;
import java.io.IOException;

@WebServlet(name = "SendAuto", value = "/SendAuto")
public class SendAuto extends HttpServlet {
    private String whereTo = "GoAdminHome";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            startMethod(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            startMethod(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(whereTo);
        dispatcher.forward(request,response);
    }
    private Automation2 getAutomation(HttpServletRequest request, Long longTaskId, Activity activity){
        Automation2 a = new Automation2();
        a.setTaskId(longTaskId);
        a.setSubject("");
        a.setMessage("");
        a.setNonStandard(false);  //Allow for additional actions ** MUST SET IN METHOD BELOW ***
        a.setReminder(false);     //Include Reminder Text
        a.setShouldClose(true);   //Close the Task after completion
        a.setStatusChangeFlag(1); //Waiting on Them
        int taskId = longTaskId.intValue();
        switch (taskId){
            case 8065:
                Renewal r = (Renewal) activity;
                a.setSubject("Rates Needed for COBRA Renewal");
                a.setMessage(Q.COBRA_NEED_RATES_GENERAL + Q.insertCobraQuestionnaireGoogle(r.getId(),r.getFullName()));
                break;
            case 3616:
                a.setSubject("Eligibility Testing Follow Up");
                a.setMessage(Q.TEST_ELIGIBILITY + Q.insert125Test(Math.toIntExact((Long) activity.getId()),activity.getFullName()));
                a.setReminder(true);
                a.setShouldClose(false);
                break;
            case 3803:
                a.setSubject("Participation Testing");
                a.setMessage(Q.TEST_PARTICIPATION + Q.insert105TestGoogle(activity.getFullName()));
                break;
            case 3805:
                a.setSubject("Participation Testing Follow Up");
                a.setMessage(Q.TEST_PARTICIPATION + Q.insert105TestGoogle(activity.getFullName()));
                a.setReminder(true);
                a.setShouldClose(false);
                break;
            case 5097,3635:
                a.setSubject("Plan Documents for your Plan");
                a.setMessage(Q.PLAN_DOCUMENTATION);
                a.setShouldClose(false);
                a.setNonStandard(true);
                break;
            case 11495, 552:
                a.setSubject("Enhance Your Benefits with an FSA");
                a.setMessage(Q.FSA_ADVERT_2023);
                a.setShouldClose(false);
                break;
            case 4976:
                Renewal renewal = (Renewal) activity;
                int planTypeId = renewal.getRenewalItemList().get(0).getBenefit().getPlanType().getPlanTypeId();
                int type = switch (planTypeId) {
                    case 1, 2, 3, 5, 6, 7, 8, 1001, 1002, 1003, 1004 -> 1;
                    default -> 0;
                };
                a.setSubject("Exciting Changes to Our Benefits Portal");
                a.setMessage(getSummitText(request,type));
                System.out.println("SUMMIT: IN CASE 4976");
                //request.getSession().setAttribute("auto");
                break;
            case 2465:
                a.setSubject("COBRA Information Received / Renewal Complete");
                a.setMessage(Q.COBRA_COMPLETE);
                break;
            case 19002:
                a.setSubject("Section 125 Plan Renewal");
                a.setMessage(Q.WELCOME_RENEWAL_POP_1);
                a.setShouldClose(true);
            default:
                break;
        }

        System.out.println("SHOULD CLOSE: " + a.shouldClose());
        return a;
    }


    private String getSummitText(HttpServletRequest request, int type){
        String summitText="";
        String userName = request.getSession().getAttribute("cUsername").toString();
        String passWord = request.getSession().getAttribute("cPassword").toString();
        summitText = Q.getSummitTransition(userName,passWord,type);
        return summitText;
    }

    private void nonStandardAutomation(HttpServletRequest request, EntityManager em, Activity activity, Automation2 automation2, Person user) throws MessagingException {
        int taskId = automation2.getTaskId().intValue();
        whereTo = "ResetEmailView";
        request.setAttribute("opt","doc");
        request.getSession().setAttribute("tempSubject", automation2.getSubject());
        request.getSession().setAttribute("tempMessage", automation2.getMessage());
        switch (taskId){
            case 1001:
                standardAutomation(request,em,activity, automation2,user);
                break;
            default:
                break;
        }
    }
    private ActivityStatus getActivityStatus(EntityManager em, Automation2 a){
        return dM.getActivityStatusById(em,a.getStatusChangeFlag());
    }

    private void startMethod(HttpServletRequest request) throws MessagingException {
        request.setAttribute("opt","");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        sendAutomation(request,em);
        em.close();
    }
    private void sendAutomation(HttpServletRequest request, EntityManager em) throws MessagingException {
        String taskIdString = request.getParameter("tId");
        if(request.getSession().getAttribute("autoTask")!=null){
            String flag = request.getSession().getAttribute("autoTask").toString();
            if(flag.equals("sumTran")){
                taskIdString = "4976";
                System.out.println("SUMMIT: TASKID STRING IS SET");
            }
        }

        Long taskId = Long.parseLong(taskIdString);
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        Automation2 automation2 = getAutomation(request,taskId,a);

        if(automation2.isNonStandard())
            nonStandardAutomation(request,em,a, automation2,user);
        else if(automation2.getSubject().equals("") && automation2.getMessage().equals(""))
            return;
        else
            standardAutomation(request,em,a, automation2,user);
    }

    private void standardAutomation(HttpServletRequest request, EntityManager em, Activity activity, Automation2 a, Person user) throws MessagingException {
        String subject = a.getSubject();
        String message = getMessage(a,user);

        System.out.println("AUTOMATION SETTING: " + a.getStatusChangeFlag());
        ActivityStatus activityStatus = getActivityStatus(em,a);
        System.out.println("ACTIVITY STATUS: " + activityStatus.getDescription());
        System.out.println("ACTIVITY STATUS: " + activityStatus.getId());
        Email email = getEmail(request,em,activity,subject,message,activityStatus.getId(),user);
        dbEmail.sendEmail(email,em);
        closeTask(em,activity,a,user);
    }


    private Email getEmail(HttpServletRequest request, EntityManager em, Activity a, String subject, String message, int statusId, Person user){
        Email email = StdAuto.createEmail(request,em,a,subject,message,statusId,user);
        return dM.getEmailById(em,email.getId());
    }

    private void closeTask(EntityManager em, Activity a, Automation2 automation2, Person user){

        System.out.println("CLOSE TASK: " + automation2.shouldClose());
        if(automation2.shouldClose()){
            System.out.println("Tried to close " + automation2.getTaskId());
            StdAuto.closeThisTask(em,a, automation2.getTaskId() ,user);
        }

    }

    private String getMessage(Automation2 a, Person user){
        String message = a.getMessage();
        int indexEnd = message.indexOf("</b></p>");
        String header = message.substring(0,indexEnd+8);
        String footer = message.substring(indexEnd+8);
        String reminder="";
        if(a.isReminder())
            reminder = Q.REMINDER_TEXT;
        return header + reminder + footer + StdAuto.userSignature(user);
    }
}
