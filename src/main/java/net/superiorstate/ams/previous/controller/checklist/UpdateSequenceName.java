package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;

import java.io.IOException;

@WebServlet(name = "UpdateSequenceName", value = "/UpdateSequenceName")
public class UpdateSequenceName extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        TaskSequence taskSequence = (TaskSequence) request.getSession().getAttribute("currentTaskSequence");
        String sequenceName = request.getParameter("sequenceName");
        int templatePurposeId = Integer.parseInt(request.getParameter("templatePurposeList"));

        TaskSequence currentTaskSequence = ddC.getTaskSequenceById(em, taskSequence.getId());
        boolean descriptionChanged = !currentTaskSequence.getDescription().equals(sequenceName);
        boolean purposeChanged = false; //FIXME: changed this
        boolean purposeProtected = (ddC.getTemplateGroupByPurposeId(em,templatePurposeId).getId()==1 || ddC.getTemplateGroupByPurposeId(em,templatePurposeId).getId()==2);
        boolean purposeAlreadyExists = ddC.sequenceLoggedForPurpose(em,templatePurposeId);
        int groupId = ddC.getTemplateGroupByPurposeId(em,templatePurposeId).getId();
        ddC.flipFilterFlags(request,groupId);


        if(descriptionChanged && !purposeChanged){
            currentTaskSequence.setDescription(sequenceName);
        } else {
            em.close();
            return;
        }

        em.getTransaction().begin();
        em.persist(currentTaskSequence);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentTaskSequence",currentTaskSequence);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SequenceDetailView");
        dispatcher.forward(request,response);
    }
}
