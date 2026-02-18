package net.superiorstate.ams.controller.email;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.data.util.AutoSafe;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;

@WebServlet("/PreviewAutomation")
public class PreviewAutomation extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get automation the exact same way every other servlet does
        EntityManagerFactory emf = (EntityManagerFactory)
                getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        int autoId;
        try {
            autoId = Integer.parseInt(request.getParameter("aeId"));
        } catch (Exception e) {
            em.close();
            response.sendError(400, "Missing aeId");
            return;
        }

        Automation a = EntityLookup.getAutomationById(em, autoId);
        em.close();

        if (a == null) {
            response.sendError(404, "Automation not found");
            return;
        }

        // Dummy data so the preview always shows something useful
        for (int i = 0; i < 20; i++) {
            request.setAttribute("dummyInput" + i, "Test Value " + (i + 1));
        }

        String content = a.getContent();

        // Apply the same replacements your real flow does
        content = content.replace("<<sig>>", "<p>John Doe<br/>Superior State Administrators, Inc.</p>");
        content = content.replace("<<close>>", "");
        content = content.replace("<<#erName>>", "Sample Employer Inc.");

        // Replace the <[{0}]> … <[{19}]> placeholders with dummy values
        for (int i = 0; i < 20; i++) {
            content = content.replace("<[{" + i + "}]>",
                    AutoSafe.clean(request.getAttribute("dummyInput" + i).toString()));
        }

        // Split subject/body exactly like SendAutoFinal25 does
        String[] lines = content.split("\n", 2);
        String subject = lines[0].trim();
        if (subject.isEmpty()) subject = a.getAutomationName();

        String body = lines.length > 1 ? lines[1] : content;

        // Final HTML cleanup (same as real email)
        body = AutoSafe.clean(body)
                .replace("\n", "<br>")
                .replace("<nl>", "<br>");

        request.setAttribute("previewSubject", subject);
        request.setAttribute("previewBody", body);

        request.getRequestDispatcher("/WEB-INF/view/a/taskManager/previewEmailModal.jsp")
                .forward(request, response);
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
}