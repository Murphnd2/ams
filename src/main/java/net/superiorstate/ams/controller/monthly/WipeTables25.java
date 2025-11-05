package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Cleaner;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "WipeTables25", value = "/WipeTables25")
public class WipeTables25 extends HttpServlet {

    private static final List<String> TABLES = List.of(
            "import1employer",
            "import2employee",
            "import3employeealt",
            "import4benefitcdh",
            "import5benefityear",
            "import6enrollment",
            "import7benefitpb",
            "import8cobraqb",
            "import9cobrapart",
            "importacoverage",
            "importbcobraterm",
            "importbenefittier",
            "hsaaccount"
    );

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        executeWipe(request);
        goToPage(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    private void executeWipe(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Cleaner cleaner = new Cleaner(em) {}; // use anonymous class or subclass
            cleaner.wipeTables(TABLES);
        } finally {
            em.close();
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }
}


