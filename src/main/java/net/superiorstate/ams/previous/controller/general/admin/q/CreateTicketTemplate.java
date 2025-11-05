package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplateGroup;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateTicketTemplate", value = "/CreateTicketTemplate")
public class CreateTicketTemplate extends HttpServlet {
    private RequestDispatcher dispatcher;

    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processSubmittal(request);
        getDispatcher().forward(request,response);

    }

    private void processSubmittal(HttpServletRequest request){
        String buttonClicked = request.getParameter("submitButton");
        List<String> oldList = (List<String>) request.getSession().getAttribute("taskListBuilder");
        List<String> taskList = new ArrayList<>();
        for(int n = 0; n < oldList.size(); n++){
            String paramName = "taskDesc" + n;
            String iterationDescription = request.getParameter(paramName);
            taskList.add(iterationDescription);
        }
        String buttonMethod = buttonClicked.substring(0,1);
        int buttonIndex=-1;
        List<String> newList = new ArrayList<>();
        int categoryId = Integer.parseInt(request.getParameter("categoryList"));
        switch (buttonMethod) {
            case "D":
                buttonIndex = getButtonIndex(buttonClicked);
                for(int i = 0; i < taskList.size(); i++ ){
                    if(i!=buttonIndex)
                        newList.add(taskList.get(i));
                }
                request.getSession().setAttribute("taskListBuilder",newList);
                request.getSession().setAttribute("taskListStat",1);
                request.getSession().setAttribute("templateName",request.getParameter("templateName"));
                request.getSession().setAttribute("templateCategoryId",categoryId);
                setDispatcher(request.getRequestDispatcher("/WEB-INF/view/activity/template/createTicketTemplate.jsp"));
                break;
            case "A":
                buttonIndex = getButtonIndex(buttonClicked);
                for(int j = 0; j < taskList.size(); j++){
                    newList.add(taskList.get(j));
                    if(j==buttonIndex)
                        newList.add("");
                }
                request.getSession().setAttribute("taskListBuilder",newList);
                request.getSession().setAttribute("taskListStat",1);
                request.getSession().setAttribute("templateName",request.getParameter("templateName"));
                request.getSession().setAttribute("templateCategoryId",categoryId);
                setDispatcher(request.getRequestDispatcher("/WEB-INF/view/activity/template/createTicketTemplate.jsp"));
                break;
            default:
                createTemplate(request,categoryId, taskList);
                setDispatcher(getServletContext().getNamedDispatcher("SequenceHome"));
                break;
        }
    }

    private int getButtonIndex(String buttonValue){
        return Integer.parseInt(buttonValue.substring(2));
    }

    private void createTemplate(HttpServletRequest request, int catId, List<String> taskList){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        TicketCategory tc = dM.getTicketCategoryById(em, (long) catId);
        String templateName = request.getParameter("templateName");
        TemplateGroup tg = dM.getTemplateGroupById(em,3);

        //------TEMPLATE PURPOSE---------------
        em.getTransaction().begin();
        TemplatePurpose tp = new TemplatePurpose();
        tp.setTemplateGroup(tg);
        tp.setSortOrder(100);
        assert tc != null;
        tp.setDescription(tc.getShortText()+": "+templateName);
        em.persist(tp);
        em.getTransaction().commit();

        //-----REQUIRED TASK LIST---------------
        em.getTransaction().begin();
        RequiredTaskList rtl = new RequiredTaskList();
        rtl.setTemplatePurpose(tp);
        rtl.setPsp(dM.getPspById(em,4));
        rtl.setDescription(tc.getShortText()+": "+templateName);
        rtl.setInActive(false);
        em.persist(rtl);
        em.getTransaction().commit();

        //------

        //-----ESTABLISH TASK LIST------------------
        for(int i = 0; i < taskList.size(); i++){
            int sortOrder = (i+1)*10;
            //--CREATE THE TASK--------
            em.getTransaction().begin();
            Task t = new Task();
            t.setPsp(dM.getPspById(em,4));
            t.setReUsable(false);
            t.setDescription(taskList.get(i));
            em.persist(t);
            em.getTransaction().commit();

            //--ASSIGN IT TO THE LIST-------
            em.getTransaction().begin();
            TaskSequenceTable tst = new TaskSequenceTable();
            tst.setTask(t);
            tst.setSortOrder(sortOrder);
            tst.setTaskSequence(rtl);
            em.persist(tst);
            em.getTransaction().commit();
        }

        //------TICKET SUBCATEGORY TIED TO REQUIRED LIST-----------------
        em.getTransaction().begin();
        TicketSubCategory tsc = new TicketSubCategory();
        tsc.setActive(true);
        tsc.setTicketCategory(tc);
        tsc.setTemplatePurpose(tp);
        tsc.setDescription(templateName);
        em.persist(tsc);
        em.getTransaction().commit();

        request.getSession().setAttribute("templatePurposeList", ddC.getTemplatePurposes(em));
        request.getSession().setAttribute("ticketSubCategories", dbTicket.getTicketSubCategoryList(em));
        em.close();
    }
}
