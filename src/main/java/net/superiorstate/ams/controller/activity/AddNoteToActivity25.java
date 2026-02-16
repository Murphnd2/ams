package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.Note;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "AddNoteToActivity25", value = "/AddNoteToActivity25")
public class AddNoteToActivity25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToActivityView(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToActivityView(request, response);
    }

    private void forwardToActivityView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void handleRequest(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Note newNote = buildNoteFromRequest(request, em, local);
            if (newNote == null) return;

            persistNote(em, newNote, local);
            updateActivityWithNote(em, local.getCurrentActivity().getActivity().getId(), newNote);

            List<Note> newNoteList = new ArrayList<>(local.getCurrentActivity().getNotes());
            newNoteList.add(0, newNote);
            local.getCurrentActivity().setNotes(newNoteList);

            local.getCurrentActivity().setReFilterOnExit(newNote.getReasonCreated().isOutbound() || newNote.getStatus().getId() != 2);
            if(local.getCurrentActivity().isReFilterOnExit())
                handleAnyActivityStatusUpdates(global, local, newNote);

            request.getSession().setAttribute("local", local);
            request.getServletContext().setAttribute("global",global);

        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private void handleAnyActivityStatusUpdates(AmsDataGlobal global, AmsDataLocal local, Note n){
        List<Activity25u> openActivities = new ArrayList<>(global.getActivitiesAllOpen());
        Optional<Activity25u> au = openActivities.stream().filter(obj -> Objects.equals(obj.getActivity().getId(),local.getCurrentActivity().getActivity().getId())).findFirst();
        if(au.isPresent() && local.getCurrentActivity().isReFilterOnExit()){
            Activity25u a25u = au.get();

            if(n.getReasonCreated().isOutbound())
                a25u.setDaysSinceContact(0);
            if(n.getStatus().getId()==1)
                a25u.setWaitingOnUs(false);
            else if(n.getStatus().getId()==3)
                a25u.setWaitingOnUs(true);

            List<Activity25u> listToUpdate = openActivities.stream().filter(a-> !Objects.equals(a.getActivity().getId(), a25u.getActivity().getId())).collect(Collectors.toList());
            listToUpdate.add(a25u);

            global.setActivitiesAllOpen(listToUpdate);
            local.setActivitiesAllOpen(global.getActivitiesAllOpen());
        }
    }

    private Note buildNoteFromRequest(HttpServletRequest request, EntityManager em, AmsDataLocal local) {
        try {
            String noteText = request.getParameter("noteText");
            int statusId = Integer.parseInt(request.getParameter("noteStatus"));
            int reasonId = Integer.parseInt(request.getParameter("reasonList"));

            Note note = new Note();
            note.setDetail(noteText);
            note.setActivity(local.getCurrentActivity().getActivity());
            note.setStatus(dM.getActivityStatusById(em, statusId));
            note.setReasonCreated(dM.getReasonById(em, reasonId));
            note.setDateGenerated(Date.valueOf(LocalDate.now()));
            note.setCreatedBy(local.getCurrentPerson());
            return note;
        } catch (Exception e) {
            return null;
        }
    }

    private void persistNote(EntityManager em, Note note, AmsDataLocal local) {
        em.getTransaction().begin();
        em.persist(note);
        em.getTransaction().commit();
    }

    private void updateActivityWithNote(EntityManager em, Long activityId, Note note) {
        em.getTransaction().begin();
        Activity activity = dM.getActivityById(em, activityId);
        if (activity != null && activity.getNoteList() != null) {
            activity.getNoteList().add(note);
            em.persist(activity);
        }
        em.getTransaction().commit();
    }
}

