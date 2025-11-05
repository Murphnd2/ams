package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ProcessAutomationContent", value = "/ProcessAutomationContent")
public class ProcessAutomationContent extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void getPreviewDetail(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        getAndSetEmailSubject(request);
        getAndSetEmailRecipientList(request, em);
        getAndSetMessageBody(request, em);
        em.close();
    }

    private void getAndSetMessageBody(HttpServletRequest request, EntityManager em){
        int numberOfInputs = (int) request.getSession().getAttribute("aNumInputs");
        String rawContent = request.getSession().getAttribute("aRawContent").toString();
        String contentWithAnyInputs = rawContent;
        if(numberOfInputs>0)
            contentWithAnyInputs = fillInputs(request, rawContent);
        String formattedText;
        formattedText = formatText(contentWithAnyInputs);
        String finalizedText;
        finalizedText = finalizeText(formattedText, em);
        request.getSession().setAttribute("aEmailBody", finalizedText);
    }

    private String finalizeText(String textToFinalize, EntityManager em){
        String unprocessedText = textToFinalize;
        StringBuilder processedText  = new StringBuilder();
        while(unprocessedText.contains("<rf>") && unprocessedText.contains("</rf>")){
            String holder = unprocessedText;
            int locStart = unprocessedText.indexOf("<rf>");
            int locEnd = unprocessedText.indexOf("</rf>");
            processedText.append(unprocessedText, 0, locStart);
            String idString = unprocessedText.substring(locStart+4,locEnd);
            int id = Integer.parseInt(idString);
            String linkString = getLinkString(em, id);
            processedText.append(linkString);
            unprocessedText = holder.substring(locEnd+5);
        }
        processedText.append(unprocessedText);
        return processedText.toString();
    }

    private String getLinkString(EntityManager em, int refId){
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.id = :id");
        q.setParameter("id",refId);
        WebLink webLink;
        try{
            webLink = (WebLink) q.getSingleResult();
        } catch (NoResultException e){
            webLink = null;
        }
        if(webLink == null)
            return "";
        String linkName = webLink.getPlainText();
        String linkPath = webLink.getLinkPath();
        return "<a target=\"_blank\" href=\"" + linkPath + "\">"+linkName+"</a>";
    }

    private String formatText(String textToFormat){
        String unformattedText = textToFormat;
        StringBuilder formattedText = new StringBuilder("<p>");
        while (unformattedText.contains("<br/>")) {
            int breakLoc = unformattedText.indexOf("<br/>");
            formattedText.append(unformattedText.substring(0,breakLoc));
            formattedText.append("</p><p>");
            String holdText = unformattedText;
            unformattedText = holdText.substring(breakLoc+5);
        }
        formattedText.append(unformattedText);
        formattedText.append("</p>");
        return formattedText.toString();
    }

    private String fillInputs(HttpServletRequest request, String remainingText){
        StringBuilder processedText = new StringBuilder();
        int j = 0;
        String iCode = getInputCode(j);
        while(remainingText.contains(iCode)){
            int codeLoc = remainingText.indexOf(iCode);
            processedText.append(remainingText, 0, codeLoc);
            String pString = getParameterString(request,j);
            processedText.append(pString);
            String holdText = remainingText;
            int codeLength = getInputCode(j).length();
            remainingText = holdText.substring(codeLoc+codeLength);
            j+=1;
            iCode = getInputCode(j);
        }
        processedText.append(remainingText);
        return processedText.toString();
    }

    private String getInputCode(int j){
        return "[{" + j + "}]";
    }

    private String getParameterString(HttpServletRequest request,int i){
        List<String> inputTypes =(List<String>) request.getSession().getAttribute("aInputTypes");
        boolean isLink = false;
        if(inputTypes.get(i).equals("LINK"))
            isLink = true;
        String parameterName = "aInput-" + i;
        String userInput = request.getParameter(parameterName);
        if(isLink)
            return wrapInput(userInput);
        return " " + userInput + " ";
    }

    private String wrapInput(String input){
        return " <a target=\"_blank\" href=\"" + input + "\">" + input + "</a> ";
    }

    private void getAndSetEmailRecipientList(HttpServletRequest request, EntityManager em){
        Activity activity = (Activity) request.getSession().getAttribute("currentActivity");
        List<Person> recipientList = StdAuto.getRecipientList(request,em,activity);
        request.getSession().setAttribute("aRecipientList",recipientList);
    }

    private void getAndSetEmailSubject(HttpServletRequest request){
        String rawContent = request.getSession().getAttribute("aRawContent").toString();
        Automation a = (Automation) request.getSession().getAttribute("aAutomation");
        String emailSubject = a.getAutomationName();
        if(rawContent.contains("<sbj>"))
            emailSubject = emailSubject.substring(emailSubject.indexOf("<sbj>")+5,emailSubject.indexOf("</sbj>"));
        request.getSession().setAttribute("aEmailSubject",emailSubject);
    }
}
