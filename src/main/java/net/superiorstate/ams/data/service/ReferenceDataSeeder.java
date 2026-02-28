package net.superiorstate.ams.data.service;

import jakarta.persistence.*;
import net.superiorstate.ams.data.dao.SequenceDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskFrequency;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.note.ActivityStatus;
import net.superiorstate.ams.model.activity.note.ReasonCreated;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.PriceItem;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.agency.RateTable;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ModuleDetail;
import net.superiorstate.ams.model.sales.offering.ServiceModule;
import net.superiorstate.ams.model.summit.archive.PlanType;

public abstract class ReferenceDataSeeder {

    private static final String PERSISTENCE_UNIT_NAME = "default";

    private static Agency fillAgency(EntityManager em, PSP psp, Address address, Person contact){
        Agency agency = new Agency();
        agency.setAddress(address);
        agency.setName("Superior State Administrators, Inc.");
        agency.setPsp(psp);
        agency.setTaxId("38-3283073");
        agency.setPhone("906-863-4488");
        agency.setPrimaryContact(contact);
        em.persist(agency);
        return agency;
    }

    private static PlanType createSummitPlanType(EntityManager em, int typeId, String code, String name, int groupId, int tpId){
        ServiceItem tp = EntityLookup.getServiceItemById(em,tpId);
        BillingGroup bg = EntityLookup.getBillingGroupById(em,groupId);
        em.getTransaction().begin();
        PlanType PlanType = new PlanType();
        PlanType.setPlanTypeId(typeId);
        PlanType.setCode(code);
        PlanType.setPlanTypeName(name);
        PlanType.setBillingGroup(bg);
        PlanType.setServiceItem(tp);
        em.persist(PlanType);
        em.getTransaction().commit();
        return PlanType;
    }

    private static BillingGroup createBillingGroup(EntityManager em, int id, String description){

        em.getTransaction().begin();
        BillingGroup billingGroup = new BillingGroup();
        billingGroup.setId(id);
        billingGroup.setDescription(description);
        em.persist(billingGroup);
        em.getTransaction().commit();
        return billingGroup;
    }

    private static void fillBillingGroups(EntityManager em){
        try{
            createBillingGroup(em,1,"FSA");
            createBillingGroup(em,2,"HRA");
            createBillingGroup(em,3,"COBRA");
            createBillingGroup(em,4,"Transit");
            createBillingGroup(em,5,"HSA");
            createBillingGroup(em,6,"LSA");
            createBillingGroup(em,7,"DUAL");
        } catch (Exception e){
            e.printStackTrace();
        }

    }


    private static void fillPlanTypes(EntityManager em){
        try{
            createSummitPlanType(em,1,"DCA","Dependent Care Account",1,3);
            createSummitPlanType(em,2,"FSA", "Medical Flexible Spending Account",1,2);
            createSummitPlanType(em,3,"HRA","Health Reimbursement Arrangement",2,5);
            createSummitPlanType(em,4,"HSA","Health Savings Account",5,4);
            createSummitPlanType(em,5,"LFSA","Limited Purpose Medical FSA",1,2);
            createSummitPlanType(em,6,"MERP", "Medical Expense Reimbursement Plan",2,6);
            createSummitPlanType(em,7,"PRK","Parking Plan",4,23);
            createSummitPlanType(em,8,"TRN","Transportation Plan",4,23);
            createSummitPlanType(em,9,"Dental","Dental",3,7);
            createSummitPlanType(em,10,"EAP","EAP",3,7);
            createSummitPlanType(em,11,"Life","Life",3,7);
            createSummitPlanType(em,12,"Medical","Medical",3,7);
            createSummitPlanType(em,13,"Pharmacy","Pharmacy",3,7);
            createSummitPlanType(em,14,"Vision","Vision",3,7);
            createSummitPlanType(em,15,"NEFSA","NEFSA",3,9);
            createSummitPlanType(em,16,"LSA","Lifestyle Spending Account",6,23);
            createSummitPlanType(em,1001,"PRA", "Premium Reimbursement Account",1,2);
            createSummitPlanType(em,1002,"ICHRA","Individual Coverage HRA",2,5);
            createSummitPlanType(em,1003,"EBHRA", "Excepted Benefit HRA",2,5);
            createSummitPlanType(em,1004,"DRiP","Deductible Reimbursement Plan",2,6);
        } catch (Exception e){
            e.printStackTrace();
        }


    }
    private static Person fillPerson(EntityManager em, PSP psp, Address address) {
        Person person = new Person();
        person.setPsp(psp);
        person.setFirstName("Kevin");
        person.setLastName("Murphy");
        person.setAddress(address);
        person.setEmail("kevin@superiorstate.net");
        person.setMiddleInit("M");
        person.setTitle("President");
        person.setPhone("906-863-4499");
        em.persist(person);
        return person;
    }



    private static Address fillAddress(EntityManager em) {
        Address address = new Address();
        address.setAddress1("1101 11th Avenue");
        address.setAddress2("Suite B2");
        address.setCity("Menominee");
        address.setState("MI");
        address.setZipCode("49858");
        em.persist(address);
        return address;
    }

    private static PSP fillPSP(EntityManager em, Address address) {
        PSP psp = new PSP();
        psp.setFullName("Superior State Administrators, Inc.");
        psp.setTaxId("38-3283073");
        psp.setAddress(address);
        em.persist(psp);
        return psp;
    }
    private static void fillLOS(EntityManager em, PSP psp){
        LOS los1 = new LOS();
        los1.setPsp(psp);
        los1.setDescription("Premium Only Plan");
        los1.setShortText("POP");
        LOS los2 = new LOS();
        los2.setPsp(psp);
        los2.setShortText("FSA");
        los2.setDescription("Flexible Spending Accounts");
        LOS los3 = new LOS();
        los3.setPsp(psp);
        los3.setShortText("HRA/MERP");
        los3.setDescription("Health Reimbursement Arrangements");
        LOS los4 = new LOS();
        los4.setPsp(psp);
        los4.setShortText("COBRA");
        los4.setDescription("COBRA Administration");
        LOS los5 = new LOS();
        los5.setPsp(psp);
        los5.setShortText("HSA");
        los5.setDescription("Health Savings Accounts");
        LOS los6 = new LOS();
        los6.setPsp(psp);
        los6.setShortText("TRANSIT");
        los6.setDescription("Transit/Commuter Plans");

        em.persist(los1);
        em.persist(los2);
        em.persist(los3);
        em.persist(los4);
        em.persist(los5);
        em.persist(los6);
    }


   /* private static void fillLastDay(EntityManager em){
        LastDayOfCoverage ldoc1 = getLDOC(1,"End of Month");
        LastDayOfCoverage ldoc2 = getLDOC(2,"Date of Termination");
        LastDayOfCoverage ldoc3 = getLDOC(3,"14 Days from Termination");
        LastDayOfCoverage ldoc4 = getLDOC(4,"30 Days from Termination");
        LastDayOfCoverage ldoc5 = getLDOC(5,"14th of Each Month");
        LastDayOfCoverage ldoc6 = getLDOC(6,"15th of Each Month");
        LastDayOfCoverage ldoc7 = getLDOC(7,"20th of Each Month");
        LastDayOfCoverage ldoc8 = getLDOC(8,"15th or 31st");
        LastDayOfCoverage ldoc9 = getLDOC(9,"Date Before Termination");
        LastDayOfCoverage ldoc10 = getLDOC(10,"End of Next Month");
        LastDayOfCoverage ldoc11 = getLDOC(11,"End of Last Month If Before 16th");
        LastDayOfCoverage ldoc12 = getLDOC(12,"End of Last Month If Before 15th");
        LastDayOfCoverage ldoc13 = getLDOC(13,"Sunday");
        LastDayOfCoverage ldoc14 = getLDOC(14, "Determined by Payroll Schedule");

        em.persist(ldoc1);
        em.persist(ldoc2);
        em.persist(ldoc3);
        em.persist(ldoc4);
        em.persist(ldoc5);
        em.persist(ldoc6);
        em.persist(ldoc7);
        em.persist(ldoc8);
        em.persist(ldoc9);
        em.persist(ldoc10);
        em.persist(ldoc11);
        em.persist(ldoc12);
        em.persist(ldoc13);
        em.persist(ldoc14);
    }*/
    /*private static LastDayOfCoverage getLDOC(int id, String desc){
        LastDayOfCoverage ldoc = new LastDayOfCoverage();
        ldoc.setId(id);
        ldoc.setDescription(desc);
        ldoc.setSortOrder(id);
        return ldoc;
    }*/
    /*private static void fillInsuranceTypes(EntityManager em){
        InsuranceType it1 = getInsuranceType(1,"Medical Plan");
        InsuranceType it2 = getInsuranceType(2,"Dental Plan");
        InsuranceType it3 = getInsuranceType(3, "Vision Plan");
        InsuranceType it4 = getInsuranceType(4,"Prescription Drug Plan");
        InsuranceType it5 = getInsuranceType(5,"Health Flexible Spending Account");
        InsuranceType it6 = getInsuranceType(6,"Life Insurance");
        InsuranceType it7 = getInsuranceType(7,"Employee Assistance Plan");
        InsuranceType it8 = getInsuranceType(8,"Health Reimbursement Arrangement");
        InsuranceType it9 = getInsuranceType(9,"Non Excepted Health FSA");
        em.persist(it1);
        em.persist(it2);
        em.persist(it3);
        em.persist(it4);
        em.persist(it5);
        em.persist(it6);
        em.persist(it7);
        em.persist(it8);
        em.persist(it9);
    }*/
    /*private static InsuranceType getInsuranceType(int id, String desc){
        InsuranceType it = new InsuranceType();
        it.setId(id);
        it.setDescription(desc);
        return it;
    }

    private static void fillInsuranceRateType(EntityManager em){
        InsuranceRateType irt1 = getInsuranceRateType(1,"Flat Rate");
        InsuranceRateType irt2 = getInsuranceRateType(2,"Standard Tiered Rates");
        InsuranceRateType irt3 = getInsuranceRateType(3,"Individually Rated");
        InsuranceRateType irt4 = getInsuranceRateType(4,"Age Rated");
        InsuranceRateType irt5 = getInsuranceRateType(5,"Other");
        em.persist(irt1);
        em.persist(irt2);
        em.persist(irt3);
        em.persist(irt4);
        em.persist(irt5);
    }
    private static InsuranceRateType getInsuranceRateType(int id, String desc){
        InsuranceRateType irt = new InsuranceRateType();
        irt.setId(id);
        irt.setDescription(desc);
        return irt;
    }*/

    /*private static void fillBankAccountType(EntityManager em){
        BankAccountType bat1 = getBankAccountType(1,"Checking");
        BankAccountType bat2 = getBankAccountType(2,"Savings");
        BankAccountType bat3 = getBankAccountType(3, "Other");
        em.persist(bat1);
        em.persist(bat2);
        em.persist(bat3);
    }
    private static BankAccountType getBankAccountType(int id, String desc){
        BankAccountType bat = new BankAccountType();
        bat.setId(id);
        bat.setDescription(desc);
        return bat;
    }*/

    /*private static void fillFsaMode(EntityManager em){
        FsaMode fm1 = getFsaMode(1,"Spend Down");
        FsaMode fm2 = getFsaMode(2,"Carry Over");
        FsaMode fm3 = getFsaMode(3,"No Enhancement");
        em.persist(fm1);
        em.persist(fm2);
        em.persist(fm3);
    }
    private static FsaMode getFsaMode(int id, String desc){
        FsaMode fsaMode = new FsaMode();
        fsaMode.setId(id);
        fsaMode.setDescription(desc);
        return fsaMode;
    }*/

    private static void fillNoteReasons(EntityManager em){
        ReasonCreated nr1 = getNoteReason(1,"Internal Note",false);
        ReasonCreated nr2 = getNoteReason(2,"Received Call",false);
        ReasonCreated nr3 = getNoteReason(3,"Received Voicemail",false);
        ReasonCreated nr4 = getNoteReason(4,"Received Email",false);
        ReasonCreated nr5 = getNoteReason(5,"Made Call",true);
        ReasonCreated nr6 = getNoteReason(6,"Left Voicemail",true);
        ReasonCreated nr7 = getNoteReason(7,"Sent Email Message",true);
        ReasonCreated nr8 = getNoteReason(8,"Quick Action",true);
        em.persist(nr1);
        em.persist(nr2);
        em.persist(nr3);
        em.persist(nr4);
        em.persist(nr5);
        em.persist(nr6);
        em.persist(nr7);
        em.persist(nr8);
    }
    private static ReasonCreated getNoteReason(int id, String desc, boolean outbound){
        ReasonCreated noteReason = new ReasonCreated();
        noteReason.setId(id);
        noteReason.setDescription(desc);
        noteReason.setOutbound(outbound);
        return noteReason;
    }

    private static void fillActivityStatus(EntityManager em){
        ActivityStatus sc1 = getActivityStatus(1,"Waiting on Them");
        ActivityStatus sc2 = getActivityStatus(2,"No Change");
        ActivityStatus sc3 = getActivityStatus(3,"Waiting on Us");
        em.persist(sc1);
        em.persist(sc2);
        em.persist(sc3);
    }
    private static ActivityStatus getActivityStatus(int id, String desc){
        ActivityStatus statusChangeType = new ActivityStatus();
        statusChangeType.setId(id);
        statusChangeType.setDescription(desc);
        return statusChangeType;
    }

    /*private static void fillPayFrequency(EntityManager em){
        PayFrequency pf1 = getPayFrequency(1,"Annual",1);
        PayFrequency pf2 = getPayFrequency(2,"Bi-Weekly",26);
        PayFrequency pf3 = getPayFrequency(3,"Monthly",12);
        PayFrequency pf4 = getPayFrequency(4,"Semi-Monthly",24);
        PayFrequency pf5 = getPayFrequency(5,"Weekly",52);
        em.persist(pf1);
        em.persist(pf2);
        em.persist(pf3);
        em.persist(pf4);
        em.persist(pf5);
    }
    private static PayFrequency getPayFrequency(int id, String desc, int times){
        PayFrequency pf = new PayFrequency();
        pf.setId(id);
        pf.setDescription(desc);
        pf.setTimesPerYear(times);
        pf.setSortOrder(id);
        return pf;
    }*/
    /*private static void fillPreTaxOption(EntityManager em){
        PreTaxOption pto1 = getPreTaxOption(1,"Major Medical Plan");
        PreTaxOption pto2 = getPreTaxOption(2,"Supplemental Medical Plan");
        PreTaxOption pto3 = getPreTaxOption(3,"Group Term Life");
        PreTaxOption pto4 = getPreTaxOption(4,"Short Term Disability");
        PreTaxOption pto5 = getPreTaxOption(5,"Long Term Disability");
        PreTaxOption pto6 = getPreTaxOption(6,"Flex Dollars");
        PreTaxOption pto7 = getPreTaxOption(7,"FSA Medical");
        PreTaxOption pto8 = getPreTaxOption(8,"FSA Dependent Care");
        PreTaxOption pto9 = getPreTaxOption(9,"Vacation Purchase");
        PreTaxOption ptoA = getPreTaxOption(10,"Individual Owned Health Plan");
        PreTaxOption ptoB = getPreTaxOption(11,"Health Savings Account");
        em.persist(pto1);
        em.persist(pto2);
        em.persist(pto3);
        em.persist(pto3);
        em.persist(pto4);
        em.persist(pto5);
        em.persist(pto6);
        em.persist(pto7);
        em.persist(pto8);
        em.persist(pto9);
        em.persist(ptoA);
        em.persist(ptoB);
    }
    private static PreTaxOption getPreTaxOption(int id, String desc){
        PreTaxOption pto = new PreTaxOption();
        pto.setId(id);
        pto.setDescription(desc);
        pto.setSortOrder(id);
        return pto;
    }*/

    private static void fillContactMethod(EntityManager em){
        createContactMethod(em,1,"Phone");
        createContactMethod(em,2,"Email");
        createContactMethod(em,3,"Mail");
        createContactMethod(em,4,"Walk-in");
    }
    private static void createContactMethod(EntityManager em, int id, String desc){
        ContactMethod cm = new ContactMethod();
        cm.setId(id);
        cm.setDescription(desc);
        em.persist(cm);
    }

    /*private static void fillContactTypes(EntityManager em){
        createContactType(em,1,"Internal Contact");
        createContactType(em,2,"DataPath Contact");
    }
    private static void createContactType(EntityManager em, int id, String desc){
        ContactType ct = new ContactType();
        ct.setId(id);
        ct.setDescription(desc);
        ct.setInactive(false);
        em.persist(ct);
    }*/
   /* private static void fillSpendDownEvent(EntityManager em){
        createSpendDownEvent(em,1,"Termination");
        createSpendDownEvent(em,2,"Death");
        createSpendDownEvent(em,3,"Disability");
        createSpendDownEvent(em,4,"Retirement");
        createSpendDownEvent(em,5,"Employee Loss of Eligibility");
        createSpendDownEvent(em,6,"USERRA Leave");
    }
    private static void createSpendDownEvent(EntityManager em, int id, String desc){
        SpendDownEvent sde = new SpendDownEvent();
        sde.setId(id);
        sde.setDescription(desc);
        sde.setSortOrder(id);
        em.persist(sde);
    }*/

    /*private static void fillHsaBillingType(EntityManager em){
        createHsaBillingType(em,1,"Bill the Group/Employer");
        createHsaBillingType(em,2,"Bill the HSA Accounts Individually");
        createHsaBillingType(em,3,"Other");
    }
    private static void createHsaBillingType(EntityManager em, int id, String desc){
        HsaBillingType hbt = new HsaBillingType();
        hbt.setId(id);
        hbt.setDescription(desc);
        em.persist(hbt);
    }

    private static void fillHsaFundingType(EntityManager em){
        createHsaFundingType(em,1,"EFT my Bank Account");
        createHsaFundingType(em,2,"Send a Wire Each Time (Manual)");
        createHsaFundingType(em,3,"I will Mail a Check Each Time");
    }
    private static void createHsaFundingType(EntityManager em, int id, String desc){
        HsaFundingType hft = new HsaFundingType();
        hft.setId(id);
        hft.setDescription(desc);
        em.persist(hft);
    }*/

   /* private static void fillStandardCopay(EntityManager em){
        createStandardCopay(em,1,"Office Visit");
        createStandardCopay(em,2,"Urgent Care");
        createStandardCopay(em,3,"Emergency Room");
        createStandardCopay(em,4,"Prescription");
        createStandardCopay(em,5,"Lab");
        createStandardCopay(em,6,"Other");
    }
    private static void createStandardCopay(EntityManager em, int id, String desc){
        StandardCopay sc = new StandardCopay();
        sc.setId(id);
        sc.setDescription(desc);
        sc.setSortOrder(id);
        em.persist(sc);
    }*/
   public static void loadDefaultTicketCategories(EntityManager em){

       // ── Service-Oriented Categories (match production IDs and getTicketCategory() in CreateTicket25) ──
       createTicketCategory(em, 11L, "Claims",            "Claims");
       createTicketCategory(em, 12L, "Access / Online",   "Access");
       createTicketCategory(em, 13L, "Debit Card",        "Debit");
       createTicketCategory(em, 14L, "COBRA",             "COBRA");
       createTicketCategory(em, 15L, "HSA",               "HSA");
       createTicketCategory(em, 16L, "Enrollment",        "Enroll");
       createTicketCategory(em, 17L, "Plan Services",     "Plans");
       createTicketCategory(em, 18L, "Billing",           "Billing");
       createTicketCategory(em, 21L, "General",           "General");

       // ── Starter ServiceItems (group 3, no sequence — just dropdown entries) ──
       // These give a new PSP immediate ticket categorization options.
       // Sequences can be built later through Sequence Builder.
       ActivityCategory ticketGroup = EntityLookup.getTemplateGroupById(em, 3);
       createStarterServiceItem(em, ticketGroup, 11L, "Claim not paid");
       createStarterServiceItem(em, ticketGroup, 11L, "Claim paid incorrectly");
       createStarterServiceItem(em, ticketGroup, 12L, "Can't log in to portal");
       createStarterServiceItem(em, ticketGroup, 12L, "Need online access");
       createStarterServiceItem(em, ticketGroup, 13L, "Debit card not working");
       createStarterServiceItem(em, ticketGroup, 13L, "Debit card replacement");
       createStarterServiceItem(em, ticketGroup, 14L, "COBRA enrollment");
       createStarterServiceItem(em, ticketGroup, 14L, "COBRA payment issue");
       createStarterServiceItem(em, ticketGroup, 15L, "HSA contribution question");
       createStarterServiceItem(em, ticketGroup, 15L, "HSA eligible expense question");
       createStarterServiceItem(em, ticketGroup, 16L, "New hire enrollment");
       createStarterServiceItem(em, ticketGroup, 16L, "Open enrollment");
       createStarterServiceItem(em, ticketGroup, 16L, "Qualifying life event");
       createStarterServiceItem(em, ticketGroup, 17L, "FSA question");
       createStarterServiceItem(em, ticketGroup, 17L, "HRA question");
       createStarterServiceItem(em, ticketGroup, 17L, "Plan quote request");
       createStarterServiceItem(em, ticketGroup, 18L, "Billing discrepancy");
       createStarterServiceItem(em, ticketGroup, 18L, "Invoice request");
       createStarterServiceItem(em, ticketGroup, 21L, "General inquiry");
       createStarterServiceItem(em, ticketGroup, 21L, "Other");
   }

    /**
     * Creates a starter ServiceItem (group 3) for ticket dropdown options.
     * These are just categorization entries — no sequence attached.
     */
    private static void createStarterServiceItem(EntityManager em, ActivityCategory ticketGroup, Long catId, String description){
        TicketCategory tc = EntityLookup.getTicketCategoryById(em, catId);
        if(tc == null) return;
        em.getTransaction().begin();
        ServiceItem si = new ServiceItem();
        si.setDescription(description);
        si.setActivityCategory(ticketGroup);
        si.setTicketCategory(tc);
        si.setSourceType("SYSTEM");
        si.setSortOrder(100);
        em.persist(si);
        em.getTransaction().commit();
    }

    public static void createTicketCategory(EntityManager em, Long id, String name, String shrt){
        em.getTransaction().begin();
        TicketCategory tc = new TicketCategory();
        tc.setId(id);
        tc.setDescription(name);
        tc.setShortText(shrt);
        em.persist(tc);
        em.getTransaction().commit();
    }

    public static void createTicketCategory(EntityManager em, String name){
        em.getTransaction().begin();
        TicketCategory tc = new TicketCategory();
        tc.setDescription(name);
        em.persist(tc);
        em.getTransaction().commit();
    }

    private static void fillUserRoles(EntityManager em){
        createUserRole(em,1,"PSP User");
        createUserRole(em,2,"Agent");
        createUserRole(em,3,"Client");
        createUserRole(em,4,"Applicant");
        createUserRole(em,5,"PSP Admin");
        createUserRole(em,8,"Agency Admin");
        createUserRole(em,9,"PSP Super User");
        createUserRole(em,102,"BPO Admin");
        createUserRole(em,103,"BPO User");
    }
    private static void createUserRole(EntityManager em, int id, String desc){
        UserRole ur = new UserRole();
        ur.setId(id);
        ur.setDescription(desc);
        em.persist(ur);
    }

    private static ActivityCategory createTemplateCategory(EntityManager em, int id, String desc) {
        ActivityCategory tc = new ActivityCategory();
        tc.setId(id);
        tc.setDescription(desc);
        return tc;
    }

    private static void fillTemplateType(EntityManager em) {
        ActivityCategory renewal = createTemplateCategory(em, 1, "Renewal");
        ActivityCategory setup = createTemplateCategory(em, 2, "Setup");
        ActivityCategory ticket = createTemplateCategory(em, 3, "Ticket");
        ActivityCategory opportunity = createTemplateCategory(em, 4, "Opportunity");
        em.persist(renewal);
        em.persist(setup);
        em.persist(ticket);
        em.persist(opportunity);
        createTemplateType(em, 1, "125 Insurance", renewal);
        createTemplateType(em, 2, "125 Health FSA", renewal);
        createTemplateType(em, 3, "125 DCAP", renewal);
        createTemplateType(em, 4, "125 HSA", renewal);
        createTemplateType(em, 5, "105 HRA", renewal);
        createTemplateType(em, 6, "105 MERP", renewal);
        createTemplateType(em, 7, "COBRA Insurance", renewal);
        createTemplateType(em, 8, "COBRA HRA", renewal);
        createTemplateType(em, 9, "COBRA FSA", renewal);
        createTemplateType(em, 10, "CMS Copay", renewal);
        createTemplateType(em, 11, "POP", setup);
        createTemplateType(em, 12, "FSA", setup);
        createTemplateType(em, 13, "HRA", setup);
        createTemplateType(em, 14, "COBRA", setup);
        createTemplateType(em, 15, "Transit", setup);
        createTemplateType(em, 16, "HSA", setup);
        createTemplateType(em, 17, "Payments / Check", setup);
        createTemplateType(em, 18, "Payments / EFT", setup);
        createTemplateType(em, 19, "Debit Cards", setup);
        createTemplateType(em, 20, "Card Co-pays", setup);
        createTemplateType(em, 21, "Ticket General", ticket);
        createTemplateType(em, 22, "Opportunity General", opportunity);
        createTemplateType(em,23,"Other",renewal);
    }

    private static void createTemplateType(EntityManager em, int id, String desc, ActivityCategory group) {
        ServiceItem tt = new ServiceItem();
        tt.setId(id);
        tt.setDescription(desc);
        tt.setSortOrder(id);
        tt.setActivityCategory(group);
        em.persist(tt);
    }

    private static void fillRecurringFrequency(EntityManager em) {
        createRecurringFrequency(em, 1, "Daily (Weekdays)");
        createRecurringFrequency(em, 2, "Weekly");
        createRecurringFrequency(em, 3, "Bi-Weekly");
        createRecurringFrequency(em, 4, "1st and 15th");
        createRecurringFrequency(em, 5, "15th and Last");
        createRecurringFrequency(em, 6, "Monthly");
        createRecurringFrequency(em, 7, "Quarterly");
        createRecurringFrequency(em, 8, "Semi-Annually");
        createRecurringFrequency(em, 9, "Annually");
        createRecurringFrequency(em, 10, "First Monday of the Month");
        createRecurringFrequency(em, 11, "First Tuesday of the Month");
        createRecurringFrequency(em, 12, "First Wednesday of the Month");
        createRecurringFrequency(em, 13, "First Thursday of the Month");
        createRecurringFrequency(em, 14, "First Friday of the Month");
        createRecurringFrequency(em, 15, "Last Monday of the Month");
        createRecurringFrequency(em, 16, "Last Tuesday of the Month");
        createRecurringFrequency(em, 17, "Last Wednesday of the Month");
        createRecurringFrequency(em, 18, "Last Thursday of the Month");
        createRecurringFrequency(em, 19, "Last Friday of the Month");
    }

    private static void createRecurringFrequency(EntityManager em, int id, String desc) {
        TaskFrequency rf = new TaskFrequency();
        rf.setId(id);
        rf.setDescription(desc);
        em.persist(rf);
    }

    private static void fillSSAData(EntityManager em) {
        em.getTransaction().begin();
        Address address = fillAddress(em);
        PSP psp = fillPSP(em, address);
        fillLOS(em,psp);
        Address address1 = fillAddress(em);
        Person person = fillPerson(em, psp, address1);
        Address address2 = fillAddress(em);
        Agency agency = fillAgency(em,psp,address2,person);

        fillServiceModules(em, psp);
        fillPriceItems(em, psp);
        fillServiceItems(em, psp);
        em.getTransaction().commit();



        agency.addAgent(person);
        em.getTransaction().begin();
        em.persist(agency);
        em.persist(person);
        em.getTransaction().commit();
        //assignRoles(em, dbAuth.getUserFromPerson(em,person));
        createRate(em,agency,psp);
        assignModulesToLos(em);
    }

    private static void assignModulesToLos(EntityManager em){
        ServiceModule smDebitCards = SalesDAO.getServModByShortText(em,"Cards");
        ServiceModule smPayment = SalesDAO.getServModByShortText(em,"Payment");
        ServiceModule smPOP = SalesDAO.getServModByShortText(em,"POP");
        ServiceModule smFSA = SalesDAO.getServModByShortText(em,"FSA");
        ServiceModule smHRA = SalesDAO.getServModByShortText(em,"HRA");
        ServiceModule smHSA = SalesDAO.getServModByShortText(em,"HSA");
        ServiceModule smCobra = SalesDAO.getServModByShortText(em,"COBRA");
        ServiceModule smTrans = SalesDAO.getServModByShortText(em,"Transit");
        LOS pop = SalesDAO.getLosByShortText(em,"POP");
        LOS fsa = SalesDAO.getLosByShortText(em,"FSA");
        LOS hra = SalesDAO.getLosByShortText(em,"HRA/MERP");
        LOS hsa = SalesDAO.getLosByShortText(em,"HSA");
        LOS cobra = SalesDAO.getLosByShortText(em,"COBRA");
        LOS transit = SalesDAO.getLosByShortText(em,"TRANSIT");
        assignMods(em,smPOP,pop);
        assignMods(em,smFSA,fsa);
        assignMods(em,smDebitCards,fsa);
        assignMods(em,smPayment,fsa);
        assignMods(em,smHRA,hra);
        assignMods(em,smPayment,hra);
        assignMods(em,smDebitCards,hra);
        assignMods(em,smTrans,transit);
        assignMods(em,smHSA,hsa);
        assignMods(em,smCobra,cobra);
    }

    private static void assignMods(EntityManager em, ServiceModule sm, LOS los){
        em.getTransaction().begin();
        los.addServiceModule(sm);
        em.persist(los);
        em.persist(sm);
        em.getTransaction().commit();
    }
    private static void createRate(EntityManager em, Agency agency, PSP psp){
        Rate rate = new Rate();
        rate.setDescription("Standard Rate");
        rate.setPsp(psp);
        rate.setSuppressed(false);
        em.getTransaction().begin();
        em.persist(rate);
        em.getTransaction().commit();

        Agency superiorState = SalesDAO.getAgencyFull(em, agency.getId());
        superiorState.addRate(rate);
        em.getTransaction().begin();
        em.persist(superiorState);
        em.getTransaction().commit();

        developRateTable(em,rate);

    }

    public static void developRateTable(EntityManager em, Rate rate){
        ServiceModule pop = SalesDAO.getServModByShortText(em,"POP");
        ServiceModule fsa = SalesDAO.getServModByShortText(em,"FSA");
        ServiceModule hra = SalesDAO.getServModByShortText(em,"HRA");
        ServiceModule hsa = SalesDAO.getServModByShortText(em,"HSA");
        PriceItem iSetup = SalesDAO.getPriceItemBySortOrder(em,100);
        PriceItem iAnnual = SalesDAO.getPriceItemBySortOrder(em,200);
        PriceItem iBase = SalesDAO.getPriceItemBySortOrder(em,300);
        PriceItem iOnline = SalesDAO.getPriceItemBySortOrder(em,280);
        PriceItem iPaper = SalesDAO.getPriceItemBySortOrder(em,290);
        //pop plans
        assignRateTable(em,rate,pop,iAnnual,199.00);
        //fsa plans;
        assignRateTable(em,rate,fsa,iSetup,350.00);
        assignRateTable(em,rate,fsa,iAnnual,350.00);
        assignRateTable(em,rate,fsa,iBase,3.75);
        //hra plans
        assignRateTable(em,rate,hra,iSetup,350.00);
        assignRateTable(em,rate,hra,iAnnual,350.00);
        assignRateTable(em,rate,hra,iBase,3.75);
        //hsa plans
        assignRateTable(em,rate,hsa,iOnline,15.00);
        assignRateTable(em,rate,hsa,iPaper,20.00);
        assignRateTable(em,rate,hsa,iBase,4.00);
    }
    private static void assignRateTable(EntityManager em, Rate rate, ServiceModule sm, PriceItem pi, double price){
        RateTable rateTable = new RateTable();
        rateTable.setRate(rate);
        rateTable.setModule(sm);
        rateTable.setPriceItem(pi);
        rateTable.setPrice(price);
        em.getTransaction().begin();
        em.persist(rateTable);
        em.getTransaction().commit();
    }
   private static void assignRoles(EntityManager em, User user) {
        UserRole agentRole = AuthDAO.getUserRoleById(em, 2);
        UserRole pspRole = AuthDAO.getUserRoleById(em, 1);
        UserRole pspAdminRole = AuthDAO.getUserRoleById(em, 5);
        user.addUserToRole(agentRole);
        user.addUserToRole(pspRole);
        user.addUserToRole(pspAdminRole);
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
    }

    private static void createServiceModule(EntityManager em,String shortText, String desc, int sortOrder, PSP psp){
        ServiceModule sm = new ServiceModule();
        sm.setDescription(desc);
        sm.setShortText(shortText);
        sm.setPsp(psp);
        sm.setSortOrder(sortOrder);
        em.persist(sm);
    }
    private static void fillServiceModules(EntityManager em,PSP psp){
        createServiceModule(em,"POP","Section 125 Premium Only Plans",100,psp);
        createServiceModule(em,"FSA","Section 125 Full Flex Plans with FSAs",200,psp);
        createServiceModule(em,"Other","Expanded 125 Plans with HSA Contributions", 150,psp);
        createServiceModule(em,"HRA","Section 105 HRAs / MERPs",300,psp);
        createServiceModule(em,"HSA","Health Savings Accounts (HSAs)",400,psp);
        createServiceModule(em,"Payment","Payment Services",500,psp);
        createServiceModule(em,"Cards","Debit Card Services",600,psp);
        createServiceModule(em,"Doc's","Document Services (When/If Required)",700,psp);
        createServiceModule(em,"COBRA","COBRA Administration",800,psp);
        createServiceModule(em,"Discounts","Multi-plan Discounts for FSA/HRA Plans",900,psp);
        createServiceModule(em,"Transit","Transit Plans",450,psp);
        createServiceModule(em,"Notes","Notes for POP Pricing",149,psp);
        createServiceModule(em,"Notes","Notes for 125 w/HSA Pricing", 199,psp);
        createServiceModule(em,"Notes","Notes for FSA Pricing", 249,psp);
        createServiceModule(em,"Notes","Notes for HRA/MERP Pricing", 349,psp);
        createServiceModule(em,"Notes","Notes for COBRA Pricing",849,psp);
    }
    private static void fillPriceItems(EntityManager em,PSP psp){
        createPriceItem(em,"Setup (One-Time) Fee",100,psp);
        createPriceItem(em,"Annual Administration Fee",200,psp);
        createPriceItem(em,"Base Monthly Fee per Participant",300,psp);
        createPriceItem(em,"Online Application Fee",280,psp);
        createPriceItem(em,"Paper Application Fee",290,psp);
        createPriceItem(em,"One-Time Card Creation/Mailing Fee",400,psp);
        createPriceItem(em,"Plan Amendment Fee",1000,psp);
        createPriceItem(em,"Plan Restatement Fee",1100,psp);
        createPriceItem(em,"Form 5500 Annual Filing Fee",1200,psp);
        createPriceItem(em,"Blanket Mailing of Initial Notices",500,psp);
        createPriceItem(em,"Carrier Notice Fee per Month per Employee",600,psp);
        createPriceItem(em,"Monthly Fee per Employee",450,psp);
        createPriceItem(em,"Overall Setup Fee Reduction",700,psp);
        createPriceItem(em,"Overall Annual Fee Reduction",800,psp);
        createPriceItem(em,"Fee per Month Reduction for Dual-Plan Participants", 900,psp);
    }

    private static void fillServiceItems(EntityManager em, PSP psp){
        createServiceItem(em,psp,"Legal Documents to Establish Your Plan",100,"");
        createServiceItem(em,psp,"Non-discrimination Testing Services",200,"");
        createServiceItem(em,psp,"Accounting and Reconciliation Services",300,"");
        createServiceItem(em,psp,"Reporting of Account Activity and Account Balances",400,"");
        createServiceItem(em,psp,"Online Web Access and Claim Filing",500,"");
        createServiceItem(em,psp,"iPhone and Android Phone App Access and Claim Filing",600,"");
    }
    private static void createServiceItem(EntityManager em, PSP psp, String bullet, int sortOrder, String desc){
        ModuleDetail si = new ModuleDetail();
        si.setSuppressed(false);
        si.setBulletPoint(bullet);
        si.setDescription(desc);
        si.setPsp(psp);
        si.setSortOrder(sortOrder);
        em.persist(si);
    }
    private static void createPriceItem(EntityManager em, String desc, int sortOrder,PSP psp){
        PriceItem pi = new PriceItem();
        pi.setDescription(desc);
        pi.setPsp(psp);
        pi.setSortOrder(sortOrder);
        em.persist(pi);
    }
    private static void fillDaysOfTheWeek(EntityManager em){
        SequenceDAO.createDoW(em,17,"Sunday",7);
        SequenceDAO.createDoW(em,11,"Monday",1);
        SequenceDAO.createDoW(em,12,"Tuesday",2);
        SequenceDAO.createDoW(em,13,"Wednesday",3);
        SequenceDAO.createDoW(em,14,"Thursday",4);
        SequenceDAO.createDoW(em,15,"Friday",5);
        SequenceDAO.createDoW(em,16,"Saturday",6);
    }

    private static void fillLinkTypes(EntityManager em){
        SequenceDAO.createLinkType(em,1,"File Upload");
        SequenceDAO.createLinkType(em,2,"Hyperlink");
    }


    public static void fillStarterData() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = emf.createEntityManager();
        fillDaysOfTheWeek(em);
        fillLinkTypes(em);
        //********************** STANDARD SELECTION DATA ******************************************
        em.getTransaction().begin();
//        fillLastDay(em); //COBRA Last Day of Coverage Standard Selections
//        fillInsuranceTypes(em); //COBRA Insurance Type Standard Selections
//        fillInsuranceRateType(em);//COBRA Insurance Rate Types
//        fillBankAccountType(em); //Checking / Savings / Other....
//        fillFsaMode(em); // Spend Down, Carryover
//        fillPayFrequency(em); //125 Plan Pay Frequencies
//        fillPreTaxOption(em); //125 Plan Type Selections for Inclusion in Plan
        fillContactMethod(em); //Tickets - Methods to Reach Out
//        fillSpendDownEvent(em); //Valid Events that Cause HRA Spend-down
//        fillHsaBillingType(em);
//        fillHsaFundingType(em);
        fillUserRoles(em);
//        fillContactTypes(em); //Classifying contact type (internal db versus datapath members table)
//        fillActivityType(em); //Ways to classify task assignments (Setup, Renewal, Ticket)
        fillTemplateType(em); //For Task Templates for Setups and Renewals
        fillRecurringFrequency(em); //For Task Recurrences
//        fillStandardCopay(em); //For Copay setups
        fillActivityStatus(em);
        fillNoteReasons(em); //For Notes

        em.getTransaction().commit();
        fillBillingGroups(em);
        fillPlanTypes(em);
        loadDefaultTicketCategories(em);
        // ****************************************************************************************

        fillSSAData(em); // ADDS THE SSA BASIC INFORMATION INTO THE SYSTEM

        emf.close();
    }
}
