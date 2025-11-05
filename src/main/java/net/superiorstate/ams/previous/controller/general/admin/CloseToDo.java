package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "CloseToDo", value = "/CloseToDo")
public class CloseToDo extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request,response);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request,response);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String btnString = request.getParameter("btnToDo");
        request.getSession().setAttribute("sumTran","0");
        char firstLetter = btnString.charAt(0);
        System.out.println("Button String: " + btnString);
        if(firstLetter == 'L'){
            Long id1 = Long.parseLong(btnString.substring(2));
            em.getTransaction().begin();
            WebLink link = new WebLink();
            link.setLinkPath(request.getParameter("linkPath"));
            link.setLinkType(ddC.getLinkTypeById(em,2));
            link.setPlainText(request.getParameter("linkName"));
            em.persist(link);
            em.getTransaction().commit();
            System.out.println("Weblink ID: " + link.getId());

            em.getTransaction().begin();
            ToDo toDo = dM.getToDoById(em,id1);
            System.out.println("ToDo ID: " + toDo.getId());
            Task task = dM.getTaskById(em,toDo.getTask().getId());
            System.out.println("Task ID: " + task.getId());
            task.addWebLink(link);
            em.persist(task);
            em.persist(link);
            em.getTransaction().commit();

            CheckList c = (CheckList) request.getSession().getAttribute("currentChecklist");
            List<ToDo> toDoList = ViewSelectedChecklist.getToDoListByChecklistId(em,c.getId());
            request.getSession().setAttribute("currentToDoList",toDoList);

        } else if (firstLetter == 'Z'){
            String servletName = request.getParameter("servName");
            String automationText = request.getParameter("autoText");
            String content = request.getParameter("content");
            em.getTransaction().begin();
            ToDo toDo = dM.getToDoById(em,Long.parseLong(btnString.substring(2)));
            Task task = dM.getTaskById(em,toDo.getTask().getId());
            if(servletName!=null && !servletName.equals("") && automationText!=null && !automationText.equals("")){
                task.setHasAutomation(true);
                task.setAutomationText(automationText);
                task.setServletName(servletName);
                em.persist(task);
            }
            em.getTransaction().commit();
            em.getTransaction().begin();
            Automation a = new Automation();
            a.setId(task.getId().intValue());
            a.setContent(content);
            a.setAutomationName(automationText);
            em.persist(a);
            em.getTransaction().commit();
            CheckList c = (CheckList) request.getSession().getAttribute("currentChecklist");
            List<ToDo> toDoList = ViewSelectedChecklist.getToDoListByChecklistId(em,c.getId());
            request.getSession().setAttribute("currentToDoList",toDoList);
        }
        else if (firstLetter=='A'){
            String servletName = request.getParameter("servName");
            String automationText = request.getParameter("autoText");
            em.getTransaction().begin();
            ToDo toDo = dM.getToDoById(em,Long.parseLong(btnString.substring(2)));
            System.out.println("ToDo ID: " + toDo.getId());
            Task task = dM.getTaskById(em,toDo.getTask().getId());
            System.out.println("Task ID: " + task.getId());
            if(servletName!=null && !servletName.equals("") && automationText!=null && !automationText.equals("")){
                task.setHasAutomation(true);
                task.setAutomationText(automationText);
                task.setServletName(servletName);
                em.persist(task);
            }
            em.getTransaction().commit();

            CheckList c = (CheckList) request.getSession().getAttribute("currentChecklist");
            List<ToDo> toDoList = ViewSelectedChecklist.getToDoListByChecklistId(em,c.getId());
            request.getSession().setAttribute("currentToDoList",toDoList);
        } else if (firstLetter=='S'){
            System.out.println("SUMMIT: FOUND LETTER S");
            String uName = request.getParameter("contactUserName");
            String uPass = request.getParameter("contactPassword");
            request.getSession().setAttribute("cUsername",uName);
            request.getSession().setAttribute("cPassword",uPass);
            request.getSession().setAttribute("autoTask","sumTran");
            em.close();
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SendAuto");
            dispatcher.forward(request,response);
        } else if (firstLetter=='P' || firstLetter=='B'){
            String pAgentName = request.getParameter("agentName");
            String pAgentEmail = request.getParameter("agentEmail");
            String pContactName = request.getParameter("prosContactName");
            String pContactEmail = request.getParameter("prosContactEmail");
            String pCompany = request.getParameter("prosCompany");
            String pProposal = request.getParameter("prosProposal");
            request.getSession().setAttribute("autoTask","sendProp");
            request.getSession().setAttribute("pToWho",firstLetter);
            request.getSession().setAttribute("pAgentName",pAgentName);
            request.getSession().setAttribute("pContactName",pContactName);
            request.getSession().setAttribute("pAgentEmail",pAgentEmail);
            request.getSession().setAttribute("pContactEmail",pContactEmail);
            request.getSession().setAttribute("pCompany",pCompany);
            request.getSession().setAttribute("pProposal",pProposal);
            em.close();
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SendQuote1");
            dispatcher.forward(request,response);
        }
        else{
            System.out.println("Skipped It");
            Long id = Long.parseLong(request.getParameter("btnToDo"));
            em.getTransaction().begin();
            ToDo toDo = dM.getToDoById(em,id);
            toDo.setComplete(true);
            toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
            em.persist(toDo);
            em.getTransaction().commit();
            request.getSession().setAttribute("currentToDoList",ViewSelectedChecklist.getToDoListByChecklistId(em,toDo.getCheckList().getId()));
        }

        if(em.getTransaction().isActive())
            em.close();
    }

}
