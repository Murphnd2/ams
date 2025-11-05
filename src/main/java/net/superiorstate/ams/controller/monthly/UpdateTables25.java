package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.*;

import java.io.IOException;

@WebServlet(name = "UpdateTables25", value = "/UpdateTables25")
public class UpdateTables25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request,response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            //Importer.importMissingPlanTypes(em,Helper.CSV_DIR,Helper.PROCESSED_DIR);
            Updater.processEmployerImportI1FastAddsActiveOnly(em);
            Updater.processEmployerImportI1SelectiveUpdates(em);
            Updater.processEmployeeAddsFromI2I3(em);
            Updater.processEmployeeUpdatesFromI2Efficient(em);
            Updater.reAssociateOrphanEmployees(em);
            Updater.updateEmployeeActiveStatusQueryDriven(em);
            Updater.makeEmployeesInactiveIfNotInI2OrI3(em);
            Updater.updateEmployeeStatusFromCobraList(em);
            Updater.updateEeCobraStatus(em);
            Updater.ensurePrimaryContactEmployee(em);
            Updater.mergeNegativeToPositiveEmployees(em);
            Updater.processNewBenefitI4Fast(em, Helper.SKIPPED_DIR);
            Updater.processNewBenefitI7Fast(em, Helper.SKIPPED_DIR);
            Updater.syncBenefit(em);
            Updater.processBenefitTiersFromI7Import(em);
            Updater.processHsaErFromAccounts(em);
            Updater.processHsaEeFromAccounts(em);
            // Refresh Employee / Employer / and Template Purposes
            AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
            if(global != null) {
                System.out.println("🔁 Refreshing global listing in application");
                global.miniUpdate(em);
            }
            //Refresh Renewal List after Updates
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local != null) {
                System.out.println("🔁 Refreshing renewal listing in session");
                local.refreshRenewals(em);
            }

            request.setAttribute("message", "Update process successfully completed.");
        } catch (Exception e) {
            request.setAttribute("message", "Error during Table Updates: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect("ViewHome25");
    }
}
