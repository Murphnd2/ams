package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceID;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.*;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;
import net.superiorstate.ams.model.sales.offering.*;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conference Demo Data Seeder — creates all demo entities for demo.superiorstate.biz.
 * Called from DatabaseInitializer.seedDemoData("CONFERENCE_DEMO") or SeedDemoData servlet.
 *
 * Self-contained: expects only the core seed data from DatabaseInitializer.performInitialization().
 */
public abstract class DemoDataSeeder {

    // ═══════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════

    public static void seedConferenceDemo(EntityManager em) {
        System.out.println("🎭 DemoDataSeeder: starting CONFERENCE_DEMO seed...");

        PSP psp = EntityLookup.getPspById(em, 4L);
        Person adminPerson = EntityLookup.getPersonById(em, 104L);
        if (psp == null || adminPerson == null) {
            throw new IllegalStateException("Database not initialized — PSP or admin person not found.");
        }

        // 3A — Lines of Service
        System.out.println("  Section 3A: Lines of Service");
        seedLinesOfService(em, psp);
        em.clear();

        // 3B — Enhancement
        System.out.println("  Section 3B: Enhancement");
        seedEnhancement(em, psp);
        em.clear();

        // 3C — Rate
        System.out.println("  Section 3C: Rate");
        seedDemoAgencyRate(em, psp);
        em.clear();

        // 3D — Agency + Prospects
        System.out.println("  Section 3D: Agency + Prospects");
        seedAgencyAndProspects(em, psp);
        em.clear();

        // 3E — Resource Library
        System.out.println("  Section 3E: Resource Library");
        seedResourceLibrary(em, psp);
        em.clear();

        // 3F — Features
        System.out.println("  Section 3F: Features");
        seedFeatures(em, psp);
        em.clear();

        // 3G — Reusable Tasks + Setup Sequences
        System.out.println("  Section 3G: Tasks + Setup Sequences");
        seedReusableTasks(em, psp);
        seedSetupSequences(em, psp);
        em.clear();

        // 3H — Renewal Sequences
        System.out.println("  Section 3H: Renewal Sequences");
        seedRenewalSequences(em, psp);
        em.clear();

        // 3I — Ticket Categories + Sequences
        System.out.println("  Section 3I: Ticket Categories + Sequences");
        seedTicketCategories(em, psp);
        em.clear();

        // 3J — Summit Employers + Employees + Benefits
        System.out.println("  Section 3J: Employers + Employees + Benefits");
        seedSummitData(em, psp);
        em.clear();

        // 3K — Renewals (past history)
        System.out.println("  Section 3K: Renewal History");
        seedRenewalHistory(em, psp);
        em.clear();

        // 3L — Standalone Checklists (ToDos)
        System.out.println("  Section 3L: Standalone Checklists");
        seedStandaloneChecklists(em, psp);
        em.clear();

        // 3M — Tickets
        System.out.println("  Section 3M: Tickets");
        seedTickets(em, psp);
        em.clear();

        // 3N — Setup Activity (Opportunity + Proposal + Application)
        System.out.println("  Section 3N: Opportunity + Proposal + Application");
        seedOpportunity(em, psp);
        em.clear();

        // 3O — Proposal Settings: skipped — ProposalSettings.initializeDefaults()
        //       auto-creates TITLE, FEATURES, PRICING, CLOSING on first visit.

        // 3P — BPO Partnership
        System.out.println("  Section 3P: BPO Partnership");
        seedBpoPartnership(em, psp);
        em.clear();

        // Close initialization checklist
        closeInitializationChecklist(em);

        // 3Q — Idempotency
        System.out.println("  Section 3Q: Marking complete");
        DatabaseInitializer.createConstant(em, "DEMO_DATA_SEEDED", "true");
        DatabaseInitializer.createConstant(em, "DEMO_SEEDED_DATE", LocalDate.now().toString());

        System.out.println("🎭 DemoDataSeeder: CONFERENCE_DEMO seed complete.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  3A — LINES OF SERVICE
    // ═══════════════════════════════════════════════════════════════

    private static void seedLinesOfService(EntityManager em, PSP psp) {
        ActivityCategory setupCat = EntityLookup.getTemplateGroupById(em, 2);

        // FSA
        ServiceItem siFsa = DatabaseInitializer.createServiceItemWithCode(em, 3, "Flexible Spending Accounts", "FSA", 3, setupCat, psp);
        LOS losFsa = DatabaseInitializer.createLos(em, 2L, "Flexible Spending Accounts", "FSA", psp, siFsa);
        ServiceModule smFsa = DatabaseInitializer.createServiceModule(em, 3L, "Flexible Spending Accounts", "FSA", 300, psp);
        DatabaseInitializer.associateModuleToLos(em, losFsa, smFsa);
        em.getTransaction().begin();
        smFsa.setLos(losFsa);
        em.merge(smFsa);
        em.getTransaction().commit();

        // HRA
        ServiceItem siHra = DatabaseInitializer.createServiceItemWithCode(em, 4, "Health Reimbursement Arrangement", "HRA", 4, setupCat, psp);
        LOS losHra = DatabaseInitializer.createLos(em, 3L, "Health Reimbursement Arrangement", "HRA", psp, siHra);
        ServiceModule smHra = DatabaseInitializer.createServiceModule(em, 4L, "Health Reimbursement Arrangement", "HRA", 400, psp);
        DatabaseInitializer.associateModuleToLos(em, losHra, smHra);
        em.getTransaction().begin();
        smHra.setLos(losHra);
        em.merge(smHra);
        em.getTransaction().commit();

        // COBRA
        ServiceItem siCobra = DatabaseInitializer.createServiceItemWithCode(em, 5, "COBRA", "COBRA", 5, setupCat, psp);
        LOS losCobra = DatabaseInitializer.createLos(em, 4L, "COBRA", "COBRA", psp, siCobra);
        ServiceModule smCobra = DatabaseInitializer.createServiceModule(em, 5L, "COBRA", "COBRA", 500, psp);
        DatabaseInitializer.associateModuleToLos(em, losCobra, smCobra);
        em.getTransaction().begin();
        smCobra.setLos(losCobra);
        em.merge(smCobra);
        em.getTransaction().commit();

        // Assign ALL-scoped ApplicationSections to each new LOS
        Enhancement enhMain = em.find(Enhancement.class, 1L);
        DatabaseInitializer.assignAllSectionsToLosAndEnhancement(em, psp, losFsa, enhMain);
        DatabaseInitializer.assignAllSectionsToLosAndEnhancement(em, psp, losHra, enhMain);
        DatabaseInitializer.assignAllSectionsToLosAndEnhancement(em, psp, losCobra, enhMain);
    }

    // ═══════════════════════════════════════════════════════════════
    //  3B — ENHANCEMENT
    // ═══════════════════════════════════════════════════════════════

    private static void seedEnhancement(EntityManager em, PSP psp) {
        ActivityCategory setupCat = EntityLookup.getTemplateGroupById(em, 2);

        ServiceItem siCards = DatabaseInitializer.createServiceItemWithCode(em, 6, "Debit Card Services", "CARDS", 6, setupCat, psp);
        Enhancement enhCards = DatabaseInitializer.createEnhancement(em, 2L, "Debit Card Services", "CARDS", psp, siCards);
        ServiceModule smCards = DatabaseInitializer.createServiceModule(em, 6L, "Debit Card Services", "CARDS", 600, psp);
        em.getTransaction().begin();
        smCards.setEnhancement(enhCards);
        em.merge(smCards);
        em.getTransaction().commit();

        // Link Enhancement to FSA LOS only
        LOS fsaLos = em.find(LOS.class, 2L);
        em.getTransaction().begin();
        enhCards.setLosList(new ArrayList<>());
        enhCards.getLosList().add(fsaLos);
        em.merge(enhCards);
        em.getTransaction().commit();

        // Assign ALL-scoped ApplicationSections to Enhancement
        List<LOS> allLos = em.createQuery(
                "SELECT l FROM LOS l WHERE l.psp.id = :pspId", LOS.class)
                .setParameter("pspId", psp.getId())
                .getResultList();
        // We already assigned sections in 3A; just assign to this new enhancement
        List<ApplicationSection> sections = em.createQuery(
                "SELECT s FROM ApplicationSection s WHERE s.psp.id = :pspId AND s.scope = 'ALL' AND s.suppressed = false",
                ApplicationSection.class)
                .setParameter("pspId", psp.getId().longValue())
                .getResultList();
        for (ApplicationSection section : sections) {
            em.getTransaction().begin();
            if (section.getEnhancementList() == null) section.setEnhancementList(new ArrayList<>());
            section.getEnhancementList().add(enhCards);
            em.merge(section);
            em.getTransaction().commit();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  3C — DEMO AGENCY RATE
    // ═══════════════════════════════════════════════════════════════

    private static void seedDemoAgencyRate(EntityManager em, PSP psp) {
        Rate demoRate = DatabaseInitializer.createRate(em, 2L, "Demo Agency Rate", psp);

        PriceItem pi1 = EntityLookup.getPriceItemById(em, 1L); // Setup
        PriceItem pi2 = EntityLookup.getPriceItemById(em, 2L); // Annual
        PriceItem pi3 = EntityLookup.getPriceItemById(em, 3L); // Monthly

        ServiceModule smFsa = EntityLookup.getServiceModuleById(em, 3, true);
        ServiceModule smCobra = EntityLookup.getServiceModuleById(em, 5, true);
        ServiceModule smCards = EntityLookup.getServiceModuleById(em, 6, true);

        // FSA pricing (sortOrder 100-300 keeps FSA entries grouped first)
        assignRateTable(em, demoRate, smFsa, pi1, 500.00, 100);
        assignRateTable(em, demoRate, smFsa, pi2, 400.00, 200);
        assignRateTable(em, demoRate, smFsa, pi3, 5.00, 300);

        // COBRA pricing (sortOrder 400-600 keeps COBRA grouped after FSA)
        assignRateTable(em, demoRate, smCobra, pi1, 100.00, 400);
        assignRateTable(em, demoRate, smCobra, pi2, 80.00, 500);
        assignRateTable(em, demoRate, smCobra, pi3, 1.00, 600);

        // Debit Cards pricing (sortOrder 700 keeps Cards grouped last)
        assignRateTable(em, demoRate, smCards, pi3, 1.00, 700);
    }

    // ═══════════════════════════════════════════════════════════════
    //  3D — AGENCY + PROSPECTS
    // ═══════════════════════════════════════════════════════════════

    private static void seedAgencyAndProspects(EntityManager em, PSP psp) {
        // Create agency manager / primary contact
        Address agencyAddr = seedAddress(em, "2000 Technology Drive", "Suite 300",
                "Traverse City", "MI", "49684");
        Person agencyManager = seedPerson(em, "Sarah", "Mitchell", "Agency Owner",
                "s.mitchell@accelvantage.com", "231-555-0100", agencyAddr, psp);

        // Create Demo Agency (id=15)
        Agency demoAgency = DatabaseInitializer.createAgency(em, 15L,
                "AccelVantage Benefits", "231-555-0100", "38-4001234",
                agencyAddr, agencyManager, psp);

        // Create sales agent person and add to agency
        Person salesAgent = seedPerson(em, "James", "Rivera", "Sales Representative",
                "agent@pspdemo.com", "231-555-0101", agencyAddr, psp);
        em.getTransaction().begin();
        demoAgency.getAgentList().add(salesAgent);
        em.merge(demoAgency);
        em.getTransaction().commit();

        // Assign Demo Agency Rate to the agency
        Rate demoRate = EntityLookup.getRateById(em, 2L, true);
        if (demoRate != null) {
            Agency agencyFull = SalesDAO.getAgencyFull(em, demoAgency.getId());
            agencyFull.addRate(demoRate);
            em.getTransaction().begin();
            em.merge(agencyFull);
            em.getTransaction().commit();
        }

        // Create Prospect 1: Marcus Holloway
        Address addr1 = seedAddress(em, "350 Commerce Way", "", "Grand Rapids", "MI", "49503");
        Person marcusPerson = seedPerson(em, "Marcus", "Holloway", "Prospect Contact",
                "m.holloway@hollowaylogistics.com", "616-555-0312", addr1, psp);
        Prospect prospect1 = seedProspect(em, "Holloway Logistics Inc", marcusPerson, addr1, salesAgent);

        // Create Prospect 2: Diana Reyes
        Address addr2 = seedAddress(em, "8200 Ridge Boulevard", "Suite 400", "Ann Arbor", "MI", "48104");
        Person dianaPerson = seedPerson(em, "Diana", "Reyes", "VP of Human Resources",
                "d.reyes@summitridgehc.com", "734-555-0198", addr2, psp);
        Prospect prospect2 = seedProspect(em, "Summit Ridge Healthcare", dianaPerson, addr2, salesAgent);
    }

    // ═══════════════════════════════════════════════════════════════
    //  3E — RESOURCE LIBRARY
    // ═══════════════════════════════════════════════════════════════

    private static void seedResourceLibrary(EntityManager em, PSP psp) {
        // Resource Categories
        ResourceCategory catBrochures = seedResourceCategory(em, "Brochures", "bi-file-earmark-text", 100, psp);
        ResourceCategory catHowTo = seedResourceCategory(em, "How To", "bi-question-circle", 200, psp);

        // Marketing Materials (Resources) — type LINK renders "Open Link" in UI
        seedMarketingMaterial(em, "FSA Brochure", "Flexible Spending Account program overview brochure.",
                "LINK", "https://ams-file-storage.s3.wasabisys.com/demo/resources/fsa-brochure.pdf",
                catBrochures, 100, psp);
        seedMarketingMaterial(em, "COBRA Brochure", "COBRA administration services overview brochure.",
                "LINK", "https://ams-file-storage.s3.wasabisys.com/demo/resources/cobra-brochure.pdf",
                catBrochures, 200, psp);
        seedMarketingMaterial(em, "How To Login", "Participant portal login guide.",
                "LINK", "https://ams-file-storage.s3.wasabisys.com/demo/resources/how-to-login.pdf",
                catHowTo, 100, psp);
    }

    // ═══════════════════════════════════════════════════════════════
    //  3F — FEATURES
    // ═══════════════════════════════════════════════════════════════

    private static void seedFeatures(EntityManager em, PSP psp) {
        ServiceModule smFsa = EntityLookup.getServiceModuleById(em, 3, true);
        ServiceModule smCobra = EntityLookup.getServiceModuleById(em, 5, true);

        // Look up FSA Brochure and COBRA Brochure
        MarketingMaterial fsaBrochure = findMaterialByTitle(em, "FSA Brochure", psp);
        MarketingMaterial cobraBrochure = findMaterialByTitle(em, "COBRA Brochure", psp);

        // FSA Feature
        seedFeature(em, smFsa,
                "Maximize Your Pre-Tax Savings",
                "Our Flexible Spending Account program helps employees reduce taxable income while covering eligible medical expenses. Paired with our easy-to-use participant portal and debit card access.",
                100, psp, fsaBrochure);

        // COBRA Feature
        seedFeature(em, smCobra,
                "Seamless COBRA Administration",
                "We handle the full COBRA lifecycle — qualifying event notices, election tracking, premium billing, and compliance reporting — so your HR team can focus on what matters.",
                100, psp, cobraBrochure);
    }

    // ═══════════════════════════════════════════════════════════════
    //  3G — REUSABLE TASKS + SETUP SEQUENCES
    // ═══════════════════════════════════════════════════════════════

    private static void seedReusableTasks(EntityManager em, PSP psp) {
        Person admin = EntityLookup.getPersonById(em, 104L);

        createReusableTask(em, 200L, "Send Welcome Message", psp, admin);
        createReusableTask(em, 201L, "Send Service Agreement", psp, admin);
        createReusableTask(em, 202L, "Received Service Agreement", psp, admin);
        createReusableTask(em, 203L, "Create Client Billing", psp, admin);
        createReusableTask(em, 204L, "Setup Summit Employer and Benefits per App", psp, admin);
        createReusableTask(em, 205L, "Send Implementation Completed Message", psp, admin);
        createReusableTask(em, 206L, "Send Welcome to Renewal", psp, admin);
        createReusableTask(em, 207L, "Send Renewal Completion Message", psp, admin);
    }

    private static void seedSetupSequences(EntityManager em, PSP psp) {
        // FSA Setup Sequence (ServiceItem id=3)
        ServiceItem siFsa = EntityLookup.getServiceItemById(em, 3, true);
        siFsa = DatabaseInitializer.setHasRequiredTasks(em, siFsa, true);
        DatabaseInitializer.createRequiredTaskList(em, siFsa, psp);
        seedSequenceTasks(em, siFsa, psp, new String[]{
                null, null, null, null, null, // reusable tasks 200-205 handled separately
                "Send FSA Enrollment Pack & Instructions",
                "Received FSA Enrollments",
                "Load FSA Enrollments in Summit",
                null // reusable 205
        }, new Long[]{200L, 201L, 202L, 203L, 204L, null, null, null, 205L});

        // COBRA Setup Sequence (ServiceItem id=5)
        ServiceItem siCobra = EntityLookup.getServiceItemById(em, 5, true);
        siCobra = DatabaseInitializer.setHasRequiredTasks(em, siCobra, true);
        DatabaseInitializer.createRequiredTaskList(em, siCobra, psp);
        seedSequenceTasks(em, siCobra, psp, new String[]{
                null, null, null, null, null,
                "Send Request for List of Existing Beneficiaries",
                "Send COBRA Census Request",
                "Received COBRA Census",
                null
        }, new Long[]{200L, 201L, 202L, 203L, 204L, null, null, null, 205L});

        // Debit Cards Enhancement Setup (ServiceItem id=6)
        ServiceItem siCards = EntityLookup.getServiceItemById(em, 6, true);
        siCards = DatabaseInitializer.setHasRequiredTasks(em, siCards, true);
        DatabaseInitializer.createRequiredTaskList(em, siCards, psp);
        seedSequenceTasks(em, siCards, psp, new String[]{
                "Activate Debit Card Feature in Employer Screen",
                "Assign Debit Card to Appropriate Benefit Plans",
                "Establish Card Setup in Reimbursement Settings"
        }, new Long[]{null, null, null});
    }

    // ═══════════════════════════════════════════════════════════════
    //  3H — RENEWAL SEQUENCES
    // ═══════════════════════════════════════════════════════════════

    private static void seedRenewalSequences(EntityManager em, PSP psp) {
        // HRA Renewal Sequence — PlanType id=3, find its ServiceItem
        PlanType ptHra = em.find(PlanType.class, 3);
        if (ptHra != null && ptHra.getServiceItem() != null) {
            ServiceItem siHraRenewal = ptHra.getServiceItem();
            siHraRenewal = DatabaseInitializer.setHasRequiredTasks(em, siHraRenewal, true);
            DatabaseInitializer.createRequiredTaskList(em, siHraRenewal, psp);
            seedSequenceTasks(em, siHraRenewal, psp, new String[]{
                    null,
                    "Contact Customer to Discuss HRA Changes, If Any",
                    "If Changing, Send Written Confirmation of Intended Changes",
                    "Create/Update HRA Benefit to New Plan Year",
                    "Request HRA Enrollments",
                    "Received HRA Enrollments",
                    "Load HRA Enrollments",
                    "Send Enrollment Confirmation Report",
                    null
            }, new Long[]{206L, null, null, null, null, null, null, null, 207L});
        }

        // COBRA Renewal Sequence — PlanType id=12 (Medical), find its ServiceItem
        PlanType ptMedical = em.find(PlanType.class, 12);
        if (ptMedical != null && ptMedical.getServiceItem() != null) {
            ServiceItem siCobraRenewal = ptMedical.getServiceItem();
            siCobraRenewal = DatabaseInitializer.setHasRequiredTasks(em, siCobraRenewal, true);
            DatabaseInitializer.createRequiredTaskList(em, siCobraRenewal, psp);
            seedSequenceTasks(em, siCobraRenewal, psp, new String[]{
                    null,
                    "Send COBRA Renewal Requirements and Questionnaire",
                    "Received COBRA Questionnaire",
                    "Load/Update Benefits in Summit",
                    "Send COBRA Renewal Complete Message"
            }, new Long[]{206L, null, null, null, null});
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  3I — TICKET CATEGORIES + SEQUENCES
    // ═══════════════════════════════════════════════════════════════

    private static void seedTicketCategories(EntityManager em, PSP psp) {
        ActivityCategory ticketCat = EntityLookup.getTemplateGroupById(em, 3);

        // Online Access
        TicketCategory tcOnline = DatabaseInitializer.createTicketCategory(em, 2L, "Online Access", "ONLINE");
        ServiceItem siOnline = DatabaseInitializer.createServiceItem(em, 20, "Can't Get Online", 20, ticketCat, psp);
        siOnline = DatabaseInitializer.setHasRequiredTasks(em, siOnline, true);
        DatabaseInitializer.setTicketCategoryOnServiceItem(em, siOnline, tcOnline);
        DatabaseInitializer.createRequiredTaskList(em, siOnline, psp);
        seedSequenceTasks(em, siOnline, psp, new String[]{
                "Acknowledge Receipt of Request",
                "Verify Participant Identity",
                "Attempt Portal Access Reset",
                "Confirm Access Restored with Participant",
                "Close Ticket"
        }, new Long[]{null, null, null, null, null});

        // Banking
        TicketCategory tcBank = DatabaseInitializer.createTicketCategory(em, 3L, "Banking", "BANK");
        ServiceItem siBank = DatabaseInitializer.createServiceItem(em, 30, "Update Bank Account", 30, ticketCat, psp);
        siBank = DatabaseInitializer.setHasRequiredTasks(em, siBank, true);
        DatabaseInitializer.setTicketCategoryOnServiceItem(em, siBank, tcBank);
        DatabaseInitializer.createRequiredTaskList(em, siBank, psp);
        seedSequenceTasks(em, siBank, psp, new String[]{
                "Acknowledge Receipt of Request",
                "Verify Participant Identity",
                "Collect New Banking Information",
                "Submit Banking Update to System",
                "Confirm Update Completed with Participant",
                "Close Ticket"
        }, new Long[]{null, null, null, null, null, null});
    }

    // ═══════════════════════════════════════════════════════════════
    //  3J — SUMMIT EMPLOYERS + EMPLOYEES + BENEFITS
    // ═══════════════════════════════════════════════════════════════

    private static void seedSummitData(EntityManager em, PSP psp) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonthFirst = today.plusMonths(1).withDayOfMonth(1);

        // ── Employer 1: Meridian Group Benefits ──
        Employer meridian = seedEmployer(em, -200, "Meridian Group Benefits", "hr@meridiangroup.com", "Patricia Walsh", -200);
        Employee eeWalsh = seedEmployee(em, -200, "Patricia", "Walsh", meridian, "patricia.walsh@meridiangroup.com");
        seedEmployee(em, -201, "Brian", "Kowalski", meridian, "b.kowalski@meridiangroup.com");
        seedEmployee(em, -202, "Angela", "Torres", meridian, "a.torres@meridiangroup.com");
        seedEmployee(em, -203, "Steven", "Park", meridian, "s.park@meridiangroup.com");
        seedEmployee(em, -204, "Linda", "Osei", meridian, "l.osei@meridiangroup.com");
        addContactToEmployer(em, meridian, eeWalsh);

        // Meridian Person for contacts
        Address meridianAddr = seedAddress(em, "2400 Commerce Center Dr", "Suite 310", "Kalamazoo", "MI", "49002");
        Person pWalsh = seedPerson(em, "Patricia", "Walsh", "HR Director",
                "patricia.walsh@meridiangroup.com", "269-555-0140", meridianAddr, psp);
        linkPersonToEmployee(em, pWalsh, eeWalsh);

        // Meridian Benefits
        PlanType ptHra = em.find(PlanType.class, 3);
        PlanType ptMedical = em.find(PlanType.class, 12);

        Date hraRenewalDue = Date.valueOf(nextMonthFirst);
        Date hraEffective = Date.valueOf(nextMonthFirst.minusYears(1));
        Benefit bMeridianHra = seedBenefit(em, -200, "CDH", "Meridian HRA", meridian, ptHra, hraRenewalDue, hraEffective);

        Date cobraRenewalDue = Date.valueOf(nextMonthFirst.plusMonths(6));
        Date cobraEffective = Date.valueOf(nextMonthFirst.plusMonths(6).minusYears(1));
        Benefit bMeridianCobra = seedBenefit(em, -201, "COBRA", "Meridian COBRA", meridian, ptMedical, cobraRenewalDue, cobraEffective);

        // ── Employer 2: Riverstone Administrative Services ──
        Employer riverstone = seedEmployer(em, -201, "Riverstone Administrative Services", "admin@riverstoneadmin.com", "Douglas Fenn", -201);
        Employee eeFenn = seedEmployee(em, -205, "Douglas", "Fenn", riverstone, "d.fenn@riverstoneadmin.com");
        seedEmployee(em, -206, "Cynthia", "Bell", riverstone, "c.bell@riverstoneadmin.com");
        seedEmployee(em, -207, "Marcus", "Webb", riverstone, "m.webb@riverstoneadmin.com");
        seedEmployee(em, -208, "Theresa", "Nguyen", riverstone, "t.nguyen@riverstoneadmin.com");
        addContactToEmployer(em, riverstone, eeFenn);

        Address riverstoneAddr = seedAddress(em, "1100 Maple Avenue", "", "Traverse City", "MI", "49684");
        Person pFenn = seedPerson(em, "Douglas", "Fenn", "Office Manager",
                "d.fenn@riverstoneadmin.com", "231-555-0267", riverstoneAddr, psp);
        linkPersonToEmployee(em, pFenn, eeFenn);

        // Riverstone Benefit — past-due COBRA
        LocalDate rvCobraRenewalDate = today.withDayOfMonth(1).minusMonths(1);
        Date rvCobraRenewalDue = Date.valueOf(rvCobraRenewalDate);
        Date rvCobraEffective = Date.valueOf(rvCobraRenewalDate.minusYears(1));
        Benefit bRiverstoneCobra = seedBenefit(em, -202, "COBRA", "Riverstone COBRA", riverstone, ptMedical, rvCobraRenewalDue, rvCobraEffective);
    }

    // ═══════════════════════════════════════════════════════════════
    //  3K — RENEWAL HISTORY
    // ═══════════════════════════════════════════════════════════════

    private static void seedRenewalHistory(EntityManager em, PSP psp) {
        Person adminPerson = EntityLookup.getPersonById(em, 104L);
        Employer meridian = EntityLookup.getEmployerById(em, -200, true);
        Employer riverstone = EntityLookup.getEmployerById(em, -201, true);

        Benefit bMeridianHra = EntityLookup.getBenefitBySummitKey(em, "CDH", -200);
        Benefit bMeridianCobra = EntityLookup.getBenefitBySummitKey(em, "COBRA", -201);
        Benefit bRiverstoneCobra = EntityLookup.getBenefitBySummitKey(em, "COBRA", -202);

        // 1. Meridian HRA Renewal (past, complete)
        if (bMeridianHra != null) {
            Date hraDue = bMeridianHra.getNextRenewalDue();
            Date pastHraDue = Date.valueOf(hraDue.toLocalDate().minusYears(1));
            seedPastRenewal(em, "Meridian Group Benefits — HRA Renewal", meridian, bMeridianHra,
                    pastHraDue, adminPerson, psp, getHraRenewalSteps());
        }

        // 2. Meridian COBRA Renewal (past, complete)
        if (bMeridianCobra != null) {
            Date cobraDue = bMeridianCobra.getNextRenewalDue();
            Date pastCobraDue = Date.valueOf(cobraDue.toLocalDate().minusYears(1));
            seedPastRenewal(em, "Meridian Group Benefits — COBRA Renewal", meridian, bMeridianCobra,
                    pastCobraDue, adminPerson, psp, getCobraRenewalSteps());
        }

        // 3. Riverstone COBRA Renewal (past, complete)
        if (bRiverstoneCobra != null) {
            Date rvCobraDue = bRiverstoneCobra.getNextRenewalDue();
            Date pastRvCobraDue = Date.valueOf(rvCobraDue.toLocalDate().minusYears(1));
            seedPastRenewal(em, "Riverstone Administrative Services — COBRA Renewal", riverstone, bRiverstoneCobra,
                    pastRvCobraDue, adminPerson, psp, getCobraRenewalSteps());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  3L — STANDALONE CHECKLISTS
    // ═══════════════════════════════════════════════════════════════

    private static void seedStandaloneChecklists(EntityManager em, PSP psp) {
        Person adminPerson = EntityLookup.getPersonById(em, 104L);
        LocalDate today = LocalDate.now();

        // Single-task reminders
        seedStandaloneChecklist(em, "Wish Bob a Happy 50th!", Date.valueOf(today.plusDays(3)),
                adminPerson, psp, new String[]{"Call Bob and wish him a happy 50th birthday"});

        seedStandaloneChecklist(em, "Submit Q2 Compliance Report", Date.valueOf(today.plusDays(14)),
                adminPerson, psp, new String[]{"Submit Q2 compliance summary to carrier portal"});

        seedStandaloneChecklist(em, "Follow Up with Demo Agency", Date.valueOf(today.minusDays(3)),
                adminPerson, psp, new String[]{"Follow up with Demo Agency on pending applications"});

        seedStandaloneChecklist(em, "Review Debit Card Enrollment Numbers", Date.valueOf(today.minusDays(7)),
                adminPerson, psp, new String[]{"Pull and review debit card enrollment counts for FSA clients"});

        // Multi-task: Run Payroll
        seedStandaloneChecklist(em, "Run Payroll", Date.valueOf(today),
                adminPerson, psp, new String[]{
                        "Confirm hours submitted by all employees",
                        "Review deductions and adjustments",
                        "Submit payroll to processor",
                        "Confirm payroll confirmation received",
                        "File payroll records"
                });

        // Multi-task: Month-End Reconciliation
        seedStandaloneChecklist(em, "Month-End Reconciliation", Date.valueOf(today.plusDays(7)),
                adminPerson, psp, new String[]{
                        "Pull monthly billing report",
                        "Reconcile participant counts",
                        "Verify FSA/HRA disbursements",
                        "Submit reconciliation summary"
                });

        // Multi-task: New Client Onboarding Review
        seedStandaloneChecklist(em, "New Client Onboarding Review", Date.valueOf(today.minusDays(14)),
                adminPerson, psp, new String[]{
                        "Confirm signed service agreement on file",
                        "Verify billing setup complete",
                        "Confirm Summit employer record created",
                        "Send welcome communication"
                });
    }

    // ═══════════════════════════════════════════════════════════════
    //  3M — TICKETS
    // ═══════════════════════════════════════════════════════════════

    private static void seedTickets(EntityManager em, PSP psp) {
        Person adminPerson = EntityLookup.getPersonById(em, 104L);
        LocalDate today = LocalDate.now();

        ContactMethod cmPhone = em.find(ContactMethod.class, 1);
        ContactMethod cmEmail = em.find(ContactMethod.class, 2);
        ServiceItem siBanking = EntityLookup.getServiceItemById(em, 30, true);
        ServiceItem siGeneral = EntityLookup.getServiceItemById(em, 10, true);

        // Find Patricia Walsh person
        Person pWalsh = findPersonByEmail(em, "patricia.walsh@meridiangroup.com");

        // Ticket 1 — Update Bank Account (pre-seeded employee)
        Ticket t1 = seedTicket(em, "Meridian Group — Update Bank Account",
                "Participant Patricia Walsh requesting update to direct deposit banking information on file.",
                pWalsh, adminPerson, adminPerson, cmEmail, siBanking,
                Date.valueOf(today.plusDays(5)), false, psp, null);

        // Mark first 2 ToDos complete
        markFirstNTodosComplete(em, t1, 2);

        // Add notes to ticket 1
        ReasonCreated rcEmail = em.find(ReasonCreated.class, 4);
        ReasonCreated rcCall = em.find(ReasonCreated.class, 2);
        seedNote(em, t1, adminPerson, rcEmail,
                "Received email from Patricia Walsh requesting banking update. She has a new checking account at First National Bank.");
        seedNote(em, t1, adminPerson, rcCall,
                "Called Patricia to confirm identity and collect new routing and account numbers. Will process update today.");

        // Ticket 2 — Freeform (person not in DB)
        Person robertPerson = seedPerson(em, "Robert", "Hartley", "Plan Participant",
                "r.hartley@personal.com", "555-867-5309", null, psp);

        Ticket t2 = seedTicket(em, "Robert Hartley — Reimbursement Status Inquiry",
                "Participant requesting status update on FSA reimbursement claim submitted approximately 3 weeks ago. Has not received payment or denial notice.",
                robertPerson, adminPerson, adminPerson, cmPhone, siGeneral,
                Date.valueOf(today.plusDays(2)), false, psp, new String[]{
                        "Review claim status in system",
                        "Follow up with participant on findings"
                });

        // Add notes to ticket 2
        ReasonCreated rcInternal = em.find(ReasonCreated.class, 1);
        seedNote(em, t2, adminPerson, rcCall,
                "Robert called regarding FSA claim submitted on 2/10. Claim ID unknown — will need to look up by SSN/name.");
        seedNote(em, t2, adminPerson, rcInternal,
                "Located claim in system — pending secondary review. Expected 5–7 business days. Will notify participant.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  3N — OPPORTUNITY + PROPOSAL + APPLICATION
    // ═══════════════════════════════════════════════════════════════

    private static void seedOpportunity(EntityManager em, PSP psp) {
        Person adminPerson = EntityLookup.getPersonById(em, 104L);
        LocalDate today = LocalDate.now();

        // Create Greg Hartfield person
        Address hartfieldAddr = seedAddress(em, "1800 Forge Drive", "", "Marquette", "MI", "49855");
        Person gregPerson = seedPerson(em, "Greg", "Hartfield", "Owner",
                "g.hartfield@hartfieldmfg.com", "906-555-0199", hartfieldAddr, psp);

        // Create Prospect
        Prospect prospect = seedProspect(em, "Hartfield Manufacturing LLC", gregPerson, hartfieldAddr, adminPerson);

        // Create Opportunity
        Agency homeAgency = EntityLookup.getAgencyById(em, 14L);
        em.getTransaction().begin();
        Opportunity opp = new Opportunity();
        opp.setFullName("Hartfield Manufacturing LLC");
        opp.setProspect(prospect);
        opp.setAgency(homeAgency);
        opp.setLoggedBy(adminPerson);
        opp.setAssignedTo(adminPerson);
        opp.setPrimaryContact(gregPerson);
        opp.setManagedBy(adminPerson);
        opp.setDueDate(Date.valueOf(today.plusDays(30)));
        opp.setStage("WON");
        opp.setEstimatedEmployees(47);
        opp.setComplete(false);
        em.persist(opp);
        em.getTransaction().commit();

        // Create checklist for opportunity
        CheckList oppCl = createChecklistForActivity(em, opp, psp, new String[]{
                "Review prospect information and requirements",
                "Prepare and send proposal",
                "Follow up on application status",
                "Complete onboarding upon approval"
        }, false);
        // Re-find the Opportunity to avoid SINGLE_TABLE merge descriptor confusion
        em.getTransaction().begin();
        Opportunity managedOpp = em.find(Opportunity.class, opp.getId());
        managedOpp.setCheckList(oppCl);
        em.getTransaction().commit();

        // Create Proposal
        Rate standardRate = EntityLookup.getRateById(em, 1L, true);
        LOS fsaLos = em.find(LOS.class, 2L);

        em.getTransaction().begin();
        Proposal proposal = new Proposal();
        proposal.setProspect(prospect);
        proposal.setRate(standardRate);
        proposal.setApplicationGUID(UUID.randomUUID().toString());
        proposal.setStatus("APPLIED");
        proposal.setCreatedBy(adminPerson);
        proposal.setSourceActivity(opp);
        proposal.setLosList(new ArrayList<>());
        proposal.getLosList().add(fsaLos);
        em.persist(proposal);
        em.getTransaction().commit();

        // Create Application
        em.getTransaction().begin();
        Application app = new Application();
        app.setProposal(proposal);
        app.setStatus("SUBMITTED");
        app.setDateStarted(new Timestamp(System.currentTimeMillis()));
        app.setDateSubmitted(new Timestamp(System.currentTimeMillis()));
        em.persist(app);
        em.getTransaction().commit();

        // Populate application field values
        seedApplicationFieldValue(em, app, "company_legal_name", "Hartfield Manufacturing LLC");
        seedApplicationFieldValue(em, app, "company_ein", "38-1234567");
        seedApplicationFieldValue(em, app, "company_employees", "47");
        seedApplicationFieldValue(em, app, "company_phone", "906-555-0199");
        seedApplicationFieldValue(em, app, "contact_name", "Greg Hartfield");
        seedApplicationFieldValue(em, app, "contact_email", "g.hartfield@hartfieldmfg.com");
        seedApplicationFieldValue(em, app, "contact_phone", "906-555-0199");
        seedApplicationFieldValue(em, app, "address_street1", "1800 Forge Drive");
        seedApplicationFieldValue(em, app, "address_city", "Marquette");
        seedApplicationFieldValue(em, app, "address_state", "MI");
        seedApplicationFieldValue(em, app, "address_zip", "49855");
    }

    // ═══════════════════════════════════════════════════════════════
    //  3O — PROPOSAL DEFAULTS (Title + Closing HTML)
    // ═══════════════════════════════════════════════════════════════
    //  3P — BPO PARTNERSHIP
    // ═══════════════════════════════════════════════════════════════

    private static void seedBpoPartnership(EntityManager em, PSP psp) {
        // Check if a BPO registration already exists
        List<BpoRegistration> existing = em.createQuery(
                "SELECT b FROM BpoRegistration b WHERE b.psp.id = :pspId", BpoRegistration.class)
                .setParameter("pspId", psp.getId())
                .getResultList();
        if (!existing.isEmpty()) return;

        em.getTransaction().begin();
        BpoRegistration bpoReg = new BpoRegistration();
        bpoReg.setPsp(psp);
        bpoReg.setBpoName("SSA BPO Services");
        bpoReg.setBpoUrl("https://bpo.superiorstate.biz");
        bpoReg.setActive(true);
        bpoReg.setApproved(true);
        bpoReg.setRequested(true);
        bpoReg.setAccepted(true);
        em.persist(bpoReg);
        em.getTransaction().commit();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLOSE INITIALIZATION CHECKLIST
    // ═══════════════════════════════════════════════════════════════

    private static void closeInitializationChecklist(EntityManager em) {
        CheckList cl = em.find(CheckList.class, 29L);
        if (cl != null && !cl.isComplete()) {
            em.getTransaction().begin();
            cl.setComplete(true);
            cl.setDateCompleted(Date.valueOf(LocalDate.now().minusDays(1)));
            cl.setCompletedBy(cl.getLoggedBy());
            em.merge(cl);
            em.getTransaction().commit();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private static Address seedAddress(EntityManager em, String ad1, String ad2,
                                        String city, String state, String zip) {
        em.getTransaction().begin();
        Address a = new Address();
        a.setAddress1(ad1);
        a.setAddress2(ad2);
        a.setCity(city);
        a.setState(state);
        a.setZipCode(zip);
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }

    private static Person seedPerson(EntityManager em, String firstName, String lastName,
                                      String title, String email, String phone,
                                      Address address, PSP psp) {
        em.getTransaction().begin();
        Person p = new Person();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setFullName(firstName + " " + lastName);
        p.setTitle(title);
        p.setEmail(email);
        p.setPhone(phone);
        p.setAddress(address);
        p.setPsp(psp);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private static Prospect seedProspect(EntityManager em, String name, Person contact,
                                          Address address, Person agent) {
        em.getTransaction().begin();
        Prospect pr = new Prospect();
        pr.setName(name);
        pr.setContact(contact);
        pr.setAddress(address);
        pr.setAgent(agent);
        em.persist(pr);
        em.getTransaction().commit();
        return pr;
    }

    private static Employer seedEmployer(EntityManager em, int id, String name,
                                          String email, String contactName, int erKey) {
        Employer existing = EntityLookup.getEmployerById(em, id, true);
        if (existing != null) return existing;

        em.getTransaction().begin();
        Employer er = new Employer();
        er.setId(id);
        er.setEmployerName(name);
        er.setActive(true);
        er.setEmail(email);
        er.setContactName(contactName);
        er.setErKey(erKey);
        er.setAltId(id);
        er.setBillable(true);
        er.setHasPop(true);
        er.setHasPb(true);
        er.setHasCdh(true);
        em.persist(er);
        em.getTransaction().commit();
        return er;
    }

    private static Employee seedEmployee(EntityManager em, int id, String firstName,
                                          String lastName, Employer employer, String email) {
        Employee existing = EntityLookup.getEmployeeById(em, id, true);
        if (existing != null) return existing;

        em.getTransaction().begin();
        Employee ee = new Employee();
        ee.setId(id);
        ee.setFirstName(firstName);
        ee.setLastName(lastName);
        ee.setMmKey(id);
        ee.setActive(true);
        ee.setEmployer(employer);
        ee.setEmail(email);
        em.persist(ee);
        em.getTransaction().commit();
        return ee;
    }

    private static void addContactToEmployer(EntityManager em, Employer employer, Employee contact) {
        em.getTransaction().begin();
        employer.addContact(contact);
        em.merge(employer);
        em.getTransaction().commit();
    }

    private static void linkPersonToEmployee(EntityManager em, Person person, Employee employee) {
        em.getTransaction().begin();
        person.setEmployee(employee);
        em.merge(person);
        em.getTransaction().commit();
    }

    private static Benefit seedBenefit(EntityManager em, int summitId, String sourceType,
                                        String name, Employer employer, PlanType planType,
                                        Date nextRenewalDue, Date effectiveDate) {
        Benefit existing = EntityLookup.getBenefitBySummitKey(em, sourceType, summitId);
        if (existing != null) return existing;

        em.getTransaction().begin();
        Benefit b = new Benefit();
        b.setSummitId(summitId);
        b.setSourceType(sourceType);
        b.setPlanName(name);
        b.setPlanDescription(name);
        b.setEmployer(employer);
        b.setPlanType(planType);
        b.setEffectiveDate(effectiveDate);
        b.setNextRenewalDue(nextRenewalDue);
        b.setActive(true);
        b.setPbBenId(0);
        em.persist(b);
        em.getTransaction().commit();
        return b;
    }

    private static ResourceCategory seedResourceCategory(EntityManager em, String name,
                                                          String iconClass, int sortOrder, PSP psp) {
        em.getTransaction().begin();
        ResourceCategory rc = new ResourceCategory();
        rc.setName(name);
        rc.setIconClass(iconClass);
        rc.setSortOrder(sortOrder);
        rc.setPsp(psp);
        em.persist(rc);
        em.getTransaction().commit();
        return rc;
    }

    private static MarketingMaterial seedMarketingMaterial(EntityManager em, String title,
                                                            String description, String materialType,
                                                            String url, ResourceCategory category,
                                                            int sortOrder, PSP psp) {
        em.getTransaction().begin();
        MarketingMaterial mm = new MarketingMaterial();
        mm.setTitle(title);
        mm.setDescription(description);
        mm.setMaterialType(materialType);
        mm.setUrl(url);
        mm.setCategory(category);
        mm.setSortOrder(sortOrder);
        mm.setPsp(psp);
        em.persist(mm);
        em.getTransaction().commit();
        return mm;
    }

    private static void seedFeature(EntityManager em, ServiceModule sm, String headline,
                                     String description, int sortOrder, PSP psp,
                                     MarketingMaterial resource) {
        em.getTransaction().begin();
        Feature f = new Feature();
        f.setServiceModule(sm);
        f.setHeadline(headline);
        f.setDescription(description);
        f.setSortOrder(sortOrder);
        f.setPsp(psp);
        f.setLibraryResource(resource);
        em.persist(f);
        em.getTransaction().commit();
    }

    private static void createReusableTask(EntityManager em, Long id, String description,
                                            PSP psp, Person owner) {
        if (EntityLookup.getTaskById(em, id) != null) return;
        em.getTransaction().begin();
        Task t = new Task();
        t.setId(id);
        t.setDescription(description);
        t.setReUsable(true);
        t.setPsp(psp);
        t.setOwner(owner);
        t.setHasOwner(true);
        t.setHasAutomation(false);
        t.setHasGoTo(false);
        t.setHasInfo(false);
        t.setSourced(false);
        t.setAllowNonOwner(true);
        t.setAllowEarly(true);
        t.setAllowFuture(true);
        em.persist(t);
        em.getTransaction().commit();
    }

    /**
     * Seeds tasks into a RequiredTaskList for a ServiceItem.
     * Uses TaskSequenceTable (join entity) to link tasks to the sequence.
     * taskDescriptions and reusableTaskIds must be same length.
     * If reusableTaskIds[i] is non-null, uses the existing reusable task.
     * If null, creates a new non-reusable task with taskDescriptions[i].
     */
    private static void seedSequenceTasks(EntityManager em, ServiceItem si, PSP psp,
                                           String[] taskDescriptions, Long[] reusableTaskIds) {
        RequiredTaskList rtl = findRequiredTaskList(em, si);
        if (rtl == null) return;

        int sortOrder = 10;
        for (int i = 0; i < taskDescriptions.length; i++) {
            Task task;
            if (reusableTaskIds[i] != null) {
                task = EntityLookup.getTaskById(em, reusableTaskIds[i]);
            } else {
                em.getTransaction().begin();
                task = new Task();
                task.setDescription(taskDescriptions[i]);
                task.setReUsable(false);
                task.setPsp(psp);
                task.setHasOwner(false);
                task.setSourced(false);
                task.setAllowNonOwner(true);
                task.setHasAutomation(false);
                task.setHasGoTo(false);
                task.setHasInfo(false);
                task.setAllowEarly(true);
                task.setAllowFuture(true);
                em.persist(task);
                em.getTransaction().commit();
            }

            if (task != null) {
                em.getTransaction().begin();
                TaskSequenceID tsId = new TaskSequenceID();
                tsId.setTaskId(task.getId());
                tsId.setTaskSequenceId(rtl.getId());
                TaskSequenceTable tst = new TaskSequenceTable();
                tst.setTaskSequenceID(tsId);
                tst.setTask(task);
                tst.setTaskSequence(rtl);
                tst.setSortOrder(sortOrder);
                em.persist(tst);
                em.getTransaction().commit();
            }

            sortOrder += 10;
        }
    }

    private static void seedPastRenewal(EntityManager em, String name, Employer employer,
                                         Benefit benefit, Date dueDate, Person admin,
                                         PSP psp, String[] steps) {
        em.getTransaction().begin();
        Renewal r = new Renewal();
        r.setFullName(name);
        r.setEmployer(employer);
        r.setLoggedBy(admin);
        r.setAssignedTo(admin);
        r.setPrimaryContact(admin);
        r.setDueDate(dueDate);
        r.setComplete(true);
        r.setDateCompleted(dueDate);
        r.setCompletedBy(admin);
        em.persist(r);
        em.getTransaction().commit();

        // Create RenewalItem
        em.getTransaction().begin();
        RenewalItem ri = new RenewalItem();
        ri.setBenefit(benefit);
        ri.setRenewal(r);
        ri.setDateFor(dueDate);
        em.persist(ri);
        em.getTransaction().commit();

        // Create CheckList with all steps complete
        CheckList cl = createChecklistForActivity(em, r, psp, steps, true);
        em.getTransaction().begin();
        r.setCheckList(cl);
        em.merge(r);
        em.getTransaction().commit();
    }

    private static void seedStandaloneChecklist(EntityManager em, String name, Date dueDate,
                                                 Person assignedTo, PSP psp, String[] steps) {
        em.getTransaction().begin();
        CheckList cl = new CheckList();
        cl.setFullName(name);
        cl.setDueDate(dueDate);
        cl.setAssignedTo(assignedTo);
        cl.setLoggedBy(assignedTo);
        cl.setComplete(false);
        em.persist(cl);
        em.getTransaction().commit();

        int sortOrder = 10;
        for (String desc : steps) {
            em.getTransaction().begin();
            Task task = new Task();
            task.setReUsable(false);
            task.setDescription(desc);
            task.setHasOwner(false);
            task.setSourced(false);
            task.setAllowNonOwner(true);
            task.setHasAutomation(false);
            task.setHasGoTo(false);
            task.setHasInfo(false);
            task.setAllowEarly(true);
            task.setAllowFuture(true);
            task.setPsp(psp);
            em.persist(task);
            em.getTransaction().commit();

            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setComplete(false);
            toDo.setCheckList(cl);
            toDo.setTask(task);
            toDo.setSortOrder(sortOrder);
            em.persist(toDo);
            em.getTransaction().commit();

            sortOrder += 10;
        }
    }

    private static Ticket seedTicket(EntityManager em, String name, String description,
                                      Person contact, Person loggedBy, Person assignedTo,
                                      ContactMethod contactMethod, ServiceItem serviceItem,
                                      Date dueDate, boolean isComplete, PSP psp,
                                      String[] customSteps) {
        em.getTransaction().begin();
        Ticket t = new Ticket();
        t.setFullName(name);
        t.setDescription(description);
        t.setContact(contact);
        t.setLoggedBy(loggedBy);
        t.setAssignedTo(assignedTo);
        t.setPrimaryContact(contact);
        t.setContactMethod(contactMethod);
        t.setTicketServiceItem(serviceItem);
        t.setDueDate(dueDate);
        t.setComplete(isComplete);
        if (isComplete) {
            t.setDateCompleted(dueDate);
            t.setCompletedBy(assignedTo);
        }
        em.persist(t);
        em.getTransaction().commit();

        // Create checklist — use custom steps if provided, otherwise defaults
        String[] steps = customSteps != null ? customSteps : new String[]{
                "Review request details and verify contact",
                "Research issue and gather documentation",
                "Resolve issue or escalate as needed",
                "Confirm resolution with contact"
        };
        CheckList cl = createChecklistForActivity(em, t, psp, steps, false);

        // Re-find the Ticket to avoid SINGLE_TABLE merge descriptor confusion
        em.getTransaction().begin();
        Ticket managedTicket = em.find(Ticket.class, t.getId());
        managedTicket.setCheckList(cl);
        em.getTransaction().commit();

        return managedTicket;
    }

    private static CheckList createChecklistForActivity(EntityManager em, Activity activity,
                                                         PSP psp, String[] taskDescriptions,
                                                         boolean allComplete) {
        em.getTransaction().begin();
        CheckList cl = new CheckList();
        cl.setAssignedTo(activity);
        cl.setDueDate(activity.getDueDate());
        cl.setFullName(activity.getFullName());
        cl.setLoggedBy(activity.getLoggedBy());
        cl.setComplete(allComplete);
        if (allComplete) {
            cl.setDateCompleted(activity.getDueDate());
            cl.setCompletedBy(activity.getLoggedBy());
        }
        em.persist(cl);
        em.getTransaction().commit();

        int sortOrder = 10;
        for (String desc : taskDescriptions) {
            em.getTransaction().begin();
            Task task = new Task();
            task.setReUsable(false);
            task.setDescription(desc);
            task.setHasOwner(false);
            task.setSourced(false);
            task.setAllowNonOwner(true);
            task.setHasAutomation(false);
            task.setHasGoTo(false);
            task.setHasInfo(false);
            task.setAllowEarly(true);
            task.setAllowFuture(true);
            task.setPsp(psp);
            em.persist(task);
            em.getTransaction().commit();

            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setComplete(allComplete);
            toDo.setCheckList(cl);
            toDo.setTask(task);
            toDo.setSortOrder(sortOrder);
            em.persist(toDo);
            em.getTransaction().commit();

            sortOrder += 10;
        }

        return cl;
    }

    private static void markFirstNTodosComplete(EntityManager em, Ticket ticket, int n) {
        if (ticket.getCheckList() == null) return;
        CheckList cl = em.find(CheckList.class, ticket.getCheckList().getId());
        if (cl == null || cl.getToDoList() == null) return;

        // Sort by sortOrder and mark first N complete
        List<ToDo> todos = em.createQuery(
                "SELECT td FROM ToDo td WHERE td.checkList.id = :clId ORDER BY td.sortOrder",
                ToDo.class)
                .setParameter("clId", cl.getId())
                .getResultList();

        int count = 0;
        for (ToDo td : todos) {
            if (count >= n) break;
            em.getTransaction().begin();
            td.setComplete(true);
            em.merge(td);
            em.getTransaction().commit();
            count++;
        }
    }

    private static void seedNote(EntityManager em, Activity activity, Person createdBy,
                                  ReasonCreated reason, String detail) {
        em.getTransaction().begin();
        Note note = new Note();
        note.setActivity(activity);
        note.setCreatedBy(createdBy);
        note.setReasonCreated(reason);
        note.setDetail(detail);
        note.setDateGenerated(Date.valueOf(LocalDate.now()));
        em.persist(note);
        em.getTransaction().commit();
    }

    private static void seedApplicationFieldValue(EntityManager em, Application app,
                                                    String fieldKey, String value) {
        ApplicationField field = em.find(ApplicationField.class, fieldKey);
        if (field == null) {
            System.out.println("    ⚠ ApplicationField not found: " + fieldKey);
            return;
        }

        em.getTransaction().begin();
        ApplicationFieldValue fv = new ApplicationFieldValue();
        fv.setApplication(app);
        fv.setApplicationField(field);
        fv.setFieldValue(value);
        em.persist(fv);
        em.getTransaction().commit();
        System.out.println("    ✓ Field value seeded: " + fieldKey + " = " + value);
    }

    private static void assignRateTable(EntityManager em, Rate rate, ServiceModule sm,
                                         PriceItem pi, double price, int sortOrder) {
        em.getTransaction().begin();
        RateTable rt = new RateTable();
        RateTableID rtId = new RateTableID();
        rtId.setRateId(rate.getId());
        rtId.setModuleId(sm.getId());
        rtId.setPriceItemId(pi.getId());
        rt.setRateTableID(rtId);
        rt.setRate(rate);
        rt.setModule(sm);
        rt.setPriceItem(pi);
        rt.setPrice(price);
        rt.setSortOrder(sortOrder);
        em.persist(rt);
        em.getTransaction().commit();
    }

    private static Person findPersonByEmail(EntityManager em, String email) {
        try {
            return em.createQuery("SELECT p FROM Person p WHERE p.email = :email", Person.class)
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private static MarketingMaterial findMaterialByTitle(EntityManager em, String title, PSP psp) {
        try {
            return em.createQuery(
                    "SELECT m FROM MarketingMaterial m WHERE m.title = :title AND m.psp.id = :pspId",
                    MarketingMaterial.class)
                    .setParameter("title", title)
                    .setParameter("pspId", psp.getId())
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private static RequiredTaskList findRequiredTaskList(EntityManager em, ServiceItem si) {
        try {
            return em.createQuery(
                    "SELECT r FROM RequiredTaskList r WHERE r.serviceItem.id = :siId",
                    RequiredTaskList.class)
                    .setParameter("siId", si.getId())
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENEWAL STEP DEFINITIONS
    // ═══════════════════════════════════════════════════════════════

    private static String[] getHraRenewalSteps() {
        return new String[]{
                "Send Welcome to Renewal",
                "Contact Customer to Discuss HRA Changes, If Any",
                "If Changing, Send Written Confirmation of Intended Changes",
                "Create/Update HRA Benefit to New Plan Year",
                "Request HRA Enrollments",
                "Received HRA Enrollments",
                "Load HRA Enrollments",
                "Send Enrollment Confirmation Report",
                "Send Renewal Completion Message"
        };
    }

    private static String[] getCobraRenewalSteps() {
        return new String[]{
                "Send Welcome to Renewal",
                "Send COBRA Renewal Requirements and Questionnaire",
                "Received COBRA Questionnaire",
                "Load/Update Benefits in Summit",
                "Send COBRA Renewal Complete Message"
        };
    }

}
