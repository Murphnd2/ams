package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.data.util.BillingHelper;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.billing.BillingGrid;
import net.superiorstate.ams.model.billing.BillingGroup;
import net.superiorstate.ams.model.billing.BillingLink;
import net.superiorstate.ams.model.billing.BillingMonth;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.CoverageStatus;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.model.summit.imports.HsaAccount;
import net.superiorstate.ams.model.summit.imports.HsaEe;
import net.superiorstate.ams.model.summit.imports.order.ImportBenefitCdh;
import net.superiorstate.ams.model.summit.imports.order.ImportEnrollment;
import net.superiorstate.ams.model.summit.temp.Coverage;
import net.superiorstate.ams.model.summit.temp.Enrollment2;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MonthlyBiller extends Biller {

    public MonthlyBiller(EntityManager em) {
        super(em);
    }
    @Override
    public void run() {
        setBillingFlags();
        createBillingMonth();
        clearBillingEnrollmentTable();
        fillBillingEnrollmentTable();
        clearBillingCoverageTable();
        clearCoverageStatusForMonth();
        fillBillingCoverageTableAlt(0);
        logCoverageStatusForThisMonthCDH();
        logCoverageStatusForThisMonthPB(0);
        clearBillingGridForMonth();
        fillBillingGrid(0);
        BillingMonth bm = getBillingMonthByDate(BillingHelper.getMonthFor());
        fillHsaBillingGrid(bm);
        fillBillingLinks();
        fillDualParticipantGrid();
    }
    protected void createBillingMonth() {
        LocalDate billingDate = LocalDate.now().withDayOfMonth(1);
        Date fullDate = Date.valueOf(billingDate);

        boolean exists = em.createQuery(
                        "SELECT COUNT(bm) FROM BillingMonth bm WHERE bm.fullDate = :date", Long.class)
                .setParameter("date", fullDate)
                .getSingleResult() > 0;

        if (!exists) {
            em.getTransaction().begin();
            BillingMonth bm = new BillingMonth();
            bm.setFullDate(fullDate);
            bm.setYear(billingDate.getYear());
            bm.setMonth(billingDate.getMonthValue());
            em.persist(bm);
            em.getTransaction().commit();
        }
    }
    protected BillingMonth getBillingMonthByDate(Date date) {
        try {
            return em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.fullDate = :date", BillingMonth.class)
                    .setParameter("date", date)
                    .getSingleResult();
        } catch (NoResultException e) {
            return new BillingMonth(); // empty default — you can refine
        }
    }
    protected void clearBillingEnrollmentTable() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Enrollment2").executeUpdate();
        em.getTransaction().commit();
    }
    @SuppressWarnings("unchecked")
    protected void fillBillingEnrollmentTable() {
        List<ImportEnrollment> enrollments = em.createQuery(
                "SELECT ie FROM ImportEnrollment ie WHERE TRIM(LOWER(ie.planStatus)) = 'active'",
                ImportEnrollment.class
        ).getResultList();

        System.out.println("📄 Found enrollments: " + enrollments.size());
        if (enrollments.isEmpty()) return;

        // Step 1: Preload all benefits
        Map<Integer, ImportBenefitCdh> benefitMap = em.createQuery(
                "SELECT b FROM ImportBenefitCdh b", ImportBenefitCdh.class
        ).getResultStream().collect(Collectors.toMap(
                ImportBenefitCdh::getBenefitId,
                b -> b
        ));

        em.getTransaction().begin();
        int count = 0;
        int skipped = 0;

        for (ImportEnrollment ie : enrollments) {
            try {
                if (ie.getImportBenefitYear() == null) {
                    System.out.println("⚠️ Skipping: Enrollment ID " + ie.getEnrollmentId() + " has null ImportBenefitYear");
                    skipped++;
                    continue;
                }

                int benefitId = Integer.parseInt(ie.getEmployerPlanId());
                ImportBenefitCdh ib = benefitMap.get(benefitId);
                if (ib == null) {
                    System.out.println("⚠️ Skipping: No benefit found for ID " + benefitId + " (Enrollment ID " + ie.getEnrollmentId() + ")");
                    skipped++;
                    continue;
                }

                Enrollment2 e = new Enrollment2();
                e.setEnrollmentId(ie.getEnrollmentId());
                e.setImportEmployer(ie.getImportEmployee().getImportEmployer());
                e.setImportEmployee(ie.getImportEmployee());
                e.setImportBenefitCdh(ib);
                e.setImportBenefitYear(ie.getImportBenefitYear());
                e.setBillingGroup(ib.getPlanType().getBillingGroup());
                e.setCurrentMonth(BillingHelper.getMonthFor());
                e.setTermDate(Date.valueOf(ie.getTermDate()));
                e.setStartDate(Date.valueOf(ie.getImportBenefitYear().getPlanYearStart()));
                e.setEndDate(Date.valueOf(ie.getImportBenefitYear().getPlanYearEnd()));
                e.setBillable(ie.isBillableThisMonth());
                e.setEmployerName(ie.getEmployerName());
                e.setParticipantName(ie.getImportEmployee().getFullNameLastThenFirst());
                e.setBenefitName(ib.getPlanName());
                e.setBenefitGroup(ib.getPlanType().getBillingGroup().getDescription());
                e.setCardEnabled(ib.getCardEnabledBoolean());

                em.persist(e);

                if (++count % 100 == 0) {
                    System.out.println("💾 Flushing after " + count + " inserts...");
                    em.flush();
                    em.clear();
                }

            } catch (Exception ex) {
                System.out.println("❌ Failed to import ID " + ie.getEnrollmentId() + ": " + ex.getMessage());
                ex.printStackTrace();
                skipped++;
            }
        }

        em.getTransaction().commit();
        System.out.printf("✅ Billing enrollment import complete. Imported: %d, Skipped: %d%n", count, skipped);
    }
    protected void clearBillingCoverageTable() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Coverage").executeUpdate();
        em.getTransaction().commit();
    }
    protected void clearCoverageStatusForMonth() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM CoverageStatus cs WHERE cs.monthFor = :month")
                .setParameter("month", BillingHelper.getMonthFor())
                .executeUpdate();
        em.getTransaction().commit();
    }
    @SuppressWarnings("unchecked")
    protected void fillBillingCoverageTableAlt(int erId) {
        int coverageId = (erId != 0) ? 99999 : 0;
        Date monthFor = BillingHelper.getMonthFor();
        int count = 0, skipped = 0;

        // Step 1: Load existing CoverageStatus keys to skip duplicates
        Set<String> existingKeys = em.createQuery("""
        SELECT CONCAT(cs.monthFor, '-', cs.benefit.id, '-', cs.employee.id)
        FROM CoverageStatus cs
        WHERE cs.monthFor = :month
    """, String.class)
                .setParameter("month", monthFor)
                .getResultStream()
                .collect(Collectors.toSet());

        // Step 2: Load employers
        List<Employer> employers = (erId != 0)
                ? Collections.singletonList(EntityLookup.getEmployerById(em, erId))
                : em.createQuery("SELECT er FROM Employer er WHERE er.hasPb = true", Employer.class)
                .getResultList();

        em.getTransaction().begin();

        for (Employer er : employers) {
            List<Benefit> benefits = em.createQuery("""
            SELECT b FROM Benefit b
            WHERE b.employer.id = :id
              AND b.isActive = true
              AND b.planType.billingGroup.id = 3
        """, Benefit.class)
                    .setParameter("id", er.getId())
                    .getResultList();

            if (benefits.isEmpty()) continue;

            Benefit b = benefits.get(0);
            BillingGroup group = EntityLookup.getBillingGroupById(em, 3);

            List<Employee> employees = em.createQuery("""
            SELECT ee FROM Employee ee
            WHERE ee.employer.id = :id AND ee.id > 0
        """, Employee.class)
                    .setParameter("id", er.getId())
                    .getResultList();

            for (Employee ee : employees) {
                if (!isBillable(ee)) continue;

                String key = monthFor.toString() + "-" + b.getId() + "-" + ee.getId();
                if (existingKeys.contains(key)) {
                    skipped++;
                    continue;
                }

                Coverage c = new Coverage();
                c.setCoverageId(++coverageId);
                c.setSummitOrganization(er);
                c.setSummitEmployee(ee);
                c.setEmployerId(er.getAltId());
                c.setSummitPlanType(b.getPlanType());
                c.setStatus(getStatus(ee));
                c.setPbBenefitId(b.getId());
                c.setBillable(!"TERM".equals(c.getStatus()));
                c.setCurrentMonth(monthFor);
                c.setBenefitName(b.getPlanName());
                c.setBillingGroup(group);
                c.setTierName(b.getPlanType().getPlanTypeName());

                em.persist(c);

                if (++count % 50 == 0) {
                    em.flush();
                    em.clear();
                    System.out.println("💾 Flushed at count: " + count);
                }
            }
        }

        em.getTransaction().commit();
        System.out.printf("✅ Coverage import complete. Inserted: %d, Skipped (duplicates): %d%n", count, skipped);
    }
    @SuppressWarnings("unchecked")
    protected void logCoverageStatusForThisMonthCDH() {
        List<Enrollment2> list = em.createQuery(
                "SELECT e FROM Enrollment2 e WHERE e.billable = true", Enrollment2.class).getResultList();

        em.getTransaction().begin();
        int count = 0;

        for (Enrollment2 e : list) {
            Benefit b = EntityLookup.getBenefitById(em, e.getImportBenefitCdh().getBenefitId());
            Employee ee = EntityLookup.getEmployeeById(em, e.getImportEmployee().getId());

            CoverageStatus cs = new CoverageStatus();
            cs.setId(e.getCurrentMonth() + "-" + b.getId() + "-" + ee.getId());
            cs.setBenefit(b);
            cs.setBillingGroup(b.getPlanType().getBillingGroup());
            cs.setEmployer(ee.getEmployer());
            cs.setEmployee(ee);
            cs.setMonthFor(e.getCurrentMonth());
            cs.setActive(e.isBillable());
            cs.setHasCards(e.isCardEnabled());

            em.persist(cs);
            if (++count % 50 == 0) {
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
    }
    @SuppressWarnings("unchecked")
    protected void logCoverageStatusForThisMonthPB(int erId) {
        List<Coverage> coverages = em.createQuery(
                "SELECT c FROM Coverage c WHERE c.isBillable = true", Coverage.class).getResultList();

        em.getTransaction().begin();
        int count = 0;

        for (Coverage coverage : coverages) {
            if (erId != 0 && coverage.getSummitOrganization().getId() != erId) continue;

            Benefit b = EntityLookup.getBenefitById(em, coverage.getPbBenefitId());
            Employee ee = EntityLookup.getEmployeeById(em, coverage.getSummitEmployee().getId());

            if (b == null || ee == null) continue;

            CoverageStatus cs = new CoverageStatus();
            cs.setId(coverage.getCurrentMonth() + "-" + b.getId() + "-" + ee.getId());
            cs.setBenefit(b);
            cs.setBillingGroup(b.getPlanType().getBillingGroup());
            cs.setEmployee(ee);
            cs.setEmployer(ee.getEmployer());
            cs.setMonthFor(coverage.getCurrentMonth());
            cs.setActive(true);
            cs.setHasCards(false);

            em.persist(cs);

            if (++count % 50 == 0) {
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
    }
    @SuppressWarnings("unchecked")
    protected void fillBillingGrid(int erId) {
        Date targetMonth = BillingHelper.getMonthFor();

        // Fetch all active CoverageStatus records for the billing month
        List<CoverageStatus> statuses = em.createQuery(
                        "SELECT cs FROM CoverageStatus cs WHERE cs.monthFor = :monthFor AND cs.isActive = true", CoverageStatus.class)
                .setParameter("monthFor", targetMonth)
                .getResultList();

        if (statuses.isEmpty()) return;

        // Preload all existing BillingGrids for that month
        Map<String, BillingGrid> gridMap = preloadGridMap(targetMonth);

        em.getTransaction().begin();
        int count = 0;

        for (CoverageStatus cs : statuses) {
            if (erId != 0 && cs.getEmployer().getId() != erId) continue;

            String gridId = targetMonth + "-" + cs.getEmployee().getId();
            BillingGrid bg = gridMap.get(gridId);

            if (bg == null) {
                bg = new BillingGrid();
                bg.setGridId(gridId);
                bg.setBillingMonth(getOrCreateBillingMonth(targetMonth)); // optional safety
                bg.setEmployer(cs.getEmployer());
                bg.setEmployee(cs.getEmployee());
                gridMap.put(gridId, bg);
            }

            addCoverageStatusToBillingGrid(cs, bg);
            em.merge(bg);

            if (++count % 50 == 0) {
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
    }
    private Map<String, BillingGrid> preloadGridMap(Date monthFor) {
        List<BillingGrid> grids = em.createQuery(
                        "SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.fullDate = :monthFor", BillingGrid.class)
                .setParameter("monthFor", monthFor)
                .getResultList();

        return grids.stream()
                .collect(Collectors.toMap(BillingGrid::getGridId, Function.identity()));
    }
    private BillingMonth getOrCreateBillingMonth(Date fullDate) {
        try {
            return em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.fullDate = :date", BillingMonth.class)
                    .setParameter("date", fullDate)
                    .getSingleResult();
        } catch (NoResultException e) {
            BillingMonth bm = new BillingMonth();
            bm.setFullDate(fullDate);
            bm.setMonth(LocalDate.ofInstant(fullDate.toInstant(), ZoneId.systemDefault()).getMonthValue());
            bm.setYear(LocalDate.ofInstant(fullDate.toInstant(), ZoneId.systemDefault()).getYear());
            em.persist(bm);
            return bm;
        }
    }
    protected void addCoverageStatusToBillingGrid(CoverageStatus cs, BillingGrid bg) {
        bg.setBillingMonth(getBillingMonthByDate(BillingHelper.getMonthFor()));
        bg.setEmployee(cs.getEmployee());
        bg.setEmployer(cs.getEmployer());
        bg.setCurrentStatus("TBD");

        switch (cs.getBillingGroup().getId()) {
            case 1 -> {
                bg.setFlexSpend(true);
                if (bg.isHealthReimb()) bg.setDualPlan(true);
            }
            case 2 -> {
                bg.setHealthReimb(true);
                if (bg.isFlexSpend()) bg.setDualPlan(true);
            }
            case 3 -> bg.setCobra(true);
            case 4 -> bg.setTransit(true);
            case 5 -> bg.setHsa(true);
            case 6 -> bg.setLsa(true);
        }
    }
    @SuppressWarnings("unchecked")
    protected void fillBillingLinks() {
        List<BillingMonth> months = em.createQuery(
                "SELECT bm FROM BillingMonth bm ORDER BY bm.year, bm.month", BillingMonth.class).getResultList();

        em.getTransaction().begin();
        int count = 0;

        for (BillingMonth bm : months) {
            List<BillingGrid> grids = em.createQuery(
                            "SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.monthId = :mId", BillingGrid.class)
                    .setParameter("mId", bm.getMonthId())
                    .getResultList();

            for (BillingGrid bg : grids) {
                if (bg.getEmployer() == null) continue;

                Long linkCount = em.createQuery("""
                SELECT COUNT(bl) FROM BillingLink bl 
                WHERE bl.billingMonth.monthId = :mId AND bl.employer.id = :eId
                """, Long.class)
                        .setParameter("mId", bm.getMonthId())
                        .setParameter("eId", bg.getEmployer().getId())
                        .getSingleResult();

                if (linkCount == 0) {
                    BillingLink bl = new BillingLink();
                    bl.setBillingId(bm.getFullDate() + "-" + bg.getEmployer().getId());
                    bl.setBillingMonth(bm);
                    bl.setEmployer(bg.getEmployer());
                    bl.setUniqueId(UUID.randomUUID().toString());

                    em.persist(bl);

                    if (++count % 50 == 0) {
                        em.flush();
                        em.clear();
                    }
                }
            }
        }

        em.getTransaction().commit();
    }
    @SuppressWarnings("unchecked")
    protected void fillDualParticipantGrid() {
        BillingMonth bm = getBillingMonthByDate(BillingHelper.getMonthFor());

        List<BillingGrid> grids = em.createQuery(
                        "SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.monthId = :id", BillingGrid.class)
                .setParameter("id", bm.getMonthId())
                .getResultList();

        if (grids.isEmpty()) return;

        em.getTransaction().begin();
        int count = 0;

        for (BillingGrid bg : grids) {
            if (qualifiesForDualPlan(bg) && !bg.isDualPlan()) {
                bg.setDualPlan(true); // Managed entity, no need for persist or merge

                if (++count % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
        }

        em.getTransaction().commit();
    }
    private boolean qualifiesForDualPlan(BillingGrid bg) {
        boolean hasFS = bg.isFlexSpend();
        boolean hasHRA = bg.isHealthReimb();
        boolean hasHSA = bg.isHsa();

        return (hasFS && hasHRA) || (hasFS && hasHSA) || (hasHRA && hasHSA);
    }
    protected void setBillingFlags() {
        List<Object[]> results = em.createQuery("""
        SELECT b.employer.id, b.planType.planTypeId 
        FROM Benefit b 
        WHERE b.isActive = true
        """, Object[].class).getResultList();

        // Step 1: Build maps of employer ID -> flag types
        Map<Integer, Boolean> hasPb = new HashMap<>();
        Map<Integer, Boolean> hasCdh = new HashMap<>();
        Map<Integer, Boolean> hasPop = new HashMap<>();

        for (Object[] row : results) {
            Integer employerId = (Integer) row[0];
            Integer planTypeId = (Integer) row[1];

            if (planTypeId == 1005 || planTypeId == 1007) {
                hasPop.put(employerId, true);
            } else if (planTypeId <= 8 || (planTypeId >= 1001 && planTypeId <= 1004)) {
                hasCdh.put(employerId, true);
            } else if (planTypeId <= 14 || planTypeId == 1010) {
                hasPb.put(employerId, true);
            }
        }

        // Step 2: Bulk load all employers and apply flag logic
        List<Employer> employers = em.createQuery(
                "SELECT er FROM Employer er WHERE er.id > 0", Employer.class).getResultList();

        em.getTransaction().begin();
        for (Employer er : employers) {
            int id = er.getId();
            boolean pb = hasPb.getOrDefault(id, false);
            boolean cdh = hasCdh.getOrDefault(id, false);
            boolean pop = hasPop.getOrDefault(id, false);

            // Only update if any value differs
            if (er.isPb() != pb || er.isCdh() != cdh || er.isPop() != pop) {
                er.setHasPb(pb);
                er.setHasCdh(cdh);
                er.setHasPop(pop);
                em.persist(er);
            }
        }
        em.getTransaction().commit();
    }

    // Determines if the employee should be considered billable
    private boolean isBillable(Employee ee) {
        if (ee.getCobraStatusId() != null && ee.getCobraStatusId() == 1) return false; // Terminated
        if (ee.getSystemStatusId() != null && ee.getSystemStatusId() == 2) return false; // System inactive
        if (ee.getEeStatusId() != null && ee.getEeStatusId() > 8) return false; // Inactive employment status
        return ee.isActive(); // Active flag must be true
    }

    // Resolves coverage status label for reporting
    private String getStatus(Employee ee) {
        if (ee.getCobraStatusId() != null) {
            return switch (ee.getCobraStatusId()) {
                case 2 -> "QB";   // Qualified Beneficiary
                case 1 -> "TERM"; // Terminated
                default -> "COBRA";
            };
        }
        return "Active";
    }
    protected BillingGrid getOrCreateGridById(String id, Date month, Employer er, Employee ee) {
        try {
            return em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.gridId = :id", BillingGrid.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            BillingGrid bg = new BillingGrid();
            bg.setGridId(id);
            bg.setBillingMonth(getBillingMonthByDate(month));
            bg.setEmployer(er);
            bg.setEmployee(ee);
            bg.setCurrentStatus("TBD");
            return bg;
        }
    }
    private String getGridId(HsaEe hsaEe, BillingMonth bm) {
        int eeId = hsaEe.getEmployee().getId();
        String eeIdString = (eeId < 0) ? "N" + (-1 * eeId) : String.valueOf(eeId);
        return bm.getFullDate() + "-" + eeIdString;
    }
    private Map<String, BillingGrid> getGridMap(BillingMonth bm) {
        List<BillingGrid> grids = em.createQuery(
                        "SELECT bg FROM BillingGrid bg WHERE bg.billingMonth = :bm", BillingGrid.class)
                .setParameter("bm", bm)
                .getResultList();

        return grids.stream().collect(Collectors.toMap(BillingGrid::getGridId, Function.identity()));
    }
    protected BillingGrid createBillingGridForEe(HsaEe hsaEe, BillingMonth bm) {
        BillingGrid bg = new BillingGrid();
        bg.setGridId(getGridId(hsaEe, bm));
        bg.setBillingMonth(bm);
        bg.setEmployee(hsaEe.getEmployee());
        bg.setEmployer(hsaEe.getHsaEr().getEmployer());

        bg.setCurrentStatus("TBD");
        bg.setCobra(false);
        bg.setDirect(false);
        bg.setDualPlan(false);
        bg.setFlexSpend(false);
        bg.setHealthReimb(false);
        bg.setHsa(true);
        bg.setLsa(false);
        bg.setRetiree(false);
        bg.setTransit(false);

        em.persist(bg);
        return bg;
    }
    protected void fillHsaBillingGrid(BillingMonth bm) {
        List<HsaAccount> accounts = getActiveHsas();
        if (accounts.isEmpty()) return;

        // Preload all relevant HsaEe records mapped by HsaId
        Map<Integer, HsaEe> eeMap = getHsaEeMap();

        // Preload all BillingGrid records for the given billing month, mapped by gridId
        Map<String, BillingGrid> gridMap = getGridMap(bm);

        em.getTransaction().begin();
        int count = 0;

        for (HsaAccount account : accounts) {
            HsaEe hsaEe = eeMap.get(account.getHsaId());
            if (hsaEe == null || hsaEe.getEmployee() == null) continue;

            if (!hsaEe.getHsaEr().isBilledDirect()) {
                String gridId = getGridId(hsaEe, bm);
                BillingGrid bg = gridMap.get(gridId);

                if (bg == null) {
                    bg = createBillingGridForEe(hsaEe, bm);
                    gridMap.put(gridId, bg); // cache new grid
                } else {
                    bg.setHsa(true);
                    em.merge(bg);
                }

                if (++count % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
        }

        em.getTransaction().commit();
    }
    protected List<HsaAccount> getActiveHsas() {
        return em.createQuery("SELECT h FROM HsaAccount h WHERE h.active = true", HsaAccount.class)
                .getResultList();
    }
    private Map<Integer, HsaEe> getHsaEeMap() {
        List<HsaEe> list = em.createQuery(
                        "SELECT e FROM HsaEe e WHERE e.employee IS NOT NULL", HsaEe.class)
                .getResultList();

        return list.stream().collect(Collectors.toMap(HsaEe::getHsaId, Function.identity()));
    }
    protected void clearBillingGridForMonth() {
        int monthId = resolveMonthId();  // already wraps Helper.getMonthFor()
        if (monthId == -1) {
            System.out.println("⚠️ No billing month found — skipping billing grid clear.");
            return;
        }

        System.out.println("🧹 Clearing BillingGrid entries for monthId: " + monthId);
        em.getTransaction().begin();
        em.createQuery("DELETE FROM BillingGrid bg WHERE bg.billingMonth.monthId = :monthId")
                .setParameter("monthId", monthId)
                .executeUpdate();
        em.getTransaction().commit();
    }


}

