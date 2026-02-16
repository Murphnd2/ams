package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@WebServlet(name = "RemoveRecipient25", value = "/RemoveRecipient25")
public class RemoveRecipient25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    private void doThis(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        String btnString = request.getSession().getAttribute("emailAction").toString();
        Long personId = Long.parseLong(btnString.substring(3));
        System.out.println("pid: "+ personId);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person p = dM.getPersonById(em,personId);
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        System.out.println(local.getCurrentEmail().getRecipientList().size());
        List<Person> newList = local.getCurrentEmail().getRecipientList().stream().filter(obj-> !Objects.equals(obj.getId(), p.getId())).collect(Collectors.toList());
        System.out.println(newList.size());
        local.getCurrentEmail().setRecipientList(newList);
        request.getSession().setAttribute("local",local);
        em.close();
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        dispatcher.forward(request,response);
    }
}
