package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.sales.offering.LOS;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "ModuleView", value = "/ModuleView")
public class ModuleView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        parseButtonClick(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        parseButtonClick(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);

    }

    private void parseButtonClick(HttpServletRequest request, HttpServletResponse response){
        String buttonCode = request.getParameter("moduleSelectButton");
        int stringLength = buttonCode.length();
        int moduleId = Integer.parseInt(buttonCode.substring(2,stringLength));
        System.out.println(moduleId);
        int buttonOption = Integer.parseInt(buttonCode.substring(0,1));
        if(buttonOption==1)
            removeModule(request, response, moduleId);
        else fillModuleTable(request,response,moduleId);
    }


    private void fillModuleTable(HttpServletRequest request, HttpServletResponse response, int moduleId){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        ServiceModule currentModule = dM.getServiceModuleById(em,moduleId);
        request.getSession().setAttribute("hasCurrentModule",true);
        request.getSession().setAttribute("currentModule",currentModule);
        em.close();
    }
    private void removeModule(HttpServletRequest request, HttpServletResponse response, int moduleId){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LOS currentLos = (LOS) request.getSession().getAttribute("currentLos");
        LOS los = dG.getLosFull(em,currentLos.getId());
        ServiceModule serviceModule = dG.getModuleFull(em, moduleId);
        los.removeServiceModule(serviceModule);
        em.persist(los);
        em.persist(serviceModule);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("hasCurrentModule",false);
        request.getSession().setAttribute("currentModule", new ServiceModule());
    }
}
