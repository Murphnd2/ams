package net.superiorstate.ams.controller;

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

@WebServlet(name = "SendAuto25", value = "/SendAuto25")
public class SendAuto25 extends HttpServlet {
    private String currentLabel;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        action(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        action(request,response);
    }

    private void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        // Get Automation Parameter
        Automation a = null;
        try{
            int autoId = Integer.parseInt(request.getParameter("aeId").toString().trim());
            a = dM.getAutomationById(em,autoId);
        } catch (Exception e2){return;}
        if(a==null)
            return;
        System.out.println("ID: "+ a.getId());
        // Prepare The Automation
        String remainingText = a.getContent();
        StringBuilder processedText = new StringBuilder();
        boolean shouldClose = remainingText.contains("<<close>>");
        remainingText = remainingText.replace("<<close>>",""); // Strip flag from text
        boolean addSignature = remainingText.contains("<<sig>>");
        remainingText = remainingText.replace("<<sig>>",""); // Strip flag from text
        boolean includeCc = false;

        // Locate and process all inputs required
        int count = 0;
        int iiFlagStart;
        int iiFlagEnd;
        List<String> inputType = new ArrayList<>();
        List<String> inputLabel = new ArrayList<>();
        while(remainingText.contains("<ii>")){
            count +=1;
            iiFlagStart = remainingText.indexOf("<ii>");
            iiFlagEnd = remainingText.indexOf("</ii>");
            currentLabel = remainingText.substring(iiFlagStart+4,iiFlagEnd);
            inputType.add(getLabelType(currentLabel));
            inputLabel.add(currentLabel);

            //Remove label detail and replace with label index #
            processedText.append(remainingText.substring(0,iiFlagStart)).append("<[{").append(count-1).append("}]>");
            if(remainingText.length()>iiFlagEnd+6) // Deal with situation where no text after last input
                remainingText = remainingText.substring(iiFlagEnd+5);
            else remainingText = "";
        }
        processedText.append(remainingText); //Add remaining text after last input

        em.close();

        // Set Session Variables
        request.getSession().setAttribute("a1auto",a);
        request.getSession().setAttribute("a1autoName",a.getAutomationName());
        request.getSession().setAttribute("a1includeCc",includeCc);
        request.getSession().setAttribute("a1shouldClose",shouldClose);
        request.getSession().setAttribute("a1addSignature",addSignature);
        request.getSession().setAttribute("a1inputLabels",inputLabel);
        request.getSession().setAttribute("a1inputTypes",inputType);
        request.getSession().setAttribute("a1content",processedText.toString());
        request.getSession().setAttribute("a1inputCount",count);
        // === FORCE CSRF TOKEN CREATION (so the JSP always has it) ===
        HttpSession session = request.getSession();
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
        }
        RequestDispatcher d = getServletContext().getNamedDispatcher("SendAutoFinal25");
        if(count>0)
            d = request.getRequestDispatcher("/WEB-INF/view/a/taskManager/autoInputScreen25.jsp");

        d.forward(request,response);
    }

    private String getLabelType(String cl){
        if(cl.contains("<l>")){
            currentLabel = cl.substring(3);
            return "LINK";
        }
        if(cl.contains("<cc>")){
            currentLabel = "CC (Separate with Semi-Colon)";
            return "CC";
        }
        return "INPUT";
    }
}
