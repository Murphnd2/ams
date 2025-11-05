package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.renewal.GenerateRen;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;
import net.superiorstate.ams.previous.model.summit.imports.DpiEr;
import net.superiorstate.ams.previous.model.summit.imports.sEmployer;

import java.io.IOException;

@WebServlet(name = "CreateNewEmployer", value = "/CreateNewEmployer")
public class CreateNewEmployer extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createEmployer(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createEmployer(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetAdminView");
        dispatcher.forward(request,response);
    }

    private void createEmployer(HttpServletRequest request){
        int oid = Integer.parseInt(request.getParameter("oid"));
        int eid = Integer.parseInt(request.getParameter("eid"));
        String eky = request.getParameter("eky");
        int erKey = Integer.parseInt(eky);

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Query q = em.createQuery("SELECT d FROM DpiEr d WHERE d.erKey = :erKey");
        q.setParameter("erKey",erKey);
        DpiEr dpiEr;
        try{
            dpiEr = (DpiEr) q.getSingleResult();
        } catch (NoResultException e){
            dpiEr = null;
        }
        if(dpiEr==null)
            return;

        Query query = em.createQuery("SELECT s FROM sEmployer s WHERE s.dpiSuiteErKey = :erKey");
        query.setParameter("erKey",eky.trim());
        sEmployer ser;
        try{
            ser = (sEmployer) query.getSingleResult();
        } catch (NoResultException e){
            ser = null;
        }
        if(ser!=null)
            return;

        em.getTransaction().begin();
        sEmployer er = new sEmployer();
        er.setOrganizationId(oid);
        er.setEmployerId(eid);
        er.setDpiSuiteErKey(eky);
        er.setEmployerName(dpiEr.getErCompany());
        er.setPhone(dpiEr.getPhone());
        er.setPhoneNumber(dpiEr.getPhoneClean());
        er.setEmail(dpiEr.getEmail());
        er.setStatus("Active");
        er.setPrimaryContact(dpiEr.getFullName());
        er.setSetup("Active, Setup Note Complete");
        er.setTaxId(dpiEr.getTaxId());
        em.persist(er);
        em.getTransaction().commit();

        Query q2 = em.createQuery("SELECT e FROM Employer e WHERE e.id = :id");
        q2.setParameter("id",er.getOrganizationId());
        Employer employer;
        try{
            employer = (Employer) q2.getSingleResult();
        } catch (NoResultException e){
            employer = null;
        }
        if(employer!=null)
            return;

        em.getTransaction().begin();
        employer = new Employer();
        employer.setPhone(er.getPhone());
        employer.setEmail(er.getEmail());
        employer.setEmployerName(er.getEmployerName());
        employer.setErKey(erKey);
        employer.setContactName(er.getPrimaryContact());
        employer.setActive(false);
        employer.setAgency(false);
        employer.setBillable(false);
        em.persist(employer);
        em.getTransaction().commit();

        Person creator = (Person) request.getSession().getAttribute("currentPerson");
        Employee ee = GenerateRen.createNewEmployee(em,employer,employer.getEmail(),creator, dpiEr.getErFirst(), dpiEr.getErLast());
        GenerateRen.assignEmployeeAsContact(em,employer,ee);

        em.close();
    }
}
