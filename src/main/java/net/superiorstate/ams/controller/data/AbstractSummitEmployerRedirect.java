package net.superiorstate.ams.controller.data;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.SummitEmployerResolver;
import net.superiorstate.ams.model.activity.Activity;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base for Summit employer-scoped redirect servlets. Resolves the current
 * active activity to a Summit employer altId and redirects to a
 * subclass-provided Summit URL. Renders a "not applicable" page when the
 * active activity has no resolvable employer.
 */
public abstract class AbstractSummitEmployerRedirect extends HttpServlet {

    /** Build the destination Summit URL from base path, tpaGuid, and employer altId. */
    protected abstract String buildSummitUrl(String summitPath, String tpaGuid, int employerAltId);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Activity activity = (local != null && local.getCurrentActivity() != null)
                ? local.getCurrentActivity().getActivity() : null;

        Integer altId = SummitEmployerResolver.resolveAltId(activity);
        if (altId == null) {
            renderNotApplicable(response);
            return;
        }

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        String url = buildSummitUrl(global.getSummitPath(), global.getSummitTpaGuid(), altId);
        response.sendRedirect(url);
    }

    private void renderNotApplicable(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html><head><title>Not Applicable</title>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<style>body{font-family:sans-serif;padding:2rem;color:#333;}"
                + ".msg{max-width:520px;margin:3rem auto;text-align:center;}"
                + "h2{color:#0b5;} p{color:#555;}</style></head><body>");
        out.println("<div class='msg'><h2>Not Applicable</h2>");
        out.println("<p>This Summit link isn't available for the current activity. "
                + "It applies only to renewals and to tickets tied to an employee of an employer.</p>");
        out.println("<p>You can close this tab.</p></div></body></html>");
        out.flush();
    }
}
