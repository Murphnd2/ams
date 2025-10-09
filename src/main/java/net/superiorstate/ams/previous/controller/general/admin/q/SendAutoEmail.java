package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SendAutoEmail", value = "/SendAutoEmail")
public class SendAutoEmail extends HttpServlet {
    private String currentLabel;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // *********** REMOVED ACTION METHOD TO CONVERT TO UPDATED WEBSITE DESIGN ***********************************
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SendAuto25");
        dispatcher.forward(request,response);
    }

    private void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int automationId;
        request.getSession().setAttribute("useCcList",0);
        try{
            automationId = Integer.parseInt(request.getParameter("aeId").toString().trim());
        } catch (Exception e){
            automationId = -1;
        }
        if(automationId==-1)
            return;

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Automation a;

        try{
            a = dM.getAutomationById(em,automationId);
        } catch (Exception e){
            a = null;
        }
        if(a!=null){
            request.getSession().setAttribute("automationTitle",a.getAutomationName());
            assert a != null;
            prepareAutomation(request, response, a);
        }

        em.close();
    }

    private String addSignature(HttpServletRequest request, String text){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        String signature = "<br/>"+currentPerson.getFirstName()+" "+currentPerson.getLastName()+"<nl>Superior State Administrators, Inc.";

        String returnText = text.replace("<<sig>>","") + signature;
        em.close();
        return returnText;
    }

    private void prepareAutomation(HttpServletRequest request, HttpServletResponse response,Automation a) throws ServletException, IOException {
        String remainingText = a.getContent();
        int shouldClose = 0;
        int getWebLinkAsTask = 0;
        if(remainingText.contains("<<close>>")){
            remainingText = remainingText.replace("<<close>>","");
            shouldClose = 1;
        }
        if(remainingText.contains("<<webLinkTask>>")){
            remainingText = "X<ii>Document Name</ii>XX<ii><l>Document Link</ii>X";
            getWebLinkAsTask = 1;
        }
        if(remainingText.contains("<<sig>>")){
            String sendText = remainingText;
            remainingText = addSignature(request,sendText);
        }
        String shouldCloseIt = a.getId() + "-" + shouldClose;
        request.getSession().setAttribute("shouldClose",shouldCloseIt);
        request.getSession().setAttribute("webLinkTask",getWebLinkAsTask);
        StringBuilder processedText = new StringBuilder();
        int numInputs = 0;
        List<String> inputType = new ArrayList<>();
        List<String> inputLabels = new ArrayList<>();
        while(remainingText.contains("<ii>")){
            int count = numInputs + 1;

            System.out.println("*** PASS #" + count + " *******************************************");
            int locStart = remainingText.indexOf("<ii>");
            int locEnd = remainingText.indexOf("</ii>");
            currentLabel = remainingText.substring(locStart+4,locEnd);
            System.out.println("Label Content: "+currentLabel);

            String labelType = getLabelType(request,currentLabel);
            inputType.add(labelType);
            System.out.println("Label Type:"+labelType);

            inputLabels.add(currentLabel);
            System.out.println("Current Label POST:"+currentLabel);

            processedText.append(remainingText, 0, locStart);
            processedText.append("<[{").append(numInputs).append("}]>");
            System.out.println("CURRENT PROCESSED -----------------------");
            System.out.println(processedText.toString());

            String holdText = remainingText;
            int rtLength = remainingText.length();
            int locCloseBracket = remainingText.indexOf("</ii>");
            if(rtLength <= locCloseBracket+6)
                remainingText = "";
            else
                remainingText = holdText.substring(locCloseBracket + 5);

            System.out.println("REMAINING TEXT--------------------");
            System.out.println(remainingText);
            numInputs +=1;
        }
        processedText.append(remainingText);

        System.out.println("FINAL PROCESSED ------------------------------");
        System.out.println(processedText.toString());
        request.getSession().setAttribute("aNumInputs", numInputs);
        request.getSession().setAttribute("aInputLabels", inputLabels);
        request.getSession().setAttribute("aInputTypes", inputType);
        request.getSession().setAttribute("aContent", processedText.toString());

        RequestDispatcher dispatcher;
        if(numInputs == 0){
            dispatcher = getServletContext().getNamedDispatcher("SendAutomationEmailFinal");
        } else {
            dispatcher = request.getRequestDispatcher("/WEB-INF/view/autoInputScreen.jsp");
        }
        dispatcher.forward(request,response);
    }

    private String getLabelType(HttpServletRequest request, String curLabel){
        String labelType = "INPUT";
        if(curLabel.contains("<l>")){
            currentLabel = curLabel.substring(3);
            labelType = "LINK";
        } else if(curLabel.contains("<cc>")){
            request.getSession().setAttribute("useCcList",1);
            currentLabel="CC (Separate with Semi-Colon)";
            labelType = "CC";
        }
        return labelType;
    }




}
