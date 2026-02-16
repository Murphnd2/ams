package net.superiorstate.ams.data;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Importer;

import java.io.IOException;

@WebServlet(name = "ShowUploadPage", value = "/ShowUploadPage")
public class ShowUploadPage extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Preload table mappings for JavaScript preview
        request.setAttribute("tableMappings", Importer.TABLE_MAPPINGS);

        // Forward to upload JSP
        request.getRequestDispatcher("/WEB-INF/view/a/z_acessory/fileUploadPage.jsp")
                .forward(request, response);
    }

    // Not used here, but retained for clarity
    private void goToPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome25.jsp");
        dispatcher.forward(request, response);
    }
}

