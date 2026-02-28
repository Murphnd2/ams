package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.billing.*;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class BillingQueryDAO {

    public static Employer getEmployerByBillingGuid(EntityManager em, String guid) {
        BillingLink b = getBillingLinkByGuid(em, guid);
        Employer e;
        try {
            e = EntityLookup.getEmployerById(em, b.getsEmployer().getOrganizationId());
        } catch (Exception e1) {
            e1.printStackTrace();
            return null;
        }
        return e;
    }

    public static List<BillingItem> getBillingItemForMonth(EntityManager em, Employer er, BillingMonth bm) {
        Query q = em.createQuery("SELECT bg FROM BillingItem bg WHERE bg.employerId = :erId AND bg.billingMonth.monthId = :bmId order by bg.participantName, bg.benefitGroup");
        q.setParameter("erId", er.getId());
        q.setParameter("bmId", bm.getMonthId());
        List<BillingItem> billingItemList;
        try {
            billingItemList = (List<BillingItem>) q.getResultList();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return billingItemList;
    }

    public static List<BillingGrid> getBillingGridForMonth(EntityManager em, Employer er, BillingMonth bm) {
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.employer.id = :erid AND bg.billingMonth.monthId = :bmid");
        q.setParameter("erid", er.getId());
        q.setParameter("bmid", bm.getMonthId());
        List<BillingGrid> billingGridList;
        try {
            billingGridList = (List<BillingGrid>) q.getResultList();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return billingGridList;
    }

    public static BillingMonth getBillingMonthByBillingGuid(EntityManager em, String guid) {
        if (getBillingLinkByGuid(em, guid) != null)
            return getBillingLinkByGuid(em, guid).getBillingMonth();
        return null;
    }

    public static BillingLink getBillingLinkByGuid(EntityManager em, String guid) {
        Query q = em.createQuery("SELECT b FROM BillingLink b WHERE b.uniqueId = :guid");
        q.setParameter("guid", guid);
        BillingLink b;
        try {
            b = (BillingLink) q.getSingleResult();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return b;
    }

    public static List<BillingMonth> getBillingMonths(EntityManager em) {
        Query q = em.createQuery("SELECT b FROM BillingMonth b order by b.fullDate desc");
        return (List<BillingMonth>) q.getResultList();
    }

    public static List<BillingSummary> getMonthlyEmployerSummaries(EntityManager em, BillingMonth bm) {
        Query q = em.createQuery("SELECT bs FROM BillingSummary bs WHERE bs.billingMonth.monthId = :mId order by bs.employer.employerName");
        q.setParameter("mId", bm.getMonthId());
        List<BillingSummary> billingSummaryList;
        try {
            billingSummaryList = (List<BillingSummary>) q.getResultList();
        } catch (NoResultException e) {
            e.printStackTrace();
            billingSummaryList = new ArrayList<>();
        }
        return billingSummaryList;
    }

    public static List<BillingGrid> getEmployerMonthlyDetail(EntityManager em, Employer er, BillingMonth bm) {
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.billingMonth.monthId = :mId AND bg.employer.id = :eId order by bg.employee.lastName, bg.employee.firstName");
        q.setParameter("mId", bm.getMonthId());
        q.setParameter("eId", er.getId());
        List<BillingGrid> billingGridList;
        try {
            billingGridList = (List<BillingGrid>) q.getResultList();
        } catch (NoResultException e) {
            e.printStackTrace();
            billingGridList = new ArrayList<>();
        }
        return billingGridList;
    }

    public static BillingMonth getBillingMonthById(EntityManager em, int id) {
        Query q = em.createQuery("SELECT b FROM BillingMonth b WHERE b.monthId = :id");
        q.setParameter("id", id);
        return (BillingMonth) q.getSingleResult();
    }

    public static BillingMonth getCurrentBillingMonth(EntityManager em) {
        LocalDate today = LocalDate.now();
        int theYear = today.getYear();
        int theMonth = today.getMonthValue();
        Query q = em.createQuery("SELECT b FROM BillingMonth b WHERE b.month = :m and b.year = :y");
        q.setParameter("m", theMonth);
        q.setParameter("y", theYear);
        BillingMonth bm;
        try {
            bm = (BillingMonth) q.getSingleResult();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return bm;
    }

    public static BillingMonth getLastBillingMonth(EntityManager em, BillingMonth bm) {
        List<BillingMonth> billingMonthList = getBillingMonths(em);
        BillingMonth nextMonth;
        int currentIndex = -1;
        for (int i = 0; i < billingMonthList.size(); i++) {
            if (billingMonthList.get(i).getMonthId() == bm.getMonthId()) {
                currentIndex = i;
                break;
            }
        }
        try {
            nextMonth = billingMonthList.get(currentIndex + 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return nextMonth;
    }

    public static List<EmployeeVariance> getEeMonthlyVariance(EntityManager em, BillingMonth currentMonth, Employer er) {
        BillingMonth lastMonth = getLastBillingMonth(em, currentMonth);
        List<BillingGrid> billingGridNowList = getEmployerMonthlyDetail(em, er, currentMonth);
        List<EmployeeVariance> employeeVarianceList = new ArrayList<>();
        List<Employee> masterList = new ArrayList<>();
        if (billingGridNowList.size() == 0)
            return employeeVarianceList;
        for (BillingGrid bg : billingGridNowList) {
            masterList.add(bg.getEmployee());
            BillingGrid bgl = null;
            EmployeeVariance ev = new EmployeeVariance();
            if (lastMonth != null) {
                ev.setLastMonth(lastMonth);
                bgl = getBillingGridForEmployeeMonth(em, bg.getEmployee(), lastMonth);
            }
            ev.setEmployee(bg.getEmployee());
            ev.setEmployer(bg.getEmployer());
            ev.setCobraCurrent(booToInt(bg.isCobra()));
            ev.setFsaCurrent(booToInt(bg.isFlexSpend()));
            ev.setHraCurrent(booToInt(bg.isHealthReimb()));
            ev.setDualCurrent(booToInt(bg.isDualPlan()));
            ev.setTranCurrent(booToInt(bg.isTransit()));
            ev.setHsaCurrent(booToInt(bg.isHsa()));
            ev.setDirectCurrent(booToInt(bg.isDirect()));
            ev.setRetireeCurrent(booToInt(bg.isRetiree()));
            ev.setLsaCurrent(booToInt(bg.isLsa()));
            if (bgl == null) {
                ev.setCobraLast(0);
                ev.setFsaLast(0);
                ev.setHraLast(0);
                ev.setDualLast(0);
                ev.setTranLast(0);
                ev.setHsaLast(0);
                ev.setDirectLast(0);
                ev.setRetireeLast(0);
                ev.setLsaLast(0);
            } else {
                ev.setCobraLast(booToInt(bgl.isCobra()));
                ev.setFsaLast(booToInt(bgl.isFlexSpend()));
                ev.setHraLast(booToInt(bgl.isHealthReimb()));
                ev.setDualLast(booToInt(bgl.isDualPlan()));
                ev.setTranLast(booToInt(bgl.isTransit()));
                ev.setHsaLast(booToInt(bgl.isHsa()));
                ev.setDirectLast(booToInt(bgl.isDirect()));
                ev.setRetireeLast(booToInt(bgl.isRetiree()));
                ev.setLsaLast(booToInt(bgl.isLsa()));
            }
            employeeVarianceList.add(ev);
        }
        if (lastMonth != null) {
            List<BillingGrid> lastMonthsGrid = getEmployerMonthlyDetail(em, er, lastMonth);
            for (BillingGrid bg1 : lastMonthsGrid) {
                boolean hasEmployee = false;
                for (Employee ee : masterList) {
                    if (bg1.getEmployee().getId() == ee.getId()) {
                        hasEmployee = true;
                        break;
                    }
                }
                if (!hasEmployee) {
                    EmployeeVariance ev = new EmployeeVariance();
                    ev.setEmployee(bg1.getEmployee());
                    ev.setEmployer(bg1.getEmployer());
                    ev.setCobraLast(booToInt(bg1.isCobra()));
                    ev.setFsaLast(booToInt(bg1.isFlexSpend()));
                    ev.setHraLast(booToInt(bg1.isHealthReimb()));
                    ev.setDualLast(booToInt(bg1.isDualPlan()));
                    ev.setTranLast(booToInt(bg1.isTransit()));
                    ev.setHsaLast(booToInt(bg1.isHsa()));
                    ev.setDirectLast(booToInt(bg1.isDirect()));
                    ev.setRetireeLast(booToInt(bg1.isRetiree()));
                    ev.setLsaLast(booToInt(bg1.isLsa()));
                    ev.setCobraCurrent(0);
                    ev.setFsaCurrent(0);
                    ev.setHraCurrent(0);
                    ev.setDualCurrent(0);
                    ev.setTranCurrent(0);
                    ev.setHsaCurrent(0);
                    ev.setDirectCurrent(0);
                    ev.setRetireeCurrent(0);
                    ev.setLsaCurrent(0);
                    employeeVarianceList.add(ev);
                }
            }
        }
        return employeeVarianceList;
    }

    private static int booToInt(boolean inPut) {
        if (inPut)
            return 1;
        return 0;
    }

    public static List<EmployerVariance> getOnlyChanges(EntityManager em, BillingMonth bm) {
        List<EmployerVariance> changeOnlyList = new ArrayList<>();
        List<EmployerVariance> fullList = getMonthlyVariance(em, bm);
        for (EmployerVariance ev : fullList) {
            if (ev.getCobraCurrent() != ev.getCobraLast() || ev.getHraCurrent() != ev.getHraLast() || ev.getFsaCurrent() != ev.getFsaLast() ||
                    ev.getHsaCurrent() != ev.getHsaLast() || ev.getDirectCurrent() != ev.getDirectLast() ||
                    ev.getLsaCurrent() != ev.getLsaLast() || ev.getTranCurrent() != ev.getTranLast()) {
                changeOnlyList.add(ev);
            }
        }
        return changeOnlyList;
    }

    public static List<EmployerVariance> getMonthlyVariance(EntityManager em, BillingMonth currentMonth) {
        BillingMonth lastMonth = getLastBillingMonth(em, currentMonth);
        List<BillingSummary> billingSummaryNowList = getMonthlyEmployerSummaries(em, currentMonth);
        List<EmployerVariance> employerVarianceList = new ArrayList<>();
        if (billingSummaryNowList.size() == 0)
            return employerVarianceList;

        for (BillingSummary bs : billingSummaryNowList) {
            System.out.println(bs.getEmployer().getEmployerName() + ": " + bs.getBillingMonth().getFullDate());
            BillingSummary bsl = null;
            EmployerVariance ev = new EmployerVariance();
            if (lastMonth != null) {
                ev.setLastMonth(lastMonth);
                bsl = getBillingSummaryForEmployerMonth(em, bs.getEmployer(), lastMonth);
            }
            ev.setEmployer(bs.getEmployer());
            ev.setCurrentMonth(currentMonth);
            ev.setCobraCurrent(bs.getCobraTotal());
            ev.setFsaCurrent(bs.getFsaTotal());
            ev.setHraCurrent(bs.getHraTotal());
            ev.setHsaCurrent(bs.getHsaTotal());
            ev.setTranCurrent(bs.getTransitTotal());
            ev.setDualCurrent(bs.getDualPlanTotal());
            ev.setDirectCurrent(bs.getDirectTotal());
            ev.setRetireeCurrent(bs.getRetireeTotal());
            ev.setLsaCurrent(bs.getLsaTotal());
            if (bsl == null) {
                ev.setCobraLast(0);
                ev.setFsaLast(0);
                ev.setHraLast(0);
                ev.setHsaLast(0);
                ev.setTranLast(0);
                ev.setDualLast(0);
                ev.setDirectLast(0);
                ev.setRetireeLast(0);
                ev.setLsaLast(0);
            } else {
                ev.setCobraLast(bsl.getCobraTotal());
                ev.setFsaLast(bsl.getFsaTotal());
                ev.setHraLast(bsl.getHraTotal());
                ev.setHsaLast(bsl.getHsaTotal());
                ev.setTranLast(bsl.getTransitTotal());
                ev.setDualLast(bsl.getDualPlanTotal());
                ev.setDirectLast(bsl.getDirectTotal());
                ev.setRetireeLast(bsl.getRetireeTotal());
                ev.setLsaLast(bsl.getLsaTotal());
            }
            employerVarianceList.add(ev);
        }
        return employerVarianceList;
    }

    public static BillingSummary getBillingSummaryForEmployerMonth(EntityManager em, Employer er, BillingMonth bm) {
        Query q = em.createQuery("SELECT bs FROM BillingSummary bs WHERE bs.employer.id = :eId AND bs.billingMonth.monthId = :mId");
        q.setParameter("mId", bm.getMonthId());
        q.setParameter("eId", er.getId());
        BillingSummary bs;
        try {
            bs = (BillingSummary) q.getSingleResult();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return bs;
    }

    public static String getLastGuid(EntityManager em, Employer er) {
        Query q = em.createQuery("SELECT bl FROM BillingLink bl WHERE bl.employer.id = :eId ORDER BY bl.billingMonth.fullDate DESC");
        q.setParameter("eId", er.getId());
        q.setMaxResults(1);
        try {
            BillingLink bl = (BillingLink) q.getSingleResult();
            return bl.getUniqueId();
        } catch (NoResultException e) {
            return null;
        }
    }

    public static BillingGrid getBillingGridForEmployeeMonth(EntityManager em, Employee ee, BillingMonth bm) {
        Query q = em.createQuery("SELECT bg FROM BillingGrid bg WHERE bg.employee.id = :eId and bg.billingMonth.monthId = :mId");
        q.setParameter("eId", ee.getId());
        q.setParameter("mId", bm.getMonthId());
        BillingGrid billingGrid;
        try {
            billingGrid = (BillingGrid) q.getSingleResult();
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return billingGrid;
    }
}
