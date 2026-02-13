package net.superiorstate.ams.previous.controller.summit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.previous.data.Starter;

import java.io.IOException;

@WebServlet(name = "InitializeDataBase", value = "/InitializeDataBase")
public class InitializeDataBase extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Security check: require deployment key
        String deploymentKey = request.getParameter("deploymentKey");
        String expectedKey = System.getenv("DB_INIT_KEY");

        if (expectedKey == null || expectedKey.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Server configuration error: DB_INIT_KEY environment variable not set");
            return;
        }

        if (deploymentKey == null || !deploymentKey.equals(expectedKey)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Invalid deployment key");
            return;
        }

        // Original initialization code
        System.out.println("GOT HERE");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        doThisFirst(request,em);
        request.getSession().setAttribute("uninitialized",1);

        //Load global data after initialization
        AmsDataGlobal global = new AmsDataGlobal();
        global.initializeGlobalData(em);
        request.getServletContext().setAttribute("global",global);

        em.close();
        goToPage(request,response);
    }

    private void doThisFirst(HttpServletRequest request,EntityManager em) {
        Starter.initializeDataBase(request,em);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("LogOut");
        dispatcher.forward(request,response);
    }

}
