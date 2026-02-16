package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Updater;

import java.io.IOException;

@WebServlet(name = "ReviewHsaAccounts25", value = "/ReviewHsaAccounts25")
public class ReviewHsaAccounts25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Updater.processHsaErFromAccounts(em);
            Updater.processHsaEeFromAccounts(em);
            request.setAttribute("message", "HSA review process completed successfully.");
        } catch (Exception e) {
            request.setAttribute("message", "Error during HSA review: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em.isOpen()) em.close();
        }

        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/admin/result.jsp");
        dispatcher.forward(request, response);
    }

}


