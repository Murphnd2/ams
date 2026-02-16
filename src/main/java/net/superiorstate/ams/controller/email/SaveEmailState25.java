package net.superiorstate.ams.controller.email;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;

import java.io.IOException;

@WebServlet(name = "SaveEmailState25", value = "/SaveEmailState25")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    //1 MB
        maxFileSize = 1024 * 1024 * 10,         //10 MB
        maxRequestSize = 1024 * 1024 * 100      //100 MB
)
public class SaveEmailState25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        saveState(request,response);
    }

    private void saveState(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        String subject = request.getParameter("eSubject");
        String body = request.getParameter("eBody");
        String emailString = request.getParameter("emailName");
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");

        local.getCurrentEmail().setSubject(subject);
        local.getCurrentEmail().setBody(body);
        local.getCurrentEmail().setEmailToAdd(emailString);
        local.getCurrentEmail().setFirstName(firstName);
        local.getCurrentEmail().setLastName(lastName);
        request.getSession().setAttribute("local",local);

        String submit = request.getParameter("action");
        String action = submit.substring(0,2);
        request.getSession().setAttribute("emailAction",submit);

        if("SE".equals(action) && hasAllRequiredFields(local)){
            System.out.println("GOT HERE: Send Email Servlet Forwarding");
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SendEmail25");
            dispatcher.forward(request,response);
        } else if("DR".equals(action)){
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RemoveRecipient25");
            dispatcher.forward(request,response);
        } else if("DA".equals(action)){
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RemoveAttachment25");
            dispatcher.forward(request,response);
        } else if ("AR".equals(action)){
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("AddRecipient25");
            dispatcher.forward(request,response);
        } else if("AA".equals(action)){
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("AddAttachment25");
            dispatcher.forward(request,response);
        } else {
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
            dispatcher.forward(request,response);
        }
    }

    private boolean hasAllRequiredFields(AmsDataLocal local){
        if(local.getCurrentEmail().getRecipientList()==null || local.getCurrentEmail().getRecipientList().size()==0) {
            System.out.println("FAILED RECIPIENTS");
            return false;
        }
        if(local.getCurrentEmail().getSubject()==null || local.getCurrentEmail().getSubject().equals("")) {
            System.out.println("FAILED SUBJECT");
            return false;
        }
        if(local.getCurrentEmail().getBody()==null || local.getCurrentEmail().getBody().equals("")) {
            System.out.println("FAILED BODY");
            return false;
        }
        return true;
    }
}
