package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.TimeCorrectionRequest;
import net.superiorstate.ams.model.general.TimeLog;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@WebServlet(name = "SubmitTimeCorrection", value = "/SubmitTimeCorrection")
public class SubmitTimeCorrection extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        submitCorrection(request);
        response.sendRedirect("ViewHome25");
    }

    private void submitCorrection(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Read form parameters
            String inLogIdStr = request.getParameter("inLogId");
            String outLogIdStr = request.getParameter("outLogId");
            String originalDateStr = request.getParameter("originalDate");
            String originalInTimeStr = request.getParameter("originalInTime");
            String originalOutTimeStr = request.getParameter("originalOutTime");
            String newInTimeStr = request.getParameter("newInTime");
            String newOutTimeStr = request.getParameter("newOutTime");
            String changeIn = request.getParameter("changeIn");     // "true" if checkbox checked
            String changeOut = request.getParameter("changeOut");   // "true" if checkbox checked
            String note = request.getParameter("correctionNote");

            // Look up the actual TimeLog entities
            TimeLog inLog = em.find(TimeLog.class, Long.parseLong(inLogIdStr));
            TimeLog outLog = null;
            if (outLogIdStr != null && !outLogIdStr.isEmpty()) {
                outLog = em.find(TimeLog.class, Long.parseLong(outLogIdStr));
            }

            // Build the request
            TimeCorrectionRequest tcr = new TimeCorrectionRequest();
            tcr.setRequestor(local.getCurrentPerson());
            tcr.setInLog(inLog);
            tcr.setOutLog(outLog);
            tcr.setOriginalDate(Date.valueOf(originalDateStr));
            tcr.setOriginalInTime(parseTime(originalInTimeStr));
            if (originalOutTimeStr != null && !originalOutTimeStr.isEmpty()) {
                tcr.setOriginalOutTime(parseTime(originalOutTimeStr));
            }

            // Only set requested times if the user checked the corresponding checkbox
            if ("true".equals(changeIn) && newInTimeStr != null && !newInTimeStr.isEmpty()) {
                tcr.setRequestedInTime(parseTime(newInTimeStr));
            }
            if ("true".equals(changeOut) && newOutTimeStr != null && !newOutTimeStr.isEmpty()) {
                tcr.setRequestedOutTime(parseTime(newOutTimeStr));
            }

            tcr.setRequestNote(note != null ? note.trim() : null);
            tcr.setDateRequested(new Timestamp(System.currentTimeMillis()));

            em.getTransaction().begin();
            em.persist(tcr);
            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }
    }

    /** Parses "HH:mm" from an HTML time input into java.sql.Time */
    private Time parseTime(String htmlTime) {
        if (htmlTime == null || htmlTime.isEmpty()) return null;
        LocalTime lt = LocalTime.parse(htmlTime, DateTimeFormatter.ofPattern("HH:mm"));
        return Time.valueOf(lt);
    }
}
