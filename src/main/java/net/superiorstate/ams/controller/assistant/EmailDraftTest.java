package net.superiorstate.ams.controller.assistant;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Simple admin test page for the EmailAssistantDraft endpoint.
 * PSP Admin only. GET forwards to the JSP; the JSP submits
 * directly to /EmailAssistantDraft via JavaScript fetch().
 *
 * URL: /EmailDraftTest
 */
@WebServlet(name = "EmailDraftTest", value = "/EmailDraftTest")
public class EmailDraftTest extends HttpServlet {

    private static final Logger log = LogManager.getLogger(EmailDraftTest.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        AmsDataLocal local  = (session != null)
                ? (AmsDataLocal) session.getAttribute("local")
                : null;
        if (local == null || !local.isPspAdmin()) {
            response.sendRedirect("ViewHome25");
            return;
        }

        request.setAttribute("pageTitle", "Email Draft Test");
        request.setAttribute("pageIcon",  "bi-envelope-paper");
        log.debug("EmailDraftTest: admin {} loading test page",
                local.getCurrentPerson().getEmail());
        request.getRequestDispatcher(
                "/WEB-INF/view/a/assistant/emailDraftTest25.jsp")
                .forward(request, response);
    }
}
