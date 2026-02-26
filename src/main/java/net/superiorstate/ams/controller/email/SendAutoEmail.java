package net.superiorstate.ams.controller.email;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Legacy redirect wrapper — DB task.servletName records contain "SendAutoEmail?aeId=123".
 * Forwards all requests to SendAuto25.
 *
 * Future cleanup: Update UpdateTask25.addAutomationToTask() to write "SendAuto25?aeId=...",
 * migrate existing DB records, then delete this servlet.
 */
@WebServlet(name = "SendAutoEmail", value = "/SendAutoEmail")
public class SendAutoEmail extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        forward(request, response);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String aeId = request.getParameter("aeId");
        if (aeId == null || aeId.isBlank()) {
            response.sendError(400, "Missing aeId parameter");
            return;
        }
        request.getRequestDispatcher("/SendAuto25?aeId=" + aeId).forward(request, response);
    }
}