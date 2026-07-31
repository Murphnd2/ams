package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Post-create hook run whenever a Setup activity is born from an Application.
 * Adds cross-links on the new Setup, optionally promotes the source
 * Opportunity to WON, and closes it when a PSP user owned it.
 *
 * Called from ReviewApplication (approve path) and CreateSetup25.
 * Must never throw — the Setup has already been persisted by the caller.
 */
public final class SetupPromotionService {

    private SetupPromotionService() {}

    public static void promoteAfterSetupCreation(
            HttpServletRequest request,
            EntityManager em,
            Setup setup,
            Application application,
            Person currentPerson) {

        if (setup == null || application == null) return;
        Proposal proposal = application.getProposal();
        if (proposal == null) return;

        // Step A — Application link on the Setup (always)
        try {
            String prospectName = (proposal.getProspect() != null)
                    ? proposal.getProspect().getName() : null;
            String label = (prospectName != null && !prospectName.isBlank())
                    ? "Application — " + prospectName
                    : "Application";
            attachExternalLink(em, setup.getId(), label,
                    "ReviewApplication?id=" + proposal.getId());
        } catch (Exception e) {
            System.out.println("[SetupPromotionService] Step A (application link) failed: " + e.getMessage());
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Step B — Resolve source Opportunity
        Activity src = proposal.getSourceActivity();
        if (!(src instanceof Opportunity)) return;

        Opportunity opp;
        try {
            opp = em.find(Opportunity.class, src.getId());
        } catch (Exception e) {
            System.out.println("[SetupPromotionService] Step B (opp lookup) failed: " + e.getMessage());
            return;
        }
        if (opp == null) return;

        // Step C — Source Opportunity link on the Setup
        try {
            String label = "Source Opportunity — " + safeName(opp);
            attachExternalLink(em, setup.getId(), label, "ViewById?id=" + opp.getId());
        } catch (Exception e) {
            System.out.println("[SetupPromotionService] Step C (opportunity link) failed: " + e.getMessage());
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        // Step D — Promote stage to WON (unless already WON) + Step E — log note
        boolean stageWasChanged = false;
        try {
            if (!"WON".equals(opp.getStage())) {
                em.getTransaction().begin();
                opp.setStage("WON");
                em.merge(opp);
                em.getTransaction().commit();
                stageWasChanged = true;
            }
        } catch (Exception e) {
            System.out.println("[SetupPromotionService] Step D (stage promote) failed: " + e.getMessage());
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        if (stageWasChanged) {
            try {
                logPromotionNote(em, opp, proposal, application, setup, currentPerson);
            } catch (Exception e) {
                System.out.println("[SetupPromotionService] Step E (note) failed: " + e.getMessage());
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
            }
        }

        // Step F — Close Opportunity when managed by a PSP user
        if (opp.getManagedBy() != null) {
            try {
                em.getTransaction().begin();
                opp.setComplete(true);
                opp.setDateCompleted(Date.valueOf(LocalDate.now()));
                opp.setCompletedBy(currentPerson);
                em.merge(opp);
                em.getTransaction().commit();

                removeFromOpenCache(request, opp.getId());
            } catch (Exception e) {
                System.out.println("[SetupPromotionService] Step F (close opp) failed: " + e.getMessage());
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
            }
        }
    }

    // ======================== Helpers ========================

    private static void attachExternalLink(EntityManager em, Long activityId,
                                           String label, String linkPath) {
        LinkType externalType = EntityLookup.getLinkTypeById(em, 2);
        WebLink link = new WebLink();
        link.setPlainText(label);
        link.setLinkPath(linkPath);
        link.setLinkType(externalType);
        link.setActive(true);

        em.getTransaction().begin();
        em.persist(link);
        em.getTransaction().commit();

        Activity activity = EntityLookup.getActivityById(em, activityId);
        if (activity == null) return;

        em.getTransaction().begin();
        activity.addWebLink(link);
        em.persist(activity);
        em.getTransaction().commit();
    }

    private static void logPromotionNote(EntityManager em, Opportunity opp, Proposal proposal,
                                         Application application, Setup setup, Person currentPerson) {
        String prospectName = (proposal.getProspect() != null && proposal.getProspect().getName() != null)
                ? proposal.getProspect().getName() : "prospect";
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String services = resolveServicesDescription(em, application, proposal);

        StringBuilder detail = new StringBuilder();
        detail.append("Setup created on ").append(today).append(" for ").append(prospectName).append(".<br>");
        detail.append("Services selected: ").append(services).append("<br>");
        detail.append("Setup: <a href=\"ViewById?id=").append(setup.getId())
                .append("\" target=\"_blank\">#").append(setup.getId()).append("</a>");

        em.getTransaction().begin();
        Note note = new Note();
        note.setDetail(detail.toString());
        note.setActivity(opp);
        note.setCreatedBy(currentPerson);
        note.setDateGenerated(Date.valueOf(LocalDate.now()));
        note.setReasonCreated(EntityLookup.getReasonById(em, 1));   // Internal Note
        note.setStatus(EntityLookup.getActivityStatusById(em, 2));  // No Change
        em.persist(note);
        em.getTransaction().commit();
    }

    private static String resolveServicesDescription(EntityManager em,
                                                     Application application,
                                                     Proposal proposal) {
        List<String> parts = new ArrayList<>();

        List<Long> losIds = (application != null) ? application.getSelectedLosIdList() : null;
        if (losIds != null && !losIds.isEmpty()) {
            for (Long id : losIds) {
                try {
                    LOS los = EntityLookup.getLosById(em, id);
                    String name = (los != null) ? los.getShortText() : null;
                    if (name != null && !name.isBlank()) parts.add(name);
                } catch (Exception ignored) {}
            }
            List<Long> enhIds = application.getSelectedEnhancementIdList();
            if (enhIds != null) {
                for (Long id : enhIds) {
                    try {
                        Enhancement enh = em.find(Enhancement.class, id);
                        if (enh != null) {
                            String name = (enh.getShortText() != null && !enh.getShortText().isBlank())
                                    ? enh.getShortText() : enh.getDescription();
                            if (name != null && !name.isBlank()) parts.add(name);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (parts.isEmpty() && proposal != null && proposal.getLosList() != null) {
            for (LOS los : proposal.getLosList()) {
                if (los != null && los.getShortText() != null && !los.getShortText().isBlank()) {
                    parts.add(los.getShortText());
                }
            }
        }

        return parts.isEmpty() ? "(none specified)" : String.join(", ", parts);
    }

    private static void removeFromOpenCache(HttpServletRequest request, Long oppId) {
        try {
            AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (global == null || global.getActivitiesAllOpen() == null) return;

            // Collectors.toList(), not .toList() - this list is pushed into the global/local
            // caches below and later mutated in place (e.g. AmsDataLocal's ADD_RENEWAL branch).
            // .toList() returns an immutable list and poisons the cache.
            List<Activity25u> updated = global.getActivitiesAllOpen().stream()
                    .filter(au -> au.getActivity() == null
                            || !oppId.equals(au.getActivity().getId()))
                    .collect(Collectors.toList());
            global.setActivitiesAllOpen(updated);
            if (local != null) {
                local.setActivitiesAllOpen(updated);
                request.getSession().setAttribute("local", local);
            }
            request.getServletContext().setAttribute("global", global);
        } catch (Exception e) {
            System.out.println("[SetupPromotionService] cache removal skipped: " + e.getMessage());
        }
    }

    private static String safeName(Opportunity opp) {
        try {
            String n = opp.getFullName();
            if (n != null && !n.isBlank()) return n;
        } catch (Exception ignored) {}
        return "Source Opportunity";
    }
}
