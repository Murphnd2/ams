package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;

@WebServlet(name = "ServiceManagerAction", value = "/ServiceManagerAction")
public class ServiceManagerAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String action = request.getParameter("action");
        String losIdParam = request.getParameter("losId");
        String enhIdParam = request.getParameter("enhId");

        try {
            switch (action) {

                // ── LOS CRUD ────────────────────────────────────────────

                case "createLos" -> {
                    String description = request.getParameter("description");
                    String shortText = request.getParameter("shortText");
                    LOS los = new LOS();
                    los.setDescription(description.trim());
                    los.setShortText(shortText.trim());
                    los.setSortOrder(9999);
                    los.setSuppressed(false);
                    los.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(los);
                    em.getTransaction().commit();
                    losIdParam = los.getId().toString();
                }

                case "editLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    los.setDescription(request.getParameter("description").trim());
                    los.setShortText(request.getParameter("shortText").trim());
                    em.merge(los);
                    em.getTransaction().commit();
                }

                case "suppressLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    los.setSuppressed(!los.isSuppressed());
                    em.merge(los);
                    em.getTransaction().commit();
                }

                // ── Enhancement CRUD ────────────────────────────────────

                case "createEnhancement" -> {
                    String description = request.getParameter("description");
                    String shortText = request.getParameter("shortText");
                    Enhancement enh = new Enhancement();
                    enh.setDescription(description.trim());
                    enh.setShortText(shortText.trim());
                    enh.setSortOrder(9999);
                    enh.setSuppressed(false);
                    enh.setPsp(psp);
                    em.getTransaction().begin();
                    em.persist(enh);
                    em.getTransaction().commit();
                    enhIdParam = enh.getId().toString();
                }

                case "editEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    em.getTransaction().begin();
                    enh.setDescription(request.getParameter("description").trim());
                    enh.setShortText(request.getParameter("shortText").trim());
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                case "suppressEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    em.getTransaction().begin();
                    enh.setSuppressed(!enh.isSuppressed());
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                // ── Enhancement ↔ LOS Associations ──────────────────────

                case "assignEnhancementToLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long enhId = Long.parseLong(request.getParameter("enhId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    if (!enh.getLosList().contains(los)) {
                        em.getTransaction().begin();
                        enh.getLosList().add(los);
                        em.merge(enh);
                        em.getTransaction().commit();
                    }
                }

                case "removeEnhancementFromLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long enhId = Long.parseLong(request.getParameter("enhId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    em.getTransaction().begin();
                    enh.getLosList().remove(los);
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                case "assignLosToEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long losId = Long.parseLong(request.getParameter("losId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    LOS los = EntityLookup.getLosById(em, losId);
                    if (!enh.getLosList().contains(los)) {
                        em.getTransaction().begin();
                        enh.getLosList().add(los);
                        em.merge(enh);
                        em.getTransaction().commit();
                    }
                }

                case "removeLosFromEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long losId = Long.parseLong(request.getParameter("losId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    LOS los = EntityLookup.getLosById(em, losId);
                    em.getTransaction().begin();
                    enh.getLosList().remove(los);
                    em.merge(enh);
                    em.getTransaction().commit();
                }

                // ── ApplicationSection ↔ LOS Associations ───────────────

                case "assignAppSectionToLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    ApplicationSection section = em.find(ApplicationSection.class, sectionId);
                    if (!section.getLosList().contains(los)) {
                        em.getTransaction().begin();
                        section.getLosList().add(los);
                        em.merge(section);
                        em.getTransaction().commit();
                    }
                }

                case "removeAppSectionFromLos" -> {
                    long losId = Long.parseLong(losIdParam);
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    LOS los = EntityLookup.getLosById(em, losId);
                    ApplicationSection section = em.find(ApplicationSection.class, sectionId);
                    em.getTransaction().begin();
                    section.getLosList().remove(los);
                    em.merge(section);
                    em.getTransaction().commit();
                }

                // ── ApplicationSection ↔ Enhancement Associations ───────

                case "assignAppSectionToEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    ApplicationSection section = em.find(ApplicationSection.class, sectionId);
                    if (!section.getEnhancementList().contains(enh)) {
                        em.getTransaction().begin();
                        section.getEnhancementList().add(enh);
                        em.merge(section);
                        em.getTransaction().commit();
                    }
                }

                case "removeAppSectionFromEnhancement" -> {
                    long enhId = Long.parseLong(enhIdParam);
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    Enhancement enh = em.find(Enhancement.class, enhId);
                    ApplicationSection section = em.find(ApplicationSection.class, sectionId);
                    em.getTransaction().begin();
                    section.getEnhancementList().remove(enh);
                    em.merge(section);
                    em.getTransaction().commit();
                }
            }

        } finally {
            em.close();
        }

        // Redirect back preserving selection and tab
        String redirect = "ServiceManagerHome";
        if (losIdParam != null && !losIdParam.isEmpty()) {
            redirect += "?losId=" + losIdParam;
        } else if (enhIdParam != null && !enhIdParam.isEmpty()) {
            redirect += "?enhId=" + enhIdParam + "&tab=enhancement";
        }
        response.sendRedirect(redirect);
    }
}
