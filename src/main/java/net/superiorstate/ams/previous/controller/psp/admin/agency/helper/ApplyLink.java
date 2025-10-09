package net.superiorstate.ams.previous.controller.psp.admin.agency.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;

import java.io.IOException;

@WebServlet(name = "ApplyLink", value = "/ApplyLink")
public class ApplyLink extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String hString = hrefString(request);
        response.sendRedirect(hString);
    }

    private String hrefString(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String pGuid = request.getParameter("btnProposalApply");
        Proposal proposal = dG.getProposalByGuid(em,pGuid);
        String string = dG.getJotFormParameterString(em,proposal);
        em.close();
        return string;
    }
}
