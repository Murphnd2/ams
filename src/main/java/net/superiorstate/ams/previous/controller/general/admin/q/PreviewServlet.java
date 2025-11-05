package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Automation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "PreviewServlet", value = "/PreviewServlet")
public class PreviewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher;
        if(processServlet(request)==-1)
            dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        else if(processServlet(request)==0)
            dispatcher = getServletContext().getNamedDispatcher("ProcessAutomationContent");
        else
            dispatcher = request.getRequestDispatcher("/WEB-INF/view/automationPreview.jsp");
        dispatcher.forward(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher;
        if(processServlet(request)==-1)
            dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        else if(processServlet(request)==0)
            dispatcher = getServletContext().getNamedDispatcher("ProcessAutomationContent");
        else
            dispatcher = request.getRequestDispatcher("/WEB-INF/view/automationPreview.jsp");
        dispatcher.forward(request,response);
    }

    private int processServlet(HttpServletRequest request){
        if(getAutomationId(request)==-1)
            return -1;
        if(getAutomation(request,getAutomationId(request))==null)
            return -1;
        return setInputsAndContent(request);

    }

    private int setInputsAndContent(HttpServletRequest request){
        Automation a = getAutomation(request,getAutomationId(request));
        String remainingText = a.getContent();
        StringBuilder processedText = new StringBuilder();
        int numberOfInputs = 0;
        List<String> inputLabelList = new ArrayList<>();
        List<String> inputLabelTypeList = new ArrayList<>();
        while(remainingText.contains("<ii>")){
            int locStart = remainingText.indexOf("<ii>");
            int locEnd = remainingText.indexOf("</ii>");
            String labelContent = remainingText.substring(locStart+4,locEnd);
            String labelName = labelContent;
            String labelType = "INPUT";
            if(labelContent.substring(0,3).equals("<l>")) {
                labelName = labelContent.substring(3);
                labelType = "LINK";
            }
            inputLabelList.add(labelName);
            inputLabelTypeList.add(labelType);
            processedText.append(remainingText,0,locStart);
            processedText.append("[{");
            processedText.append(numberOfInputs);
            processedText.append("}]");
            if(remainingText.length()>locEnd+5)
                remainingText = remainingText.substring(locEnd+5);
            else
                remainingText = "";
            numberOfInputs +=1;
        }
        processedText.append(remainingText);
        request.getSession().setAttribute("aAutomation",a);
        request.getSession().setAttribute("aNumInputs", numberOfInputs);
        request.getSession().setAttribute("aInputLabels", inputLabelList);
        request.getSession().setAttribute("aInputTypes", inputLabelTypeList);
        request.getSession().setAttribute("aRawContent", processedText.toString());
        request.getSession().setAttribute("aPreviewView",1);
        if(numberOfInputs>0)
            request.getSession().setAttribute("aPreviewView",2);
        return numberOfInputs;
    }

    private Automation getAutomation(HttpServletRequest request, int id){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Automation a;
        try{
            a = dM.getAutomationById(em,id);
        } catch (Exception e){
            a = null;
        }
        em.close();
        return a;
    }

    private int getAutomationId(HttpServletRequest request){
        int automationId;
        try{
            automationId = Integer.parseInt(request.getParameter("aeId").toString().trim());
        } catch (Exception e){
            automationId = -1;
        }
        return automationId;
    }
}
