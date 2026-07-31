package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.resolver.IchraAccessResolver;

import java.io.IOException;

/**
 * Top-level ICHRA hub — the front door every future ICHRA capability hangs off.
 * Pure navigation: carries no rate data, no carrier names, no plan lists, no numbers.
 */
@WebServlet(name = "IchraHome", value = "/IchraHome")
public class IchraHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isAuthorized(request)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        request.setAttribute("pageTitle", "ICHRA");
        request.setAttribute("pageIcon", "bi-heart-pulse");
        request.getRequestDispatcher("/WEB-INF/view/market/ichraHome25.jsp").forward(request, response);
    }

    private boolean isAuthorized(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            return IchraAccessResolver.isAvailable(em, request);
        } finally {
            if (em.isOpen()) em.close();
        }
    }
}
