package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Helper;
import net.superiorstate.ams.data.Importer;

import java.io.File;
import java.io.IOException;

@WebServlet(name = "ImportCsvFiles25", value = "/ImportCsvFiles25")
public class ImportCsvFiles25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            Importer.importAllMatchingFilesInMappingOrder(em, new File(Helper.CSV_DIR), new File(Helper.PROCESSED_DIR));

            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }

        response.sendRedirect("ViewActivity25");
    }

}



